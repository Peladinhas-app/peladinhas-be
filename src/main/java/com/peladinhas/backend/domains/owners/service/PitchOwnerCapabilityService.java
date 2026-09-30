package com.peladinhas.backend.domains.owners.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.owners.persistence.PitchOwnerInvitationCodeEntity;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerInvitationCodeRepository;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerProfileEntity;
import com.peladinhas.backend.domains.owners.persistence.PitchOwnerProfileRepository;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PitchOwnerCapabilityService {

    private final Clock clock;
    private final OwnerInvitationCodeHasher codeHasher;
    private final PitchOwnerInvitationCodeRepository invitationCodeRepository;
    private final PitchOwnerProfileRepository ownerProfileRepository;
    private final UserRepository userRepository;

    public PitchOwnerCapabilityService(
            final Clock clock,
            final OwnerInvitationCodeHasher codeHasher,
            final PitchOwnerInvitationCodeRepository invitationCodeRepository,
            final PitchOwnerProfileRepository ownerProfileRepository,
            final UserRepository userRepository) {
        this.clock = clock;
        this.codeHasher = codeHasher;
        this.invitationCodeRepository = invitationCodeRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.userRepository = userRepository;
    }

    public boolean hasPitchOwnerCapability(final UUID userId) {
        return ownerProfileRepository.existsById(userId);
    }

    public void requirePitchOwnerCapability(final UUID userId) {
        if (!hasPitchOwnerCapability(userId)) {
            throw new PitchOwnerCapabilityRequiredException();
        }
    }

    @Transactional
    public PitchOwnerProfileEntity activateOwnerCapability(final UUID userId, final String invitationCode) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
        return activateOwnerCapability(user, invitationCode);
    }

    @Transactional
    public PitchOwnerProfileEntity activateOwnerCapability(final UserEntity user, final String invitationCode) {
        if (hasPitchOwnerCapability(user.getId())) {
            return ownerProfileRepository.findById(user.getId()).orElseThrow();
        }
        PitchOwnerInvitationCodeEntity invitation = lockedInvitation(invitationCode);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (invitation.isUsed()) {
            throw new UsedOwnerInvitationCodeException();
        }
        if (invitation.isExpired(now)) {
            throw new ExpiredOwnerInvitationCodeException();
        }

        PitchOwnerProfileEntity profile = new PitchOwnerProfileEntity();
        profile.setUser(user);
        profile.setActivatedAt(now);
        profile.setCreatedAt(now);
        PitchOwnerProfileEntity savedProfile = ownerProfileRepository.save(profile);

        invitation.setUsedAt(now);
        invitation.setUsedByUser(user);
        invitationCodeRepository.save(invitation);
        return savedProfile;
    }

    private PitchOwnerInvitationCodeEntity lockedInvitation(final String invitationCode) {
        String codeHash = codeHasher.hash(invitationCode);
        return invitationCodeRepository.findByCodeHashForUpdate(codeHash)
                .orElseThrow(InvalidOwnerInvitationCodeException::new);
    }
}
