package com.peladinhas.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PeladinhasApplication {

    /**
     * Starts the Peladinhas backend application.
     *
     * @param args command-line options passed by the runtime
     */
    public static void main(final String[] args) {
        SpringApplication.run(PeladinhasApplication.class, args);
    }
}
