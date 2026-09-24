package com.peladinhas.backend.domains.funding.service;

import java.math.BigDecimal;
import java.util.UUID;

public record FundingSummary(
        UUID matchId,
        UUID bookingId,
        BigDecimal targetAmount,
        BigDecimal settledCoveredAmount,
        BigDecimal outstandingAmount,
        boolean fullyFunded) {
}
