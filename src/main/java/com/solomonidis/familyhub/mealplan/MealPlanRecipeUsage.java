package com.solomonidis.familyhub.mealplan;

import com.solomonidis.familyhub.recipe.RecipeUsage;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A recipe planned for today or later can't be deleted (the message says when it's planned);
 * past plan entries are removed together with the recipe.
 * Kept separate from MealPlanService to avoid a RecipeService ↔ MealPlanService dependency cycle.
 */
@Component
public class MealPlanRecipeUsage implements RecipeUsage {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private final MealPlanRepository repository;
    private final Clock clock;

    public MealPlanRecipeUsage(MealPlanRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> blockingUsages(Long recipeId) {
        List<MealPlanEntry> upcoming =
            repository.findByRecipeIdAndDateGreaterThanEqualOrderByDateAsc(recipeId, LocalDate.now(clock));
        if (upcoming.isEmpty()) {
            return List.of();
        }
        MealPlanEntry first = upcoming.getFirst();
        String when = first.getDate().format(DAY) + " (" + first.getSlot().name().toLowerCase(Locale.ROOT) + ")";
        String more = upcoming.size() > 1 ? " and " + (upcoming.size() - 1) + " more" : "";
        return List.of("on the meal plan for " + when + more);
    }

    @Override
    @Transactional
    public void beforeRecipeDeleted(Long recipeId) {
        repository.deleteByRecipeId(recipeId);
    }
}
