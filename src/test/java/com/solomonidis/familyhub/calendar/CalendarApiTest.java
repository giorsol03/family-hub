package com.solomonidis.familyhub.calendar;

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
class CalendarApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void eventCrud() throws Exception {
        String body = mvc.perform(json(post("/api/calendar/events"), """
                {"title":"Night out","type":"NIGHT_OUT","allDay":false,
                 "start":"2026-11-14T20:00:00","end":"2026-11-14T23:30:00","location":"Nicosia"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.kind").value("EVENT"))
            .andExpect(jsonPath("$.start").value("2026-11-14T20:00:00"))
            .andExpect(jsonPath("$.attendees.length()").value(0))
            .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(get("/api/calendar").param("from", "2026-11-09").param("to", "2026-11-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.from").value("2026-11-09"))
            .andExpect(jsonPath("$.today").exists())
            .andExpect(jsonPath("$.items[0].title").value("Night out"));

        mvc.perform(json(put("/api/calendar/events/{id}", id), """
                {"title":"Cinema","type":"NIGHT_OUT","allDay":true,
                 "start":"2026-11-14T00:00:00","end":"2026-11-14T00:00:00"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allDay").value(true))
            .andExpect(jsonPath("$.location").doesNotExist());

        mvc.perform(delete("/api/calendar/events/{id}", id).with(csrf()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/calendar/events/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void validation() throws Exception {
        mvc.perform(json(post("/api/calendar/events"), """
                {"title":"","type":null,"start":null,"end":null}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists())
            .andExpect(jsonPath("$.fieldErrors.type").exists())
            .andExpect(jsonPath("$.fieldErrors.start").exists());

        mvc.perform(json(post("/api/calendar/events"), """
                {"title":"Backwards","type":"EVENT","start":"2026-11-14T20:00:00","end":"2026-11-14T19:00:00"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.end").value("Ends before it starts"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
