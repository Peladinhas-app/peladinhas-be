package com.peladinhas.backend.domains.matches.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;

public record CreateMatchCommand(
        UUID groupId,
        UUID creatorUserId,
        OffsetDateTime startsAt,
        int durationMinutes,
        int maxPlayers,
        MatchJoinMode joinMode,
        boolean publicVacanciesEnabled) {
}
