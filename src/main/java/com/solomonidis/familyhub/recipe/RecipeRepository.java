package com.solomonidis.familyhub.recipe;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findAllByOrderByTitleAsc();

    @Query("select distinct t from Recipe r join r.tags t order by t")
    List<String> findAllTags();
}
