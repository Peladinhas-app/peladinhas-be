package com.peladinhas.backend.domains.users.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.auth.AuthenticatedUserPrincipal;
import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final Clock clock;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public UserProfileService(
            final Clock clock,
            final CurrentUserService currentUserService,
            final UserRepository userRepository) {
        this.clock = clock;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity createProfile(final CreateUserProfileCommand command) {
        validate(command);
        AuthenticatedUserPrincipal principal = command.principal();
        if (userRepository.existsByAuthProviderAndAuthSubject(principal.provider(), principal.subject())
                || userRepository.existsByEmail(principal.email())) {
            throw new UserProfileConflictException("A Peladinhas profile already exists for this authenticated user.");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(principal.email());
        user.setName(command.name().trim());
        user.setPreferredLanguage(command.preferredLanguage());
        user.setAuthProvider(principal.provider());
        user.setAuthSubject(principal.subject());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new UserProfileConflictException("A Peladinhas profile already exists for this authenticated user.");
        }
    }

    @Transactional(readOnly = true)
    public UserEntity currentProfile() {
        return currentUserService.requireCurrentUser();
    }

    private void validate(final CreateUserProfileCommand command) {
        if (command.principal() == null) {
            throw new DomainException("Authenticated identity is required.");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new DomainException("Profile name is required.");
        }
        if (command.preferredLanguage() == null) {
            throw new DomainException("Preferred language is required.");
        }
    }
}
