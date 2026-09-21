package com.peladinhas.backend.domains.pitches.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchBlockRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PitchAvailabilityService {

    private final BookingRepository bookingRepository;
    private final PitchBlockRepository pitchBlockRepository;
    private final PitchScheduleRepository pitchScheduleRepository;
    private final PitchService pitchService;

    public PitchAvailabilityService(
            final BookingRepository bookingRepository,
            final PitchBlockRepository pitchBlockRepository,
            final PitchScheduleRepository pitchScheduleRepository,
            final PitchService pitchService) {
        this.bookingRepository = bookingRepository;
        this.pitchBlockRepository = pitchBlockRepository;
        this.pitchScheduleRepository = pitchScheduleRepository;
        this.pitchService = pitchService;
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(
            final UUID pitchId,
            final OffsetDateTime startsAt,
            final OffsetDateTime endsAt) {
        validateInterval(startsAt, endsAt);
        PitchEntity pitch = pitchService.requirePitch(pitchId);
        return Boolean.TRUE.equals(pitch.getActive())
                && isInsideRecurringSchedule(pitch, startsAt, endsAt)
                && !pitchBlockRepository.existsOverlappingBlock(pitchId, startsAt, endsAt)
                && !bookingRepository.existsOverlappingConfirmedBooking(pitchId, startsAt, endsAt);
    }

    @Transactional(readOnly = true)
    public void requireAvailable(
            final UUID pitchId,
            final OffsetDateTime startsAt,
            final OffsetDateTime endsAt) {
        if (!isAvailable(pitchId, startsAt, endsAt)) {
            throw new PitchNotAvailableException("Pitch is not available for the requested interval.");
        }
    }

    private boolean isInsideRecurringSchedule(
            final PitchEntity pitch,
            final OffsetDateTime startsAt,
            final OffsetDateTime endsAt) {
        ZoneId pitchZone = ZoneId.of(pitch.getTimezone());
        ZonedDateTime localStart = startsAt.atZoneSameInstant(pitchZone);
        ZonedDateTime localEnd = endsAt.atZoneSameInstant(pitchZone);
        LocalDate startDate = localStart.toLocalDate();
        LocalDate endDate = localEnd.toLocalDate();
        if (!startDate.equals(endDate)) {
            return false;
        }

        short dayOfWeek = (short) localStart.getDayOfWeek().getValue();
        LocalTime requestedStart = localStart.toLocalTime();
        LocalTime requestedEnd = localEnd.toLocalTime();
        return pitchScheduleRepository.findByPitch_IdAndDayOfWeek(pitch.getId(), dayOfWeek).stream()
                .anyMatch(schedule -> contains(schedule, requestedStart, requestedEnd));
    }

    private boolean contains(
            final PitchScheduleEntity schedule,
            final LocalTime requestedStart,
            final LocalTime requestedEnd) {
        return !requestedStart.isBefore(schedule.getStartsAt())
                && !requestedEnd.isAfter(schedule.getEndsAt());
    }

    private void validateInterval(final OffsetDateTime startsAt, final OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new InvalidPitchIntervalException("Pitch availability interval is not valid.");
        }
    }
}
