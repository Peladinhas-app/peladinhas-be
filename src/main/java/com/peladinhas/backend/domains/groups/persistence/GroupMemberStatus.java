package com.peladinhas.backend.domains.groups.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum GroupMemberStatus implements DatabaseEnum {
    ACTIVE("active"),
    LEFT("left"),
    REMOVED("removed");

    private final String value;

    GroupMemberStatus(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<GroupMemberStatus> {
        public ConverterImpl() {
            super(GroupMemberStatus.class);
        }
    }
}
