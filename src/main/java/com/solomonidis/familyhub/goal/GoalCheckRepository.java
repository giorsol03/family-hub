package com.solomonidis.familyhub.goal;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalCheckRepository extends JpaRepository<GoalCheck, Long> {

    Optional<GoalCheck> findByGoalAndDate(Goal goal, LocalDate date);

    /** All ticked days up to {@code until} — needed for streaks, which can run back across months. */
    List<GoalCheck> findByGoalInAndDoneTrueAndDateLessThanEqual(Collection<Goal> goals, LocalDate until);
}
