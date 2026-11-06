package com.solomonidis.familyhub.goal;

import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.goal.GoalDtos.GoalRequest;
import com.solomonidis.familyhub.goal.GoalDtos.GoalResponse;
import com.solomonidis.familyhub.goal.GoalDtos.GoalRow;
import com.solomonidis.familyhub.goal.GoalDtos.MonthResponse;
import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalCheckRepository checkRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    public GoalService(GoalRepository goalRepository, GoalCheckRepository checkRepository,
                       MemberRepository memberRepository, Clock clock) {
        this.goalRepository = goalRepository;
        this.checkRepository = checkRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * The monthly grid, ordered by owner then goal name. Shows active goals that had started by the end of
     * the month, plus removed (inactive) goals that have ticks in that month — so history stays visible.
     */
    @Transactional(readOnly = true)
    public MonthResponse month(YearMonth month, Long ownerId) {
        LocalDate today = today();
        YearMonth m = month != null ? month : YearMonth.from(today);
        List<Goal> candidates = goalRepository.findAllByOrderByOwnerNameAscNameAsc().stream()
            .filter(g -> ownerId == null || g.getOwner().getId().equals(ownerId))
            .filter(g -> !g.getStartDate().isAfter(m.atEndOfMonth()))
            .toList();
        Map<Long, Set<LocalDate>> doneByGoal = doneDates(candidates, today);

        List<GoalRow> rows = candidates.stream()
            .filter(g -> g.isActive() || doneByGoal.getOrDefault(g.getId(), Set.of()).stream()
                .anyMatch(d -> YearMonth.from(d).equals(m)))
            .map(g -> row(g, doneByGoal.getOrDefault(g.getId(), Set.of()), m, today))
            .toList();
        return new MonthResponse(m, today, m.lengthOfMonth(), rows);
    }

    /** Ticks or unticks a day. Days before the goal's start or in the future can't be ticked. Returns the updated row. */
    public GoalRow setCheck(Long goalId, LocalDate date, boolean done) {
        Goal goal = get(goalId);
        LocalDate today = today();
        if (date.isAfter(today)) {
            throw new BusinessRuleException(null, "You can't tick a day that hasn't happened yet");
        }
        if (date.isBefore(goal.getStartDate())) {
            throw new BusinessRuleException(null, "This goal started on " + goal.getStartDate());
        }
        checkRepository.findByGoalAndDate(goal, date).ifPresentOrElse(
            check -> check.setDone(done),
            () -> checkRepository.save(new GoalCheck(goal, date, done)));
        checkRepository.flush();
        Set<LocalDate> done2 = doneDates(List.of(goal), today).getOrDefault(goal.getId(), Set.of());
        return row(goal, done2, YearMonth.from(date), today);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(boolean includeInactive) {
        return goalRepository.findAllByOrderByOwnerNameAscNameAsc().stream()
            .filter(g -> includeInactive || g.isActive())
            .map(GoalResponse::of)
            .toList();
    }

    public GoalResponse create(GoalRequest request) {
        Goal goal = new Goal(request.name().trim(), findOwner(request.ownerId()),
            request.startDate() != null ? request.startDate() : today(), request.targetDaysPerWeek());
        return GoalResponse.of(goalRepository.save(goal));
    }

    public GoalResponse update(Long id, GoalRequest request) {
        Goal goal = get(id);
        goal.setName(request.name().trim());
        goal.setOwner(findOwner(request.ownerId()));
        if (request.startDate() != null) {
            goal.setStartDate(request.startDate());
        }
        goal.setTargetDaysPerWeek(request.targetDaysPerWeek());
        return GoalResponse.of(goal);
    }

    /** "Remove" = deactivate: the goal leaves the grid but its history is kept. */
    public GoalResponse setActive(Long id, boolean active) {
        Goal goal = get(id);
        goal.setActive(active);
        return GoalResponse.of(goal);
    }

    private GoalRow row(Goal goal, Set<LocalDate> done, YearMonth month, LocalDate today) {
        List<LocalDate> inMonth = done.stream().filter(d -> YearMonth.from(d).equals(month)).sorted().toList();
        return new GoalRow(GoalResponse.of(goal), inMonth, GoalStats.compute(goal.getStartDate(), done, month, today));
    }

    private Map<Long, Set<LocalDate>> doneDates(List<Goal> goals, LocalDate today) {
        if (goals.isEmpty()) {
            return Map.of();
        }
        return checkRepository.findByGoalInAndDoneTrueAndDateLessThanEqual(goals, today).stream()
            .collect(Collectors.groupingBy(c -> c.getGoal().getId(),
                Collectors.mapping(GoalCheck::getDate, Collectors.toSet())));
    }

    private Member findOwner(Long ownerId) {
        return memberRepository.findById(ownerId)
            .orElseThrow(() -> new BusinessRuleException("ownerId", "Unknown member"));
    }

    private Goal get(Long id) {
        return goalRepository.findById(id).orElseThrow(() -> new NotFoundException("Goal " + id + " not found"));
    }
}
