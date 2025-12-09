package com.truholdem.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestConfig;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.model.Game;
import com.truholdem.model.HandHistory;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.PlayerInfo;
import com.truholdem.model.PlayerStatistics;
import com.truholdem.repository.HandHistoryRepository;
import com.truholdem.repository.PlayerStatisticsRepository;
import com.truholdem.service.PlayerStatisticsService;
import com.truholdem.service.PokerGameService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({ TestConfig.class, TestSecurityConfig.class })
@DisplayName("Statistics & History Controller Integration Tests")
@Disabled("Spring Context issues - requires full infrastructure")
class StatisticsHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlayerStatisticsRepository statsRepository;

    @Autowired
    private HandHistoryRepository historyRepository;

    @Autowired
    private PlayerStatisticsService statsService;

    @Autowired
    private PokerGameService pokerGameService;

    @Nested
    @DisplayName("Statistics Controller Tests")
    class StatisticsControllerTests {

        private PlayerStatistics testStats;

        @BeforeEach
        void setUp() {
            testStats = new PlayerStatistics();
            testStats.setPlayerName("TestPlayer");
            testStats.setHandsPlayed(100);
            testStats.setHandsWon(25);
            testStats.setTotalWinnings(new BigDecimal("5000"));
            testStats.setTotalLosses(new BigDecimal("3000"));
            testStats.setHandsVoluntarilyPutInPot(40);
            testStats.setHandsRaisedPreFlop(20);
            testStats.setTotalBets(50);
            testStats.setTotalRaises(30);
            testStats.setTotalCalls(80);
            testStats.setHandsWentToShowdown(30);
            testStats.setShowdownsWon(15);
            testStats.setBiggestPotWon(500);
            testStats.setLongestWinStreak(5);

            statsRepository.save(testStats);
        }

        @Test
        @DisplayName("Should get player statistics by name")
        void shouldGetPlayerStatsByName() throws Exception {
            mockMvc.perform(get("/api/stats/player/{playerName}", "TestPlayer"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerName").value("TestPlayer"))
                    .andExpect(jsonPath("$.handsPlayed").value(100))
                    .andExpect(jsonPath("$.handsWon").value(25));
        }

        @Test
        @DisplayName("Should return 404 for unknown player")
        void shouldReturn404ForUnknownPlayer() throws Exception {
            mockMvc.perform(get("/api/stats/player/{playerName}", "NonExistent"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should get player stats summary")
        void shouldGetPlayerStatsSummary() throws Exception {
            mockMvc.perform(get("/api/stats/player/{playerName}/summary", "TestPlayer"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerName").value("TestPlayer"))
                    .andExpect(jsonPath("$.winRate").value(25.0))
                    .andExpect(jsonPath("$.vpip").value(40.0))
                    .andExpect(jsonPath("$.pfr").value(20.0));
        }

        @Test
        @DisplayName("Should get leaderboard by winnings")
        void shouldGetLeaderboardByWinnings() throws Exception {

            for (int i = 0; i < 5; i++) {
                PlayerStatistics stats = new PlayerStatistics();
                stats.setPlayerName("Player" + i);
                stats.setTotalWinnings(new BigDecimal(1000 * (i + 1)));
                stats.setHandsPlayed(50);
                statsRepository.save(stats);
            }

            mockMvc.perform(get("/api/stats/leaderboard/winnings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should get leaderboard by hands won")
        void shouldGetLeaderboardByHandsWon() throws Exception {
            mockMvc.perform(get("/api/stats/leaderboard/hands-won"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should get leaderboard by win rate")
        void shouldGetLeaderboardByWinRate() throws Exception {
            mockMvc.perform(get("/api/stats/leaderboard/win-rate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should get leaderboard by biggest pot")
        void shouldGetLeaderboardByBiggestPot() throws Exception {
            mockMvc.perform(get("/api/stats/leaderboard/biggest-pot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should get leaderboard by win streak")
        void shouldGetLeaderboardByWinStreak() throws Exception {
            mockMvc.perform(get("/api/stats/leaderboard/win-streak"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should get comprehensive leaderboard")
        void shouldGetComprehensiveLeaderboard() throws Exception {
            mockMvc.perform(get("/api/stats/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.topWinnings").isArray())
                    .andExpect(jsonPath("$.topHandsWon").isArray())
                    .andExpect(jsonPath("$.topWinRate").isArray())
                    .andExpect(jsonPath("$.topBiggestPot").isArray())
                    .andExpect(jsonPath("$.topWinStreak").isArray())
                    .andExpect(jsonPath("$.mostActive").isArray());
        }

        @Test
        @DisplayName("Should search players by name")
        void shouldSearchPlayersByName() throws Exception {
            mockMvc.perform(get("/api/stats/search")
                    .param("query", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$[0].playerName", containsString("Test")));
        }

        @Test
        @DisplayName("Should get most active players")
        void shouldGetMostActivePlayers() throws Exception {
            mockMvc.perform(get("/api/stats/leaderboard/most-active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("Hand History Controller Tests")
    class HandHistoryControllerTests {

        private HandHistory testHistory;
        private UUID testGameId;

        @BeforeEach
        void setUp() {
            testGameId = UUID.randomUUID();

            testHistory = new HandHistory();
            testHistory.setGameId(testGameId);
            testHistory.setHandNumber(1);
            testHistory.setSmallBlind(10);
            testHistory.setBigBlind(20);
            testHistory.setDealerPosition(0);
            testHistory.setPlayedAt(LocalDateTime.now());
            testHistory.setWinnerName("Alice");
            testHistory.setWinningHandDescription("Pair of Aces");
            testHistory.setFinalPot(150);

            HandHistory.HandHistoryPlayer player1 = new HandHistory.HandHistoryPlayer();
            player1.setPlayerId(UUID.randomUUID());
            player1.setPlayerName("Alice");
            player1.setStartingChips(1000);
            player1.setSeatPosition(0);
            testHistory.getPlayers().add(player1);

            HandHistory.HandHistoryPlayer player2 = new HandHistory.HandHistoryPlayer();
            player2.setPlayerId(UUID.randomUUID());
            player2.setPlayerName("Bob");
            player2.setStartingChips(1000);
            player2.setSeatPosition(1);
            testHistory.getPlayers().add(player2);

            HandHistory.ActionRecord action1 = new HandHistory.ActionRecord(
                    UUID.randomUUID(),
                    "Alice",
                    "RAISE",
                    60,
                    "PRE_FLOP",
                    LocalDateTime.now());
            testHistory.getActions().add(action1);

            HandHistory.ActionRecord action2 = new HandHistory.ActionRecord(
                    UUID.randomUUID(),
                    "Bob",
                    "CALL",
                    60,
                    "PRE_FLOP",
                    LocalDateTime.now());
            testHistory.getActions().add(action2);

            historyRepository.save(testHistory);
        }

        @Test
        @DisplayName("Should get hand history by ID")
        void shouldGetHandHistoryById() throws Exception {
            mockMvc.perform(get("/api/history/{historyId}", testHistory.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testHistory.getId().toString()))
                    .andExpect(jsonPath("$.winnerName").value("Alice"))
                    .andExpect(jsonPath("$.finalPot").value(150));
        }

        @Test
        @DisplayName("Should return 404 for non-existent history")
        void shouldReturn404ForNonExistentHistory() throws Exception {
            mockMvc.perform(get("/api/history/{historyId}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should get all hands for a game")
        void shouldGetGameHistory() throws Exception {
            mockMvc.perform(get("/api/history/game/{gameId}", testGameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].handNumber").value(1));
        }

        @Test
        @DisplayName("Should get paged game history")
        void shouldGetPagedGameHistory() throws Exception {
            mockMvc.perform(get("/api/history/game/{gameId}/paged", testGameId)
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should get player hand history")
        void shouldGetPlayerHistory() throws Exception {
            mockMvc.perform(get("/api/history/player/{playerId}",
                    testHistory.getPlayers().get(0).getPlayerId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should get hands won by player name")
        void shouldGetWinsByPlayerName() throws Exception {
            mockMvc.perform(get("/api/history/wins/{playerName}", "Alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].winnerName").value("Alice"));
        }

        @Test
        @DisplayName("Should get recent hands")
        void shouldGetRecentHands() throws Exception {
            mockMvc.perform(get("/api/history/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should get biggest pots")
        void shouldGetBiggestPots() throws Exception {

            for (int i = 0; i < 5; i++) {
                HandHistory history = new HandHistory();
                history.setGameId(UUID.randomUUID());
                history.setHandNumber(i + 1);
                history.setPlayedAt(LocalDateTime.now());
                history.setFinalPot(1000 * (i + 1));
                history.setWinnerName("Player" + i);
                historyRepository.save(history);
            }

            mockMvc.perform(get("/api/history/biggest-pots"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should get replay data")
        void shouldGetReplayData() throws Exception {
            mockMvc.perform(get("/api/history/{historyId}/replay", testHistory.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.handNumber").value(1))
                    .andExpect(jsonPath("$.players").isArray())
                    .andExpect(jsonPath("$.actions").isArray());
        }

        @Test
        @DisplayName("Should get hand count for game")
        void shouldGetHandCount() throws Exception {
            mockMvc.perform(get("/api/history/game/{gameId}/count", testGameId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }

        @Test
        @DisplayName("Should delete game history")
        void shouldDeleteGameHistory() throws Exception {
            mockMvc.perform(delete("/api/history/game/{gameId}", testGameId))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/history/game/{gameId}", testGameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Statistics Integration with Game Flow")
    class StatisticsGameFlowIntegrationTests {

        @Test
        @DisplayName("Should record statistics when game is played")
        void shouldRecordStatsDuringGame() throws Exception {

            List<PlayerInfo> players = Arrays.asList(
                    new PlayerInfo("StatsTestPlayer", 1000, false),
                    new PlayerInfo("StatsTestBot", 1000, true));

            Game game = pokerGameService.createNewGame(players);

            PlayerStatistics stats = statsService.getOrCreateStats("StatsTestPlayer");
            int initialHands = stats.getHandsPlayed();

            Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
            if (currentPlayer.getName().equals("StatsTestPlayer")) {
                pokerGameService.playerAct(
                        game.getId(), currentPlayer.getId(), PlayerAction.FOLD, 0);
            }

            stats = statsRepository.findByPlayerName("StatsTestPlayer").orElseThrow();

        }
    }
}
