package com.peladinhas.backend.domains.matches.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum MatchParticipantStatus implements DatabaseEnum {
    REQUESTED("requested"),
    APPROVED("approved"),
    REJECTED("rejected"),
    AWAITING_PAYMENT("awaiting_payment"),
    CONFIRMED("confirmed"),
    CANCELLED("cancelled");

    private final String value;

    MatchParticipantStatus(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<MatchParticipantStatus> {
        public ConverterImpl() {
            super(MatchParticipantStatus.class);
        }
    }
}
