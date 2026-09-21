package com.peladinhas.backend.domains.matches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMatchRequest(
        @NotNull UUID groupId,
        @NotNull OffsetDateTime startsAt,
        @NotNull @Positive Integer durationMinutes,
        @NotNull @Positive Integer maxPlayers,
        @NotBlank String joinMode,
        @NotNull Boolean publicVacanciesEnabled) {
}
