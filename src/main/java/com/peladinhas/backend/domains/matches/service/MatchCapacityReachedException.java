package com.peladinhas.backend.domains.matches.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class MatchCapacityReachedException extends DomainException {

    public MatchCapacityReachedException(final String message) {
        super(message);
    }
}
