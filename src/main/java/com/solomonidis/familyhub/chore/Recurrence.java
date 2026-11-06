package com.solomonidis.familyhub.chore;

import java.time.LocalDate;

/** How often a chore template repeats. {@link #EVERY_N_DAYS} uses the template's interval. */
public enum Recurrence {
    NONE, DAILY, WEEKLY, EVERY_N_DAYS, MONTHLY;

    /**
     * The next date after {@code today}, stepping from {@code scheduled} in whole periods so the
     * rhythm is kept (a weekly Monday chore done late on Wednesday comes back next Monday, not next Wednesday).
     * Monthly steps are counted from the original date, so the 31st becomes the 30th/28th without drifting.
     *
     * @param interval days between occurrences; only used (and required, ≥ 1) for {@link #EVERY_N_DAYS}
     * @return the next date, or null for {@link #NONE}
     */
    public LocalDate next(LocalDate scheduled, LocalDate today, Integer interval) {
        if (this == NONE) {
            return null;
        }
        if (this == EVERY_N_DAYS && (interval == null || interval < 1)) {
            throw new IllegalArgumentException("EVERY_N_DAYS needs an interval of at least 1");
        }
        LocalDate next;
        int steps = 0;
        do {
            steps++;
            next = step(scheduled, steps, interval);
        } while (!next.isAfter(today));
        return next;
    }

    private LocalDate step(LocalDate from, int steps, Integer interval) {
        return switch (this) {
            case DAILY -> from.plusDays(steps);
            case WEEKLY -> from.plusWeeks(steps);
            case EVERY_N_DAYS -> from.plusDays((long) steps * interval);
            case MONTHLY -> from.plusMonths(steps);
            case NONE -> throw new IllegalStateException();
        };
    }
}
