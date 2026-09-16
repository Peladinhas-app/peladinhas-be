package com.peladinhas.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
public abstract class PostgreSqlContainerTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:18.6")
            .withDatabaseName("peladinhas_test")
            .withUsername("peladinhas_test")
            .withPassword("peladinhas_test_password");

    /**
     * Gives Spring the temporary database connection created for this test run.
     *
     * @param registry Spring's dynamic setting registry
     */
    @DynamicPropertySource
    static void registerPostgreSqlProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRESQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.fail-on-missing-locations", () -> "false");
        registry.add("spring.flyway.clean-disabled", () -> "true");
    }
}
