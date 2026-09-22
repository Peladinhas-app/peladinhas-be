package com.peladinhas.backend.auth;

public record AuthenticatedUserPrincipal(
        String provider,
        String subject,
        String email) {
}
