package com.peladinhas.backend.domains.matches.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipantEntity, UUID> {

    Optional<MatchParticipantEntity> findByMatch_IdAndUser_Id(UUID matchId, UUID userId);

    long countByMatch_IdAndStatusIn(UUID matchId, Collection<MatchParticipantStatus> statuses);

    @EntityGraph(attributePaths = "user")
    List<MatchParticipantEntity> findAllByMatch_IdOrderByJoinedAtAscIdAsc(UUID matchId);
}
