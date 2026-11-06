package com.solomonidis.familyhub.mealplan;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import com.solomonidis.familyhub.recipe.Recipe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A recipe planned for a day and meal. "Planned by" is {@link #getCreatedBy()}. */
@Entity
@Table(name = "meal_plan_entry")
public class MealPlanEntry extends AuditableEntity {

    @Column(name = "plan_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private MealSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    protected MealPlanEntry() {
        // for JPA
    }

    public MealPlanEntry(LocalDate date, MealSlot slot, Recipe recipe) {
        this.date = date;
        this.slot = slot;
        this.recipe = recipe;
    }

    public LocalDate getDate() {
        return date;
    }

    public MealSlot getSlot() {
        return slot;
    }

    public Recipe getRecipe() {
        return recipe;
    }
}
