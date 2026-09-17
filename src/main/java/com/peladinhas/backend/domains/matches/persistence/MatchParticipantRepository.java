package com.peladinhas.backend.domains.matches.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipantEntity, UUID> {
}
