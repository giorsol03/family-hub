package com.solomonidis.familyhub.recipe;

import com.solomonidis.familyhub.grocery.GroceryCategory;
import com.solomonidis.familyhub.grocery.QuantityUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One ingredient line of a recipe. {@code category} is optional and is used when the
 * ingredient is added to the grocery list (meal planner).
 */
@Embeddable
public class RecipeIngredient {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private QuantityUnit unit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private GroceryCategory category;

    protected RecipeIngredient() {
        // for JPA
    }

    public RecipeIngredient(String name, BigDecimal quantity, QuantityUnit unit, GroceryCategory category) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public QuantityUnit getUnit() {
        return unit;
    }

    public GroceryCategory getCategory() {
        return category;
    }
}
