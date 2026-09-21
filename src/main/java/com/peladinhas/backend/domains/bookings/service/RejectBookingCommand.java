package com.peladinhas.backend.domains.bookings.service;

import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionReason;

public record RejectBookingCommand(
        UUID bookingId,
        UUID actingOwnerUserId,
        BookingRejectionReason reasonCode,
        String explanation) {
}
