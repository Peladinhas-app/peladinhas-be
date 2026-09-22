package com.peladinhas.backend.domains.bookings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionReason;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingStatus;
import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.groups.service.CreateGroupCommand;
import com.peladinhas.backend.domains.groups.service.GroupService;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.service.CreateMatchCommand;
import com.peladinhas.backend.domains.matches.service.MatchService;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleEntity;
import com.peladinhas.backend.domains.pitches.service.CreatePitchBlockCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchScheduleCommand;
import com.peladinhas.backend.domains.pitches.service.PitchAvailabilityService;
import com.peladinhas.backend.domains.pitches.service.PitchBlockService;
import com.peladinhas.backend.domains.pitches.service.PitchNotAvailableException;
import com.peladinhas.backend.domains.pitches.service.PitchScheduleConflictException;
import com.peladinhas.backend.domains.pitches.service.PitchScheduleService;
import com.peladinhas.backend.domains.pitches.service.PitchService;
import com.peladinhas.backend.domains.pitches.service.UpdatePitchCommand;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ContextualPermissionDeniedException;
import com.peladinhas.backend.shared.domain.DomainException;
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
class PitchBookingServiceTests extends PostgreSqlContainerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-17T12:00:00Z"),
            ZoneOffset.UTC);
    private static final OffsetDateTime MONDAY_09_00 = OffsetDateTime.parse("2026-09-21T09:00:00+01:00");

    @Autowired
    private BookingRejectionRepository bookingRejectionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private PitchAvailabilityService pitchAvailabilityService;

    @Autowired
    private PitchBlockService pitchBlockService;

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
    void ownerCanManageOwnPitchAndAnotherUserCannot() {
        UserEntity owner = createUser();
        UserEntity outsider = createUser();
        PitchEntity pitch = createPitch(owner);

        PitchEntity updated = pitchService.updatePitch(pitch.getId(), owner.getId(), new UpdatePitchCommand(
                "Updated pitch",
                "Updated description",
                "Updated address",
                new BigDecimal("38.7100"),
                new BigDecimal("-9.1400"),
                "Europe/Lisbon",
                new BigDecimal("70.00"),
                "EUR",
                true));

        assertThat(updated.getName()).isEqualTo("Updated pitch");
        assertThatThrownBy(() -> pitchService.updatePitch(pitch.getId(), outsider.getId(), new UpdatePitchCommand(
                "Blocked update",
                null,
                "Blocked address",
                null,
                null,
                "Europe/Lisbon",
                new BigDecimal("60.00"),
                "EUR",
                true))).isInstanceOf(ContextualPermissionDeniedException.class);
    }

    @Test
    void createsValidRecurringScheduleRejectsOverlapAndAllowsAdjacentWindow() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);

        PitchScheduleEntity morning = createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(morning.getId()).isNotNull();
        assertThatThrownBy(() -> createSchedule(pitch, owner, LocalTime.of(10, 30), LocalTime.of(12, 0)))
                .isInstanceOf(PitchScheduleConflictException.class);

        PitchScheduleEntity adjacent = createSchedule(pitch, owner, LocalTime.of(11, 0), LocalTime.of(12, 0));
        assertThat(adjacent.getStartsAt()).isEqualTo(LocalTime.of(11, 0));
    }


    @Test
    void pitchServiceRejectsCoordinatesOutsideApprovedRanges() {
        UserEntity owner = createUser();

        assertThat(createPitch(owner, true, new BigDecimal("-90"), new BigDecimal("-180")).getId()).isNotNull();
        assertThat(createPitch(owner, true, new BigDecimal("90"), new BigDecimal("180")).getId()).isNotNull();
        assertThatThrownBy(() -> createPitch(owner, true, new BigDecimal("-90.01"), BigDecimal.ZERO))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> createPitch(owner, true, new BigDecimal("90.01"), BigDecimal.ZERO))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> createPitch(owner, true, BigDecimal.ZERO, new BigDecimal("-180.01")))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> createPitch(owner, true, BigDecimal.ZERO, new BigDecimal("180.01")))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> createPitch(owner, true, new BigDecimal("38.7200"), null))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> createPitch(owner, true, null, new BigDecimal("-9.1300")))
                .isInstanceOf(DomainException.class);
    }
    @Test
    void pitchBlocksAffectAvailabilityUsingHalfOpenIntervals() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));

        pitchBlockService.createBlock(new CreatePitchBlockCommand(
                pitch.getId(),
                owner.getId(),
                MONDAY_09_00.plusHours(1),
                MONDAY_09_00.plusHours(2),
                "maintenance",
                "Local maintenance"));

        assertThat(isAvailable(pitch, MONDAY_09_00, MONDAY_09_00.plusHours(1))).isTrue();
        assertThat(isAvailable(pitch, MONDAY_09_00.plusHours(1), MONDAY_09_00.plusHours(2))).isFalse();
        assertThat(isAvailable(pitch, MONDAY_09_00.plusHours(2), MONDAY_09_00.plusHours(3))).isTrue();
    }

    @Test
    void availabilityRequiresScheduleAndIgnoresProvisionalBookingsButNotConfirmedBookings() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity match = createMatch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(isAvailable(pitch, MONDAY_09_00, MONDAY_09_00.plusHours(1))).isTrue();
        assertThat(isAvailable(pitch, MONDAY_09_00.minusHours(1), MONDAY_09_00)).isFalse();

        BookingEntity provisional = createBooking(match, pitch);
        assertThat(provisional.getStatus()).isEqualTo(BookingStatus.PROVISIONAL);
        assertThat(isAvailable(pitch, MONDAY_09_00.plusMinutes(30), MONDAY_09_00.plusMinutes(90))).isTrue();

        provisional.setStatus(BookingStatus.CONFIRMED);
        provisional.setConfirmedAt(OffsetDateTime.now(FIXED_CLOCK));
        bookingRepository.save(provisional);

        assertThat(isAvailable(pitch, MONDAY_09_00.plusMinutes(30), MONDAY_09_00.plusMinutes(90))).isFalse();
    }

    @Test
    void inactivePitchIsUnavailableEvenInsideValidSchedule() {
        UserEntity owner = createUser();
        PitchEntity activePitch = createPitch(owner);
        PitchEntity inactivePitch = createPitch(owner, false);
        MatchEntity match = createMatch(owner);
        createSchedule(activePitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        createSchedule(inactivePitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));

        assertThat(isAvailable(activePitch, MONDAY_09_00, MONDAY_09_00.plusHours(1))).isTrue();
        assertThat(isAvailable(inactivePitch, MONDAY_09_00, MONDAY_09_00.plusHours(1))).isFalse();
        assertThatThrownBy(() -> createBooking(match, inactivePitch))
                .isInstanceOf(PitchNotAvailableException.class);
    }

    @Test
    void createsProvisionalBookingsAndAllowsProvisionalOverlap() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity firstMatch = createMatch(owner);
        MatchEntity secondMatch = createMatch(createUser(), MONDAY_09_00.plusMinutes(30));
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));

        BookingEntity first = createBooking(firstMatch, pitch);
        BookingEntity second = createBooking(secondMatch, pitch);

        assertThat(first.getStatus()).isEqualTo(BookingStatus.PROVISIONAL);
        assertThat(first.getStartsAt()).isEqualTo(firstMatch.getStartsAt());
        assertThat(first.getEndsAt()).isEqualTo(firstMatch.getEndsAt());
        assertThat(second.getStatus()).isEqualTo(BookingStatus.PROVISIONAL);
        assertThat(second.getStartsAt()).isEqualTo(secondMatch.getStartsAt());
        assertThat(second.getEndsAt()).isEqualTo(secondMatch.getEndsAt());
    }

    @Test
    void onlyMatchAdminCanCreateProvisionalBooking() {
        UserEntity owner = createUser();
        UserEntity nonAdmin = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity match = createMatch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        long bookingCountBefore = bookingRepository.count();

        assertThatThrownBy(() -> bookingService.createProvisionalBooking(new CreateProvisionalBookingCommand(
                nonAdmin.getId(),
                match.getId(),
                pitch.getId(),
                new BigDecimal("120.00"),
                "EUR"))).isInstanceOf(ContextualPermissionDeniedException.class);
        assertThat(bookingRepository.count()).isEqualTo(bookingCountBefore);
    }

    @Test
    void rejectsProvisionalBookingOutsideAvailability() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity outsideScheduleMatch = createMatch(owner, MONDAY_09_00.minusHours(1));
        MatchEntity blockedMatch = createMatch(owner, MONDAY_09_00.plusHours(1));
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        pitchBlockService.createBlock(new CreatePitchBlockCommand(
                pitch.getId(),
                owner.getId(),
                MONDAY_09_00.plusHours(1),
                MONDAY_09_00.plusHours(2),
                "maintenance",
                "Blocked"));

        assertThatThrownBy(() -> createBooking(outsideScheduleMatch, pitch))
                .isInstanceOf(PitchNotAvailableException.class);
        assertThatThrownBy(() -> createBooking(blockedMatch, pitch))
                .isInstanceOf(PitchNotAvailableException.class);
    }

    @Test
    void rejectsProvisionalBookingOverlappingConfirmedBooking() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity confirmedMatch = createMatch(owner);
        MatchEntity overlappingChallengerMatch = createMatch(createUser(), MONDAY_09_00.plusMinutes(30));
        MatchEntity adjacentChallengerMatch = createMatch(createUser(), MONDAY_09_00.plusHours(1));
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));

        BookingEntity confirmed = createBooking(confirmedMatch, pitch);
        confirmed.setStatus(BookingStatus.CONFIRMED);
        confirmed.setConfirmedAt(OffsetDateTime.now(FIXED_CLOCK));
        bookingRepository.save(confirmed);

        assertThatThrownBy(() -> createBooking(
                overlappingChallengerMatch,
                pitch)).isInstanceOf(PitchNotAvailableException.class);
        assertThat(createBooking(adjacentChallengerMatch, pitch).getId())
                .isNotNull();
    }

    @Test
    void ownerCanRejectProvisionalBookingAndRecordReason() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity match = createMatch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        BookingEntity booking = createBooking(match, pitch);

        BookingEntity rejected = bookingService.rejectBooking(new RejectBookingCommand(
                booking.getId(),
                owner.getId(),
                BookingRejectionReason.MAINTENANCE,
                "Pitch maintenance"));

        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(rejected.getRejectedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
        BookingRejectionEntity rejection = bookingRejectionRepository.findAll().stream()
                .filter(candidate -> candidate.getBooking().getId().equals(booking.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(rejection.getReasonCode()).isEqualTo(BookingRejectionReason.MAINTENANCE);
        assertThat(rejection.getExplanation()).isEqualTo("Pitch maintenance");
    }

    @Test
    void nonOwnerCannotRejectBookingAndInvalidTransitionIsRejected() {
        UserEntity owner = createUser();
        UserEntity outsider = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity match = createMatch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        BookingEntity booking = createBooking(match, pitch);

        assertThatThrownBy(() -> bookingService.rejectBooking(new RejectBookingCommand(
                booking.getId(),
                outsider.getId(),
                BookingRejectionReason.OTHER,
                null))).isInstanceOf(ContextualPermissionDeniedException.class);

        bookingService.rejectBooking(new RejectBookingCommand(
                booking.getId(),
                owner.getId(),
                BookingRejectionReason.OTHER,
                null));
        assertThatThrownBy(() -> bookingService.rejectBooking(new RejectBookingCommand(
                booking.getId(),
                owner.getId(),
                BookingRejectionReason.OTHER,
                null))).isInstanceOf(InvalidBookingTransitionException.class);
    }

    @Test
    void missingRejectionReasonDoesNotMutateBookingOrCreateRejectionRecord() {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner);
        MatchEntity match = createMatch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        BookingEntity booking = createBooking(match, pitch);

        assertThatThrownBy(() -> bookingService.rejectBooking(new RejectBookingCommand(
                booking.getId(),
                owner.getId(),
                null,
                "Missing structured reason"))).isInstanceOf(DomainException.class);

        BookingEntity unchanged = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(BookingStatus.PROVISIONAL);
        assertThat(unchanged.getRejectedAt()).isNull();
        assertThat(bookingRejectionRepository.findAll()).noneMatch(
                rejection -> rejection.getBooking().getId().equals(booking.getId()));
    }

    private boolean isAvailable(
            final PitchEntity pitch,
            final OffsetDateTime startsAt,
            final OffsetDateTime endsAt) {
        return pitchAvailabilityService.isAvailable(pitch.getId(), startsAt, endsAt);
    }

    private BookingEntity createBooking(
            final MatchEntity match,
            final PitchEntity pitch) {
        return bookingService.createProvisionalBooking(new CreateProvisionalBookingCommand(
                match.getCreatedByUser().getId(),
                match.getId(),
                pitch.getId(),
                new BigDecimal("120.00"),
                "EUR"));
    }

    private PitchScheduleEntity createSchedule(
            final PitchEntity pitch,
            final UserEntity owner,
            final LocalTime startsAt,
            final LocalTime endsAt) {
        return pitchScheduleService.createSchedule(new CreatePitchScheduleCommand(
                pitch.getId(),
                owner.getId(),
                (short) 1,
                startsAt,
                endsAt));
    }

    private PitchEntity createPitch(final UserEntity owner) {
        return createPitch(owner, true);
    }

    private PitchEntity createPitch(final UserEntity owner, final boolean active) {
        return createPitch(owner, active, new BigDecimal("38.7200"), new BigDecimal("-9.1300"));
    }

    private PitchEntity createPitch(
            final UserEntity owner,
            final boolean active,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        return pitchService.createPitch(new CreatePitchCommand(
                owner.getId(),
                uniqueName("Pitch"),
                "Pitch description",
                "Lisbon",
                latitude,
                longitude,
                "Europe/Lisbon",
                new BigDecimal("60.00"),
                "EUR",
                active));
    }

    private MatchEntity createMatch(final UserEntity creator) {
        return createMatch(creator, MONDAY_09_00);
    }

    private MatchEntity createMatch(final UserEntity creator, final OffsetDateTime startsAt) {
        GroupEntity group = groupService.createGroup(new CreateGroupCommand(
                uniqueName("Group"),
                "Group description",
                GroupVisibility.PRIVATE,
                creator.getId()));
        return matchService.createMatch(new CreateMatchCommand(
                group.getId(),
                creator.getId(),
                startsAt,
                60,
                10,
                MatchJoinMode.OPEN_JOIN,
                true));
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

    private String uniqueName(final String prefix) {
        return prefix + " " + UUID.randomUUID();
    }
}
