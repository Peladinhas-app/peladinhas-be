package com.peladinhas.backend.domains.matches.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.service.BookingService;
import com.peladinhas.backend.domains.bookings.service.CreateProvisionalBookingCommand;
import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberEntity;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberId;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRole;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberStatus;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
import com.peladinhas.backend.domains.groups.service.CreateGroupCommand;
import com.peladinhas.backend.domains.groups.service.GroupService;
import com.peladinhas.backend.domains.matches.persistence.MatchDiscoveryRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchFundingMode;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchParticipantStatus;
import com.peladinhas.backend.domains.matches.persistence.MatchRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchStatus;
import com.peladinhas.backend.domains.matches.service.CreateMatchCommand;
import com.peladinhas.backend.domains.matches.service.MatchDiscoveryCriteria;
import com.peladinhas.backend.domains.matches.service.MatchDiscoveryRow;
import com.peladinhas.backend.domains.matches.service.MatchDiscoveryService;
import com.peladinhas.backend.domains.matches.service.MatchService;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerProfileEntity;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerProfileRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.service.CreatePitchCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchScheduleCommand;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
class MatchDiscoveryApiTests extends PostgreSqlContainerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-10-08T08:00:00Z"),
            ZoneOffset.UTC);
    private static final OffsetDateTime FUTURE_START = OffsetDateTime.parse("2026-10-20T09:00:00Z");

    @Autowired
    private BookingService bookingService;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MatchParticipantRepository participantRepository;

    @Autowired
    private MatchDiscoveryRepository matchDiscoveryRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchService matchService;

    private MockMvc mockMvc;

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
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        clearData();
    }

    @Test
    void authenticationIsRequired() throws Exception {
        mockMvc.perform(get("/api/v1/matches/discovery"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void futureDiscoverableMatchReturnsSafeDiscoveryResponse() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        PitchEntity pitch = createPitch(organizer, "Central Pitch", "Lisbon Center");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                FUTURE_START,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        createBooking(match, pitch);
        String expectedDisplayName = jdbcTemplate.queryForObject(
                "select g.name from groups g join matches m on m.group_id = g.id where m.id = ?",
                String.class,
                match.getId());
        createParticipant(match, createUser(), MatchParticipantStatus.APPROVED);
        createParticipant(match, createUser(), MatchParticipantStatus.REQUESTED);

        mockMvc.perform(discovery(viewer)
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].matchId").value(match.getId().toString()))
                .andExpect(jsonPath("$.matches[0].displayName").value(expectedDisplayName))
                .andExpect(jsonPath("$.matches[0].durationMinutes").value(60))
                .andExpect(jsonPath("$.matches[0].maxPlayers").value(10))
                .andExpect(jsonPath("$.matches[0].occupiedPlaces").value(2))
                .andExpect(jsonPath("$.matches[0].availablePlaces").value(8))
                .andExpect(jsonPath("$.matches[0].pitchId").value(pitch.getId().toString()))
                .andExpect(jsonPath("$.matches[0].pitchName").value("Central Pitch"))
                .andExpect(jsonPath("$.matches[0].pitchAddress").value("Lisbon Center"))
                .andExpect(jsonPath("$.matches[0].pitchBasePrice").value(60.00))
                .andExpect(jsonPath("$.matches[0].pitchCurrency").value("EUR"))
                .andExpect(jsonPath("$.matches[0].joinMode").value("open_join"))
                .andExpect(jsonPath("$.matches[0].createdByUserId").doesNotExist())
                .andExpect(jsonPath("$.matches[0].ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.matches[0].participants").doesNotExist());
    }

    @Test
    void excludesPastDraftCancelledCompletedAndUnauthorizedPrivateMatches() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        MatchEntity visible = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                FUTURE_START,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        createRecruitingMatch(organizer, GroupVisibility.PUBLIC, FUTURE_START.minusDays(30), MatchJoinMode.OPEN_JOIN, true, 10);
        createMatchWithStatus(organizer, GroupVisibility.PUBLIC, FUTURE_START.plusHours(1), MatchStatus.DRAFT, true);
        createMatchWithStatus(organizer, GroupVisibility.PUBLIC, FUTURE_START.plusHours(2), MatchStatus.CANCELLED, true);
        createMatchWithStatus(organizer, GroupVisibility.PUBLIC, FUTURE_START.plusHours(3), MatchStatus.COMPLETED, true);
        createRecruitingMatch(organizer, GroupVisibility.PRIVATE, FUTURE_START.plusHours(4), MatchJoinMode.OPEN_JOIN, false, 10);

        mockMvc.perform(discovery(viewer)
                        .queryParam("startsFrom", FUTURE_START.minusDays(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(visible.getId().toString()));
    }

    @Test
    void privateMatchesAreDiscoverableForPublicVacanciesOrActiveGroupMembers() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        MatchEntity publicVacancy = createRecruitingMatch(
                organizer,
                GroupVisibility.PRIVATE,
                FUTURE_START,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        MatchEntity memberOnly = createRecruitingMatch(
                organizer,
                GroupVisibility.PRIVATE,
                FUTURE_START.plusHours(1),
                MatchJoinMode.OPEN_JOIN,
                false,
                10);
        addActiveMember(memberOnly.getGroup(), viewer);
        createRecruitingMatch(organizer, GroupVisibility.PRIVATE, FUTURE_START.plusHours(2), MatchJoinMode.OPEN_JOIN, false, 10);

        mockMvc.perform(discovery(viewer)
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.matches[0].matchId").value(publicVacancy.getId().toString()))
                .andExpect(jsonPath("$.matches[1].matchId").value(memberOnly.getId().toString()));
    }

    @Test
    void availableOnlyUsesExistingCapacityReservingStatuses() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        MatchEntity full = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                FUTURE_START,
                MatchJoinMode.OPEN_JOIN,
                true,
                4);
        createParticipant(full, createUser(), MatchParticipantStatus.APPROVED);
        createParticipant(full, createUser(), MatchParticipantStatus.AWAITING_PAYMENT);
        createParticipant(full, createUser(), MatchParticipantStatus.CONFIRMED);
        createParticipant(full, createUser(), MatchParticipantStatus.REQUESTED);
        createParticipant(full, createUser(), MatchParticipantStatus.REJECTED);
        createParticipant(full, createUser(), MatchParticipantStatus.CANCELLED);
        MatchEntity available = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                FUTURE_START.plusHours(1),
                MatchJoinMode.OPEN_JOIN,
                true,
                3);
        createParticipant(available, createUser(), MatchParticipantStatus.AWAITING_PAYMENT);

        mockMvc.perform(discovery(viewer)
                        .queryParam("availableOnly", "true")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(available.getId().toString()))
                .andExpect(jsonPath("$.matches[0].occupiedPlaces").value(2))
                .andExpect(jsonPath("$.matches[0].availablePlaces").value(1));
    }

    @Test
    void filtersAreCombinableAndAreaMatchingIsTrimmedCaseInsensitive() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        PitchEntity pitch = createPitch(organizer, "Riverside Pitch", "Porto Riverside");
        MatchEntity expected = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                FUTURE_START,
                MatchJoinMode.REQUEST_TO_JOIN,
                true,
                10);
        createBooking(expected, pitch);
        createRecruitingMatch(organizer, GroupVisibility.PUBLIC, FUTURE_START.plusDays(2), MatchJoinMode.REQUEST_TO_JOIN, true, 10);
        createRecruitingMatch(organizer, GroupVisibility.PUBLIC, FUTURE_START.plusHours(2), MatchJoinMode.OPEN_JOIN, true, 10);

        mockMvc.perform(discovery(viewer)
                        .queryParam("area", "  riverside  ")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString())
                        .queryParam("timeFrom", "08:00:00")
                        .queryParam("timeTo", "10:00:00")
                        .queryParam("joinMode", "request_to_join"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(expected.getId().toString()))
                .andExpect(jsonPath("$.matches[0].pitchName").value("Riverside Pitch"));
    }

    @Test
    void localTimeFilterUsesAssociatedPitchTimezoneForSummerLisbonTime() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        OffsetDateTime summerStart = OffsetDateTime.parse("2027-07-20T17:00:00Z");
        PitchEntity pitch = createPitch(organizer, "Summer Local Pitch", "Lisbon", "Europe/Lisbon");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                summerStart,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        createBooking(match, pitch);

        mockMvc.perform(discovery(viewer)
                        .queryParam("timeFrom", "18:00:00")
                        .queryParam("timeTo", "18:00:00")
                        .queryParam("startsFrom", summerStart.minusMinutes(1).toString())
                        .queryParam("startsTo", summerStart.plusMinutes(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(match.getId().toString()));
    }

    @Test
    void localTimeFilterUsesAssociatedPitchTimezoneForWinterLisbonTime() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        OffsetDateTime winterStart = OffsetDateTime.parse("2027-01-20T18:00:00Z");
        PitchEntity pitch = createPitch(organizer, "Winter Local Pitch", "Lisbon", "Europe/Lisbon");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                winterStart,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        createBooking(match, pitch);

        mockMvc.perform(discovery(viewer)
                        .queryParam("timeFrom", "18:00:00")
                        .queryParam("timeTo", "18:00:00")
                        .queryParam("startsFrom", winterStart.minusMinutes(1).toString())
                        .queryParam("startsTo", winterStart.plusMinutes(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(match.getId().toString()));
    }

    @Test
    void localTimeFilterUsesEuropeLisbonFallbackWhenMatchHasNoAssociatedPitch() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        OffsetDateTime summerStart = OffsetDateTime.parse("2027-07-21T17:00:00Z");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                summerStart,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);

        mockMvc.perform(discovery(viewer)
                        .queryParam("timeFrom", "18:00:00")
                        .queryParam("timeTo", "18:00:00")
                        .queryParam("startsFrom", summerStart.minusMinutes(1).toString())
                        .queryParam("startsTo", summerStart.plusMinutes(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(match.getId().toString()));
    }

    @Test
    void repositoryUsesConfiguredFallbackTimezoneForMatchesWithoutPitch() {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        OffsetDateTime newYorkEveningStart = OffsetDateTime.parse("2027-07-22T22:00:00Z");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                newYorkEveningStart,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);

        MatchDiscoveryCriteria criteria = new MatchDiscoveryCriteria(
                viewer.getId(),
                OffsetDateTime.now(FIXED_CLOCK),
                null,
                newYorkEveningStart.minusMinutes(1),
                newYorkEveningStart.plusMinutes(1),
                LocalTime.of(18, 0),
                LocalTime.of(18, 0),
                null,
                "America/New_York",
                false,
                0,
                20);
        List<MatchDiscoveryRow> rows = matchDiscoveryRepository.findDiscoverableMatches(criteria);

        assertThat(matchDiscoveryRepository.countDiscoverableMatches(criteria)).isEqualTo(1);
        assertThat(rows).extracting(MatchDiscoveryRow::matchId).containsExactly(match.getId());
    }

    @Test
    void invalidFallbackTimezoneConfigurationFailsClearly() {
        assertThatThrownBy(() -> new MatchDiscoveryService(
                        FIXED_CLOCK,
                        mock(MatchDiscoveryRepository.class),
                        "Not/A_Time_Zone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MatchDiscoveryService.FALLBACK_TIME_ZONE_PROPERTY);
    }

    @Test
    void effectiveBookingSelectionPrefersConfirmedOverNewerProvisional() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        OffsetDateTime summerStart = OffsetDateTime.parse("2027-07-20T17:00:00Z");
        PitchEntity confirmedPitch = createPitch(
                organizer,
                "Confirmed Pitch",
                "Confirmed Lisbon Area",
                "Europe/Lisbon");
        PitchEntity newerProvisionalPitch = createPitch(
                organizer,
                "Later Provisional Pitch",
                "Later Provisional Area",
                "America/New_York");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                summerStart,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        UUID confirmedBookingId = createBooking(match, confirmedPitch);
        setBookingStatusAndCreatedAt(
                confirmedBookingId,
                "confirmed",
                OffsetDateTime.parse("2026-10-08T08:00:00Z"));
        UUID provisionalBookingId = createBooking(match, newerProvisionalPitch);
        setBookingStatusAndCreatedAt(
                provisionalBookingId,
                "provisional",
                OffsetDateTime.parse("2026-10-08T09:00:00Z"));

        mockMvc.perform(discovery(viewer)
                        .queryParam("area", "confirmed lisbon")
                        .queryParam("timeFrom", "18:00:00")
                        .queryParam("timeTo", "18:00:00")
                        .queryParam("startsFrom", summerStart.minusMinutes(1).toString())
                        .queryParam("startsTo", summerStart.plusMinutes(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches.length()").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(match.getId().toString()))
                .andExpect(jsonPath("$.matches[0].pitchId").value(confirmedPitch.getId().toString()))
                .andExpect(jsonPath("$.matches[0].pitchName").value("Confirmed Pitch"))
                .andExpect(jsonPath("$.matches[0].pitchAddress").value("Confirmed Lisbon Area"));

        mockMvc.perform(discovery(viewer)
                        .queryParam("area", "later provisional")
                        .queryParam("startsFrom", summerStart.minusMinutes(1).toString())
                        .queryParam("startsTo", summerStart.plusMinutes(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.matches.length()").value(0));

        mockMvc.perform(discovery(viewer)
                        .queryParam("timeFrom", "13:00:00")
                        .queryParam("timeTo", "13:00:00")
                        .queryParam("startsFrom", summerStart.minusMinutes(1).toString())
                        .queryParam("startsTo", summerStart.plusMinutes(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.matches.length()").value(0));
    }

    @Test
    void provisionalBookingIsSelectedOnlyWhenNoConfirmedBookingExists() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        PitchEntity rejectedPitch = createPitch(organizer, "Rejected Pitch", "Rejected Area", "Europe/Lisbon");
        PitchEntity cancelledPitch = createPitch(organizer, "Cancelled Pitch", "Cancelled Area", "Europe/Lisbon");
        PitchEntity olderProvisionalPitch = createPitch(organizer, "Older Provisional Pitch", "Older Area", "Europe/Lisbon");
        PitchEntity newerProvisionalPitch = createPitch(organizer, "Newest Provisional Pitch", "Newest Area", "Europe/Lisbon");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                FUTURE_START,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        UUID olderProvisionalBookingId = createBooking(match, olderProvisionalPitch);
        setBookingStatusAndCreatedAt(
                olderProvisionalBookingId,
                "provisional",
                OffsetDateTime.parse("2026-10-08T08:00:00Z"));
        UUID newerProvisionalBookingId = createBooking(match, newerProvisionalPitch);
        setBookingStatusAndCreatedAt(
                newerProvisionalBookingId,
                "provisional",
                OffsetDateTime.parse("2026-10-08T09:00:00Z"));
        UUID rejectedBookingId = createBooking(match, rejectedPitch);
        setBookingStatusAndCreatedAt(
                rejectedBookingId,
                "rejected",
                OffsetDateTime.parse("2026-10-08T10:00:00Z"));
        UUID cancelledBookingId = createBooking(match, cancelledPitch);
        setBookingStatusAndCreatedAt(
                cancelledBookingId,
                "cancelled",
                OffsetDateTime.parse("2026-10-08T11:00:00Z"));

        mockMvc.perform(discovery(viewer)
                        .queryParam("area", "newest")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches.length()").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(match.getId().toString()))
                .andExpect(jsonPath("$.matches[0].pitchId").value(newerProvisionalPitch.getId().toString()))
                .andExpect(jsonPath("$.matches[0].pitchName").value("Newest Provisional Pitch"));

        mockMvc.perform(discovery(viewer)
                        .queryParam("area", "rejected")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.matches.length()").value(0));

        mockMvc.perform(discovery(viewer)
                        .queryParam("area", "cancelled")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.matches.length()").value(0));
    }

    @Test
    void effectiveBookingSelectionUsesStableIdTieBreakerWithinSameStatus() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        PitchEntity firstPitch = createPitch(organizer, "Tie Breaker First Pitch", "Tie Breaker Area", "Europe/Lisbon");
        PitchEntity secondPitch = createPitch(organizer, "Tie Breaker Second Pitch", "Tie Breaker Area", "Europe/Lisbon");
        MatchEntity match = createRecruitingMatch(
                organizer,
                GroupVisibility.PUBLIC,
                FUTURE_START,
                MatchJoinMode.OPEN_JOIN,
                true,
                10);
        UUID firstBookingId = createBooking(match, firstPitch);
        UUID secondBookingId = createBooking(match, secondPitch);
        OffsetDateTime sameTimestamp = OffsetDateTime.parse("2026-10-08T08:00:00Z");
        setBookingStatusAndCreatedAt(firstBookingId, "provisional", sameTimestamp);
        setBookingStatusAndCreatedAt(secondBookingId, "provisional", sameTimestamp);
        UUID expectedPitchId = jdbcTemplate.queryForObject(
                "select pitch_id from bookings where id in (?, ?) order by id asc limit 1",
                UUID.class,
                firstBookingId,
                secondBookingId);

        mockMvc.perform(discovery(viewer)
                        .queryParam("area", "tie breaker")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.matches.length()").value(1))
                .andExpect(jsonPath("$.matches[0].matchId").value(match.getId().toString()))
                .andExpect(jsonPath("$.matches[0].pitchId").value(expectedPitchId.toString()));
    }
    @Test
    void paginationIsBoundedAndDeterministic() throws Exception {
        UserEntity viewer = createUser();
        UserEntity organizer = createUser();
        MatchEntity first = createRecruitingMatch(organizer, GroupVisibility.PUBLIC, FUTURE_START, MatchJoinMode.OPEN_JOIN, true, 10);
        MatchEntity second = createRecruitingMatch(organizer, GroupVisibility.PUBLIC, FUTURE_START, MatchJoinMode.REQUEST_TO_JOIN, true, 10);
        second.setStartsAt(first.getStartsAt());
        second.setEndsAt(first.getEndsAt());
        matchRepository.save(second);

        List<UUID> orderedIds = jdbcTemplate.query(
                "select id from matches where id in (?, ?) order by starts_at asc, id asc",
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                first.getId(),
                second.getId());
        UUID expectedFirst = orderedIds.get(0);
        UUID expectedSecond = orderedIds.get(1);

        mockMvc.perform(discovery(viewer)
                        .queryParam("size", "1")
                        .queryParam("page", "0")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.matches[0].matchId").value(expectedFirst.toString()));

        mockMvc.perform(discovery(viewer)
                        .queryParam("size", "1")
                        .queryParam("page", "1")
                        .queryParam("startsFrom", FUTURE_START.minusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].matchId").value(expectedSecond.toString()));
    }

    @Test
    void invalidFiltersFailSafely() throws Exception {
        UserEntity viewer = createUser();

        mockMvc.perform(discovery(viewer).queryParam("startsFrom", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
        mockMvc.perform(discovery(viewer).queryParam("timeFrom", "99:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
        mockMvc.perform(discovery(viewer).queryParam("joinMode", "unsupported"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
        mockMvc.perform(discovery(viewer).queryParam("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("domain_error"));
        mockMvc.perform(discovery(viewer).queryParam("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("domain_error"));
        mockMvc.perform(discovery(viewer)
                        .queryParam("startsFrom", FUTURE_START.plusHours(1).toString())
                        .queryParam("startsTo", FUTURE_START.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("domain_error"));
        mockMvc.perform(discovery(viewer)
                        .queryParam("timeFrom", "11:00:00")
                        .queryParam("timeTo", "10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("domain_error"));
    }

    @Test
    void existingJoinFlowStillWorks() throws Exception {
        UserEntity organizer = createUser();
        UserEntity player = createUser();
        MatchEntity match = createRecruitingMatch(organizer, GroupVisibility.PUBLIC, FUTURE_START, MatchJoinMode.OPEN_JOIN, true, 10);

        mockMvc.perform(post("/api/v1/matches/{matchId}/join", match.getId()).with(jwtFor(player)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(player.getId().toString()))
                .andExpect(jsonPath("$.status").value("awaiting_payment"));
    }

    private MockHttpServletRequestBuilder discovery(final UserEntity user) {
        return get("/api/v1/matches/discovery").with(jwtFor(user));
    }

    private MatchEntity createRecruitingMatch(
            final UserEntity organizer,
            final GroupVisibility visibility,
            final OffsetDateTime startsAt,
            final MatchJoinMode joinMode,
            final boolean publicVacanciesEnabled,
            final int maxPlayers) {
        return createMatchWithStatus(
                organizer,
                visibility,
                startsAt,
                MatchStatus.RECRUITING,
                publicVacanciesEnabled,
                joinMode,
                maxPlayers);
    }

    private MatchEntity createMatchWithStatus(
            final UserEntity organizer,
            final GroupVisibility visibility,
            final OffsetDateTime startsAt,
            final MatchStatus status,
            final boolean publicVacanciesEnabled) {
        return createMatchWithStatus(
                organizer,
                visibility,
                startsAt,
                status,
                publicVacanciesEnabled,
                MatchJoinMode.OPEN_JOIN,
                10);
    }

    private MatchEntity createMatchWithStatus(
            final UserEntity organizer,
            final GroupVisibility visibility,
            final OffsetDateTime startsAt,
            final MatchStatus status,
            final boolean publicVacanciesEnabled,
            final MatchJoinMode joinMode,
            final int maxPlayers) {
        GroupEntity group = groupService.createGroup(new CreateGroupCommand(
                uniqueName("Discovery Group"),
                "Discovery group",
                visibility,
                organizer.getId()));
        MatchEntity match = matchService.createMatch(new CreateMatchCommand(
                group.getId(),
                organizer.getId(),
                startsAt,
                60,
                maxPlayers,
                joinMode,
                publicVacanciesEnabled,
                MatchFundingMode.SPLIT_PAYMENT));
        match.setStatus(status);
        return matchRepository.save(match);
    }

    private UUID createBooking(final MatchEntity match, final PitchEntity pitch) {
        return bookingService.createProvisionalBooking(new CreateProvisionalBookingCommand(
                match.getCreatedByUser().getId(),
                match.getId(),
                pitch.getId(),
                new BigDecimal("60.00"),
                "EUR")).getId();
    }

    private void setBookingStatusAndCreatedAt(
            final UUID bookingId,
            final String status,
            final OffsetDateTime timestamp) {
        OffsetDateTime confirmedAt = "confirmed".equals(status) ? timestamp : null;
        OffsetDateTime rejectedAt = "rejected".equals(status) ? timestamp : null;
        OffsetDateTime cancelledAt = "cancelled".equals(status) ? timestamp : null;
        jdbcTemplate.update(
                """
                        update bookings
                        set status = ?,
                            created_at = ?,
                            updated_at = ?,
                            confirmed_at = ?,
                            rejected_at = ?,
                            cancelled_at = ?
                        where id = ?
                        """,
                status,
                timestamp,
                timestamp,
                confirmedAt,
                rejectedAt,
                cancelledAt,
                bookingId);
    }
    private PitchEntity createPitch(final UserEntity owner, final String name, final String address) {
        return createPitch(owner, name, address, "Europe/Lisbon");
    }

    private PitchEntity createPitch(
            final UserEntity owner,
            final String name,
            final String address,
            final String timezone) {
        activateOwner(owner);
        PitchEntity pitch = pitchService.createPitch(new CreatePitchCommand(
                owner.getId(),
                name,
                "Discovery pitch",
                address,
                new BigDecimal("38.7200"),
                new BigDecimal("-9.1300"),
                timezone,
                new BigDecimal("60.00"),
                "EUR",
                true));
        for (short dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            pitchScheduleService.createSchedule(new CreatePitchScheduleCommand(
                    pitch.getId(),
                    owner.getId(),
                    dayOfWeek,
                    LocalTime.of(0, 0),
                    LocalTime.of(23, 59)));
        }
        return pitch;
    }

    private void createParticipant(
            final MatchEntity match,
            final UserEntity user,
            final MatchParticipantStatus status) {
        MatchParticipantEntity participant = new MatchParticipantEntity();
        participant.setId(UUID.randomUUID());
        participant.setMatch(match);
        participant.setUser(user);
        participant.setStatus(status);
        participant.setJoinedAt(OffsetDateTime.now(FIXED_CLOCK));
        if (status == MatchParticipantStatus.CONFIRMED) {
            participant.setConfirmedAt(OffsetDateTime.now(FIXED_CLOCK));
        }
        if (status == MatchParticipantStatus.CANCELLED) {
            participant.setCancelledAt(OffsetDateTime.now(FIXED_CLOCK));
        }
        participantRepository.save(participant);
    }

    private void addActiveMember(final GroupEntity group, final UserEntity user) {
        GroupMemberEntity member = new GroupMemberEntity();
        member.setId(new GroupMemberId(group.getId(), user.getId()));
        member.setGroup(group);
        member.setUser(user);
        member.setRole(GroupMemberRole.MEMBER);
        member.setStatus(GroupMemberStatus.ACTIVE);
        member.setJoinedAt(OffsetDateTime.now(FIXED_CLOCK));
        member.setUpdatedAt(OffsetDateTime.now(FIXED_CLOCK));
        groupMemberRepository.save(member);
    }

    private UserEntity createUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(uniqueName("user") + "@example.test");
        user.setName(uniqueName("User"));
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setAuthProvider("supabase");
        user.setAuthSubject(user.getId().toString());
        user.setCreatedAt(OffsetDateTime.now(FIXED_CLOCK));
        user.setUpdatedAt(OffsetDateTime.now(FIXED_CLOCK));
        return userRepository.save(user);
    }

    private void activateOwner(final UserEntity user) {
        PitchOwnerProfileEntity profile = new PitchOwnerProfileEntity();
        profile.setUser(user);
        profile.setActivatedAt(OffsetDateTime.now(FIXED_CLOCK));
        profile.setCreatedAt(OffsetDateTime.now(FIXED_CLOCK));
        pitchOwnerProfileRepository.save(profile);
    }

    private RequestPostProcessor jwtFor(final UserEntity user) {
        return jwt().jwt(token -> token.subject(user.getAuthSubject()));
    }

    private void clearData() {
        jdbcTemplate.update("delete from match_funding_contributions");
        jdbcTemplate.update("delete from booking_rejections");
        jdbcTemplate.update("delete from bookings");
        jdbcTemplate.update("delete from match_admins");
        jdbcTemplate.update("delete from match_participants");
        jdbcTemplate.update("delete from matches");
        jdbcTemplate.update("delete from group_members");
        jdbcTemplate.update("delete from groups");
        jdbcTemplate.update("delete from pitch_blocks");
        jdbcTemplate.update("delete from pitch_schedules");
        jdbcTemplate.update("delete from pitch_images");
        jdbcTemplate.update("delete from pitches");
        jdbcTemplate.update("delete from pitch_owner_profiles");
        jdbcTemplate.update("delete from users");
    }

    private String uniqueName(final String prefix) {
        return prefix + " " + UUID.randomUUID();
    }
}
