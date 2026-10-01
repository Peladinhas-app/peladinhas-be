package com.peladinhas.backend.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.peladinhas.backend.config.StartupPerformanceAnalyzer.StartupPerformanceReport;
import com.peladinhas.backend.config.StartupPerformanceAnalyzer.StartupStepSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.stereotype.Component;

@Component
public class StartupPerformanceLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupPerformanceLogger.class);

    private final StartupPerformanceAnalyzer analyzer;

    private volatile Integer webServerPort;

    public StartupPerformanceLogger(final StartupPerformanceAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @EventListener
    public void onWebServerInitialized(final WebServerInitializedEvent event) {
        this.webServerPort = event.getWebServer().getPort();
    }

    @EventListener
    public void onApplicationReady(final ApplicationReadyEvent event) {
        final ApplicationStartup startup = event.getApplicationContext().getApplicationStartup();
        if (!(startup instanceof BufferingApplicationStartup bufferingStartup)) {
            LOGGER.info("startup.performance unavailable reason=buffering-application-startup-not-configured");
            return;
        }

        final List<StartupStepSample> samples = samples(bufferingStartup.drainBufferedTimeline());
        final StartupPerformanceReport report = analyzer.analyze(samples);

        LOGGER.info(
                "startup.performance summary applicationReadyMs={} recordedSteps={} recordedDurationMs={} webServerPort={}",
                event.getTimeTaken().toMillis(),
                report.recordedStepCount(),
                report.recordedDuration().toMillis(),
                webServerPort == null ? "unknown" : webServerPort);
        report.phases().forEach(phase -> LOGGER.info(
                "startup.performance phase={} steps={} durationMs={}",
                phase.name(),
                phase.stepCount(),
                phase.duration().toMillis()));
        for (int index = 0; index < report.slowestSteps().size(); index++) {
            final StartupStepSample step = report.slowestSteps().get(index);
            LOGGER.info(
                    "startup.performance slowStepRank={} durationMs={} name={} tags={}",
                    index + 1,
                    step.duration().toMillis(),
                    step.name(),
                    step.tagsForLog());
        }
    }

    private List<StartupStepSample> samples(final StartupTimeline timeline) {
        return timeline.getEvents().stream()
                .map(event -> new StartupStepSample(
                        event.getStartupStep().getName(),
                        tags(event.getStartupStep()),
                        event.getDuration()))
                .toList();
    }

    private Map<String, String> tags(final StartupStep startupStep) {
        final Map<String, String> tags = new LinkedHashMap<>();
        startupStep.getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));
        return tags;
    }
}
