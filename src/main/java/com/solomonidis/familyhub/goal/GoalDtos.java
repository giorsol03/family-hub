package com.solomonidis.familyhub.goal;

import com.solomonidis.familyhub.member.MemberSummary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class GoalDtos {

    private GoalDtos() {
    }

    public record GoalRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long ownerId,
        /* optional; defaults to today */
        LocalDate startDate,
        @Min(value = 1, message = "1–7 days") @Max(value = 7, message = "1–7 days") Integer targetDaysPerWeek
    ) {
    }

    public record CheckRequest(@NotNull Boolean done) {
    }

    public record GoalResponse(Long id, String name, MemberSummary owner, boolean active, LocalDate startDate,
                               Integer targetDaysPerWeek) {
        static GoalResponse of(Goal g) {
            return new GoalResponse(g.getId(), g.getName(), MemberSummary.of(g.getOwner()), g.isActive(),
                g.getStartDate(), g.getTargetDaysPerWeek());
        }
    }

    /** One row of the monthly grid: the goal, its ticked days in the month, and its stats. */
    public record GoalRow(GoalResponse goal, List<LocalDate> doneDates, GoalStats stats) {
    }

    public record MonthResponse(YearMonth month, LocalDate today, int daysInMonth, List<GoalRow> rows) {
    }
}
