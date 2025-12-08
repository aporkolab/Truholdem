package com.truholdem.service;

import com.truholdem.dto.ShowdownResult;
import com.truholdem.model.*;
import com.truholdem.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PokerGameService Tests")
class PokerGameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private HandEvaluator handEvaluator;

    @Mock
    private HandHistoryService handHistoryService;

    @Mock
    private PlayerStatisticsService playerStatisticsService;

    @Mock
    private GameNotificationService notificationService;

    private PokerGameService pokerGameService;

    @BeforeEach
    void setUp() {
        pokerGameService = new PokerGameService(
            gameRepository, 
            handEvaluator,
            handHistoryService,
            playerStatisticsService,
            notificationService
        );
    }

    @Nested
    @DisplayName("Game Creation Tests")
    class GameCreationTests {

        @Test
        @DisplayName("Should create a new game with valid players")
        void shouldCreateNewGameWithValidPlayers() {
            
            List<PlayerInfo> players = List.of(
                new PlayerInfo("Player1", 1000, false),
                new PlayerInfo("Bot1", 1000, true)
            );

            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
                Game game = invocation.getArgument(0);
                
                if (game.getId() == null) {
                    game.setId(UUID.randomUUID());
                }
                return game;
            });

            
            Game game = pokerGameService.createNewGame(players);

            
            assertNotNull(game);
            assertEquals(2, game.getPlayers().size());
            assertEquals(GamePhase.PRE_FLOP, game.getPhase());
            
            
            for (Player player : game.getPlayers()) {
                assertEquals(2, player.getHand().size());
            }
            
            
            assertTrue(game.getCurrentPot() > 0);
            assertEquals(game.getBigBlind(), game.getCurrentBet());
        }

        @Test
        @DisplayName("Should throw exception for too few players")
        void shouldThrowExceptionForTooFewPlayers() {
            List<PlayerInfo> players = List.of(
                new PlayerInfo("Player1", 1000, false)
            );

            assertThrows(IllegalArgumentException.class, 
                () -> pokerGameService.createNewGame(players));
        }

        @Test
        @DisplayName("Should throw exception for too many players")
        void shouldThrowExceptionForTooManyPlayers() {
            List<PlayerInfo> players = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                players.add(new PlayerInfo("Player" + i, 1000, false));
            }

            assertThrows(IllegalArgumentException.class, 
                () -> pokerGameService.createNewGame(players));
        }

        @Test
        @DisplayName("Should throw exception for null players list")
        void shouldThrowExceptionForNullPlayers() {
            assertThrows(IllegalArgumentException.class, 
                () -> pokerGameService.createNewGame(null));
        }
    }

    @Nested
    @DisplayName("Player Action Tests")
    class PlayerActionTests {

        private Game setupGameWithPlayers() {
            Game game = new Game();
            game.setId(UUID.randomUUID());
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            game.setCurrentPot(30);
            game.setMinRaiseAmount(20);

            Player player1 = new Player("Player1", 1000, false);
            player1.setId(UUID.randomUUID());
            player1.setBetAmount(10); 
            
            Player player2 = new Player("Player2", 1000, false);
            player2.setId(UUID.randomUUID());
            player2.setBetAmount(20); 

            game.addPlayer(player1);
            game.addPlayer(player2);
            game.setCurrentPlayerIndex(0);

            return game;
        }

        @Test
        @DisplayName("Should handle fold action correctly")
        void shouldHandleFoldCorrectly() {
            
            Game game = setupGameWithPlayers();
            UUID gameId = game.getId();
            UUID playerId = game.getPlayers().get(0).getId();

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.playerAct(gameId, playerId, PlayerAction.FOLD, 0);

            
            assertTrue(result.getPlayers().get(0).isFolded());
            verify(gameRepository).save(any(Game.class));
        }

        @Test
        @DisplayName("Should handle check action when bet is matched")
        void shouldHandleCheckWhenBetIsMatched() {
            
            Game game = setupGameWithPlayers();
            UUID gameId = game.getId();
            Player player = game.getPlayers().get(0);
            player.setBetAmount(20); 
            UUID playerId = player.getId();

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.playerAct(gameId, playerId, PlayerAction.CHECK, 0);

            
            assertTrue(result.getPlayers().get(0).hasActed());
        }

        @Test
        @DisplayName("Should throw exception when checking with unmatched bet")
        void shouldThrowExceptionWhenCheckingWithUnmatchedBet() {
            
            Game game = setupGameWithPlayers();
            UUID gameId = game.getId();
            UUID playerId = game.getPlayers().get(0).getId();

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

            
            assertThrows(IllegalStateException.class, 
                () -> pokerGameService.playerAct(gameId, playerId, PlayerAction.CHECK, 0));
        }

        @Test
        @DisplayName("Should handle call action correctly")
        void shouldHandleCallCorrectly() {
            
            Game game = setupGameWithPlayers();
            UUID gameId = game.getId();
            Player player = game.getPlayers().get(0);
            int initialChips = player.getChips();
            UUID playerId = player.getId();

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.playerAct(gameId, playerId, PlayerAction.CALL, 0);

            
            Player updatedPlayer = result.getPlayers().get(0);
            assertEquals(20, updatedPlayer.getBetAmount()); 
            assertTrue(updatedPlayer.getChips() < initialChips);
        }

        @Test
        @DisplayName("Should handle raise action correctly")
        void shouldHandleRaiseCorrectly() {
            
            Game game = setupGameWithPlayers();
            UUID gameId = game.getId();
            Player player = game.getPlayers().get(0);
            UUID playerId = player.getId();
            int raiseAmount = 60;

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.playerAct(gameId, playerId, PlayerAction.RAISE, raiseAmount);

            
            assertEquals(raiseAmount, result.getCurrentBet());
            assertTrue(result.getPlayers().get(0).hasActed());
            
            assertFalse(result.getPlayers().get(1).hasActed());
        }

        @Test
        @DisplayName("Should throw exception when not player's turn")
        void shouldThrowExceptionWhenNotPlayersTurn() {
            
            Game game = setupGameWithPlayers();
            UUID gameId = game.getId();
            UUID wrongPlayerId = game.getPlayers().get(1).getId(); 

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

            
            assertThrows(IllegalStateException.class, 
                () -> pokerGameService.playerAct(gameId, wrongPlayerId, PlayerAction.FOLD, 0));
        }

        @Test
        @DisplayName("Should throw exception when player has folded")
        void shouldThrowExceptionWhenPlayerHasFolded() {
            
            Game game = setupGameWithPlayers();
            UUID gameId = game.getId();
            Player player = game.getPlayers().get(0);
            player.setFolded(true);
            UUID playerId = player.getId();

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

            
            assertThrows(IllegalStateException.class, 
                () -> pokerGameService.playerAct(gameId, playerId, PlayerAction.CALL, 0));
        }
    }

    @Nested
    @DisplayName("Game Progression Tests")
    class GameProgressionTests {

        @Test
        @DisplayName("Should advance to flop after pre-flop betting completes")
        void shouldAdvanceToFlopAfterPreFlop() {
            
            Game game = createGameAtPhase(GamePhase.PRE_FLOP);
            UUID gameId = game.getId();
            
            
            for (Player player : game.getPlayers()) {
                player.setBetAmount(game.getCurrentBet());
                player.setHasActed(true);
            }
            game.setCurrentPlayerIndex(0);
            game.getPlayers().get(0).setHasActed(false);

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.playerAct(gameId, 
                game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);

            
            assertEquals(GamePhase.FLOP, result.getPhase());
            assertEquals(3, result.getCommunityCards().size());
        }

        @Test
        @DisplayName("Should advance to showdown when only one player remains")
        void shouldAdvanceToShowdownWhenOnePlayerRemains() {
            
            Game game = createGameAtPhase(GamePhase.FLOP);
            UUID gameId = game.getId();
            
            
            for (int i = 1; i < game.getPlayers().size(); i++) {
                game.getPlayers().get(i).setFolded(true);
            }
            game.setCurrentPlayerIndex(0);

            when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.playerAct(gameId, 
                game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);

            
            assertTrue(result.isFinished());
            assertEquals(game.getPlayers().get(0).getName(), result.getWinnerName());
        }

        private Game createGameAtPhase(GamePhase phase) {
            Game game = new Game();
            game.setId(UUID.randomUUID());
            game.setPhase(phase);
            game.setCurrentBet(0);
            game.setCurrentPot(100);
            game.setMinRaiseAmount(20);

            Deck deck = new Deck();
            deck.shuffle();
            game.setDeck(deck.getCards());

            Player player1 = new Player("Player1", 1000, false);
            player1.setId(UUID.randomUUID());
            player1.addCardToHand(deck.drawCard());
            player1.addCardToHand(deck.drawCard());

            Player player2 = new Player("Player2", 1000, false);
            player2.setId(UUID.randomUUID());
            player2.addCardToHand(deck.drawCard());
            player2.addCardToHand(deck.drawCard());

            game.addPlayer(player1);
            game.addPlayer(player2);
            game.setCurrentPlayerIndex(0);

            if (phase == GamePhase.FLOP || phase == GamePhase.TURN || phase == GamePhase.RIVER) {
                game.addCommunityCard(deck.drawCard());
                game.addCommunityCard(deck.drawCard());
                game.addCommunityCard(deck.drawCard());
            }
            if (phase == GamePhase.TURN || phase == GamePhase.RIVER) {
                game.addCommunityCard(deck.drawCard());
            }
            if (phase == GamePhase.RIVER) {
                game.addCommunityCard(deck.drawCard());
            }

            return game;
        }
    }

    @Nested
    @DisplayName("Showdown Tests")
    class ShowdownTests {

        @Test
        @DisplayName("Should correctly determine winner at showdown")
        void shouldCorrectlyDetermineWinnerAtShowdown() {
            
            Game game = createShowdownScenario();
            UUID gameId = game.getId();
            
            Player player1 = game.getPlayers().get(0);
            Player player2 = game.getPlayers().get(1);

            HandRanking strongHand = new HandRanking(
                HandRanking.HandType.TWO_PAIR, 
                List.of(Value.ACE, Value.KING), 
                List.of(Value.QUEEN)
            );
            HandRanking weakHand = new HandRanking(
                HandRanking.HandType.ONE_PAIR, 
                List.of(Value.JACK), 
                List.of(Value.TEN, Value.NINE, Value.EIGHT)
            );

            when(handEvaluator.evaluate(eq(player1.getHand()), any()))
                .thenReturn(strongHand);
            when(handEvaluator.evaluate(eq(player2.getHand()), any()))
                .thenReturn(weakHand);

            
            ShowdownResult result = pokerGameService.resolveShowdown(game);

            
            assertNotNull(result);
            assertEquals(1, result.getWinners().size());
            assertEquals(player1.getName(), result.getWinners().get(0).getPlayerName());
            assertEquals(game.getCurrentPot(), result.getWinners().get(0).getAmountWon());
        }

        @Test
        @DisplayName("Should split pot on tie")
        void shouldSplitPotOnTie() {
            
            Game game = createShowdownScenario();
            game.setCurrentPot(100);

            Player player1 = game.getPlayers().get(0);
            Player player2 = game.getPlayers().get(1);

            HandRanking tiedHand = new HandRanking(
                HandRanking.HandType.ONE_PAIR, 
                List.of(Value.ACE), 
                List.of(Value.KING, Value.QUEEN, Value.JACK)
            );

            when(handEvaluator.evaluate(any(), any())).thenReturn(tiedHand);

            
            ShowdownResult result = pokerGameService.resolveShowdown(game);

            
            assertEquals(2, result.getWinners().size());
            assertEquals(50, result.getWinners().get(0).getAmountWon());
            assertEquals(50, result.getWinners().get(1).getAmountWon());
        }

        private Game createShowdownScenario() {
            Game game = new Game();
            game.setId(UUID.randomUUID());
            game.setPhase(GamePhase.RIVER);
            game.setCurrentPot(200);

            Deck deck = new Deck();
            deck.shuffle();

            Player player1 = new Player("Player1", 1000, false);
            player1.setId(UUID.randomUUID());
            player1.addCardToHand(deck.drawCard());
            player1.addCardToHand(deck.drawCard());
            player1.setTotalBetInRound(100);

            Player player2 = new Player("Player2", 1000, false);
            player2.setId(UUID.randomUUID());
            player2.addCardToHand(deck.drawCard());
            player2.addCardToHand(deck.drawCard());
            player2.setTotalBetInRound(100);

            game.addPlayer(player1);
            game.addPlayer(player2);

            
            for (int i = 0; i < 5; i++) {
                game.addCommunityCard(deck.drawCard());
            }

            return game;
        }
    }

    @Nested
    @DisplayName("Bot Action Tests")
    class BotActionTests {

        @Test
        @DisplayName("Should execute bot action successfully")
        void shouldExecuteBotActionSuccessfully() {
            
            Game game = new Game();
            game.setId(UUID.randomUUID());
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(0);
            game.setCurrentPot(100);
            game.setMinRaiseAmount(20);

            Deck deck = new Deck();
            deck.shuffle();
            game.setDeck(deck.getCards());

            Player human = new Player("Human", 1000, false);
            human.setId(UUID.randomUUID());
            human.addCardToHand(deck.drawCard());
            human.addCardToHand(deck.drawCard());

            Player bot = new Player("Bot1", 1000, true);
            bot.setId(UUID.randomUUID());
            bot.addCardToHand(deck.drawCard());
            bot.addCardToHand(deck.drawCard());

            game.addPlayer(human);
            game.addPlayer(bot);
            game.setCurrentPlayerIndex(1); 

            for (int i = 0; i < 3; i++) {
                game.addCommunityCard(deck.drawCard());
            }

            when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.executeBotAction(game.getId(), bot.getId());

            
            assertNotNull(result);
            assertTrue(bot.hasActed());
        }

        @Test
        @DisplayName("Should throw exception when executing action for non-bot")
        void shouldThrowExceptionForNonBot() {
            
            Game game = new Game();
            game.setId(UUID.randomUUID());
            
            Player human = new Player("Human", 1000, false);
            human.setId(UUID.randomUUID());
            game.addPlayer(human);
            game.setCurrentPlayerIndex(0);

            when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

            
            assertThrows(IllegalStateException.class, 
                () -> pokerGameService.executeBotAction(game.getId(), human.getId()));
        }
    }

    @Nested
    @DisplayName("New Hand Tests")
    class NewHandTests {

        @Test
        @DisplayName("Should start new hand with existing players")
        void shouldStartNewHandWithExistingPlayers() {
            
            Game game = new Game();
            game.setId(UUID.randomUUID());
            game.setPhase(GamePhase.SHOWDOWN);
            game.setFinished(true);
            game.setHandNumber(1);

            Player player1 = new Player("Player1", 900, false);
            player1.setId(UUID.randomUUID());
            player1.setFolded(true);
            player1.setBetAmount(100);

            Player player2 = new Player("Player2", 1100, false);
            player2.setId(UUID.randomUUID());

            game.addPlayer(player1);
            game.addPlayer(player2);

            when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.startNewHand(game.getId());

            
            assertEquals(2, result.getHandNumber());
            assertEquals(GamePhase.PRE_FLOP, result.getPhase());
            assertFalse(result.isFinished());
            
            for (Player p : result.getPlayers()) {
                assertFalse(p.isFolded());
                assertEquals(2, p.getHand().size());
            }
        }

        @Test
        @DisplayName("Should remove players with no chips")
        void shouldRemovePlayersWithNoChips() {
            
            Game game = new Game();
            game.setId(UUID.randomUUID());
            game.setPhase(GamePhase.SHOWDOWN);

            Player player1 = new Player("Player1", 1000, false);
            player1.setId(UUID.randomUUID());

            Player player2 = new Player("Player2", 0, false); 
            player2.setId(UUID.randomUUID());

            Player player3 = new Player("Player3", 500, false);
            player3.setId(UUID.randomUUID());

            game.addPlayer(player1);
            game.addPlayer(player2);
            game.addPlayer(player3);

            when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
            when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));

            
            Game result = pokerGameService.startNewHand(game.getId());

            
            assertEquals(2, result.getPlayers().size());
            assertTrue(result.getPlayers().stream().allMatch(p -> p.getChips() > 0));
        }

        @Test
        @DisplayName("Should throw exception when not enough players with chips")
        void shouldThrowExceptionWhenNotEnoughPlayers() {
            
            Game game = new Game();
            game.setId(UUID.randomUUID());

            Player player1 = new Player("Player1", 1000, false);
            player1.setId(UUID.randomUUID());

            Player player2 = new Player("Player2", 0, false);
            player2.setId(UUID.randomUUID());

            game.addPlayer(player1);
            game.addPlayer(player2);

            when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

            
            assertThrows(IllegalStateException.class, 
                () -> pokerGameService.startNewHand(game.getId()));
        }
    }
}
