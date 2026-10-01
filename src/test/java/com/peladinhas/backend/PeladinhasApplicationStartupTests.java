package com.peladinhas.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

class PeladinhasApplicationStartupTests {

    /**
     * Verifies that production startup records Spring startup timing steps.
     */
    @Test
    void configuresBoundedStartupStepBuffer() {
        assertThat(PeladinhasApplication.applicationStartup())
                .isInstanceOf(BufferingApplicationStartup.class);
    }
}
