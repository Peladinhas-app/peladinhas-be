package com.peladinhas.backend.domains.matches.web;

import java.util.List;

public record MatchDiscoveryPageResponse(
        List<MatchDiscoveryResponse> matches,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
