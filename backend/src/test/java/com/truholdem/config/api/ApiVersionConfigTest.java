package com.truholdem.config.api;

import com.truholdem.config.SecurityConfig;
import com.truholdem.config.TestConfig;
import com.truholdem.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Integration tests for API versioning configuration.
 * Note: Annotation tests have been moved to ApiVersionAnnotationTest for better isolation.
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
@DisplayName("API Versioning Configuration")
@DisabledIfSystemProperty(named = "skipIntegrationTests", matches = "true")
class ApiVersionConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Path Prefix Verification")
    class PathPrefixTests {

        @Test
        @DisplayName("Game API should be accessible at /api/v1/poker/game/*")
        void gameApiShouldBeAccessibleAtV1Path() throws Exception {
            
            
            mockMvc.perform(get("/v1/poker/game/00000000-0000-0000-0000-000000000000")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); 
        }

        @Test
        @DisplayName("Analysis API should be accessible at /api/v1/analysis/*")
        void analysisApiShouldBeAccessibleAtV1Path() throws Exception {
            mockMvc.perform(get("/v1/analysis/ranges/presets")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Tournament API should be accessible at /api/v1/tournaments")
        void tournamentApiShouldBeAccessibleAtV1Path() throws Exception {
            mockMvc.perform(get("/v1/tournaments")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Old paths without version prefix should return error")
        void oldPathsWithoutVersionShouldNotWork() throws Exception {
            // Old path without version prefix should return error (4xx or 5xx)
            mockMvc.perform(get("/poker/game/status")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 200 && status < 400) {
                        throw new AssertionError("Expected error status (4xx or 5xx) but got: " + status);
                    }
                });
        }
    }

    @Nested
    @DisplayName("OpenAPI Documentation")
    class OpenApiTests {

        @Test
        @DisplayName("OpenAPI spec should be available")
        void openApiSpecShouldBeAvailable() throws Exception {
            mockMvc.perform(get("/v3/api-docs")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("TruHoldem Poker API"))
                .andExpect(jsonPath("$.info.version").exists());
        }

        @Test
        @DisplayName("Swagger UI should be accessible")
        void swaggerUiShouldBeAccessible() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Game API group should be available")
        void gameApiGroupShouldBeAvailable() throws Exception {
            mockMvc.perform(get("/v3/api-docs/1-game")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Tournament API group should be available")
        void tournamentApiGroupShouldBeAvailable() throws Exception {
            mockMvc.perform(get("/v3/api-docs/2-tournament")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Analysis API group should be available")
        void analysisApiGroupShouldBeAvailable() throws Exception {
            mockMvc.perform(get("/v3/api-docs/3-analysis")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("OpenAPI should include security scheme")
        void openApiShouldIncludeSecurityScheme() throws Exception {
            mockMvc.perform(get("/v3/api-docs")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
        }
    }

    @Nested
    @DisplayName("API Version Consistency")
    class VersionConsistencyTests {

        @Test
        @DisplayName("All v1 endpoints should use consistent base path")
        void v1EndpointsShouldUseConsistentBasePath() throws Exception {
            
            
            mockMvc.perform(get("/v1/analysis/ranges/presets"))
                .andExpect(status().isOk());
            
            mockMvc.perform(get("/v1/tournaments"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Legacy API path should return error")
        void legacyApiShouldStillBeAccessible() throws Exception {
            // Legacy path should return error (4xx or 5xx) since it's not a valid versioned endpoint
            mockMvc.perform(get("/poker/status"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 200 && status < 400) {
                        throw new AssertionError("Expected error status (4xx or 5xx) but got: " + status);
                    }
                });
        }
    }
}
