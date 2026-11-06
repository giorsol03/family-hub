package com.solomonidis.familyhub.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solomonidis.familyhub.calendar.CalendarDtos.CalendarItem;
import com.solomonidis.familyhub.calendar.CalendarDtos.EventRequest;
import com.solomonidis.familyhub.calendar.CalendarDtos.ItemKind;
import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlanRequest;
import com.solomonidis.familyhub.mealplan.MealPlanService;
import com.solomonidis.familyhub.mealplan.MealSlot;
import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberRepository;
import com.solomonidis.familyhub.member.MemberSummary;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeRequest;
import com.solomonidis.familyhub.recipe.RecipeService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithUserDetails("admin")
class CalendarServiceTest {

    static final LocalDate NOV_10 = LocalDate.of(2026, 11, 10);

    @Autowired
    CalendarService service;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MealPlanService mealPlanService;

    @Autowired
    RecipeService recipeService;

    Member admin;
    Member maria;

    @BeforeEach
    void setUp() {
        admin = memberRepository.findByUsername("admin").orElseThrow();
        maria = memberRepository.save(new Member("Maria", "maria", "{noop}x", "#ff0000"));
    }

    @Test
    void createTimedEventWithAttendees() {
        CalendarItem item = service.create(new EventRequest("Dinner at Yiayia's", "Bring dessert", "Limassol",
            EventType.MEAL, false, NOV_10.atTime(19, 30), NOV_10.atTime(22, 0), List.of(maria.getId(), admin.getId())));

        assertThat(item.kind()).isEqualTo(ItemKind.EVENT);
        assertThat(item.start()).isEqualTo(NOV_10.atTime(19, 30));
        assertThat(item.attendees()).extracting(MemberSummary::name).containsExactly("Admin", "Maria");
        assertThat(item.createdBy().name()).isEqualTo("Admin");
    }

    @Test
    void allDayEventsAreNormalisedToMidnightAndSpanDays() {
        CalendarItem trip = service.create(new EventRequest("Trip to Troodos", null, null, EventType.OTHER, true,
            NOV_10.atTime(15, 45), NOV_10.plusDays(2).atTime(9, 0), List.of()));

        assertThat(trip.start()).isEqualTo(NOV_10.atStartOfDay());
        assertThat(trip.end()).isEqualTo(NOV_10.plusDays(2).atStartOfDay());
        // visible on every day it covers
        assertThat(service.items(NOV_10.plusDays(2), NOV_10.plusDays(2), null).items()).hasSize(1);
        assertThat(service.items(NOV_10.plusDays(3), NOV_10.plusDays(3), null).items()).isEmpty();
    }

    @Test
    void rangeIncludesOverlappingEventsOnly() {
        event("Before", NOV_10.minusDays(1).atTime(10, 0), NOV_10.minusDays(1).atTime(11, 0), List.of());
        event("Inside", NOV_10.atTime(10, 0), NOV_10.atTime(11, 0), List.of());
        event("Overnight", NOV_10.minusDays(1).atTime(22, 0), NOV_10.atTime(2, 0), List.of());
        event("After", NOV_10.plusDays(1).atTime(0, 0), NOV_10.plusDays(1).atTime(1, 0), List.of());

        assertThat(titles(service.items(NOV_10, NOV_10, null).items())).containsExactly("Overnight", "Inside");
    }

    @Test
    void memberFilterKeepsTheirEventsHouseholdEventsAndMeals() {
        event("Maria's yoga", NOV_10.atTime(9, 0), NOV_10.atTime(10, 0), List.of(maria.getId()));
        event("Admin's gym", NOV_10.atTime(18, 0), NOV_10.atTime(19, 0), List.of(admin.getId()));
        event("Family BBQ", NOV_10.atTime(13, 0), NOV_10.atTime(16, 0), List.of());
        Long soup = recipeService.create(new RecipeRequest("Soup", null, null, null, null, null, List.of(), List.of())).id();
        mealPlanService.plan(new PlanRequest(NOV_10, MealSlot.DINNER, soup));

        assertThat(titles(service.items(NOV_10, NOV_10, maria.getId()).items()))
            .containsExactly("Dinner: Soup", "Maria's yoga", "Family BBQ");
        assertThat(service.items(NOV_10, NOV_10, null).items()).hasSize(4);
    }

    @Test
    void mealPlanEntriesAppearAsReadOnlyAllDayItems() {
        Long soup = recipeService.create(new RecipeRequest("Soup", null, null, null, null, null, List.of(), List.of())).id();
        mealPlanService.plan(new PlanRequest(NOV_10, MealSlot.DINNER, soup));
        mealPlanService.plan(new PlanRequest(NOV_10, MealSlot.BREAKFAST, soup));

        List<CalendarItem> items = service.items(NOV_10, NOV_10, null).items();

        assertThat(titles(items)).containsExactly("Breakfast: Soup", "Dinner: Soup");
        assertThat(items.getFirst().kind()).isEqualTo(ItemKind.MEAL_PLAN);
        assertThat(items.getFirst().allDay()).isTrue();
        assertThat(items.getFirst().recipeId()).isEqualTo(soup);
        assertThat(items.getFirst().slot()).isEqualTo(MealSlot.BREAKFAST);
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> event("Bad", NOV_10.atTime(12, 0), NOV_10.atTime(11, 0), List.of()))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("field").isEqualTo("end");
    }

    @Test
    void unknownAttendeeIsRejected() {
        assertThatThrownBy(() -> event("X", NOV_10.atTime(12, 0), NOV_10.atTime(13, 0), List.of(999_999L)))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("field").isEqualTo("attendeeIds");
    }

    @Test
    void updateAndDelete() {
        Long id = event("Dentist", NOV_10.atTime(9, 0), NOV_10.atTime(9, 30), List.of(admin.getId())).id();

        CalendarItem updated = service.update(id, new EventRequest("Dentist (moved)", null, "Clinic", EventType.OTHER,
            false, NOV_10.atTime(11, 0), NOV_10.atTime(11, 30), List.of(maria.getId())));
        assertThat(updated.title()).isEqualTo("Dentist (moved)");
        assertThat(updated.attendees()).extracting(MemberSummary::name).containsExactly("Maria");

        service.delete(id);
        assertThat(service.items(NOV_10, NOV_10, null).items()).isEmpty();
    }

    @Test
    void hugeRangesAreRejected() {
        assertThatThrownBy(() -> service.items(NOV_10, NOV_10.plusYears(1), null))
            .isInstanceOf(BusinessRuleException.class);
    }

    private CalendarItem event(String title, LocalDateTime start, LocalDateTime end, List<Long> attendees) {
        return service.create(new EventRequest(title, null, null, EventType.EVENT, false, start, end, attendees));
    }

    private static List<String> titles(List<CalendarItem> items) {
        return items.stream().map(CalendarItem::title).toList();
    }
}
