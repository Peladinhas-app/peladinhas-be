package com.peladinhas.backend.domains.matches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.shared.web.ApiEnumParser;

public record ParticipantResponse(
        UUID id,
        UUID matchId,
        UUID userId,
        String status,
        OffsetDateTime joinedAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt) {

    public static ParticipantResponse from(final MatchParticipantEntity participant) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getMatch().getId(),
                participant.getUser().getId(),
                ApiEnumParser.value(participant.getStatus()),
                participant.getJoinedAt(),
                participant.getConfirmedAt(),
                participant.getCancelledAt());
    }
}