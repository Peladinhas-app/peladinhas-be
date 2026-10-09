package com.peladinhas.backend.domains.matches.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MatchManagementResponse(
        UUID matchId,
        String displayName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer durationMinutes,
        String status,
        String joinMode,
        String fundingMode,
        String fundingState,
        Integer maxPlayers,
        Long occupiedPlaces,
        Integer availablePlaces,
        List<MatchParticipantSummaryResponse> participants,
        List<MatchParticipantSummaryResponse> pendingRequests) {
}
