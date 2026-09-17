package com.peladinhas.backend.domains.payments.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum PaymentType implements DatabaseEnum {
    INITIAL("initial"),
    TOP_UP("top_up");

    private final String value;

    PaymentType(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<PaymentType> {
        public ConverterImpl() {
            super(PaymentType.class);
        }
    }
}
