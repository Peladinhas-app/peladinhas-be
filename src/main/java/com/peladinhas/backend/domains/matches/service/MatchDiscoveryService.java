package com.peladinhas.backend.domains.matches.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.peladinhas.backend.domains.matches.persistence.MatchDiscoveryRepository;
import com.peladinhas.backend.domains.matches.persistence.MatchJoinMode;
import com.peladinhas.backend.domains.matches.web.MatchDiscoveryPageResponse;
import com.peladinhas.backend.domains.matches.web.MatchDiscoveryResponse;
import com.peladinhas.backend.shared.domain.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchDiscoveryService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;
    public static final String FALLBACK_TIME_ZONE_PROPERTY = "peladinhas.discovery.fallback-time-zone";
    public static final String DEFAULT_FALLBACK_TIME_ZONE = "Europe/Lisbon";

    private final Clock clock;
    private final String fallbackTimeZone;
    private final MatchDiscoveryRepository matchDiscoveryRepository;

    public MatchDiscoveryService(
            final Clock clock,
            final MatchDiscoveryRepository matchDiscoveryRepository,
            @Value("${peladinhas.discovery.fallback-time-zone:Europe/Lisbon}") final String fallbackTimeZone) {
        this.clock = clock;
        this.matchDiscoveryRepository = matchDiscoveryRepository;
        this.fallbackTimeZone = validatedFallbackTimeZone(fallbackTimeZone);
    }

    @Transactional(readOnly = true)
    public MatchDiscoveryPageResponse discoverMatches(
            final UUID userId,
            final String area,
            final OffsetDateTime startsFrom,
            final OffsetDateTime startsTo,
            final LocalTime timeFrom,
            final LocalTime timeTo,
            final MatchJoinMode joinMode,
            final Boolean availableOnly,
            final Integer page,
            final Integer size) {
        MatchDiscoveryCriteria criteria = criteria(
                userId,
                area,
                startsFrom,
                startsTo,
                timeFrom,
                timeTo,
                joinMode,
                availableOnly,
                page,
                size);
        long totalElements = matchDiscoveryRepository.countDiscoverableMatches(criteria);
        List<MatchDiscoveryResponse> matches = matchDiscoveryRepository.findDiscoverableMatches(criteria).stream()
                .map(this::response)
                .toList();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / criteria.size());
        return new MatchDiscoveryPageResponse(matches, criteria.page(), criteria.size(), totalElements, totalPages);
    }

    private MatchDiscoveryCriteria criteria(
            final UUID userId,
            final String area,
            final OffsetDateTime startsFrom,
            final OffsetDateTime startsTo,
            final LocalTime timeFrom,
            final LocalTime timeTo,
            final MatchJoinMode joinMode,
            final Boolean availableOnly,
            final Integer page,
            final Integer size) {
        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (safePage < 0) {
            throw new DomainException("Page must be zero or greater.");
        }
        if (safeSize < 1 || safeSize > MAX_PAGE_SIZE) {
            throw new DomainException("Page size must be between 1 and 50.");
        }
        if (startsFrom != null && startsTo != null && startsFrom.isAfter(startsTo)) {
            throw new DomainException("Match discovery date range is not valid.");
        }
        if (timeFrom != null && timeTo != null && timeFrom.isAfter(timeTo)) {
            throw new DomainException("Match discovery time range is not valid.");
        }
        return new MatchDiscoveryCriteria(
                userId,
                OffsetDateTime.now(clock),
                area == null ? null : area.trim(),
                startsFrom,
                startsTo,
                timeFrom,
                timeTo,
                joinMode,
                fallbackTimeZone,
                Boolean.TRUE.equals(availableOnly),
                safePage,
                safeSize);
    }

    private String validatedFallbackTimeZone(final String configuredTimeZone) {
        String zone = configuredTimeZone == null || configuredTimeZone.isBlank()
                ? DEFAULT_FALLBACK_TIME_ZONE
                : configuredTimeZone.trim();
        try {
            return ZoneId.of(zone).getId();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    FALLBACK_TIME_ZONE_PROPERTY + " must be a valid IANA time zone.",
                    exception);
        }
    }

    private MatchDiscoveryResponse response(final MatchDiscoveryRow row) {
        int durationMinutes = (int) java.time.Duration.between(row.startsAt(), row.endsAt()).toMinutes();
        int occupiedPlaces = row.occupiedPlaces().intValue();
        int availablePlaces = Math.max(0, row.maxPlayers() - occupiedPlaces);
        return new MatchDiscoveryResponse(
                row.matchId(),
                row.displayName(),
                row.startsAt(),
                row.endsAt(),
                durationMinutes,
                row.maxPlayers(),
                row.occupiedPlaces(),
                availablePlaces,
                row.pitchId(),
                row.pitchName(),
                row.pitchAddress(),
                row.pitchBasePrice(),
                row.pitchCurrency(),
                row.groupVisibility(),
                row.joinMode(),
                row.viewerIsOrganizer(),
                row.viewerParticipationStatus());
    }
}
