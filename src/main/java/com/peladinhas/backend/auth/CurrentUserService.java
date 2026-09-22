package com.peladinhas.backend.auth;

import java.util.UUID;

import com.peladinhas.backend.config.PeladinhasAuthProperties;
import com.peladinhas.backend.domains.users.persistence.UserEntity;
import com.peladinhas.backend.domains.users.persistence.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final PeladinhasAuthProperties authProperties;
    private final UserRepository userRepository;

    public CurrentUserService(
            final PeladinhasAuthProperties authProperties,
            final UserRepository userRepository) {
        this.authProperties = authProperties;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserEntity requireCurrentUser() {
        return requireUser(identityFromSecurityContext());
    }

    public AuthenticatedUserPrincipal principalFromSecurityContext() {
        Jwt jwt = jwtFromSecurityContext();
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated JWT subject is required.");
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new InvalidAuthenticatedEmailException("Authenticated email claim is required.");
        }
        return new AuthenticatedUserPrincipal(authProperties.providerOrDefault(), subject, email);
    }

    @Transactional(readOnly = true)
    public UUID requireCurrentUserId() {
        return requireCurrentUser().getId();
    }

    @Transactional(readOnly = true)
    public UserEntity requireUser(final AuthenticatedUserIdentity identity) {
        return userRepository.findByAuthProviderAndAuthSubject(identity.provider(), identity.subject())
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(
                        "Authenticated user has not completed a Peladinhas profile."));
    }

    public AuthenticatedUserIdentity identityFromSecurityContext() {
        Jwt jwt = jwtFromSecurityContext();
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated JWT subject is required.");
        }
        return new AuthenticatedUserIdentity(authProperties.providerOrDefault(), subject);
    }

    private Jwt jwtFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated JWT principal is required.");
        }
        return jwtAuthentication.getToken();
    }
}
