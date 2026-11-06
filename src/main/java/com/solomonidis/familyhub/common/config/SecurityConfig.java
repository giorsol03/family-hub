package com.solomonidis.familyhub.common.config;

import com.solomonidis.familyhub.member.MemberUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String H2_CONSOLE = "/h2-console/**";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MemberUserDetailsService userDetailsService,
            @Value("${app.remember-me-key}") String rememberMeKey,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico", "/error", "/actuator/health")
                    .permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(60 * 60 * 24 * 30));

        if (h2ConsoleEnabled) {
            // Dev only: the H2 console uses frames and its own forms without CSRF tokens.
            http.csrf(csrf -> csrf.ignoringRequestMatchers(H2_CONSOLE))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
