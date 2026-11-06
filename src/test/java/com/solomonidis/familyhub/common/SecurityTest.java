package com.solomonidis.familyhub.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
class SecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Log in")));
    }

    @Test
    void bootstrapMemberCanLogIn() throws Exception {
        mvc.perform(formLogin().user("admin").password("admin-pass"))
            .andExpect(authenticated().withUsername("admin"))
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void usernameIsCaseInsensitive() throws Exception {
        mvc.perform(formLogin().user("ADMIN").password("admin-pass"))
            .andExpect(authenticated());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin().user("admin").password("nope"))
            .andExpect(unauthenticated())
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithUserDetails("admin")
    void homeShowsGreetingForLoggedInMember() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Hi <span>Admin</span>")));
    }
}
