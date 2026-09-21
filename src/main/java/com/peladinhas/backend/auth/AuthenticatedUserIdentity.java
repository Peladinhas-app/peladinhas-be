package com.peladinhas.backend.auth;

public record AuthenticatedUserIdentity(String provider, String subject) {
}
