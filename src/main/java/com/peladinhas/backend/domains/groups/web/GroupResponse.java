package com.peladinhas.backend.domains.groups.web;

import java.util.UUID;

import com.peladinhas.backend.domains.groups.persistence.GroupEntity;
import com.peladinhas.backend.shared.web.ApiEnumParser;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        String visibility,
        UUID createdByUserId) {

    public static GroupResponse from(final GroupEntity group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                ApiEnumParser.value(group.getVisibility()),
                group.getCreatedByUser().getId());
    }
}