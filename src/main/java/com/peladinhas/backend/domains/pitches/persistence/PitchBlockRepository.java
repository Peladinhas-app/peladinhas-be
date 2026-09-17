package com.peladinhas.backend.domains.pitches.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PitchBlockRepository extends JpaRepository<PitchBlockEntity, UUID> {
}
