package com.peladinhas.backend.domains.groups.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum GroupVisibility implements DatabaseEnum {
    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    GroupVisibility(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<GroupVisibility> {
        public ConverterImpl() {
            super(GroupVisibility.class);
        }
    }
}
