package com.peladinhas.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "peladinhas.cors")
public record PeladinhasCorsProperties(List<String> allowedOrigins) {

    public List<String> allowedOriginsOrDefault() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }
        final List<String> normalizedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        if (normalizedOrigins.isEmpty()) {
            return List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }
        return normalizedOrigins;
    }
}
