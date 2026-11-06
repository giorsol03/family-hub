package com.solomonidis.familyhub.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The built SPA is public; hashed bundles are cached for a year, index.html is always revalidated. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticResourceTest {

    @Autowired
    MockMvc mvc;

    @Test
    void hashedAssetsAreCachedLongAndPublic() throws Exception {
        mvc.perform(get("/assets/app-abc123.js"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
            .andExpect(header().string("Cache-Control", containsString("immutable")));
    }

    @Test
    void indexHtmlIsServedWithoutLoginAndRevalidated() throws Exception {
        mvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }
}
