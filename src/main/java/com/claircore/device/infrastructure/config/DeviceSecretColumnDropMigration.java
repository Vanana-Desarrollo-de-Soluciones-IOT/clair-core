package com.claircore.device.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time migration that removes the legacy {@code device_secret} column
 * from the {@code devices} table after it was renamed to {@code api_key}.
 */
@Component
public class DeviceSecretColumnDropMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceSecretColumnDropMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public DeviceSecretColumnDropMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void dropLegacyColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE devices DROP COLUMN IF EXISTS device_secret");
            LOGGER.info("Dropped legacy device_secret column from devices table");
        } catch (Exception e) {
            LOGGER.warn("Could not drop device_secret column (may already be gone): {}", e.getMessage());
        }
    }
}
