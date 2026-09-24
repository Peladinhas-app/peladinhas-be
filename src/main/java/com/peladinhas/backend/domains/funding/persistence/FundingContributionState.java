package com.peladinhas.backend.domains.funding.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum FundingContributionState implements DatabaseEnum {
    PENDING("pending"),
    SETTLED("settled"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    REVERSED("reversed");

    private final String value;

    FundingContributionState(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<FundingContributionState> {
        public ConverterImpl() {
            super(FundingContributionState.class);
        }
    }
}
