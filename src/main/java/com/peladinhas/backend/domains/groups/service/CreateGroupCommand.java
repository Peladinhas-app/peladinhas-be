package com.peladinhas.backend.domains.groups.service;

import java.util.UUID;

import com.peladinhas.backend.domains.groups.persistence.GroupVisibility;

public record CreateGroupCommand(
        String name,
        String description,
        GroupVisibility visibility,
        UUID creatorUserId) {
}
