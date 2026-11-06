package com.solomonidis.familyhub.calendar;

import com.solomonidis.familyhub.mealplan.MealPlanDtos.EntryResponse;
import com.solomonidis.familyhub.mealplan.MealSlot;
import com.solomonidis.familyhub.member.MemberSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CalendarDtos {

    private CalendarDtos() {
    }

    public enum ItemKind { EVENT, MEAL_PLAN }

    /** For all-day events only the dates of {@code start}/{@code end} matter ({@code end} = inclusive last day). */
    public record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @Size(max = 200) String location,
        @NotNull EventType type,
        /* optional in JSON; null = false (a primitive would make Jackson 3 reject a missing value) */
        Boolean allDay,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        List<Long> attendeeIds
    ) {
        public boolean isAllDay() {
            return Boolean.TRUE.equals(allDay);
        }
    }

    /**
     * One thing on the calendar: a real event, or a meal-plan entry shown read-only
     * (kind MEAL_PLAN, all-day, with its slot and recipe id; edit it in the planner).
     */
    public record CalendarItem(
        ItemKind kind,
        Long id,
        String title,
        EventType type,
        boolean allDay,
        LocalDateTime start,
        LocalDateTime end,
        String location,
        String description,
        List<MemberSummary> attendees,
        MemberSummary createdBy,
        MealSlot slot,
        Long recipeId
    ) {
        static CalendarItem of(CalendarEvent e) {
            List<MemberSummary> attendees = e.getAttendees().stream()
                .map(MemberSummary::of)
                .sorted(Comparator.comparing(MemberSummary::name))
                .toList();
            return new CalendarItem(ItemKind.EVENT, e.getId(), e.getTitle(), e.getType(), e.isAllDay(),
                e.getStartAt(), e.getEndAt(), e.getLocation(), e.getDescription(), attendees,
                MemberSummary.of(e.getCreatedBy()), null, null);
        }

        static CalendarItem of(EntryResponse entry) {
            String slot = entry.slot().name().charAt(0) + entry.slot().name().substring(1).toLowerCase(Locale.ROOT);
            LocalDateTime day = entry.date().atStartOfDay();
            return new CalendarItem(ItemKind.MEAL_PLAN, entry.id(), slot + ": " + entry.recipe().title(),
                EventType.MEAL, true, day, day, null, null, List.of(), entry.plannedBy(),
                entry.slot(), entry.recipe().id());
        }
    }

    public record CalendarResponse(LocalDate from, LocalDate to, LocalDate today, List<CalendarItem> items) {
    }
}
