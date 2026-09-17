package com.peladinhas.backend.domains.payments.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum RefundStatus implements DatabaseEnum {
    PENDING("pending"),
    PROCESSED("processed"),
    FAILED("failed");

    private final String value;

    RefundStatus(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<RefundStatus> {
        public ConverterImpl() {
            super(RefundStatus.class);
        }
    }
}
