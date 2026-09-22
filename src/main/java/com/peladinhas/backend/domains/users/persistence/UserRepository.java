package com.peladinhas.backend.domains.users.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByAuthProviderAndAuthSubject(String authProvider, String authSubject);

    boolean existsByEmail(String email);

    boolean existsByAuthProviderAndAuthSubject(String authProvider, String authSubject);
}
