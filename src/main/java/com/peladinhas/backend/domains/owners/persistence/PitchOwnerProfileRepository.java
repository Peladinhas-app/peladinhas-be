package com.peladinhas.backend.domains.owners.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PitchOwnerProfileRepository extends JpaRepository<PitchOwnerProfileEntity, UUID> {
}
