package com.truholdem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.model.HandHistory;
import com.truholdem.model.HandHistory.ActionRecord;
import com.truholdem.model.HandHistory.CardRecord;
import com.truholdem.model.HandHistory.HandHistoryPlayer;
import com.truholdem.service.HandHistoryService;
import com.truholdem.service.HandHistoryService.PlayerSnapshot;
import com.truholdem.service.HandHistoryService.ReplayAction;
import com.truholdem.service.HandHistoryService.ReplayData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(HandHistoryController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("HandHistoryController Integration Tests")
class HandHistoryControllerIT {

    private static final String BASE_URL = "/api/history";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HandHistoryService handHistoryService;

    private HandHistory testHistory;
    private UUID historyId;
    private UUID gameId;
    private UUID playerId;
    private List<HandHistory> historyList;

    @BeforeEach
    void setUp() {
        historyId = UUID.randomUUID();
        gameId = UUID.randomUUID();
        playerId = UUID.randomUUID();

        testHistory = createTestHandHistory();
        historyList = createTestHistoryList();
    }

    private HandHistory createTestHandHistory() {
        HandHistory history = new HandHistory();
        history.setId(historyId);
        history.setGameId(gameId);
        history.setHandNumber(1);
        history.setPlayedAt(LocalDateTime.now());
        history.setSmallBlind(10);
        history.setBigBlind(20);
        history.setDealerPosition(0);
        history.setWinnerName("Alice");
        history.setWinningHandDescription("Two Pair - Aces and Kings");
        history.setFinalPot(200);

        
        List<HandHistoryPlayer> players = new ArrayList<>();
        HandHistoryPlayer player1 = new HandHistoryPlayer();
        player1.setPlayerId(playerId);
        player1.setPlayerName("Alice");
        player1.setStartingChips(1000);
        player1.setSeatPosition(0);
        player1.setHoleCard1Suit("SPADES");
        player1.setHoleCard1Value("ACE");
        player1.setHoleCard2Suit("HEARTS");
        player1.setHoleCard2Value("KING");
        players.add(player1);

        HandHistoryPlayer player2 = new HandHistoryPlayer();
        player2.setPlayerId(UUID.randomUUID());
        player2.setPlayerName("Bob");
        player2.setStartingChips(1000);
        player2.setSeatPosition(1);
        player2.setHoleCard1Suit("DIAMONDS");
        player2.setHoleCard1Value("QUEEN");
        player2.setHoleCard2Suit("CLUBS");
        player2.setHoleCard2Value("JACK");
        players.add(player2);
        history.setPlayers(players);

        
        List<ActionRecord> actions = new ArrayList<>();
        actions.add(new ActionRecord(playerId, "Alice", "CALL", 20, "PRE_FLOP", LocalDateTime.now()));
        actions.add(new ActionRecord(player2.getPlayerId(), "Bob", "CHECK", 0, "PRE_FLOP", LocalDateTime.now()));
        actions.add(new ActionRecord(playerId, "Alice", "BET", 50, "FLOP", LocalDateTime.now()));
        actions.add(new ActionRecord(player2.getPlayerId(), "Bob", "FOLD", 0, "FLOP", LocalDateTime.now()));
        history.setActions(actions);

        
        List<CardRecord> board = new ArrayList<>();
        board.add(new CardRecord("HEARTS", "TEN"));
        board.add(new CardRecord("SPADES", "JACK"));
        board.add(new CardRecord("DIAMONDS", "QUEEN"));
        board.add(new CardRecord("CLUBS", "TWO"));
        board.add(new CardRecord("HEARTS", "THREE"));
        history.setBoard(board);

        return history;
    }

    private List<HandHistory> createTestHistoryList() {
        List<HandHistory> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            HandHistory history = new HandHistory();
            history.setId(UUID.randomUUID());
            history.setGameId(gameId);
            history.setHandNumber(i);
            history.setPlayedAt(LocalDateTime.now().minusMinutes(i * 10));
            history.setSmallBlind(10);
            history.setBigBlind(20);
            history.setWinnerName("Player" + i);
            history.setFinalPot(100 * i);
            history.setPlayers(new ArrayList<>());
            history.setActions(new ArrayList<>());
            history.setBoard(new ArrayList<>());
            list.add(history);
        }
        return list;
    }

    
    
    
    @Nested
    @DisplayName("Hand Retrieval Tests - GET /api/history/{id}")
    class HandRetrievalTests {

        @Test
        @DisplayName("Should return existing hand history - returns 200")
        void getHandHistory_ExistingHand_Returns200() throws Exception {
            when(handHistoryService.getHandHistory(historyId)).thenReturn(Optional.of(testHistory));

            mockMvc.perform(get(BASE_URL + "/{historyId}", historyId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(historyId.toString()))
                    .andExpect(jsonPath("$.gameId").value(gameId.toString()))
                    .andExpect(jsonPath("$.handNumber").value(1))
                    .andExpect(jsonPath("$.winnerName").value("Alice"))
                    .andExpect(jsonPath("$.finalPot").value(200));

            verify(handHistoryService).getHandHistory(historyId);
        }

        @Test
        @DisplayName("Should return 404 for non-existing hand")
        void getHandHistory_NonExistingHand_Returns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(handHistoryService.getHandHistory(nonExistentId)).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_URL + "/{historyId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return hand with all actions")
        void getHandHistory_ReturnsAllActions() throws Exception {
            when(handHistoryService.getHandHistory(historyId)).thenReturn(Optional.of(testHistory));

            mockMvc.perform(get(BASE_URL + "/{historyId}", historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actions", hasSize(4)))
                    .andExpect(jsonPath("$.actions[0].playerName").value("Alice"))
                    .andExpect(jsonPath("$.actions[0].action").value("CALL"))
                    .andExpect(jsonPath("$.actions[0].phase").value("PRE_FLOP"));
        }

        @Test
        @DisplayName("Should return hand with community cards")
        void getHandHistory_ReturnsCommunityCards() throws Exception {
            when(handHistoryService.getHandHistory(historyId)).thenReturn(Optional.of(testHistory));

            mockMvc.perform(get(BASE_URL + "/{historyId}", historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.board", hasSize(5)))
                    .andExpect(jsonPath("$.board[0].suit").value("HEARTS"))
                    .andExpect(jsonPath("$.board[0].value").value("TEN"));
        }

        @Test
        @DisplayName("Should return hand with winner and pot info")
        void getHandHistory_ReturnsWinnerAndPot() throws Exception {
            when(handHistoryService.getHandHistory(historyId)).thenReturn(Optional.of(testHistory));

            mockMvc.perform(get(BASE_URL + "/{historyId}", historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.winnerName").value("Alice"))
                    .andExpect(jsonPath("$.winningHandDescription").value("Two Pair - Aces and Kings"))
                    .andExpect(jsonPath("$.finalPot").value(200));
        }

        @Test
        @DisplayName("Should return hand with player details")
        void getHandHistory_ReturnsPlayerDetails() throws Exception {
            when(handHistoryService.getHandHistory(historyId)).thenReturn(Optional.of(testHistory));

            mockMvc.perform(get(BASE_URL + "/{historyId}", historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.players", hasSize(2)))
                    .andExpect(jsonPath("$.players[0].playerName").value("Alice"))
                    .andExpect(jsonPath("$.players[0].startingChips").value(1000))
                    .andExpect(jsonPath("$.players[0].holeCard1Value").value("ACE"));
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID format")
        void getHandHistory_InvalidUUID_Returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{historyId}", "not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    
    
    
    @Nested
    @DisplayName("Replay Data Tests - GET /api/history/{id}/replay")
    class ReplayDataTests {

        @Test
        @DisplayName("Should return replay data for existing hand - returns 200")
        void getReplayData_ExistingHand_Returns200() throws Exception {
            ReplayData replayData = new ReplayData(
                    historyId, 1, 10, 20, 0,
                    List.of(
                            new PlayerSnapshot(playerId, "Alice", 1000, 0, "ACE of SPADES", "KING of HEARTS"),
                            new PlayerSnapshot(UUID.randomUUID(), "Bob", 1000, 1, "QUEEN of DIAMONDS", "JACK of CLUBS")
                    ),
                    List.of(
                            new ReplayAction("Alice", "CALL", 20, "PRE_FLOP"),
                            new ReplayAction("Bob", "CHECK", 0, "PRE_FLOP"),
                            new ReplayAction("Alice", "BET", 50, "FLOP"),
                            new ReplayAction("Bob", "FOLD", 0, "FLOP")
                    ),
                    List.of("TEN of HEARTS", "JACK of SPADES", "QUEEN of DIAMONDS", "TWO of CLUBS", "THREE of HEARTS"),
                    "Alice",
                    "Two Pair - Aces and Kings",
                    200
            );
            when(handHistoryService.generateReplayData(historyId)).thenReturn(replayData);

            mockMvc.perform(get(BASE_URL + "/{historyId}/replay", historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(historyId.toString()))
                    .andExpect(jsonPath("$.handNumber").value(1))
                    .andExpect(jsonPath("$.smallBlind").value(10))
                    .andExpect(jsonPath("$.bigBlind").value(20));
        }

        @Test
        @DisplayName("Should return replay with phase transitions")
        void getReplayData_ContainsPhaseTransitions() throws Exception {
            ReplayData replayData = new ReplayData(
                    historyId, 1, 10, 20, 0,
                    List.of(new PlayerSnapshot(playerId, "Alice", 1000, 0, "ACE of SPADES", "KING of HEARTS")),
                    List.of(
                            new ReplayAction("Alice", "CALL", 20, "PRE_FLOP"),
                            new ReplayAction("Alice", "BET", 50, "FLOP"),
                            new ReplayAction("Alice", "CHECK", 0, "TURN")
                    ),
                    List.of("TEN of HEARTS", "JACK of SPADES", "QUEEN of DIAMONDS"),
                    "Alice", "Pair", 100
            );
            when(handHistoryService.generateReplayData(historyId)).thenReturn(replayData);

            mockMvc.perform(get(BASE_URL + "/{historyId}/replay", historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.actions[0].phase").value("PRE_FLOP"))
                    .andExpect(jsonPath("$.actions[1].phase").value("FLOP"))
                    .andExpect(jsonPath("$.actions[2].phase").value("TURN"));
        }

        @Test
        @DisplayName("Should return replay with card reveals")
        void getReplayData_ContainsCardReveals() throws Exception {
            ReplayData replayData = new ReplayData(
                    historyId, 1, 10, 20, 0,
                    List.of(new PlayerSnapshot(playerId, "Alice", 1000, 0, "ACE of SPADES", "KING of HEARTS")),
                    List.of(),
                    List.of("TEN of HEARTS", "JACK of SPADES", "QUEEN of DIAMONDS", "TWO of CLUBS", "THREE of HEARTS"),
                    "Alice", "Pair", 100
            );
            when(handHistoryService.generateReplayData(historyId)).thenReturn(replayData);

            mockMvc.perform(get(BASE_URL + "/{historyId}/replay", historyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.board", hasSize(5)))
                    .andExpect(jsonPath("$.players[0].holeCard1").value("ACE of SPADES"))
                    .andExpect(jsonPath("$.players[0].holeCard2").value("KING of HEARTS"));
        }

        @Test
        @DisplayName("Should return 404 for non-existing hand replay")
        void getReplayData_NonExistingHand_Returns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(handHistoryService.generateReplayData(nonExistentId)).thenReturn(null);

            mockMvc.perform(get(BASE_URL + "/{historyId}/replay", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    
    
    
    @Nested
    @DisplayName("Game History Tests - GET /api/history/game/{gameId}")
    class GameHistoryTests {

        @Test
        @DisplayName("Should return all hands for game - returns 200")
        void getGameHistory_ExistingGame_ReturnsAllHands() throws Exception {
            when(handHistoryService.getGameHistory(gameId)).thenReturn(historyList);

            mockMvc.perform(get(BASE_URL + "/game/{gameId}", gameId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(5)));

            verify(handHistoryService).getGameHistory(gameId);
        }

        @Test
        @DisplayName("Should return empty list for game with no hands")
        void getGameHistory_NoHands_ReturnsEmptyList() throws Exception {
            UUID emptyGameId = UUID.randomUUID();
            when(handHistoryService.getGameHistory(emptyGameId)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/game/{gameId}", emptyGameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should support pagination - returns paged results")
        void getGameHistoryPaged_ReturnsPagedResults() throws Exception {
            Page<HandHistory> pagedHistory = new PageImpl<>(historyList, PageRequest.of(0, 20), 5);
            when(handHistoryService.getGameHistory(eq(gameId), eq(0), eq(20))).thenReturn(pagedHistory);

            mockMvc.perform(get(BASE_URL + "/game/{gameId}/paged", gameId)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("Should return hand count for game")
        void getHandCount_ReturnsCount() throws Exception {
            when(handHistoryService.getHandCount(gameId)).thenReturn(5L);

            mockMvc.perform(get(BASE_URL + "/game/{gameId}/count", gameId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }
    }

    
    
    
    @Nested
    @DisplayName("Recent Hands Tests - GET /api/history/recent")
    class RecentHandsTests {

        @Test
        @DisplayName("Should return recent hands - returns 200")
        void getRecentHands_ReturnsRecent() throws Exception {
            when(handHistoryService.getRecentHands()).thenReturn(historyList);

            mockMvc.perform(get(BASE_URL + "/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));

            verify(handHistoryService).getRecentHands();
        }

        @Test
        @DisplayName("Should return hands sorted by timestamp descending")
        void getRecentHands_SortedByTimestamp() throws Exception {
            when(handHistoryService.getRecentHands()).thenReturn(historyList);

            mockMvc.perform(get(BASE_URL + "/recent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].handNumber").value(1))
                    .andExpect(jsonPath("$[4].handNumber").value(5));
        }

        @Test
        @DisplayName("Should filter hands by player - returns player's hands")
        void getPlayerHistory_ReturnsFilteredHands() throws Exception {
            when(handHistoryService.getPlayerHistory(playerId)).thenReturn(historyList);

            mockMvc.perform(get(BASE_URL + "/player/{playerId}", playerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Should return player wins")
        void getPlayerWins_ReturnsWins() throws Exception {
            when(handHistoryService.getPlayerWins("Alice")).thenReturn(historyList.subList(0, 2));

            mockMvc.perform(get(BASE_URL + "/wins/{playerName}", "Alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should return biggest pots")
        void getBiggestPots_ReturnsBiggestPots() throws Exception {
            when(handHistoryService.getBiggestPots()).thenReturn(historyList);

            mockMvc.perform(get(BASE_URL + "/biggest-pots"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }
    }

    
    
    
    @Nested
    @DisplayName("Delete Tests - DELETE /api/history/game/{gameId}")
    class DeleteTests {

        @Test
        @DisplayName("Should delete game history - returns 204")
        void deleteGameHistory_ReturnsNoContent() throws Exception {
            doNothing().when(handHistoryService).deleteGameHistory(gameId);

            mockMvc.perform(delete(BASE_URL + "/game/{gameId}", gameId))
                    .andExpect(status().isNoContent());

            verify(handHistoryService).deleteGameHistory(gameId);
        }
    }

    
    
    
    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Should return JSON content type for hand history")
        void handHistory_ShouldReturnJson() throws Exception {
            when(handHistoryService.getHandHistory(historyId)).thenReturn(Optional.of(testHistory));

            mockMvc.perform(get(BASE_URL + "/{historyId}", historyId))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return JSON content type for replay data")
        void replayData_ShouldReturnJson() throws Exception {
            ReplayData replayData = new ReplayData(
                    historyId, 1, 10, 20, 0,
                    List.of(), List.of(), List.of(), "Alice", "Pair", 100
            );
            when(handHistoryService.generateReplayData(historyId)).thenReturn(replayData);

            mockMvc.perform(get(BASE_URL + "/{historyId}/replay", historyId))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
