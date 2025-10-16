package com.truholdem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestConfig;
import com.truholdem.service.HandAnalysisService;
import com.truholdem.service.HandHistoryService;
import com.truholdem.service.HandEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(HandAnalysisController.class)
@Import(TestConfig.class)
@DisplayName("HandAnalysisController Integration Tests")
class HandAnalysisControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HandHistoryService handHistoryService;


    private HandAnalysisService handAnalysisService;
    private HandEvaluator handEvaluator;

    @BeforeEach
    void setUp() {
        handEvaluator = new HandEvaluator();
        handAnalysisService = new HandAnalysisService(handEvaluator);
    }



    @Nested
    @DisplayName("POST /api/v1/analysis/equity")
    class EquityEndpointTests {

        @Test
        @DisplayName("Should calculate equity for valid request")
        void calculateEquity_ValidRequest_ReturnsEquityResult() throws Exception {
            String request = """
                {
                    "heroHand": "AS,AH",
                    "communityCards": null,
                    "villainRange": "KK,QQ,JJ,AKs,AKo",
                    "simulations": 1000
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/equity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.equity").exists())
                    .andExpect(jsonPath("$.winProbability").exists())
                    .andExpect(jsonPath("$.tieProbability").exists())
                    .andExpect(jsonPath("$.loseProbability").exists())
                    .andExpect(jsonPath("$.simulationCount").exists());
        }

        @Test
        @DisplayName("Should return 400 for missing hero hand")
        void calculateEquity_MissingHeroHand_Returns400() throws Exception {
            String request = """
                {
                    "villainRange": "AA,KK"
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/equity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should calculate equity with community cards")
        void calculateEquity_WithCommunityCards_ReturnsEquityResult() throws Exception {
            String request = """
                {
                    "heroHand": "AS,KS",
                    "communityCards": "TS,JS,2H",
                    "villainRange": "QQ,JJ,TT",
                    "simulations": 500
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/equity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.equity").isNumber());
        }

        @Test
        @DisplayName("Should calculate equity using villain position")
        void calculateEquity_WithVillainPosition_ReturnsEquityResult() throws Exception {
            String request = """
                {
                    "heroHand": "QS,QH",
                    "villainPosition": "DEALER"
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/equity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.equity").isNumber());
        }
    }



    @Nested
    @DisplayName("GET /api/v1/analysis/equity/quick")
    class QuickEquityEndpointTests {

        @Test
        @DisplayName("Should return quick equity calculation")
        void quickEquity_ValidParams_ReturnsResult() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/equity/quick")
                            .param("heroHand", "AS,AH")
                            .param("villainPosition", "DEALER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.equity").isNumber());
        }

        @Test
        @DisplayName("Should work with community cards")
        void quickEquity_WithCommunityCards_ReturnsResult() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/equity/quick")
                            .param("heroHand", "KS,KH")
                            .param("communityCards", "AS,7D,2C")
                            .param("villainPosition", "BIG_BLIND"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.equity").isNumber());
        }

        @Test
        @DisplayName("Should return 400 for missing hero hand")
        void quickEquity_MissingHeroHand_Returns400() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/equity/quick")
                            .param("villainPosition", "DEALER"))
                    .andExpect(status().isBadRequest());
        }
    }



    @Nested
    @DisplayName("POST /api/v1/analysis/ev")
    class EVEndpointTests {

        @Test
        @DisplayName("Should calculate EV for all actions")
        void calculateEV_ValidRequest_ReturnsAllActions() throws Exception {
            String request = """
                {
                    "heroHand": "AS,KS",
                    "communityCards": "QS,JS,2H",
                    "potSize": 100,
                    "betToCall": 50,
                    "villainRange": "AA,KK,QQ"
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/ev")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.FOLD").exists())
                    .andExpect(jsonPath("$.CALL").exists());
        }

        @Test
        @DisplayName("Should include raise option when bet to call is 0")
        void calculateEV_NoBetToCall_IncludesCheck() throws Exception {
            String request = """
                {
                    "heroHand": "9S,9H",
                    "potSize": 100,
                    "betToCall": 0,
                    "villainRange": "AA,KK,QQ,JJ,TT"
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/ev")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.CHECK").exists())
                    .andExpect(jsonPath("$.BET").exists());
        }

        @Test
        @DisplayName("Should return 400 for missing pot size")
        void calculateEV_MissingPotSize_Returns400() throws Exception {
            String request = """
                {
                    "heroHand": "AS,KS",
                    "betToCall": 50
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/ev")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }
    }



    @Nested
    @DisplayName("POST /api/v1/analysis/recommend")
    class RecommendationEndpointTests {

        @Test
        @DisplayName("Should return GTO recommendation")
        void getRecommendation_ValidRequest_ReturnsRecommendation() throws Exception {
            String request = """
                {
                    "heroHand": "AS,KS",
                    "potSize": 100,
                    "betToCall": 30,
                    "position": "DEALER",
                    "totalPlayers": 6
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/recommend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.primaryAction").exists())
                    .andExpect(jsonPath("$.confidence").isNumber())
                    .andExpect(jsonPath("$.reasoning").isString());
        }

        @Test
        @DisplayName("Should include position advice")
        void getRecommendation_WithPosition_IncludesPositionAdvice() throws Exception {
            String request = """
                {
                    "heroHand": "7S,7H",
                    "potSize": 50,
                    "betToCall": 10,
                    "position": "EARLY",
                    "seatNumber": 1,
                    "dealerPosition": 5,
                    "totalPlayers": 6
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/recommend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.positionAdvice").isString())
                    .andExpect(jsonPath("$.handStrengthCategory").isString());
        }

        @Test
        @DisplayName("Should return alternatives")
        void getRecommendation_Always_ReturnsAlternatives() throws Exception {
            String request = """
                {
                    "heroHand": "QS,JH",
                    "potSize": 100,
                    "position": "CUTOFF"
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/recommend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alternatives").isArray());
        }
    }



    @Nested
    @DisplayName("GET /api/v1/analysis/ranges/presets")
    class RangePresetsEndpointTests {

        @Test
        @DisplayName("Should return list of range presets")
        void getRangePresets_ReturnsPresets() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/ranges/presets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].description").exists())
                    .andExpect(jsonPath("$[0].hands").isArray())
                    .andExpect(jsonPath("$[0].percentage").isNumber());
        }

        @Test
        @DisplayName("Should include common presets")
        void getRangePresets_IncludesCommonPresets() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/analysis/ranges/presets"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();

            assertThat(content).contains("premium");
            assertThat(content).contains("buttonOpen");
            assertThat(content).contains("earlyPosition");
        }

        @Test
        @DisplayName("Presets should have valid percentages")
        void getRangePresets_ValidPercentages() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/ranges/presets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].percentage", everyItem(greaterThan(0.0))))
                    .andExpect(jsonPath("$[*].percentage", everyItem(lessThanOrEqualTo(100.0))));
        }
    }



    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid JSON gracefully")
        void invalidJson_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/analysis/equity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle invalid card notation")
        void invalidCardNotation_Returns400() throws Exception {
            String request = """
                {
                    "heroHand": "INVALID,CARDS",
                    "potSize": 100
                }
                """;

            mockMvc.perform(post("/api/v1/analysis/equity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 for non-existent hand history")
        void nonExistentHandHistory_Returns404() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/hand/{handId}", "00000000-0000-0000-0000-000000000000")
                            .param("playerName", "TestPlayer"))
                    .andExpect(status().isNotFound());
        }
    }



    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Quick equity should complete in reasonable time")
        void quickEquity_Performance() throws Exception {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(get("/api/v1/analysis/equity/quick")
                            .param("heroHand", "AS,AH")
                            .param("villainPosition", "DEALER"))
                    .andExpect(status().isOk());

            long elapsed = System.currentTimeMillis() - startTime;
            assertThat(elapsed)
                    .as("Quick equity should complete within 2 seconds")
                    .isLessThan(2000);
        }

        @Test
        @DisplayName("Range presets should be fast (cached)")
        void rangePresets_Performance() throws Exception {

            mockMvc.perform(get("/api/v1/analysis/ranges/presets"))
                    .andExpect(status().isOk());


            long startTime = System.currentTimeMillis();
            mockMvc.perform(get("/api/v1/analysis/ranges/presets"))
                    .andExpect(status().isOk());
            long elapsed = System.currentTimeMillis() - startTime;

            assertThat(elapsed)
                    .as("Range presets endpoint should be fast")
                    .isLessThan(100);
        }
    }
}