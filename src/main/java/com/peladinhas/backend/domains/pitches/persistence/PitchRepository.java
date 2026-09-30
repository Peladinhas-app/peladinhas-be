package com.peladinhas.backend.domains.pitches.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PitchRepository extends JpaRepository<PitchEntity, UUID> {

    @EntityGraph(attributePaths = "ownerUser")
    Optional<PitchEntity> findWithOwnerUserById(UUID id);

    @EntityGraph(attributePaths = "ownerUser")
    List<PitchEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PitchEntity p where p.id = :id")
    Optional<PitchEntity> findByIdForUpdate(@Param("id") UUID id);
}
