package com.peladinhas.backend.domains.owners.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pitch_owner_invitation_codes")
public class PitchOwnerInvitationCodeEntity extends AbstractUuidEntity {

    @Column(name = "code_hash", nullable = false, columnDefinition = "varchar")
    private String codeHash;

    @Column(name = "expires_at", columnDefinition = "timestamptz")
    private OffsetDateTime expiresAt;

    @Column(name = "used_at", columnDefinition = "timestamptz")
    private OffsetDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private UserEntity usedByUser;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(final String codeHash) {
        this.codeHash = codeHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(final OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(final OffsetDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public UserEntity getUsedByUser() {
        return usedByUser;
    }

    public void setUsedByUser(final UserEntity usedByUser) {
        this.usedByUser = usedByUser;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(final OffsetDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
