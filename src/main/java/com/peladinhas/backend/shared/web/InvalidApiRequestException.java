package com.peladinhas.backend.shared.web;

import com.peladinhas.backend.shared.domain.DomainException;

public class InvalidApiRequestException extends DomainException {

    public InvalidApiRequestException(final String message) {
        super(message);
    }
}