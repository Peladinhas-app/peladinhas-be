package com.peladinhas.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class PeladinhasApplication {

    static final int STARTUP_BUFFER_CAPACITY = 4096;

    /**
     * Starts the Peladinhas backend application.
     *
     * @param args command-line options passed by the runtime
     */
    public static void main(final String[] args) {
        SpringApplication application = new SpringApplication(PeladinhasApplication.class);
        application.setApplicationStartup(applicationStartup());
        application.run(args);
    }

    static BufferingApplicationStartup applicationStartup() {
        return new BufferingApplicationStartup(STARTUP_BUFFER_CAPACITY);
    }
}
