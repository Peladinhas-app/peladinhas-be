package com.peladinhas.backend.domains.matches.web;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransitionMatchStatusRequest(
        @NotNull UUID actingAdminUserId,
        @NotBlank String nextStatus) {
}
