package com.peladinhas.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.peladinhas.backend.support.PostgreSqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseConnectionTests extends PostgreSqlContainerTest {

    private final JdbcTemplate jdbcTemplate;

    DatabaseConnectionTests(@Autowired final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verifies that Spring can reach the temporary PostgreSQL database.
     */
    @Test
    void connectsToTemporaryPostgreSQL() {
        Map<String, Object> result = jdbcTemplate.queryForMap(
                "select current_database() as database_name, current_user as user_name");

        assertThat(result)
                .containsEntry("database_name", "peladinhas_test")
                .containsEntry("user_name", "peladinhas_test");
    }

    /**
     * Verifies that Flyway starts before real application migrations exist.
     */
    @Test
    void initializesFlywayWithoutApplicationTables() {
        List<String> tables = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                order by table_name
                """, String.class);
        Integer migrationRows = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history", Integer.class);

        assertThat(tables).containsExactly("flyway_schema_history");
        assertThat(migrationRows).isZero();
    }
}
