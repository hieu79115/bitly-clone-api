package com.thinh.shortener.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // Ensure short_code column can hold up to 100 characters in PostgreSQL if not already altered
            jdbcTemplate.execute("ALTER TABLE urls ALTER COLUMN short_code TYPE VARCHAR(100)");
        } catch (Exception ex) {
            log.debug("Column short_code length check/alter completed: {}", ex.getMessage());
        }

        try {
            // Free up any previously soft-deleted URLs that still retain their original short_code
            int updated = jdbcTemplate.update(
                    "UPDATE urls SET short_code = CONCAT(short_code, '_del_', id) " +
                    "WHERE deleted_at IS NOT NULL AND short_code NOT LIKE '%_del_%'"
            );
            if (updated > 0) {
                log.info("Migrated {} existing soft-deleted URLs to free up their short codes for reuse", updated);
            }
        } catch (Exception ex) {
            log.warn("Soft-deleted URLs migration skipped or failed: {}", ex.getMessage());
        }
    }
}
