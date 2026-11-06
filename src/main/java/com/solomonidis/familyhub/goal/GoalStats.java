package com.solomonidis.familyhub.goal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

/**
 * Habit statistics for one goal.
 *
 * @param streak             consecutive done days up to today — or up to yesterday while today isn't ticked yet,
 *                           so a streak isn't "broken" until the day is over
 * @param doneThisMonth      ticked days in the month
 * @param eligibleDays       days in the month the goal could have been done so far
 *                           (from the start date, not after today)
 * @param percentThisMonth   doneThisMonth / eligibleDays, rounded; null when there are no eligible days yet
 * @param doneThisWeek       ticked days Monday–Sunday of the current week (for the days-per-week target)
 */
public record GoalStats(int streak, int doneThisMonth, int eligibleDays, Integer percentThisMonth, int doneThisWeek) {

    public static GoalStats compute(LocalDate startDate, Set<LocalDate> doneDates, YearMonth month, LocalDate today) {
        return new GoalStats(
            streak(startDate, doneDates, today),
            (int) doneDates.stream().filter(d -> YearMonth.from(d).equals(month) && !d.isAfter(today)).count(),
            eligibleDays(startDate, month, today),
            percent(startDate, doneDates, month, today),
            doneThisWeek(doneDates, today));
    }

    static int streak(LocalDate startDate, Set<LocalDate> doneDates, LocalDate today) {
        LocalDate day = doneDates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (!day.isBefore(startDate) && doneDates.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    static int eligibleDays(LocalDate startDate, YearMonth month, LocalDate today) {
        LocalDate from = later(month.atDay(1), startDate);
        LocalDate to = earlier(month.atEndOfMonth(), today);
        return to.isBefore(from) ? 0 : (int) (to.toEpochDay() - from.toEpochDay() + 1);
    }

    private static Integer percent(LocalDate startDate, Set<LocalDate> doneDates, YearMonth month, LocalDate today) {
        int eligible = eligibleDays(startDate, month, today);
        if (eligible == 0) {
            return null;
        }
        long done = doneDates.stream()
            .filter(d -> YearMonth.from(d).equals(month) && !d.isBefore(startDate) && !d.isAfter(today))
            .count();
        return (int) Math.round(100.0 * done / eligible);
    }

    private static int doneThisWeek(Set<LocalDate> doneDates, LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return (int) doneDates.stream().filter(d -> !d.isBefore(monday) && !d.isAfter(today)).count();
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
