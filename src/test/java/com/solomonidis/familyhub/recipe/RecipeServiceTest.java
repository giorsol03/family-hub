package com.solomonidis.familyhub.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.grocery.GroceryCategory;
import com.solomonidis.familyhub.grocery.QuantityUnit;
import com.solomonidis.familyhub.recipe.RecipeDtos.IngredientDto;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeRequest;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeResponse;
import com.solomonidis.familyhub.recipe.RecipeDtos.RecipeSummary;
import java.math.BigDecimal;
import java.util.List;
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
class RecipeServiceTest {

    @Autowired
    RecipeService service;

    @Test
    void createKeepsIngredientOrderAndNormalisesTags() {
        RecipeResponse r = service.create(pancakes(List.of(" Breakfast", "QUICK", "breakfast", " ")));

        assertThat(r.id()).isNotNull();
        assertThat(r.tags()).containsExactly("breakfast", "quick");
        assertThat(r.ingredients()).extracting(IngredientDto::name).containsExactly("Flour", "Milk", "Eggs");
        assertThat(r.ingredients().get(0).quantity()).isEqualByComparingTo("250");
        assertThat(r.ingredients().get(0).quantity().toPlainString()).isEqualTo("250");
        assertThat(r.ingredients().get(1).unit()).isEqualTo(QuantityUnit.ML);
        assertThat(r.ingredients().get(2).category()).isEqualTo(GroceryCategory.DAIRY);
        assertThat(r.createdBy().name()).isEqualTo("Admin");
    }

    @Test
    void updateReplacesIngredientsAndTags() {
        Long id = service.create(pancakes(List.of("breakfast"))).id();

        RecipeResponse updated = service.update(id, new RecipeRequest("Crêpes", "Thin", 2, 15, "Mix. Fry.", "",
            List.of("dessert"),
            List.of(new IngredientDto("Flour", new BigDecimal("125"), QuantityUnit.G, GroceryCategory.PANTRY))));

        assertThat(updated.title()).isEqualTo("Crêpes");
        assertThat(updated.imageUrl()).isNull();
        assertThat(updated.tags()).containsExactly("dessert");
        assertThat(updated.ingredients()).hasSize(1);
        assertThat(service.get(id).ingredients()).hasSize(1);
    }

    @Test
    void searchMatchesTitleDescriptionAndIngredientsCaseInsensitively() {
        service.create(pancakes(List.of("breakfast")));
        service.create(new RecipeRequest("Greek salad", "Fresh and quick", 2, 10, null, null, List.of("salad"),
            List.of(new IngredientDto("Feta", BigDecimal.ONE, QuantityUnit.PACK, GroceryCategory.DAIRY))));

        assertThat(titles(service.list("PANCAKE", null))).containsExactly("Pancakes");
        assertThat(titles(service.list("fresh", null))).containsExactly("Greek salad");
        assertThat(titles(service.list("feta", null))).containsExactly("Greek salad");
        assertThat(titles(service.list(null, "Breakfast"))).containsExactly("Pancakes");
        assertThat(titles(service.list(null, null))).containsExactly("Greek salad", "Pancakes");
        assertThat(service.list(null, null).get(1).ingredientCount()).isEqualTo(3);
        assertThat(service.tags()).containsExactly("breakfast", "salad");
    }

    @Test
    void deleteRemovesTheRecipe() {
        Long id = service.create(pancakes(List.of())).id();

        service.delete(id);

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }

    private static List<String> titles(List<RecipeSummary> recipes) {
        return recipes.stream().map(RecipeSummary::title).toList();
    }

    private static RecipeRequest pancakes(List<String> tags) {
        return new RecipeRequest("Pancakes", "Fluffy", 4, 20, "Mix everything.\nFry in a pan.",
            "https://example.com/pancakes.jpg", tags, List.of(
                new IngredientDto("Flour", new BigDecimal("250.000"), QuantityUnit.G, GroceryCategory.PANTRY),
                new IngredientDto("Milk", new BigDecimal("500"), QuantityUnit.ML, null),
                new IngredientDto("Eggs", new BigDecimal("2"), QuantityUnit.PCS, GroceryCategory.DAIRY)));
    }
}
