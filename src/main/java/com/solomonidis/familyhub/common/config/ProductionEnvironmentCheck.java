package com.solomonidis.familyhub.common.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/**
 * Runs before anything starts when the {@code prod} profile is active:
 * <ul>
 *   <li>Fails fast with a readable message listing missing env vars (instead of a deep datasource stack trace).</li>
 *   <li>Accepts Neon's connection string exactly as the dashboard shows it
 *       ({@code postgresql://user:pass@host/db?sslmode=require}) as well as the JDBC form
 *       ({@code jdbc:postgresql://host/db?sslmode=require} + {@code DATABASE_USERNAME}/{@code DATABASE_PASSWORD}).</li>
 * </ul>
 * Registered in {@code META-INF/spring.factories}.
 */
public class ProductionEnvironmentCheck implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        Map<String, Object> resolved = resolve(
            environment.getProperty("DATABASE_URL"),
            environment.getProperty("DATABASE_USERNAME"),
            environment.getProperty("DATABASE_PASSWORD"),
            environment.getProperty("REMEMBER_ME_KEY"));
        environment.getPropertySources().addFirst(new MapPropertySource("productionEnvironmentCheck", resolved));
    }

    /**
     * Validates the production env vars and returns the datasource properties to use.
     *
     * @throws IllegalStateException listing every problem found
     */
    static Map<String, Object> resolve(String databaseUrl, String username, String password, String rememberMeKey) {
        List<String> problems = new ArrayList<>();
        Map<String, Object> props = new LinkedHashMap<>();

        if (!StringUtils.hasText(databaseUrl)) {
            problems.add("DATABASE_URL is not set (Neon connection string, e.g. postgresql://user:pass@host/db?sslmode=require)");
        } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
            // already JDBC: application-prod.properties maps it as is
        } else if (databaseUrl.startsWith("postgresql://") || databaseUrl.startsWith("postgres://")) {
            URI uri = URI.create(databaseUrl.trim());
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery();
            if (!query.contains("sslmode=")) {
                query = query.isEmpty() ? "sslmode=require" : query + "&sslmode=require";
            }
            String hostPort = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            props.put("spring.datasource.url", "jdbc:postgresql://" + hostPort + uri.getRawPath() + "?" + query);
            if (uri.getRawUserInfo() != null) {
                String[] userPass = uri.getRawUserInfo().split(":", 2);
                if (!StringUtils.hasText(username)) {
                    username = decode(userPass[0]);
                    props.put("spring.datasource.username", username);
                }
                if (!StringUtils.hasText(password) && userPass.length == 2) {
                    password = decode(userPass[1]);
                    props.put("spring.datasource.password", password);
                }
            }
        } else {
            problems.add("DATABASE_URL must start with postgresql:// (as Neon shows it) or jdbc:postgresql://");
        }

        if (!StringUtils.hasText(username)) {
            problems.add("DATABASE_USERNAME is not set (or put user:password in DATABASE_URL)");
        }
        if (!StringUtils.hasText(password)) {
            problems.add("DATABASE_PASSWORD is not set (or put user:password in DATABASE_URL)");
        }
        if (!StringUtils.hasText(rememberMeKey) || rememberMeKey.length() < 32) {
            problems.add("REMEMBER_ME_KEY must be set to a random string of at least 32 characters");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("Production configuration is incomplete:\n  - " + String.join("\n  - ", problems));
        }
        return props;
    }

    private static String decode(String value) {
        // percent-decoding only: a literal '+' in a password must stay a '+', not become a space
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // after application-*.properties and env vars are loaded
    }
}
