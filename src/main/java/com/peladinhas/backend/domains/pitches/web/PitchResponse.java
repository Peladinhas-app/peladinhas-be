package com.peladinhas.backend.domains.pitches.web;

import java.math.BigDecimal;
import java.util.UUID;

import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;

public record PitchResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String timezone,
        BigDecimal basePrice,
        String currency,
        Boolean active) {

    public static PitchResponse from(final PitchEntity pitch) {
        return new PitchResponse(
                pitch.getId(),
                pitch.getOwnerUser().getId(),
                pitch.getName(),
                pitch.getDescription(),
                pitch.getAddress(),
                pitch.getLatitude(),
                pitch.getLongitude(),
                pitch.getTimezone(),
                pitch.getBasePrice(),
                pitch.getCurrency(),
                pitch.getActive());
    }
}
