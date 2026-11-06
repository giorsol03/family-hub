package com.solomonidis.familyhub.chore;

import com.solomonidis.familyhub.chore.ChoreDtos.ChoreListResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreRequest;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.FinishResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.View;
import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.common.audit.CurrentMemberProvider;
import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChoreService {

    private final ChoreRepository choreRepository;
    private final ChoreTemplateRepository templateRepository;
    private final MemberRepository memberRepository;
    private final CurrentMemberProvider currentMember;
    private final Clock clock;

    public ChoreService(ChoreRepository choreRepository, ChoreTemplateRepository templateRepository,
                        MemberRepository memberRepository, CurrentMemberProvider currentMember, Clock clock) {
        this.choreRepository = choreRepository;
        this.templateRepository = templateRepository;
        this.memberRepository = memberRepository;
        this.currentMember = currentMember;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * TODAY: everything scheduled today (done ones included, shown ticked).
     * WEEK: Monday–Sunday of the current week.
     * OVERDUE: still TODO and scheduled before today.
     */
    @Transactional(readOnly = true)
    public ChoreListResponse list(View view, Long assigneeId) {
        LocalDate today = today();
        return switch (view) {
            case TODAY -> new ChoreListResponse(view, today, today, today,
                toResponses(choreRepository.findScheduledBetween(today, today, assigneeId)));
            case WEEK -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                yield new ChoreListResponse(view, today, monday, sunday,
                    toResponses(choreRepository.findScheduledBetween(monday, sunday, assigneeId)));
            }
            case OVERDUE -> new ChoreListResponse(view, today, null, today.minusDays(1),
                toResponses(choreRepository.findOverdue(today, assigneeId)));
        };
    }

    public ChoreResponse create(ChoreRequest request) {
        Chore chore = new Chore(request.title().trim(), blankToNull(request.description()), request.scheduledDate(),
            findAssignee(request.assigneeId()), null);
        return ChoreResponse.of(choreRepository.save(chore));
    }

    /** One-click: a chore from a template, today (or the given date), for the template's default assignee unless overridden. */
    public ChoreResponse createFromTemplate(Long templateId, LocalDate date, Long assigneeId) {
        ChoreTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new NotFoundException("Chore template " + templateId + " not found"));
        Member assignee = assigneeId != null ? findAssignee(assigneeId) : template.getDefaultAssignee();
        Chore chore = new Chore(template.getName(), template.getDescription(),
            date != null ? date : today(), assignee, template);
        return ChoreResponse.of(choreRepository.save(chore));
    }

    public ChoreResponse update(Long id, ChoreRequest request) {
        Chore chore = get(id);
        chore.setTitle(request.title().trim());
        chore.setDescription(blankToNull(request.description()));
        chore.setScheduledDate(request.scheduledDate());
        chore.setAssignee(findAssignee(request.assigneeId()));
        return ChoreResponse.of(chore);
    }

    public void delete(Long id) {
        choreRepository.delete(get(id));
    }

    /**
     * Marks a chore DONE or SKIPPED. For recurring templates the next occurrence is created
     * (same assignee), unless an identical TODO already exists — so re-ticking never duplicates.
     */
    public FinishResponse finish(Long id, ChoreStatus status) {
        Chore chore = get(id);
        if (chore.getStatus() != ChoreStatus.TODO) {
            throw new BusinessRuleException(null, "This chore is already " + chore.getStatus().name().toLowerCase());
        }
        chore.finish(status, currentMember.getCurrentAuditor().orElse(null), clock.instant());
        return new FinishResponse(ChoreResponse.of(chore), createNextOccurrence(chore));
    }

    /** Undo DONE/SKIPPED. A next occurrence that was already created is kept. */
    public ChoreResponse reopen(Long id) {
        Chore chore = get(id);
        chore.reopen();
        return ChoreResponse.of(chore);
    }

    private ChoreResponse createNextOccurrence(Chore finished) {
        ChoreTemplate template = finished.getTemplate();
        if (template == null) {
            return null;
        }
        LocalDate nextDate = template.nextDate(finished.getScheduledDate(), today());
        if (nextDate == null
            || choreRepository.existsByTemplateAndStatusAndScheduledDate(template, ChoreStatus.TODO, nextDate)) {
            return null;
        }
        Chore next = new Chore(template.getName(), template.getDescription(), nextDate, finished.getAssignee(), template);
        return ChoreResponse.of(choreRepository.save(next));
    }

    private Chore get(Long id) {
        return choreRepository.findById(id).orElseThrow(() -> new NotFoundException("Chore " + id + " not found"));
    }

    Member findAssignee(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessRuleException("assigneeId", "Unknown member"));
    }

    private List<ChoreResponse> toResponses(List<Chore> chores) {
        return chores.stream().map(ChoreResponse::of).toList();
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
