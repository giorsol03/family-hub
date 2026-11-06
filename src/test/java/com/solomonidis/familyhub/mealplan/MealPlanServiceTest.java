package com.solomonidis.familyhub.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.grocery.GroceryCategory;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemRequest;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemResponse;
import com.solomonidis.familyhub.grocery.GroceryService;
import com.solomonidis.familyhub.grocery.GrocerySource;
import com.solomonidis.familyhub.grocery.QuantityUnit;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.AddToGroceriesResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.EntryResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlanRequest;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlannedResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.WeekResponse;
import com.solomonidis.familyhub.recipe.RecipeDtos.IngredientDto;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeRequest;
import com.solomonidis.familyhub.recipe.RecipeService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
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
class MealPlanServiceTest {

    @Autowired
    MealPlanService service;

    @Autowired
    RecipeService recipeService;

    @Autowired
    GroceryService groceryService;

    LocalDate today;
    Long pancakes;

    @BeforeEach
    void setUp() {
        today = service.week(null).today();
        pancakes = recipeService.create(new RecipeRequest("Pancakes", null, 4, 20, null, null, List.of(), List.of(
            new IngredientDto("Flour", new BigDecimal("500"), QuantityUnit.G, GroceryCategory.PANTRY),
            new IngredientDto("Milk", new BigDecimal("0.5"), QuantityUnit.L, null),
            new IngredientDto("Onion", new BigDecimal("0.5"), QuantityUnit.PCS, GroceryCategory.FRUIT_VEG)))).id();
    }

    @Test
    void weekRunsMondayToSundayAndOrdersBySlot() {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        service.plan(new PlanRequest(monday.plusDays(1), MealSlot.DINNER, pancakes));
        service.plan(new PlanRequest(monday.plusDays(1), MealSlot.BREAKFAST, pancakes));
        service.plan(new PlanRequest(monday.plusDays(7), MealSlot.LUNCH, pancakes));

        WeekResponse week = service.week(today);

        assertThat(week.weekStart()).isEqualTo(monday);
        assertThat(week.weekEnd()).isEqualTo(monday.plusDays(6));
        assertThat(week.entries()).extracting(EntryResponse::slot).containsExactly(MealSlot.BREAKFAST, MealSlot.DINNER);
        assertThat(week.entries().getFirst().recipe().title()).isEqualTo("Pancakes");
        assertThat(week.entries().getFirst().plannedBy().name()).isEqualTo("Admin");
        assertThat(service.week(monday.plusDays(9)).entries()).hasSize(1);
    }

    @Test
    void planningReturnsTheIngredientsForThePopup() {
        PlannedResponse planned = service.plan(new PlanRequest(today, MealSlot.DINNER, pancakes));

        assertThat(planned.entry().id()).isNotNull();
        assertThat(planned.ingredients()).extracting(IngredientDto::name).containsExactly("Flour", "Milk", "Onion");
    }

    @Test
    void addSelectedIngredientsMergesRoundsPiecesAndBorrowsFavouriteCategory() {
        groceryService.add(new GroceryItemRequest("flour", GroceryCategory.PANTRY, new BigDecimal("1"), QuantityUnit.KG,
            null, null), GrocerySource.MANUAL);
        Long entry = service.plan(new PlanRequest(today, MealSlot.DINNER, pancakes)).entry().id();

        AddToGroceriesResponse result = service.addToGroceries(entry, List.of(0, 1, 2));

        assertThat(result.added()).isEqualTo(2);
        assertThat(result.merged()).isEqualTo(1);
        GroceryItemResponse flour = find(result, "flour");
        assertThat(flour.quantity()).isEqualByComparingTo("1.5");
        assertThat(flour.unit()).isEqualTo(QuantityUnit.KG);
        assertThat(flour.notes()).isEqualTo("For Pancakes");

        GroceryItemResponse milk = find(result, "Milk");
        assertThat(milk.source()).isEqualTo(GrocerySource.RECIPE);
        assertThat(milk.category()).isEqualTo(GroceryCategory.DAIRY); // from the seeded "Milk" favourite

        GroceryItemResponse onion = find(result, "Onion");
        assertThat(onion.quantity()).isEqualByComparingTo("1"); // 0.5 pcs rounded up
    }

    @Test
    void onlySelectedIngredientsAreAdded() {
        Long entry = service.plan(new PlanRequest(today, MealSlot.DINNER, pancakes)).entry().id();

        AddToGroceriesResponse result = service.addToGroceries(entry, List.of(1, 1));

        assertThat(result.items()).extracting(GroceryItemResponse::name).containsExactly("Milk");
        assertThat(groceryService.list()).hasSize(1);
    }

    @Test
    void favouriteShopIsUsedForIngredientsWithoutCategory() {
        Long tomatoes = recipeService.create(new RecipeRequest("Salad", null, null, null, null, null, List.of(), List.of(
            new IngredientDto("tomatoes", new BigDecimal("300"), QuantityUnit.G, null)))).id();
        Long entry = service.plan(new PlanRequest(today, MealSlot.LUNCH, tomatoes)).entry().id();

        GroceryItemResponse item = service.addToGroceries(entry, List.of(0)).items().getFirst();

        assertThat(item.category()).isEqualTo(GroceryCategory.FRUIT_VEG);
        assertThat(item.merchant()).isNull(); // seed favourites have no preferred shop
    }

    @Test
    void unknownIngredientIndexIsRejected() {
        Long entry = service.plan(new PlanRequest(today, MealSlot.DINNER, pancakes)).entry().id();

        assertThatThrownBy(() -> service.addToGroceries(entry, List.of(7)))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void removeEntry() {
        Long entry = service.plan(new PlanRequest(today, MealSlot.DINNER, pancakes)).entry().id();

        service.remove(entry);

        assertThat(service.week(today).entries()).isEmpty();
        assertThatThrownBy(() -> service.remove(entry)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void plannedRecipesCannotBeDeletedButPastPlansDoNotBlock() {
        service.plan(new PlanRequest(today.plusDays(2), MealSlot.DINNER, pancakes));

        assertThatThrownBy(() -> recipeService.delete(pancakes))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("on the meal plan for");

        Long other = recipeService.create(new RecipeRequest("Soup", null, null, null, null, null, List.of(), List.of())).id();
        service.plan(new PlanRequest(today.minusDays(3), MealSlot.LUNCH, other));
        recipeService.delete(other);
        assertThat(service.week(today.minusDays(3)).entries()).noneMatch(e -> e.recipe().id().equals(other));
    }

    private static GroceryItemResponse find(AddToGroceriesResponse result, String name) {
        return result.items().stream().filter(i -> i.name().equals(name)).findFirst().orElseThrow();
    }
}
