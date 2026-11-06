package com.solomonidis.familyhub;

import static org.assertj.core.api.Assertions.assertThat;

import com.solomonidis.familyhub.calendar.CalendarDtos.EventRequest;
import com.solomonidis.familyhub.calendar.CalendarService;
import com.solomonidis.familyhub.calendar.EventType;
import com.solomonidis.familyhub.chore.ChoreDtos.View;
import com.solomonidis.familyhub.chore.ChoreService;
import com.solomonidis.familyhub.chore.ChoreTemplateService;
import com.solomonidis.familyhub.goal.GoalDtos.GoalRequest;
import com.solomonidis.familyhub.goal.GoalService;
import com.solomonidis.familyhub.grocery.GroceryCategory;
import com.solomonidis.familyhub.grocery.GroceryDefaultService;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemRequest;
import com.solomonidis.familyhub.grocery.GroceryService;
import com.solomonidis.familyhub.grocery.GrocerySource;
import com.solomonidis.familyhub.grocery.QuantityUnit;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlanRequest;
import com.solomonidis.familyhub.mealplan.MealPlanService;
import com.solomonidis.familyhub.mealplan.MealSlot;
import com.solomonidis.familyhub.member.MemberRepository;
import com.solomonidis.familyhub.recipe.RecipeDtos.IngredientDto;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeRequest;
import com.solomonidis.familyhub.recipe.RecipeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Runs every Flyway migration against a real PostgreSQL (the same major version Neon uses), lets Hibernate
 * validate the schema against the entities, then exercises each feature's custom queries and a few writes.
 * H2's PostgreSQL mode is close but not identical, so this is the check that the SQL really works in prod.
 * <p>
 * Needs Docker; skipped (not failed) on machines without it.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
@WithUserDetails("admin")
class PostgresMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired Flyway flyway;
    @Autowired MemberRepository members;
    @Autowired GroceryService groceries;
    @Autowired GroceryDefaultService groceryDefaults;
    @Autowired ChoreService chores;
    @Autowired ChoreTemplateService choreTemplates;
    @Autowired RecipeService recipes;
    @Autowired MealPlanService mealPlan;
    @Autowired CalendarService calendar;
    @Autowired GoalService goals;

    @Test
    void allMigrationsApplyOnPostgres() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(flyway.getConfiguration().getDataSource().getClass().getName()).contains("Hikari");
        assertThat(members.findByUsername("admin")).isPresent();
    }

    @Test
    void seedDataIsThere() {
        assertThat(groceryDefaults.list(false)).hasSize(9);
        assertThat(choreTemplates.list(false)).hasSize(8);
    }

    @Test
    void groceryMergingWorksOnPostgres() {
        groceries.add(new GroceryItemRequest("Flour", GroceryCategory.PANTRY, new BigDecimal("500"), QuantityUnit.G, null, null),
            GrocerySource.MANUAL);
        var merged = groceries.add(new GroceryItemRequest("flour", GroceryCategory.PANTRY, BigDecimal.ONE, QuantityUnit.KG, null, null),
            GrocerySource.MANUAL);

        assertThat(merged.merged()).isTrue();
        assertThat(merged.item().quantity()).isEqualByComparingTo("1.5");
        assertThat(merged.item().unit()).isEqualTo(QuantityUnit.KG);
        assertThat(groceries.list()).hasSize(1);
    }

    @Test
    void choreViewsRun() {
        for (View view : View.values()) {
            assertThat(chores.list(view, null)).isNotNull();
        }
    }

    @Test
    void recipesMealPlanCalendarAndGoalsQueriesRun() {
        LocalDate today = goals.today();
        var recipe = recipes.create(new RecipeRequest("Pasta al pomodoro", null, 4, 20, "Boil, mix.", null,
            List.of("Quick", "Vegetarian"),
            List.of(new IngredientDto("Spaghetti", new BigDecimal("500"), QuantityUnit.G, GroceryCategory.PANTRY))));

        assertThat(recipes.list("pomo", null)).hasSize(1);
        assertThat(recipes.list(null, "Quick")).hasSize(1);
        assertThat(recipes.tags()).contains("quick", "vegetarian"); // tags are normalised to lower case

        mealPlan.plan(new PlanRequest(today, MealSlot.DINNER, recipe.id()));
        assertThat(mealPlan.week(today)).isNotNull();

        Long adminId = members.findByUsername("admin").orElseThrow().getId();
        calendar.create(new EventRequest("Dinner out", null, null, EventType.NIGHT_OUT, false,
            today.atTime(20, 0), today.atTime(23, 0), List.of(adminId)));
        var items = calendar.items(today.minusDays(3), today.plusDays(3), adminId).items();
        assertThat(items).hasSize(2); // the event + the planned dinner

        var goal = goals.create(new GoalRequest("Yoga", adminId, today, 3));
        goals.setCheck(goal.id(), today, true);
        goals.setCheck(goal.id(), today, true); // idempotent against the unique (goal_id, check_date)
        var row = goals.month(YearMonth.from(today), adminId).rows().getFirst();
        assertThat(row.doneDates()).containsExactly(today);
        assertThat(row.stats().streak()).isEqualTo(1);
    }
}
