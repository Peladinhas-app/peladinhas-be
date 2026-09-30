package com.peladinhas.backend.domains.users.web;

import java.util.UUID;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.web.ApiEnumParser;

public record UserProfileResponse(
        UUID id,
        String email,
        String name,
        String preferredLanguage,
        UserCapabilitiesResponse capabilities) {

    public static UserProfileResponse from(final UserEntity user, final boolean pitchOwner) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                ApiEnumParser.value(user.getPreferredLanguage()),
                new UserCapabilitiesResponse(true, pitchOwner));
    }
}
