package com.peladinhas.backend.domains.payments.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum PaymentStatus implements DatabaseEnum {
    PENDING("pending"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String value;

    PaymentStatus(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<PaymentStatus> {
        public ConverterImpl() {
            super(PaymentStatus.class);
        }
    }
}
