package com.peladinhas.backend.domains.pitches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class InvalidPitchIntervalException extends DomainException {

    public InvalidPitchIntervalException(final String message) {
        super(message);
    }
}
