package com.peladinhas.backend.domains.matches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class InvalidParticipantTransitionException extends DomainException {

    public InvalidParticipantTransitionException(final String message) {
        super(message);
    }
}
