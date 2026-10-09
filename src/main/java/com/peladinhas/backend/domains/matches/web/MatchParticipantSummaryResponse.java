package com.peladinhas.backend.domains.matches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.shared.web.ApiEnumParser;

public record MatchParticipantSummaryResponse(
        UUID participantId,
        UUID userId,
        String displayName,
        String status,
        OffsetDateTime joinedAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt) {

    public static MatchParticipantSummaryResponse from(final MatchParticipantEntity participant) {
        return new MatchParticipantSummaryResponse(
                participant.getId(),
                participant.getUser().getId(),
                participant.getUser().getName(),
                ApiEnumParser.value(participant.getStatus()),
                participant.getJoinedAt(),
                participant.getConfirmedAt(),
                participant.getCancelledAt());
    }
}
