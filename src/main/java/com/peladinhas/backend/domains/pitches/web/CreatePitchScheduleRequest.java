package com.peladinhas.backend.domains.pitches.web;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePitchScheduleRequest(
        @NotNull @Min(1) @Max(7) Short dayOfWeek,
        @NotNull LocalTime startsAt,
        @NotNull LocalTime endsAt) {
}
