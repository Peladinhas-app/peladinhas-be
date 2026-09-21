package com.peladinhas.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.peladinhas.backend.config.PeladinhasAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class PeladinhasJwtValidatorFactoryTests {

    private static final String EXPECTED_ISSUER = "https://example.supabase.co/auth/v1";
    private static final String EXPECTED_AUDIENCE = "authenticated";

    private final PeladinhasJwtValidatorFactory validatorFactory = new PeladinhasJwtValidatorFactory();

    @Test
    void acceptsExpectedIssuerAndAuthenticatedAudience() {
        OAuth2TokenValidator<Jwt> validator = validator();

        assertThat(validator.validate(jwt(EXPECTED_ISSUER, List.of(EXPECTED_AUDIENCE), Instant.now().plusSeconds(300)))
                .hasErrors()).isFalse();
    }

    @Test
    void rejectsWrongIssuer() {
        OAuth2TokenValidator<Jwt> validator = validator();

        assertThat(validator.validate(jwt(
                "https://other-project.supabase.co/auth/v1",
                List.of(EXPECTED_AUDIENCE),
                Instant.now().plusSeconds(300))).hasErrors()).isTrue();
    }

    @Test
    void rejectsMissingAuthenticatedAudience() {
        OAuth2TokenValidator<Jwt> validator = validator();

        assertThat(validator.validate(jwt(EXPECTED_ISSUER, List.of("service_role"), Instant.now().plusSeconds(300)))
                .hasErrors()).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        OAuth2TokenValidator<Jwt> validator = validator();

        assertThat(validator.validate(jwt(EXPECTED_ISSUER, List.of(EXPECTED_AUDIENCE), Instant.now().minusSeconds(300)))
                .hasErrors()).isTrue();
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return validatorFactory.create(new PeladinhasAuthProperties(
                "supabase",
                EXPECTED_ISSUER,
                "https://example.supabase.co/auth/v1/.well-known/jwks.json",
                EXPECTED_AUDIENCE));
    }

    private Jwt jwt(final String issuer, final List<String> audience, final Instant expiresAt) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(60) : now.minusSeconds(60);
        return new Jwt(
                "token",
                issuedAt,
                expiresAt,
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", issuer,
                        "sub", "supabase-user",
                        "aud", audience));
    }
}
