package com.peladinhas.backend.domains.matches.service;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;

public record MatchDiscoveryCriteria(
        UUID userId,
        OffsetDateTime now,
        String area,
        OffsetDateTime startsFrom,
        OffsetDateTime startsTo,
        LocalTime timeFrom,
        LocalTime timeTo,
        MatchJoinMode joinMode,
        String fallbackTimeZone,
        boolean availableOnly,
        int page,
        int size) {
}