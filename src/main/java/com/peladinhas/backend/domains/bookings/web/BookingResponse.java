package com.peladinhas.backend.domains.bookings.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.shared.web.ApiEnumParser;

public record BookingResponse(
        UUID id,
        UUID matchId,
        UUID pitchId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BigDecimal totalPrice,
        String currency,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime rejectedAt,
        OffsetDateTime cancelledAt) {

    public static BookingResponse from(final BookingEntity booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getMatch().getId(),
                booking.getPitch().getId(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.getTotalPrice(),
                booking.getCurrency(),
                ApiEnumParser.value(booking.getStatus()),
                booking.getCreatedAt(),
                booking.getConfirmedAt(),
                booking.getRejectedAt(),
                booking.getCancelledAt());
    }
}
