package com.peladinhas.backend.domains.bookings.web;

import jakarta.validation.constraints.NotBlank;

public record RejectBookingRequest(
        @NotBlank String reasonCode,
        String explanation) {
}
