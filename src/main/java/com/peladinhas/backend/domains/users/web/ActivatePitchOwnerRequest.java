package com.peladinhas.backend.domains.users.web;

import jakarta.validation.constraints.NotBlank;

public record ActivatePitchOwnerRequest(@NotBlank String invitationCode) {
}
