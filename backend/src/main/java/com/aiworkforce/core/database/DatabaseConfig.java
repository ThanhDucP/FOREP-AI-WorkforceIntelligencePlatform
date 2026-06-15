package com.aiworkforce.core.database;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
@EnableJpaAuditing
@Slf4j
public class DatabaseConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            DataSource dataSource,
            @Value("${app.database.reset-on-start:false}") boolean resetOnStart,
            @Value("${app.database.reset-schema:public}") String resetSchema
    ) {
        return flyway -> {
            if (resetOnStart) {
                String schema = sanitizeSchemaName(resetSchema);
                log.warn("APP_DATABASE_RESET_ON_START is enabled. Dropping and recreating schema '{}' before Flyway migration.", schema);
                resetSchema(dataSource, schema);
            }
            flyway.migrate();
        };
    }

    private void resetSchema(DataSource dataSource, String schema) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("GRANT ALL ON SCHEMA " + schema + " TO PUBLIC");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reset database schema '" + schema + "'", e);
        }
    }

    private String sanitizeSchemaName(String schema) {
        if (schema == null || schema.isBlank()) {
            return "public";
        }
        String trimmed = schema.trim();
        if (!trimmed.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid database schema name: " + schema);
        }
        return trimmed;
    }
}
