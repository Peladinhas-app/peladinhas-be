package com.peladinhas.backend.domains.pitches.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.pitches.persistence.PitchBlockEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.persistence.PitchScheduleEntity;
import com.peladinhas.backend.domains.pitches.service.CreatePitchBlockCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchCommand;
import com.peladinhas.backend.domains.pitches.service.CreatePitchScheduleCommand;
import com.peladinhas.backend.domains.pitches.service.PitchAvailabilityService;
import com.peladinhas.backend.domains.pitches.service.PitchBlockService;
import com.peladinhas.backend.domains.pitches.service.PitchScheduleService;
import com.peladinhas.backend.domains.pitches.service.PitchService;
import com.peladinhas.backend.domains.pitches.service.UpdatePitchCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/pitches")
public class PitchController {

    private final CurrentUserService currentUserService;
    private final PitchAvailabilityService pitchAvailabilityService;
    private final PitchBlockService pitchBlockService;
    private final PitchScheduleService pitchScheduleService;
    private final PitchService pitchService;

    public PitchController(
            final CurrentUserService currentUserService,
            final PitchAvailabilityService pitchAvailabilityService,
            final PitchBlockService pitchBlockService,
            final PitchScheduleService pitchScheduleService,
            final PitchService pitchService) {
        this.currentUserService = currentUserService;
        this.pitchAvailabilityService = pitchAvailabilityService;
        this.pitchBlockService = pitchBlockService;
        this.pitchScheduleService = pitchScheduleService;
        this.pitchService = pitchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PitchResponse createPitch(@Valid @RequestBody final CreatePitchRequest request) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        PitchEntity pitch = pitchService.createPitch(new CreatePitchCommand(
                currentUserId,
                request.name(),
                request.description(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.timezone(),
                request.basePrice(),
                request.currency(),
                request.active()));
        return PitchResponse.from(pitch);
    }

    @PutMapping("/{pitchId}")
    public PitchResponse updatePitch(
            @PathVariable final UUID pitchId,
            @Valid @RequestBody final UpdatePitchRequest request) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        PitchEntity pitch = pitchService.updatePitch(pitchId, currentUserId, new UpdatePitchCommand(
                request.name(),
                request.description(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.timezone(),
                request.basePrice(),
                request.currency(),
                request.active()));
        return PitchResponse.from(pitch);
    }

    @GetMapping("/{pitchId}")
    public PitchResponse getPitch(@PathVariable final UUID pitchId) {
        return PitchResponse.from(pitchService.requirePitch(pitchId));
    }

    @PostMapping("/{pitchId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public PitchScheduleResponse createSchedule(
            @PathVariable final UUID pitchId,
            @Valid @RequestBody final CreatePitchScheduleRequest request) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        PitchScheduleEntity schedule = pitchScheduleService.createSchedule(new CreatePitchScheduleCommand(
                pitchId,
                currentUserId,
                request.dayOfWeek(),
                request.startsAt(),
                request.endsAt()));
        return PitchScheduleResponse.from(schedule);
    }

    @PostMapping("/{pitchId}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public PitchBlockResponse createBlock(
            @PathVariable final UUID pitchId,
            @Valid @RequestBody final CreatePitchBlockRequest request) {
        UUID currentUserId = currentUserService.requireCurrentUserId();
        PitchBlockEntity block = pitchBlockService.createBlock(new CreatePitchBlockCommand(
                pitchId,
                currentUserId,
                request.startsAt(),
                request.endsAt(),
                request.reasonCode(),
                request.note()));
        return PitchBlockResponse.from(block);
    }

    @GetMapping("/{pitchId}/availability")
    public PitchAvailabilityResponse checkAvailability(
            @PathVariable final UUID pitchId,
            @RequestParam @NotNull final OffsetDateTime startsAt,
            @RequestParam @NotNull final OffsetDateTime endsAt) {
        boolean available = pitchAvailabilityService.isAvailable(pitchId, startsAt, endsAt);
        return new PitchAvailabilityResponse(pitchId, startsAt, endsAt, available);
    }
}
