package com.peladinhas.backend.domains.bookings.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum BookingRejectionReason implements DatabaseEnum {
    MAINTENANCE("maintenance"),
    SCHEDULING_CONFLICT("scheduling_conflict"),
    PRIVATE_EVENT("private_event"),
    PITCH_UNAVAILABLE("pitch_unavailable"),
    OTHER("other");

    private final String value;

    BookingRejectionReason(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<BookingRejectionReason> {
        public ConverterImpl() {
            super(BookingRejectionReason.class);
        }
    }
}
