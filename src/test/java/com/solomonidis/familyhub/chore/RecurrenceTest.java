package com.solomonidis.familyhub.chore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RecurrenceTest {

    @ParameterizedTest(name = "{0} scheduled {1}, done {2} -> {4}")
    @CsvSource({
        // done on the day
        "DAILY,        2026-11-09, 2026-11-09,    , 2026-11-10",
        "WEEKLY,       2026-11-09, 2026-11-09,    , 2026-11-16",
        "EVERY_N_DAYS, 2026-11-09, 2026-11-09,  3 , 2026-11-12",
        "MONTHLY,      2026-11-09, 2026-11-09,    , 2026-12-09",
        // done late: keep the rhythm, but never schedule in the past
        "DAILY,        2026-11-05, 2026-11-09,    , 2026-11-10",
        "WEEKLY,       2026-11-09, 2026-11-11,    , 2026-11-16",
        "WEEKLY,       2026-11-02, 2026-11-11,    , 2026-11-16",
        "EVERY_N_DAYS, 2026-11-01, 2026-11-09,  3 , 2026-11-10",
        "MONTHLY,      2026-10-15, 2026-11-20,    , 2026-12-15",
        // done early: next one is still one period after the scheduled date
        "WEEKLY,       2026-11-16, 2026-11-12,    , 2026-11-23",
        // month ends don't drift (counted from the original date)
        "MONTHLY,      2026-01-31, 2026-01-31,    , 2026-02-28",
        "MONTHLY,      2026-01-31, 2026-03-01,    , 2026-03-31",
    })
    void nextDate(Recurrence recurrence, LocalDate scheduled, LocalDate today, Integer interval, LocalDate expected) {
        assertThat(recurrence.next(scheduled, today, interval)).isEqualTo(expected);
    }

    @Test
    void noneHasNoNextDate() {
        assertThat(Recurrence.NONE.next(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9), null)).isNull();
    }

    @Test
    void everyNDaysNeedsAnInterval() {
        assertThatThrownBy(() -> Recurrence.EVERY_N_DAYS.next(LocalDate.now(), LocalDate.now(), null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
