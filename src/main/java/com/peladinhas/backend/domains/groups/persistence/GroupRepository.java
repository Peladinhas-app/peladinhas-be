package com.peladinhas.backend.domains.groups.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<GroupEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GroupEntity g where g.id = :id")
    Optional<GroupEntity> findByIdForUpdate(@Param("id") UUID id);
}
