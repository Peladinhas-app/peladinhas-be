package com.peladinhas.backend.domains.pitches.service;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePitchBlockCommand(
        UUID pitchId,
        UUID actingUserId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String reasonCode,
        String note) {
}
