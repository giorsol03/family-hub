package com.solomonidis.familyhub.common.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousApiCallIs401NotRedirect() throws Exception {
        mvc.perform(get("/api/members"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithCorrectPasswordReturns204() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "admin")
                .param("password", "admin-pass"))
            .andExpect(status().isNoContent())
            .andExpect(authenticated().withUsername("admin"));
    }

    @Test
    void usernameIsCaseInsensitive() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "ADMIN")
                .param("password", "admin-pass"))
            .andExpect(authenticated());
    }

    @Test
    void rememberMeSetsCookie() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "admin")
                .param("password", "admin-pass")
                .param("remember-me", "true"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().exists("remember-me"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "admin")
                .param("password", "nope"))
            .andExpect(status().isUnauthorized())
            .andExpect(unauthenticated());
    }

    @Test
    void loginWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/login")
                .param("username", "admin")
                .param("password", "admin-pass"))
            .andExpect(status().isForbidden());
    }

    // Note: spring-security-test replaces the CSRF repository under MockMvc, so the
    // XSRF-TOKEN cookie / X-XSRF-TOKEN header name can only be checked against the running app.
    @Test
    void csrfEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @WithUserDetails("admin")
    void meReturnsCurrentMember() throws Exception {
        mvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.name").value("Admin"));
    }

    @Test
    @WithUserDetails("admin")
    void logoutReturns204() throws Exception {
        mvc.perform(post("/api/auth/logout").with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void spaDeepLinksForwardToIndex() throws Exception {
        mvc.perform(get("/members/3"))
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @WithUserDetails("admin")
    void unknownApiPathIs404() throws Exception {
        mvc.perform(get("/api/nope"))
            .andExpect(status().isNotFound());
    }
}
