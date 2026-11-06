package com.solomonidis.familyhub.mealplan;

import com.solomonidis.familyhub.grocery.GroceryDtos.GroceryItemResponse;
import com.solomonidis.familyhub.member.MemberSummary;
import com.solomonidis.familyhub.recipe.Recipe;
import com.solomonidis.familyhub.recipe.RecipeDtos.IngredientDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class MealPlanDtos {

    private MealPlanDtos() {
    }

    public record PlanRequest(@NotNull LocalDate date, @NotNull MealSlot slot, @NotNull Long recipeId) {
    }

    public record RecipeRef(Long id, String title, String imageUrl, Integer prepMinutes) {
        static RecipeRef of(Recipe r) {
            return new RecipeRef(r.getId(), r.getTitle(), r.getImageUrl(), r.getPrepMinutes());
        }
    }

    public record EntryResponse(Long id, LocalDate date, MealSlot slot, RecipeRef recipe, MemberSummary plannedBy) {
        public static EntryResponse of(MealPlanEntry e) {
            return new EntryResponse(e.getId(), e.getDate(), e.getSlot(), RecipeRef.of(e.getRecipe()),
                MemberSummary.of(e.getCreatedBy()));
        }
    }

    public record WeekResponse(LocalDate weekStart, LocalDate weekEnd, LocalDate today, List<EntryResponse> entries) {
    }

    /** Returned after planning a recipe: the entry plus the recipe's ingredients for the "add to groceries?" popup. */
    public record PlannedResponse(EntryResponse entry, List<IngredientDto> ingredients) {
    }

    /** Which ingredient lines (0-based, in recipe order) to put on the grocery list. */
    public record AddToGroceriesRequest(@NotNull @NotEmpty(message = "Pick at least one ingredient") List<Integer> ingredientIndexes) {
    }

    public record AddToGroceriesResponse(int added, int merged, List<GroceryItemResponse> items) {
    }
}
