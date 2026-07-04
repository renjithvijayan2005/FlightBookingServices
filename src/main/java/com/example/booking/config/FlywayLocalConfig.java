package com.example.booking.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Repairs Flyway schema history before migrating in the local dev profile.
 * Needed because MySQL lacks DDL transaction support: a failed migration is
 * recorded as success=0 and Flyway refuses to proceed without a repair step.
 */
@Configuration
@Profile("local")
public class FlywayLocalConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
