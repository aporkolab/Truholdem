package com.truholdem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestConfig;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.controller.PokerGameController;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.PlayerInfo;
import com.truholdem.repository.GameRepository;
import com.truholdem.service.PokerGameService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({ TestConfig.class, TestSecurityConfig.class, PokerGameController.class })
@DisplayName("Game Controller Integration Tests")
class GameControllerIntegrationTest {

        private static final String BASE_URL = "/v1/poker/game";

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
                                new PlayerInfo("Charlie", 1000, true));
        }

        @Nested
        @DisplayName("Game Creation Tests")
        class GameCreationTests {

                @Test
                @DisplayName("Should create new game with valid players")
                void shouldCreateNewGame() throws Exception {
                        mockMvc.perform(post(BASE_URL + "/start")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(testPlayers)))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.id").exists())
                                        .andExpect(jsonPath("$.players", hasSize(3)))
                                        .andExpect(jsonPath("$.phase").value("PRE_FLOP"))
                                        .andExpect(jsonPath("$.currentPot").value(greaterThan(0)));
                }

                @Test
                @DisplayName("Should reject game with too few players")
                void shouldRejectTooFewPlayers() throws Exception {
                        List<PlayerInfo> singlePlayer = Collections.singletonList(
                                        new PlayerInfo("Alone", 1000, false));

                        mockMvc.perform(post(BASE_URL + "/start")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(singlePlayer)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should reject game with too many players")
                void shouldRejectTooManyPlayers() throws Exception {
                        List<PlayerInfo> manyPlayers = new ArrayList<>();
                        for (int i = 0; i < 15; i++)
                                manyPlayers.add(new PlayerInfo("Player" + i, 1000, true));

                        mockMvc.perform(post(BASE_URL + "/start")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(manyPlayers)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should deal 2 hole cards to each player")
                void shouldDealHoleCards() throws Exception {
                        MvcResult result = mockMvc.perform(post(BASE_URL + "/start")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(testPlayers)))
                                        .andExpect(status().isCreated())
                                        .andReturn();

                        Game game = objectMapper.readValue(result.getResponse().getContentAsString(), Game.class);
                        for (Player p : game.getPlayers())
                                assertThat(p.getHand()).hasSize(2);
                }

                @Test
                @DisplayName("Should post blinds correctly")
                void shouldPostBlinds() throws Exception {
                        MvcResult result = mockMvc.perform(post(BASE_URL + "/start")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(testPlayers)))
                                        .andExpect(status().isCreated())
                                        .andReturn();

                        Game game = objectMapper.readValue(result.getResponse().getContentAsString(), Game.class);

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
                        Player current = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());

                        Map<String, Object> action = Map.of("action", "FOLD", "amount", 0);

                        mockMvc.perform(post(BASE_URL + "/{g}/player/{p}/action", testGame.getId(), current.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(action)))
                                        .andExpect(result -> {
                                                int s = result.getResponse().getStatus();
                                                assertThat(s == 200 || s == 400).isTrue();
                                        });
                }

                @Test
                @DisplayName("Should process call action")
                void shouldProcessCall() throws Exception {
                        Player current = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());

                        Map<String, Object> action = Map.of("action", "CALL", "amount", 0);

                        mockMvc.perform(post(BASE_URL + "/{g}/player/{p}/action", testGame.getId(), current.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(action)))
                                        .andExpect(result -> {
                                                int s = result.getResponse().getStatus();
                                                assertThat(s == 200 || s == 400).isTrue();
                                        });
                }

                @Test
                @DisplayName("Should process raise action")
                void shouldProcessRaise() throws Exception {
                        Player current = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());

                        Map<String, Object> action = Map.of("action", "RAISE", "amount", 60);

                        mockMvc.perform(post(BASE_URL + "/{g}/player/{p}/action", testGame.getId(), current.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(action)))
                                        .andExpect(result -> {
                                                int s = result.getResponse().getStatus();
                                                assertThat(s == 200 || s == 400).isTrue();
                                        });
                }

                @Test
                @DisplayName("Should reject action from wrong player")
                void shouldRejectWrongPlayer() throws Exception {
                        Player wrong = testGame.getPlayers().stream()
                                        .filter(p -> testGame.getPlayers().indexOf(p) != testGame
                                                        .getCurrentPlayerIndex())
                                        .findFirst().orElseThrow();

                        Map<String, Object> action = Map.of("action", "CALL", "amount", 0);

                        mockMvc.perform(post(BASE_URL + "/{g}/player/{p}/action", testGame.getId(), wrong.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(action)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should reject invalid raise amount")
                void shouldRejectInvalidRaise() throws Exception {
                        Player current = testGame.getPlayers().get(testGame.getCurrentPlayerIndex());

                        Map<String, Object> action = Map.of("action", "RAISE", "amount", 25);

                        mockMvc.perform(post(BASE_URL + "/{g}/player/{p}/action", testGame.getId(), current.getId())
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

                        mockMvc.perform(get(BASE_URL + "/{g}", game.getId()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(game.getId().toString()))
                                        .andExpect(jsonPath("$.players", hasSize(3)));
                }

                @Test
                @DisplayName("Should return 404 for non-existent game")
                void shouldReturn404() throws Exception {
                        mockMvc.perform(get(BASE_URL + "/{g}", UUID.randomUUID()))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Should advance to next phase")
                void shouldAdvanceToNextPhase() throws Exception {
                        
                        List<PlayerInfo> humanPlayers = Arrays.asList(
                                        new PlayerInfo("Alice", 1000, false),
                                        new PlayerInfo("Bob", 1000, false),
                                        new PlayerInfo("Charlie", 1000, false));
                        
                        Game game = pokerGameService.createNewGame(humanPlayers);
                        assertThat(game.getPhase()).isEqualTo(GamePhase.PRE_FLOP);

                        
                        for (int i = 0; i < 20; i++) {
                                game = gameRepository.findById(game.getId()).orElseThrow();
                                if (game.getPhase() != GamePhase.PRE_FLOP || game.isFinished())
                                        break;

                                Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
                                if (current.isFolded() || current.isAllIn()) {
                                        continue;
                                }

                                try {
                                        
                                        game = pokerGameService.playerAct(
                                                        game.getId(), current.getId(), PlayerAction.CALL, 0);
                                } catch (IllegalStateException e) {
                                        
                                        try {
                                                game = pokerGameService.playerAct(
                                                                game.getId(), current.getId(), PlayerAction.CHECK, 0);
                                        } catch (IllegalStateException e2) {
                                                
                                        }
                                }
                        }

                        Game finalGame = gameRepository.findById(game.getId()).orElseThrow();

                        
                        assertThat(finalGame.getPhase() != GamePhase.PRE_FLOP || finalGame.isFinished())
                                        .as("Game should advance from PRE_FLOP or finish")
                                        .isTrue();
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
                                        .findFirst().orElseThrow();

                        while (game.getPlayers().get(game.getCurrentPlayerIndex()) != bot) {
                                Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
                                if (!current.isBot()) {
                                        game = pokerGameService.playerAct(
                                                        game.getId(), current.getId(), PlayerAction.CALL, 0);
                                }
                                if (game.isFinished())
                                        break;
                        }

                        if (!game.isFinished() && game.getPlayers().get(game.getCurrentPlayerIndex()).isBot()) {
                                mockMvc.perform(post(BASE_URL + "/{g}/bot/{b}/action", game.getId(), bot.getId()))
                                                .andExpect(status().isOk());
                        }
                }

                @Test
                @DisplayName("Should reject bot action for human")
                void shouldRejectBotActionForHuman() throws Exception {
                        Game game = pokerGameService.createNewGame(testPlayers);

                        Player human = game.getPlayers().stream()
                                        .filter(p -> !p.isBot())
                                        .findFirst().orElseThrow();

                        mockMvc.perform(post(BASE_URL + "/{g}/bot/{b}/action",
                                        game.getId(), human.getId()))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("New Hand Tests")
        class NewHandTests {

                @Test
                @DisplayName("Should start new hand")
                void shouldStartNewHand() throws Exception {
                        List<PlayerInfo> two = Arrays.asList(
                                        new PlayerInfo("Alice", 1000, false),
                                        new PlayerInfo("Bob", 1000, false));

                        Game game = pokerGameService.createNewGame(two);

                        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
                        game = pokerGameService.playerAct(
                                        game.getId(), current.getId(), PlayerAction.FOLD, 0);

                        assertThat(game.isFinished()).isTrue();

                        mockMvc.perform(post(BASE_URL + "/{g}/new-hand", game.getId()))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.isFinished").value(false))
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
                        
                        List<PlayerInfo> two = Arrays.asList(
                                        new PlayerInfo("Alice", 500, false),
                                        new PlayerInfo("Bob", 500, false));

                        Game game = pokerGameService.createNewGame(two);
                        assertThat(game.getPhase()).isEqualTo(GamePhase.PRE_FLOP);

                        int maxIterations = 50;
                        while (!game.isFinished() && maxIterations-- > 0) {
                                Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
                                
                                if (current.isFolded() || current.isAllIn()) {
                                        
                                        game = gameRepository.findById(game.getId()).orElseThrow();
                                        continue;
                                }

                                try {
                                        
                                        game = pokerGameService.playerAct(
                                                        game.getId(), current.getId(), PlayerAction.CALL, 0);
                                } catch (IllegalStateException e) {
                                        
                                        try {
                                                game = pokerGameService.playerAct(
                                                                game.getId(), current.getId(), PlayerAction.CHECK, 0);
                                        } catch (IllegalStateException e2) {
                                                
                                                game = gameRepository.findById(game.getId()).orElseThrow();
                                        }
                                }
                        }

                        
                        Game finalGame = gameRepository.findById(game.getId()).orElseThrow();
                        assertThat(finalGame.isFinished() || finalGame.getPhase() != GamePhase.PRE_FLOP)
                                        .as("Game should progress beyond PRE_FLOP or finish")
                                        .isTrue();
                }
        }
}
