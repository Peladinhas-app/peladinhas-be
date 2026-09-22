package com.peladinhas.backend.domains.users.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class UserProfileConflictException extends DomainException {

    public UserProfileConflictException(final String message) {
        super(message);
    }
}
