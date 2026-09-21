package com.peladinhas.backend.domains.matches.web;

import jakarta.validation.constraints.NotBlank;

public record TransitionMatchStatusRequest(@NotBlank String nextStatus) {
}
