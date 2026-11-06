package com.solomonidis.familyhub.recipe;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class RecipeApiTest {

    private static final String SALAD = """
        {"title":"Greek salad","servings":2,"prepMinutes":10,"instructions":"Chop.\\nMix.",
         "tags":["Salad","quick"],
         "ingredients":[
           {"name":"Tomatoes","quantity":0.5,"unit":"KG","category":"FRUIT_VEG"},
           {"name":"Feta","quantity":1,"unit":"PACK"}]}""";

    @Autowired
    MockMvc mvc;

    @Test
    void crud() throws Exception {
        String body = mvc.perform(json(post("/api/recipes"), SALAD))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tags[0]").value("salad"))
            .andExpect(jsonPath("$.ingredients[0].quantity").value(0.5))
            .andExpect(jsonPath("$.ingredients[1].category").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(get("/api/recipes").param("tag", "quick"))
            .andExpect(jsonPath("$[0].title").value("Greek salad"))
            .andExpect(jsonPath("$[0].ingredientCount").value(2))
            .andExpect(jsonPath("$[0].instructions").doesNotExist());

        mvc.perform(get("/api/recipes/tags"))
            .andExpect(jsonPath("$").value(org.hamcrest.Matchers.contains("quick", "salad")));

        mvc.perform(json(put("/api/recipes/{id}", id), SALAD.replace("Greek salad", "Village salad")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Village salad"));

        mvc.perform(delete("/api/recipes/{id}", id).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void validationReportsNestedIngredientFields() throws Exception {
        mvc.perform(json(post("/api/recipes"), """
                {"title":"","servings":0,"imageUrl":"not a url",
                 "ingredients":[{"name":"","quantity":0,"unit":"G"}]}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists())
            .andExpect(jsonPath("$.fieldErrors.servings").exists())
            .andExpect(jsonPath("$.fieldErrors.imageUrl").exists())
            .andExpect(jsonPath("$.fieldErrors['ingredients[0].name']").exists())
            .andExpect(jsonPath("$.fieldErrors['ingredients[0].quantity']").value("Must be more than 0"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
