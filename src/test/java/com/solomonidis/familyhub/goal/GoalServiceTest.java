package com.solomonidis.familyhub.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.goal.GoalDtos.GoalRequest;
import com.solomonidis.familyhub.goal.GoalDtos.GoalResponse;
import com.solomonidis.familyhub.goal.GoalDtos.GoalRow;
import com.solomonidis.familyhub.goal.GoalDtos.MonthResponse;
import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithUserDetails("admin")
class GoalServiceTest {

    @Autowired
    GoalService service;

    @Autowired
    MemberRepository memberRepository;

    LocalDate today;
    YearMonth thisMonth;
    Member admin;
    Member maria;

    @BeforeEach
    void setUp() {
        today = service.today();
        thisMonth = YearMonth.from(today);
        admin = memberRepository.findByUsername("admin").orElseThrow();
        maria = memberRepository.save(new Member("Maria", "maria", "{noop}x", "#ff0000"));
    }

    @Test
    void createDefaultsToStartingToday() {
        GoalResponse goal = service.create(new GoalRequest(" Yoga ", maria.getId(), null, 3));

        assertThat(goal.name()).isEqualTo("Yoga");
        assertThat(goal.startDate()).isEqualTo(today);
        assertThat(goal.owner().name()).isEqualTo("Maria");
        assertThat(goal.targetDaysPerWeek()).isEqualTo(3);
        assertThat(goal.active()).isTrue();
    }

    @Test
    void tickingAndUntickingUpdatesTheRowAndStats() {
        Long yoga = goal("Yoga", maria, today.minusDays(5)).id();

        service.setCheck(yoga, today.minusDays(1), true);
        GoalRow row = service.setCheck(yoga, today, true);
        assertThat(row.stats().streak()).isEqualTo(2);
        assertThat(row.doneDates()).contains(today);

        GoalRow unticked = service.setCheck(yoga, today, false);
        assertThat(unticked.doneDates()).doesNotContain(today);
        assertThat(unticked.stats().streak()).isEqualTo(1); // yesterday still counts until today is over

        service.setCheck(yoga, today, true); // re-ticking reuses the same row
        assertThat(service.month(thisMonth, null).rows().getFirst().doneDates()).contains(today);
    }

    @Test
    void futureDaysAndDaysBeforeTheStartCannotBeTicked() {
        Long gym = goal("Gym", admin, today).id();

        assertThatThrownBy(() -> service.setCheck(gym, today.plusDays(1), true))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("hasn't happened");
        assertThatThrownBy(() -> service.setCheck(gym, today.minusDays(1), true))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("started on");
    }

    @Test
    void monthIsOrderedByOwnerThenGoalAndCanBeFiltered() {
        goal("Yoga", maria, today);
        goal("No caffeine", admin, today);
        goal("Gym", admin, today);

        MonthResponse all = service.month(thisMonth, null);
        assertThat(all.rows()).extracting(r -> r.goal().owner().name() + ":" + r.goal().name())
            .containsExactly("Admin:Gym", "Admin:No caffeine", "Maria:Yoga");
        assertThat(all.daysInMonth()).isEqualTo(thisMonth.lengthOfMonth());
        assertThat(all.today()).isEqualTo(today);

        assertThat(service.month(thisMonth, maria.getId()).rows()).extracting(r -> r.goal().name())
            .containsExactly("Yoga");
    }

    @Test
    void goalsAppearFromTheirStartMonth() {
        goal("Future habit", admin, thisMonth.plusMonths(1).atDay(1));

        assertThat(service.month(thisMonth, null).rows()).isEmpty();
        assertThat(service.month(thisMonth.plusMonths(1), null).rows()).hasSize(1);
    }

    @Test
    void removedGoalsDisappearUnlessTheyHaveHistoryThatMonth() {
        Long withHistory = goal("Eat healthy", admin, today).id();
        Long without = goal("Run", admin, today).id();
        service.setCheck(withHistory, today, true);

        service.setActive(withHistory, false);
        service.setActive(without, false);

        assertThat(service.month(thisMonth, null).rows()).extracting(r -> r.goal().name()).containsExactly("Eat healthy");
        assertThat(service.month(thisMonth.plusMonths(1), null).rows()).isEmpty();
        assertThat(service.list(false)).isEmpty();
        assertThat(service.list(true)).hasSize(2);

        service.setActive(without, true);
        assertThat(service.list(false)).extracting(GoalResponse::name).containsExactly("Run");
    }

    @Test
    void unknownOwnerIsAFieldError() {
        assertThatThrownBy(() -> service.create(new GoalRequest("X", 999_999L, null, null)))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("field").isEqualTo("ownerId");
    }

    private GoalResponse goal(String name, Member owner, LocalDate start) {
        return service.create(new GoalRequest(name, owner.getId(), start, null));
    }
}
