package com.peladinhas.backend.auth;

import com.peladinhas.backend.shared.domain.DomainException;

public class AuthenticatedUserNotFoundException extends DomainException {

    public AuthenticatedUserNotFoundException(final String message) {
        super(message);
    }
}
