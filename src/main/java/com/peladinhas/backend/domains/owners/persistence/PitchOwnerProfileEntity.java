package com.peladinhas.backend.domains.owners.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pitch_owner_profiles")
public class PitchOwnerProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "activated_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime activatedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    public java.util.UUID getUserId() {
        return userId;
    }

    public void setUserId(final java.util.UUID userId) {
        this.userId = userId;
    }

    public void setUser(final UserEntity user) {
        this.userId = user == null ? null : user.getId();
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(final OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
