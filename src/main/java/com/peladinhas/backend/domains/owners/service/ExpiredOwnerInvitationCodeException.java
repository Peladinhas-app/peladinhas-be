package com.peladinhas.backend.domains.owners.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class ExpiredOwnerInvitationCodeException extends DomainException {

    public ExpiredOwnerInvitationCodeException() {
        super("Pitch owner invitation code has expired.");
    }
}
