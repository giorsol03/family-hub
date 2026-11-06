package com.solomonidis.familyhub.common.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Single source of "now" for the app, in the household timezone. Inject {@link Clock}
 * instead of calling {@code LocalDate.now()} so behaviour is testable.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentMemberProvider", dateTimeProviderRef = "auditingDateTimeProvider")
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
