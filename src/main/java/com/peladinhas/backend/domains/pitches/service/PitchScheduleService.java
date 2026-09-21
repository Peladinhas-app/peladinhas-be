package com.peladinhas.backend.domains.pitches.service;

import java.util.UUID;

import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PitchScheduleService {

    private final PitchScheduleRepository pitchScheduleRepository;
    private final PitchService pitchService;

    public PitchScheduleService(
            final PitchScheduleRepository pitchScheduleRepository,
            final PitchService pitchService) {
        this.pitchScheduleRepository = pitchScheduleRepository;
        this.pitchService = pitchService;
    }

    @Transactional
    public PitchScheduleEntity createSchedule(final CreatePitchScheduleCommand command) {
        validateSchedule(command);
        PitchEntity pitch = pitchService.requireOwnedPitch(command.pitchId(), command.actingUserId());

        PitchScheduleEntity schedule = new PitchScheduleEntity();
        schedule.setId(UUID.randomUUID());
        schedule.setPitch(pitch);
        schedule.setDayOfWeek(command.dayOfWeek());
        schedule.setStartsAt(command.startsAt());
        schedule.setEndsAt(command.endsAt());

        try {
            return pitchScheduleRepository.saveAndFlush(schedule);
        } catch (DataIntegrityViolationException exception) {
            throw new PitchScheduleConflictException("Pitch schedule overlaps an existing schedule.");
        }
    }

    private void validateSchedule(final CreatePitchScheduleCommand command) {
        if (command.dayOfWeek() < 1 || command.dayOfWeek() > 7) {
            throw new InvalidPitchIntervalException("Pitch schedule day of week must be between 1 and 7.");
        }
        if (command.startsAt() == null || command.endsAt() == null || !command.startsAt().isBefore(command.endsAt())) {
            throw new InvalidPitchIntervalException("Pitch schedule time range is not valid.");
        }
    }
}
