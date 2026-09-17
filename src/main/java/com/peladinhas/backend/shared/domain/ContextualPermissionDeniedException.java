package com.peladinhas.backend.shared.domain;

public class ContextualPermissionDeniedException extends DomainException {

    public ContextualPermissionDeniedException(final String message) {
        super(message);
    }
}
