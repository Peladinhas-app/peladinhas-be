package com.peladinhas.backend.domains.groups.web;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest(
        @NotBlank String name,
        String description,
        @NotBlank String visibility) {
}
