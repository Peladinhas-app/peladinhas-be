package com.peladinhas.backend.domains.core.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberRole;
import com.peladinhas.backend.domains.groups.persistence.GroupMemberStatus;
import com.peladinhas.backend.domains.groups.persistence.GroupRepository;
import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
class CoreMatchApiTests extends PostgreSqlContainerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-18T12:00:00Z"),
            ZoneOffset.UTC);
    private static final OffsetDateTime DEFAULT_START = OffsetDateTime.parse("2026-09-19T19:00:00Z");

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupRepository groupRepository;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void authenticatedUserCreatesGroupAsThemselves() throws Exception {
        UserEntity creator = createUser();

        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", uniqueName("Group"),
                                "description", "Group description",
                                "visibility", "private"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.visibility").value("private"))
                .andReturn();

        UUID groupId = uuidAt(result, "id");
        assertThat(groupMemberRepository.existsByGroup_IdAndUser_IdAndRoleAndStatus(
                groupId,
                creator.getId(),
                GroupMemberRole.ADMIN,
                GroupMemberStatus.ACTIVE)).isTrue();
    }

    @Test
    void unauthenticatedGroupCreationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", uniqueName("Group"),
                                "description", "Group description",
                                "visibility", "private"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    @Test
    void obsoleteCreatorUserIdIsRejectedOnGroupCreation() throws Exception {
        UserEntity creator = createUser();
        UserEntity spoofedUser = createUser();

        mockMvc.perform(post("/api/v1/groups")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "creatorUserId", spoofedUser.getId(),
                                "name", uniqueName("Group"),
                                "description", "Group description",
                                "visibility", "private"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void invalidGroupRequestReturnsBadRequest() throws Exception {
        UserEntity creator = createUser();

        mockMvc.perform(post("/api/v1/groups")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("visibility", "private"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void malformedJsonReturnsStableBadRequest() throws Exception {
        UserEntity creator = createUser();

        mockMvc.perform(post("/api/v1/groups")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void invalidJsonValueTypeReturnsStableBadRequest() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);
        String body = """
                {
                  "groupId": "%s",
                  "startsAt": "%s",
                  "durationMinutes": 60,
                  "maxPlayers": "many",
                  "joinMode": "open_join",
                  "publicVacanciesEnabled": true
                }
                """.formatted(groupId, DEFAULT_START);

        mockMvc.perform(post("/api/v1/matches")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void malformedStartTimeReturnsStableBadRequest() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);
        String body = """
                {
                  "groupId": "%s",
                  "startsAt": "not-a-date",
                  "durationMinutes": 60,
                  "maxPlayers": 10,
                  "joinMode": "open_join",
                  "publicVacanciesEnabled": true
                }
                """.formatted(groupId);

        mockMvc.perform(post("/api/v1/matches")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void invalidUuidPathVariableReturnsStableBadRequest() throws Exception {
        UserEntity creator = createUser();

        mockMvc.perform(get("/api/v1/matches/not-a-uuid").with(jwtFor(creator)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void authenticatedUserCreatesMatchAsThemselves() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);

        MvcResult result = createMatch(groupId, creator, 90, 10, "open_join");

        json(result).andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.createdByUserId").value(creator.getId().toString()))
                .andExpect(jsonPath("$.endsAt").value("2026-09-19T20:30:00Z"))
                .andExpect(jsonPath("$.joinMode").value("open_join"))
                .andExpect(jsonPath("$.status").value("draft"));
    }

    @Test
    void authenticatedUserCreatesDirectMatchAsThemselves() throws Exception {
        UserEntity creator = createUser();

        MvcResult result = mockMvc.perform(post("/api/v1/matches/direct")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "groupName", uniqueName("Direct Group"),
                                "groupDescription", "Direct group description",
                                "startsAt", DEFAULT_START,
                                "durationMinutes", 60,
                                "maxPlayers", 8,
                                "joinMode", "request_to_join",
                                "publicVacanciesEnabled", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByUserId").value(creator.getId().toString()))
                .andExpect(jsonPath("$.groupName").exists())
                .andExpect(jsonPath("$.joinMode").value("request_to_join"))
                .andReturn();

        UUID groupId = uuidAt(result, "groupId");
        assertThat(groupRepository.findById(groupId)).hasValueSatisfying(group ->
                assertThat(group.getVisibility()).isEqualTo(GroupVisibility.PUBLIC));
    }

    @Test
    void obsoleteCreatorUserIdIsRejectedOnMatchCreation() throws Exception {
        UserEntity creator = createUser();
        UserEntity spoofedUser = createUser();
        UUID groupId = createGroup(creator);

        mockMvc.perform(post("/api/v1/matches")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "groupId", groupId,
                                "creatorUserId", spoofedUser.getId(),
                                "startsAt", DEFAULT_START,
                                "durationMinutes", 60,
                                "maxPlayers", 10,
                                "joinMode", "open_join",
                                "publicVacanciesEnabled", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void unsupportedDurationReturnsBadRequest() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);

        mockMvc.perform(post("/api/v1/matches")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matchRequest(groupId, 45, 10, "open_join")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void secondActiveUpcomingMatchReturnsConflict() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);
        createMatch(groupId, creator, 60, 10, "open_join");

        mockMvc.perform(post("/api/v1/matches")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matchRequest(groupId, 60, 10, "open_join")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));
    }

    @Test
    void authenticatedMatchAdminTransitionsMatchStatus() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);
        UUID matchId = uuidAt(createMatch(groupId, creator, 60, 10, "open_join"), "id");

        mockMvc.perform(post("/api/v1/matches/{matchId}/status-transitions", matchId)
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nextStatus", "recruiting"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchId.toString()))
                .andExpect(jsonPath("$.status").value("recruiting"));
    }

    @Test
    void nonAdminStatusTransitionReturnsForbidden() throws Exception {
        UserEntity creator = createUser();
        UserEntity nonAdmin = createUser();
        UUID groupId = createGroup(creator);
        UUID matchId = uuidAt(createMatch(groupId, creator, 60, 10, "open_join"), "id");

        mockMvc.perform(post("/api/v1/matches/{matchId}/status-transitions", matchId)
                        .with(jwtFor(nonAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nextStatus", "recruiting"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));
    }

    @Test
    void invalidStatusTransitionReturnsConflict() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);
        UUID matchId = uuidAt(createMatch(groupId, creator, 60, 10, "open_join"), "id");

        mockMvc.perform(post("/api/v1/matches/{matchId}/status-transitions", matchId)
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nextStatus", "completed"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));
    }

    @Test
    void malformedNextStatusReturnsStableBadRequest() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);
        UUID matchId = uuidAt(createMatch(groupId, creator, 60, 10, "open_join"), "id");

        mockMvc.perform(post("/api/v1/matches/{matchId}/status-transitions", matchId)
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nextStatus", "warming_up"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void obsoleteActingAdminUserIdIsRejectedOnStatusTransition() throws Exception {
        UserEntity creator = createUser();
        UserEntity spoofedAdmin = createUser();
        UUID groupId = createGroup(creator);
        UUID matchId = uuidAt(createMatch(groupId, creator, 60, 10, "open_join"), "id");

        mockMvc.perform(post("/api/v1/matches/{matchId}/status-transitions", matchId)
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "actingAdminUserId", spoofedAdmin.getId(),
                                "nextStatus", "recruiting"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void getExistingAndUnknownMatch() throws Exception {
        UserEntity creator = createUser();
        UUID groupId = createGroup(creator);
        UUID matchId = uuidAt(createMatch(groupId, creator, 60, 10, "open_join"), "id");

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{matchId}", matchId).with(jwtFor(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchId.toString()))
                .andExpect(jsonPath("$.groupName").exists())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("hibernateLazyInitializer", "handler");

        mockMvc.perform(get("/api/v1/matches/{matchId}", UUID.randomUUID()).with(jwtFor(creator)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource_not_found"));
    }

    @Test
    void authenticatedPlayerOpenJoinsAsThemselvesAndCapacityConflictRemains() throws Exception {
        UserEntity creator = createUser();
        UserEntity firstPlayer = createUser();
        UserEntity secondPlayer = createUser();
        UUID matchId = createRecruitingMatch(creator, "open_join", 1);

        mockMvc.perform(post("/api/v1/matches/{matchId}/join", matchId)
                        .with(jwtFor(firstPlayer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(firstPlayer.getId().toString()))
                .andExpect(jsonPath("$.status").value("awaiting_payment"));

        mockMvc.perform(post("/api/v1/matches/{matchId}/join", matchId)
                        .with(jwtFor(secondPlayer)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));
    }

    @Test
    void openJoinIgnoresSpoofedUserIdAndUsesAuthenticatedPlayer() throws Exception {
        UserEntity creator = createUser();
        UserEntity authenticatedPlayer = createUser();
        UserEntity spoofedPlayer = createUser();
        UUID matchId = createRecruitingMatch(creator, "open_join", 10);

        mockMvc.perform(post("/api/v1/matches/{matchId}/join", matchId)
                        .with(jwtFor(authenticatedPlayer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", spoofedPlayer.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(authenticatedPlayer.getId().toString()))
                .andExpect(jsonPath("$.status").value("awaiting_payment"));
    }

    @Test
    void openJoinWithRequestModeReturnsConflict() throws Exception {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        UUID matchId = createRecruitingMatch(creator, "request_to_join", 10);

        mockMvc.perform(post("/api/v1/matches/{matchId}/join", matchId)
                        .with(jwtFor(player)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("business_conflict"));
    }

    @Test
    void requestJoinApproveAndRejectUseAuthenticatedActors() throws Exception {
        UserEntity creator = createUser();
        UserEntity approvedPlayer = createUser();
        UserEntity rejectedPlayer = createUser();
        UUID matchId = createRecruitingMatch(creator, "request_to_join", 10);

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests", matchId)
                        .with(jwtFor(approvedPlayer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(approvedPlayer.getId().toString()))
                .andExpect(jsonPath("$.status").value("requested"));

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests/{userId}/approve", matchId, approvedPlayer.getId())
                        .with(jwtFor(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(approvedPlayer.getId().toString()))
                .andExpect(jsonPath("$.status").value("approved"));

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests", matchId)
                        .with(jwtFor(rejectedPlayer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(rejectedPlayer.getId().toString()))
                .andExpect(jsonPath("$.status").value("requested"));

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests/{userId}/reject", matchId, rejectedPlayer.getId())
                        .with(jwtFor(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(rejectedPlayer.getId().toString()))
                .andExpect(jsonPath("$.status").value("rejected"));
    }

    @Test
    void requestToJoinIgnoresSpoofedUserIdAndUsesAuthenticatedPlayer() throws Exception {
        UserEntity creator = createUser();
        UserEntity authenticatedPlayer = createUser();
        UserEntity spoofedPlayer = createUser();
        UUID matchId = createRecruitingMatch(creator, "request_to_join", 10);

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests", matchId)
                        .with(jwtFor(authenticatedPlayer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", spoofedPlayer.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(authenticatedPlayer.getId().toString()))
                .andExpect(jsonPath("$.status").value("requested"));
    }

    @Test
    void nonAdminApprovalReturnsForbidden() throws Exception {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        UserEntity nonAdmin = createUser();
        UUID matchId = createRecruitingMatch(creator, "request_to_join", 10);
        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests", matchId)
                        .with(jwtFor(player)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests/{userId}/approve", matchId, player.getId())
                        .with(jwtFor(nonAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));
    }

    @Test
    void adminActionBodyWithObsoleteActingAdminUserIdDoesNotBypassCurrentUser() throws Exception {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        UserEntity nonAdmin = createUser();
        UUID matchId = createRecruitingMatch(creator, "request_to_join", 10);
        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests", matchId)
                        .with(jwtFor(player)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests/{userId}/approve", matchId, player.getId())
                        .with(jwtFor(nonAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("actingAdminUserId", creator.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("permission_denied"));
    }

    @Test
    void authenticatedPrincipalWithoutLocalUserReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
                        .with(jwt().jwt(token -> token.subject("missing-user-subject")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", uniqueName("Group"),
                                "description", "Group description",
                                "visibility", "private"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("authenticated_user_not_found"));
    }

    @Test
    void awaitingPaymentEndpointIsNotPubliclyAvailableUntilActorDecisionIsFinalized() throws Exception {
        UserEntity creator = createUser();
        UserEntity player = createUser();
        UUID matchId = createRecruitingMatch(creator, "request_to_join", 10);
        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests", matchId)
                        .with(jwtFor(player)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests/{userId}/approve", matchId, player.getId())
                        .with(jwtFor(creator)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/matches/{matchId}/join-requests/{userId}/awaiting-payment", matchId, player.getId())
                        .with(jwtFor(player)))
                .andExpect(status().isNotFound());
    }

    private MvcResult createMatch(
            final UUID groupId,
            final UserEntity creator,
            final int durationMinutes,
            final int maxPlayers,
            final String joinMode) throws Exception {
        return mockMvc.perform(post("/api/v1/matches")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(matchRequest(groupId, durationMinutes, maxPlayers, joinMode)))
                .andReturn();
    }

    private UUID createRecruitingMatch(final UserEntity creator, final String joinMode, final int maxPlayers) throws Exception {
        UUID groupId = createGroup(creator);
        UUID matchId = uuidAt(createMatch(groupId, creator, 60, maxPlayers, joinMode), "id");
        transitionMatchToRecruiting(matchId, creator);
        return matchId;
    }

    private void transitionMatchToRecruiting(final UUID matchId, final UserEntity actingAdmin) throws Exception {
        mockMvc.perform(post("/api/v1/matches/{matchId}/status-transitions", matchId)
                        .with(jwtFor(actingAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nextStatus", "recruiting"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("recruiting"));
    }

    private UUID createGroup(final UserEntity creator) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .with(jwtFor(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", uniqueName("Group"),
                                "description", "Group description",
                                "visibility", "private"))))
                .andExpect(status().isCreated())
                .andReturn();
        return uuidAt(result, "id");
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

    private String matchRequest(
            final UUID groupId,
            final int durationMinutes,
            final int maxPlayers,
            final String joinMode) throws Exception {
        return json(Map.of(
                "groupId", groupId,
                "startsAt", DEFAULT_START,
                "durationMinutes", durationMinutes,
                "maxPlayers", maxPlayers,
                "joinMode", joinMode,
                "publicVacanciesEnabled", true));
    }

    private RequestPostProcessor jwtFor(final UserEntity user) {
        return jwt().jwt(token -> token.subject(user.getAuthSubject()));
    }

    private ResultMatcherWithReturn json(final MvcResult result) {
        return new ResultMatcherWithReturn(result);
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

    private final class ResultMatcherWithReturn {

        private final MvcResult result;

        private ResultMatcherWithReturn(final MvcResult result) {
            this.result = result;
        }

        private ResultMatcherWithReturn andExpect(final ResultMatcher matcher) throws Exception {
            matcher.match(result);
            return this;
        }
    }
}
