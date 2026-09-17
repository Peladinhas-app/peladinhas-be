package com.peladinhas.backend.domains.notifications.persistence;

import java.time.OffsetDateTime;
import java.util.Map;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class NotificationEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "type", nullable = false, columnDefinition = "varchar")
    private String type;

    @Column(name = "related_entity_type", columnDefinition = "varchar")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private java.util.UUID relatedEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "read_at", columnDefinition = "timestamptz")
    private OffsetDateTime readAt;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(final UserEntity user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(final String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public java.util.UUID getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(final java.util.UUID relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(final Map<String, Object> payload) {
        this.payload = payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(final OffsetDateTime readAt) {
        this.readAt = readAt;
    }
}
