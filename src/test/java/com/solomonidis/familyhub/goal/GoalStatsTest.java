package com.solomonidis.familyhub.goal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GoalStatsTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 11); // a Wednesday
    static final YearMonth NOV = YearMonth.of(2026, 11);
    static final LocalDate LONG_AGO = LocalDate.of(2026, 1, 1);

    static Set<LocalDate> nov(int... days) {
        return IntStream.of(days).mapToObj(d -> LocalDate.of(2026, 11, d)).collect(Collectors.toSet());
    }

    @Test
    void streakCountsBackFromToday() {
        assertThat(GoalStats.streak(LONG_AGO, nov(9, 10, 11), TODAY)).isEqualTo(3);
    }

    @Test
    void streakIsNotBrokenUntilTodayIsOver() {
        assertThat(GoalStats.streak(LONG_AGO, nov(8, 9, 10), TODAY)).isEqualTo(3);
    }

    @Test
    void aMissedDayResetsTheStreak() {
        assertThat(GoalStats.streak(LONG_AGO, nov(7, 8, 10, 11), TODAY)).isEqualTo(2);
        assertThat(GoalStats.streak(LONG_AGO, nov(7, 8, 9), TODAY)).isZero();
    }

    @Test
    void streakCrossesMonthBoundariesButNotTheStartDate() {
        Set<LocalDate> done = Set.of(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
        assertThat(GoalStats.streak(LONG_AGO, done, LocalDate.of(2026, 11, 2))).isEqualTo(3);
        assertThat(GoalStats.streak(LocalDate.of(2026, 11, 1), done, LocalDate.of(2026, 11, 2))).isEqualTo(2);
    }

    @Test
    void percentUsesOnlyDaysSoFar() {
        GoalStats stats = GoalStats.compute(LONG_AGO, nov(1, 2, 3, 4, 5, 6, 7, 8), NOV, TODAY);

        assertThat(stats.eligibleDays()).isEqualTo(11);
        assertThat(stats.doneThisMonth()).isEqualTo(8);
        assertThat(stats.percentThisMonth()).isEqualTo(73);
    }

    @Test
    void percentStartsAtTheGoalsStartDate() {
        GoalStats stats = GoalStats.compute(LocalDate.of(2026, 11, 7), nov(7, 8, 9, 10, 11), NOV, TODAY);

        assertThat(stats.eligibleDays()).isEqualTo(5);
        assertThat(stats.percentThisMonth()).isEqualTo(100);
    }

    @Test
    void pastAndFutureMonths() {
        GoalStats october = GoalStats.compute(LONG_AGO, Set.of(LocalDate.of(2026, 10, 1)), YearMonth.of(2026, 10), TODAY);
        assertThat(october.eligibleDays()).isEqualTo(31);
        assertThat(october.percentThisMonth()).isEqualTo(3);

        GoalStats december = GoalStats.compute(LONG_AGO, Set.of(), YearMonth.of(2026, 12), TODAY);
        assertThat(december.eligibleDays()).isZero();
        assertThat(december.percentThisMonth()).isNull();
    }

    @Test
    void doneThisWeekCountsMondayToToday() {
        // Mon 9, Tue 10, Wed 11 this week; Sun 8 is last week
        assertThat(GoalStats.compute(LONG_AGO, nov(8, 9, 11), NOV, TODAY).doneThisWeek()).isEqualTo(2);
    }
}
