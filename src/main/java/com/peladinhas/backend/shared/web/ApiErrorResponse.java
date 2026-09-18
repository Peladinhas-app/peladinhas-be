package com.peladinhas.backend.shared.web;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        List<FieldErrorResponse> fieldErrors) {
}