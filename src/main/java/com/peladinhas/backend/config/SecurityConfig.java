package com.peladinhas.backend.config;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import tools.jackson.databind.ObjectMapper;
import com.peladinhas.backend.auth.PeladinhasJwtValidatorFactory;
import com.peladinhas.backend.shared.web.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(PeladinhasAuthProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            final HttpSecurity http,
            final ObjectMapper objectMapper,
            final Clock clock) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/api/v1/**").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, objectMapper, clock, HttpStatus.UNAUTHORIZED,
                                        "unauthenticated", "Authentication is required."))
                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, objectMapper, clock, HttpStatus.UNAUTHORIZED,
                                        "unauthenticated", "Authentication is required."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, objectMapper, clock, HttpStatus.FORBIDDEN,
                                        "permission_denied", "Authenticated user is not permitted.")))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            final PeladinhasAuthProperties properties,
            final PeladinhasJwtValidatorFactory jwtValidatorFactory) {
        if (properties.hasJwkSetUri()) {
            NimbusJwtDecoder decoder = supabaseAccessTokenDecoder(
                            NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()))
                    .build();
            decoder.setJwtValidator(jwtValidatorFactory.create(properties));
            return decoder;
        }
        if (properties.hasIssuerUri()) {
            NimbusJwtDecoder decoder = supabaseAccessTokenDecoder(
                            NimbusJwtDecoder.withIssuerLocation(properties.issuerUri()))
                    .build();
            decoder.setJwtValidator(jwtValidatorFactory.create(properties));
            return decoder;
        }
        return token -> {
            throw new BadJwtException("JWT verification is not configured.");
        };
    }

    private NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder supabaseAccessTokenDecoder(
            final NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder builder) {
        return builder.jwsAlgorithm(SignatureAlgorithm.ES256);
    }

    private void writeError(
            final HttpServletResponse response,
            final ObjectMapper objectMapper,
            final Clock clock,
            final HttpStatus status,
            final String code,
            final String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                code,
                message,
                OffsetDateTime.now(clock),
                List.of()));
    }
}
