package com.solomonidis.familyhub.recipe;

import java.util.List;

/**
 * Implemented by features that reference recipes (e.g. the meal plan), so the recipe package
 * doesn't depend on them. RecipeService consults every implementation before deleting a recipe.
 */
public interface RecipeUsage {

    /** Human-readable reasons the recipe can't be deleted right now; empty when deletion is fine. */
    List<String> blockingUsages(Long recipeId);

    /** Remove any remaining (non-blocking) references just before the recipe is deleted. */
    void beforeRecipeDeleted(Long recipeId);
}
