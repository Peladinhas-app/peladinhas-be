package com.peladinhas.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
class PeladinhasApplicationTests {

    /**
     * Verifies that the Spring application context can start.
     */
    @Test
    void contextLoads() {
    }
}
