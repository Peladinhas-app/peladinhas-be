package com.peladinhas.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.List;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.peladinhas.backend.auth.PeladinhasJwtValidatorFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class SecurityConfigJwtDecoderTests {

    private static final String EXPECTED_ISSUER = "https://example.supabase.co/auth/v1";
    private static final String EXPECTED_AUDIENCE = "authenticated";

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final PeladinhasJwtValidatorFactory validatorFactory = new PeladinhasJwtValidatorFactory();

    private HttpServer jwksServer;
    private ECKey ecSigningKey;
    private RSAKey rsaSigningKey;

    @BeforeEach
    void startJwksServer() throws Exception {
        ecSigningKey = ecSigningKey();
        rsaSigningKey = rsaSigningKey();
        JWKSet jwkSet = new JWKSet(List.of(ecSigningKey.toPublicJWK(), rsaSigningKey.toPublicJWK()));
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/.well-known/jwks.json", exchange -> {
            byte[] body = jwkSet.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream response = exchange.getResponseBody()) {
                response.write(body);
            }
        });
        jwksServer.start();
    }

    @AfterEach
    void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @Test
    void acceptsValidEs256SupabaseToken() throws Exception {
        Jwt jwt = decoder().decode(es256Token(EXPECTED_ISSUER, List.of(EXPECTED_AUDIENCE), future()));

        assertThat(jwt.getSubject()).isEqualTo("supabase-user");
        assertThat(jwt.getIssuer().toString()).isEqualTo(EXPECTED_ISSUER);
        assertThat(jwt.getAudience()).containsExactly(EXPECTED_AUDIENCE);
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String token = es256Token("https://other-project.supabase.co/auth/v1", List.of(EXPECTED_AUDIENCE), future());

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = es256Token(EXPECTED_ISSUER, List.of("service_role"), future());

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = es256Token(EXPECTED_ISSUER, List.of(EXPECTED_AUDIENCE), Instant.now().minusSeconds(7200));

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = tamperSignature(es256Token(EXPECTED_ISSUER, List.of(EXPECTED_AUDIENCE), future()));

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsUnsupportedAlgorithms() throws Exception {
        String token = rs256Token(EXPECTED_ISSUER, List.of(EXPECTED_AUDIENCE), future());

        assertThatThrownBy(() -> decoder().decode(token)).isInstanceOf(JwtException.class);
    }

    private JwtDecoder decoder() {
        return securityConfig.jwtDecoder(new PeladinhasAuthProperties(
                        "supabase",
                        EXPECTED_ISSUER,
                        jwkSetUri(),
                        EXPECTED_AUDIENCE),
                validatorFactory);
    }

    private String jwkSetUri() {
        return "http://127.0.0.1:%d/.well-known/jwks.json".formatted(jwksServer.getAddress().getPort());
    }

    private String es256Token(
            final String issuer,
            final List<String> audience,
            final Instant expiresAt) throws JOSEException {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(ecSigningKey.getKeyID()).build(),
                claims(issuer, audience, expiresAt));
        jwt.sign(new ECDSASigner(ecSigningKey));
        return jwt.serialize();
    }

    private String rs256Token(
            final String issuer,
            final List<String> audience,
            final Instant expiresAt) throws JOSEException {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaSigningKey.getKeyID()).build(),
                claims(issuer, audience, expiresAt));
        jwt.sign(new RSASSASigner(rsaSigningKey));
        return jwt.serialize();
    }

    private JWTClaimsSet claims(final String issuer, final List<String> audience, final Instant expiresAt) {
        Instant issuedAt = Instant.now().minusSeconds(60);
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("supabase-user")
                .audience(audience)
                .issueTime(java.util.Date.from(issuedAt))
                .expirationTime(java.util.Date.from(expiresAt))
                .claim("role", "authenticated")
                .build();
    }

    private Instant future() {
        return Instant.now().plusSeconds(300);
    }

    private String tamperSignature(final String token) {
        String[] parts = token.split("\\.");
        String signature = parts[2];
        String replacement = signature.startsWith("A") ? "B" : "A";
        parts[2] = replacement + signature.substring(1);
        return String.join(".", parts);
    }

    private ECKey ecSigningKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();
        return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey((ECPrivateKey) keyPair.getPrivate())
                .keyID("supabase-es256-test-key")
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.ES256)
                .build();
    }

    private RSAKey rsaSigningKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID("unsupported-rs256-test-key")
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
    }
}
