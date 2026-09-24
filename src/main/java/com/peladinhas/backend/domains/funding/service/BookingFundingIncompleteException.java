package com.peladinhas.backend.domains.funding.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class BookingFundingIncompleteException extends DomainException {

    public BookingFundingIncompleteException(final String message) {
        super(message);
    }
}
