package com.peladinhas.backend.domains.chats.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum ChatType implements DatabaseEnum {
    GROUP("group"),
    MATCH("match"),
    BOOKING("booking");

    private final String value;

    ChatType(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<ChatType> {
        public ConverterImpl() {
            super(ChatType.class);
        }
    }
}
