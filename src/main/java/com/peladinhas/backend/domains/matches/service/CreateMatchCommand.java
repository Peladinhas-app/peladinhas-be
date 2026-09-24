package com.peladinhas.backend.domains.matches.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchFundingMode;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;

public record CreateMatchCommand(
        UUID groupId,
        UUID creatorUserId,
        OffsetDateTime startsAt,
        int durationMinutes,
        int maxPlayers,
        MatchJoinMode joinMode,
        boolean publicVacanciesEnabled,
        MatchFundingMode fundingMode) {

    public CreateMatchCommand(
            final UUID groupId,
            final UUID creatorUserId,
            final OffsetDateTime startsAt,
            final int durationMinutes,
            final int maxPlayers,
            final MatchJoinMode joinMode,
            final boolean publicVacanciesEnabled) {
        this(groupId, creatorUserId, startsAt, durationMinutes, maxPlayers, joinMode, publicVacanciesEnabled,
                MatchFundingMode.SPLIT_PAYMENT);
    }
}
