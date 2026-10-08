package com.peladinhas.backend.domains.matches.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchDiscoveryResponse(
        UUID matchId,
        String displayName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer durationMinutes,
        Integer maxPlayers,
        Long occupiedPlaces,
        Integer availablePlaces,
        UUID pitchId,
        String pitchName,
        String pitchAddress,
        BigDecimal pitchBasePrice,
        String pitchCurrency,
        String groupVisibility,
        String joinMode) {
}
