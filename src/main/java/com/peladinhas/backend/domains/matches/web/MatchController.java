package com.peladinhas.backend.domains.matches.web;

import java.util.UUID;

import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchStatus;
import com.peladinhas.backend.domains.matches.service.CreateDirectMatchCommand;
import com.peladinhas.backend.domains.matches.service.CreateMatchCommand;
import com.peladinhas.backend.domains.matches.service.MatchParticipationService;
import com.peladinhas.backend.domains.matches.service.MatchService;
import com.peladinhas.backend.shared.web.ApiEnumParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final CurrentUserService currentUserService;
    private final MatchParticipationService participationService;
    private final MatchService matchService;

    public MatchController(
            final CurrentUserService currentUserService,
            final MatchParticipationService participationService,
            final MatchService matchService) {
        this.currentUserService = currentUserService;
        this.participationService = participationService;
        this.matchService = matchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse createMatch(@Valid @RequestBody final CreateMatchRequest request) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        MatchJoinMode joinMode = ApiEnumParser.parse(MatchJoinMode.class, request.joinMode(), "joinMode");
        MatchEntity match = matchService.createMatch(new CreateMatchCommand(
                request.groupId(),
                currentUserId,
                request.startsAt(),
                request.durationMinutes(),
                request.maxPlayers(),
                joinMode,
                request.publicVacanciesEnabled()));
        return MatchResponse.from(match);
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse createDirectMatch(@Valid @RequestBody final CreateDirectMatchRequest request) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        MatchJoinMode joinMode = ApiEnumParser.parse(MatchJoinMode.class, request.joinMode(), "joinMode");
        MatchEntity match = matchService.createDirectMatch(new CreateDirectMatchCommand(
                currentUserId,
                request.groupName(),
                request.groupDescription(),
                request.startsAt(),
                request.durationMinutes(),
                request.maxPlayers(),
                joinMode,
                request.publicVacanciesEnabled()));
        return MatchResponse.from(match);
    }

    @GetMapping("/{matchId}")
    public MatchResponse getMatch(@PathVariable final UUID matchId) {
        return MatchResponse.from(matchService.requireMatchSummary(matchId));
    }

    @PostMapping("/{matchId}/status-transitions")
    public MatchResponse transitionMatchStatus(
            @PathVariable final UUID matchId,
            @Valid @RequestBody final TransitionMatchStatusRequest request) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        MatchStatus nextStatus = ApiEnumParser.parse(MatchStatus.class, request.nextStatus(), "nextStatus");
        matchService.transitionMatchStatus(matchId, currentUserId, nextStatus);
        return MatchResponse.from(matchService.requireMatchSummary(matchId));
    }

    @PostMapping("/{matchId}/join")
    public ParticipantResponse joinOpenMatch(@PathVariable final UUID matchId) {
        MatchParticipantEntity participant = participationService.joinOpenMatch(
                matchId,
                currentUserService.requireCurrentUserId());
        return ParticipantResponse.from(participant);
    }

    @PostMapping("/{matchId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse requestToJoin(@PathVariable final UUID matchId) {
        MatchParticipantEntity participant = participationService.requestToJoin(
                matchId,
                currentUserService.requireCurrentUserId());
        return ParticipantResponse.from(participant);
    }

    @PostMapping("/{matchId}/join-requests/{userId}/approve")
    public ParticipantResponse approveJoinRequest(
            @PathVariable final UUID matchId,
            @PathVariable final UUID userId) {
        MatchParticipantEntity participant = participationService.approveRequest(
                matchId,
                userId,
                currentUserService.requireCurrentUserId());
        return ParticipantResponse.from(participant);
    }

    @PostMapping("/{matchId}/join-requests/{userId}/reject")
    public ParticipantResponse rejectJoinRequest(
            @PathVariable final UUID matchId,
            @PathVariable final UUID userId) {
        MatchParticipantEntity participant = participationService.rejectRequest(
                matchId,
                userId,
                currentUserService.requireCurrentUserId());
        return ParticipantResponse.from(participant);
    }
}
