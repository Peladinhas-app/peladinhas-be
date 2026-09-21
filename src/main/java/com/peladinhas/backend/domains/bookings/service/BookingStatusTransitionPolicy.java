package com.peladinhas.backend.domains.bookings.service;

import java.util.Map;
import java.util.Set;

import com.peladinhas.backend.domains.bookings.persistence.BookingStatus;

public final class BookingStatusTransitionPolicy {

    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.of(
            BookingStatus.PROVISIONAL,
            Set.of(
                    BookingStatus.CONFIRMED,
                    BookingStatus.LOST,
                    BookingStatus.REJECTED,
                    BookingStatus.CANCELLED),
            BookingStatus.CONFIRMED,
            Set.of(BookingStatus.CANCELLED));

    private BookingStatusTransitionPolicy() {
    }

    public static void requireValid(final BookingStatus current, final BookingStatus next) {
        if (!VALID_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new InvalidBookingTransitionException("Booking status transition is not valid.");
        }
    }
}
