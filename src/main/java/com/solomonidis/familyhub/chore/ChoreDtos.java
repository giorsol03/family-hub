package com.solomonidis.familyhub.chore;

import com.solomonidis.familyhub.member.MemberSummary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response records for the chores API. */
public final class ChoreDtos {

    private ChoreDtos() {
    }

    public enum View { TODAY, WEEK, OVERDUE }

    public record ChoreRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 1000) String description,
        @NotNull LocalDate scheduledDate,
        Long assigneeId
    ) {
    }

    /** Optional overrides when creating a chore from a template (both default to today / the template's assignee). */
    public record FromTemplateRequest(LocalDate scheduledDate, Long assigneeId) {
    }

    public record ChoreResponse(
        Long id,
        String title,
        String description,
        LocalDate scheduledDate,
        MemberSummary assignee,
        ChoreStatus status,
        Instant completedAt,
        MemberSummary completedBy,
        Long templateId,
        Recurrence recurrence,
        Integer estimatedMinutes
    ) {
        static ChoreResponse of(Chore c) {
            ChoreTemplate t = c.getTemplate();
            return new ChoreResponse(c.getId(), c.getTitle(), c.getDescription(), c.getScheduledDate(),
                MemberSummary.of(c.getAssignee()), c.getStatus(), c.getCompletedAt(),
                MemberSummary.of(c.getCompletedBy()),
                t == null ? null : t.getId(),
                t == null ? Recurrence.NONE : t.getRecurrence(),
                t == null ? null : t.getEstimatedMinutes());
        }
    }

    /** A view of chores plus the dates it covers (in the household timezone), so the UI doesn't guess "today". */
    public record ChoreListResponse(View view, LocalDate today, LocalDate from, LocalDate to, List<ChoreResponse> chores) {
    }

    /** Result of finishing a chore; {@code next} is the auto-created next occurrence for recurring chores. */
    public record FinishResponse(ChoreResponse chore, ChoreResponse next) {
    }

    public record ChoreTemplateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        Long defaultAssigneeId,
        @Min(value = 1, message = "At least 1 minute") @Max(value = 600, message = "At most 600 minutes") Integer estimatedMinutes,
        @NotNull Recurrence recurrence,
        @Min(value = 1, message = "At least 1 day") @Max(value = 365, message = "At most 365 days") Integer recurrenceInterval
    ) {
    }

    public record ChoreTemplateResponse(
        Long id,
        String name,
        String description,
        MemberSummary defaultAssignee,
        Integer estimatedMinutes,
        Recurrence recurrence,
        Integer recurrenceInterval,
        boolean active
    ) {
        static ChoreTemplateResponse of(ChoreTemplate t) {
            return new ChoreTemplateResponse(t.getId(), t.getName(), t.getDescription(),
                MemberSummary.of(t.getDefaultAssignee()), t.getEstimatedMinutes(), t.getRecurrence(),
                t.getRecurrenceInterval(), t.isActive());
        }
    }
}
