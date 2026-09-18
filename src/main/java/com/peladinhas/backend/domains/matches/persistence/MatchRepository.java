package com.peladinhas.backend.domains.matches.persistence;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MatchEntity m where m.id = :id")
    Optional<MatchEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select m from MatchEntity m join fetch m.group g join fetch m.createdByUser where m.id = :id")
    Optional<MatchEntity> findByIdWithSummary(@Param("id") UUID id);

    boolean existsByGroup_IdAndStatusInAndEndsAtAfter(
            UUID groupId,
            Collection<MatchStatus> statuses,
            OffsetDateTime currentTime);
}