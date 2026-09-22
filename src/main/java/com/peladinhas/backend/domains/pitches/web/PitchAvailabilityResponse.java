package com.peladinhas.backend.domains.pitches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PitchAvailabilityResponse(
        UUID pitchId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean available) {
}
