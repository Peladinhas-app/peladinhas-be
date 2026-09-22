package com.peladinhas.backend.auth;

import com.peladinhas.backend.shared.domain.DomainException;

public class InvalidAuthenticatedEmailException extends DomainException {

    public InvalidAuthenticatedEmailException(final String message) {
        super(message);
    }
}
