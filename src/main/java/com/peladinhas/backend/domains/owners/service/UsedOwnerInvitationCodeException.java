package com.peladinhas.backend.domains.owners.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class UsedOwnerInvitationCodeException extends DomainException {

    public UsedOwnerInvitationCodeException() {
        super("Pitch owner invitation code has already been used.");
    }
}
