package com.peladinhas.backend.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.peladinhas.backend.config.PeladinhasCorsProperties;
import com.peladinhas.backend.config.WebConfig;
import com.peladinhas.backend.health.HealthController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.yaml.snakeyaml.Yaml;

class DeploymentConfigurationTests {

    private AnnotationConfigWebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMvcContext() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of(
                        "peladinhas.cors.allowed-origins=https://peladinhas-test.web.app, http://localhost:5173")
                .applyTo(context);
        context.register(DeploymentTestConfig.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void closeMvcContext() {
        context.close();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void configuredCorsOriginIsAllowedForApiPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/groups")
                        .header(HttpHeaders.ORIGIN, "https://peladinhas-test.web.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://peladinhas-test.web.app"));
    }

    @Test
    void nonConfiguredCorsOriginIsNotAllowedForApiPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/groups")
                        .header(HttpHeaders.ORIGIN, "https://example.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void corsOriginsAreLoadedFromConfiguration() {
        PeladinhasCorsProperties corsProperties = context.getBean(PeladinhasCorsProperties.class);

        assertThat(corsProperties.allowedOriginsOrDefault())
                .containsExactly("https://peladinhas-test.web.app", "http://localhost:5173");
    }

    @Test
    void renderBlueprintUsesOnlyFreeWebServicePlan() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> blueprint = new Yaml().load(Files.readString(Path.of("render.yaml")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> services = (List<Map<String, Object>>) blueprint.get("services");

        assertThat(blueprint).doesNotContainKeys("databases");
        assertThat(services).singleElement().satisfies(service -> {
            assertThat(service)
                    .containsEntry("type", "web")
                    .containsEntry("name", "peladinhas-backend")
                    .containsEntry("plan", "free");
            assertThat(service).doesNotContainKeys("disk", "disks");
        });
    }

    @Configuration
    @EnableWebMvc
    @EnableConfigurationProperties(PeladinhasCorsProperties.class)
    @Import(WebConfig.class)
    static class DeploymentTestConfig {

        @Bean
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        HealthController healthController(final Clock clock) {
            return new HealthController(clock);
        }

        @Bean
        TestApiController testApiController() {
            return new TestApiController();
        }
    }

    @RestController
    static class TestApiController {

        @GetMapping("/api/v1/groups")
        String apiRoute() {
            return "ok";
        }
    }
}
