package com.peladinhas.backend.domains.matches.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.groups.service.CreateGroupCommand;
import com.peladinhas.backend.domains.groups.service.GroupService;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminId;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchFundingMode;
import com.peladinhas.backend.domains.matches.persistence.MatchFundingState;
import com.peladinhas.backend.domains.matches.persistence.MatchRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchStatus;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ContextualPermissionDeniedException;
import com.peladinhas.backend.shared.domain.DomainException;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private static final Set<Integer> SUPPORTED_DURATIONS_MINUTES = Set.of(60, 90, 120, 150);
    private static final Set<MatchStatus> ACTIVE_UPCOMING_STATUSES = Set.of(
            MatchStatus.DRAFT,
            MatchStatus.RECRUITING,
            MatchStatus.READY);

    private final Clock clock;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final MatchAdminRepository matchAdminRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public MatchService(
            final Clock clock,
            final GroupRepository groupRepository,
            final GroupService groupService,
            final MatchAdminRepository matchAdminRepository,
            final MatchRepository matchRepository,
            final UserRepository userRepository) {
        this.clock = clock;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
        this.matchAdminRepository = matchAdminRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MatchEntity createMatch(final CreateMatchCommand command) {
        validateCreateMatchCommand(command.durationMinutes(), command.maxPlayers(), command.fundingMode());

        GroupEntity group = groupRepository.findByIdForUpdate(command.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group was not found."));
        groupService.requireActiveGroupAdmin(group.getId(), command.creatorUserId());

        OffsetDateTime now = OffsetDateTime.now(clock);
        rejectWhenActiveUpcomingMatchExists(group.getId(), now);

        UserEntity creator = requireUser(command.creatorUserId());
        return createMatchAfterGroupLock(group, creator, command, now);
    }

    @Transactional
    public MatchEntity createDirectMatch(final CreateDirectMatchCommand command) {
        GroupEntity group = groupService.createGroup(new CreateGroupCommand(
                command.groupName(),
                command.groupDescription(),
                GroupVisibility.PUBLIC,
                command.creatorUserId()));

        return createMatch(new CreateMatchCommand(
                group.getId(),
                command.creatorUserId(),
                command.startsAt(),
                command.durationMinutes(),
                command.maxPlayers(),
                command.joinMode(),
                command.publicVacanciesEnabled(),
                command.fundingMode()));
    }

    @Transactional
    public MatchEntity transitionMatchStatus(
            final UUID matchId,
            final UUID actingUserId,
            final MatchStatus nextStatus) {
        MatchEntity match = requireMatch(matchId);
        requireMatchAdmin(matchId, actingUserId);
        MatchStatusTransitionPolicy.requireValid(match.getStatus(), nextStatus);
        match.setStatus(nextStatus);
        match.setUpdatedAt(OffsetDateTime.now(clock));
        return matchRepository.save(match);
    }

    @Transactional
    public MatchEntity markMatchNotReady(final UUID matchId) {
        MatchEntity match = requireMatch(matchId);
        MatchStatusTransitionPolicy.requireValid(match.getStatus(), MatchStatus.RECRUITING);
        match.setStatus(MatchStatus.RECRUITING);
        match.setUpdatedAt(OffsetDateTime.now(clock));
        return matchRepository.save(match);
    }

    @Transactional(readOnly = true)
    public MatchEntity requireMatchSummary(final UUID matchId) {
        return matchRepository.findByIdWithSummary(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match was not found."));
    }

    @Transactional(readOnly = true)
    public MatchEntity requireMatch(final UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match was not found."));
    }

    @Transactional(readOnly = true)
    public void requireMatchAdmin(final UUID matchId, final UUID userId) {
        if (!matchAdminRepository.existsByMatch_IdAndUser_Id(matchId, userId)) {
            throw new ContextualPermissionDeniedException("User is not a match admin.");
        }
    }

    private void validateCreateMatchCommand(
            final int durationMinutes,
            final int maxPlayers,
            final MatchFundingMode fundingMode) {
        if (!SUPPORTED_DURATIONS_MINUTES.contains(durationMinutes)) {
            throw new UnsupportedMatchDurationException("Match duration is not supported.");
        }
        if (maxPlayers <= 0) {
            throw new DomainException("Maximum players must be greater than zero.");
        }
        if (fundingMode == null) {
            throw new DomainException("Match funding mode is required.");
        }
    }

    private UserEntity requireUser(final UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }

    private void rejectWhenActiveUpcomingMatchExists(final UUID groupId, final OffsetDateTime now) {
        boolean exists = matchRepository.existsByGroup_IdAndStatusInAndEndsAtAfter(
                groupId,
                ACTIVE_UPCOMING_STATUSES,
                now);
        if (exists) {
            throw new ActiveUpcomingMatchExistsException("Group already has an active upcoming match.");
        }
    }

    private MatchEntity createMatchAfterGroupLock(
            final GroupEntity group,
            final UserEntity creator,
            final CreateMatchCommand command,
            final OffsetDateTime now) {
        MatchEntity match = new MatchEntity();
        match.setId(UUID.randomUUID());
        match.setGroup(group);
        match.setCreatedByUser(creator);
        match.setStartsAt(command.startsAt());
        match.setEndsAt(command.startsAt().plusMinutes(command.durationMinutes()));
        match.setMaxPlayers(command.maxPlayers());
        match.setJoinMode(command.joinMode());
        match.setStatus(MatchStatus.DRAFT);
        match.setFundingMode(command.fundingMode());
        match.setFundingState(MatchFundingState.COLLECTING);
        match.setPublicVacanciesEnabled(command.publicVacanciesEnabled());
        match.setCreatedAt(now);
        match.setUpdatedAt(now);

        MatchEntity savedMatch = matchRepository.save(match);
        addCreatorAsMatchAdmin(savedMatch, creator, now);
        return savedMatch;
    }

    private void addCreatorAsMatchAdmin(final MatchEntity match, final UserEntity creator, final OffsetDateTime now) {
        MatchAdminEntity admin = new MatchAdminEntity();
        admin.setId(new MatchAdminId(match.getId(), creator.getId()));
        admin.setMatch(match);
        admin.setUser(creator);
        admin.setAssignedAt(now);
        matchAdminRepository.save(admin);
    }
}
