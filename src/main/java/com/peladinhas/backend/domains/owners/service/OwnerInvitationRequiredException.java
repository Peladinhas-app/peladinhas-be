package com.peladinhas.backend.domains.owners.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class OwnerInvitationRequiredException extends DomainException {

    public OwnerInvitationRequiredException() {
        super("A pitch owner invitation code is required.");
    }
}
