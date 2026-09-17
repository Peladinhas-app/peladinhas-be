package com.peladinhas.backend.domains.groups.persistence;

import com.peladinhas.backend.shared.persistence.AbstractDatabaseEnumConverter;
import com.peladinhas.backend.shared.persistence.DatabaseEnum;
import jakarta.persistence.Converter;

public enum GroupMemberRole implements DatabaseEnum {
    MEMBER("member"),
    ADMIN("admin");

    private final String value;

    GroupMemberRole(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Converter
    public static class ConverterImpl extends AbstractDatabaseEnumConverter<GroupMemberRole> {
        public ConverterImpl() {
            super(GroupMemberRole.class);
        }
    }
}
