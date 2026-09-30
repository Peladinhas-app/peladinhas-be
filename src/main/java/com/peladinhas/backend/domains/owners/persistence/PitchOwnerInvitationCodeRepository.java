package com.peladinhas.backend.domains.owners.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PitchOwnerInvitationCodeRepository extends JpaRepository<PitchOwnerInvitationCodeEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from PitchOwnerInvitationCodeEntity invitation where invitation.codeHash = :codeHash")
    Optional<PitchOwnerInvitationCodeEntity> findByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
