package com.solomonidis.familyhub.grocery;

import static org.hamcrest.Matchers.hasItem;
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
class GroceryApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void addThenMergeThroughTheApi() throws Exception {
        mvc.perform(json(post("/api/groceries/items"), """
                {"name":"Flour","category":"PANTRY","quantity":500,"unit":"G"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.merged").value(false))
            .andExpect(jsonPath("$.item.quantity").value(500))
            .andExpect(jsonPath("$.item.addedBy.name").value("Admin"))
            .andExpect(jsonPath("$.item.source").value("MANUAL"));

        mvc.perform(json(post("/api/groceries/items"), """
                {"name":"FLOUR","category":"PANTRY","quantity":1,"unit":"KG"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.merged").value(true))
            .andExpect(jsonPath("$.item.quantity").value(1.5))
            .andExpect(jsonPath("$.item.unit").value("KG"));

        mvc.perform(get("/api/groceries/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void validationErrorsAreReportedPerField() throws Exception {
        mvc.perform(json(post("/api/groceries/items"), """
                {"name":"","category":null,"quantity":0,"unit":"KG"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.fieldErrors.category").exists())
            .andExpect(jsonPath("$.fieldErrors.quantity").value("Must be more than 0"));
    }

    @Test
    void fractionalPiecesAreRejectedOnTheQuantityField() throws Exception {
        mvc.perform(json(post("/api/groceries/items"), """
                {"name":"Bread","category":"BAKERY","quantity":1.5,"unit":"PCS"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void tickAndClearBought() throws Exception {
        long id = addItem("Bread", "BAKERY", "1", "PCS");

        mvc.perform(json(put("/api/groceries/items/{id}/bought", id), """
                {"bought":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bought").value(true))
            .andExpect(jsonPath("$.boughtAt").isNotEmpty());

        mvc.perform(post("/api/groceries/items/clear-bought").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.removed").value(1));
    }

    @Test
    void editAndDeleteItem() throws Exception {
        long id = addItem("Bread", "BAKERY", "1", "PCS");

        mvc.perform(json(put("/api/groceries/items/{id}", id), """
                {"name":"Pitta bread","category":"BAKERY","quantity":2,"unit":"PACK","merchant":"LIDL","notes":"wholemeal"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pitta bread"))
            .andExpect(jsonPath("$.merchant").value("LIDL"))
            .andExpect(jsonPath("$.notes").value("wholemeal"));

        mvc.perform(delete("/api/groceries/items/{id}", id).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/groceries/items/{id}", id).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void defaultsLifecycle() throws Exception {
        mvc.perform(get("/api/groceries/defaults"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", hasItem("Toilet paper")));

        String body = mvc.perform(json(post("/api/groceries/defaults"), """
                {"name":"Halloumi","category":"DAIRY","defaultQuantity":2,"unit":"PACK","preferredMerchant":"ALPHAMEGA"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(post("/api/groceries/items/from-default/{id}", id).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.item.name").value("Halloumi"))
            .andExpect(jsonPath("$.item.merchant").value("ALPHAMEGA"))
            .andExpect(jsonPath("$.item.source").value("DEFAULT"));

        mvc.perform(post("/api/groceries/defaults/{id}/deactivate", id).with(csrf()))
            .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/groceries/defaults?includeInactive=true"))
            .andExpect(jsonPath("$[*].name", hasItem("Halloumi")));
        mvc.perform(post("/api/groceries/defaults/{id}/activate", id).with(csrf()))
            .andExpect(jsonPath("$.active").value(true));
    }

    private long addItem(String name, String category, String quantity, String unit) throws Exception {
        String body = mvc.perform(json(post("/api/groceries/items"), """
                {"name":"%s","category":"%s","quantity":%s,"unit":"%s"}""".formatted(name, category, quantity, unit)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.item.id")).longValue();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
