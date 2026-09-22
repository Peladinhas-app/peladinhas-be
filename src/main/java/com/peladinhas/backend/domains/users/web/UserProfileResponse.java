package com.peladinhas.backend.domains.users.web;

import java.util.UUID;

import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.shared.web.ApiEnumParser;

public record UserProfileResponse(
        UUID id,
        String email,
        String name,
        String preferredLanguage) {

    public static UserProfileResponse from(final UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                ApiEnumParser.value(user.getPreferredLanguage()));
    }
}
