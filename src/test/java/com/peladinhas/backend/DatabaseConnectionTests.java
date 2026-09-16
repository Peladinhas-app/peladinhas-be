package com.peladinhas.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "PELADINHAS_RUN_LOCAL_DB_TESTS", matches = "true")
class DatabaseConnectionTests {

    private final JdbcTemplate jdbcTemplate;

    DatabaseConnectionTests(@Autowired final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verifies that Spring can reach the local PostgreSQL database.
     */
    @Test
    void connectsToLocalPostgreSQL() {
        Map<String, Object> result = jdbcTemplate.queryForMap(
                "select current_database() as database_name, current_user as user_name");

        assertThat(result)
                .containsEntry("database_name", "peladinhas_dev")
                .containsEntry("user_name", "peladinhas_dev");
    }
}
