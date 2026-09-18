package com.peladinhas.backend.domains.matches.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AdminActionRequest(@NotNull UUID actingAdminUserId) {
}