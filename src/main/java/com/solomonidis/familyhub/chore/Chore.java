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
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A chore to do on a given day. */
@Entity
@Table(name = "chore")
public class Chore extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Member assignee;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private ChoreStatus status = ChoreStatus.TODO;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private Member completedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ChoreTemplate template;

    protected Chore() {
        // for JPA
    }

    public Chore(String title, String description, LocalDate scheduledDate, Member assignee, ChoreTemplate template) {
        this.title = title;
        this.description = description;
        this.scheduledDate = scheduledDate;
        this.assignee = assignee;
        this.template = template;
    }

    /** DONE or SKIPPED, recording who and when. */
    public void finish(ChoreStatus status, Member by, Instant at) {
        if (status == ChoreStatus.TODO) {
            throw new IllegalArgumentException("Use reopen() to go back to TODO");
        }
        this.status = status;
        this.completedBy = by;
        this.completedAt = at;
    }

    public void reopen() {
        this.status = ChoreStatus.TODO;
        this.completedBy = null;
        this.completedAt = null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Member getAssignee() {
        return assignee;
    }

    public void setAssignee(Member assignee) {
        this.assignee = assignee;
    }

    public ChoreStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Member getCompletedBy() {
        return completedBy;
    }

    public ChoreTemplate getTemplate() {
        return template;
    }
}
