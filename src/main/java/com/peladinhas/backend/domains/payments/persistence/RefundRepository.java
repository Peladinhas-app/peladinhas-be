package com.peladinhas.backend.domains.payments.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
}
