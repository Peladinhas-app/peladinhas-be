package com.peladinhas.backend.domains.matches.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchDiscoveryRow(
        UUID matchId,
        String displayName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer maxPlayers,
        Long occupiedPlaces,
        UUID pitchId,
        String pitchName,
        String pitchAddress,
        BigDecimal pitchBasePrice,
        String pitchCurrency,
        String groupVisibility,
        String joinMode) {
}
