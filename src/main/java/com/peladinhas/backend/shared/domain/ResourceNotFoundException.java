package com.peladinhas.backend.shared.domain;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
