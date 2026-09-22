package com.peladinhas.backend.domains.users.service;

import com.peladinhas.backend.auth.AuthenticatedUserPrincipal;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;

public record CreateUserProfileCommand(
        AuthenticatedUserPrincipal principal,
        String name,
        PreferredLanguage preferredLanguage) {
}
