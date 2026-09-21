package com.peladinhas.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "peladinhas.auth")
public record PeladinhasAuthProperties(
        String provider,
        String issuerUri,
        String jwkSetUri,
        String audience) {

    public String providerOrDefault() {
        return hasText(provider) ? provider : "supabase";
    }

    public boolean hasIssuerUri() {
        return hasText(issuerUri);
    }

    public boolean hasJwkSetUri() {
        return hasText(jwkSetUri);
    }

    public String audienceOrDefault() {
        return hasText(audience) ? audience : "authenticated";
    }

    private boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
