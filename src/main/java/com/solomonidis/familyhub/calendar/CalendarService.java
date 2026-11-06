package com.solomonidis.familyhub.calendar;

import com.solomonidis.familyhub.calendar.CalendarDtos.CalendarItem;
import com.solomonidis.familyhub.calendar.CalendarDtos.CalendarResponse;
import com.solomonidis.familyhub.calendar.CalendarDtos.EventRequest;
import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.mealplan.MealPlanService;
import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CalendarService {

    /** Guard against accidentally huge queries (a month view needs ~6 weeks). */
    private static final int MAX_RANGE_DAYS = 100;

    private final CalendarEventRepository repository;
    private final MemberRepository memberRepository;
    private final MealPlanService mealPlanService;
    private final Clock clock;

    public CalendarService(CalendarEventRepository repository, MemberRepository memberRepository,
                           MealPlanService mealPlanService, Clock clock) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.mealPlanService = mealPlanService;
        this.clock = clock;
    }

    /**
     * Events overlapping [from, to] plus meal-plan entries in that range, sorted by start (all-day first).
     * With {@code memberId}: that member's events, plus events nobody in particular attends (household-wide)
     * and the meal plan (everyone eats).
     */
    @Transactional(readOnly = true)
    public CalendarResponse items(LocalDate from, LocalDate to, Long memberId) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("to", "'to' must not be before 'from'");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new BusinessRuleException("to", "Range too large (max " + MAX_RANGE_DAYS + " days)");
        }
        List<CalendarItem> items = new ArrayList<>();
        repository.findOverlapping(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
            .filter(e -> memberId == null || e.getAttendees().isEmpty()
                || e.getAttendees().stream().anyMatch(m -> m.getId().equals(memberId)))
            .map(CalendarItem::of)
            .forEach(items::add);
        mealPlanService.between(from, to).stream().map(CalendarItem::of).forEach(items::add);
        items.sort(Comparator.comparing((CalendarItem i) -> i.start().toLocalDate())
            .thenComparing(i -> !i.allDay())
            .thenComparing(CalendarItem::start)
            .thenComparing(i -> i.slot() == null ? -1 : i.slot().ordinal())
            .thenComparing(CalendarItem::title));
        return new CalendarResponse(from, to, LocalDate.now(clock), items);
    }

    @Transactional(readOnly = true)
    public CalendarItem get(Long id) {
        return CalendarItem.of(find(id));
    }

    public CalendarItem create(EventRequest request) {
        CalendarEvent event = new CalendarEvent(request.title().trim(), request.type());
        apply(event, request);
        return CalendarItem.of(repository.save(event));
    }

    public CalendarItem update(Long id, EventRequest request) {
        CalendarEvent event = find(id);
        event.setTitle(request.title().trim());
        event.setType(request.type());
        apply(event, request);
        return CalendarItem.of(event);
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    private void apply(CalendarEvent event, EventRequest request) {
        boolean allDay = request.isAllDay();
        boolean endBeforeStart = allDay
            ? request.end().toLocalDate().isBefore(request.start().toLocalDate())
            : request.end().isBefore(request.start());
        if (endBeforeStart) {
            throw new BusinessRuleException("end", "Ends before it starts");
        }
        event.setSpan(allDay, request.start(), request.end());
        event.setDescription(blankToNull(request.description()));
        event.setLocation(blankToNull(request.location()));
        event.replaceAttendees(findMembers(request.attendeeIds()));
    }

    private Set<Member> findMembers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> unique = new HashSet<>(ids);
        List<Member> members = memberRepository.findAllById(unique);
        if (members.size() != unique.size()) {
            throw new BusinessRuleException("attendeeIds", "Unknown member");
        }
        return new HashSet<>(members);
    }

    private CalendarEvent find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Event " + id + " not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
