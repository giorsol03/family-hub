package com.solomonidis.familyhub.common.config;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Caching for the built React app served from {@code static/} (production only; in dev Vite serves it).
 * Vite puts a content hash in every file name under {@code assets/}, so those can be cached for a year:
 * a new build produces new names. {@code index.html} and other files are revalidated on every load
 * (see {@code spring.web.resources.cache.cachecontrol.no-cache}) so phones pick up a new release at once.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
