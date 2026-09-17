package com.peladinhas.backend.domains.matches.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MatchAdminId implements Serializable {

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public MatchAdminId() {
    }

    public MatchAdminId(final UUID matchId, final UUID userId) {
        this.matchId = matchId;
        this.userId = userId;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(final UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MatchAdminId that)) {
            return false;
        }
        return Objects.equals(matchId, that.matchId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, userId);
    }
}
