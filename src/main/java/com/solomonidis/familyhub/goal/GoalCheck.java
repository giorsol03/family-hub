package com.solomonidis.familyhub.goal;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** One day of one goal: ticked (done) or unticked. */
@Entity
@Table(name = "goal_check")
public class GoalCheck extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "check_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean done;

    protected GoalCheck() {
        // for JPA
    }

    public GoalCheck(Goal goal, LocalDate date, boolean done) {
        this.goal = goal;
        this.date = date;
        this.done = done;
    }

    public Goal getGoal() {
        return goal;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
