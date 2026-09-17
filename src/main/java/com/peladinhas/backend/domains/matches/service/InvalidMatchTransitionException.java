package com.peladinhas.backend.domains.matches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class InvalidMatchTransitionException extends DomainException {

    public InvalidMatchTransitionException(final String message) {
        super(message);
    }
}
