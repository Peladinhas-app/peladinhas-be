package com.peladinhas.backend.domains.matches.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantStatus;
import com.peladinhas.backend.domains.matches.web.MatchManagementResponse;
import com.peladinhas.backend.domains.matches.web.MatchParticipantSummaryResponse;
import com.peladinhas.backend.shared.web.ApiEnumParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchManagementService {

    private static final Set<MatchParticipantStatus> CAPACITY_STATUSES = Set.of(
            MatchParticipantStatus.APPROVED,
            MatchParticipantStatus.AWAITING_PAYMENT,
            MatchParticipantStatus.CONFIRMED);

    private final MatchParticipantRepository participantRepository;
    private final MatchService matchService;

    public MatchManagementService(
            final MatchParticipantRepository participantRepository,
            final MatchService matchService) {
        this.participantRepository = participantRepository;
        this.matchService = matchService;
    }

    @Transactional(readOnly = true)
    public MatchManagementResponse managementSummary(final UUID matchId, final UUID actingUserId) {
        matchService.requireMatchAdmin(matchId, actingUserId);
        MatchEntity match = matchService.requireMatchSummary(matchId);
        List<MatchParticipantEntity> participants = participantRepository.findAllByMatch_IdOrderByJoinedAtAscIdAsc(matchId);
        long occupiedPlaces = participants.stream()
                .filter(participant -> CAPACITY_STATUSES.contains(participant.getStatus()))
                .count();
        int availablePlaces = Math.max(0, match.getMaxPlayers() - (int) occupiedPlaces);
        List<MatchParticipantSummaryResponse> participantResponses = participants.stream()
                .map(MatchParticipantSummaryResponse::from)
                .toList();
        List<MatchParticipantSummaryResponse> pendingRequests = participants.stream()
                .filter(participant -> participant.getStatus() == MatchParticipantStatus.REQUESTED)
                .map(MatchParticipantSummaryResponse::from)
                .toList();

        return new MatchManagementResponse(
                match.getId(),
                match.getGroup().getName(),
                match.getStartsAt(),
                match.getEndsAt(),
                (int) java.time.Duration.between(match.getStartsAt(), match.getEndsAt()).toMinutes(),
                ApiEnumParser.value(match.getStatus()),
                ApiEnumParser.value(match.getJoinMode()),
                ApiEnumParser.value(match.getFundingMode()),
                ApiEnumParser.value(match.getFundingState()),
                match.getMaxPlayers(),
                occupiedPlaces,
                availablePlaces,
                participantResponses,
                pendingRequests);
    }
}
