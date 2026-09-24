package com.peladinhas.backend.domains.funding.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_funding_contributions")
public class FundingContributionEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private BookingEntity booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private UserEntity actorUser;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Convert(converter = FundingContributionPurpose.ConverterImpl.class)
    @Column(name = "purpose", nullable = false, columnDefinition = "varchar")
    private FundingContributionPurpose purpose;

    @Convert(converter = FundingContributionState.ConverterImpl.class)
    @Column(name = "state", nullable = false, columnDefinition = "varchar")
    private FundingContributionState state;

    @Column(name = "external_provider", columnDefinition = "varchar")
    private String externalProvider;

    @Column(name = "external_payment_reference", columnDefinition = "varchar")
    private String externalPaymentReference;

    @Column(name = "idempotency_key", columnDefinition = "varchar")
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "settled_at", columnDefinition = "timestamptz")
    private OffsetDateTime settledAt;

    public MatchEntity getMatch() {
        return match;
    }

    public void setMatch(final MatchEntity match) {
        this.match = match;
    }

    public BookingEntity getBooking() {
        return booking;
    }

    public void setBooking(final BookingEntity booking) {
        this.booking = booking;
    }

    public UserEntity getActorUser() {
        return actorUser;
    }

    public void setActorUser(final UserEntity actorUser) {
        this.actorUser = actorUser;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public FundingContributionPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(final FundingContributionPurpose purpose) {
        this.purpose = purpose;
    }

    public FundingContributionState getState() {
        return state;
    }

    public void setState(final FundingContributionState state) {
        this.state = state;
    }

    public String getExternalProvider() {
        return externalProvider;
    }

    public void setExternalProvider(final String externalProvider) {
        this.externalProvider = externalProvider;
    }

    public String getExternalPaymentReference() {
        return externalPaymentReference;
    }

    public void setExternalPaymentReference(final String externalPaymentReference) {
        this.externalPaymentReference = externalPaymentReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(final String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(final OffsetDateTime settledAt) {
        this.settledAt = settledAt;
    }
}
