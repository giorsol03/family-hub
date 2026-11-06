package com.solomonidis.familyhub.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class MemberApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void listReturnsMembersWithoutPasswordHash() throws Exception {
        mvc.perform(get("/api/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].username", hasItem("admin")))
            .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void createValidMember() throws Exception {
        mvc.perform(json(post("/api/members"), """
                {"name":"Maria","username":"Maria","color":"#FF00AA","password":"secret-123"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.username").value("maria"))
            .andExpect(jsonPath("$.color").value("#ff00aa"))
            .andExpect(jsonPath("$.active").value(true));

        assertThat(memberRepository.findByUsername("maria")).isPresent();
    }

    @Test
    void createWithInvalidFieldsReturnsFieldErrors() throws Exception {
        mvc.perform(json(post("/api/members"), """
                {"name":"","username":"a b","color":"red","password":"short"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.fieldErrors.username").exists())
            .andExpect(jsonPath("$.fieldErrors.color").exists())
            .andExpect(jsonPath("$.fieldErrors.password").value("At least 8 characters"));
    }

    @Test
    void createWithoutPasswordIsRejected() throws Exception {
        mvc.perform(json(post("/api/members"), """
                {"name":"Maria","username":"maria","color":"#ff00aa"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));
    }

    @Test
    void createWithTakenUsernameIsConflict() throws Exception {
        mvc.perform(json(post("/api/members"), """
                {"name":"Another","username":"ADMIN","color":"#ff00aa","password":"secret-123"}"""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fieldErrors.username").value("Username is already taken"));
    }

    @Test
    void updateKeepsPasswordWhenBlank() throws Exception {
        Member admin = memberRepository.findByUsername("admin").orElseThrow();
        String hash = admin.getPasswordHash();

        mvc.perform(json(put("/api/members/{id}", admin.getId()), """
                {"name":"Admin Renamed","username":"admin","color":"#123456","password":""}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Admin Renamed"));

        assertThat(memberRepository.findByUsername("admin").orElseThrow().getPasswordHash()).isEqualTo(hash);
    }

    @Test
    void unknownMemberIs404ProblemDetail() throws Exception {
        mvc.perform(get("/api/members/{id}", 999_999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deactivateAndActivate() throws Exception {
        Member other = memberRepository.save(new Member("Kostas", "kostas", "{noop}x", "#00ff00"));

        mvc.perform(post("/api/members/{id}/deactivate", other.getId()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
        mvc.perform(post("/api/members/{id}/activate", other.getId()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivatingYourselfIsRejected() throws Exception {
        Long adminId = memberRepository.findByUsername("admin").orElseThrow().getId();

        mvc.perform(post("/api/members/{id}/deactivate", adminId).with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("You can't deactivate yourself"));
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
