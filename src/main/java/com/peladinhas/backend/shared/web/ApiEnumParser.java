package com.peladinhas.backend.shared.web;

import java.util.Arrays;
import java.util.function.Function;

import com.peladinhas.backend.shared.persistence.DatabaseEnum;

public final class ApiEnumParser {

    private ApiEnumParser() {
    }

    public static <T extends Enum<T> & DatabaseEnum> T parse(
            final Class<T> type,
            final String value,
            final String fieldName) {
        return Arrays.stream(type.getEnumConstants())
                .filter(candidate -> candidate.value().equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidApiRequestException("Invalid value for " + fieldName + "."));
    }

    public static <T extends DatabaseEnum> String value(final T value) {
        return value.value();
    }
}