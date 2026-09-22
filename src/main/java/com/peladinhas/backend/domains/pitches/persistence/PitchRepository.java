package com.peladinhas.backend.domains.pitches.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PitchRepository extends JpaRepository<PitchEntity, UUID> {

    @EntityGraph(attributePaths = "ownerUser")
    Optional<PitchEntity> findWithOwnerUserById(UUID id);
}
