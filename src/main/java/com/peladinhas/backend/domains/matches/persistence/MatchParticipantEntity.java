package com.peladinhas.backend.domains.matches.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "match_participants")
public class MatchParticipantEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Convert(converter = MatchParticipantStatus.ConverterImpl.class)
    @Column(name = "status", nullable = false, columnDefinition = "varchar")
    private MatchParticipantStatus status;

    @Column(name = "joined_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime joinedAt;

    @Column(name = "confirmed_at", columnDefinition = "timestamptz")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancelled_at", columnDefinition = "timestamptz")
    private OffsetDateTime cancelledAt;

    public MatchEntity getMatch() {
        return match;
    }

    public void setMatch(final MatchEntity match) {
        this.match = match;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(final UserEntity user) {
        this.user = user;
    }

    public MatchParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(final MatchParticipantStatus status) {
        this.status = status;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(final OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(final OffsetDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(final OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
