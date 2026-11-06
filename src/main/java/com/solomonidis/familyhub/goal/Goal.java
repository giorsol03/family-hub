package com.solomonidis.familyhub.goal;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import com.solomonidis.familyhub.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** A daily habit chosen by one member, e.g. "Yoga". */
@Entity
@Table(name = "goal")
public class Goal extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Optional: how many days a week the owner aims for. */
    @Column(name = "target_days_per_week")
    private Integer targetDaysPerWeek;

    protected Goal() {
        // for JPA
    }

    public Goal(String name, Member owner, LocalDate startDate, Integer targetDaysPerWeek) {
        this.name = name;
        this.owner = owner;
        this.startDate = startDate;
        this.targetDaysPerWeek = targetDaysPerWeek;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Member getOwner() {
        return owner;
    }

    public void setOwner(Member owner) {
        this.owner = owner;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Integer getTargetDaysPerWeek() {
        return targetDaysPerWeek;
    }

    public void setTargetDaysPerWeek(Integer targetDaysPerWeek) {
        this.targetDaysPerWeek = targetDaysPerWeek;
    }
}
