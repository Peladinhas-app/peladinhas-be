package com.peladinhas.backend.domains.users.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum PreferredLanguage implements DatabaseEnum {
    PORTUGUESE("pt"),
    ENGLISH("en");

    private final String value;

    PreferredLanguage(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<PreferredLanguage> {
        public ConverterImpl() {
            super(PreferredLanguage.class);
        }
    }
}
