package com.solomonidis.familyhub.chore;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import com.solomonidis.familyhub.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A reusable chore ("default"), optionally recurring. */
@Entity
@Table(name = "chore_template")
public class ChoreTemplate extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_assignee_id")
    private Member defaultAssignee;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 15)
    private Recurrence recurrence = Recurrence.NONE;

    /** Days between occurrences; only for {@link Recurrence#EVERY_N_DAYS}. */
    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;

    @Column(nullable = false)
    private boolean active = true;

    protected ChoreTemplate() {
        // for JPA
    }

    public ChoreTemplate(String name) {
        this.name = name;
    }

    /** Next date after {@code today} for a chore scheduled on {@code scheduled}; null when not recurring or inactive. */
    public LocalDate nextDate(LocalDate scheduled, LocalDate today) {
        return active ? recurrence.next(scheduled, today, recurrenceInterval) : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Member getDefaultAssignee() {
        return defaultAssignee;
    }

    public void setDefaultAssignee(Member defaultAssignee) {
        this.defaultAssignee = defaultAssignee;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrence(Recurrence recurrence, Integer interval) {
        this.recurrence = recurrence;
        this.recurrenceInterval = recurrence == Recurrence.EVERY_N_DAYS ? interval : null;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
