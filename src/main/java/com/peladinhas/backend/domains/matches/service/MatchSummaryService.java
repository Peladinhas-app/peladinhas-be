package com.peladinhas.backend.domains.matches.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchSummaryRepository;
import com.peladinhas.backend.domains.matches.web.MatchSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchSummaryService {

    private final Clock clock;
    private final MatchSummaryRepository matchSummaryRepository;

    public MatchSummaryService(final Clock clock, final MatchSummaryRepository matchSummaryRepository) {
        this.clock = clock;
        this.matchSummaryRepository = matchSummaryRepository;
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> upcomingMatches(final UUID userId) {
        return matchSummaryRepository.findUpcomingForViewer(userId, OffsetDateTime.now(clock)).stream()
                .map(this::response)
                .toList();
    }

    private MatchSummaryResponse response(final MatchSummaryRow row) {
        int durationMinutes = (int) java.time.Duration.between(row.startsAt(), row.endsAt()).toMinutes();
        int occupiedPlaces = row.occupiedPlaces().intValue();
        int availablePlaces = Math.max(0, row.maxPlayers() - occupiedPlaces);
        return new MatchSummaryResponse(
                row.matchId(),
                row.displayName(),
                row.startsAt(),
                row.endsAt(),
                durationMinutes,
                row.maxPlayers(),
                row.occupiedPlaces(),
                availablePlaces,
                row.matchStatus(),
                row.joinMode(),
                row.viewerIsOrganizer(),
                row.viewerParticipationStatus());
    }
}
