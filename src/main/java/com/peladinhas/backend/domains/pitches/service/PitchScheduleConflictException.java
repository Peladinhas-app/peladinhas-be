package com.peladinhas.backend.domains.pitches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class PitchScheduleConflictException extends DomainException {

    public PitchScheduleConflictException(final String message) {
        super(message);
    }
}
