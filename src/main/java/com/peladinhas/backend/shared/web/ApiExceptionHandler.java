package com.peladinhas.backend.shared.web;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import com.peladinhas.backend.auth.AuthenticatedUserNotFoundException;
import com.peladinhas.backend.domains.matches.service.ActiveUpcomingMatchExistsException;
import com.peladinhas.backend.domains.matches.service.InvalidMatchTransitionException;
import com.peladinhas.backend.domains.matches.service.InvalidParticipantTransitionException;
import com.peladinhas.backend.domains.matches.service.MatchCapacityReachedException;
import com.peladinhas.backend.domains.matches.service.MatchNotAcceptingParticipantsException;
import com.peladinhas.backend.domains.matches.service.UnsupportedMatchDurationException;
import com.peladinhas.backend.shared.domain.ContextualPermissionDeniedException;
import com.peladinhas.backend.shared.domain.DomainException;
import com.peladinhas.backend.shared.domain.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final Clock clock;

    public ApiExceptionHandler(final Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(final ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "resource_not_found", exception.getMessage(), List.of());
    }

    @ExceptionHandler(ContextualPermissionDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleForbidden(final ContextualPermissionDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "permission_denied", exception.getMessage(), List.of());
    }

    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleAuthenticatedUserNotFound(
            final AuthenticatedUserNotFoundException exception) {
        return error(HttpStatus.FORBIDDEN, "authenticated_user_not_found", exception.getMessage(), List.of());
    }

    @ExceptionHandler({
            ActiveUpcomingMatchExistsException.class,
            MatchCapacityReachedException.class,
            InvalidMatchTransitionException.class,
            InvalidParticipantTransitionException.class,
            MatchNotAcceptingParticipantsException.class
    })
    ResponseEntity<ApiErrorResponse> handleConflict(final DomainException exception) {
        return error(HttpStatus.CONFLICT, "business_conflict", exception.getMessage(), List.of());
    }

    @ExceptionHandler({UnsupportedMatchDurationException.class, InvalidApiRequestException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(final DomainException exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request", exception.getMessage(), List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> handleMalformedRequest(final Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request", "Request body or path value is invalid.", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(final MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed.", fieldErrors);
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiErrorResponse> handleDomain(final DomainException exception) {
        return error(HttpStatus.BAD_REQUEST, "domain_error", exception.getMessage(), List.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            final HttpStatus status,
            final String code,
            final String message,
            final List<FieldErrorResponse> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                code,
                message,
                OffsetDateTime.now(clock),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private FieldErrorResponse fieldError(final FieldError fieldError) {
        return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
