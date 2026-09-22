package com.peladinhas.backend.domains.pitches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.pitches.persistence.PitchBlockEntity;

public record PitchBlockResponse(
        UUID id,
        UUID pitchId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String reasonCode,
        String note,
        OffsetDateTime createdAt) {

    public static PitchBlockResponse from(final PitchBlockEntity block) {
        return new PitchBlockResponse(
                block.getId(),
                block.getPitch().getId(),
                block.getStartsAt(),
                block.getEndsAt(),
                block.getReasonCode(),
                block.getNote(),
                block.getCreatedAt());
    }
}
