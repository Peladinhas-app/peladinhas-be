package com.peladinhas.backend.domains.groups.web;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGroupRequest(
        @NotNull UUID creatorUserId,
        @NotBlank String name,
        String description,
        @NotBlank String visibility) {
}