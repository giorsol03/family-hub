package com.solomonidis.familyhub.mealplan;

import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.grocery.GroceryDtos.AddItemResponse;
import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemResponse;
import com.solomonidis.familyhub.grocery.GroceryService;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.AddToGroceriesResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.EntryResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlanRequest;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.PlannedResponse;
import com.solomonidis.familyhub.mealplan.MealPlanDtos.WeekResponse;
import com.solomonidis.familyhub.recipe.Recipe;
import com.solomonidis.familyhub.recipe.RecipeIngredient;
import com.solomonidis.familyhub.recipe.RecipeService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MealPlanService {

    private final MealPlanRepository repository;
    private final RecipeService recipeService;
    private final GroceryService groceryService;
    private final Clock clock;

    public MealPlanService(MealPlanRepository repository, RecipeService recipeService,
                           GroceryService groceryService, Clock clock) {
        this.repository = repository;
        this.recipeService = recipeService;
        this.groceryService = groceryService;
        this.clock = clock;
    }

    /** The Monday–Sunday week containing {@code anyDay} (default: this week, household timezone). */
    @Transactional(readOnly = true)
    public WeekResponse week(LocalDate anyDay) {
        LocalDate today = LocalDate.now(clock);
        LocalDate monday = (anyDay != null ? anyDay : today).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        return new WeekResponse(monday, sunday, today, between(monday, sunday));
    }

    /** Entries in a date range, ordered by day, then breakfast → dinner. Also used by the calendar. */
    @Transactional(readOnly = true)
    public List<EntryResponse> between(LocalDate from, LocalDate to) {
        return repository.findByDateBetweenOrderByDateAscIdAsc(from, to).stream()
            .sorted(Comparator.comparing(MealPlanEntry::getDate).thenComparing(MealPlanEntry::getSlot))
            .map(EntryResponse::of)
            .toList();
    }

    public PlannedResponse plan(PlanRequest request) {
        Recipe recipe = recipeService.find(request.recipeId());
        MealPlanEntry entry = repository.save(new MealPlanEntry(request.date(), request.slot(), recipe));
        return new PlannedResponse(EntryResponse.of(entry), RecipeService.ingredientsOf(recipe));
    }

    public void remove(Long id) {
        repository.delete(get(id));
    }

    /** The planned recipe's ingredients (to re-open the "add to groceries?" popup later). */
    @Transactional(readOnly = true)
    public PlannedResponse plannedWithIngredients(Long id) {
        MealPlanEntry entry = get(id);
        return new PlannedResponse(EntryResponse.of(entry), RecipeService.ingredientsOf(entry.getRecipe()));
    }

    /**
     * Puts the selected ingredients of the planned recipe on the grocery list (source RECIPE),
     * using the grocery merging rule. Indexes refer to the recipe's ingredient order.
     */
    public AddToGroceriesResponse addToGroceries(Long id, List<Integer> indexes) {
        MealPlanEntry entry = get(id);
        Recipe recipe = entry.getRecipe();
        List<RecipeIngredient> ingredients = recipe.getIngredients();
        for (Integer index : indexes) {
            if (index == null || index < 0 || index >= ingredients.size()) {
                throw new BusinessRuleException("ingredientIndexes", "Unknown ingredient: " + index);
            }
        }
        String note = "For " + recipe.getTitle();
        int merged = 0;
        List<GroceryItemResponse> items = new ArrayList<>();
        for (Integer index : new LinkedHashSet<>(indexes)) {
            RecipeIngredient ing = ingredients.get(index);
            AddItemResponse result = groceryService.addIngredient(
                ing.getName(), ing.getQuantity(), ing.getUnit(), ing.getCategory(), note);
            if (result.merged()) {
                merged++;
            }
            items.add(result.item());
        }
        return new AddToGroceriesResponse(items.size() - merged, merged, items);
    }

    private MealPlanEntry get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Meal plan entry " + id + " not found"));
    }
}
