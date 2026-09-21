package com.peladinhas.backend.domains.pitches.service;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePitchCommand(
        UUID ownerUserId,
        String name,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String timezone,
        BigDecimal basePrice,
        String currency,
        boolean active) {
}
