package com.peladinhas.backend.domains.pitches.web;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record CreatePitchBlockRequest(
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        String reasonCode,
        String note) {
}
