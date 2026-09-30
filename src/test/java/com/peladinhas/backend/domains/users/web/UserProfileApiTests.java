package com.peladinhas.backend.domains.users.web;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerInvitationCodeEntity;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerInvitationCodeRepository;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerProfileRepository;
import com.peladinhas.backend.domains.owners.service.OwnerInvitationCodeHasher;
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
class UserProfileApiTests extends PostgreSqlContainerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-22T10:00:00Z"),
            ZoneOffset.UTC);

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OwnerInvitationCodeHasher ownerInvitationCodeHasher;

    @Autowired
    private PitchOwnerInvitationCodeRepository pitchOwnerInvitationCodeRepository;

    @Autowired
    private PitchOwnerProfileRepository pitchOwnerProfileRepository;

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
    void authenticatedUserWithoutLocalProfileCreatesProfileFromTrustedJwtIdentity() throws Exception {
        UUID externalSubject = UUID.randomUUID();
        String email = uniqueEmail("onboarding");

        MvcResult result = mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor(externalSubject.toString(), email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Onboarded User", "pt"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Onboarded User"))
                .andExpect(jsonPath("$.preferredLanguage").value("pt"))
                .andExpect(jsonPath("$.capabilities.player").value(true))
                .andExpect(jsonPath("$.capabilities.pitchOwner").value(false))
                .andExpect(jsonPath("$.authSubject").doesNotExist())
                .andReturn();

        UUID internalUserId = uuidAt(result, "id");
        assertThat(internalUserId).isNotEqualTo(externalSubject);
        UserEntity persisted = userRepository.findById(internalUserId).orElseThrow();
        assertThat(persisted.getAuthProvider()).isEqualTo("supabase");
        assertThat(persisted.getAuthSubject()).isEqualTo(externalSubject.toString());
        assertThat(persisted.getEmail()).isEqualTo(email);
        assertThat(persisted.getName()).isEqualTo("Onboarded User");
        assertThat(persisted.getPreferredLanguage()).isEqualTo(PreferredLanguage.PORTUGUESE);
        assertThat(pitchOwnerProfileRepository.existsById(internalUserId)).isFalse();
    }

    @Test
    void profileCreationRejectsSpoofedIdentityFields() throws Exception {
        long userCountBefore = userRepository.count();
        Map<String, Object> request = profileRequest("Spoof Attempt", "en");
        request.put("id", UUID.randomUUID());
        request.put("email", "spoofed@example.test");
        request.put("authProvider", "development");
        request.put("authSubject", "spoofed-subject");

        mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor("real-subject-" + UUID.randomUUID(), uniqueEmail("real")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));

        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }

    @Test
    void playerOnboardingWithExplicitAccountTypeDoesNotRequireInvitation() throws Exception {
        String email = uniqueEmail("player");

        MvcResult result = mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor("player-subject-" + UUID.randomUUID(), email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Player User", "en", "PLAYER", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capabilities.player").value(true))
                .andExpect(jsonPath("$.capabilities.pitchOwner").value(false))
                .andReturn();

        assertThat(pitchOwnerProfileRepository.existsById(uuidAt(result, "id"))).isFalse();
    }

    @Test
    void validOwnerInvitationCreatesPlayerAndPitchOwnerCapability() throws Exception {
        String invitationCode = uniqueInvitationCode();
        createInvitation(invitationCode, null);

        MvcResult result = mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor("owner-subject-" + UUID.randomUUID(), uniqueEmail("owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Owner User", "pt", "PITCH_OWNER", invitationCode))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capabilities.player").value(true))
                .andExpect(jsonPath("$.capabilities.pitchOwner").value(true))
                .andReturn();

        UUID userId = uuidAt(result, "id");
        assertThat(pitchOwnerProfileRepository.existsById(userId)).isTrue();
        String invitationHash = ownerInvitationCodeHasher.hash(invitationCode);
        assertThat(pitchOwnerInvitationCodeRepository.findAll().stream()
                .filter(invitation -> invitation.getCodeHash().equals(invitationHash))
                .toList()).singleElement()
                .satisfies(invitation -> {
                    assertThat(invitation.getUsedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
                    assertThat(invitation.getUsedByUser().getId()).isEqualTo(userId);
                });
    }

    @Test
    void invalidExpiredAndUsedOwnerInvitationsAreRejectedWithoutCreatingProfile() throws Exception {
        String expiredCode = uniqueInvitationCode();
        String usedCode = uniqueInvitationCode();
        createInvitation(expiredCode, OffsetDateTime.now(FIXED_CLOCK).minusMinutes(1));
        createUsedInvitation(usedCode);
        long userCountBefore = userRepository.count();

        expectOwnerOnboardingRejected("missing-code", null, "invalid_request");
        expectOwnerOnboardingRejected("invalid-code", uniqueInvitationCode(), "invalid_request");
        expectOwnerOnboardingRejected("expired-code", expiredCode, "invalid_request");
        expectOwnerOnboardingRejected("used-code", usedCode, "owner_invitation_code_used");

        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }

    @Test
    void concurrentOwnerInvitationRedemptionAllowsExactlyOneSuccess() throws Exception {
        String invitationCode = uniqueInvitationCode();
        createInvitation(invitationCode, null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> createOwnerProfileStatus("concurrent-a", invitationCode, ready, start));
            Future<Integer> second = executor.submit(() -> createOwnerProfileStatus("concurrent-b", invitationCode, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(pitchOwnerProfileRepository.findAll()).hasSize(1);
            assertThat(pitchOwnerInvitationCodeRepository.findAll()).singleElement()
                    .satisfies(invitation -> assertThat(invitation.getUsedAt()).isNotNull());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void existingPlayerActivatesPitchOwnerCapabilityFromProfileSettings() throws Exception {
        UserEntity user = createUser("settings-subject-" + UUID.randomUUID(), uniqueEmail("settings"));
        String invitationCode = uniqueInvitationCode();
        createInvitation(invitationCode, null);

        mockMvc.perform(post("/api/v1/profile/pitch-owner")
                        .with(jwtFor(user.getAuthSubject(), user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("invitationCode", invitationCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.capabilities.player").value(true))
                .andExpect(jsonPath("$.capabilities.pitchOwner").value(true));

        assertThat(pitchOwnerProfileRepository.existsById(user.getId())).isTrue();
    }

    @Test
    void duplicateProfileCreationForSameAuthenticatedIdentityIsRejected() throws Exception {
        String subject = "duplicate-subject-" + UUID.randomUUID();
        String email = uniqueEmail("duplicate");

        mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor(subject, email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("First Profile", "en"))))
                .andExpect(status().isCreated());
        long userCountAfterFirstCreate = userRepository.count();

        mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor(subject, email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Second Profile", "pt"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("profile_conflict"));

        assertThat(userRepository.count()).isEqualTo(userCountAfterFirstCreate);
    }

    @Test
    void profileCreationWithAlreadyUsedAuthenticatedEmailIsRejected() throws Exception {
        String email = uniqueEmail("occupied");
        createUser("existing-subject-" + UUID.randomUUID(), email);
        long userCountBefore = userRepository.count();

        mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor("new-subject-" + UUID.randomUUID(), email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Email Conflict", "en"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("profile_conflict"));

        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }

    @Test
    void missingAuthenticatedEmailClaimReturnsStableBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/profile")
                        .with(jwt().jwt(token -> token.subject("missing-email-" + UUID.randomUUID())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Missing Email", "en"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void invalidProfileRequestReturnsStableBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor("invalid-request-" + UUID.randomUUID(), uniqueEmail("invalid")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("preferredLanguage", "en"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor("invalid-language-" + UUID.randomUUID(), uniqueEmail("language")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Invalid Language", "es"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    @Test
    void mappedAuthenticatedUserGetsProfile() throws Exception {
        UserEntity user = createUser("lookup-subject-" + UUID.randomUUID(), uniqueEmail("lookup"));

        mockMvc.perform(get("/api/v1/profile")
                        .with(jwtFor(user.getAuthSubject(), user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.name").value(user.getName()))
                .andExpect(jsonPath("$.preferredLanguage").value("en"))
                .andExpect(jsonPath("$.capabilities.player").value(true))
                .andExpect(jsonPath("$.capabilities.pitchOwner").value(false));
    }

    @Test
    void authenticatedUserWithoutLocalProfileGetsStableProfileNotFoundError() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                        .with(jwtFor("missing-profile-" + UUID.randomUUID(), uniqueEmail("missing"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("authenticated_user_not_found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void unauthenticatedProfileRequestsReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Anonymous", "en"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"));
    }

    private void expectOwnerOnboardingRejected(
            final String subjectPrefix,
            final String invitationCode,
            final String expectedCode) throws Exception {
        mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor(subjectPrefix + "-" + UUID.randomUUID(), uniqueEmail(subjectPrefix)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Rejected Owner", "en", "PITCH_OWNER", invitationCode))))
                .andExpect(status().is(expectedCode.equals("owner_invitation_code_used") ? 409 : 400))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private int createOwnerProfileStatus(
            final String subjectPrefix,
            final String invitationCode,
            final CountDownLatch ready,
            final CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        return mockMvc.perform(post("/api/v1/profile")
                        .with(jwtFor(subjectPrefix + "-" + UUID.randomUUID(), uniqueEmail(subjectPrefix)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileRequest("Concurrent Owner", "en", "PITCH_OWNER", invitationCode))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private PitchOwnerInvitationCodeEntity createInvitation(
            final String invitationCode,
            final OffsetDateTime expiresAt) {
        PitchOwnerInvitationCodeEntity invitation = new PitchOwnerInvitationCodeEntity();
        invitation.setId(UUID.randomUUID());
        invitation.setCodeHash(ownerInvitationCodeHasher.hash(invitationCode));
        invitation.setExpiresAt(expiresAt);
        invitation.setCreatedAt(OffsetDateTime.now(FIXED_CLOCK));
        return pitchOwnerInvitationCodeRepository.save(invitation);
    }

    private PitchOwnerInvitationCodeEntity createUsedInvitation(final String invitationCode) {
        UserEntity user = createUser("used-invitation-" + UUID.randomUUID(), uniqueEmail("used-invitation"));
        PitchOwnerInvitationCodeEntity invitation = createInvitation(invitationCode, null);
        invitation.setUsedAt(OffsetDateTime.now(FIXED_CLOCK));
        invitation.setUsedByUser(user);
        return pitchOwnerInvitationCodeRepository.save(invitation);
    }

    private UserEntity createUser(final String authSubject, final String email) {
        OffsetDateTime now = OffsetDateTime.now(FIXED_CLOCK);
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName("Existing User");
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setAuthProvider("supabase");
        user.setAuthSubject(authSubject);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private Map<String, Object> profileRequest(final String name, final String preferredLanguage) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("preferredLanguage", preferredLanguage);
        return request;
    }

    private Map<String, Object> profileRequest(
            final String name,
            final String preferredLanguage,
            final String accountType,
            final String ownerInvitationCode) {
        Map<String, Object> request = profileRequest(name, preferredLanguage);
        request.put("accountType", accountType);
        if (ownerInvitationCode != null) {
            request.put("ownerInvitationCode", ownerInvitationCode);
        }
        return request;
    }

    private RequestPostProcessor jwtFor(final String subject, final String email) {
        return jwt().jwt(token -> token.subject(subject).claim("email", email));
    }

    private String json(final Map<String, Object> values) throws Exception {
        return objectMapper.writeValueAsString(new LinkedHashMap<>(values));
    }

    private UUID uuidAt(final MvcResult result, final String field) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(root.get(field).asText());
    }

    private String uniqueInvitationCode() {
        return "owner-code-" + UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private String uniqueEmail(final String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }
}
