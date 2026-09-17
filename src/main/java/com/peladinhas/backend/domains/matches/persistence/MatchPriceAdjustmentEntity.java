package com.peladinhas.backend.domains.matches.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
@Table(name = "match_price_adjustments")
public class MatchPriceAdjustmentEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @Column(name = "old_player_count", nullable = false)
    private Integer oldPlayerCount;

    @Column(name = "new_player_count", nullable = false)
    private Integer newPlayerCount;

    @Column(name = "old_price_per_player", nullable = false, precision = 10, scale = 2)
    private BigDecimal oldPricePerPlayer;

    @Column(name = "new_price_per_player", nullable = false, precision = 10, scale = 2)
    private BigDecimal newPricePerPlayer;

    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdByUser;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    public MatchEntity getMatch() {
        return match;
    }

    public void setMatch(final MatchEntity match) {
        this.match = match;
    }

    public Integer getOldPlayerCount() {
        return oldPlayerCount;
    }

    public void setOldPlayerCount(final Integer oldPlayerCount) {
        this.oldPlayerCount = oldPlayerCount;
    }

    public Integer getNewPlayerCount() {
        return newPlayerCount;
    }

    public void setNewPlayerCount(final Integer newPlayerCount) {
        this.newPlayerCount = newPlayerCount;
    }

    public BigDecimal getOldPricePerPlayer() {
        return oldPricePerPlayer;
    }

    public void setOldPricePerPlayer(final BigDecimal oldPricePerPlayer) {
        this.oldPricePerPlayer = oldPricePerPlayer;
    }

    public BigDecimal getNewPricePerPlayer() {
        return newPricePerPlayer;
    }

    public void setNewPricePerPlayer(final BigDecimal newPricePerPlayer) {
        this.newPricePerPlayer = newPricePerPlayer;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public UserEntity getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(final UserEntity createdByUser) {
        this.createdByUser = createdByUser;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
