package com.peladinhas.backend.domains.matches.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchFundingMode;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;

public record CreateDirectMatchCommand(
        UUID creatorUserId,
        String groupName,
        String groupDescription,
        OffsetDateTime startsAt,
        int durationMinutes,
        int maxPlayers,
        MatchJoinMode joinMode,
        boolean publicVacanciesEnabled,
        MatchFundingMode fundingMode) {

    public CreateDirectMatchCommand(
            final UUID creatorUserId,
            final String groupName,
            final String groupDescription,
            final OffsetDateTime startsAt,
            final int durationMinutes,
            final int maxPlayers,
            final MatchJoinMode joinMode,
            final boolean publicVacanciesEnabled) {
        this(creatorUserId, groupName, groupDescription, startsAt, durationMinutes, maxPlayers, joinMode,
                publicVacanciesEnabled, MatchFundingMode.SPLIT_PAYMENT);
    }
}
