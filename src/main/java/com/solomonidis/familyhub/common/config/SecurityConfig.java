package com.solomonidis.familyhub.common.config;

import com.solomonidis.familyhub.member.MemberUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Session-cookie auth for the React SPA.
 * <ul>
 *   <li>{@code POST /api/auth/login} — form-encoded {@code username}, {@code password}, {@code remember-me}; 204 or 401</li>
 *   <li>{@code POST /api/auth/logout} — 204</li>
 *   <li>Anonymous {@code /api/**} → 401 (never a redirect)</li>
 *   <li>CSRF: {@code XSRF-TOKEN} cookie echoed back in the {@code X-XSRF-TOKEN} header</li>
 * </ul>
 * Everything outside {@code /api} is the SPA's static files and is public.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MemberUserDetailsService userDetailsService,
            @Value("${app.remember-me-key}") String rememberMeKey,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {

        var unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/csrf", "/actuator/health").permitAll()
                .requestMatchers("/api/**", "/h2-console/**").authenticated()
                .anyRequest().permitAll())
            .csrf(csrf -> csrf
                .spa()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) ->
                    response.setStatus(HttpStatus.NO_CONTENT.value()))
                .failureHandler((request, response, exception) ->
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Wrong username or password")))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(60 * 60 * 24 * 30))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorized));

        if (h2ConsoleEnabled) {
            // Dev only: the H2 console uses frames and its own forms without CSRF tokens.
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
