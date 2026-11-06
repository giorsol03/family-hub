package com.solomonidis.familyhub.mealplan;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealPlanRepository extends JpaRepository<MealPlanEntry, Long> {

    @EntityGraph(attributePaths = {"recipe", "createdBy"})
    List<MealPlanEntry> findByDateBetweenOrderByDateAscIdAsc(LocalDate from, LocalDate to);

    List<MealPlanEntry> findByRecipeIdAndDateGreaterThanEqualOrderByDateAsc(Long recipeId, LocalDate date);

    /** Derived delete: loads and removes each entry, so the persistence context stays consistent. */
    void deleteByRecipeId(Long recipeId);
}
