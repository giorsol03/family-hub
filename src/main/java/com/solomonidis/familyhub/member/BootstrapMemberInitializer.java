package com.solomonidis.familyhub.member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, if no members exist, creates the first one from
 * APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD (and optional APP_ADMIN_NAME).
 * Passwords are never seeded through SQL migrations.
 */
@Component
public class BootstrapMemberInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapMemberInitializer.class);

    private final MemberService memberService;
    private final String name;
    private final String username;
    private final String password;

    public BootstrapMemberInitializer(MemberService memberService,
                                      @Value("${app.bootstrap.name}") String name,
                                      @Value("${app.bootstrap.username}") String username,
                                      @Value("${app.bootstrap.password}") String password) {
        this.memberService = memberService;
        this.name = name;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) {
            if (memberService.findAll().isEmpty()) {
                log.warn("No members exist and APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD are not set - nobody can log in.");
            }
            return;
        }
        if (memberService.createBootstrapMemberIfNone(name, username, password)) {
            log.info("Created first member '{}'", username);
        }
    }
}
