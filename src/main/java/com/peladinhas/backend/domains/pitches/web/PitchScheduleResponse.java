package com.peladinhas.backend.domains.pitches.web;

import java.time.LocalTime;
import java.util.UUID;

import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleEntity;

public record PitchScheduleResponse(
        UUID id,
        UUID pitchId,
        Short dayOfWeek,
        LocalTime startsAt,
        LocalTime endsAt) {

    public static PitchScheduleResponse from(final PitchScheduleEntity schedule) {
        return new PitchScheduleResponse(
                schedule.getId(),
                schedule.getPitch().getId(),
                schedule.getDayOfWeek(),
                schedule.getStartsAt(),
                schedule.getEndsAt());
    }
}
