package com.peladinhas.backend.health;

import java.time.OffsetDateTime;

public record HealthResponse(String status, OffsetDateTime timestamp) {
}
