package com.peladinhas.backend.domains.payments.persistence;

import java.math.BigDecimal;
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
@Table(name = "refunds")
public class RefundEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Convert(converter = RefundReason.ConverterImpl.class)
    @Column(name = "reason_code", nullable = false, columnDefinition = "varchar")
    private RefundReason reasonCode;

    @Convert(converter = RefundStatus.ConverterImpl.class)
    @Column(name = "status", nullable = false, columnDefinition = "varchar")
    private RefundStatus status;

    @Column(name = "provider_refund_id", columnDefinition = "varchar")
    private String providerRefundId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "processed_at", columnDefinition = "timestamptz")
    private OffsetDateTime processedAt;

    public PaymentEntity getPayment() {
        return payment;
    }

    public void setPayment(final PaymentEntity payment) {
        this.payment = payment;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public RefundReason getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(final RefundReason reasonCode) {
        this.reasonCode = reasonCode;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(final RefundStatus status) {
        this.status = status;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public void setProviderRefundId(final String providerRefundId) {
        this.providerRefundId = providerRefundId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(final OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
