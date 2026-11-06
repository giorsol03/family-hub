package com.solomonidis.familyhub.chore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solomonidis.familyhub.chore.ChoreDtos.ChoreListResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreRequest;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreTemplateRequest;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreTemplateResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.FinishResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.View;
import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
class ChoreServiceTest {

    @Autowired
    ChoreService service;

    @Autowired
    ChoreTemplateService templateService;

    @Autowired
    MemberRepository memberRepository;

    LocalDate today;
    Member admin;
    Member maria;

    @BeforeEach
    void setUp() {
        today = service.today();
        admin = memberRepository.findByUsername("admin").orElseThrow();
        maria = memberRepository.save(new Member("Maria", "maria", "{noop}x", "#ff0000"));
    }

    @Test
    void completingARecurringChoreCreatesTheNextOne() {
        ChoreTemplateResponse weekly = template("Mopping", Recurrence.WEEKLY, null);
        ChoreResponse chore = service.createFromTemplate(weekly.id(), today, maria.getId());

        FinishResponse result = service.finish(chore.id(), ChoreStatus.DONE);

        assertThat(result.chore().status()).isEqualTo(ChoreStatus.DONE);
        assertThat(result.chore().completedBy().name()).isEqualTo("Admin");
        assertThat(result.chore().completedAt()).isNotNull();
        assertThat(result.next()).isNotNull();
        assertThat(result.next().scheduledDate()).isEqualTo(today.plusWeeks(1));
        assertThat(result.next().assignee().id()).isEqualTo(maria.getId());
        assertThat(result.next().status()).isEqualTo(ChoreStatus.TODO);
        assertThat(result.next().recurrence()).isEqualTo(Recurrence.WEEKLY);
    }

    @Test
    void lateCompletionNeverSchedulesTheNextOneInThePast() {
        ChoreTemplateResponse daily = template("Dishes", Recurrence.DAILY, null);
        ChoreResponse late = service.createFromTemplate(daily.id(), today.minusDays(3), null);

        assertThat(service.finish(late.id(), ChoreStatus.DONE).next().scheduledDate()).isEqualTo(today.plusDays(1));
    }

    @Test
    void skippingAlsoCreatesTheNextOne() {
        ChoreTemplateResponse everyThree = template("Laundry", Recurrence.EVERY_N_DAYS, 3);
        ChoreResponse chore = service.createFromTemplate(everyThree.id(), today, null);

        FinishResponse result = service.finish(chore.id(), ChoreStatus.SKIPPED);

        assertThat(result.chore().status()).isEqualTo(ChoreStatus.SKIPPED);
        assertThat(result.next().scheduledDate()).isEqualTo(today.plusDays(3));
    }

    @Test
    void reopeningAndFinishingAgainDoesNotDuplicateTheNextOne() {
        ChoreTemplateResponse weekly = template("Vacuuming", Recurrence.WEEKLY, null);
        ChoreResponse chore = service.createFromTemplate(weekly.id(), today, null);

        assertThat(service.finish(chore.id(), ChoreStatus.DONE).next()).isNotNull();
        service.reopen(chore.id());
        assertThat(service.finish(chore.id(), ChoreStatus.DONE).next()).isNull();
    }

    @Test
    void nonRecurringAndManualChoresHaveNoNextOne() {
        ChoreTemplateResponse once = template("Clean garage", Recurrence.NONE, null);
        ChoreResponse fromTemplate = service.createFromTemplate(once.id(), today, null);
        ChoreResponse manual = service.create(new ChoreRequest("Fix shelf", null, today, null));

        assertThat(service.finish(fromTemplate.id(), ChoreStatus.DONE).next()).isNull();
        assertThat(service.finish(manual.id(), ChoreStatus.DONE).next()).isNull();
    }

    @Test
    void deactivatedTemplatesStopRecurring() {
        ChoreTemplateResponse daily = template("Plants", Recurrence.DAILY, null);
        ChoreResponse chore = service.createFromTemplate(daily.id(), today, null);
        templateService.setActive(daily.id(), false);

        assertThat(service.finish(chore.id(), ChoreStatus.DONE).next()).isNull();
    }

    @Test
    void finishingTwiceIsRejected() {
        ChoreResponse chore = service.create(new ChoreRequest("Fix shelf", null, today, null));
        service.finish(chore.id(), ChoreStatus.DONE);

        assertThatThrownBy(() -> service.finish(chore.id(), ChoreStatus.SKIPPED))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void fromTemplateUsesTheDefaultAssigneeAndToday() {
        ChoreTemplateResponse t = templateService.create(new ChoreTemplateRequest(
            "Rubbish", "Recycling too", maria.getId(), 5, Recurrence.EVERY_N_DAYS, 2));

        ChoreResponse chore = service.createFromTemplate(t.id(), null, null);

        assertThat(chore.scheduledDate()).isEqualTo(today);
        assertThat(chore.assignee().name()).isEqualTo("Maria");
        assertThat(chore.description()).isEqualTo("Recycling too");
        assertThat(chore.estimatedMinutes()).isEqualTo(5);
        assertThat(chore.templateId()).isEqualTo(t.id());
    }

    @Test
    void viewsSelectTheRightChores() {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        ChoreResponse todayTodo = service.create(new ChoreRequest("Today", null, today, maria.getId()));
        ChoreResponse todayDone = service.create(new ChoreRequest("Today done", null, today, null));
        service.finish(todayDone.id(), ChoreStatus.DONE);
        ChoreResponse overdue = service.create(new ChoreRequest("Old", null, today.minusDays(10), null));
        ChoreResponse oldDone = service.create(new ChoreRequest("Old done", null, today.minusDays(10), null));
        service.finish(oldDone.id(), ChoreStatus.DONE);
        ChoreResponse sunday = service.create(new ChoreRequest("Sunday", null, monday.plusDays(6), null));
        service.create(new ChoreRequest("Next week", null, monday.plusDays(7), null));

        ChoreListResponse todayView = service.list(View.TODAY, null);
        assertThat(todayView.today()).isEqualTo(today);
        assertThat(todayView.chores()).extracting(ChoreResponse::id).containsExactlyInAnyOrder(todayTodo.id(), todayDone.id());

        ChoreListResponse week = service.list(View.WEEK, null);
        assertThat(week.from()).isEqualTo(monday);
        assertThat(week.to()).isEqualTo(monday.plusDays(6));
        assertThat(week.chores()).extracting(ChoreResponse::id)
            .contains(todayTodo.id(), todayDone.id(), sunday.id())
            .doesNotContain(overdue.id());

        assertThat(service.list(View.OVERDUE, null).chores()).extracting(ChoreResponse::id).containsExactly(overdue.id());
    }

    @Test
    void assigneeFilter() {
        ChoreResponse mine = service.create(new ChoreRequest("Mine", null, today, admin.getId()));
        ChoreResponse hers = service.create(new ChoreRequest("Hers", null, today, maria.getId()));
        ChoreResponse nobody = service.create(new ChoreRequest("Nobody's", null, today, null));

        assertThat(service.list(View.TODAY, maria.getId()).chores()).extracting(ChoreResponse::id).containsExactly(hers.id());
        assertThat(service.list(View.TODAY, null).chores()).extracting(ChoreResponse::id)
            .contains(mine.id(), hers.id(), nobody.id());
    }

    @Test
    void unknownAssigneeIsAFieldError() {
        assertThatThrownBy(() -> service.create(new ChoreRequest("X", null, today, 999_999L)))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("field").isEqualTo("assigneeId");
    }

    @Test
    void seedTemplatesExist() {
        assertThat(templateService.list(false)).extracting(ChoreTemplateResponse::name).contains(
            "Wash the dishes", "Mopping", "Vacuuming", "Laundry", "Take out the rubbish",
            "Clean bathroom", "Change bed sheets", "Water the plants");
    }

    @Test
    void everyNDaysNeedsAnInterval() {
        assertThatThrownBy(() -> templateService.create(new ChoreTemplateRequest(
            "Bad", null, null, null, Recurrence.EVERY_N_DAYS, null)))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("field").isEqualTo("recurrenceInterval");
    }

    private ChoreTemplateResponse template(String name, Recurrence recurrence, Integer interval) {
        return templateService.create(new ChoreTemplateRequest(name, null, null, null, recurrence, interval));
    }
}
