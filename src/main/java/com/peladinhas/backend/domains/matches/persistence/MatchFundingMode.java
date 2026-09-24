package com.peladinhas.backend.domains.matches.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum MatchFundingMode implements DatabaseEnum {
    ORGANIZER_PREPAID("organizer_prepaid"),
    SPLIT_PAYMENT("split_payment");

    private final String value;

    MatchFundingMode(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<MatchFundingMode> {
        public ConverterImpl() {
            super(MatchFundingMode.class);
        }
    }
}
