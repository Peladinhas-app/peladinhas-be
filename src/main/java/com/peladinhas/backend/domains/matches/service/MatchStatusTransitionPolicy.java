package com.peladinhas.backend.domains.matches.service;

import java.util.Map;
import java.util.Set;

import com.peladinhas.backend.domains.matches.persistence.MatchStatus;

final class MatchStatusTransitionPolicy {

    private static final Map<MatchStatus, Set<MatchStatus>> VALID_TRANSITIONS = Map.of(
            MatchStatus.DRAFT, Set.of(MatchStatus.RECRUITING, MatchStatus.CANCELLED),
            MatchStatus.RECRUITING, Set.of(MatchStatus.READY, MatchStatus.CANCELLED),
            MatchStatus.READY, Set.of(MatchStatus.RECRUITING, MatchStatus.COMPLETED, MatchStatus.CANCELLED));

    private MatchStatusTransitionPolicy() {
    }

    static void requireValid(final MatchStatus currentStatus, final MatchStatus nextStatus) {
        if (!VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus)) {
            throw new InvalidMatchTransitionException("Match status transition is not valid.");
        }
    }
}
