package com.peladinhas.backend.domains.matches.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRole;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberStatus;
import com.peladinhas.backend.domains.groups.persistence.GroupRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.groups.service.CreateGroupCommand;
import com.peladinhas.backend.domains.groups.service.GroupService;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantStatus;
import com.peladinhas.backend.domains.matches.persistence.MatchRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchStatus;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ContextualPermissionDeniedException;
import com.peladinhas.backend.shared.domain.DomainException;
import com.peladinhas.backend.support.PostgreSqlContainerTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class CoreMatchServiceTests extends PostgreSqlContainerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-17T12:00:00Z"),
            ZoneOffset.UTC);
    private static final OffsetDateTime DEFAULT_START = OffsetDateTime.parse("2026-09-18T19:00:00Z");

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private MatchParticipationService participationService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return FIXED_CLOCK;
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {60, 90, 120, 150})
    void createsMatchWithSupportedDuration(final int durationMinutes) {
        UserEntity creator = createUser();
        GroupEntity group = createGroup(creator);

        MatchEntity match = createMatch(group, creator, durationMinutes, 10, MatchJoinMode.OPEN_JOIN);

        assertThat(match.getEndsAt()).isEqualTo(DEFAULT_START.plusMinutes(durationMinutes));
        assertThat(match.getStatus()).isEqualTo(MatchStatus.DRAFT);
    }

    @Test
    void existingGroupMatchCreationAddsCreatorAsApprovedParticipant() {
        UserEntity creator = createUser();
        GroupEntity group = createGroup(creator);

        MatchEntity match = createMatch(group, creator, 60, 10, MatchJoinMode.OPEN_JOIN);

        assertThat(participantRepository.findByMatch_IdAndUser_Id(match.getId(), creator.getId()))
                .hasValueSatisfying(participant -> {
                    assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.APPROVED);
                    assertThat(participant.getConfirmedAt()).isNull();
                    assertThat(participant.getCancelledAt()).isNull();
                });
        assertThat(capacityReservingParticipantsForMatch(match)).hasSize(1);
    }

    @Test
    void rejectsUnsupportedDuration() {
        UserEntity creator = createUser();
        GroupEntity group = createGroup(creator);

        assertThatThrownBy(() -> createMatch(group, creator, 45, 10, MatchJoinMode.OPEN_JOIN))
                .isInstanceOf(UnsupportedMatchDurationException.class);
    }

    @Test
    void rejectsInvalidMaxPlayers() {
        UserEntity creator = createUser();
        GroupEntity group = createGroup(creator);

        assertThatThrownBy(() -> createMatch(group, creator, 60, 0, MatchJoinMode.OPEN_JOIN))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsSecondActiveUpcomingMatchForSameGroup() {
        UserEntity creator = createUser();
        GroupEntity group = createGroup(creator);
        createMatch(group, creator, 60, 10, MatchJoinMode.OPEN_JOIN);

        assertThatThrownBy(() -> createMatch(group, creator, 90, 10, MatchJoinMode.OPEN_JOIN))
                .isInstanceOf(ActiveUpcomingMatchExistsException.class);
    }

    @Test
    void terminalStatusesDoNotBlockNewMatch() {
        UserEntity completedCreator = createUser();
        GroupEntity completedGroup = createGroup(completedCreator);
        MatchEntity completed = createMatch(completedGroup, completedCreator, 60, 10, MatchJoinMode.OPEN_JOIN);
        completed.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(completed);

        UserEntity cancelledCreator = createUser();
        GroupEntity cancelledGroup = createGroup(cancelledCreator);
        MatchEntity cancelled = createMatch(cancelledGroup, cancelledCreator, 60, 10, MatchJoinMode.OPEN_JOIN);
        cancelled.setStatus(MatchStatus.CANCELLED);
        matchRepository.save(cancelled);

        MatchEntity afterCompleted = createMatch(completedGroup, completedCreator, 90, 10, MatchJoinMode.OPEN_JOIN);
        MatchEntity afterCancelled = createMatch(cancelledGroup, cancelledCreator, 90, 10, MatchJoinMode.OPEN_JOIN);

        assertThat(afterCompleted.getId()).isNotNull();
        assertThat(afterCancelled.getId()).isNotNull();
    }

    @Test
    void pastActiveStatusDoesNotBlockNewFutureMatch() {
        UserEntity creator = createUser();
        GroupEntity group = createGroup(creator);
        MatchEntity pastDraft = createMatch(group, creator, 60, 10, MatchJoinMode.OPEN_JOIN);
        pastDraft.setStartsAt(OffsetDateTime.parse("2026-09-16T19:00:00Z"));
        pastDraft.setEndsAt(OffsetDateTime.parse("2026-09-16T20:00:00Z"));
        matchRepository.save(pastDraft);

        MatchEntity future = createMatch(group, creator, 90, 10, MatchJoinMode.OPEN_JOIN);

        assertThat(future.getEndsAt()).isEqualTo(DEFAULT_START.plusMinutes(90));
    }

    @Test
    void serializesCompetingMatchCreationForSameGroup() throws Exception {
        UserEntity creator = createUser();
        GroupEntity group = createGroup(creator);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            Callable<String> task = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    createMatch(group, creator, 60, 10, MatchJoinMode.OPEN_JOIN);
                    return "created";
                } catch (ActiveUpcomingMatchExistsException exception) {
                    return "rejected";
                }
            };

            Future<String> first = executor.submit(task);
            Future<String> second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder("created", "rejected");
            assertThat(matchesForGroup(group)).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void directMatchCreationCreatesPublicGroupAdminAndMatch() {
        UserEntity creator = createUser();

        MatchEntity match = matchService.createDirectMatch(new CreateDirectMatchCommand(
                creator.getId(),
                uniqueName("Direct Group"),
                "Direct group description",
                DEFAULT_START,
                90,
                12,
                MatchJoinMode.REQUEST_TO_JOIN,
                true));

        GroupEntity group = match.getGroup();
        assertThat(group.getVisibility()).isEqualTo(GroupVisibility.PUBLIC);
        assertThat(groupMemberRepository.existsByGroup_IdAndUser_IdAndRoleAndStatus(
                group.getId(),
                creator.getId(),
                GroupMemberRole.ADMIN,
                GroupMemberStatus.ACTIVE)).isTrue();
        assertThat(match.getEndsAt()).isEqualTo(DEFAULT_START.plusMinutes(90));
        assertThat(participantRepository.findByMatch_IdAndUser_Id(match.getId(), creator.getId()))
                .hasValueSatisfying(participant -> {
                    assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.APPROVED);
                    assertThat(participant.getConfirmedAt()).isNull();
                });
    }

    @Test
    void directMatchCreationRollsBackWhenLaterStepFails() {
        UserEntity creator = createUser();
        String groupName = uniqueName("Rollback Group");

        assertThatThrownBy(() -> matchService.createDirectMatch(new CreateDirectMatchCommand(
                creator.getId(),
                groupName,
                "Direct group description",
                DEFAULT_START,
                45,
                12,
                MatchJoinMode.REQUEST_TO_JOIN,
                true))).isInstanceOf(UnsupportedMatchDurationException.class);

        assertThat(groupRepository.findAll())
                .noneMatch(group -> groupName.equals(group.getName()));
    }

    @Test
    void openJoinCreatesAwaitingPaymentParticipantWithoutConfirmation() {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.OPEN_JOIN, 10);

        MatchParticipantEntity participant = participationService.joinOpenMatch(match.getId(), player.getId());

        assertThat(participant.getStatus()).isEqualTo(MatchParticipantStatus.AWAITING_PAYMENT);
        assertThat(participant.getConfirmedAt()).isNull();
    }

    @Test
    void requestToJoinApprovalPaymentAndRejectionFlowsWork() {
        UserEntity creator = createUser();
        UserEntity approvedPlayer = createUser();
        UserEntity rejectedPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 10);

        MatchParticipantEntity requested = participationService.requestToJoin(match.getId(), approvedPlayer.getId());
        MatchParticipantEntity approved = participationService.approveRequest(
                match.getId(),
                approvedPlayer.getId(),
                creator.getId());
        MatchParticipantEntity awaitingPayment = participationService.moveApprovedToAwaitingPayment(
                match.getId(),
                approvedPlayer.getId());
        MatchParticipantEntity rejected = participationService.requestToJoin(match.getId(), rejectedPlayer.getId());
        MatchParticipantEntity rejectedResult = participationService.rejectRequest(
                match.getId(),
                rejectedPlayer.getId(),
                creator.getId());

        assertThat(requested.getStatus()).isEqualTo(MatchParticipantStatus.REQUESTED);
        assertThat(approved.getStatus()).isEqualTo(MatchParticipantStatus.APPROVED);
        assertThat(awaitingPayment.getStatus()).isEqualTo(MatchParticipantStatus.AWAITING_PAYMENT);
        assertThat(rejected.getStatus()).isEqualTo(MatchParticipantStatus.REQUESTED);
        assertThat(rejectedResult.getStatus()).isEqualTo(MatchParticipantStatus.REJECTED);
    }

    @Test
    void duplicateOpenJoinIsRejectedForActiveParticipant() {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.OPEN_JOIN, 10);

        participationService.joinOpenMatch(match.getId(), player.getId());

        assertThatThrownBy(() -> participationService.joinOpenMatch(match.getId(), player.getId()))
                .isInstanceOf(InvalidParticipantTransitionException.class);
        assertThat(participantsForMatch(match)).hasSize(2);
    }

    @Test
    void reentryPreservesOriginalJoinedAt() {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 10);
        MatchParticipantEntity participant = participationService.requestToJoin(match.getId(), player.getId());
        OffsetDateTime originalJoinedAt = participant.getJoinedAt();
        participationService.rejectRequest(match.getId(), player.getId(), creator.getId());

        MatchParticipantEntity reentered = participationService.requestToJoin(match.getId(), player.getId());

        assertThat(reentered.getId()).isEqualTo(participant.getId());
        assertThat(reentered.getJoinedAt()).isEqualTo(originalJoinedAt);
        assertThat(reentered.getStatus()).isEqualTo(MatchParticipantStatus.REQUESTED);
    }

    @Test
    void invalidParticipantTransitionIsRejected() {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 10);
        participationService.requestToJoin(match.getId(), player.getId());

        assertThatThrownBy(() -> participationService.moveApprovedToAwaitingPayment(match.getId(), player.getId()))
                .isInstanceOf(InvalidParticipantTransitionException.class);
    }

    @Test
    void requestedParticipantsDoNotReserveCapacityAndCanExceedMaxPlayers() {
        UserEntity creator = createUser();
        UserEntity firstPlayer = createUser();
        UserEntity secondPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 1);

        MatchParticipantEntity first = participationService.requestToJoin(match.getId(), firstPlayer.getId());
        MatchParticipantEntity second = participationService.requestToJoin(match.getId(), secondPlayer.getId());

        assertThat(first.getStatus()).isEqualTo(MatchParticipantStatus.REQUESTED);
        assertThat(second.getStatus()).isEqualTo(MatchParticipantStatus.REQUESTED);
        assertThat(participantsForMatch(match)).hasSize(3);
    }

    @Test
    void approvedParticipantsReserveCapacity() {
        UserEntity creator = createUser();
        UserEntity firstPlayer = createUser();
        UserEntity secondPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 2);
        participationService.requestToJoin(match.getId(), firstPlayer.getId());
        participationService.requestToJoin(match.getId(), secondPlayer.getId());
        participationService.approveRequest(match.getId(), firstPlayer.getId(), creator.getId());

        assertThatThrownBy(() -> participationService.approveRequest(match.getId(), secondPlayer.getId(), creator.getId()))
                .isInstanceOf(MatchCapacityReachedException.class);
    }

    @Test
    void awaitingPaymentParticipantsReserveCapacity() {
        UserEntity creator = createUser();
        UserEntity firstPlayer = createUser();
        UserEntity secondPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.OPEN_JOIN, 2);
        participationService.joinOpenMatch(match.getId(), firstPlayer.getId());

        assertThatThrownBy(() -> participationService.joinOpenMatch(match.getId(), secondPlayer.getId()))
                .isInstanceOf(MatchCapacityReachedException.class);
    }

    @Test
    void confirmedParticipantsReserveCapacity() {
        UserEntity creator = createUser();
        UserEntity firstPlayer = createUser();
        UserEntity secondPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.OPEN_JOIN, 2);
        MatchParticipantEntity first = participationService.joinOpenMatch(match.getId(), firstPlayer.getId());
        first.setStatus(MatchParticipantStatus.CONFIRMED);
        participantRepository.save(first);

        assertThatThrownBy(() -> participationService.joinOpenMatch(match.getId(), secondPlayer.getId()))
                .isInstanceOf(MatchCapacityReachedException.class);
    }

    @Test
    void rejectedParticipantsDoNotReserveCapacity() {
        UserEntity creator = createUser();
        UserEntity rejectedPlayer = createUser();
        UserEntity approvedPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 2);
        participationService.requestToJoin(match.getId(), rejectedPlayer.getId());
        participationService.rejectRequest(match.getId(), rejectedPlayer.getId(), creator.getId());
        participationService.requestToJoin(match.getId(), approvedPlayer.getId());

        MatchParticipantEntity approved = participationService.approveRequest(
                match.getId(),
                approvedPlayer.getId(),
                creator.getId());

        assertThat(approved.getStatus()).isEqualTo(MatchParticipantStatus.APPROVED);
    }

    @Test
    void cancelledParticipantsDoNotReserveCapacity() {
        UserEntity creator = createUser();
        UserEntity cancelledPlayer = createUser();
        UserEntity approvedPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 2);
        MatchParticipantEntity cancelled = participationService.requestToJoin(match.getId(), cancelledPlayer.getId());
        cancelled.setStatus(MatchParticipantStatus.CANCELLED);
        cancelled.setCancelledAt(OffsetDateTime.now(FIXED_CLOCK));
        participantRepository.save(cancelled);
        participationService.requestToJoin(match.getId(), approvedPlayer.getId());

        MatchParticipantEntity approved = participationService.approveRequest(
                match.getId(),
                approvedPlayer.getId(),
                creator.getId());

        assertThat(approved.getStatus()).isEqualTo(MatchParticipantStatus.APPROVED);
    }

    @Test
    void serializesCompetingOpenJoinCapacityReservations() throws Exception {
        UserEntity creator = createUser();
        UserEntity firstPlayer = createUser();
        UserEntity secondPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.OPEN_JOIN, 2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            Callable<String> firstTask = capacityOpenJoinTask(match.getId(), firstPlayer.getId(), ready, start);
            Callable<String> secondTask = capacityOpenJoinTask(match.getId(), secondPlayer.getId(), ready, start);
            Future<String> first = executor.submit(firstTask);
            Future<String> second = executor.submit(secondTask);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder("awaiting_payment", "capacity_reached");
            assertThat(capacityReservingParticipantsForMatch(match)).hasSize(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void serializesCompetingRequestApprovalsForCapacity() throws Exception {
        UserEntity creator = createUser();
        UserEntity firstPlayer = createUser();
        UserEntity secondPlayer = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 2);
        participationService.requestToJoin(match.getId(), firstPlayer.getId());
        participationService.requestToJoin(match.getId(), secondPlayer.getId());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            Callable<String> firstTask = capacityApprovalTask(match.getId(), firstPlayer.getId(), creator.getId(), ready, start);
            Callable<String> secondTask = capacityApprovalTask(match.getId(), secondPlayer.getId(), creator.getId(), ready, start);
            Future<String> first = executor.submit(firstTask);
            Future<String> second = executor.submit(secondTask);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder("approved", "capacity_reached");
            assertThat(capacityReservingParticipantsForMatch(match)).hasSize(2);
        } finally {
            executor.shutdownNow();
        }
    }
    @Test
    void contextualAdminOperationSucceedsForAdminAndFailsForNonAdmin() {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        UserEntity nonAdmin = createUser();
        MatchEntity match = createRecruitingMatch(createGroup(creator), creator, MatchJoinMode.REQUEST_TO_JOIN, 10);
        participationService.requestToJoin(match.getId(), player.getId());

        assertThatThrownBy(() -> participationService.approveRequest(match.getId(), player.getId(), nonAdmin.getId()))
                .isInstanceOf(ContextualPermissionDeniedException.class);

        MatchParticipantEntity approved = participationService.approveRequest(
                match.getId(),
                player.getId(),
                creator.getId());
        assertThat(approved.getStatus()).isEqualTo(MatchParticipantStatus.APPROVED);
    }

    private Callable<String> capacityOpenJoinTask(
            final UUID matchId,
            final UUID userId,
            final CountDownLatch ready,
            final CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                participationService.joinOpenMatch(matchId, userId);
                return "awaiting_payment";
            } catch (MatchCapacityReachedException exception) {
                return "capacity_reached";
            }
        };
    }

    private Callable<String> capacityApprovalTask(
            final UUID matchId,
            final UUID participantUserId,
            final UUID actingAdminUserId,
            final CountDownLatch ready,
            final CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                participationService.approveRequest(matchId, participantUserId, actingAdminUserId);
                return "approved";
            } catch (MatchCapacityReachedException exception) {
                return "capacity_reached";
            }
        };
    }

    private List<MatchParticipantEntity> capacityReservingParticipantsForMatch(final MatchEntity match) {
        return participantsForMatch(match).stream()
                .filter(participant -> participant.getStatus() == MatchParticipantStatus.APPROVED
                        || participant.getStatus() == MatchParticipantStatus.AWAITING_PAYMENT
                        || participant.getStatus() == MatchParticipantStatus.CONFIRMED)
                .toList();
    }
    private MatchEntity createRecruitingMatch(
            final GroupEntity group,
            final UserEntity creator,
            final MatchJoinMode joinMode,
            final int maxPlayers) {
        MatchEntity match = createMatch(group, creator, 60, maxPlayers, joinMode);
        return matchService.transitionMatchStatus(match.getId(), creator.getId(), MatchStatus.RECRUITING);
    }

    private MatchEntity createMatch(
            final GroupEntity group,
            final UserEntity creator,
            final int durationMinutes,
            final int maxPlayers,
            final MatchJoinMode joinMode) {
        return matchService.createMatch(new CreateMatchCommand(
                group.getId(),
                creator.getId(),
                DEFAULT_START,
                durationMinutes,
                maxPlayers,
                joinMode,
                true));
    }

    private GroupEntity createGroup(final UserEntity creator) {
        return groupService.createGroup(new CreateGroupCommand(
                uniqueName("Group"),
                "Group description",
                GroupVisibility.PRIVATE,
                creator.getId()));
    }

    private UserEntity createUser() {
        OffsetDateTime now = OffsetDateTime.now(FIXED_CLOCK);
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(uniqueName("user") + "@example.test");
        user.setName(uniqueName("User"));
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setAuthProvider("test");
        user.setAuthSubject(user.getId().toString());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private List<MatchEntity> matchesForGroup(final GroupEntity group) {
        return matchRepository.findAll().stream()
                .filter(match -> match.getGroup().getId().equals(group.getId()))
                .toList();
    }

    private List<MatchParticipantEntity> participantsForMatch(final MatchEntity match) {
        return participantRepository.findAll().stream()
                .filter(participant -> participant.getMatch().getId().equals(match.getId()))
                .toList();
    }

    private String uniqueName(final String prefix) {
        return prefix + " " + UUID.randomUUID();
    }
}
