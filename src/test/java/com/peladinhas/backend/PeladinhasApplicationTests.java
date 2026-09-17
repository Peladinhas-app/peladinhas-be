package com.peladinhas.backend;

import com.peladinhas.backend.support.PostgreSqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class PeladinhasApplicationTests extends PostgreSqlContainerTest {

    /**
     * Verifies that the Spring application context can start.
     */
    @Test
    void contextLoads() {
    }
}
