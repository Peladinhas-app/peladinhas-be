package com.peladinhas.backend.domains.bookings.persistence;

import java.time.OffsetDateTime;

import com.peladinhas.backend.shared.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_rejections")
public class BookingRejectionEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingEntity booking;

    @Convert(converter = BookingRejectionReason.ConverterImpl.class)
    @Column(name = "reason_code", nullable = false, columnDefinition = "varchar")
    private BookingRejectionReason reasonCode;

    @Column(name = "explanation", columnDefinition = "text")
    private String explanation;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    public BookingEntity getBooking() {
        return booking;
    }

    public void setBooking(final BookingEntity booking) {
        this.booking = booking;
    }

    public BookingRejectionReason getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(final BookingRejectionReason reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(final String explanation) {
        this.explanation = explanation;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
