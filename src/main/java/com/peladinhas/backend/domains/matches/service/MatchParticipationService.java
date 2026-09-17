package com.peladinhas.backend.domains.matches.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantStatus;
import com.peladinhas.backend.domains.matches.persistence.MatchRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchStatus;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchParticipationService {

    private static final Set<MatchParticipantStatus> CAPACITY_STATUSES = Set.of(
            MatchParticipantStatus.APPROVED,
            MatchParticipantStatus.AWAITING_PAYMENT,
            MatchParticipantStatus.CONFIRMED);

    private final Clock clock;
    private final MatchParticipantRepository participantRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final UserRepository userRepository;

    public MatchParticipationService(
            final Clock clock,
            final MatchParticipantRepository participantRepository,
            final MatchRepository matchRepository,
            final MatchService matchService,
            final UserRepository userRepository) {
        this.clock = clock;
        this.participantRepository = participantRepository;
        this.matchRepository = matchRepository;
        this.matchService = matchService;
        this.userRepository = userRepository;
    }

    @Transactional
    public MatchParticipantEntity joinOpenMatch(final UUID matchId, final UUID userId) {
        MatchEntity match = requireMatchForCapacityUpdate(matchId);
        requireAcceptingParticipants(match);
        requireJoinMode(match, MatchJoinMode.OPEN_JOIN);
        UserEntity user = requireUser(userId);

        return participantRepository.findByMatch_IdAndUser_Id(matchId, userId)
                .map(participant -> reenterOpenJoin(match, participant))
                .orElseGet(() -> createParticipant(match, user, MatchParticipantStatus.AWAITING_PAYMENT));
    }

    @Transactional
    public MatchParticipantEntity requestToJoin(final UUID matchId, final UUID userId) {
        MatchEntity match = matchService.requireMatch(matchId);
        requireAcceptingParticipants(match);
        requireJoinMode(match, MatchJoinMode.REQUEST_TO_JOIN);
        UserEntity user = requireUser(userId);

        return participantRepository.findByMatch_IdAndUser_Id(matchId, userId)
                .map(participant -> reenterRequestToJoin(participant))
                .orElseGet(() -> createParticipant(match, user, MatchParticipantStatus.REQUESTED));
    }

    @Transactional
    public MatchParticipantEntity approveRequest(
            final UUID matchId,
            final UUID participantUserId,
            final UUID actingAdminUserId) {
        matchService.requireMatchAdmin(matchId, actingAdminUserId);
        MatchEntity match = requireMatchForCapacityUpdate(matchId);
        MatchParticipantEntity participant = requireParticipant(matchId, participantUserId);
        requireParticipantStatus(participant, MatchParticipantStatus.REQUESTED);
        requireCapacity(match);
        participant.setStatus(MatchParticipantStatus.APPROVED);
        return participantRepository.save(participant);
    }

    @Transactional
    public MatchParticipantEntity moveApprovedToAwaitingPayment(final UUID matchId, final UUID participantUserId) {
        MatchParticipantEntity participant = requireParticipant(matchId, participantUserId);
        requireParticipantStatus(participant, MatchParticipantStatus.APPROVED);
        participant.setStatus(MatchParticipantStatus.AWAITING_PAYMENT);
        return participantRepository.save(participant);
    }

    @Transactional
    public MatchParticipantEntity rejectRequest(
            final UUID matchId,
            final UUID participantUserId,
            final UUID actingAdminUserId) {
        matchService.requireMatchAdmin(matchId, actingAdminUserId);
        MatchParticipantEntity participant = requireParticipant(matchId, participantUserId);
        requireParticipantStatus(participant, MatchParticipantStatus.REQUESTED);
        participant.setStatus(MatchParticipantStatus.REJECTED);
        return participantRepository.save(participant);
    }

    private MatchParticipantEntity createParticipant(
            final MatchEntity match,
            final UserEntity user,
            final MatchParticipantStatus status) {
        if (CAPACITY_STATUSES.contains(status)) {
            requireCapacity(match);
        }

        MatchParticipantEntity participant = new MatchParticipantEntity();
        participant.setId(UUID.randomUUID());
        participant.setMatch(match);
        participant.setUser(user);
        participant.setStatus(status);
        participant.setJoinedAt(OffsetDateTime.now(clock));
        return participantRepository.save(participant);
    }

    private MatchParticipantEntity reenterOpenJoin(
            final MatchEntity match,
            final MatchParticipantEntity participant) {
        if (participant.getStatus() == MatchParticipantStatus.CANCELLED) {
            requireCapacity(match);
            participant.setStatus(MatchParticipantStatus.AWAITING_PAYMENT);
            participant.setCancelledAt(null);
            participant.setConfirmedAt(null);
            return participantRepository.save(participant);
        }
        if (participant.getStatus() == MatchParticipantStatus.AWAITING_PAYMENT
                || participant.getStatus() == MatchParticipantStatus.CONFIRMED) {
            return participant;
        }
        throw new InvalidParticipantTransitionException("Participant cannot re-enter this Open Join flow.");
    }

    private MatchParticipantEntity reenterRequestToJoin(final MatchParticipantEntity participant) {
        if (participant.getStatus() == MatchParticipantStatus.REJECTED
                || participant.getStatus() == MatchParticipantStatus.CANCELLED) {
            participant.setStatus(MatchParticipantStatus.REQUESTED);
            participant.setCancelledAt(null);
            participant.setConfirmedAt(null);
            return participantRepository.save(participant);
        }
        if (participant.getStatus() == MatchParticipantStatus.REQUESTED
                || participant.getStatus() == MatchParticipantStatus.APPROVED
                || participant.getStatus() == MatchParticipantStatus.AWAITING_PAYMENT
                || participant.getStatus() == MatchParticipantStatus.CONFIRMED) {
            return participant;
        }
        throw new InvalidParticipantTransitionException("Participant cannot re-enter this Request to Join flow.");
    }

    private void requireAcceptingParticipants(final MatchEntity match) {
        if (match.getStatus() != MatchStatus.RECRUITING) {
            throw new MatchNotAcceptingParticipantsException("Match is not accepting participants.");
        }
    }

    private void requireJoinMode(final MatchEntity match, final MatchJoinMode expectedMode) {
        if (match.getJoinMode() != expectedMode) {
            throw new InvalidParticipantTransitionException("Match join mode does not support this participant flow.");
        }
    }

    private void requireCapacity(final MatchEntity match) {
        long activeParticipants = participantRepository.countByMatch_IdAndStatusIn(match.getId(), CAPACITY_STATUSES);
        if (activeParticipants >= match.getMaxPlayers()) {
            throw new MatchCapacityReachedException("Match participant capacity has been reached.");
        }
    }

    private MatchEntity requireMatchForCapacityUpdate(final UUID matchId) {
        return matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match was not found."));
    }

    private MatchParticipantEntity requireParticipant(final UUID matchId, final UUID userId) {
        return participantRepository.findByMatch_IdAndUser_Id(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match participant was not found."));
    }

    private void requireParticipantStatus(
            final MatchParticipantEntity participant,
            final MatchParticipantStatus expectedStatus) {
        if (participant.getStatus() != expectedStatus) {
            throw new InvalidParticipantTransitionException("Participant status transition is not valid.");
        }
    }

    private UserEntity requireUser(final UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }
}