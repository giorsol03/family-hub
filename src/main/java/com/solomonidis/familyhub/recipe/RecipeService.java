package com.solomonidis.familyhub.recipe;

import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import org.springframework.http.HttpStatus;
import com.solomonidis.familyhub.recipe.RecipeDtos.IngredientDto;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeRequest;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeResponse;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeSummary;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecipeService {

    private final RecipeRepository repository;
    private final List<RecipeUsage> usages;

    public RecipeService(RecipeRepository repository, List<RecipeUsage> usages) {
        this.repository = repository;
        this.usages = usages;
    }

    /**
     * All recipes, optionally filtered by a search text (title, description or ingredient, case-insensitive)
     * and a tag. Filtering happens in memory — a household has tens of recipes, not thousands.
     */
    @Transactional(readOnly = true)
    public List<RecipeSummary> list(String query, String tag) {
        String q = query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
        String t = tag == null || tag.isBlank() ? null : normalizeTag(tag);
        return repository.findAllByOrderByTitleAsc().stream()
            .filter(r -> t == null || r.getTags().contains(t))
            .filter(r -> q == null || matches(r, q))
            .map(RecipeSummary::of)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> tags() {
        return repository.findAllTags();
    }

    @Transactional(readOnly = true)
    public RecipeResponse get(Long id) {
        return RecipeResponse.of(find(id));
    }

    /** For other features (e.g. the meal planner) that need the entity itself. */
    @Transactional(readOnly = true)
    public Recipe find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Recipe " + id + " not found"));
    }

    public RecipeResponse create(RecipeRequest request) {
        Recipe recipe = new Recipe(request.title().trim());
        apply(recipe, request);
        return RecipeResponse.of(repository.save(recipe));
    }

    public RecipeResponse update(Long id, RecipeRequest request) {
        Recipe recipe = find(id);
        recipe.setTitle(request.title().trim());
        apply(recipe, request);
        return RecipeResponse.of(recipe);
    }

    /** Refused (409) while the recipe is still planned; other references are cleaned up first. */
    public void delete(Long id) {
        Recipe recipe = find(id);
        List<String> blocking = usages.stream().flatMap(u -> u.blockingUsages(id).stream()).toList();
        if (!blocking.isEmpty()) {
            throw new BusinessRuleException(null, "“" + recipe.getTitle() + "” is " + String.join(", ", blocking)
                + ". Remove it from there first.", HttpStatus.CONFLICT);
        }
        usages.forEach(u -> u.beforeRecipeDeleted(id));
        repository.delete(recipe);
    }

    private void apply(Recipe recipe, RecipeRequest request) {
        recipe.setDescription(blankToNull(request.description()));
        recipe.setServings(request.servings());
        recipe.setPrepMinutes(request.prepMinutes());
        recipe.setInstructions(blankToNull(request.instructions()));
        recipe.setImageUrl(blankToNull(request.imageUrl()));
        recipe.replaceTags(normalizeTags(request.tags()));
        recipe.replaceIngredients(request.ingredients() == null ? List.of() : request.ingredients().stream()
            .map(i -> new RecipeIngredient(i.name().trim(), i.quantity(), i.unit(), i.category()))
            .toList());
    }

    private static boolean matches(Recipe r, String q) {
        return contains(r.getTitle(), q)
            || contains(r.getDescription(), q)
            || r.getIngredients().stream().anyMatch(i -> contains(i.getName(), q));
    }

    private static boolean contains(String text, String q) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(q);
    }

    /** Lower-case, trimmed, de-duplicated, in the order given. */
    static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(tags.stream()
            .filter(t -> t != null && !t.isBlank())
            .map(RecipeService::normalizeTag)
            .toList()));
    }

    private static String normalizeTag(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Exposed for the meal planner: ingredient lines as DTOs. */
    public static List<IngredientDto> ingredientsOf(Recipe recipe) {
        return recipe.getIngredients().stream().map(IngredientDto::of).toList();
    }
}
