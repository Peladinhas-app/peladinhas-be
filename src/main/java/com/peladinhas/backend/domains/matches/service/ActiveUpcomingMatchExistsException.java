package com.peladinhas.backend.domains.matches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class ActiveUpcomingMatchExistsException extends DomainException {

    public ActiveUpcomingMatchExistsException(final String message) {
        super(message);
    }
}
