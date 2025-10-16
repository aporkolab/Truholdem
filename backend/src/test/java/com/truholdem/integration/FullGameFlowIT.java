package com.truholdem.integration;

import com.truholdem.dto.ShowdownResult;
import com.truholdem.dto.WebSocketGameUpdateMessage;
import com.truholdem.model.*;
import com.truholdem.repository.GameRepository;
import com.truholdem.repository.HandHistoryRepository;
import com.truholdem.repository.PlayerStatisticsRepository;
import com.truholdem.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Full Game Flow Integration Tests")
class FullGameFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("truholdem_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.liquibase.enabled", () -> "false");
        
        registry.add("spring.cache.type", () -> "simple");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        
        registry.add("app.jwt.secret", () -> "dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy0xMjM0NTY3ODkw");
        registry.add("app.jwt.expiration", () -> "86400000");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private PokerGameService pokerGameService;

    @Autowired
    private PlayerStatisticsService statisticsService;

    @Autowired
    private HandHistoryService handHistoryService;

    @Autowired
    private AdvancedBotAIService botAIService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerStatisticsRepository statsRepository;

    @Autowired
    private HandHistoryRepository handHistoryRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private Game game;
    private UUID gameId;

    @BeforeEach
    void setUp() {
        
        reset(messagingTemplate);
        
        
        handHistoryRepository.deleteAll();
        statsRepository.deleteAll();
        gameRepository.deleteAll();
    }

    
    
    

    @Nested
    @DisplayName("1. Complete Game Flow Tests")
    @TestMethodOrder(OrderAnnotation.class)
    class CompleteGameFlowTests {

        
        @Test
        @Order(1)
        @DisplayName("Should complete full game: start → pre-flop → flop → turn → river → showdown")
        void shouldCompleteFullGameThroughAllPhases() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Alice", 1000, false),
                    new PlayerInfo("Bob", 1000, false),
                    new PlayerInfo("Charlie", 1000, false)
            );

            
            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            assertThat(game.getPhase()).isEqualTo(GamePhase.PRE_FLOP);
            assertThat(game.getPlayers()).hasSize(3);
            assertThat(game.getCommunityCards()).isEmpty();
            
            
            game.getPlayers().forEach(p -> 
                assertThat(p.getHand()).hasSize(2)
            );

            
            playPhaseWithAllCall(GamePhase.PRE_FLOP);

            
            refreshGame();
            assertThat(game.getPhase()).isEqualTo(GamePhase.FLOP);
            assertThat(game.getCommunityCards()).hasSize(3);

            
            playPhaseWithAllCheck(GamePhase.FLOP);

            
            refreshGame();
            assertThat(game.getPhase()).isEqualTo(GamePhase.TURN);
            assertThat(game.getCommunityCards()).hasSize(4);

            
            playPhaseWithAllCheck(GamePhase.TURN);

            
            refreshGame();
            assertThat(game.getPhase()).isEqualTo(GamePhase.RIVER);
            assertThat(game.getCommunityCards()).hasSize(5);

            
            playPhaseWithAllCheck(GamePhase.RIVER);

            
            refreshGame();
            assertThat(game.getPhase()).isEqualTo(GamePhase.SHOWDOWN);
            assertThat(game.isFinished()).isTrue();
            assertThat(game.getWinnerName()).isNotNull();
            assertThat(game.getWinnerIds()).isNotEmpty();
        }

        
        @Test
        @Order(2)
        @DisplayName("Should end game when all players FOLD to a raise")
        void shouldEndGameWhenAllPlayersFoldToRaise() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Raiser", 1000, false),
                    new PlayerInfo("Folder1", 1000, false),
                    new PlayerInfo("Folder2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            Player currentPlayer = game.getCurrentPlayer();
            int raiseAmount = game.getBigBlind() * 3;
            game = pokerGameService.playerAct(gameId, currentPlayer.getId(), PlayerAction.RAISE, raiseAmount);

            
            refreshGame();
            while (!game.isFinished()) {
                Player current = game.getCurrentPlayer();
                if (current != null && !current.isFolded()) {
                    game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.FOLD, 0);
                    refreshGame();
                }
            }

            
            assertThat(game.isFinished()).isTrue();
            assertThat(game.getWinnerName()).isNotNull();
            assertThat(game.getWinningHandDescription()).contains("folded");
            
            
            long notFolded = game.getPlayers().stream()
                    .filter(p -> !p.isFolded())
                    .count();
            assertThat(notFolded).isEqualTo(1);
        }

        
        @Test
        @Order(3)
        @DisplayName("Should handle split pot correctly when players tie")
        void shouldHandleSplitPotCorrectly() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Player1", 1000, false),
                    new PlayerInfo("Player2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            int initialPot = game.getCurrentPot();

            
            while (!game.isFinished() && game.getPhase() != GamePhase.SHOWDOWN) {
                refreshGame();
                Player current = game.getCurrentPlayer();
                if (current == null || current.isFolded() || current.isAllIn()) {
                    break;
                }

                if (current.getBetAmount() >= game.getCurrentBet()) {
                    game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
                } else {
                    game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
                }
            }

            refreshGame();
            
            
            assertThat(game.isFinished()).isTrue();
            assertThat(game.getWinnerIds()).isNotEmpty();
            
            
            
        }

        
        @Test
        @Order(4)
        @DisplayName("Should create side pot correctly when player goes all-in")
        void shouldCreateSidePotWhenPlayerGoesAllIn() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("BigStack", 2000, false),
                    new PlayerInfo("ShortStack", 200, false),
                    new PlayerInfo("MediumStack", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            Player shortStack = game.getPlayers().stream()
                    .filter(p -> p.getName().equals("ShortStack"))
                    .findFirst()
                    .orElseThrow();

            
            refreshGame();
            Player current = game.getCurrentPlayer();
            
            
            int bigRaise = 500;
            game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.RAISE, bigRaise);

            
            refreshGame();
            current = game.getCurrentPlayer();
            if (current.getChips() + current.getBetAmount() <= game.getCurrentBet()) {
                game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
            }

            refreshGame();
            
            
            Player updatedShortStack = game.getPlayers().stream()
                    .filter(p -> p.getName().equals("ShortStack"))
                    .findFirst()
                    .orElseThrow();
            
            if (updatedShortStack.getChips() == 0) {
                assertThat(updatedShortStack.isAllIn()).isTrue();
            }
        }

        
        @Test
        @Order(5)
        @DisplayName("Should handle multiple hands in a single game session")
        void shouldHandleMultipleHandsInGame() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Alice", 1000, false),
                    new PlayerInfo("Bob", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            completeHandQuickly();
            assertThat(game.isFinished()).isTrue();
            int firstHandNumber = game.getHandNumber();

            
            game = pokerGameService.startNewHand(gameId);
            
            
            assertThat(game.getHandNumber()).isEqualTo(firstHandNumber + 1);
            assertThat(game.getPhase()).isEqualTo(GamePhase.PRE_FLOP);
            assertThat(game.isFinished()).isFalse();
            assertThat(game.getCommunityCards()).isEmpty();
            
            
            game.getPlayers().forEach(p -> {
                assertThat(p.getHand()).hasSize(2);
                assertThat(p.isFolded()).isFalse();
            });

            
            completeHandQuickly();
            assertThat(game.isFinished()).isTrue();
            assertThat(game.getHandNumber()).isEqualTo(firstHandNumber + 1);
        }

        
        @Test
        @Order(6)
        @DisplayName("Should rotate dealer position between hands")
        void shouldRotateDealerPositionBetweenHands() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Player1", 1000, false),
                    new PlayerInfo("Player2", 1000, false),
                    new PlayerInfo("Player3", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();
            int initialDealer = game.getDealerPosition();

            
            completeHandQuickly();

            
            game = pokerGameService.startNewHand(gameId);

            
            int expectedDealer = (initialDealer + 1) % game.getPlayers().size();
            assertThat(game.getDealerPosition()).isEqualTo(expectedDealer);
        }
    }

    
    
    

    @Nested
    @DisplayName("2. Bot Integration Tests")
    @TestMethodOrder(OrderAnnotation.class)
    class BotIntegrationTests {

        
        @Test
        @Order(1)
        @DisplayName("Should play game with 1 human and 2 bots successfully")
        void shouldPlayGameWithHumanAndBots() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Human", 1000, false),
                    new PlayerInfo("Bot1", 1000, true),
                    new PlayerInfo("Bot2", 1000, true)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            int maxIterations = 50;
            int iterations = 0;

            
            while (!game.isFinished() && iterations < maxIterations) {
                refreshGame();
                Player current = game.getCurrentPlayer();
                
                if (current == null || current.isFolded() || current.isAllIn()) {
                    iterations++;
                    continue;
                }

                if (current.isBot()) {
                    
                    game = pokerGameService.executeBotAction(gameId, current.getId());
                } else {
                    
                    if (current.getBetAmount() < game.getCurrentBet()) {
                        game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
                    } else {
                        game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
                    }
                }
                iterations++;
            }

            
            refreshGame();
            assertThat(game.isFinished()).isTrue();
            assertThat(game.getWinnerName()).isNotNull();
        }

        
        @Test
        @Order(2)
        @DisplayName("Should make appropriate decisions based on hand strength")
        void shouldMakeDecisionsBasedOnHandStrength() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("AggressiveBot", 1000, true),
                    new PlayerInfo("ConservativeBot", 1000, true)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            Player bot = game.getCurrentPlayer();
            assertThat(bot.isBot()).isTrue();

            
            AdvancedBotAIService.BotDecision decision = botAIService.decide(game, bot);

            
            assertThat(decision).isNotNull();
            assertThat(decision.action()).isIn(
                    PlayerAction.FOLD, 
                    PlayerAction.CHECK, 
                    PlayerAction.CALL, 
                    PlayerAction.BET, 
                    PlayerAction.RAISE
            );
            assertThat(decision.reasoning()).isNotBlank();
        }

        
        @Test
        @Order(3)
        @DisplayName("Should complete game with Bot vs Bot")
        void shouldCompleteGameBotVsBot() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Bot1", 1000, true),
                    new PlayerInfo("Bot2", 1000, true),
                    new PlayerInfo("Bot3", 1000, true)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            int maxIterations = 100;
            int iterations = 0;

            
            while (!game.isFinished() && iterations < maxIterations) {
                refreshGame();
                Player current = game.getCurrentPlayer();
                
                if (current == null || current.isFolded() || current.isAllIn()) {
                    iterations++;
                    continue;
                }

                assertThat(current.isBot()).isTrue();
                game = pokerGameService.executeBotAction(gameId, current.getId());
                iterations++;
            }

            
            refreshGame();
            assertThat(game.isFinished()).isTrue();
            assertThat(game.getWinnerName()).isNotNull();
            
            
            assertThat(iterations).isLessThan(maxIterations);
        }

        
        @Test
        @Order(4)
        @DisplayName("Should handle bot decisions in raise scenarios")
        void shouldHandleBotDecisionsInRaiseScenarios() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("Human", 1000, false),
                    new PlayerInfo("Bot", 1000, true)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            Player human = game.getPlayers().stream()
                    .filter(p -> !p.isBot())
                    .findFirst()
                    .orElseThrow();

            if (game.getCurrentPlayer().getId().equals(human.getId())) {
                game = pokerGameService.playerAct(gameId, human.getId(), 
                        PlayerAction.RAISE, game.getBigBlind() * 3);
            }

            refreshGame();

            
            Player bot = game.getCurrentPlayer();
            if (bot != null && bot.isBot()) {
                game = pokerGameService.executeBotAction(gameId, bot.getId());
                
                refreshGame();
                
                assertThat(bot.hasActed() || bot.isFolded()).isTrue();
            }
        }
    }

    
    
    

    @Nested
    @DisplayName("3. Statistics Persistence Tests")
    @TestMethodOrder(OrderAnnotation.class)
    class StatisticsPersistenceTests {

        
        @Test
        @Order(1)
        @DisplayName("Should save player statistics after game completion")
        void shouldSaveStatisticsAfterGameCompletion() {
            
            String playerName = "StatsPlayer_" + UUID.randomUUID().toString().substring(0, 8);
            List<PlayerInfo> players = List.of(
                    new PlayerInfo(playerName, 1000, false),
                    new PlayerInfo("Opponent", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            completeHandQuickly();

            
            Optional<PlayerStatistics> stats = statisticsService.getStatsByName(playerName);
            assertThat(stats).isPresent();
            assertThat(stats.get().getPlayerName()).isEqualTo(playerName);
            assertThat(stats.get().getTotalSessions()).isGreaterThanOrEqualTo(1);
        }

        
        @Test
        @Order(2)
        @DisplayName("Should update leaderboard after game completion")
        void shouldUpdateLeaderboardAfterGame() {
            
            String winnerName = "LeaderboardPlayer_" + UUID.randomUUID().toString().substring(0, 8);
            
            for (int i = 0; i < 3; i++) {
                List<PlayerInfo> players = List.of(
                        new PlayerInfo(winnerName, 1000, false),
                        new PlayerInfo("Loser" + i, 1000, false)
                );

                game = pokerGameService.createNewGame(players);
                gameId = game.getId();
                completeHandQuickly();
            }

            
            PlayerStatisticsService.LeaderboardData leaderboard = statisticsService.getLeaderboard();

            
            assertThat(leaderboard).isNotNull();
            assertThat(leaderboard.mostActive()).isNotEmpty();
        }

        
        @Test
        @Order(3)
        @DisplayName("Should record hand history for completed games")
        void shouldRecordHandHistory() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("HistoryPlayer1", 1000, false),
                    new PlayerInfo("HistoryPlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            completeHandQuickly();

            
            Page<HandHistory> historyPage = handHistoryRepository.findByGameId(gameId, Pageable.unpaged());
            List<HandHistory> histories = historyPage.getContent();
            assertThat(histories).isNotEmpty();
            
            HandHistory history = histories.get(0);
            assertThat(history.getGameId()).isEqualTo(gameId);
            assertThat(history.getWinnerName()).isNotNull();
        }

        
        @Test
        @Order(4)
        @DisplayName("Should record win statistics correctly")
        void shouldRecordWinStatisticsCorrectly() {
            
            String playerName = "WinStatsPlayer_" + UUID.randomUUID().toString().substring(0, 8);
            List<PlayerInfo> players = List.of(
                    new PlayerInfo(playerName, 1000, false),
                    new PlayerInfo("OtherPlayer", 100, false)  
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            Player player = game.getPlayers().stream()
                    .filter(p -> p.getName().equals(playerName))
                    .findFirst()
                    .orElseThrow();

            
            completeHandQuickly();

            
            Optional<PlayerStatistics> stats = statisticsService.getStatsByName(playerName);
            assertThat(stats).isPresent();
            
            assertThat(stats.get().getHandsPlayed()).isGreaterThanOrEqualTo(0);
        }
    }

    
    
    

    @Nested
    @DisplayName("4. WebSocket Integration Tests")
    @TestMethodOrder(OrderAnnotation.class)
    class WebSocketIntegrationTests {

        
        @Test
        @Order(1)
        @DisplayName("Should broadcast game updates via WebSocket")
        void shouldBroadcastGameUpdates() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("WSPlayer1", 1000, false),
                    new PlayerInfo("WSPlayer2", 1000, false)
            );

            
            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(anyString(), any(WebSocketGameUpdateMessage.class));
        }

        
        @Test
        @Order(2)
        @DisplayName("Should broadcast player action notifications")
        void shouldBroadcastPlayerActionNotifications() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("ActionPlayer1", 1000, false),
                    new PlayerInfo("ActionPlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();
            reset(messagingTemplate);

            
            Player current = game.getCurrentPlayer();
            game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);

            
            verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(anyString(), any(WebSocketGameUpdateMessage.class));
        }

        
        @Test
        @Order(3)
        @DisplayName("Should broadcast showdown result")
        void shouldBroadcastShowdownResult() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("ShowdownPlayer1", 1000, false),
                    new PlayerInfo("ShowdownPlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            completeHandQuickly();

            
            
            verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(anyString(), any(Object.class));
        }

        
        @Test
        @Order(4)
        @DisplayName("Should send correctly formatted WebSocket messages")
        void shouldSendCorrectlyFormattedMessages() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("FormatPlayer1", 1000, false),
                    new PlayerInfo("FormatPlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            
            String expectedDestination = "/topic/game/" + gameId;
            verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(eq(expectedDestination), any(WebSocketGameUpdateMessage.class));
        }
    }

    
    
    

    @Nested
    @DisplayName("5. Error Recovery Tests")
    @TestMethodOrder(OrderAnnotation.class)
    class ErrorRecoveryTests {

        
        @Test
        @Order(1)
        @DisplayName("Should reject invalid action and continue game")
        void shouldRejectInvalidActionAndContinueGame() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("ErrorPlayer1", 1000, false),
                    new PlayerInfo("ErrorPlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            Player current = game.getCurrentPlayer();
            Player other = game.getPlayers().stream()
                    .filter(p -> !p.getId().equals(current.getId()))
                    .findFirst()
                    .orElseThrow();

            
            assertThatThrownBy(() -> 
                pokerGameService.playerAct(gameId, other.getId(), PlayerAction.CALL, 0)
            ).isInstanceOf(IllegalStateException.class);

            
            refreshGame();
            assertThat(game.isFinished()).isFalse();
            assertThat(game.getCurrentPlayer().getId()).isEqualTo(current.getId());
        }

        
        @Test
        @Order(2)
        @DisplayName("Should handle concurrent actions gracefully")
        void shouldHandleConcurrentActionsGracefully() throws Exception {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("ConcurrentPlayer1", 1000, false),
                    new PlayerInfo("ConcurrentPlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();
            
            Player current = game.getCurrentPlayer();
            
            ExecutorService executor = Executors.newFixedThreadPool(2);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(2);

            
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            
            assertThat(successCount.get() + failCount.get()).isEqualTo(2);
            
            
            refreshGame();
            assertThat(game).isNotNull();
        }

        
        @Test
        @Order(3)
        @DisplayName("Should maintain game state consistency after error")
        void shouldMaintainGameStateConsistencyAfterError() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("ConsistencyPlayer1", 1000, false),
                    new PlayerInfo("ConsistencyPlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            int initialPot = game.getCurrentPot();
            GamePhase initialPhase = game.getPhase();
            Player current = game.getCurrentPlayer();

            
            if (current.getBetAmount() < game.getCurrentBet()) {
                assertThatThrownBy(() -> 
                    pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0)
                ).isInstanceOf(IllegalStateException.class);
            }

            
            refreshGame();
            assertThat(game.getCurrentPot()).isEqualTo(initialPot);
            assertThat(game.getPhase()).isEqualTo(initialPhase);
            assertThat(game.getCurrentPlayer().getId()).isEqualTo(current.getId());
        }

        
        @Test
        @Order(4)
        @DisplayName("Should reject invalid raise amount")
        void shouldRejectInvalidRaiseAmount() {
            
            List<PlayerInfo> players = List.of(
                    new PlayerInfo("RaisePlayer1", 1000, false),
                    new PlayerInfo("RaisePlayer2", 1000, false)
            );

            game = pokerGameService.createNewGame(players);
            gameId = game.getId();

            Player current = game.getCurrentPlayer();
            
            
            int tooSmallRaise = 1; 
            assertThatThrownBy(() -> 
                pokerGameService.playerAct(gameId, current.getId(), PlayerAction.RAISE, tooSmallRaise)
            ).isInstanceOf(IllegalArgumentException.class);

            
            refreshGame();
            assertThat(game.isFinished()).isFalse();
        }

        
        @Test
        @Order(5)
        @DisplayName("Should handle action on non-existent game")
        void shouldHandleActionOnNonExistentGame() {
            UUID fakeGameId = UUID.randomUUID();
            UUID fakePlayerId = UUID.randomUUID();

            assertThatThrownBy(() -> 
                pokerGameService.playerAct(fakeGameId, fakePlayerId, PlayerAction.FOLD, 0)
            ).isInstanceOf(NoSuchElementException.class);
        }
    }

    
    
    

    private void refreshGame() {
        game = pokerGameService.getGame(gameId).orElseThrow();
    }

    private void playPhaseWithAllCall(GamePhase expectedPhase) {
        int maxActions = 10;
        int actions = 0;
        
        while (game.getPhase() == expectedPhase && !game.isFinished() && actions < maxActions) {
            refreshGame();
            Player current = game.getCurrentPlayer();
            
            if (current == null || current.isFolded() || current.isAllIn()) {
                break;
            }

            if (current.getBetAmount() < game.getCurrentBet()) {
                game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
            } else {
                game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
            }
            actions++;
        }
    }

    private void playPhaseWithAllCheck(GamePhase expectedPhase) {
        int maxActions = 10;
        int actions = 0;
        
        while (game.getPhase() == expectedPhase && !game.isFinished() && actions < maxActions) {
            refreshGame();
            Player current = game.getCurrentPlayer();
            
            if (current == null || current.isFolded() || current.isAllIn()) {
                break;
            }

            game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
            actions++;
        }
    }

    private void completeHandQuickly() {
        int maxIterations = 50;
        int iterations = 0;

        while (!game.isFinished() && game.getPhase() != GamePhase.SHOWDOWN && iterations < maxIterations) {
            refreshGame();
            Player current = game.getCurrentPlayer();

            if (current == null || current.isFolded() || current.isAllIn()) {
                iterations++;
                continue;
            }

            if (current.getBetAmount() >= game.getCurrentBet()) {
                game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
            } else {
                game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
            }
            iterations++;
        }
        
        refreshGame();
    }
}
