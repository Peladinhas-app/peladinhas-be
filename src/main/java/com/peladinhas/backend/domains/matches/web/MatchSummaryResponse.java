package com.peladinhas.backend.domains.matches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchSummaryResponse(
        UUID matchId,
        String displayName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer durationMinutes,
        Integer maxPlayers,
        Long occupiedPlaces,
        Integer availablePlaces,
        String status,
        String joinMode,
        Boolean viewerIsOrganizer,
        String viewerParticipationStatus) {
}
