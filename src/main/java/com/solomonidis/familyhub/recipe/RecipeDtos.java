package com.solomonidis.familyhub.recipe;

import com.solomonidis.familyhub.grocery.GroceryCategory;
import com.solomonidis.familyhub.grocery.QuantityUnit;
import com.solomonidis.familyhub.member.MemberSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class RecipeDtos {

    private RecipeDtos() {
    }

    public record IngredientDto(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin(value = "0.001", message = "Must be more than 0")
        @Digits(integer = 7, fraction = 3, message = "Too many digits") BigDecimal quantity,
        @NotNull QuantityUnit unit,
        GroceryCategory category
    ) {
        static IngredientDto of(RecipeIngredient i) {
            BigDecimal q = i.getQuantity().stripTrailingZeros();
            return new IngredientDto(i.getName(), q.scale() < 0 ? q.setScale(0) : q, i.getUnit(), i.getCategory());
        }
    }

    public record RecipeRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @Min(value = 1, message = "At least 1") @Max(value = 100, message = "At most 100") Integer servings,
        @Min(value = 0, message = "Can't be negative") @Max(value = 1440, message = "At most 24 hours") Integer prepMinutes,
        @Size(max = 10000) String instructions,
        @Size(max = 500) @Pattern(regexp = "^$|https?://\\S+", message = "Must be a web address starting with http(s)://")
        String imageUrl,
        @Size(max = 20, message = "At most 20 tags") List<@NotBlank @Size(max = 40) String> tags,
        @Size(max = 100, message = "At most 100 ingredients") List<@Valid IngredientDto> ingredients
    ) {
    }

    public record RecipeResponse(
        Long id,
        String title,
        String description,
        Integer servings,
        Integer prepMinutes,
        String instructions,
        String imageUrl,
        List<String> tags,
        List<IngredientDto> ingredients,
        MemberSummary createdBy,
        Instant createdAt
    ) {
        static RecipeResponse of(Recipe r) {
            return new RecipeResponse(r.getId(), r.getTitle(), r.getDescription(), r.getServings(), r.getPrepMinutes(),
                r.getInstructions(), r.getImageUrl(), List.copyOf(r.getTags()),
                r.getIngredients().stream().map(IngredientDto::of).toList(),
                MemberSummary.of(r.getCreatedBy()), r.getCreatedAt());
        }
    }

    /** List-page card: no instructions or ingredient details. */
    public record RecipeSummary(
        Long id,
        String title,
        String description,
        Integer servings,
        Integer prepMinutes,
        String imageUrl,
        List<String> tags,
        int ingredientCount
    ) {
        static RecipeSummary of(Recipe r) {
            return new RecipeSummary(r.getId(), r.getTitle(), r.getDescription(), r.getServings(), r.getPrepMinutes(),
                r.getImageUrl(), List.copyOf(r.getTags()), r.getIngredients().size());
        }
    }
}
