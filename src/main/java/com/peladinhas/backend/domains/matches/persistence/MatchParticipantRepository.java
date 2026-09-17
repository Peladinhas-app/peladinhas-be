package com.peladinhas.backend.domains.matches.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipantEntity, UUID> {

    Optional<MatchParticipantEntity> findByMatch_IdAndUser_Id(UUID matchId, UUID userId);

    long countByMatch_IdAndStatusIn(UUID matchId, Collection<MatchParticipantStatus> statuses);
}
