package com.truholdem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.dto.PlayerActionRequest;
import com.truholdem.model.*;
import com.truholdem.service.PokerGameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PokerGameController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("PokerGameController Integration Tests")
class PokerGameControllerIT {

    private static final String BASE_URL = "/v1/poker/game";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PokerGameService pokerGameService;

    private Game testGame;
    private List<PlayerInfo> validPlayers;
    private UUID gameId;
    private UUID playerId;
    private UUID botId;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        botId = UUID.randomUUID();

        validPlayers = Arrays.asList(
                new PlayerInfo("Alice", 1000, false),
                new PlayerInfo("Bob", 1000, true),
                new PlayerInfo("Charlie", 1000, true)
        );

        testGame = createTestGame();
    }

    private Game createTestGame() {
        Game game = new Game();
        game.setId(gameId);
        game.setPhase(GamePhase.PRE_FLOP);
        game.setCurrentPot(30);
        game.setCurrentBet(20);
        game.setFinished(false);
        game.setHandNumber(1);
        game.setCurrentPlayerIndex(0);

        List<Player> players = new ArrayList<>();

        Player humanPlayer = new Player();
        humanPlayer.setId(playerId);
        humanPlayer.setName("Alice");
        humanPlayer.setChips(980);
        humanPlayer.setBot(false);
        humanPlayer.setFolded(false);
        humanPlayer.setAllIn(false);
        humanPlayer.setHand(Arrays.asList(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.KING)
        ));
        players.add(humanPlayer);

        Player botPlayer = new Player();
        botPlayer.setId(botId);
        botPlayer.setName("Bob");
        botPlayer.setChips(990);
        botPlayer.setBot(true);
        botPlayer.setFolded(false);
        botPlayer.setAllIn(false);
        botPlayer.setHand(Arrays.asList(
                new Card(Suit.DIAMONDS, Value.QUEEN),
                new Card(Suit.CLUBS, Value.JACK)
        ));
        players.add(botPlayer);

        game.setPlayers(players);
        game.setCommunityCards(new ArrayList<>());

        return game;
    }

    
    
    
    @Nested
    @DisplayName("Game Creation Tests - POST /v1/poker/game/start")
    class GameCreationTests {

        @Test
        @DisplayName("Should create game with valid players - returns 200")
        void createGame_WithValidPlayers_Returns200() throws Exception {
            when(pokerGameService.createNewGame(any())).thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPlayers)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(gameId.toString()))
                    .andExpect(jsonPath("$.players", hasSize(2)))
                    .andExpect(jsonPath("$.phase").value("PRE_FLOP"))
                    .andExpect(jsonPath("$.currentPot").value(30))
                    .andExpect(jsonPath("$.finished").value(false));

            verify(pokerGameService).createNewGame(any());
        }

        @Test
        @DisplayName("Should create game with minimum 2 players - returns 200")
        void createGame_WithMinimumPlayers_Returns200() throws Exception {
            List<PlayerInfo> twoPlayers = Arrays.asList(
                    new PlayerInfo("Alice", 1000, false),
                    new PlayerInfo("Bob", 1000, false)
            );
            when(pokerGameService.createNewGame(any())).thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(twoPlayers)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("Should reject game with single player - returns 400")
        void createGame_WithSinglePlayer_Returns400() throws Exception {
            List<PlayerInfo> singlePlayer = Collections.singletonList(
                    new PlayerInfo("Lonely", 1000, false)
            );
            when(pokerGameService.createNewGame(any()))
                    .thenThrow(new IllegalArgumentException("Minimum 2 players required"));

            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(singlePlayer)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject game with 11 players - returns 400")
        void createGame_WithElevenPlayers_Returns400() throws Exception {
            List<PlayerInfo> tooManyPlayers = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                tooManyPlayers.add(new PlayerInfo("Player" + i, 1000, true));
            }
            when(pokerGameService.createNewGame(any()))
                    .thenThrow(new IllegalArgumentException("Maximum 10 players allowed"));

            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tooManyPlayers)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject empty player list - returns 400")
        void createGame_WithEmptyPlayerList_Returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject malformed JSON - returns 400")
        void createGame_WithMalformedJson_Returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject player with blank name - returns 400")
        void createGame_WithBlankPlayerName_Returns400() throws Exception {
            List<PlayerInfo> invalidPlayers = Arrays.asList(
                    new PlayerInfo("", 1000, false),
                    new PlayerInfo("Bob", 1000, false)
            );

            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidPlayers)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject player with zero chips - returns 400")
        void createGame_WithZeroChips_Returns400() throws Exception {
            List<PlayerInfo> invalidPlayers = Arrays.asList(
                    new PlayerInfo("Alice", 0, false),
                    new PlayerInfo("Bob", 1000, false)
            );

            mockMvc.perform(post(BASE_URL + "/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidPlayers)))
                    .andExpect(status().isBadRequest());
        }
    }

    
    
    
    @Nested
    @DisplayName("Game State Tests - GET /v1/poker/game/{gameId}")
    class GameStateTests {

        @Test
        @DisplayName("Should return existing game - returns 200")
        void getGameStatus_ExistingGame_Returns200() throws Exception {
            when(pokerGameService.getGame(gameId)).thenReturn(Optional.of(testGame));

            mockMvc.perform(get(BASE_URL + "/{gameId}", gameId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(gameId.toString()))
                    .andExpect(jsonPath("$.phase").value("PRE_FLOP"))
                    .andExpect(jsonPath("$.players").isArray())
                    .andExpect(jsonPath("$.currentPot").value(30));
        }

        @Test
        @DisplayName("Should return 404 for non-existing game")
        void getGameStatus_NonExistingGame_Returns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(pokerGameService.getGame(nonExistentId)).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_URL + "/{gameId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID format")
        void getGameStatus_InvalidUUID_Returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{gameId}", "not-a-valid-uuid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return game with all player data")
        void getGameStatus_ReturnsFullPlayerData() throws Exception {
            when(pokerGameService.getGame(gameId)).thenReturn(Optional.of(testGame));

            mockMvc.perform(get(BASE_URL + "/{gameId}", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.players[0].name").value("Alice"))
                    .andExpect(jsonPath("$.players[0].chips").value(980))
                    .andExpect(jsonPath("$.players[0].bot").value(false))
                    .andExpect(jsonPath("$.players[1].name").value("Bob"))
                    .andExpect(jsonPath("$.players[1].bot").value(true));
        }

        @Test
        @DisplayName("Should return game with community cards")
        void getGameStatus_ReturnsCommunitCards() throws Exception {
            testGame.setCommunityCards(Arrays.asList(
                    new Card(Suit.HEARTS, Value.TEN),
                    new Card(Suit.SPADES, Value.JACK),
                    new Card(Suit.DIAMONDS, Value.QUEEN)
            ));
            testGame.setPhase(GamePhase.FLOP);
            when(pokerGameService.getGame(gameId)).thenReturn(Optional.of(testGame));

            mockMvc.perform(get(BASE_URL + "/{gameId}", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.communityCards", hasSize(3)))
                    .andExpect(jsonPath("$.phase").value("FLOP"));
        }

        @Test
        @DisplayName("Should handle finished game state")
        void getGameStatus_FinishedGame_ReturnsCorrectState() throws Exception {
            testGame.setFinished(true);
            testGame.setPhase(GamePhase.SHOWDOWN);
            when(pokerGameService.getGame(gameId)).thenReturn(Optional.of(testGame));

            mockMvc.perform(get(BASE_URL + "/{gameId}", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.finished").value(true))
                    .andExpect(jsonPath("$.phase").value("SHOWDOWN"));
        }
    }

    
    
    
    @Nested
    @DisplayName("Player Action Tests - POST /v1/poker/game/{gameId}/player/{playerId}/action")
    class PlayerActionTests {

        @Test
        @DisplayName("Should process FOLD action - returns 200")
        void playerAction_Fold_Returns200() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.FOLD);
            request.setAmount(0);

            testGame.getPlayers().get(0).setFolded(true);
            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.FOLD), eq(0)))
                    .thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.players[0].folded").value(true));
        }

        @Test
        @DisplayName("Should process CHECK action - returns 200")
        void playerAction_Check_Returns200() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.CHECK);
            request.setAmount(0);

            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.CHECK), eq(0)))
                    .thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should process CALL action - returns 200")
        void playerAction_Call_Returns200() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.CALL);
            request.setAmount(0);

            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.CALL), eq(0)))
                    .thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should process BET action with amount - returns 200")
        void playerAction_BetWithAmount_Returns200() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.BET);
            request.setAmount(50);

            testGame.setCurrentBet(50);
            testGame.setCurrentPot(80);
            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.BET), eq(50)))
                    .thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentBet").value(50))
                    .andExpect(jsonPath("$.currentPot").value(80));
        }

        @Test
        @DisplayName("Should process RAISE action with amount - returns 200")
        void playerAction_RaiseWithAmount_Returns200() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.RAISE);
            request.setAmount(100);

            testGame.setCurrentBet(100);
            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.RAISE), eq(100)))
                    .thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentBet").value(100));
        }

        @Test
        @DisplayName("Should reject CHECK when facing bet - returns 400")
        void playerAction_CheckFacingBet_Returns400() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.CHECK);
            request.setAmount(0);

            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.CHECK), eq(0)))
                    .thenThrow(new IllegalArgumentException("Cannot check when facing a bet"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject action when not player's turn - returns 409")
        void playerAction_NotPlayersTurn_Returns409() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.FOLD);
            request.setAmount(0);

            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.FOLD), eq(0)))
                    .thenThrow(new IllegalStateException("It's not this player's turn"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 404 for non-existing game")
        void playerAction_GameNotFound_Returns404() throws Exception {
            UUID nonExistentGameId = UUID.randomUUID();
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.FOLD);
            request.setAmount(0);

            when(pokerGameService.playerAct(eq(nonExistentGameId), eq(playerId), any(), anyInt()))
                    .thenThrow(new NoSuchElementException("Game not found"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", nonExistentGameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 for non-existing player")
        void playerAction_PlayerNotFound_Returns404() throws Exception {
            UUID nonExistentPlayerId = UUID.randomUUID();
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(nonExistentPlayerId.toString());
            request.setAction(PlayerAction.FOLD);
            request.setAmount(0);

            when(pokerGameService.playerAct(eq(gameId), eq(nonExistentPlayerId), any(), anyInt()))
                    .thenThrow(new NoSuchElementException("Player not found"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, nonExistentPlayerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject negative bet amount - returns 400")
        void playerAction_NegativeAmount_Returns400() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.BET);
            request.setAmount(-50);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject missing action - returns 400")
        void playerAction_MissingAction_Returns400() throws Exception {
            String requestWithoutAction = """
                    {
                        "playerId": "%s",
                        "amount": 0
                    }
                    """.formatted(playerId);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestWithoutAction))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject bet exceeding player chips - returns 400")
        void playerAction_BetExceedsChips_Returns400() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.BET);
            request.setAmount(5000);

            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.BET), eq(5000)))
                    .thenThrow(new IllegalArgumentException("Bet amount exceeds available chips"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle all-in scenario")
        void playerAction_AllIn_Returns200() throws Exception {
            PlayerActionRequest request = new PlayerActionRequest();
            request.setPlayerId(playerId.toString());
            request.setAction(PlayerAction.BET);
            request.setAmount(980);

            testGame.getPlayers().get(0).setAllIn(true);
            testGame.getPlayers().get(0).setChips(0);
            when(pokerGameService.playerAct(eq(gameId), eq(playerId), eq(PlayerAction.BET), eq(980)))
                    .thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/player/{playerId}/action", gameId, playerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.players[0].allIn").value(true));
        }
    }

    
    
    
    @Nested
    @DisplayName("Bot Action Tests - POST /v1/poker/game/{gameId}/bot/{botId}/action")
    class BotActionTests {

        @Test
        @DisplayName("Should execute bot action - returns 200")
        void executeBotAction_ValidBot_Returns200() throws Exception {
            when(pokerGameService.executeBotAction(gameId, botId)).thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/bot/{botId}/action", gameId, botId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(gameId.toString()));

            verify(pokerGameService).executeBotAction(gameId, botId);
        }

        @Test
        @DisplayName("Should reject bot action for human player - returns 400")
        void executeBotAction_HumanPlayer_Returns400() throws Exception {
            when(pokerGameService.executeBotAction(gameId, playerId))
                    .thenThrow(new IllegalArgumentException("Player is not a bot"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/bot/{botId}/action", gameId, playerId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for non-existing bot")
        void executeBotAction_BotNotFound_Returns400() throws Exception {
            UUID nonExistentBotId = UUID.randomUUID();
            when(pokerGameService.executeBotAction(gameId, nonExistentBotId))
                    .thenThrow(new IllegalArgumentException("Bot not found"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/bot/{botId}/action", gameId, nonExistentBotId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for non-existing game")
        void executeBotAction_GameNotFound_Returns400() throws Exception {
            UUID nonExistentGameId = UUID.randomUUID();
            when(pokerGameService.executeBotAction(nonExistentGameId, botId))
                    .thenThrow(new IllegalStateException("Game not found"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/bot/{botId}/action", nonExistentGameId, botId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject bot action when not bot's turn - returns 400")
        void executeBotAction_NotBotsTurn_Returns400() throws Exception {
            when(pokerGameService.executeBotAction(gameId, botId))
                    .thenThrow(new IllegalStateException("It's not this bot's turn"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/bot/{botId}/action", gameId, botId))
                    .andExpect(status().isBadRequest());
        }
    }

    
    
    
    @Nested
    @DisplayName("New Hand Tests - POST /v1/poker/game/{gameId}/new-hand")
    class NewHandTests {

        @Test
        @DisplayName("Should start new hand - returns 200")
        void startNewHand_ValidRequest_Returns200() throws Exception {
            testGame.setHandNumber(2);
            testGame.setFinished(false);
            testGame.setPhase(GamePhase.PRE_FLOP);
            when(pokerGameService.startNewHand(gameId)).thenReturn(testGame);

            mockMvc.perform(post(BASE_URL + "/{gameId}/new-hand", gameId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.handNumber").value(2))
                    .andExpect(jsonPath("$.finished").value(false))
                    .andExpect(jsonPath("$.phase").value("PRE_FLOP"));
        }

        @Test
        @DisplayName("Should handle not enough players error")
        void startNewHand_NotEnoughPlayers_Returns400() throws Exception {
            when(pokerGameService.startNewHand(gameId))
                    .thenThrow(new IllegalArgumentException("Not enough players with chips to continue"));

            mockMvc.perform(post(BASE_URL + "/{gameId}/new-hand", gameId))
                    .andExpect(status().isBadRequest());
        }
    }
}
