package com.peladinhas.backend.domains.matches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.shared.web.ApiEnumParser;

public record MatchResponse(
        UUID id,
        UUID groupId,
        String groupName,
        UUID createdByUserId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Integer maxPlayers,
        String joinMode,
        String status,
        String fundingMode,
        String fundingState,
        Boolean publicVacanciesEnabled) {

    public static MatchResponse from(final MatchEntity match) {
        return new MatchResponse(
                match.getId(),
                match.getGroup().getId(),
                match.getGroup().getName(),
                match.getCreatedByUser().getId(),
                match.getStartsAt(),
                match.getEndsAt(),
                match.getMaxPlayers(),
                ApiEnumParser.value(match.getJoinMode()),
                ApiEnumParser.value(match.getStatus()),
                ApiEnumParser.value(match.getFundingMode()),
                ApiEnumParser.value(match.getFundingState()),
                match.getPublicVacanciesEnabled());
    }
}
