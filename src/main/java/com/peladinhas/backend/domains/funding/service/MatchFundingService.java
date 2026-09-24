package com.peladinhas.backend.domains.funding.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.peladinhas.backend.domains.bookings.persistence.BookingEntity;
import com.peladinhas.backend.domains.bookings.persistence.BookingRepository;
import com.peladinhas.backend.domains.funding.persistence.FundingContributionPurpose;
import com.peladinhas.backend.domains.funding.persistence.FundingContributionRepository;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchFundingService {

    private static final Set<FundingContributionPurpose> COVERING_PURPOSES = EnumSet.of(
            FundingContributionPurpose.PARTICIPANT_SHARE,
            FundingContributionPurpose.ORGANIZER_ADVANCE,
            FundingContributionPurpose.REPLACEMENT_PAYMENT);

    private final BookingRepository bookingRepository;
    private final FundingContributionRepository contributionRepository;

    public MatchFundingService(
            final BookingRepository bookingRepository,
            final FundingContributionRepository contributionRepository) {
        this.bookingRepository = bookingRepository;
        this.contributionRepository = contributionRepository;
    }

    @Transactional(readOnly = true)
    public FundingSummary summarizeBookingFunding(final UUID bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking was not found."));
        return summarize(booking);
    }

    public FundingSummary summarize(final BookingEntity booking) {
        BigDecimal covered = contributionRepository.sumSettledCoverageForMatch(
                booking.getMatch().getId(),
                COVERING_PURPOSES,
                booking.getCurrency());
        BigDecimal target = booking.getTotalPrice();
        BigDecimal outstanding = target.subtract(covered).max(BigDecimal.ZERO);
        return new FundingSummary(
                booking.getMatch().getId(),
                booking.getId(),
                target,
                covered,
                outstanding,
                covered.compareTo(target) >= 0);
    }
}
