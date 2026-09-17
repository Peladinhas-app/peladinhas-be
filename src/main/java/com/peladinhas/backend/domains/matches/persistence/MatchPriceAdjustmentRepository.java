package com.peladinhas.backend.domains.matches.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPriceAdjustmentRepository extends JpaRepository<MatchPriceAdjustmentEntity, UUID> {
}
