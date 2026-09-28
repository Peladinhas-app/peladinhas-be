package com.peladinhas.backend.health;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final Clock clock;

    public HealthController(final Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", OffsetDateTime.now(clock));
    }
}
