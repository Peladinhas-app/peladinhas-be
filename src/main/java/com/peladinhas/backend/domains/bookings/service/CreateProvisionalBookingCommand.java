package com.peladinhas.backend.domains.bookings.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateProvisionalBookingCommand(
        UUID matchId,
        UUID pitchId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BigDecimal totalPrice,
        String currency) {
}
