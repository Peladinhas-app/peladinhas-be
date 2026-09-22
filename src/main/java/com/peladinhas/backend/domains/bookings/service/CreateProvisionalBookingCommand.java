package com.peladinhas.backend.domains.bookings.service;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProvisionalBookingCommand(
        UUID actingUserId,
        UUID matchId,
        UUID pitchId,
        BigDecimal totalPrice,
        String currency) {
}
