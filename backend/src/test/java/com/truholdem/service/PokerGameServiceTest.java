package com.truholdem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.truholdem.dto.ShowdownResult;
import com.truholdem.model.Card;
import com.truholdem.model.Deck;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.HandRanking;
import com.truholdem.model.HandType;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.PlayerInfo;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;
import com.truholdem.repository.GameRepository;


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

    @Mock
    private AdvancedBotAIService botAIService;

    @Mock
    private GameMetricsService metricsService;

    private PokerGameService pokerGameService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        
        lenient().when(metricsService.timeGameCreation(any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        lenient().when(metricsService.timeActionProcessing(any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        lenient().when(metricsService.timeShowdown(any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());

        pokerGameService = new PokerGameService(
                gameRepository,
                handEvaluator,
                handHistoryService,
                playerStatisticsService,
                notificationService,
                botAIService,
                metricsService);
    }

    
    
    

    private void setupRepositorySaveToReturnArgument() {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            if (game.getId() == null) {
                game.setId(UUID.randomUUID());
            }
            return game;
        });
    }

    private List<PlayerInfo> createPlayerInfoList(int count) {
        List<PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(new PlayerInfo("Player" + (i + 1), 1000, false));
        }
        return players;
    }

    private Game createGameWithPlayers(int playerCount, int startingChips) {
        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setPhase(GamePhase.PRE_FLOP);
        game.setCurrentBet(game.getBigBlind());
        game.setCurrentPot(game.getSmallBlind() + game.getBigBlind());
        game.setMinRaiseAmount(game.getBigBlind());

        for (int i = 0; i < playerCount; i++) {
            Player player = new Player("Player" + (i + 1), startingChips, false);
            player.setId(UUID.randomUUID());
            player.setSeatPosition(i);
            game.addPlayer(player);
        }

        return game;
    }

    private Game createGameInBettingState() {
        Game game = createGameWithPlayers(3, 1000);
        
        
        Player sbPlayer = game.getPlayers().get(1);
        Player bbPlayer = game.getPlayers().get(2);
        sbPlayer.setBetAmount(10);
        sbPlayer.setTotalBetInRound(10);
        sbPlayer.setChips(990);
        
        bbPlayer.setBetAmount(20);
        bbPlayer.setTotalBetInRound(20);
        bbPlayer.setChips(980);
        
        game.setCurrentBet(20);
        game.setCurrentPot(30);
        game.setCurrentPlayerIndex(0); 
        
        return game;
    }

    
    
    

    @Nested
    @DisplayName("1. Game Lifecycle Tests")
    class GameLifecycleTests {

        @Nested
        @DisplayName("1.1 createNewGame - Player Count Validation")
        class CreateNewGameValidationTests {

            @Test
            @DisplayName("Should create game with exactly 2 players (minimum)")
            void shouldCreateGameWithMinimumPlayers() {
                List<PlayerInfo> players = createPlayerInfoList(2);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                assertNotNull(game);
                assertEquals(2, game.getPlayers().size());
                assertEquals(GamePhase.PRE_FLOP, game.getPhase());
            }

            @Test
            @DisplayName("Should create game with exactly 10 players (maximum)")
            void shouldCreateGameWithMaximumPlayers() {
                List<PlayerInfo> players = createPlayerInfoList(10);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                assertNotNull(game);
                assertEquals(10, game.getPlayers().size());
            }

            @ParameterizedTest(name = "Should create game with {0} players")
            @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10})
            @DisplayName("Should create game with valid player counts")
            void shouldCreateGameWithValidPlayerCounts(int playerCount) {
                List<PlayerInfo> players = createPlayerInfoList(playerCount);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                assertNotNull(game);
                assertEquals(playerCount, game.getPlayers().size());
            }

            @Test
            @DisplayName("Should throw exception for 1 player (too few)")
            void shouldThrowExceptionForOnePlayer() {
                List<PlayerInfo> players = createPlayerInfoList(1);

                IllegalArgumentException exception = assertThrows(
                        IllegalArgumentException.class,
                        () -> pokerGameService.createNewGame(players));

                assertTrue(exception.getMessage().contains("between 2 and 10"));
            }

            @Test
            @DisplayName("Should throw exception for 11 players (too many)")
            void shouldThrowExceptionForElevenPlayers() {
                List<PlayerInfo> players = createPlayerInfoList(11);

                IllegalArgumentException exception = assertThrows(
                        IllegalArgumentException.class,
                        () -> pokerGameService.createNewGame(players));

                assertTrue(exception.getMessage().contains("between 2 and 10"));
            }

            @Test
            @DisplayName("Should throw exception for null player list")
            void shouldThrowExceptionForNullPlayerList() {
                assertThrows(IllegalArgumentException.class,
                        () -> pokerGameService.createNewGame(null));
            }

            @Test
            @DisplayName("Should throw exception for empty player list")
            void shouldThrowExceptionForEmptyPlayerList() {
                List<PlayerInfo> players = new ArrayList<>();

                assertThrows(IllegalArgumentException.class,
                        () -> pokerGameService.createNewGame(players));
            }
        }

        @Nested
        @DisplayName("1.2 createNewGame - Initial Game State")
        class CreateNewGameInitialStateTests {

            @Test
            @DisplayName("Should initialize dealer position at 0")
            void shouldInitializeDealerPositionAtZero() {
                List<PlayerInfo> players = createPlayerInfoList(3);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                assertEquals(0, game.getDealerPosition());
            }

            @Test
            @DisplayName("Should post small blind correctly")
            void shouldPostSmallBlindCorrectly() {
                List<PlayerInfo> players = createPlayerInfoList(3);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                
                Player sbPlayer = game.getPlayers().get(1);
                assertEquals(10, sbPlayer.getBetAmount());
                assertEquals(990, sbPlayer.getChips());
            }

            @Test
            @DisplayName("Should post big blind correctly")
            void shouldPostBigBlindCorrectly() {
                List<PlayerInfo> players = createPlayerInfoList(3);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                
                Player bbPlayer = game.getPlayers().get(2);
                assertEquals(20, bbPlayer.getBetAmount());
                assertEquals(980, bbPlayer.getChips());
            }

            @Test
            @DisplayName("Should set current bet to big blind")
            void shouldSetCurrentBetToBigBlind() {
                List<PlayerInfo> players = createPlayerInfoList(3);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                assertEquals(game.getBigBlind(), game.getCurrentBet());
            }

            @Test
            @DisplayName("Should initialize pot with blinds")
            void shouldInitializePotWithBlinds() {
                List<PlayerInfo> players = createPlayerInfoList(3);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                int expectedPot = game.getSmallBlind() + game.getBigBlind();
                assertEquals(expectedPot, game.getCurrentPot());
            }

            @Test
            @DisplayName("Should deal exactly 2 hole cards to each player")
            void shouldDealTwoHoleCardsToEachPlayer() {
                List<PlayerInfo> players = createPlayerInfoList(4);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                for (Player player : game.getPlayers()) {
                    assertEquals(2, player.getHand().size(), 
                            "Player " + player.getName() + " should have 2 cards");
                }
            }

            @Test
            @DisplayName("Should deal unique cards to all players")
            void shouldDealUniqueCardsToAllPlayers() {
                List<PlayerInfo> players = createPlayerInfoList(4);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                List<Card> allCards = new ArrayList<>();
                for (Player player : game.getPlayers()) {
                    allCards.addAll(player.getHand());
                }

                
                long uniqueCount = allCards.stream().distinct().count();
                assertEquals(allCards.size(), uniqueCount, "All dealt cards should be unique");
            }

            @Test
            @DisplayName("Should set phase to PRE_FLOP")
            void shouldSetPhaseToPrefFlop() {
                List<PlayerInfo> players = createPlayerInfoList(2);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                assertEquals(GamePhase.PRE_FLOP, game.getPhase());
            }

            @Test
            @DisplayName("Should handle heads-up blinds correctly")
            void shouldHandleHeadsUpBlindsCorrectly() {
                
                List<PlayerInfo> players = createPlayerInfoList(2);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                
                Player dealer = game.getPlayers().get(0);
                assertEquals(10, dealer.getBetAmount());

                
                Player otherPlayer = game.getPlayers().get(1);
                assertEquals(20, otherPlayer.getBetAmount());
            }

            @Test
            @DisplayName("Should assign seat positions correctly")
            void shouldAssignSeatPositionsCorrectly() {
                List<PlayerInfo> players = createPlayerInfoList(5);
                setupRepositorySaveToReturnArgument();

                Game game = pokerGameService.createNewGame(players);

                for (int i = 0; i < game.getPlayers().size(); i++) {
                    assertEquals(i, game.getPlayers().get(i).getSeatPosition());
                }
            }
        }

        @Nested
        @DisplayName("1.3 startNewHand Tests")
        class StartNewHandTests {

            @Test
            @DisplayName("Should reset game state for new hand")
            void shouldResetGameStateForNewHand() {
                Game game = createGameWithPlayers(3, 1000);
                game.setPhase(GamePhase.SHOWDOWN);
                game.setFinished(true);
                game.setCurrentPot(500);
                game.setCurrentBet(100);
                game.addCommunityCard(new Card(Suit.HEARTS, Value.ACE));
                game.setWinnerName("Player1");

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.startNewHand(game.getId());

                assertEquals(GamePhase.PRE_FLOP, result.getPhase());
                assertFalse(result.isFinished());
                assertEquals(30, result.getCurrentPot()); 
                assertEquals(20, result.getCurrentBet()); 
                assertTrue(result.getCommunityCards().isEmpty());
                assertNull(result.getWinnerName());
            }

            @Test
            @DisplayName("Should remove busted players (0 chips)")
            void shouldRemoveBustedPlayers() {
                Game game = createGameWithPlayers(4, 1000);
                game.getPlayers().get(1).setChips(0); 
                game.getPlayers().get(3).setChips(0); 

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.startNewHand(game.getId());

                assertEquals(2, result.getPlayers().size());
                assertTrue(result.getPlayers().stream().allMatch(p -> p.getChips() > 0));
            }

            @Test
            @DisplayName("Should rotate dealer position")
            void shouldRotateDealerPosition() {
                Game game = createGameWithPlayers(4, 1000);
                game.setDealerPosition(0);
                game.setHandNumber(1);

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.startNewHand(game.getId());

                assertEquals(1, result.getDealerPosition());
            }

            @Test
            @DisplayName("Should wrap dealer position at end of table")
            void shouldWrapDealerPositionAtEndOfTable() {
                Game game = createGameWithPlayers(3, 1000);
                game.setDealerPosition(2);
                game.setButtonSeatPosition(2); // Set button seat to match dealer for proper dead button logic

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.startNewHand(game.getId());

                assertEquals(0, result.getDealerPosition());
            }

            @Test
            @DisplayName("Should increment hand number")
            void shouldIncrementHandNumber() {
                Game game = createGameWithPlayers(3, 1000);
                game.setHandNumber(5);

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.startNewHand(game.getId());

                assertEquals(6, result.getHandNumber());
            }

            @Test
            @DisplayName("Should reset player states")
            void shouldResetPlayerStates() {
                Game game = createGameWithPlayers(3, 1000);
                for (Player p : game.getPlayers()) {
                    p.setFolded(true);
                    p.setBetAmount(100);
                    p.setHasActed(true);
                    p.setAllIn(true);
                    p.setTotalBetInRound(200);
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.startNewHand(game.getId());

                for (Player p : result.getPlayers()) {
                    assertFalse(p.isFolded());
                    assertFalse(p.isAllIn());
                    
                    assertEquals(2, p.getHand().size()); 
                }
            }

            @Test
            @DisplayName("Should throw exception when not enough players with chips")
            void shouldThrowExceptionWhenNotEnoughPlayersWithChips() {
                Game game = createGameWithPlayers(3, 1000);
                game.getPlayers().get(0).setChips(0);
                game.getPlayers().get(1).setChips(0);
                

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(IllegalStateException.class,
                        () -> pokerGameService.startNewHand(game.getId()));
            }

            @Test
            @DisplayName("Should throw exception when game not found")
            void shouldThrowExceptionWhenGameNotFound() {
                UUID nonExistentId = UUID.randomUUID();
                when(gameRepository.findById(nonExistentId)).thenReturn(Optional.empty());

                assertThrows(NoSuchElementException.class,
                        () -> pokerGameService.startNewHand(nonExistentId));
            }

            @Test
            @DisplayName("Should deal new hole cards after reset")
            void shouldDealNewHoleCardsAfterReset() {
                Game game = createGameWithPlayers(2, 1000);
                
                List<Card> oldCards = new ArrayList<>(game.getPlayers().get(0).getHand());
                game.getPlayers().forEach(p -> p.getHand().clear());

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.startNewHand(game.getId());

                for (Player p : result.getPlayers()) {
                    assertEquals(2, p.getHand().size());
                }
            }
        }
    }

    
    
    

    @Nested
    @DisplayName("2. Betting Actions Tests")
    class BettingActionsTests {

        @Nested
        @DisplayName("2.1 Fold Actions")
        class FoldTests {

            @Test
            @DisplayName("Should mark player as folded")
            void shouldMarkPlayerAsFolded() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.FOLD, 0);

                assertTrue(result.getPlayers().get(0).isFolded());
            }

            @Test
            @DisplayName("Should not change player chips on fold")
            void shouldNotChangeChipsOnFold() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                int chipsBefore = actingPlayer.getChips();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.FOLD, 0);

                assertEquals(chipsBefore, actingPlayer.getChips());
            }

            @Test
            @DisplayName("Should throw exception when folded player tries to act")
            void shouldThrowExceptionWhenFoldedPlayerTriesToAct() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setFolded(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(IllegalStateException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), actingPlayer.getId(), PlayerAction.CHECK, 0));
            }

            @Test
            @DisplayName("Should award pot to last remaining player")
            void shouldAwardPotToLastRemainingPlayer() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(100);
                game.setCurrentBet(20);
                game.setCurrentPlayerIndex(0);
                
                Player foldingPlayer = game.getPlayers().get(0);
                Player remainingPlayer = game.getPlayers().get(1);
                int chipsBefore = remainingPlayer.getChips();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), foldingPlayer.getId(), PlayerAction.FOLD, 0);

                assertTrue(result.isFinished());
                assertEquals(remainingPlayer.getName(), result.getWinnerName());
                assertEquals(chipsBefore + 100, remainingPlayer.getChips());
            }

            @Test
            @DisplayName("Should mark folded player hasActed true")
            void shouldMarkFoldedPlayerHasActedTrue() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.FOLD, 0);

                assertTrue(actingPlayer.hasActed());
            }
        }

        @Nested
        @DisplayName("2.2 Check Actions")
        class CheckTests {

            @Test
            @DisplayName("Should allow check when no bet to match")
            void shouldAllowCheckWhenNoBetToMatch() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.CHECK, 0);

                assertTrue(result.getPlayers().get(0).hasActed());
            }

            @Test
            @DisplayName("Should allow check when bet is already matched")
            void shouldAllowCheckWhenBetIsAlreadyMatched() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(game.getCurrentBet()); 
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.CHECK, 0);

                assertTrue(result.getPlayers().get(0).hasActed());
            }

            @Test
            @DisplayName("Should throw exception when checking facing unmatched bet")
            void shouldThrowExceptionWhenCheckingFacingUnmatchedBet() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0); 
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(IllegalStateException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), actingPlayer.getId(), PlayerAction.CHECK, 0));
            }

            @Test
            @DisplayName("Should not change chips on check")
            void shouldNotChangeChipsOnCheck() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                int chipsBefore = actingPlayer.getChips();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.CHECK, 0);

                assertEquals(chipsBefore, actingPlayer.getChips());
            }

            @Test
            @DisplayName("Should not change pot on check")
            void shouldNotChangePotOnCheck() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                int potBefore = game.getCurrentPot();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.CHECK, 0);

                assertEquals(potBefore, result.getCurrentPot());
            }
        }

        @Nested
        @DisplayName("2.3 Call Actions")
        class CallTests {

            @Test
            @DisplayName("Should deduct correct call amount from chips")
            void shouldDeductCorrectCallAmountFromChips() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(1000);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.CALL, 0);

                assertEquals(980, actingPlayer.getChips()); 
                assertEquals(20, actingPlayer.getBetAmount());
            }

            @Test
            @DisplayName("Should add call amount to pot")
            void shouldAddCallAmountToPot() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                int potBefore = game.getCurrentPot();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.CALL, 0);

                assertEquals(potBefore + 20, result.getCurrentPot());
            }

            @Test
            @DisplayName("Should handle partial call when short stacked (all-in)")
            void shouldHandlePartialCallWhenShortStacked() {
                Game game = createGameInBettingState();
                game.setCurrentBet(100);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(50); 
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.CALL, 0);

                assertEquals(0, actingPlayer.getChips());
                assertEquals(50, actingPlayer.getBetAmount());
                assertTrue(actingPlayer.isAllIn());
            }

            @Test
            @DisplayName("Should return 0 when already matched bet")
            void shouldReturnZeroWhenAlreadyMatchedBet() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(game.getCurrentBet()); 
                int chipsBefore = actingPlayer.getChips();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.CALL, 0);

                assertEquals(chipsBefore, actingPlayer.getChips()); 
            }

            @Test
            @DisplayName("Should calculate correct partial call amount")
            void shouldCalculateCorrectPartialCallAmount() {
                Game game = createGameInBettingState();
                game.setCurrentBet(100);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(30); 
                actingPlayer.setChips(1000);
                int potBefore = game.getCurrentPot();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.CALL, 0);

                assertEquals(100, actingPlayer.getBetAmount()); 
                assertEquals(930, actingPlayer.getChips()); 
                assertEquals(potBefore + 70, result.getCurrentPot());
            }
        }

        @Nested
        @DisplayName("2.4 Bet Actions")
        class BetTests {

            @Test
            @DisplayName("Should allow first bet in round")
            void shouldAllowFirstBetInRound() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.BET, 50);

                assertEquals(50, result.getCurrentBet());
                assertEquals(50, actingPlayer.getBetAmount());
            }

            @Test
            @DisplayName("Should throw exception when betting facing existing bet")
            void shouldThrowExceptionWhenBettingFacingExistingBet() {
                Game game = createGameInBettingState();
                
                Player actingPlayer = game.getPlayers().get(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(IllegalStateException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), actingPlayer.getId(), PlayerAction.BET, 50));
            }

            @Test
            @DisplayName("Should enforce minimum bet equals big blind")
            void shouldEnforceMinimumBetEqualsBigBlind() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(IllegalArgumentException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), actingPlayer.getId(), PlayerAction.BET, 10)); 
            }

            @Test
            @DisplayName("Should update pot correctly on bet")
            void shouldUpdatePotCorrectlyOnBet() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                int potBefore = game.getCurrentPot();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.BET, 100);

                assertEquals(potBefore + 100, result.getCurrentPot());
            }

            @Test
            @DisplayName("Should reset hasActed flags for other players on bet")
            void shouldResetHasActedFlagsOnBet() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                
                game.getPlayers().get(1).setHasActed(true);
                game.getPlayers().get(2).setHasActed(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.BET, 50);

                assertFalse(game.getPlayers().get(1).hasActed());
                assertFalse(game.getPlayers().get(2).hasActed());
            }

            @Test
            @DisplayName("Should set minRaiseAmount to bet size")
            void shouldSetMinRaiseAmountToBetSize() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.BET, 75);

                assertEquals(75, result.getMinRaiseAmount());
            }
        }

        @Nested
        @DisplayName("2.5 Raise Actions")
        class RaiseTests {

            @Test
            @DisplayName("Should allow valid raise amount")
            void shouldAllowValidRaiseAmount() {
                Game game = createGameInBettingState();
                
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.RAISE, 40); 

                assertEquals(40, result.getCurrentBet());
            }

            @Test
            @DisplayName("Should enforce minimum raise rule")
            void shouldEnforceMinimumRaiseRule() {
                Game game = createGameInBettingState();
                game.setMinRaiseAmount(30);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(1000); 
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                
                assertThrows(IllegalArgumentException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), actingPlayer.getId(), PlayerAction.RAISE, 30)); 
            }

            @Test
            @DisplayName("Should allow all-in raise even if less than minimum (poker rules)")
            void shouldAllowAllInRaiseLessThanMinimum() {
                // According to poker rules, a player can go all-in even if they can't afford
                // the minimum raise - this is called a "short all-in"
                Game game = createGameInBettingState();
                game.setMinRaiseAmount(50);
                game.setCurrentBet(50);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(70); // Can't afford 50 (call) + 50 (min raise) = 100

                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

                // Should NOT throw - player goes all-in with 70 chips
                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.RAISE, 70);

                // Player should be all-in
                assertTrue(actingPlayer.isAllIn(), "Player should be all-in");
                assertEquals(0, actingPlayer.getChips(), "Player should have no chips left");
            }

            @Test
            @DisplayName("Should reset hasActed flags on raise")
            void shouldResetHasActedFlagsOnRaise() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                game.getPlayers().get(1).setHasActed(true);
                game.getPlayers().get(2).setHasActed(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.RAISE, 60);

                assertFalse(game.getPlayers().get(1).hasActed());
                assertFalse(game.getPlayers().get(2).hasActed());
            }

            @Test
            @DisplayName("Should update current bet to raise total")
            void shouldUpdateCurrentBetToRaiseTotal() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.RAISE, 80);

                assertEquals(80, result.getCurrentBet());
            }

            @Test
            @DisplayName("Should update minRaiseAmount to raise increment")
            void shouldUpdateMinRaiseAmountToRaiseIncrement() {
                Game game = createGameInBettingState();
                
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                Game result = pokerGameService.playerAct(
                        game.getId(), actingPlayer.getId(), PlayerAction.RAISE, 60); 

                assertEquals(40, result.getMinRaiseAmount()); 
            }

            @Test
            @DisplayName("Should correctly calculate chips after raise with partial bet")
            void shouldCorrectlyCalculateChipsAfterRaiseWithPartialBet() {
                Game game = createGameInBettingState();
                game.setCurrentBet(50);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(20); 
                actingPlayer.setChips(1000);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.RAISE, 100);

                
                assertEquals(920, actingPlayer.getChips());
                assertEquals(100, actingPlayer.getBetAmount());
            }
        }

        @Nested
        @DisplayName("2.6 All-In Actions")
        class AllInTests {

            @Test
            @DisplayName("Should set all-in flag when chips reach zero")
            void shouldSetAllInFlagWhenChipsReachZero() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(100);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.BET, 100);

                assertTrue(actingPlayer.isAllIn());
                assertEquals(0, actingPlayer.getChips());
            }

            @Test
            @DisplayName("Should throw exception when all-in player tries to act")
            void shouldThrowExceptionWhenAllInPlayerTriesToAct() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setAllIn(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(IllegalStateException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), actingPlayer.getId(), PlayerAction.CHECK, 0));
            }

            @Test
            @DisplayName("Should record all-in in statistics")
            void shouldRecordAllInInStatistics() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(100);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.BET, 100);

                verify(playerStatisticsService).recordAllIn(actingPlayer.getName());
            }

            @Test
            @DisplayName("Should allow call that causes all-in")
            void shouldAllowCallThatCausesAllIn() {
                Game game = createGameInBettingState();
                game.setCurrentBet(200);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(150); 
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.CALL, 0);

                assertTrue(actingPlayer.isAllIn());
                assertEquals(0, actingPlayer.getChips());
                assertEquals(150, actingPlayer.getBetAmount());
            }

            @Test
            @DisplayName("Should not reset hasActed for all-in players on bet")
            void shouldNotResetHasActedForAllInPlayersOnBet() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                
                Player allInPlayer = game.getPlayers().get(1);
                allInPlayer.setAllIn(true);
                allInPlayer.setHasActed(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.BET, 50);

                assertTrue(allInPlayer.hasActed()); 
            }

            @Test
            @DisplayName("Should track totalBetInRound for all-in player")
            void shouldTrackTotalBetInRoundForAllInPlayer() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setChips(75);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), PlayerAction.BET, 75);

                assertEquals(75, actingPlayer.getTotalBetInRound());
            }
        }

        @Nested
        @DisplayName("2.7 Turn Validation")
        class TurnValidationTests {

            @Test
            @DisplayName("Should throw exception when not player's turn")
            void shouldThrowExceptionWhenNotPlayersTurn() {
                Game game = createGameInBettingState();
                game.setCurrentPlayerIndex(0);
                Player wrongPlayer = game.getPlayers().get(1);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(IllegalStateException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), wrongPlayer.getId(), PlayerAction.CHECK, 0));
            }

            @Test
            @DisplayName("Should allow correct player to act")
            void shouldAllowCorrectPlayerToAct() {
                Game game = createGameInBettingState();
                game.setCurrentBet(0);
                game.setCurrentPlayerIndex(1);
                Player correctPlayer = game.getPlayers().get(1);
                correctPlayer.setBetAmount(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                assertDoesNotThrow(() -> pokerGameService.playerAct(
                        game.getId(), correctPlayer.getId(), PlayerAction.CHECK, 0));
            }

            @Test
            @DisplayName("Should throw exception for non-existent player")
            void shouldThrowExceptionForNonExistentPlayer() {
                Game game = createGameInBettingState();
                UUID fakePlayerId = UUID.randomUUID();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

                assertThrows(NoSuchElementException.class,
                        () -> pokerGameService.playerAct(
                                game.getId(), fakePlayerId, PlayerAction.CHECK, 0));
            }

            @Test
            @DisplayName("Should throw exception for non-existent game")
            void shouldThrowExceptionForNonExistentGame() {
                UUID fakeGameId = UUID.randomUUID();
                UUID fakePlayerId = UUID.randomUUID();
                
                when(gameRepository.findById(fakeGameId)).thenReturn(Optional.empty());

                assertThrows(NoSuchElementException.class,
                        () -> pokerGameService.playerAct(
                                fakeGameId, fakePlayerId, PlayerAction.FOLD, 0));
            }
        }

        @Nested
        @DisplayName("2.8 Integration Scenarios")
        class BettingIntegrationTests {

            @Test
            @DisplayName("Should handle full betting round: bet-call-fold")
            void shouldHandleFullBettingRoundBetCallFold() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentBet(0);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentPlayerIndex(0);
                game.setCurrentPot(60);
                
                for (Player p : game.getPlayers()) {
                    p.setBetAmount(0);
                }
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), game.getPlayers().get(0).getId(), 
                        PlayerAction.BET, 50);
                assertEquals(50, game.getCurrentBet());
                
                
                game.setCurrentPlayerIndex(1);
                
                
                pokerGameService.playerAct(game.getId(), game.getPlayers().get(1).getId(), 
                        PlayerAction.CALL, 0);
                assertEquals(50, game.getPlayers().get(1).getBetAmount());
                
                
                game.setCurrentPlayerIndex(2);
                
                
                pokerGameService.playerAct(game.getId(), game.getPlayers().get(2).getId(), 
                        PlayerAction.FOLD, 0);
                assertTrue(game.getPlayers().get(2).isFolded());
            }

            @Test
            @DisplayName("Should handle raise war scenario")
            void shouldHandleRaiseWarScenario() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentBet(20);
                game.setMinRaiseAmount(20);
                game.setCurrentPot(30);
                game.setCurrentPlayerIndex(0);
                
                game.getPlayers().get(0).setBetAmount(10);
                game.getPlayers().get(1).setBetAmount(20);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), game.getPlayers().get(0).getId(), 
                        PlayerAction.RAISE, 60);
                assertEquals(60, game.getCurrentBet());
                assertEquals(40, game.getMinRaiseAmount()); 
                
                
                game.setCurrentPlayerIndex(1);
                
                
                pokerGameService.playerAct(game.getId(), game.getPlayers().get(1).getId(), 
                        PlayerAction.RAISE, 140);
                assertEquals(140, game.getCurrentBet());
                assertEquals(80, game.getMinRaiseAmount());
            }
        }
    }

    
    
    

    @Nested
    @DisplayName("3. Game Flow Tests")
    class GameFlowTests {

        @Nested
        @DisplayName("3.1 Phase Transitions")
        class PhaseTransitionTests {

            private Game createGameAtPhase(GamePhase phase, int communityCardCount) {
                Game game = createGameWithPlayers(2, 1000);
                game.setPhase(phase);
                game.setCurrentBet(0);
                game.setCurrentPot(100);
                
                
                Deck deck = new Deck();
                deck.shuffle();
                game.setDeck(deck.getCards());
                
                
                for (int i = 0; i < communityCardCount && !game.getDeck().isEmpty(); i++) {
                    game.addCommunityCard(game.getDeck().remove(0));
                }
                
                
                for (Player p : game.getPlayers()) {
                    p.setBetAmount(0);
                    p.setHasActed(true);
                }
                
                return game;
            }

            @Test
            @DisplayName("Should transition from PRE_FLOP to FLOP after all players act")
            void shouldTransitionFromPreFlopToFlop() {
                Game game = createGameAtPhase(GamePhase.PRE_FLOP, 0);
                game.setCurrentPlayerIndex(0);
                
                game.getPlayers().forEach(p -> {
                    p.setHasActed(false);
                    p.setBetAmount(20);
                });
                game.setCurrentBet(20);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                
                
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(GamePhase.FLOP, game.getPhase());
            }

            @Test
            @DisplayName("Should transition from FLOP to TURN")
            void shouldTransitionFromFlopToTurn() {
                Game game = createGameAtPhase(GamePhase.FLOP, 3);
                game.setCurrentPlayerIndex(0);
                game.getPlayers().forEach(p -> p.setHasActed(false));
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(GamePhase.TURN, game.getPhase());
                assertEquals(4, game.getCommunityCards().size());
            }

            @Test
            @DisplayName("Should transition from TURN to RIVER")
            void shouldTransitionFromTurnToRiver() {
                Game game = createGameAtPhase(GamePhase.TURN, 4);
                game.setCurrentPlayerIndex(0);
                game.getPlayers().forEach(p -> p.setHasActed(false));
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(GamePhase.RIVER, game.getPhase());
                assertEquals(5, game.getCommunityCards().size());
            }

            @Test
            @DisplayName("Should transition from RIVER to SHOWDOWN")
            void shouldTransitionFromRiverToShowdown() {
                Game game = createGameAtPhase(GamePhase.RIVER, 5);
                game.setCurrentPlayerIndex(0);
                game.getPlayers().forEach(p -> {
                    p.setHasActed(false);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                });
                
                HandRanking ranking = new HandRanking(
                        HandType.HIGH_CARD,
                        List.of(Value.ACE),
                        List.of(Value.KING, Value.QUEEN, Value.JACK, Value.TEN));
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                when(handEvaluator.evaluate(any(), any())).thenReturn(ranking);
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(GamePhase.SHOWDOWN, game.getPhase());
            }

            @Test
            @DisplayName("Should deal 3 cards on FLOP transition")
            void shouldDealThreeCardsOnFlop() {
                Game game = createGameAtPhase(GamePhase.PRE_FLOP, 0);
                int deckSizeBefore = game.getDeck().size();
                game.getPlayers().forEach(p -> {
                    p.setHasActed(false);
                    p.setBetAmount(20);
                });
                game.setCurrentPlayerIndex(0);
                game.setCurrentBet(20);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(3, game.getCommunityCards().size());
            }

            @Test
            @DisplayName("Should deal 1 card on TURN")
            void shouldDealOneCardOnTurn() {
                Game game = createGameAtPhase(GamePhase.FLOP, 3);
                int deckSizeBefore = game.getDeck().size();
                game.getPlayers().forEach(p -> p.setHasActed(false));
                game.setCurrentPlayerIndex(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(4, game.getCommunityCards().size());
                
                assertEquals(deckSizeBefore - 2, game.getDeck().size());
            }

            @Test
            @DisplayName("Should reset bets on phase transition")
            void shouldResetBetsOnPhaseTransition() {
                Game game = createGameAtPhase(GamePhase.FLOP, 3);
                game.setCurrentBet(100);
                game.getPlayers().forEach(p -> {
                    p.setBetAmount(100);
                    p.setHasActed(false);
                });
                game.setCurrentPlayerIndex(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(0, game.getCurrentBet());
                for (Player p : game.getPlayers()) {
                    assertEquals(0, p.getBetAmount());
                }
            }

            @Test
            @DisplayName("Should reset minRaiseAmount to big blind on phase transition")
            void shouldResetMinRaiseAmountOnPhaseTransition() {
                Game game = createGameAtPhase(GamePhase.FLOP, 3);
                game.setMinRaiseAmount(200); 
                game.getPlayers().forEach(p -> p.setHasActed(false));
                game.setCurrentPlayerIndex(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                game.setCurrentPlayerIndex(1);
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CHECK, 0);

                assertEquals(game.getBigBlind(), game.getMinRaiseAmount());
            }
        }

        @Nested
        @DisplayName("3.2 All Fold Except One")
        class AllFoldExceptOneTests {

            @Test
            @DisplayName("Should award pot when all but one fold")
            void shouldAwardPotWhenAllButOneFold() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(150);
                game.setCurrentBet(50);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentPlayerIndex(0);
                
                Player winner = game.getPlayers().get(2);
                int winnerChipsBefore = winner.getChips();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.FOLD, 0);
                game.setCurrentPlayerIndex(1);
                
                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.FOLD, 0);

                assertTrue(game.isFinished());
                assertEquals(winner.getName(), game.getWinnerName());
                assertEquals(winnerChipsBefore + 150, winner.getChips());
            }

            @Test
            @DisplayName("Should skip showdown when only one player remains")
            void shouldSkipShowdownWhenOnlyOnePlayerRemains() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(100);
                game.setCurrentBet(50);
                game.setPhase(GamePhase.PRE_FLOP);
                game.setCurrentPlayerIndex(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.FOLD, 0);

                
                assertEquals(GamePhase.SHOWDOWN, game.getPhase());
                assertTrue(game.isFinished());
                assertEquals("All opponents folded", game.getWinningHandDescription());
            }

            @Test
            @DisplayName("Should set correct winner info when all fold")
            void shouldSetCorrectWinnerInfoWhenAllFold() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(100);
                game.setCurrentBet(20);
                game.setCurrentPlayerIndex(0);
                
                Player winner = game.getPlayers().get(1);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.FOLD, 0);

                assertEquals(winner.getName(), game.getWinnerName());
                assertTrue(game.getWinnerIds().contains(winner.getId()));
                assertEquals("All opponents folded", game.getWinningHandDescription());
            }
        }

        @Nested
        @DisplayName("3.3 Betting Round Completion")
        class BettingRoundCompletionTests {

            @Test
            @DisplayName("Should complete round when all have acted and bets matched")
            void shouldCompleteRoundWhenAllActedAndBetsMatched() {
                Game game = createGameWithPlayers(2, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentBet(50);
                game.setCurrentPot(100);
                game.setCurrentPlayerIndex(0);
                
                
                Deck deck = new Deck();
                deck.shuffle();
                game.setDeck(deck.getCards());
                for (int i = 0; i < 3; i++) {
                    game.addCommunityCard(game.getDeck().remove(0));
                }
                
                game.getPlayers().get(0).setBetAmount(50);
                game.getPlayers().get(0).setHasActed(false);
                game.getPlayers().get(1).setBetAmount(50);
                game.getPlayers().get(1).setHasActed(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);

                assertEquals(GamePhase.TURN, game.getPhase());
            }

            @Test
            @DisplayName("Should not complete round when player hasn't acted")
            void shouldNotCompleteRoundWhenPlayerHasntActed() {
                Game game = createGameWithPlayers(3, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentBet(0);
                game.setCurrentPlayerIndex(0);
                
                
                game.getPlayers().get(0).setHasActed(false);
                game.getPlayers().get(1).setHasActed(false);
                game.getPlayers().get(2).setHasActed(false);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);

                assertEquals(GamePhase.FLOP, game.getPhase()); 
            }

            @Test
            @DisplayName("Should advance phase when all bets matched after call")
            void shouldAdvancePhaseWhenBetsMatchedAfterCall() {
                Game game = createGameWithPlayers(2, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentBet(100);
                game.setCurrentPlayerIndex(1);
                
                game.getPlayers().get(0).setBetAmount(100);
                game.getPlayers().get(0).setHasActed(true);
                game.getPlayers().get(1).setBetAmount(50); 
                game.getPlayers().get(1).setHasActed(false);
                game.getPlayers().get(1).setChips(1000);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(1).getId(), PlayerAction.CALL, 0);

                
                assertEquals(GamePhase.TURN, game.getPhase());
            }
        }

        @Nested
        @DisplayName("3.4 Player Turn Order")
        class PlayerTurnOrderTests {

            @Test
            @DisplayName("Should skip folded players")
            void shouldSkipFoldedPlayers() {
                Game game = createGameWithPlayers(3, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentBet(0);
                game.setCurrentPlayerIndex(0);
                
                
                game.getPlayers().get(1).setFolded(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);

                
                assertEquals(2, game.getCurrentPlayerIndex());
            }

            @Test
            @DisplayName("Should skip all-in players")
            void shouldSkipAllInPlayers() {
                Game game = createGameWithPlayers(3, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentBet(0);
                game.setCurrentPlayerIndex(0);
                
                
                game.getPlayers().get(1).setAllIn(true);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);

                
                assertEquals(2, game.getCurrentPlayerIndex());
            }

            @Test
            @DisplayName("Should wrap around to first player")
            void shouldWrapAroundToFirstPlayer() {
                Game game = createGameWithPlayers(3, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentBet(50);
                game.setCurrentPlayerIndex(2); 
                
                game.getPlayers().forEach(p -> p.setBetAmount(50));
                game.getPlayers().get(0).setHasActed(false); 
                game.getPlayers().get(1).setHasActed(true);
                game.getPlayers().get(2).setHasActed(false);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), 
                        game.getPlayers().get(2).getId(), PlayerAction.CHECK, 0);

                
                assertEquals(0, game.getCurrentPlayerIndex());
            }

            @Test
            @DisplayName("Should set first player after dealer on new round")
            void shouldSetFirstPlayerAfterDealerOnNewRound() {
                Game game = createGameWithPlayers(4, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setDealerPosition(1);
                game.setCurrentBet(0);
                game.setCurrentPlayerIndex(0);
                
                Deck deck = new Deck();
                deck.shuffle();
                game.setDeck(deck.getCards());
                for (int i = 0; i < 3; i++) {
                    game.addCommunityCard(game.getDeck().remove(0));
                }
                
                
                game.getPlayers().forEach(p -> p.setHasActed(false));
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                for (int i = 0; i < 4; i++) {
                    game.setCurrentPlayerIndex(i);
                    pokerGameService.playerAct(game.getId(), 
                            game.getPlayers().get(i).getId(), PlayerAction.CHECK, 0);
                }

                
                assertEquals(GamePhase.TURN, game.getPhase());
                assertEquals(2, game.getCurrentPlayerIndex());
            }
        }

        @Nested
        @DisplayName("3.5 Auto-Advance to Showdown")
        class AutoAdvanceTests {

            @Test
            @DisplayName("Should handle all-in scenario properly")
            void shouldHandleAllInScenario() {
                Game game = createGameWithPlayers(2, 1000);
                game.setPhase(GamePhase.FLOP);
                game.setCurrentPot(2000);
                game.setCurrentBet(0);
                game.setCurrentPlayerIndex(0);
                
                
                game.getPlayers().forEach(p -> {
                    p.setChips(500);
                    p.setHasActed(false);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                });
                
                
                game.addCommunityCard(new Card(Suit.DIAMONDS, Value.TWO));
                game.addCommunityCard(new Card(Suit.CLUBS, Value.THREE));
                game.addCommunityCard(new Card(Suit.HEARTS, Value.FOUR));
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                
                Game result = pokerGameService.playerAct(
                        game.getId(), game.getPlayers().get(0).getId(), PlayerAction.CHECK, 0);
                
                assertNotNull(result);
            }
        }
    }

    
    
    

    @Nested
    @DisplayName("4. Pot Management Tests")
    class PotManagementTests {

        @Nested
        @DisplayName("4.1 Main Pot Calculation")
        class MainPotCalculationTests {

            @Test
            @DisplayName("Should calculate main pot with no all-ins")
            void shouldCalculateMainPotWithNoAllIns() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(300);
                game.setPhase(GamePhase.RIVER);

                // Give each player unique cards to avoid mock matching issues
                Player player0 = game.getPlayers().get(0);
                Player player1 = game.getPlayers().get(1);
                Player player2 = game.getPlayers().get(2);

                player0.setTotalBetInRound(100);
                player0.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                player0.addCardToHand(new Card(Suit.HEARTS, Value.KING));

                player1.setTotalBetInRound(100);
                player1.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                player1.addCardToHand(new Card(Suit.SPADES, Value.THREE));

                player2.setTotalBetInRound(100);
                player2.addCardToHand(new Card(Suit.CLUBS, Value.FOUR));
                player2.addCardToHand(new Card(Suit.CLUBS, Value.FIVE));

                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 6]));
                }

                HandRanking winner = new HandRanking(
                        HandType.ONE_PAIR,
                        List.of(Value.ACE),
                        List.of(Value.KING, Value.QUEEN, Value.JACK));
                HandRanking loser = new HandRanking(
                        HandType.HIGH_CARD,
                        List.of(Value.KING),
                        List.of(Value.QUEEN, Value.JACK, Value.TEN, Value.NINE));

                when(handEvaluator.evaluate(eq(player0.getHand()), any()))
                        .thenReturn(winner);
                when(handEvaluator.evaluate(eq(player1.getHand()), any()))
                        .thenReturn(loser);
                when(handEvaluator.evaluate(eq(player2.getHand()), any()))
                        .thenReturn(loser);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(300, result.getTotalPot());
                assertEquals(1, result.getWinners().size());
            }

            @Test
            @DisplayName("Should accumulate bets into pot correctly")
            void shouldAccumulateBetsIntoPotCorrectly() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                int potBefore = game.getCurrentPot();
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), 
                        PlayerAction.RAISE, 100);

                assertEquals(potBefore + 100, game.getCurrentPot());
            }

            @Test
            @DisplayName("Should track total bet in round per player")
            void shouldTrackTotalBetInRoundPerPlayer() {
                Game game = createGameInBettingState();
                Player actingPlayer = game.getPlayers().get(0);
                actingPlayer.setBetAmount(0);
                actingPlayer.setTotalBetInRound(0);
                
                when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
                setupRepositorySaveToReturnArgument();

                pokerGameService.playerAct(game.getId(), actingPlayer.getId(), 
                        PlayerAction.RAISE, 80);

                assertEquals(80, actingPlayer.getTotalBetInRound());
            }
        }

        @Nested
        @DisplayName("4.2 Side Pot - Single All-In")
        class SingleAllInSidePotTests {

            @Test
            @DisplayName("Should create side pot when one player all-in for less")
            void shouldCreateSidePotWhenOnePlayerAllInForLess() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(300);
                game.setPhase(GamePhase.RIVER);
                
                
                game.getPlayers().get(0).setChips(0);
                game.getPlayers().get(0).setAllIn(true);
                game.getPlayers().get(0).setTotalBetInRound(50);
                
                
                game.getPlayers().get(1).setTotalBetInRound(100);
                game.getPlayers().get(2).setTotalBetInRound(100);
                
                for (Player p : game.getPlayers()) {
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking ranking = new HandRanking(
                        HandType.HIGH_CARD,
                        List.of(Value.ACE),
                        List.of(Value.KING));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(ranking);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                
                assertNotNull(result);
                assertFalse(result.getWinners().isEmpty());
            }

            @Test
            @DisplayName("Should only award main pot to all-in player who wins")
            void shouldOnlyAwardMainPotToAllInPlayerWhoWins() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(150); 
                game.setPhase(GamePhase.RIVER);
                
                Player allInPlayer = game.getPlayers().get(0);
                allInPlayer.setChips(0);
                allInPlayer.setAllIn(true);
                allInPlayer.setTotalBetInRound(50);
                allInPlayer.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                allInPlayer.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player bigStackPlayer = game.getPlayers().get(1);
                bigStackPlayer.setTotalBetInRound(100);
                bigStackPlayer.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                bigStackPlayer.addCardToHand(new Card(Suit.DIAMONDS, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.CLUBS, Value.values()[i + 4]));
                }
                
                HandRanking strongHand = new HandRanking(
                        HandType.ONE_PAIR,
                        List.of(Value.ACE),
                        List.of(Value.KING));
                HandRanking weakHand = new HandRanking(
                        HandType.HIGH_CARD,
                        List.of(Value.THREE),
                        List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(allInPlayer.getHand()), any()))
                        .thenReturn(strongHand);
                when(handEvaluator.evaluate(eq(bigStackPlayer.getHand()), any()))
                        .thenReturn(weakHand);

                int allInChipsBefore = allInPlayer.getChips();
                ShowdownResult result = pokerGameService.resolveShowdown(game);

                
                
                assertTrue(result.getWinners().stream()
                        .anyMatch(w -> w.getPlayerName().equals(allInPlayer.getName())));
            }
        }

        @Nested
        @DisplayName("4.3 Side Pot - Multiple All-Ins")
        class MultipleAllInSidePotTests {

            @Test
            @DisplayName("Should handle two all-ins at different levels")
            void shouldHandleTwoAllInsAtDifferentLevels() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(350);
                game.setPhase(GamePhase.RIVER);
                
                
                game.getPlayers().get(0).setChips(0);
                game.getPlayers().get(0).setAllIn(true);
                game.getPlayers().get(0).setTotalBetInRound(50);
                
                
                game.getPlayers().get(1).setChips(0);
                game.getPlayers().get(1).setAllIn(true);
                game.getPlayers().get(1).setTotalBetInRound(100);
                
                
                game.getPlayers().get(2).setTotalBetInRound(200);
                
                for (Player p : game.getPlayers()) {
                    p.addCardToHand(new Card(Suit.HEARTS, Value.values()[p.getSeatPosition() + 10]));
                    p.addCardToHand(new Card(Suit.SPADES, Value.values()[p.getSeatPosition() + 8]));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                
                HandRanking hand0 = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.JACK), List.of(Value.NINE));
                HandRanking hand1 = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.QUEEN), List.of(Value.TEN));
                HandRanking hand2 = new HandRanking(HandType.TWO_PAIR, 
                        List.of(Value.KING, Value.JACK), List.of(Value.TEN));
                
                when(handEvaluator.evaluate(eq(game.getPlayers().get(0).getHand()), any()))
                        .thenReturn(hand0);
                when(handEvaluator.evaluate(eq(game.getPlayers().get(1).getHand()), any()))
                        .thenReturn(hand1);
                when(handEvaluator.evaluate(eq(game.getPlayers().get(2).getHand()), any()))
                        .thenReturn(hand2);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertNotNull(result);
                
                assertTrue(result.getWinners().stream()
                        .anyMatch(w -> w.getPlayerName().equals("Player3")));
            }

            @Test
            @DisplayName("Should create correct pot structure with 3 all-ins")
            void shouldCreateCorrectPotStructureWithThreeAllIns() {
                Game game = createGameWithPlayers(4, 1000);
                game.setCurrentPot(700);
                game.setPhase(GamePhase.RIVER);
                
                
                game.getPlayers().get(0).setChips(0);
                game.getPlayers().get(0).setAllIn(true);
                game.getPlayers().get(0).setTotalBetInRound(50);
                
                
                game.getPlayers().get(1).setChips(0);
                game.getPlayers().get(1).setAllIn(true);
                game.getPlayers().get(1).setTotalBetInRound(100);
                
                
                game.getPlayers().get(2).setChips(0);
                game.getPlayers().get(2).setAllIn(true);
                game.getPlayers().get(2).setTotalBetInRound(150);
                
                
                game.getPlayers().get(3).setTotalBetInRound(400);
                
                for (Player p : game.getPlayers()) {
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking ranking = new HandRanking(
                        HandType.HIGH_CARD,
                        List.of(Value.ACE),
                        List.of(Value.KING));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(ranking);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertNotNull(result);
                
                assertFalse(result.getWinners().isEmpty(), "Should have at least one winner");
            }
        }

        @Nested
        @DisplayName("4.4 Complex Side Pot Scenarios")
        class ComplexSidePotTests {

            @Test
            @DisplayName("Should handle 4+ players with mixed all-ins")
            void shouldHandleFourPlusPlayersWithMixedAllIns() {
                Game game = createGameWithPlayers(5, 1000);
                game.setCurrentPot(1000);
                game.setPhase(GamePhase.RIVER);
                
                
                game.getPlayers().get(0).setAllIn(true);
                game.getPlayers().get(0).setTotalBetInRound(100);
                game.getPlayers().get(0).setChips(0);
                
                game.getPlayers().get(1).setFolded(true);
                game.getPlayers().get(1).setTotalBetInRound(50);
                
                game.getPlayers().get(2).setAllIn(true);
                game.getPlayers().get(2).setTotalBetInRound(200);
                game.getPlayers().get(2).setChips(0);
                
                game.getPlayers().get(3).setTotalBetInRound(300);
                
                game.getPlayers().get(4).setTotalBetInRound(350);
                
                for (Player p : game.getPlayers()) {
                    if (!p.isFolded()) {
                        p.addCardToHand(new Card(Suit.HEARTS, Value.values()[p.getSeatPosition() + 7]));
                        p.addCardToHand(new Card(Suit.SPADES, Value.values()[p.getSeatPosition() + 5]));
                    }
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking ranking = new HandRanking(
                        HandType.HIGH_CARD,
                        List.of(Value.ACE),
                        List.of(Value.KING));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(ranking);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertNotNull(result);
                
                assertTrue(result.getWinners().stream()
                        .noneMatch(w -> w.getPlayerName().equals("Player2")));
            }

            @Test
            @DisplayName("Should correctly distribute pot with different hand strengths")
            void shouldCorrectlyDistributePotWithDifferentHandStrengths() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(300);
                game.setPhase(GamePhase.RIVER);
                
                
                Player p0 = game.getPlayers().get(0);
                p0.setAllIn(true);
                p0.setTotalBetInRound(50);
                p0.setChips(0);
                p0.addCardToHand(new Card(Suit.HEARTS, Value.TWO));
                p0.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                
                Player p1 = game.getPlayers().get(1);
                p1.setAllIn(true);
                p1.setTotalBetInRound(100);
                p1.setChips(0);
                p1.addCardToHand(new Card(Suit.HEARTS, Value.JACK));
                p1.addCardToHand(new Card(Suit.SPADES, Value.QUEEN));
                
                
                Player p2 = game.getPlayers().get(2);
                p2.setTotalBetInRound(150);
                p2.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                p2.addCardToHand(new Card(Suit.SPADES, Value.KING));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking weakHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                HandRanking mediumHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.QUEEN), List.of(Value.JACK));
                HandRanking strongHand = new HandRanking(HandType.TWO_PAIR, 
                        List.of(Value.ACE, Value.KING), List.of(Value.QUEEN));
                
                when(handEvaluator.evaluate(eq(p0.getHand()), any())).thenReturn(weakHand);
                when(handEvaluator.evaluate(eq(p1.getHand()), any())).thenReturn(mediumHand);
                when(handEvaluator.evaluate(eq(p2.getHand()), any())).thenReturn(strongHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                
                assertNotNull(result);
                assertFalse(result.getWinners().isEmpty(), "Should have winners");
            }
        }

        @Nested
        @DisplayName("4.5 Pot Distribution")
        class PotDistributionTests {

            @Test
            @DisplayName("Should distribute pot to single winner")
            void shouldDistributePotToSingleWinner() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                Player winner = game.getPlayers().get(0);
                winner.setTotalBetInRound(100);
                winner.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                winner.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                int winnerChipsBefore = winner.getChips();
                
                Player loser = game.getPlayers().get(1);
                loser.setTotalBetInRound(100);
                loser.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                loser.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking winningHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking losingHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(winner.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(loser.getHand()), any())).thenReturn(losingHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(1, result.getWinners().size());
                assertEquals(200, result.getWinners().get(0).getAmountWon());
                assertEquals(winnerChipsBefore + 200, winner.getChips());
            }

            @Test
            @DisplayName("Should split pot evenly on tie")
            void shouldSplitPotEvenlyOnTie() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                for (Player p : game.getPlayers()) {
                    p.setTotalBetInRound(100);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking tiedHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING, Value.QUEEN, Value.JACK));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(tiedHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(2, result.getWinners().size());
                assertEquals(100, result.getWinners().get(0).getAmountWon());
                assertEquals(100, result.getWinners().get(1).getAmountWon());
            }

            @Test
            @DisplayName("Should handle odd chip on split")
            void shouldHandleOddChipOnSplit() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(301); 
                game.setPhase(GamePhase.RIVER);
                
                for (Player p : game.getPlayers()) {
                    p.setTotalBetInRound(100);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking tiedHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.ACE), List.of(Value.KING));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(tiedHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(3, result.getWinners().size());
                int totalDistributed = result.getWinners().stream()
                        .mapToInt(ShowdownResult.WinnerInfo::getAmountWon).sum();
                assertEquals(301, totalDistributed);
            }

            @Test
            @DisplayName("Should award side pot to different winner than main pot")
            void shouldAwardSidePotToDifferentWinnerThanMainPot() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(400);
                game.setPhase(GamePhase.RIVER);
                
                
                Player p0 = game.getPlayers().get(0);
                p0.setAllIn(true);
                p0.setTotalBetInRound(50);
                p0.setChips(0);
                p0.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                p0.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                
                Player p1 = game.getPlayers().get(1);
                p1.setFolded(true);
                p1.setTotalBetInRound(100);
                
                
                Player p2 = game.getPlayers().get(2);
                p2.setTotalBetInRound(250);
                p2.addCardToHand(new Card(Suit.SPADES, Value.QUEEN));
                p2.addCardToHand(new Card(Suit.SPADES, Value.JACK));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking strongHand = new HandRanking(HandType.FLUSH, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking mediumHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.QUEEN), List.of(Value.JACK));
                
                when(handEvaluator.evaluate(eq(p0.getHand()), any())).thenReturn(strongHand);
                when(handEvaluator.evaluate(eq(p2.getHand()), any())).thenReturn(mediumHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                
                assertTrue(result.getWinners().size() >= 1);
            }
        }
    }

    
    
    

    @Nested
    @DisplayName("5. Showdown Tests")
    class ShowdownTests {

        @Nested
        @DisplayName("5.1 Winner Determination")
        class WinnerDeterminationTests {

            @Test
            @DisplayName("Should determine winner by hand ranking")
            void shouldDetermineWinnerByHandRanking() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                Player p1 = game.getPlayers().get(0);
                p1.setTotalBetInRound(100);
                p1.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                p1.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player p2 = game.getPlayers().get(1);
                p2.setTotalBetInRound(100);
                p2.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                p2.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                
                HandRanking royalFlush = new HandRanking(HandType.ROYAL_FLUSH, 
                        List.of(Value.ACE), List.of());
                HandRanking highCard = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(p1.getHand()), any())).thenReturn(royalFlush);
                when(handEvaluator.evaluate(eq(p2.getHand()), any())).thenReturn(highCard);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(1, result.getWinners().size());
                assertEquals("Player1", result.getWinners().get(0).getPlayerName());
            }

            @Test
            @DisplayName("Should determine winner by kicker")
            void shouldDetermineWinnerByKicker() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                Player p1 = game.getPlayers().get(0);
                p1.setTotalBetInRound(100);
                p1.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                p1.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player p2 = game.getPlayers().get(1);
                p2.setTotalBetInRound(100);
                p2.addCardToHand(new Card(Suit.SPADES, Value.ACE));
                p2.addCardToHand(new Card(Suit.SPADES, Value.QUEEN));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                
                HandRanking aceKingKicker = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING, Value.QUEEN, Value.JACK));
                HandRanking aceQueenKicker = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.QUEEN, Value.JACK, Value.TEN));
                
                when(handEvaluator.evaluate(eq(p1.getHand()), any())).thenReturn(aceKingKicker);
                when(handEvaluator.evaluate(eq(p2.getHand()), any())).thenReturn(aceQueenKicker);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(1, result.getWinners().size());
                assertEquals("Player1", result.getWinners().get(0).getPlayerName());
            }

            @Test
            @DisplayName("Should exclude folded players from winner determination")
            void shouldExcludeFoldedPlayersFromWinnerDetermination() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(300);
                game.setPhase(GamePhase.RIVER);
                
                
                Player p0 = game.getPlayers().get(0);
                p0.setFolded(true);
                p0.setTotalBetInRound(100);
                
                
                Player p1 = game.getPlayers().get(1);
                p1.setTotalBetInRound(100);
                p1.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                p1.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                
                Player p2 = game.getPlayers().get(2);
                p2.setTotalBetInRound(100);
                p2.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                p2.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking goodHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking badHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(p1.getHand()), any())).thenReturn(goodHand);
                when(handEvaluator.evaluate(eq(p2.getHand()), any())).thenReturn(badHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(1, result.getWinners().size());
                assertEquals("Player2", result.getWinners().get(0).getPlayerName());
            }
        }

        @Nested
        @DisplayName("5.2 Tie and Split Pot")
        class TieAndSplitPotTests {

            @Test
            @DisplayName("Should split pot on exact tie")
            void shouldSplitPotOnExactTie() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                for (Player p : game.getPlayers()) {
                    p.setTotalBetInRound(100);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking tiedRanking = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING, Value.QUEEN, Value.JACK));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(tiedRanking);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(2, result.getWinners().size());
                assertEquals(100, result.getWinners().get(0).getAmountWon());
                assertEquals(100, result.getWinners().get(1).getAmountWon());
            }

            @Test
            @DisplayName("Should split pot three ways")
            void shouldSplitPotThreeWays() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(300);
                game.setPhase(GamePhase.RIVER);
                
                for (Player p : game.getPlayers()) {
                    p.setTotalBetInRound(100);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking tiedRanking = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.ACE), List.of(Value.KING));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(tiedRanking);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(3, result.getWinners().size());
                assertEquals(100, result.getWinners().get(0).getAmountWon());
                assertEquals(100, result.getWinners().get(1).getAmountWon());
                assertEquals(100, result.getWinners().get(2).getAmountWon());
            }

            @Test
            @DisplayName("Should split pot only among tied players")
            void shouldSplitPotOnlyAmongTiedPlayers() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(300);
                game.setPhase(GamePhase.RIVER);
                
                
                Player p1 = game.getPlayers().get(0);
                p1.setTotalBetInRound(100);
                p1.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                p1.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player p2 = game.getPlayers().get(1);
                p2.setTotalBetInRound(100);
                p2.addCardToHand(new Card(Suit.SPADES, Value.ACE));
                p2.addCardToHand(new Card(Suit.SPADES, Value.KING));
                
                
                Player p3 = game.getPlayers().get(2);
                p3.setTotalBetInRound(100);
                p3.addCardToHand(new Card(Suit.CLUBS, Value.TWO));
                p3.addCardToHand(new Card(Suit.CLUBS, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking winningHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking losingHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(p1.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(p2.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(p3.getHand()), any())).thenReturn(losingHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertEquals(2, result.getWinners().size());
                assertEquals(150, result.getWinners().get(0).getAmountWon());
                assertEquals(150, result.getWinners().get(1).getAmountWon());
            }
        }

        @Nested
        @DisplayName("5.3 Multiple Side Pots Resolution")
        class MultipleSidePotsResolutionTests {

            @Test
            @DisplayName("Should resolve main and side pot with winners")
            void shouldResolveMainAndSidePotWithWinners() {
                Game game = createGameWithPlayers(3, 1000);
                game.setCurrentPot(350);
                game.setPhase(GamePhase.RIVER);
                
                
                Player p0 = game.getPlayers().get(0);
                p0.setAllIn(true);
                p0.setTotalBetInRound(50);
                p0.setChips(0);
                p0.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                p0.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                
                Player p1 = game.getPlayers().get(1);
                p1.setTotalBetInRound(100);
                p1.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                p1.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                
                Player p2 = game.getPlayers().get(2);
                p2.setTotalBetInRound(200);
                p2.addCardToHand(new Card(Suit.CLUBS, Value.JACK));
                p2.addCardToHand(new Card(Suit.CLUBS, Value.QUEEN));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking bestHand = new HandRanking(HandType.FLUSH, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking mediumHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.QUEEN), List.of(Value.JACK));
                HandRanking worstHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(p0.getHand()), any())).thenReturn(bestHand);
                when(handEvaluator.evaluate(eq(p1.getHand()), any())).thenReturn(worstHand);
                when(handEvaluator.evaluate(eq(p2.getHand()), any())).thenReturn(mediumHand);

                ShowdownResult result = pokerGameService.resolveShowdown(game);

                assertNotNull(result);
                
                assertFalse(result.getWinners().isEmpty(), "Should have winners");
            }
        }

        @Nested
        @DisplayName("5.4 Statistics Recording")
        class StatisticsRecordingTests {

            @Test
            @DisplayName("Should record showdown win in statistics")
            void shouldRecordShowdownWinInStatistics() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                Player winner = game.getPlayers().get(0);
                winner.setTotalBetInRound(100);
                winner.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                winner.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player loser = game.getPlayers().get(1);
                loser.setTotalBetInRound(100);
                loser.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                loser.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking winningHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking losingHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(winner.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(loser.getHand()), any())).thenReturn(losingHand);

                pokerGameService.resolveShowdown(game);

                verify(playerStatisticsService).recordShowdown(winner.getName(), true);
                verify(playerStatisticsService).recordShowdown(loser.getName(), false);
            }

            @Test
            @DisplayName("Should record win amount in statistics")
            void shouldRecordWinAmountInStatistics() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(500);
                game.setPhase(GamePhase.RIVER);
                
                Player winner = game.getPlayers().get(0);
                winner.setTotalBetInRound(250);
                winner.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                winner.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player loser = game.getPlayers().get(1);
                loser.setTotalBetInRound(250);
                loser.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                loser.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking winningHand = new HandRanking(HandType.FLUSH, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking losingHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(winner.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(loser.getHand()), any())).thenReturn(losingHand);

                pokerGameService.resolveShowdown(game);

                verify(playerStatisticsService).recordWin(winner.getName(), 500);
            }

            @Test
            @DisplayName("Should record all-in result in statistics")
            void shouldRecordAllInResultInStatistics() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                Player allInWinner = game.getPlayers().get(0);
                allInWinner.setAllIn(true);
                allInWinner.setTotalBetInRound(100);
                allInWinner.setChips(0);
                allInWinner.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                allInWinner.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player loser = game.getPlayers().get(1);
                loser.setTotalBetInRound(100);
                loser.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                loser.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking winningHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking losingHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(allInWinner.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(loser.getHand()), any())).thenReturn(losingHand);

                pokerGameService.resolveShowdown(game);

                verify(playerStatisticsService).recordAllInResult(allInWinner.getName(), true);
            }
        }

        @Nested
        @DisplayName("5.5 Chip Updates")
        class ChipUpdateTests {

            @Test
            @DisplayName("Should add winnings to winner chips")
            void shouldAddWinningsToWinnerChips() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(500);
                game.setPhase(GamePhase.RIVER);
                
                Player winner = game.getPlayers().get(0);
                winner.setTotalBetInRound(250);
                winner.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                winner.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                int winnerChipsBefore = winner.getChips();
                
                Player loser = game.getPlayers().get(1);
                loser.setTotalBetInRound(250);
                loser.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                loser.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking winningHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking losingHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(winner.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(loser.getHand()), any())).thenReturn(losingHand);

                pokerGameService.resolveShowdown(game);

                assertEquals(winnerChipsBefore + 500, winner.getChips());
            }

            @Test
            @DisplayName("Should not change loser chips at showdown")
            void shouldNotChangeLoserChipsAtShowdown() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                Player winner = game.getPlayers().get(0);
                winner.setTotalBetInRound(100);
                winner.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                winner.addCardToHand(new Card(Suit.HEARTS, Value.KING));
                
                Player loser = game.getPlayers().get(1);
                loser.setTotalBetInRound(100);
                loser.addCardToHand(new Card(Suit.SPADES, Value.TWO));
                loser.addCardToHand(new Card(Suit.SPADES, Value.THREE));
                int loserChipsBefore = loser.getChips();
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 4]));
                }
                
                HandRanking winningHand = new HandRanking(HandType.ONE_PAIR, 
                        List.of(Value.ACE), List.of(Value.KING));
                HandRanking losingHand = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.THREE), List.of(Value.TWO));
                
                when(handEvaluator.evaluate(eq(winner.getHand()), any())).thenReturn(winningHand);
                when(handEvaluator.evaluate(eq(loser.getHand()), any())).thenReturn(losingHand);

                pokerGameService.resolveShowdown(game);

                
                assertEquals(loserChipsBefore, loser.getChips());
            }

            @Test
            @DisplayName("Should set game as finished after showdown")
            void shouldSetGameAsFinishedAfterShowdown() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                game.setFinished(false);
                
                for (Player p : game.getPlayers()) {
                    p.setTotalBetInRound(100);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking ranking = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.ACE), List.of(Value.KING));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(ranking);

                pokerGameService.resolveShowdown(game);

                assertTrue(game.isFinished());
            }

            @Test
            @DisplayName("Should reset pot to zero after showdown")
            void shouldResetPotToZeroAfterShowdown() {
                Game game = createGameWithPlayers(2, 1000);
                game.setCurrentPot(200);
                game.setPhase(GamePhase.RIVER);
                
                for (Player p : game.getPlayers()) {
                    p.setTotalBetInRound(100);
                    p.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
                    p.addCardToHand(new Card(Suit.SPADES, Value.KING));
                }
                
                for (int i = 0; i < 5; i++) {
                    game.addCommunityCard(new Card(Suit.DIAMONDS, Value.values()[i + 2]));
                }
                
                HandRanking ranking = new HandRanking(HandType.HIGH_CARD, 
                        List.of(Value.ACE), List.of(Value.KING));
                
                when(handEvaluator.evaluate(any(), any())).thenReturn(ranking);

                pokerGameService.resolveShowdown(game);

                assertEquals(0, game.getCurrentPot());
            }
        }
    }
}
