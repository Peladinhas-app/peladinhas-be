package com.peladinhas.backend.domains.pitches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class PitchNotAvailableException extends DomainException {

    public PitchNotAvailableException(final String message) {
        super(message);
    }
}
