package com.solomonidis.familyhub.calendar;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import com.solomonidis.familyhub.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A calendar event. Times are local to the household timezone. For all-day events
 * start/end are at 00:00 and {@code endAt} is the inclusive last day.
 */
@Entity
@Table(name = "calendar_event")
public class CalendarEvent extends AuditableEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 15)
    private EventType type;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @ManyToMany
    @JoinTable(name = "calendar_event_attendee",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id"))
    private Set<Member> attendees = new HashSet<>();

    protected CalendarEvent() {
        // for JPA
    }

    public CalendarEvent(String title, EventType type) {
        this.title = title;
        this.type = type;
    }

    /** Sets the time span; all-day events are normalised to midnight of their first and last day. */
    public void setSpan(boolean allDay, LocalDateTime start, LocalDateTime end) {
        this.allDay = allDay;
        this.startAt = allDay ? start.toLocalDate().atStartOfDay() : start;
        this.endAt = allDay ? end.toLocalDate().atStartOfDay() : end;
    }

    public void replaceAttendees(Set<Member> members) {
        attendees.clear();
        attendees.addAll(members);
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public Set<Member> getAttendees() {
        return attendees;
    }
}
