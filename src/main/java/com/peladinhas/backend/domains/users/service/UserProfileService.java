package com.peladinhas.backend.domains.users.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.auth.AuthenticatedUserPrincipal;
import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.owners.service.OwnerInvitationRequiredException;
import com.peladinhas.backend.domains.owners.service.PitchOwnerCapabilityService;
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
    private final PitchOwnerCapabilityService pitchOwnerCapabilityService;
    private final UserRepository userRepository;

    public UserProfileService(
            final Clock clock,
            final CurrentUserService currentUserService,
            final PitchOwnerCapabilityService pitchOwnerCapabilityService,
            final UserRepository userRepository) {
        this.clock = clock;
        this.currentUserService = currentUserService;
        this.pitchOwnerCapabilityService = pitchOwnerCapabilityService;
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
            UserEntity savedUser = userRepository.saveAndFlush(user);
            if (command.accountType() == RequestedAccountType.PITCH_OWNER) {
                pitchOwnerCapabilityService.activateOwnerCapability(savedUser, command.ownerInvitationCode());
            }
            return savedUser;
        } catch (DataIntegrityViolationException exception) {
            throw new UserProfileConflictException("A Peladinhas profile already exists for this authenticated user.");
        }
    }

    @Transactional(readOnly = true)
    public UserEntity currentProfile() {
        return currentUserService.requireCurrentUser();
    }

    @Transactional(readOnly = true)
    public boolean hasPitchOwnerCapability(final UUID userId) {
        return pitchOwnerCapabilityService.hasPitchOwnerCapability(userId);
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
        if (command.accountType() == RequestedAccountType.PITCH_OWNER
                && (command.ownerInvitationCode() == null || command.ownerInvitationCode().isBlank())) {
            throw new OwnerInvitationRequiredException();
        }
        if (command.accountType() == RequestedAccountType.PLAYER
                && command.ownerInvitationCode() != null && !command.ownerInvitationCode().isBlank()) {
            throw new DomainException("Owner invitation code is only accepted for pitch owner onboarding.");
        }
    }
}
