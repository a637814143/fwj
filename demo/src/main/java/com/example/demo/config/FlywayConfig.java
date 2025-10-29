package com.example.demo.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy repairOnValidationErrorStrategy() {
        return flyway -> migrateWithRepairOnValidationError(flyway);
    }

    private void migrateWithRepairOnValidationError(Flyway flyway) {
        try {
            flyway.migrate();
        } catch (FlywayValidateException validateException) {
            log.warn("Flyway validation failed, attempting automatic repair before retrying migration", validateException);
            flyway.repair();
            flyway.migrate();
        }
    }
}

