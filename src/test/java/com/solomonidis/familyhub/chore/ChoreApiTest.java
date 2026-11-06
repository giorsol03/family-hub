package com.solomonidis.familyhub.chore;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
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
class ChoreApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ChoreService service;

    @Test
    void createListCompleteAndReopen() throws Exception {
        String today = service.today().toString();
        long id = idOf(mvc.perform(json(post("/api/chores"), """
                {"title":"Fix shelf","scheduledDate":"%s"}""".formatted(today)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.recurrence").value("NONE"))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/chores").param("view", "TODAY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.today").value(today))
            .andExpect(jsonPath("$.chores[*].title", hasItem("Fix shelf")));

        mvc.perform(post("/api/chores/{id}/complete", id).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chore.status").value("DONE"))
            .andExpect(jsonPath("$.chore.completedBy.name").value("Admin"))
            .andExpect(jsonPath("$.next").doesNotExist());

        mvc.perform(post("/api/chores/{id}/complete", id).with(csrf()))
            .andExpect(status().isBadRequest());

        mvc.perform(post("/api/chores/{id}/reopen", id).with(csrf()))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.completedBy").doesNotExist());
    }

    @Test
    void fromTemplateAndRecurrence() throws Exception {
        long templateId = idOf(mvc.perform(json(post("/api/chores/templates"), """
                {"name":"Water the herbs","recurrence":"EVERY_N_DAYS","recurrenceInterval":2,"estimatedMinutes":5}"""))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");

        long choreId = idOf(mvc.perform(post("/api/chores/from-template/{id}", templateId).with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Water the herbs"))
            .andExpect(jsonPath("$.estimatedMinutes").value(5))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/chores/{id}/skip", choreId).with(csrf()))
            .andExpect(jsonPath("$.chore.status").value("SKIPPED"))
            .andExpect(jsonPath("$.next.scheduledDate").value(service.today().plusDays(2).toString()));
    }

    @Test
    void validationErrors() throws Exception {
        mvc.perform(json(post("/api/chores"), """
                {"title":"","scheduledDate":null}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists())
            .andExpect(jsonPath("$.fieldErrors.scheduledDate").exists());

        mvc.perform(json(post("/api/chores/templates"), """
                {"name":"X","recurrence":"EVERY_N_DAYS"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.recurrenceInterval").exists());
    }

    @Test
    void editAndDelete() throws Exception {
        LocalDate tomorrow = service.today().plusDays(1);
        long id = idOf(mvc.perform(json(post("/api/chores"), """
                {"title":"Fix shelf","scheduledDate":"%s"}""".formatted(service.today())))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(json(put("/api/chores/{id}", id), """
                {"title":"Fix kitchen shelf","description":"Left one","scheduledDate":"%s"}""".formatted(tomorrow)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Fix kitchen shelf"))
            .andExpect(jsonPath("$.scheduledDate").value(tomorrow.toString()));

        mvc.perform(delete("/api/chores/{id}", id).with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void templatesCanBeHiddenAndShown() throws Exception {
        long id = idOf(mvc.perform(json(post("/api/chores/templates"), """
                {"name":"Iron shirts","recurrence":"WEEKLY"}"""))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/chores/templates/{id}/deactivate", id).with(csrf()))
            .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/chores/templates?includeInactive=true"))
            .andExpect(jsonPath("$[*].name", hasItem("Iron shirts")));
        mvc.perform(post("/api/chores/templates/{id}/activate", id).with(csrf()))
            .andExpect(jsonPath("$.active").value(true));
    }

    private static long idOf(String body, String path) {
        return ((Number) JsonPath.read(body, path)).longValue();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
