package com.solomonidis.familyhub.mealplan;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithUserDetails("admin")
class MealPlanApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void planAddIngredientsAndRemove() throws Exception {
        long recipeId = id(mvc.perform(json(post("/api/recipes"), """
                {"title":"Omelette","ingredients":[
                  {"name":"Eggs","quantity":3,"unit":"PCS","category":"DAIRY"},
                  {"name":"Cheese","quantity":100,"unit":"G","category":"DAIRY"}]}"""))
            .andReturn().getResponse().getContentAsString(), "$.id");

        String planned = mvc.perform(json(post("/api/meal-plan"), """
                {"date":"2026-11-10","slot":"BREAKFAST","recipeId":%d}""".formatted(recipeId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.entry.slot").value("BREAKFAST"))
            .andExpect(jsonPath("$.entry.recipe.title").value("Omelette"))
            .andExpect(jsonPath("$.ingredients.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        long entryId = id(planned, "$.entry.id");

        mvc.perform(get("/api/meal-plan").param("weekOf", "2026-11-12"))
            .andExpect(jsonPath("$.weekStart").value("2026-11-09"))
            .andExpect(jsonPath("$.weekEnd").value("2026-11-15"))
            .andExpect(jsonPath("$.entries[0].id").value(entryId));

        mvc.perform(json(post("/api/meal-plan/{id}/add-to-groceries", entryId), """
                {"ingredientIndexes":[1]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.added").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Cheese"))
            .andExpect(jsonPath("$.items[0].source").value("RECIPE"));

        mvc.perform(json(post("/api/meal-plan/{id}/add-to-groceries", entryId), """
                {"ingredientIndexes":[]}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.ingredientIndexes").exists());

        mvc.perform(delete("/api/meal-plan/{id}", entryId).with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void deletingAPlannedRecipeIsAConflict() throws Exception {
        long recipeId = id(mvc.perform(json(post("/api/recipes"), """
                {"title":"Soup"}""")).andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(json(post("/api/meal-plan"), """
                {"date":"2099-01-01","slot":"DINNER","recipeId":%d}""".formatted(recipeId)));

        mvc.perform(delete("/api/recipes/{id}", recipeId).with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("“Soup” is on the meal plan for Thu 1 Jan (dinner). Remove it from there first."));
    }

    private static long id(String body, String path) {
        return ((Number) JsonPath.read(body, path)).longValue();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
