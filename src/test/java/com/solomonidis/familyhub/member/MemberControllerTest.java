package com.solomonidis.familyhub.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithUserDetails("admin")
class MemberControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void listShowsMembersAndNav() throws Exception {
        mvc.perform(get("/members"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/list"))
            .andExpect(content().string(containsString("@admin")))
            .andExpect(content().string(containsString("bottom-nav")));
    }

    @Test
    void createValidMemberRedirectsToList() throws Exception {
        mvc.perform(post("/members").with(csrf())
                .param("name", "Maria")
                .param("username", "maria")
                .param("color", "#ff00aa")
                .param("password", "secret-123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/members"))
            .andExpect(flash().attribute("success", "Maria added"));

        assertThat(memberRepository.findByUsername("maria")).isPresent();
    }

    @Test
    void createWithInvalidFieldsRedisplaysForm() throws Exception {
        mvc.perform(post("/members").with(csrf())
                .param("name", "")
                .param("username", "a b")
                .param("color", "red")
                .param("password", "short"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/form"))
            .andExpect(model().attributeHasFieldErrors("form", "name", "username", "color", "password"));
    }

    @Test
    void createWithoutPasswordShowsError() throws Exception {
        mvc.perform(post("/members").with(csrf())
                .param("name", "Maria")
                .param("username", "maria")
                .param("color", "#ff00aa")
                .param("password", ""))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "password"));
    }

    @Test
    void createWithTakenUsernameShowsError() throws Exception {
        mvc.perform(post("/members").with(csrf())
                .param("name", "Another admin")
                .param("username", "Admin")
                .param("color", "#ff00aa")
                .param("password", "secret-123"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "username"));
    }

    @Test
    void editFormIsPrefilled() throws Exception {
        Long adminId = memberRepository.findByUsername("admin").orElseThrow().getId();

        mvc.perform(get("/members/{id}/edit", adminId))
            .andExpect(status().isOk())
            .andExpect(view().name("member/form"))
            .andExpect(content().string(containsString("value=\"admin\"")));
    }

    @Test
    void unknownMemberIs404() throws Exception {
        mvc.perform(get("/members/{id}/edit", 999_999))
            .andExpect(status().isNotFound());
    }

    @Test
    void deactivatingYourselfIsRefused() throws Exception {
        Long adminId = memberRepository.findByUsername("admin").orElseThrow().getId();

        mvc.perform(post("/members/{id}/deactivate", adminId).with(csrf()))
            .andExpect(redirectedUrl("/members"))
            .andExpect(flash().attributeExists("error"));
    }
}
