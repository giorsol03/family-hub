package com.solomonidis.familyhub.goal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.solomonidis.familyhub.member.MemberRepository;
import java.time.YearMonth;
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
class GoalApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    GoalService service;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void createTickAndReadTheMonth() throws Exception {
        long adminId = memberRepository.findByUsername("admin").orElseThrow().getId();
        String today = service.today().toString();
        String month = YearMonth.from(service.today()).toString();

        String body = mvc.perform(json(post("/api/goals"), """
                {"name":"Gym","ownerId":%d,"targetDaysPerWeek":3}""".formatted(adminId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startDate").value(today))
            .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(json(put("/api/goals/{id}/checks/{date}", id, today), """
                {"done":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.doneDates[0]").value(today))
            .andExpect(jsonPath("$.stats.streak").value(1))
            .andExpect(jsonPath("$.stats.percentThisMonth").value(100))
            .andExpect(jsonPath("$.stats.doneThisWeek").value(1));

        mvc.perform(get("/api/goals/month").param("month", month))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.month").value(month))
            .andExpect(jsonPath("$.today").value(today))
            .andExpect(jsonPath("$.rows[0].goal.name").value("Gym"))
            .andExpect(jsonPath("$.rows[0].goal.owner.name").value("Admin"));

        mvc.perform(json(put("/api/goals/{id}/checks/{date}", id, service.today().plusDays(1)), """
                {"done":true}"""))
            .andExpect(status().isBadRequest());

        mvc.perform(post("/api/goals/{id}/deactivate", id).with(csrf()))
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void validation() throws Exception {
        mvc.perform(json(post("/api/goals"), """
                {"name":"","ownerId":null,"targetDaysPerWeek":8}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.fieldErrors.ownerId").exists())
            .andExpect(jsonPath("$.fieldErrors.targetDaysPerWeek").value("1–7 days"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
