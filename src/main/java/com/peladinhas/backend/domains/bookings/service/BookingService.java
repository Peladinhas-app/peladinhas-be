package com.peladinhas.backend.domains.bookings.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRejectionRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingStatus;
import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchRepository;
import com.peladinhas.backend.domains.pitches.persistence.PitchEntity;
import com.peladinhas.backend.domains.pitches.service.InvalidPitchIntervalException;
import com.peladinhas.backend.domains.pitches.service.PitchAvailabilityService;
import com.peladinhas.backend.domains.pitches.service.PitchService;
import com.peladinhas.backend.shared.domain.DomainException;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRejectionRepository bookingRejectionRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final MatchRepository matchRepository;
    private final PitchAvailabilityService pitchAvailabilityService;
    private final PitchService pitchService;

    public BookingService(
            final BookingRejectionRepository bookingRejectionRepository,
            final BookingRepository bookingRepository,
            final Clock clock,
            final MatchRepository matchRepository,
            final PitchAvailabilityService pitchAvailabilityService,
            final PitchService pitchService) {
        this.bookingRejectionRepository = bookingRejectionRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.matchRepository = matchRepository;
        this.pitchAvailabilityService = pitchAvailabilityService;
        this.pitchService = pitchService;
    }

    @Transactional
    public BookingEntity createProvisionalBooking(final CreateProvisionalBookingCommand command) {
        validateBookingCommand(command);
        MatchEntity match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match was not found."));
        PitchEntity pitch = pitchService.requirePitch(command.pitchId());
        pitchAvailabilityService.requireAvailable(command.pitchId(), command.startsAt(), command.endsAt());
        OffsetDateTime now = OffsetDateTime.now(clock);

        BookingEntity booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setMatch(match);
        booking.setPitch(pitch);
        booking.setStartsAt(command.startsAt());
        booking.setEndsAt(command.endsAt());
        booking.setTotalPrice(command.totalPrice());
        booking.setCurrency(command.currency());
        booking.setStatus(BookingStatus.PROVISIONAL);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        return bookingRepository.save(booking);
    }

    @Transactional
    public BookingEntity rejectBooking(final RejectBookingCommand command) {
        validateRejectBookingCommand(command);
        BookingEntity booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking was not found."));
        pitchService.requirePitchOwner(booking.getPitch(), command.actingOwnerUserId());
        BookingStatusTransitionPolicy.requireValid(booking.getStatus(), BookingStatus.REJECTED);
        OffsetDateTime now = OffsetDateTime.now(clock);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectedAt(now);
        booking.setUpdatedAt(now);
        BookingEntity savedBooking = bookingRepository.save(booking);

        BookingRejectionEntity rejection = new BookingRejectionEntity();
        rejection.setId(UUID.randomUUID());
        rejection.setBooking(savedBooking);
        rejection.setReasonCode(command.reasonCode());
        rejection.setExplanation(command.explanation());
        rejection.setCreatedAt(now);
        bookingRejectionRepository.save(rejection);

        return savedBooking;
    }

    private void validateBookingCommand(final CreateProvisionalBookingCommand command) {
        if (command.startsAt() == null || command.endsAt() == null || !command.startsAt().isBefore(command.endsAt())) {
            throw new InvalidPitchIntervalException("Booking time range is not valid.");
        }
        if (command.totalPrice() == null || command.totalPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Booking total price must be zero or greater.");
        }
    }

    private void validateRejectBookingCommand(final RejectBookingCommand command) {
        if (command.reasonCode() == null) {
            throw new DomainException("Booking rejection reason is required.");
        }
    }
}
