package com.peladinhas.backend.domains.pitches.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.peladinhas.backend.domains.owners.service.PitchOwnerCapabilityService;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchRepository;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import com.peladinhas.backend.shared.domain.ContextualPermissionDeniedException;
import com.peladinhas.backend.shared.domain.DomainException;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PitchService {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final Clock clock;
    private final PitchOwnerCapabilityService pitchOwnerCapabilityService;
    private final PitchRepository pitchRepository;
    private final UserRepository userRepository;

    public PitchService(
            final Clock clock,
            final PitchOwnerCapabilityService pitchOwnerCapabilityService,
            final PitchRepository pitchRepository,
            final UserRepository userRepository) {
        this.clock = clock;
        this.pitchOwnerCapabilityService = pitchOwnerCapabilityService;
        this.pitchRepository = pitchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PitchEntity createPitch(final CreatePitchCommand command) {
        UserEntity owner = requireUser(command.ownerUserId());
        pitchOwnerCapabilityService.requirePitchOwnerCapability(owner.getId());
        validatePitchFields(command.latitude(), command.longitude(), command.timezone());
        OffsetDateTime now = OffsetDateTime.now(clock);

        PitchEntity pitch = new PitchEntity();
        pitch.setId(UUID.randomUUID());
        pitch.setOwnerUser(owner);
        pitch.setName(command.name());
        pitch.setDescription(command.description());
        pitch.setAddress(command.address());
        pitch.setLatitude(command.latitude());
        pitch.setLongitude(command.longitude());
        pitch.setTimezone(command.timezone());
        pitch.setBasePrice(command.basePrice());
        pitch.setCurrency(command.currency());
        pitch.setActive(command.active());
        pitch.setCreatedAt(now);
        pitch.setUpdatedAt(now);
        return pitchRepository.save(pitch);
    }

    @Transactional
    public PitchEntity updatePitch(
            final UUID pitchId,
            final UUID actingUserId,
            final UpdatePitchCommand command) {
        PitchEntity pitch = requireOwnedPitch(pitchId, actingUserId);
        validatePitchFields(command.latitude(), command.longitude(), command.timezone());
        pitch.setName(command.name());
        pitch.setDescription(command.description());
        pitch.setAddress(command.address());
        pitch.setLatitude(command.latitude());
        pitch.setLongitude(command.longitude());
        pitch.setTimezone(command.timezone());
        pitch.setBasePrice(command.basePrice());
        pitch.setCurrency(command.currency());
        pitch.setActive(command.active());
        pitch.setUpdatedAt(OffsetDateTime.now(clock));
        return pitchRepository.save(pitch);
    }

    @Transactional(readOnly = true)
    public List<PitchEntity> findPitchesOwnedBy(final UUID ownerUserId) {
        pitchOwnerCapabilityService.requirePitchOwnerCapability(ownerUserId);
        return pitchRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
    }

    @Transactional(readOnly = true)
    public PitchEntity requirePitch(final UUID pitchId) {
        return pitchRepository.findWithOwnerUserById(pitchId)
                .orElseThrow(() -> new ResourceNotFoundException("Pitch was not found."));
    }

    @Transactional(readOnly = true)
    public PitchEntity requireOwnedPitch(final UUID pitchId, final UUID actingUserId) {
        PitchEntity pitch = requirePitch(pitchId);
        requirePitchOwner(pitch, actingUserId);
        return pitch;
    }

    public void requirePitchOwner(final PitchEntity pitch, final UUID actingUserId) {
        if (!pitch.getOwnerUser().getId().equals(actingUserId)) {
            throw new ContextualPermissionDeniedException("User is not the pitch owner.");
        }
    }

    private UserEntity requireUser(final UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }

    private void validatePitchFields(
            final BigDecimal latitude,
            final BigDecimal longitude,
            final String timezone) {
        if ((latitude == null) != (longitude == null)) {
            throw new DomainException("Latitude and longitude must be provided together.");
        }
        if (latitude != null && (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0)) {
            throw new DomainException("Pitch latitude must be between -90 and 90.");
        }
        if (longitude != null && (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0)) {
            throw new DomainException("Pitch longitude must be between -180 and 180.");
        }
        try {
            ZoneId.of(timezone);
        } catch (RuntimeException exception) {
            throw new DomainException("Pitch timezone is not valid.");
        }
    }
}
