package com.peladinhas.backend.domains.pitches.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdatePitchRequest(
        @NotBlank String name,
        String description,
        @NotBlank String address,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotBlank String timezone,
        @PositiveOrZero BigDecimal basePrice,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull Boolean active) {
}
