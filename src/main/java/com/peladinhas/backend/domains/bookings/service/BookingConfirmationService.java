package com.peladinhas.backend.domains.bookings.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.bookings.persistence.BookingStatus;
import com.peladinhas.backend.domains.funding.service.BookingFundingIncompleteException;
import com.peladinhas.backend.domains.funding.service.FundingSummary;
import com.peladinhas.backend.domains.funding.service.MatchFundingService;
import com.peladinhas.backend.domains.matches.persistence.MatchEntity;
import com.peladinhas.backend.domains.matches.persistence.MatchFundingState;
import com.peladinhas.backend.domains.matches.persistence.MatchRepository;
import com.peladinhas.backend.domains.matches.service.MatchService;
import com.peladinhas.backend.domains.pitches.persistence.PitchRepository;
import com.peladinhas.backend.domains.pitches.service.PitchAvailabilityService;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingConfirmationService {

    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final MatchFundingService fundingService;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final PitchAvailabilityService pitchAvailabilityService;
    private final PitchRepository pitchRepository;

    public BookingConfirmationService(
            final BookingRepository bookingRepository,
            final Clock clock,
            final MatchFundingService fundingService,
            final MatchRepository matchRepository,
            final MatchService matchService,
            final PitchAvailabilityService pitchAvailabilityService,
            final PitchRepository pitchRepository) {
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.fundingService = fundingService;
        this.matchRepository = matchRepository;
        this.matchService = matchService;
        this.pitchAvailabilityService = pitchAvailabilityService;
        this.pitchRepository = pitchRepository;
    }

    @Transactional
    public BookingEntity confirmWhenFunded(final UUID bookingId, final UUID actingUserId) {
        BookingEntity bookingSnapshot = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking was not found."));
        matchService.requireMatchAdmin(bookingSnapshot.getMatch().getId(), actingUserId);
        pitchRepository.findByIdForUpdate(bookingSnapshot.getPitch().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pitch was not found."));

        BookingEntity booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking was not found."));
        MatchEntity match = booking.getMatch();

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }
        BookingStatusTransitionPolicy.requireValid(booking.getStatus(), BookingStatus.CONFIRMED);

        FundingSummary summary = fundingService.summarize(booking);
        if (!summary.fullyFunded()) {
            throw new BookingFundingIncompleteException("Booking funding is not complete.");
        }

        match.setFundingState(MatchFundingState.FULLY_FUNDED);
        matchRepository.save(match);

        if (!pitchAvailabilityService.isAvailable(booking.getPitch().getId(), booking.getStartsAt(), booking.getEndsAt())) {
            match.setFundingState(MatchFundingState.BOOKING_FAILED);
            matchRepository.save(match);
            return booking;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now);
        booking.setUpdatedAt(now);
        BookingEntity confirmed = bookingRepository.saveAndFlush(booking);

        List<BookingEntity> competitors = bookingRepository.findOverlappingProvisionalCompetitorsForUpdate(
                booking.getPitch().getId(),
                booking.getId(),
                booking.getStartsAt(),
                booking.getEndsAt());
        competitors.forEach(competitor -> {
            competitor.setStatus(BookingStatus.LOST);
            competitor.setUpdatedAt(now);
        });
        bookingRepository.saveAll(competitors);

        match.setFundingState(MatchFundingState.BOOKING_CONFIRMED);
        matchRepository.save(match);
        return confirmed;
    }
}

