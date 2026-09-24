package com.peladinhas.backend.domains.funding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingStatus;
import com.peladinhas.backend.domains.bookings.service.BookingConfirmationService;
import com.peladinhas.backend.domains.bookings.service.BookingService;
import com.peladinhas.backend.domains.bookings.service.CreateProvisionalBookingCommand;
import com.peladinhas.backend.domains.funding.persistence.FundingContributionEntity;
import com.peladinhas.backend.domains.funding.persistence.FundingContributionPurpose;
import com.peladinhas.backend.domains.funding.persistence.FundingContributionRepository;
import com.peladinhas.backend.domains.funding.persistence.FundingContributionState;
import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.groups.service.CreateGroupCommand;
import com.peladinhas.backend.domains.groups.service.GroupService;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchFundingMode;
import com.peladinhas.backend.domains.matches.persistence.MatchFundingState;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.service.CreateMatchCommand;
import com.peladinhas.backend.domains.matches.service.MatchService;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.service.CreatePitchCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchScheduleCommand;
import com.peladinhas.backend.domains.pitches.service.PitchScheduleService;
import com.peladinhas.backend.domains.pitches.service.PitchService;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ContextualPermissionDeniedException;
import com.peladinhas.backend.support.PostgreSqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class FundingFoundationServiceTests extends PostgreSqlContainerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-17T12:00:00Z"),
            ZoneOffset.UTC);
    private static final OffsetDateTime MONDAY_09_00 = OffsetDateTime.parse("2026-09-21T09:00:00+01:00");

    @Autowired
    private BookingConfirmationService bookingConfirmationService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FundingContributionRepository contributionRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private MatchFundingService fundingService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private PitchScheduleService pitchScheduleService;

    @Autowired
    private PitchService pitchService;

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

    @Test
    void matchFundingModePersistsDefaultAndExplicitValues() {
        UserEntity firstCreator = createUser();
        UserEntity secondCreator = createUser();

        MatchEntity defaultMatch = createMatch(firstCreator, MONDAY_09_00, MatchFundingMode.SPLIT_PAYMENT);
        MatchEntity prepaidMatch = createMatch(
                secondCreator,
                MONDAY_09_00.plusHours(2),
                MatchFundingMode.ORGANIZER_PREPAID);

        assertThat(defaultMatch.getFundingMode()).isEqualTo(MatchFundingMode.SPLIT_PAYMENT);
        assertThat(defaultMatch.getFundingState()).isEqualTo(MatchFundingState.COLLECTING);
        assertThat(prepaidMatch.getFundingMode()).isEqualTo(MatchFundingMode.ORGANIZER_PREPAID);
        assertThat(prepaidMatch.getFundingState()).isEqualTo(MatchFundingState.COLLECTING);
    }

    @Test
    void fundingSummaryCountsOnlySettledCoveringContributions() {
        UserEntity organizer = createUser();
        UserEntity player = createUser();
        PitchEntity pitch = createPitch(organizer);
        createSchedule(pitch, organizer);
        BookingEntity booking = createBooking(createMatch(organizer), pitch);

        createContribution(booking, player, new BigDecimal("20.00"), FundingContributionPurpose.PARTICIPANT_SHARE,
                FundingContributionState.SETTLED);
        createContribution(booking, organizer, new BigDecimal("30.00"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED);
        createContribution(booking, player, new BigDecimal("10.00"), FundingContributionPurpose.REPLACEMENT_PAYMENT,
                FundingContributionState.SETTLED);
        createContribution(booking, player, new BigDecimal("100.00"), FundingContributionPurpose.PARTICIPANT_SHARE,
                FundingContributionState.PENDING);
        createContribution(booking, organizer, new BigDecimal("100.00"),
                FundingContributionPurpose.REVERSAL_REFUND_ALLOCATION, FundingContributionState.SETTLED);

        FundingSummary summary = fundingService.summarizeBookingFunding(booking.getId());

        assertThat(summary.targetAmount()).isEqualByComparingTo("120.00");
        assertThat(summary.settledCoveredAmount()).isEqualByComparingTo("60.00");
        assertThat(summary.outstandingAmount()).isEqualByComparingTo("60.00");
        assertThat(summary.fullyFunded()).isFalse();
    }

    @Test
    void fundingSummaryCountsOnlyContributionsInBookingCurrency() {
        UserEntity organizer = createUser();
        UserEntity player = createUser();
        PitchEntity pitch = createPitch(organizer);
        createSchedule(pitch, organizer);
        BookingEntity booking = createBooking(createMatch(organizer), pitch);
        createContribution(booking, player, new BigDecimal("120.00"), FundingContributionPurpose.PARTICIPANT_SHARE,
                FundingContributionState.SETTLED, "USD");
        createContribution(booking, organizer, new BigDecimal("30.00"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED, "EUR");
        createContribution(booking, player, new BigDecimal("15.00"), FundingContributionPurpose.REPLACEMENT_PAYMENT,
                FundingContributionState.SETTLED, "USD");

        FundingSummary summary = fundingService.summarizeBookingFunding(booking.getId());

        assertThat(summary.targetAmount()).isEqualByComparingTo("120.00");
        assertThat(summary.settledCoveredAmount()).isEqualByComparingTo("30.00");
        assertThat(summary.outstandingAmount()).isEqualByComparingTo("90.00");
        assertThat(summary.fullyFunded()).isFalse();
        assertThatThrownBy(() -> bookingConfirmationService.confirmWhenFunded(booking.getId(), organizer.getId()))
                .isInstanceOf(BookingFundingIncompleteException.class);
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PROVISIONAL);
    }

    @Test
    void confirmationRequiresCompleteFunding() {
        UserEntity organizer = createUser();
        PitchEntity pitch = createPitch(organizer);
        createSchedule(pitch, organizer);
        BookingEntity booking = createBooking(createMatch(organizer), pitch);
        createContribution(booking, organizer, new BigDecimal("119.99"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED);

        assertThatThrownBy(() -> bookingConfirmationService.confirmWhenFunded(booking.getId(), organizer.getId()))
                .isInstanceOf(BookingFundingIncompleteException.class);
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PROVISIONAL);
    }

    @Test
    void confirmationRechecksAvailabilityAndRecordsBookingFailedWhenSlotWasLost() {
        UserEntity organizer = createUser();
        PitchEntity pitch = createPitch(organizer);
        createSchedule(pitch, organizer);
        BookingEntity booking = createBooking(createMatch(organizer), pitch);
        BookingEntity confirmed = createBooking(createMatch(createUser(), MONDAY_09_00.plusMinutes(30)), pitch);
        confirmed.setStatus(BookingStatus.CONFIRMED);
        confirmed.setConfirmedAt(OffsetDateTime.now(FIXED_CLOCK));
        bookingRepository.saveAndFlush(confirmed);
        createContribution(booking, organizer, new BigDecimal("120.00"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED);

        BookingEntity result = bookingConfirmationService.confirmWhenFunded(booking.getId(), organizer.getId());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PROVISIONAL);
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PROVISIONAL);
        assertThat(result.getMatch().getFundingState()).isEqualTo(MatchFundingState.BOOKING_FAILED);
    }

    @Test
    void fullyFundedConfirmationIsIdempotentAndMarksCompetitorsLost() {
        UserEntity organizer = createUser();
        PitchEntity pitch = createPitch(organizer);
        createSchedule(pitch, organizer);
        BookingEntity winner = createBooking(createMatch(organizer), pitch);
        BookingEntity competitor = createBooking(createMatch(createUser(), MONDAY_09_00.plusMinutes(30)), pitch);
        createContribution(winner, organizer, new BigDecimal("120.00"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED);

        BookingEntity first = bookingConfirmationService.confirmWhenFunded(winner.getId(), organizer.getId());
        BookingEntity second = bookingConfirmationService.confirmWhenFunded(winner.getId(), organizer.getId());

        assertThat(first.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(second.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookingRepository.findById(competitor.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.LOST);
        assertThat(first.getMatch().getFundingState()).isEqualTo(MatchFundingState.BOOKING_CONFIRMED);
    }

    @Test
    void concurrentOverlappingConfirmationsLeaveOnlyOneConfirmedBooking() throws Exception {
        UserEntity firstOrganizer = createUser();
        UserEntity secondOrganizer = createUser();
        PitchEntity pitch = createPitch(firstOrganizer);
        createSchedule(pitch, firstOrganizer);
        BookingEntity first = createBooking(createMatch(firstOrganizer), pitch);
        BookingEntity second = createBooking(createMatch(secondOrganizer, MONDAY_09_00.plusMinutes(30)), pitch);
        createContribution(first, firstOrganizer, new BigDecimal("120.00"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED);
        createContribution(second, secondOrganizer, new BigDecimal("120.00"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> firstResult = executor.submit(confirmTask(first.getId(), firstOrganizer.getId(), ready, start));
            Future<String> secondResult = executor.submit(confirmTask(second.getId(), secondOrganizer.getId(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> results = List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));

            assertThat(results).contains("confirmed");
            assertThat(bookingRepository.findAll().stream()
                    .filter(booking -> booking.getPitch().getId().equals(pitch.getId()))
                    .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED))
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void nonAdminCannotConfirmFundedBooking() {
        UserEntity organizer = createUser();
        UserEntity outsider = createUser();
        PitchEntity pitch = createPitch(organizer);
        createSchedule(pitch, organizer);
        BookingEntity booking = createBooking(createMatch(organizer), pitch);
        createContribution(booking, organizer, new BigDecimal("120.00"), FundingContributionPurpose.ORGANIZER_ADVANCE,
                FundingContributionState.SETTLED);

        assertThatThrownBy(() -> bookingConfirmationService.confirmWhenFunded(booking.getId(), outsider.getId()))
                .isInstanceOf(ContextualPermissionDeniedException.class);
    }

    private Callable<String> confirmTask(
            final UUID bookingId,
            final UUID actingUserId,
            final CountDownLatch ready,
            final CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                BookingEntity booking = bookingConfirmationService.confirmWhenFunded(bookingId, actingUserId);
                return booking.getStatus().value();
            } catch (RuntimeException exception) {
                return exception.getClass().getSimpleName();
            }
        };
    }

    private BookingEntity createBooking(final MatchEntity match, final PitchEntity pitch) {
        return bookingService.createProvisionalBooking(new CreateProvisionalBookingCommand(
                match.getCreatedByUser().getId(),
                match.getId(),
                pitch.getId(),
                new BigDecimal("120.00"),
                "EUR"));
    }

    private FundingContributionEntity createContribution(
            final BookingEntity booking,
            final UserEntity actor,
            final BigDecimal amount,
            final FundingContributionPurpose purpose,
            final FundingContributionState state) {
        FundingContributionEntity contribution = new FundingContributionEntity();
        contribution.setId(UUID.randomUUID());
        contribution.setMatch(booking.getMatch());
        contribution.setBooking(booking);
        contribution.setActorUser(actor);
        contribution.setAmount(amount);
        contribution.setCurrency(booking.getCurrency());
        contribution.setPurpose(purpose);
        contribution.setState(state);
        contribution.setCreatedAt(OffsetDateTime.now(FIXED_CLOCK));
        if (state == FundingContributionState.SETTLED) {
            contribution.setSettledAt(OffsetDateTime.now(FIXED_CLOCK));
        }
        return contributionRepository.save(contribution);
    }

    private FundingContributionEntity createContribution(
            final BookingEntity booking,
            final UserEntity actor,
            final BigDecimal amount,
            final FundingContributionPurpose purpose,
            final FundingContributionState state,
            final String currency) {
        FundingContributionEntity contribution = createContribution(booking, actor, amount, purpose, state);
        contribution.setCurrency(currency);
        return contributionRepository.save(contribution);
    }

    private PitchEntity createPitch(final UserEntity owner) {
        return pitchService.createPitch(new CreatePitchCommand(
                owner.getId(),
                "Pitch " + UUID.randomUUID(),
                null,
                "Street 1",
                null,
                null,
                "Europe/Lisbon",
                new BigDecimal("120.00"),
                "EUR",
                true));
    }

    private void createSchedule(final PitchEntity pitch, final UserEntity owner) {
        pitchScheduleService.createSchedule(new CreatePitchScheduleCommand(
                pitch.getId(),
                owner.getId(),
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)));
    }

    private MatchEntity createMatch(final UserEntity creator) {
        return createMatch(creator, MONDAY_09_00, MatchFundingMode.SPLIT_PAYMENT);
    }

    private MatchEntity createMatch(final UserEntity creator, final OffsetDateTime startsAt) {
        return createMatch(creator, startsAt, MatchFundingMode.SPLIT_PAYMENT);
    }

    private MatchEntity createMatch(
            final UserEntity creator,
            final OffsetDateTime startsAt,
            final MatchFundingMode fundingMode) {
        GroupEntity group = groupService.createGroup(new CreateGroupCommand(
                "Group " + UUID.randomUUID(),
                null,
                GroupVisibility.PUBLIC,
                creator.getId()));
        return matchService.createMatch(new CreateMatchCommand(
                group.getId(),
                creator.getId(),
                startsAt,
                60,
                10,
                MatchJoinMode.OPEN_JOIN,
                true,
                fundingMode));
    }

    private UserEntity createUser() {
        OffsetDateTime now = OffsetDateTime.now(FIXED_CLOCK);
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user-%s@example.test".formatted(user.getId()));
        user.setName("User " + user.getId());
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setAuthProvider("test");
        user.setAuthSubject(user.getId().toString());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }
}
