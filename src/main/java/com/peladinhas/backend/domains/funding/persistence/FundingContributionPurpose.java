package com.peladinhas.backend.domains.funding.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum FundingContributionPurpose implements DatabaseEnum {
    PARTICIPANT_SHARE("participant_share"),
    ORGANIZER_ADVANCE("organizer_advance"),
    REPLACEMENT_PAYMENT("replacement_payment"),
    REVERSAL_REFUND_ALLOCATION("reversal_refund_allocation");

    private final String value;

    FundingContributionPurpose(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public boolean coversBookingCost() {
        return this == PARTICIPANT_SHARE
                || this == ORGANIZER_ADVANCE
                || this == REPLACEMENT_PAYMENT;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<FundingContributionPurpose> {
        public ConverterImpl() {
            super(FundingContributionPurpose.class);
        }
    }
}
