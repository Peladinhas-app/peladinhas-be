package com.peladinhas.backend.domains.matches.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAdminRepository extends JpaRepository<MatchAdminEntity, MatchAdminId> {
}
