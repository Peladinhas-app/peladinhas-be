package com.peladinhas.backend.domains.bookings.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum BookingStatus implements DatabaseEnum {
    PROVISIONAL("provisional"),
    CONFIRMED("confirmed"),
    LOST("lost"),
    REJECTED("rejected"),
    CANCELLED("cancelled");

    private final String value;

    BookingStatus(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<BookingStatus> {
        public ConverterImpl() {
            super(BookingStatus.class);
        }
    }
}
