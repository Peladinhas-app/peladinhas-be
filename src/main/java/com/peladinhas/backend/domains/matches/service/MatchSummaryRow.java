package com.peladinhas.backend.domains.matches.service;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchSummaryRow(
        UUID matchId,
        String displayName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer maxPlayers,
        Long occupiedPlaces,
        String matchStatus,
        String joinMode,
        Boolean viewerIsOrganizer,
        String viewerParticipationStatus) {
}
