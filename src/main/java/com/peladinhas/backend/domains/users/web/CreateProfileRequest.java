package com.peladinhas.backend.domains.users.web;

import jakarta.validation.constraints.NotBlank;

public record CreateProfileRequest(
        @NotBlank String name,
        @NotBlank String preferredLanguage,
        String accountType,
        String ownerInvitationCode) {
}
