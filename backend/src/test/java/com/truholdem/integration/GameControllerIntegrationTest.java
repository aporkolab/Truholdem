package com.truholdem.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestConfig;
import com.truholdem.model.*;
import com.truholdem.repository.GameRepository;
import com.truholdem.repository.HandHistoryRepository;
import com.truholdem.repository.PlayerStatisticsRepository;
import com.truholdem.service.PokerGameService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
@DisplayName("Game Controller Integration Tests")
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PokerGameService pokerGameService;

    private List<PlayerInfo> testPlayers;

    @BeforeEach
    void setUp() {
        testPlayers = Arrays.asList(
            new PlayerInfo("Alice", 1000, false),
            new PlayerInfo("Bob", 1000, true),
            new PlayerInfo("Charlie", 1000, true)
        );
    }

    @Nested
    @DisplayName("Game Creation Tests")
    class GameCreationTests {

        @Test
        @DisplayName("Should create new game with valid players")
        void shouldCreateNewGame() throws Exception {
            mockMvc.perform(post("/api/game/new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testPlayers)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.players", hasSize(3)))
                .andExpect(jsonPath("$.phase").value("PRE_FLOP"))
                .andExpect(jsonPath("$.currentPot").value(greaterThan(0)));
        }

        @Test
        @DisplayName("Should reject game with too few players")
        void shouldRejectTooFewPlayers() throws Exception {
            List<PlayerInfo> singlePlayer = Collections.singletonList(
                new PlayerInfo("Alone", 1000, false)
            );

            mockMvc.perform(post("/api/game/new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(singlePlayer)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject game with too many players")
        void shouldRejectTooManyPlayers() throws Exception {
            List<PlayerInfo> manyPlayers = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                manyPlayers.add(new PlayerInfo("Player" + i, 1000, true));
            }

            mockMvc.perform(post("/api/game/new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(manyPlayers)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should deal 2 hole cards to each player")
        void shouldDealHoleCards() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/game/new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testPlayers)))
                .andExpect(status().isOk())
                .andReturn();

            Game game = objectMapper.readValue(
                result.getResponse().getContentAsString(), Game.class
            );

            for (Player player : game.getPlayers()) {
                assertThat(player.getHand()).hasSize(2);
            }
        }

        @Test
        @DisplayName("Should post blinds correctly")
        void shouldPostBlinds() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/game/new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testPlayers)))
                .andExpect(status().isOk())
                .andReturn();

            Game game = objectMapper.readValue(
                result.getResponse().getContentAsString(), Game.class
            );

            
            assertThat(game.getCurrentPot()).isEqualTo(30);
            assertThat(game.getCurrentBet()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Player Action Tests")
    class PlayerActionTests {

        private Game testGame;

        @BeforeEach
        void createGame() {
            testGame = pokerGameService.createNewGame(testPlayers);
        }

        @Test
        @DisplayName("Should process fold action")
        void shouldProcessFold() throws Exception {
            Player currentPlayer = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());

            Map<String, Object> action = Map.of(
                "action", "FOLD",
                "amount", 0
            );

            mockMvc.perform(post("/api/game/{gameId}/player/{playerId}/act", 
                        testGame.getId(), currentPlayer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players[?(@.id == '" + currentPlayer.getId() + "')].folded").value(hasItem(true)));
        }

        @Test
        @DisplayName("Should process call action")
        void shouldProcessCall() throws Exception {
            Player currentPlayer = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());
            int initialPot = testGame.getCurrentPot();
            int callAmount = testGame.getCurrentBet() - currentPlayer.getBetAmount();

            Map<String, Object> action = Map.of(
                "action", "CALL",
                "amount", 0
            );

            mockMvc.perform(post("/api/game/{gameId}/player/{playerId}/act",
                        testGame.getId(), currentPlayer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPot").value(initialPot + callAmount));
        }

        @Test
        @DisplayName("Should process raise action")
        void shouldProcessRaise() throws Exception {
            Player currentPlayer = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());

            Map<String, Object> action = Map.of(
                "action", "RAISE",
                "amount", 60
            );

            mockMvc.perform(post("/api/game/{gameId}/player/{playerId}/act",
                        testGame.getId(), currentPlayer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBet").value(60));
        }

        @Test
        @DisplayName("Should reject action from wrong player")
        void shouldRejectWrongPlayer() throws Exception {
            
            Player wrongPlayer = testGame.getPlayers().stream()
                .filter(p -> testGame.getPlayers().indexOf(p) != testGame.getCurrentPlayerIndex())
                .findFirst()
                .orElseThrow();

            Map<String, Object> action = Map.of(
                "action", "CALL",
                "amount", 0
            );

            mockMvc.perform(post("/api/game/{gameId}/player/{playerId}/act",
                        testGame.getId(), wrongPlayer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject invalid raise amount")
        void shouldRejectInvalidRaise() throws Exception {
            Player currentPlayer = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());

            Map<String, Object> action = Map.of(
                "action", "RAISE",
                "amount", 25 
            );

            mockMvc.perform(post("/api/game/{gameId}/player/{playerId}/act",
                        testGame.getId(), currentPlayer.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Game State Tests")
    class GameStateTests {

        @Test
        @DisplayName("Should retrieve game by ID")
        void shouldRetrieveGameById() throws Exception {
            Game game = pokerGameService.createNewGame(testPlayers);

            mockMvc.perform(get("/api/game/{gameId}", game.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(game.getId().toString()))
                .andExpect(jsonPath("$.players", hasSize(3)));
        }

        @Test
        @DisplayName("Should return 404 for non-existent game")
        void shouldReturn404ForNonExistentGame() throws Exception {
            mockMvc.perform(get("/api/game/{gameId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should advance to next phase")
        void shouldAdvanceToNextPhase() throws Exception {
            Game game = pokerGameService.createNewGame(testPlayers);

            
            
            for (int i = 0; i < 10; i++) {
                game = gameRepository.findById(game.getId()).orElseThrow();
                if (game.getPhase() != GamePhase.PRE_FLOP) break;
                
                Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
                if (!current.isFolded() && !current.isAllIn()) {
                    Map<String, Object> action = Map.of(
                        "action", "CALL",
                        "amount", 0
                    );

                    mockMvc.perform(post("/api/game/{gameId}/player/{playerId}/act",
                                game.getId(), current.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(action)))
                        .andExpect(status().isOk());
                }
            }

            Game finalGame = gameRepository.findById(game.getId()).orElseThrow();
            
            assertThat(finalGame.getPhase()).isIn(
                GamePhase.FLOP, GamePhase.TURN, GamePhase.RIVER, GamePhase.SHOWDOWN
            );
        }
    }

    @Nested
    @DisplayName("Bot Action Tests")
    class BotActionTests {

        @Test
        @DisplayName("Should execute bot action")
        void shouldExecuteBotAction() throws Exception {
            Game game = pokerGameService.createNewGame(testPlayers);

            
            Player bot = game.getPlayers().stream()
                .filter(Player::isBot)
                .findFirst()
                .orElseThrow();

            
            while (game.getPlayers().get(game.getCurrentPlayerIndex()) != bot) {
                Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
                if (!current.isBot()) {
                    game = pokerGameService.playerAct(
                        game.getId(), current.getId(), PlayerAction.CALL, 0
                    );
                }
                if (game.isFinished()) break;
            }

            if (!game.isFinished() && game.getPlayers().get(game.getCurrentPlayerIndex()).isBot()) {
                mockMvc.perform(post("/api/game/{gameId}/bot/{botId}/act",
                            game.getId(), bot.getId()))
                    .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should reject bot action for human player")
        void shouldRejectBotActionForHuman() throws Exception {
            Game game = pokerGameService.createNewGame(testPlayers);

            Player human = game.getPlayers().stream()
                .filter(p -> !p.isBot())
                .findFirst()
                .orElseThrow();

            mockMvc.perform(post("/api/game/{gameId}/bot/{botId}/act",
                        game.getId(), human.getId()))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("New Hand Tests")
    class NewHandTests {

        @Test
        @DisplayName("Should start new hand after game ends")
        void shouldStartNewHand() throws Exception {
            
            List<PlayerInfo> twoPlayers = Arrays.asList(
                new PlayerInfo("Alice", 1000, false),
                new PlayerInfo("Bob", 1000, false)
            );

            Game game = pokerGameService.createNewGame(twoPlayers);
            
            
            Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
            game = pokerGameService.playerAct(
                game.getId(), currentPlayer.getId(), PlayerAction.FOLD, 0
            );

            assertThat(game.isFinished()).isTrue();

            
            mockMvc.perform(post("/api/game/{gameId}/new-hand", game.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finished").value(false))
                .andExpect(jsonPath("$.phase").value("PRE_FLOP"))
                .andExpect(jsonPath("$.handNumber").value(2));
        }
    }

    @Nested
    @DisplayName("Full Game Flow Tests")
    class FullGameFlowTests {

        @Test
        @DisplayName("Should complete full game with showdown")
        void shouldCompleteFullGameWithShowdown() throws Exception {
            List<PlayerInfo> twoPlayers = Arrays.asList(
                new PlayerInfo("Alice", 500, false),
                new PlayerInfo("Bob", 500, false)
            );

            
            MvcResult createResult = mockMvc.perform(post("/api/game/new")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(twoPlayers)))
                .andExpect(status().isOk())
                .andReturn();

            Game game = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), Game.class
            );

            
            int maxIterations = 20;
            while (!game.isFinished() && maxIterations-- > 0) {
                Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
                
                Map<String, Object> action = Map.of(
                    "action", "CALL",
                    "amount", 0
                );

                MvcResult actionResult = mockMvc.perform(post("/api/game/{gameId}/player/{playerId}/act",
                            game.getId(), current.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                    .andExpect(status().isOk())
                    .andReturn();

                game = objectMapper.readValue(
                    actionResult.getResponse().getContentAsString(), Game.class
                );
            }

            assertThat(game.isFinished()).isTrue();
            assertThat(game.getWinnerName()).isNotBlank();
        }
    }
}
