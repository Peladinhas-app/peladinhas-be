package com.peladinhas.backend.config;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class StartupPerformanceAnalyzer {

    static final int SLOW_STEP_LIMIT = 12;

    private static final List<StartupPhase> PHASES = List.of(
            new StartupPhase("datasource", List.of("datasource", "hikari", "jdbc")),
            new StartupPhase("flyway", List.of("flyway")),
            new StartupPhase("jpa", List.of("jpa", "hibernate", "entitymanager")),
            new StartupPhase("repositories", List.of("repository")),
            new StartupPhase("web-server", List.of("tomcat", "webserver", "servlet")));

    public StartupPerformanceReport analyze(final List<StartupStepSample> samples) {
        final List<StartupStepSample> safeSamples = samples == null ? List.of() : samples;
        return new StartupPerformanceReport(
                safeSamples.size(),
                totalDuration(safeSamples),
                phaseSummaries(safeSamples),
                slowestSteps(safeSamples));
    }

    private Duration totalDuration(final List<StartupStepSample> samples) {
        return samples.stream()
                .map(StartupStepSample::duration)
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);
    }

    private List<StartupPhaseSummary> phaseSummaries(final List<StartupStepSample> samples) {
        return PHASES.stream()
                .map(phase -> summarizePhase(phase, samples))
                .toList();
    }

    private StartupPhaseSummary summarizePhase(final StartupPhase phase, final List<StartupStepSample> samples) {
        final List<StartupStepSample> matches = samples.stream()
                .filter(sample -> matchesPhase(sample, phase))
                .toList();
        final Duration duration = matches.stream()
                .map(StartupStepSample::duration)
                .reduce(Duration.ZERO, Duration::plus);
        return new StartupPhaseSummary(phase.name(), matches.size(), duration);
    }

    private boolean matchesPhase(final StartupStepSample sample, final StartupPhase phase) {
        final String searchable = (sample.name() + " " + sample.tags()).toLowerCase(Locale.ROOT);
        return phase.tokens().stream().anyMatch(searchable::contains);
    }

    private List<StartupStepSample> slowestSteps(final List<StartupStepSample> samples) {
        return samples.stream()
                .sorted(Comparator.comparing(StartupStepSample::duration, Comparator.reverseOrder()))
                .limit(SLOW_STEP_LIMIT)
                .toList();
    }

    record StartupPhase(String name, List<String> tokens) {
    }

    public record StartupStepSample(String name, Map<String, String> tags, Duration duration) {

        public StartupStepSample {
            tags = tags == null ? Map.of() : Map.copyOf(tags);
        }

        public String tagsForLog() {
            if (tags.isEmpty()) {
                return "-";
            }
            return tags.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(","));
        }
    }

    public record StartupPhaseSummary(String name, int stepCount, Duration duration) {
    }

    public record StartupPerformanceReport(
            int recordedStepCount,
            Duration recordedDuration,
            List<StartupPhaseSummary> phases,
            List<StartupStepSample> slowestSteps) {
    }
}
