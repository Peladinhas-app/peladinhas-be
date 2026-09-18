package com.peladinhas.backend.domains.matches.web;

import java.util.UUID;

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

    private final MatchParticipationService participationService;
    private final MatchService matchService;

    public MatchController(
            final MatchParticipationService participationService,
            final MatchService matchService) {
        this.participationService = participationService;
        this.matchService = matchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse createMatch(@Valid @RequestBody final CreateMatchRequest request) {
        MatchJoinMode joinMode = ApiEnumParser.parse(MatchJoinMode.class, request.joinMode(), "joinMode");
        MatchEntity match = matchService.createMatch(new CreateMatchCommand(
                request.groupId(),
                request.creatorUserId(),
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
        MatchJoinMode joinMode = ApiEnumParser.parse(MatchJoinMode.class, request.joinMode(), "joinMode");
        MatchEntity match = matchService.createDirectMatch(new CreateDirectMatchCommand(
                request.creatorUserId(),
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
        MatchStatus nextStatus = ApiEnumParser.parse(MatchStatus.class, request.nextStatus(), "nextStatus");
        matchService.transitionMatchStatus(matchId, request.actingAdminUserId(), nextStatus);
        return MatchResponse.from(matchService.requireMatchSummary(matchId));
    }

    @PostMapping("/{matchId}/join")
    public ParticipantResponse joinOpenMatch(
            @PathVariable final UUID matchId,
            @Valid @RequestBody final JoinMatchRequest request) {
        MatchParticipantEntity participant = participationService.joinOpenMatch(matchId, request.userId());
        return ParticipantResponse.from(participant);
    }

    @PostMapping("/{matchId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse requestToJoin(
            @PathVariable final UUID matchId,
            @Valid @RequestBody final JoinMatchRequest request) {
        MatchParticipantEntity participant = participationService.requestToJoin(matchId, request.userId());
        return ParticipantResponse.from(participant);
    }

    @PostMapping("/{matchId}/join-requests/{userId}/approve")
    public ParticipantResponse approveJoinRequest(
            @PathVariable final UUID matchId,
            @PathVariable final UUID userId,
            @Valid @RequestBody final AdminActionRequest request) {
        MatchParticipantEntity participant = participationService.approveRequest(
                matchId,
                userId,
                request.actingAdminUserId());
        return ParticipantResponse.from(participant);
    }

    @PostMapping("/{matchId}/join-requests/{userId}/awaiting-payment")
    public ParticipantResponse moveApprovedRequestToAwaitingPayment(
            @PathVariable final UUID matchId,
            @PathVariable final UUID userId) {
        MatchParticipantEntity participant = participationService.moveApprovedToAwaitingPayment(matchId, userId);
        return ParticipantResponse.from(participant);
    }

    @PostMapping("/{matchId}/join-requests/{userId}/reject")
    public ParticipantResponse rejectJoinRequest(
            @PathVariable final UUID matchId,
            @PathVariable final UUID userId,
            @Valid @RequestBody final AdminActionRequest request) {
        MatchParticipantEntity participant = participationService.rejectRequest(
                matchId,
                userId,
                request.actingAdminUserId());
        return ParticipantResponse.from(participant);
    }
}
