package com.peladinhas.backend.domains.matches.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAdminRepository extends JpaRepository<MatchAdminEntity, MatchAdminId> {

    boolean existsByMatch_IdAndUser_Id(UUID matchId, UUID userId);
}
