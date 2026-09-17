package com.peladinhas.backend.domains.matches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class MatchNotAcceptingParticipantsException extends DomainException {

    public MatchNotAcceptingParticipantsException(final String message) {
        super(message);
    }
}
