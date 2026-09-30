package com.peladinhas.backend.domains.pitches.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionReason;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingStatus;
import com.peladinhas.backend.domains.bookings.service.BookingService;
import com.peladinhas.backend.domains.bookings.service.CreateProvisionalBookingCommand;
import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.groups.service.CreateGroupCommand;
import com.peladinhas.backend.domains.groups.service.GroupService;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminId;
import com.peladinhas.backend.domains.matches.persistence.MatchAdminRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantStatus;
import com.peladinhas.backend.domains.matches.service.CreateDirectMatchCommand;
import com.peladinhas.backend.domains.matches.service.CreateMatchCommand;
import com.peladinhas.backend.domains.matches.service.MatchService;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerProfileEntity;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerProfileRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.service.CreatePitchBlockCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchScheduleCommand;
import com.peladinhas.backend.domains.pitches.service.PitchBlockService;
import com.peladinhas.backend.domains.pitches.service.PitchScheduleService;
import com.peladinhas.backend.domains.pitches.service.PitchService;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.support.PostgreSqlContainerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
class PitchBookingApiTests extends PostgreSqlContainerTest {

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
    private MatchAdminRepository matchAdminRepository;

    @Autowired
    private MatchParticipantRepository matchParticipantRepository;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PitchBlockService pitchBlockService;

    @Autowired
    private PitchOwnerProfileRepository pitchOwnerProfileRepository;

    @Autowired
    private PitchScheduleService pitchScheduleService;

    @Autowired
    private PitchService pitchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return FIXED_CLOCK;
        }
    }

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void authenticatedUserCreatesPitchAsThemselvesAndObsoleteOwnerCannotImpersonate() throws Exception {
        UserEntity owner = createUser();
        activateOwner(owner);
        UserEntity spoofedOwner = createUser();

        MvcResult created = mockMvc.perform(post("/api/v1/pitches")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pitchRequest(true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUserId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        assertThat(uuidAt(created, "id")).isNotNull();

        Map<String, Object> impersonationBody = pitchRequest(true);
        impersonationBody.put("ownerUserId", spoofedOwner.getId());
        mockMvc.perform(post("/api/v1/pitches")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(impersonationBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void normalPlayerCannotCreatePitchByCallingApiDirectly() throws Exception {
        UserEntity player = createUser();

        mockMvc.perform(post("/api/v1/pitches")
                        .with(jwtFor(player))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pitchRequest(true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("pitch_owner_capability_required"));
    }

    @Test
    void unauthenticatedPitchCreationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pitchRequest(true))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void ownerCanUpdateOwnPitchAndNonOwnerCannot() throws Exception {
        UserEntity owner = createUser();
        UserEntity outsider = createUser();
        UUID pitchId = uuidAt(createPitchThroughApi(owner, true), "id");

        Map<String, Object> updateBody = pitchRequest(true);
        updateBody.put("name", "Updated pitch");
        mockMvc.perform(put("/api/v1/pitches/{pitchId}", pitchId)
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated pitch"));

        mockMvc.perform(put("/api/v1/pitches/{pitchId}", pitchId)
                        .with(jwtFor(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));
    }

    @Test
    void getPitchReturnsLightweightResponse() throws Exception {
        UserEntity owner = createUser();
        UUID pitchId = uuidAt(createPitchThroughApi(owner, true), "id");

        mockMvc.perform(get("/api/v1/pitches/{pitchId}", pitchId).with(jwtFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pitchId.toString()))
                .andExpect(jsonPath("$.ownerUserId").value(owner.getId().toString()));
    }

    @Test
    void ownerQueriesReturnOnlyAuthenticatedOwnersPitchesAndBookings() throws Exception {
        UserEntity firstOwner = createUser();
        UserEntity secondOwner = createUser();
        PitchEntity firstPitch = createPitch(firstOwner, true);
        PitchEntity secondPitch = createPitch(secondOwner, true);
        MatchEntity firstMatch = createMatch(firstOwner);
        MatchEntity secondMatch = createMatch(secondOwner, MONDAY_09_00.plusHours(1));
        createSchedule(firstPitch, firstOwner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        createSchedule(secondPitch, secondOwner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        BookingEntity firstBooking = createBooking(firstMatch, firstPitch);
        BookingEntity secondBooking = createBooking(secondMatch, secondPitch);

        mockMvc.perform(get("/api/v1/pitches/mine").with(jwtFor(firstOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstPitch.getId().toString()))
                .andExpect(jsonPath("$[0].ownerUserId").value(firstOwner.getId().toString()))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/v1/bookings/owner").with(jwtFor(firstOwner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstBooking.getId().toString()))
                .andExpect(jsonPath("$[0].pitchId").value(firstPitch.getId().toString()))
                .andExpect(jsonPath("$[1]").doesNotExist());

        assertThat(secondBooking.getPitch().getId()).isEqualTo(secondPitch.getId());
    }

    @Test
    void availabilityMissingOrMalformedQueryParametersReturnStableBadRequest() throws Exception {
        UserEntity owner = createUser();
        UUID pitchId = uuidAt(createPitchThroughApi(owner, true), "id");

        mockMvc.perform(get("/api/v1/pitches/{pitchId}/availability", pitchId)
                        .with(jwtFor(owner))
                        .queryParam("endsAt", MONDAY_09_00.plusHours(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());

        mockMvc.perform(get("/api/v1/pitches/{pitchId}/availability", pitchId)
                        .with(jwtFor(owner))
                        .queryParam("startsAt", MONDAY_09_00.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());

        mockMvc.perform(get("/api/v1/pitches/{pitchId}/availability", pitchId)
                        .with(jwtFor(owner))
                        .queryParam("startsAt", "not-a-date")
                        .queryParam("endsAt", MONDAY_09_00.plusHours(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());

        mockMvc.perform(get("/api/v1/pitches/{pitchId}/availability", pitchId)
                        .with(jwtFor(owner))
                        .queryParam("startsAt", MONDAY_09_00.toString())
                        .queryParam("endsAt", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void pitchCoordinateBoundariesAreAcceptedAndOutOfRangeCoordinatesReturnStableBadRequest() throws Exception {
        UserEntity owner = createUser();
        activateOwner(owner);

        mockMvc.perform(post("/api/v1/pitches")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pitchRequest(true, new BigDecimal("-90"), new BigDecimal("-180")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latitude").value(-90))
                .andExpect(jsonPath("$.longitude").value(-180));

        mockMvc.perform(post("/api/v1/pitches")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pitchRequest(true, new BigDecimal("90"), new BigDecimal("180")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latitude").value(90))
                .andExpect(jsonPath("$.longitude").value(180));

        expectInvalidPitchCoordinates(owner, new BigDecimal("-90.01"), BigDecimal.ZERO, "validation_failed");
        expectInvalidPitchCoordinates(owner, new BigDecimal("90.01"), BigDecimal.ZERO, "validation_failed");
        expectInvalidPitchCoordinates(owner, BigDecimal.ZERO, new BigDecimal("-180.01"), "validation_failed");
        expectInvalidPitchCoordinates(owner, BigDecimal.ZERO, new BigDecimal("180.01"), "validation_failed");
        expectInvalidPitchCoordinates(owner, new BigDecimal("38.7200"), null, "domain_error");
        expectInvalidPitchCoordinates(owner, null, new BigDecimal("-9.1300"), "domain_error");
    }
    @Test
    void ownerCreatesSchedulesOverlapReturnsConflictAndAdjacentWorks() throws Exception {
        UserEntity owner = createUser();
        UserEntity outsider = createUser();
        UUID pitchId = uuidAt(createPitchThroughApi(owner, true), "id");

        mockMvc.perform(post("/api/v1/pitches/{pitchId}/schedules", pitchId)
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(scheduleRequest(1, "09:00:00", "11:00:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pitchId").value(pitchId.toString()));

        mockMvc.perform(post("/api/v1/pitches/{pitchId}/schedules", pitchId)
                        .with(jwtFor(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(scheduleRequest(1, "11:00:00", "12:00:00"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));

        mockMvc.perform(post("/api/v1/pitches/{pitchId}/schedules", pitchId)
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(scheduleRequest(1, "10:30:00", "12:00:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));

        mockMvc.perform(post("/api/v1/pitches/{pitchId}/schedules", pitchId)
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(scheduleRequest(1, "11:00:00", "12:00:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startsAt").value("11:00:00"));
    }

    @Test
    void ownerCreatesBlockAndNonOwnerCannot() throws Exception {
        UserEntity owner = createUser();
        UserEntity outsider = createUser();
        UUID pitchId = uuidAt(createPitchThroughApi(owner, true), "id");

        mockMvc.perform(post("/api/v1/pitches/{pitchId}/blocks", pitchId)
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(blockRequest(MONDAY_09_00, MONDAY_09_00.plusHours(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pitchId").value(pitchId.toString()))
                .andExpect(jsonPath("$.reasonCode").value("maintenance"));

        mockMvc.perform(post("/api/v1/pitches/{pitchId}/blocks", pitchId)
                        .with(jwtFor(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(blockRequest(MONDAY_09_00.plusHours(1), MONDAY_09_00.plusHours(2)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));
    }

    @Test
    void availabilityReflectsSchedulesInactivePitchesBlocksBookingsAndBoundaries() throws Exception {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner, true);
        PitchEntity inactivePitch = createPitch(owner, false);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        createSchedule(inactivePitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));

        expectAvailability(pitch.getId(), MONDAY_09_00, MONDAY_09_00.plusHours(1), true, owner);
        expectAvailability(pitch.getId(), MONDAY_09_00.minusHours(1), MONDAY_09_00, false, owner);
        expectAvailability(inactivePitch.getId(), MONDAY_09_00, MONDAY_09_00.plusHours(1), false, owner);

        pitchBlockService.createBlock(new CreatePitchBlockCommand(
                pitch.getId(),
                owner.getId(),
                MONDAY_09_00.plusHours(1),
                MONDAY_09_00.plusHours(2),
                "maintenance",
                "Blocked"));
        expectAvailability(pitch.getId(), MONDAY_09_00, MONDAY_09_00.plusHours(1), true, owner);
        expectAvailability(pitch.getId(), MONDAY_09_00.plusHours(1), MONDAY_09_00.plusHours(2), false, owner);
        expectAvailability(pitch.getId(), MONDAY_09_00.plusHours(2), MONDAY_09_00.plusHours(3), true, owner);

        PitchEntity bookedPitch = createPitch(owner, true);
        createSchedule(bookedPitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        MatchEntity match = createMatch(owner);
        BookingEntity provisional = createBooking(match, bookedPitch);
        expectAvailability(bookedPitch.getId(), MONDAY_09_00.plusMinutes(30), MONDAY_09_00.plusMinutes(90), true, owner);

        provisional.setStatus(BookingStatus.CONFIRMED);
        provisional.setConfirmedAt(OffsetDateTime.now(FIXED_CLOCK));
        bookingRepository.save(provisional);
        expectAvailability(bookedPitch.getId(), MONDAY_09_00.plusMinutes(30), MONDAY_09_00.plusMinutes(90), false, owner);
        expectAvailability(bookedPitch.getId(), MONDAY_09_00.plusHours(1), MONDAY_09_00.plusHours(2), true, owner);
    }

    @Test
    void pitchOwnerRejectsBookingWithStructuredReason() throws Exception {
        UserEntity owner = createUser();
        PitchEntity pitch = createPitch(owner, true);
        MatchEntity match = createMatch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        BookingEntity booking = createBooking(match, pitch);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/reject", booking.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "reasonCode", "maintenance",
                                "explanation", "Pitch maintenance"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId().toString()))
                .andExpect(jsonPath("$.status").value("rejected"));

        assertThat(bookingRejectionRepository.findAll()).anySatisfy(rejection -> {
            assertThat(rejection.getBooking().getId()).isEqualTo(booking.getId());
            assertThat(rejection.getReasonCode()).isEqualTo(BookingRejectionReason.MAINTENANCE);
            assertThat(rejection.getExplanation()).isEqualTo("Pitch maintenance");
        });
    }

    @Test
    void nonOwnerMissingReasonAndInvalidBookingTransitionAreMapped() throws Exception {
        UserEntity owner = createUser();
        UserEntity outsider = createUser();
        PitchEntity pitch = createPitch(owner, true);
        MatchEntity match = createMatch(owner);
        createSchedule(pitch, owner, LocalTime.of(9, 0), LocalTime.of(12, 0));
        BookingEntity booking = createBooking(match, pitch);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/reject", booking.getId())
                        .with(jwtFor(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reasonCode", "other"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/reject", booking.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("explanation", "Missing reason"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/reject", booking.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reasonCode", "other"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/reject", booking.getId())
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reasonCode", "other"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));
    }

    @Test
    void matchAdminCreatesProvisionalBookingAndObsoleteActorFieldCannotImpersonate() throws Exception {
        UserEntity admin = createUser();
        UserEntity spoofedAdmin = createUser();
        PitchEntity pitch = createPitch(admin, true);
        MatchEntity match = createMatch(admin);
        createSchedule(pitch, admin, LocalTime.of(9, 0), LocalTime.of(12, 0));

        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(match, pitch))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value(match.getId().toString()))
                .andExpect(jsonPath("$.pitchId").value(pitch.getId().toString()))
                .andExpect(jsonPath("$.startsAt").value(match.getStartsAt().toInstant().toString()))
                .andExpect(jsonPath("$.endsAt").value(match.getEndsAt().toInstant().toString()))
                .andExpect(jsonPath("$.status").value("provisional"))
                .andReturn();
        assertThat(uuidAt(created, "id")).isNotNull();

        Map<String, Object> impersonationBody = bookingRequest(match, pitch);
        impersonationBody.put("actingUserId", spoofedAdmin.getId());
        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(impersonationBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void additionalMatchAdminMayCreateProvisionalBooking() throws Exception {
        UserEntity creator = createUser();
        UserEntity additionalAdmin = createUser();
        PitchEntity pitch = createPitch(creator, true);
        MatchEntity match = createMatch(creator);
        addMatchAdmin(match, additionalAdmin);
        createSchedule(pitch, creator, LocalTime.of(9, 0), LocalTime.of(12, 0));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(additionalAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(match, pitch))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("provisional"));
    }

    @Test
    void directMatchCreatorCanCreateProvisionalBooking() throws Exception {
        UserEntity creator = createUser();
        PitchEntity pitch = createPitch(creator, true);
        createSchedule(pitch, creator, LocalTime.of(9, 0), LocalTime.of(12, 0));
        MatchEntity directMatch = matchService.createDirectMatch(new CreateDirectMatchCommand(
                creator.getId(),
                uniqueName("Direct Match Group"),
                "Direct match group",
                MONDAY_09_00,
                60,
                10,
                MatchJoinMode.OPEN_JOIN,
                true));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(directMatch, pitch))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value(directMatch.getId().toString()))
                .andExpect(jsonPath("$.pitchId").value(pitch.getId().toString()))
                .andExpect(jsonPath("$.startsAt").value(directMatch.getStartsAt().toInstant().toString()))
                .andExpect(jsonPath("$.endsAt").value(directMatch.getEndsAt().toInstant().toString()))
                .andExpect(jsonPath("$.status").value("provisional"));
    }

    @Test
    void participantAndUnrelatedUserCannotCreateProvisionalBooking() throws Exception {
        UserEntity admin = createUser();
        UserEntity participant = createUser();
        UserEntity unrelated = createUser();
        PitchEntity pitch = createPitch(admin, true);
        MatchEntity match = createMatch(admin);
        createParticipant(match, participant);
        createSchedule(pitch, admin, LocalTime.of(9, 0), LocalTime.of(12, 0));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(match, pitch))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(unrelated))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(match, pitch))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));
        assertThat(bookingRepository.findAll()).isEmpty();
    }

    @Test
    void unauthenticatedBookingCreationReturnsUnauthorized() throws Exception {
        UserEntity admin = createUser();
        PitchEntity pitch = createPitch(admin, true);
        MatchEntity match = createMatch(admin);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(match, pitch))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void bookingCreationMapsAvailabilityAndIntervalFailures() throws Exception {
        UserEntity admin = createUser();
        PitchEntity pitch = createPitch(admin, true);
        PitchEntity inactivePitch = createPitch(admin, false);
        MatchEntity match = createMatch(admin);
        MatchEntity outsideScheduleMatch = createMatch(admin, MONDAY_09_00.minusHours(1));
        createSchedule(pitch, admin, LocalTime.of(9, 0), LocalTime.of(12, 0));
        createSchedule(inactivePitch, admin, LocalTime.of(9, 0), LocalTime.of(12, 0));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(outsideScheduleMatch, pitch))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(match, inactivePitch))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));

        BookingEntity confirmed = createBooking(match, pitch);
        confirmed.setStatus(BookingStatus.CONFIRMED);
        confirmed.setConfirmedAt(OffsetDateTime.now(FIXED_CLOCK));
        bookingRepository.save(confirmed);
        MatchEntity overlappingMatch = createMatch(admin, MONDAY_09_00.plusMinutes(30));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(overlappingMatch, pitch))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));

        Map<String, Object> obsoleteInterval = bookingRequest(match, pitch);
        obsoleteInterval.put("startsAt", MONDAY_09_00.plusHours(2));
        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(obsoleteInterval)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void bookingCreationAllowsProvisionalOverlap() throws Exception {
        UserEntity admin = createUser();
        PitchEntity pitch = createPitch(admin, true);
        MatchEntity match = createMatch(admin);
        MatchEntity overlappingMatch = createMatch(admin, MONDAY_09_00.plusMinutes(30));
        createSchedule(pitch, admin, LocalTime.of(9, 0), LocalTime.of(12, 0));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(match, pitch))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(bookingRequest(overlappingMatch, pitch))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("provisional"));
    }

    @Test
    void malformedBookingCreationInputReturnsStableBadRequest() throws Exception {
        UserEntity admin = createUser();
        PitchEntity pitch = createPitch(admin, true);
        MatchEntity match = createMatch(admin);

        Map<String, Object> malformedUuid = bookingRequest(match, pitch);
        malformedUuid.put("matchId", "not-a-uuid");
        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(malformedUuid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));

        Map<String, Object> obsoleteDate = bookingRequest(match, pitch);
        obsoleteDate.put("startsAt", "not-a-date");
        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(obsoleteDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));

        Map<String, Object> malformedValue = bookingRequest(match, pitch);
        malformedValue.put("totalPrice", "not-a-number");
        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(malformedValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));

        Map<String, Object> invalidCurrency = bookingRequest(match, pitch);
        invalidCurrency.put("currency", "eur");
        mockMvc.perform(post("/api/v1/bookings")
                        .with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalidCurrency)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    private void expectInvalidPitchCoordinates(
            final UserEntity owner,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final String expectedCode) throws Exception {
        mockMvc.perform(post("/api/v1/pitches")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pitchRequest(true, latitude, longitude))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
    private MvcResult createPitchThroughApi(final UserEntity owner, final boolean active) throws Exception {
        activateOwner(owner);
        return mockMvc.perform(post("/api/v1/pitches")
                        .with(jwtFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pitchRequest(active))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private void expectAvailability(
            final UUID pitchId,
            final OffsetDateTime startsAt,
            final OffsetDateTime endsAt,
            final boolean expected,
            final UserEntity requester) throws Exception {
        mockMvc.perform(get("/api/v1/pitches/{pitchId}/availability", pitchId)
                        .with(jwtFor(requester))
                        .queryParam("startsAt", startsAt.toString())
                        .queryParam("endsAt", endsAt.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pitchId").value(pitchId.toString()))
                .andExpect(jsonPath("$.available").value(expected));
    }

    private Map<String, Object> pitchRequest(final boolean active) {
        return pitchRequest(active, new BigDecimal("38.7200"), new BigDecimal("-9.1300"));
    }

    private Map<String, Object> pitchRequest(
            final boolean active,
            final BigDecimal latitude,
            final BigDecimal longitude) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", uniqueName("Pitch"));
        request.put("description", "Pitch description");
        request.put("address", "Lisbon");
        request.put("latitude", latitude);
        request.put("longitude", longitude);
        request.put("timezone", "Europe/Lisbon");
        request.put("basePrice", new BigDecimal("60.00"));
        request.put("currency", "EUR");
        request.put("active", active);
        return request;
    }

    private Map<String, Object> scheduleRequest(
            final int dayOfWeek,
            final String startsAt,
            final String endsAt) {
        return Map.of(
                "dayOfWeek", dayOfWeek,
                "startsAt", startsAt,
                "endsAt", endsAt);
    }

    private Map<String, Object> blockRequest(final OffsetDateTime startsAt, final OffsetDateTime endsAt) {
        return Map.of(
                "startsAt", startsAt,
                "endsAt", endsAt,
                "reasonCode", "maintenance",
                "note", "Pitch block");
    }

    private BookingEntity createBooking(final MatchEntity match, final PitchEntity pitch) {
        return bookingService.createProvisionalBooking(new CreateProvisionalBookingCommand(
                match.getCreatedByUser().getId(),
                match.getId(),
                pitch.getId(),
                new BigDecimal("120.00"),
                "EUR"));
    }

    private Map<String, Object> bookingRequest(final MatchEntity match, final PitchEntity pitch) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("matchId", match.getId());
        request.put("pitchId", pitch.getId());
        request.put("totalPrice", new BigDecimal("120.00"));
        request.put("currency", "EUR");
        return request;
    }

    private void addMatchAdmin(final MatchEntity match, final UserEntity user) {
        MatchAdminEntity admin = new MatchAdminEntity();
        admin.setId(new MatchAdminId(match.getId(), user.getId()));
        admin.setMatch(match);
        admin.setUser(user);
        admin.setAssignedAt(OffsetDateTime.now(FIXED_CLOCK));
        matchAdminRepository.save(admin);
    }

    private void createParticipant(final MatchEntity match, final UserEntity user) {
        MatchParticipantEntity participant = new MatchParticipantEntity();
        participant.setId(UUID.randomUUID());
        participant.setMatch(match);
        participant.setUser(user);
        participant.setStatus(MatchParticipantStatus.AWAITING_PAYMENT);
        participant.setJoinedAt(OffsetDateTime.now(FIXED_CLOCK));
        matchParticipantRepository.save(participant);
    }

    private void createSchedule(
            final PitchEntity pitch,
            final UserEntity owner,
            final LocalTime startsAt,
            final LocalTime endsAt) {
        pitchScheduleService.createSchedule(new CreatePitchScheduleCommand(
                pitch.getId(),
                owner.getId(),
                (short) 1,
                startsAt,
                endsAt));
    }

    private PitchEntity createPitch(final UserEntity owner, final boolean active) {
        activateOwner(owner);
        return pitchService.createPitch(new CreatePitchCommand(
                owner.getId(),
                uniqueName("Pitch"),
                "Pitch description",
                "Lisbon",
                new BigDecimal("38.7200"),
                new BigDecimal("-9.1300"),
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
        user.setAuthProvider("supabase");
        user.setAuthSubject(user.getId().toString());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private void activateOwner(final UserEntity user) {
        if (pitchOwnerProfileRepository.existsById(user.getId())) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(FIXED_CLOCK);
        PitchOwnerProfileEntity profile = new PitchOwnerProfileEntity();
        profile.setUser(user);
        profile.setActivatedAt(now);
        profile.setCreatedAt(now);
        pitchOwnerProfileRepository.save(profile);
    }

    private RequestPostProcessor jwtFor(final UserEntity user) {
        return jwt().jwt(token -> token.subject(user.getAuthSubject()));
    }

    private String json(final Map<String, Object> values) throws Exception {
        return objectMapper.writeValueAsString(new LinkedHashMap<>(values));
    }

    private UUID uuidAt(final MvcResult result, final String field) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(root.get(field).asText());
    }

    private String uniqueName(final String prefix) {
        return prefix + " " + UUID.randomUUID();
    }
}
