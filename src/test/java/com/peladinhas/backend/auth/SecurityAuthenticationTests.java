package com.peladinhas.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.support.PostgreSqlContainerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@SpringBootTest
@Import(SecurityAuthenticationTests.CurrentUserTestController.class)
class SecurityAuthenticationTests extends PostgreSqlContainerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedProtectedRequestReturnsStableUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void invalidBearerTokenReturnsStableUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthenticated"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void authenticatedPrincipalMappedToLocalUserResolvesCurrentUser() throws Exception {
        UserEntity user = createUser("supabase-subject-a");

        mockMvc.perform(get("/api/v1/test/current-user")
                        .with(jwt().jwt(token -> token.subject("supabase-subject-a")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void authenticatedPrincipalWithoutLocalUserReturnsStableForbiddenError() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user")
                        .with(jwt().jwt(token -> token.subject("missing-subject"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("authenticated_user_not_found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void authenticatedUserCannotBecomeAnotherUserByProvidingADifferentUuid() throws Exception {
        UserEntity userA = createUser("supabase-subject-a-" + UUID.randomUUID());
        UserEntity userB = createUser("supabase-subject-b-" + UUID.randomUUID());

        mockMvc.perform(get("/api/v1/test/current-user/{providedUserId}", userB.getId())
                        .with(jwt().jwt(token -> token.subject(userA.getAuthSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userA.getId().toString()))
                .andExpect(jsonPath("$.providedUserId").value(userB.getId().toString()));
    }

    @Test
    void currentUserResolverUsesConfiguredProviderAndSubject() {
        UserEntity user = createUser("resolver-subject-" + UUID.randomUUID());

        UserEntity resolved = userRepository.findByAuthProviderAndAuthSubject("supabase", user.getAuthSubject())
                .orElseThrow();

        assertThat(resolved.getId()).isEqualTo(user.getId());
    }

    private UserEntity createUser(final String authSubject) {
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("%s@example.test".formatted(authSubject));
        user.setName("Security Test User");
        user.setPreferredLanguage(PreferredLanguage.ENGLISH);
        user.setAuthProvider("supabase");
        user.setAuthSubject(authSubject);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    @RestController
    @RequestMapping("/api/v1/test/current-user")
    static class CurrentUserTestController {

        private final CurrentUserService currentUserService;

        CurrentUserTestController(final CurrentUserService currentUserService) {
            this.currentUserService = currentUserService;
        }

        @GetMapping
        Map<String, String> currentUser() {
            return Map.of("userId", currentUserService.requireCurrentUserId().toString());
        }

        @GetMapping("/{providedUserId}")
        Map<String, String> currentUserWithProvidedTarget(@PathVariable final UUID providedUserId) {
            return Map.of(
                    "userId", currentUserService.requireCurrentUserId().toString(),
                    "providedUserId", providedUserId.toString());
        }
    }
}
