package com.truholdem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.model.PlayerStatistics;
import com.truholdem.service.PlayerStatisticsService;
import com.truholdem.service.PlayerStatisticsService.LeaderboardData;
import com.truholdem.service.PlayerStatisticsService.PlayerStatsSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(StatisticsController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("StatisticsController Integration Tests")
class StatisticsControllerIT {

    private static final String BASE_URL = "/api/stats";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlayerStatisticsService statsService;

    private PlayerStatistics testStats;
    private List<PlayerStatistics> statsList;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testStats = new PlayerStatistics("TestPlayer");
        testStats.setId(UUID.randomUUID());
        testStats.setUserId(testUserId);
        testStats.setHandsPlayed(100);
        testStats.setHandsWon(25);
        testStats.setTotalWinnings(BigDecimal.valueOf(5000));
        testStats.setTotalLosses(BigDecimal.valueOf(2000));
        testStats.setBiggestPotWon(500);
        testStats.setHandsVoluntarilyPutInPot(30);
        testStats.setHandsRaisedPreFlop(15);
        testStats.setTotalBets(50);
        testStats.setTotalRaises(30);
        testStats.setTotalCalls(40);
        testStats.setTotalFolds(20);
        testStats.setTotalChecks(60);
        testStats.setHandsWentToShowdown(40);
        testStats.setShowdownsWon(20);
        testStats.setLongestWinStreak(5);
        testStats.setFirstHandPlayed(LocalDateTime.now().minusDays(30));
        testStats.setLastHandPlayed(LocalDateTime.now());

        statsList = createTestStatsList();
    }

    private List<PlayerStatistics> createTestStatsList() {
        List<PlayerStatistics> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            PlayerStatistics stats = new PlayerStatistics("Player" + i);
            stats.setId(UUID.randomUUID());
            stats.setHandsPlayed(100 * i);
            stats.setHandsWon(25 * i);
            stats.setTotalWinnings(BigDecimal.valueOf(1000 * i));
            stats.setBiggestPotWon(100 * i);
            stats.setLongestWinStreak(i);
            list.add(stats);
        }
        return list;
    }

    
    
    
    @Nested
    @DisplayName("Player Statistics Tests - GET /api/stats/player/{name}")
    class PlayerStatisticsTests {

        @Test
        @DisplayName("Should return stats for existing player - returns 200")
        void getPlayerStats_ExistingPlayer_Returns200() throws Exception {
            when(statsService.getStatsByName("TestPlayer")).thenReturn(Optional.of(testStats));

            mockMvc.perform(get(BASE_URL + "/player/{playerName}", "TestPlayer"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.playerName").value("TestPlayer"))
                    .andExpect(jsonPath("$.handsPlayed").value(100))
                    .andExpect(jsonPath("$.handsWon").value(25))
                    .andExpect(jsonPath("$.totalWinnings").value(5000))
                    .andExpect(jsonPath("$.biggestPotWon").value(500));

            verify(statsService).getStatsByName("TestPlayer");
        }

        @Test
        @DisplayName("Should return 404 for non-existing player")
        void getPlayerStats_NonExistingPlayer_Returns404() throws Exception {
            when(statsService.getStatsByName("UnknownPlayer")).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_URL + "/player/{playerName}", "UnknownPlayer"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return stats with VPIP calculated")
        void getPlayerStats_ReturnsVPIP() throws Exception {
            when(statsService.getStatsByName("TestPlayer")).thenReturn(Optional.of(testStats));

            mockMvc.perform(get(BASE_URL + "/player/{playerName}", "TestPlayer"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.handsVoluntarilyPutInPot").value(30));
        }

        @Test
        @DisplayName("Should return stats with PFR data")
        void getPlayerStats_ReturnsPFR() throws Exception {
            when(statsService.getStatsByName("TestPlayer")).thenReturn(Optional.of(testStats));

            mockMvc.perform(get(BASE_URL + "/player/{playerName}", "TestPlayer"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.handsRaisedPreFlop").value(15));
        }

        @Test
        @DisplayName("Should return stats by user ID - returns 200")
        void getPlayerStatsByUserId_ExistingUser_Returns200() throws Exception {
            when(statsService.getStatsByUserId(testUserId)).thenReturn(Optional.of(testStats));

            mockMvc.perform(get(BASE_URL + "/player/id/{userId}", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerName").value("TestPlayer"));
        }

        @Test
        @DisplayName("Should return 404 for non-existing user ID")
        void getPlayerStatsByUserId_NonExistingUser_Returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(statsService.getStatsByUserId(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_URL + "/player/id/{userId}", unknownId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return stats summary - returns 200")
        void getPlayerStatsSummary_ExistingPlayer_Returns200() throws Exception {
            PlayerStatsSummary summary = new PlayerStatsSummary(
                    "TestPlayer", 100, 25, 25.0, BigDecimal.valueOf(3000),
                    30.0, 15.0, 2.0, 40.0, 50.0, 500, 5, 10
            );
            when(statsService.getStatsSummary("TestPlayer")).thenReturn(summary);

            mockMvc.perform(get(BASE_URL + "/player/{playerName}/summary", "TestPlayer"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerName").value("TestPlayer"))
                    .andExpect(jsonPath("$.winRate").value(25.0))
                    .andExpect(jsonPath("$.vpip").value(30.0))
                    .andExpect(jsonPath("$.pfr").value(15.0));
        }

        @Test
        @DisplayName("Should return 404 for non-existing player summary")
        void getPlayerStatsSummary_NonExistingPlayer_Returns404() throws Exception {
            when(statsService.getStatsSummary("Unknown")).thenReturn(null);

            mockMvc.perform(get(BASE_URL + "/player/{playerName}/summary", "Unknown"))
                    .andExpect(status().isNotFound());
        }
    }

    
    
    
    @Nested
    @DisplayName("Search Tests - GET /api/stats/search")
    class SearchTests {

        @Test
        @DisplayName("Should search players by name")
        void searchPlayers_ValidQuery_ReturnsResults() throws Exception {
            when(statsService.searchPlayers("Player")).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("query", "Player"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Should return empty list for no matches")
        void searchPlayers_NoMatches_ReturnsEmptyList() throws Exception {
            when(statsService.searchPlayers("NonExistent")).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("query", "NonExistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    
    
    
    @Nested
    @DisplayName("Leaderboard Tests - GET /api/stats/leaderboard/*")
    class LeaderboardTests {

        @Test
        @DisplayName("Should return comprehensive leaderboard - returns 200")
        void getLeaderboard_ReturnsAllTypes() throws Exception {
            LeaderboardData leaderboard = new LeaderboardData(
                    statsList, statsList, statsList, statsList, statsList, statsList
            );
            when(statsService.getLeaderboard()).thenReturn(leaderboard);

            mockMvc.perform(get(BASE_URL + "/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.byWinnings", hasSize(5)))
                    .andExpect(jsonPath("$.byHandsWon", hasSize(5)))
                    .andExpect(jsonPath("$.byWinRate", hasSize(5)))
                    .andExpect(jsonPath("$.byBiggestPot", hasSize(5)))
                    .andExpect(jsonPath("$.byWinStreak", hasSize(5)))
                    .andExpect(jsonPath("$.mostActive", hasSize(5)));

            verify(statsService).getLeaderboard();
        }

        @Test
        @DisplayName("Should return top by winnings - returns 200")
        void getTopByWinnings_ReturnsRankedList() throws Exception {
            when(statsService.getTopByWinnings()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/winnings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].totalWinnings").exists());
        }

        @Test
        @DisplayName("Should return top by win rate - returns 200")
        void getTopByWinRate_ReturnsRankedList() throws Exception {
            when(statsService.getTopByWinRate()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/win-rate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Should return top by hands won - returns 200")
        void getTopByHandsWon_ReturnsRankedList() throws Exception {
            when(statsService.getTopByHandsWon()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/hands-won"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].handsWon").exists());
        }

        @Test
        @DisplayName("Should return top by biggest pot - returns 200")
        void getTopByBiggestPot_ReturnsRankedList() throws Exception {
            when(statsService.getTopByBiggestPot()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/biggest-pot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].biggestPotWon").exists());
        }

        @Test
        @DisplayName("Should return top by win streak - returns 200")
        void getTopByWinStreak_ReturnsRankedList() throws Exception {
            when(statsService.getTopByWinStreak()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/win-streak"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].longestWinStreak").exists());
        }

        @Test
        @DisplayName("Should return most active players - returns 200")
        void getMostActive_ReturnsRankedList() throws Exception {
            when(statsService.getMostActive()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/most-active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Should return recently active players - returns 200")
        void getRecentlyActive_ReturnsRankedList() throws Exception {
            when(statsService.getRecentlyActive()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/recently-active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Should return empty list when no stats exist")
        void getLeaderboard_NoStats_ReturnsEmptyLists() throws Exception {
            LeaderboardData emptyLeaderboard = new LeaderboardData(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
            when(statsService.getLeaderboard()).thenReturn(emptyLeaderboard);

            mockMvc.perform(get(BASE_URL + "/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.byWinnings", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 for invalid leaderboard type")
        void getLeaderboard_InvalidType_Returns404() throws Exception {
            mockMvc.perform(get(BASE_URL + "/leaderboard/invalid-type"))
                    .andExpect(status().isNotFound());
        }
    }

    
    
    
    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Should return JSON content type")
        void responses_ShouldBeJson() throws Exception {
            when(statsService.getStatsByName("TestPlayer")).thenReturn(Optional.of(testStats));

            mockMvc.perform(get(BASE_URL + "/player/{playerName}", "TestPlayer"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Leaderboard should return JSON content type")
        void leaderboard_ShouldReturnJson() throws Exception {
            when(statsService.getTopByWinnings()).thenReturn(statsList);

            mockMvc.perform(get(BASE_URL + "/leaderboard/winnings"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
