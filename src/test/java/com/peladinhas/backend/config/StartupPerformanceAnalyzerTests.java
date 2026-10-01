package com.peladinhas.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.peladinhas.backend.config.StartupPerformanceAnalyzer.StartupPerformanceReport;
import com.peladinhas.backend.config.StartupPerformanceAnalyzer.StartupStepSample;
import org.junit.jupiter.api.Test;

class StartupPerformanceAnalyzerTests {

    private final StartupPerformanceAnalyzer analyzer = new StartupPerformanceAnalyzer();

    /**
     * Verifies that startup steps are grouped into useful production phases.
     */
    @Test
    void groupsStartupStepsIntoOperationalPhases() {
        StartupPerformanceReport report = analyzer.analyze(List.of(
                sample("spring.beans.instantiate", "beanName", "dataSource", 120),
                sample("spring.beans.instantiate", "beanName", "flyway", 230),
                sample("spring.beans.instantiate", "beanName", "entityManagerFactory", 340),
                sample("spring.data.repository.init", "repository", "matchRepository", 450),
                sample("spring.boot.webserver.start", "server", "tomcat", 560)));

        assertThat(report.recordedStepCount()).isEqualTo(5);
        assertThat(report.recordedDuration()).isEqualTo(Duration.ofMillis(1700));
        assertThat(report.phases())
                .extracting(StartupPerformanceAnalyzer.StartupPhaseSummary::name)
                .containsExactly("datasource", "flyway", "jpa", "repositories", "web-server");
        assertThat(report.phases())
                .extracting(StartupPerformanceAnalyzer.StartupPhaseSummary::duration)
                .containsExactly(
                        Duration.ofMillis(120),
                        Duration.ofMillis(230),
                        Duration.ofMillis(340),
                        Duration.ofMillis(450),
                        Duration.ofMillis(560));
    }

    /**
     * Verifies that the slowest startup steps are reported first.
     */
    @Test
    void ordersSlowestStartupStepsFirst() {
        StartupPerformanceReport report = analyzer.analyze(List.of(
                sample("fast", "beanName", "fastBean", 5),
                sample("slow", "beanName", "slowBean", 900),
                sample("medium", "beanName", "mediumBean", 200)));

        assertThat(report.slowestSteps())
                .extracting(StartupStepSample::name)
                .containsExactly("slow", "medium", "fast");
    }

    private StartupStepSample sample(
            final String name,
            final String tagKey,
            final String tagValue,
            final long durationMs) {
        return new StartupStepSample(name, Map.of(tagKey, tagValue), Duration.ofMillis(durationMs));
    }
}
