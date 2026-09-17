package com.peladinhas.backend.shared.persistence;

import java.util.Arrays;

import jakarta.persistence.AttributeConverter;

public abstract class AbstractDatabaseEnumConverter<E extends Enum<E> & DatabaseEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected AbstractDatabaseEnumConverter(final Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(final E attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public E convertToEntityAttribute(final String dbData) {
        if (dbData == null) {
            return null;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .filter(value -> value.value().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown %s database value: %s".formatted(enumType.getSimpleName(), dbData)));
    }
}
