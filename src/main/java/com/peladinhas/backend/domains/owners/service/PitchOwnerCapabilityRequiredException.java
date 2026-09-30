package com.peladinhas.backend.domains.owners.service;

import com.peladinhas.backend.shared.domain.DomainException;

public class PitchOwnerCapabilityRequiredException extends DomainException {

    public PitchOwnerCapabilityRequiredException() {
        super("Pitch owner capability is required for this operation.");
    }
}
