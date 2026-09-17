package com.peladinhas.backend.domains.matches.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum MatchJoinMode implements DatabaseEnum {
    OPEN_JOIN("open_join"),
    REQUEST_TO_JOIN("request_to_join");

    private final String value;

    MatchJoinMode(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<MatchJoinMode> {
        public ConverterImpl() {
            super(MatchJoinMode.class);
        }
    }
}
