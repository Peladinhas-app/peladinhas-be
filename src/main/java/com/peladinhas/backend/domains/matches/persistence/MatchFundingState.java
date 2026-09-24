package com.peladinhas.backend.domains.matches.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum MatchFundingState implements DatabaseEnum {
    COLLECTING("collecting"),
    FULLY_FUNDED("fully_funded"),
    BOOKING_CONFIRMED("booking_confirmed"),
    BOOKING_FAILED("booking_failed"),
    CANCELLED("cancelled");

    private final String value;

    MatchFundingState(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<MatchFundingState> {
        public ConverterImpl() {
            super(MatchFundingState.class);
        }
    }
}
