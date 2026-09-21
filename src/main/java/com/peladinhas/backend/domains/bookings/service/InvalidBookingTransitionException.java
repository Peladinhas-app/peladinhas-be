package com.peladinhas.backend.domains.bookings.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class InvalidBookingTransitionException extends DomainException {

    public InvalidBookingTransitionException(final String message) {
        super(message);
    }
}
