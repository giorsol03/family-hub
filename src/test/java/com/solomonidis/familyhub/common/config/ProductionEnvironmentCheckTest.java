package com.solomonidis.familyhub.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductionEnvironmentCheckTest {

    static final String KEY = "0123456789abcdef0123456789abcdef";

    @Test
    void acceptsNeonConnectionStringAsShownInTheDashboard() {
        Map<String, Object> props = ProductionEnvironmentCheck.resolve(
            "postgresql://familyhub_owner:p%40ss:w0rd@ep-cool-1.eu-central-1.aws.neon.tech/familyhub?sslmode=require",
            null, null, KEY);

        assertThat(props).containsEntry("spring.datasource.url",
                "jdbc:postgresql://ep-cool-1.eu-central-1.aws.neon.tech/familyhub?sslmode=require")
            .containsEntry("spring.datasource.username", "familyhub_owner")
            .containsEntry("spring.datasource.password", "p@ss:w0rd");
    }

    @Test
    void addsSslAndKeepsPortAndOtherParameters() {
        Map<String, Object> props = ProductionEnvironmentCheck.resolve(
            "postgres://u:p@host:5433/db?channel_binding=require", null, null, KEY);

        assertThat(props.get("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://host:5433/db?channel_binding=require&sslmode=require");
    }

    @Test
    void separateCredentialVariablesWinOverTheUrl() {
        Map<String, Object> props = ProductionEnvironmentCheck.resolve("postgresql://u:p@host/db", "real-user", "real-pass", KEY);

        assertThat(props).doesNotContainKeys("spring.datasource.username", "spring.datasource.password");
    }

    @Test
    void acceptsJdbcFormWithSeparateCredentials() {
        Map<String, Object> props = ProductionEnvironmentCheck.resolve(
            "jdbc:postgresql://host/db?sslmode=require", "user", "pass", KEY);

        assertThat(props).isEmpty(); // nothing to convert; application-prod.properties maps the vars
    }

    @Test
    void plusInPasswordIsKept() {
        Map<String, Object> props = ProductionEnvironmentCheck.resolve("postgresql://u:a+b@host/db", null, null, KEY);

        assertThat(props).containsEntry("spring.datasource.password", "a+b");
    }

    @Test
    void listsEveryMissingSettingAtOnce() {
        assertThatThrownBy(() -> ProductionEnvironmentCheck.resolve(null, null, null, "short"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DATABASE_URL is not set")
            .hasMessageContaining("DATABASE_USERNAME is not set")
            .hasMessageContaining("DATABASE_PASSWORD is not set")
            .hasMessageContaining("REMEMBER_ME_KEY");
    }

    @Test
    void rejectsSomethingThatIsNotAPostgresUrl() {
        assertThatThrownBy(() -> ProductionEnvironmentCheck.resolve("mysql://u:p@host/db", null, null, KEY))
            .hasMessageContaining("must start with postgresql://");
    }
}
