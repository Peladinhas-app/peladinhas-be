package com.peladinhas.backend.domains.bookings.web;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Carries temporary provisional pricing inputs until server-side pricing rules are approved.
 */
public record CreateProvisionalBookingRequest(
        @NotNull UUID matchId,
        @NotNull UUID pitchId,
        @NotNull @PositiveOrZero BigDecimal totalPrice,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {
}
