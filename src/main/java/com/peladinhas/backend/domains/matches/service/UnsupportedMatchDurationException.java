package com.peladinhas.backend.domains.matches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class UnsupportedMatchDurationException extends DomainException {

    public UnsupportedMatchDurationException(final String message) {
        super(message);
    }
}
