package com.peladinhas.backend.domains.users.web;

import java.util.Locale;

import com.peladinhas.backend.auth.AuthenticatedUserPrincipal;
import com.peladinhas.backend.auth.CurrentUserService;
import com.peladinhas.backend.domains.owners.service.PitchOwnerCapabilityService;
import com.peladinhas.backend.domains.users.persistence.PreferredLanguage;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.service.CreateUserProfileCommand;
import com.peladinhas.backend.domains.users.service.RequestedAccountType;
import com.peladinhas.backend.domains.users.service.UserProfileService;
import com.peladinhas.backend.shared.web.ApiEnumParser;
import com.peladinhas.backend.shared.web.InvalidApiRequestException;
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
    private final PitchOwnerCapabilityService pitchOwnerCapabilityService;
    private final UserProfileService userProfileService;

    public UserProfileController(
            final CurrentUserService currentUserService,
            final PitchOwnerCapabilityService pitchOwnerCapabilityService,
            final UserProfileService userProfileService) {
        this.currentUserService = currentUserService;
        this.pitchOwnerCapabilityService = pitchOwnerCapabilityService;
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
        RequestedAccountType accountType = parseAccountType(request.accountType());
        UserEntity user = userProfileService.createProfile(new CreateUserProfileCommand(
                principal,
                request.name(),
                preferredLanguage,
                accountType,
                request.ownerInvitationCode()));
        return response(user);
    }

    @GetMapping
    public UserProfileResponse currentProfile() {
        return response(userProfileService.currentProfile());
    }

    @PostMapping("/pitch-owner")
    public UserProfileResponse activatePitchOwner(@Valid @RequestBody final ActivatePitchOwnerRequest request) {
        UserEntity user = userProfileService.currentProfile();
        pitchOwnerCapabilityService.activateOwnerCapability(user, request.invitationCode());
        return response(user);
    }

    private UserProfileResponse response(final UserEntity user) {
        return UserProfileResponse.from(user, userProfileService.hasPitchOwnerCapability(user.getId()));
    }

    private RequestedAccountType parseAccountType(final String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return RequestedAccountType.PLAYER;
        }
        try {
            return RequestedAccountType.valueOf(accountType.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidApiRequestException("Invalid value for accountType.");
        }
    }
}
