package com.peladinhas.backend.domains.owners.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class InvalidOwnerInvitationCodeException extends DomainException {

    public InvalidOwnerInvitationCodeException() {
        super("Pitch owner invitation code is invalid.");
    }
}
