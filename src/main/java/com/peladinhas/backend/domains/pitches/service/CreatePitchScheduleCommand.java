package com.peladinhas.backend.domains.pitches.service;

import java.time.LocalTime;
import java.util.UUID;

public record CreatePitchScheduleCommand(
        UUID pitchId,
        UUID actingUserId,
        short dayOfWeek,
        LocalTime startsAt,
        LocalTime endsAt) {
}
