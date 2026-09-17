package com.peladinhas.backend.domains.payments.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum RefundReason implements DatabaseEnum {
    PLAYER_CANCELLED("player_cancelled"),
    MATCH_CANCELLED("match_cancelled"),
    BOOKING_REJECTED("booking_rejected"),
    BOOKING_LOST("booking_lost"),
    OTHER("other");

    private final String value;

    RefundReason(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<RefundReason> {
        public ConverterImpl() {
            super(RefundReason.class);
        }
    }
}
