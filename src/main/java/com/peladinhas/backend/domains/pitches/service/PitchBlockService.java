package com.peladinhas.backend.domains.pitches.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.pitches.persistence.PitchBlockEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchBlockRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PitchBlockService {

    private final Clock clock;
    private final PitchBlockRepository pitchBlockRepository;
    private final PitchService pitchService;

    public PitchBlockService(
            final Clock clock,
            final PitchBlockRepository pitchBlockRepository,
            final PitchService pitchService) {
        this.clock = clock;
        this.pitchBlockRepository = pitchBlockRepository;
        this.pitchService = pitchService;
    }

    @Transactional
    public PitchBlockEntity createBlock(final CreatePitchBlockCommand command) {
        validateInterval(command.startsAt(), command.endsAt());
        PitchEntity pitch = pitchService.requireOwnedPitch(command.pitchId(), command.actingUserId());

        PitchBlockEntity block = new PitchBlockEntity();
        block.setId(UUID.randomUUID());
        block.setPitch(pitch);
        block.setStartsAt(command.startsAt());
        block.setEndsAt(command.endsAt());
        block.setReasonCode(command.reasonCode());
        block.setNote(command.note());
        block.setCreatedAt(OffsetDateTime.now(clock));
        return pitchBlockRepository.save(block);
    }

    private void validateInterval(final OffsetDateTime startsAt, final OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new InvalidPitchIntervalException("Pitch block time range is not valid.");
        }
    }
}
