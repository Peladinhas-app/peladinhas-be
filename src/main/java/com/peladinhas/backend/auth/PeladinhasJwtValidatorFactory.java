package com.peladinhas.backend.auth;

import com.peladinhas.backend.config.PeladinhasAuthProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.stereotype.Component;

@Component
public class PeladinhasJwtValidatorFactory {

    private static final String INVALID_AUDIENCE_CODE = "invalid_token";

    public OAuth2TokenValidator<Jwt> create(final PeladinhasAuthProperties properties) {
        if (!properties.hasIssuerUri()) {
            throw new IllegalStateException("JWT issuer URI is required when JWT validation is configured.");
        }
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuerUri()),
                audienceValidator(properties.audienceOrDefault()));
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(final String expectedAudience) {
        return jwt -> {
            if (jwt.getAudience().contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    INVALID_AUDIENCE_CODE,
                    "JWT audience must contain '%s'.".formatted(expectedAudience),
                    null));
        };
    }
}
