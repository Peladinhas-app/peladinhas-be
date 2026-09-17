package com.peladinhas.backend.domains.matches.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum MatchStatus implements DatabaseEnum {
    DRAFT("draft"),
    RECRUITING("recruiting"),
    READY("ready"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    MatchStatus(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<MatchStatus> {
        public ConverterImpl() {
            super(MatchStatus.class);
        }
    }
}
