package com.peladinhas.backend.domains.users.web;

import com.peladinhas.backend.auth.AuthenticatedUserPrincipal;
import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.service.CreateUserProfileCommand;
import com.peladinhas.backend.domains.users.service.UserProfileService;
import com.peladinhas.backend.shared.web.ApiEnumParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    public UserProfileController(
            final CurrentUserService currentUserService,
            final UserProfileService userProfileService) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse createProfile(@Valid @RequestBody final CreateProfileRequest request) {
        AuthenticatedUserPrincipal principal = currentUserService.principalFromSecurityContext();
        PreferredLanguage preferredLanguage = ApiEnumParser.parse(
                PreferredLanguage.class,
                request.preferredLanguage(),
                "preferredLanguage");
        UserEntity user = userProfileService.createProfile(new CreateUserProfileCommand(
                principal,
                request.name(),
                preferredLanguage));
        return UserProfileResponse.from(user);
    }

    @GetMapping
    public UserProfileResponse currentProfile() {
        return UserProfileResponse.from(userProfileService.currentProfile());
    }
}
