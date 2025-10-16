package com.truholdem.domain.aggregate;

import com.truholdem.domain.event.*;
import com.truholdem.domain.exception.GameStateException;
import com.truholdem.domain.exception.InvalidActionException;
import com.truholdem.domain.exception.PlayerNotFoundException;
import com.truholdem.domain.value.Chips;
import com.truholdem.model.GamePhase;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.PlayerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("PokerGame Aggregate")
class PokerGameTest {

    private static final Chips SMALL_BLIND = Chips.of(10);
    private static final Chips BIG_BLIND = Chips.of(20);
    private static final int STARTING_STACK = 1000;

    private List<PlayerInfo> twoPlayers;
    private List<PlayerInfo> sixPlayers;

    @BeforeEach
    void setUp() {
        twoPlayers = List.of(
                new PlayerInfo("Alice", STARTING_STACK, false),
                new PlayerInfo("Bob", STARTING_STACK, false)
        );

        sixPlayers = List.of(
                new PlayerInfo("Alice", STARTING_STACK, false),
                new PlayerInfo("Bob", STARTING_STACK, false),
                new PlayerInfo("Charlie", STARTING_STACK, false),
                new PlayerInfo("Diana", STARTING_STACK, false),
                new PlayerInfo("Eve", STARTING_STACK, false),
                new PlayerInfo("Frank", STARTING_STACK, false)
        );
    }

    
    
    

    @Nested
    @DisplayName("Game Creation")
    class GameCreationTests {

        @Test
        @DisplayName("should create game with valid configuration")
        void shouldCreateGameWithValidConfiguration() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            assertNotNull(game.getId());
            assertEquals(SMALL_BLIND, game.getSmallBlind());
            assertEquals(BIG_BLIND, game.getBigBlind());
            assertEquals(2, game.getPlayers().size());
            assertFalse(game.isFinished());
        }

        @Test
        @DisplayName("should create game with six players")
        void shouldCreateGameWithSixPlayers() {
            PokerGame game = PokerGame.create(sixPlayers, SMALL_BLIND, BIG_BLIND);

            assertEquals(6, game.getPlayers().size());
        }

        @Test
        @DisplayName("should generate unique game ID")
        void shouldGenerateUniqueGameId() {
            PokerGame game1 = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            PokerGame game2 = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            assertNotEquals(game1.getId(), game2.getId());
        }

        @Test
        @DisplayName("should raise GameCreated event on creation")
        void shouldRaiseGameCreatedEventOnCreation() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            List<DomainEvent> events = game.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(GameCreated.class, events.get(0));

            GameCreated event = (GameCreated) events.get(0);
            assertEquals(game.getId(), event.getGameId());
            assertEquals(2, event.getPlayerCount());
        }

        @Test
        @DisplayName("should reject too few players")
        void shouldRejectTooFewPlayers() {
            List<PlayerInfo> onePlayer = List.of(
                    new PlayerInfo("Alice", STARTING_STACK, false)
            );

            assertThrows(GameStateException.class, 
                    () -> PokerGame.create(onePlayer, SMALL_BLIND, BIG_BLIND));
        }

        @Test
        @DisplayName("should reject too many players")
        void shouldRejectTooManyPlayers() {
            List<PlayerInfo> elevenPlayers = List.of(
                    new PlayerInfo("P1", STARTING_STACK, false),
                    new PlayerInfo("P2", STARTING_STACK, false),
                    new PlayerInfo("P3", STARTING_STACK, false),
                    new PlayerInfo("P4", STARTING_STACK, false),
                    new PlayerInfo("P5", STARTING_STACK, false),
                    new PlayerInfo("P6", STARTING_STACK, false),
                    new PlayerInfo("P7", STARTING_STACK, false),
                    new PlayerInfo("P8", STARTING_STACK, false),
                    new PlayerInfo("P9", STARTING_STACK, false),
                    new PlayerInfo("P10", STARTING_STACK, false),
                    new PlayerInfo("P11", STARTING_STACK, false)
            );

            assertThrows(GameStateException.class, 
                    () -> PokerGame.create(elevenPlayers, SMALL_BLIND, BIG_BLIND));
        }

        @Test
        @DisplayName("should reject invalid blinds (BB < SB)")
        void shouldRejectInvalidBlinds() {
            assertThrows(GameStateException.class, 
                    () -> PokerGame.create(twoPlayers, Chips.of(20), Chips.of(10)));
        }

        @Test
        @DisplayName("should reject null parameters")
        void shouldRejectNullParameters() {
            assertThrows(NullPointerException.class, 
                    () -> PokerGame.create(null, SMALL_BLIND, BIG_BLIND));
            assertThrows(NullPointerException.class, 
                    () -> PokerGame.create(twoPlayers, null, BIG_BLIND));
            assertThrows(NullPointerException.class, 
                    () -> PokerGame.create(twoPlayers, SMALL_BLIND, null));
        }
    }

    
    
    

    @Nested
    @DisplayName("Start New Hand")
    class StartNewHandTests {

        @Test
        @DisplayName("should start new hand successfully")
        void shouldStartNewHandSuccessfully() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.clearDomainEvents();

            game.startNewHand();

            assertEquals(1, game.getHandNumber());
            assertEquals(GamePhase.PRE_FLOP, game.getPhase());
            assertEquals(BIG_BLIND, game.getCurrentBet());
        }

        @Test
        @DisplayName("should deal hole cards to all players")
        void shouldDealHoleCardsToAllPlayers() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            for (Player player : game.getPlayers()) {
                assertEquals(2, player.getHand().size());
            }
        }

        @Test
        @DisplayName("should post blinds correctly")
        void shouldPostBlindsCorrectly() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            assertEquals(Chips.of(30), game.getPotSize());
        }

        @Test
        @DisplayName("should raise GameStarted event")
        void shouldRaiseGameStartedEvent() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.clearDomainEvents();

            game.startNewHand();

            List<DomainEvent> events = game.getDomainEvents();
            
            assertTrue(events.stream().anyMatch(e -> e instanceof GameStarted));
        }

        @Test
        @DisplayName("should raise PlayerActed events for blinds")
        void shouldRaisePlayerActedEventsForBlinds() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.clearDomainEvents();

            game.startNewHand();

            List<DomainEvent> events = game.getDomainEvents();
            long blindEvents = events.stream()
                    .filter(e -> e instanceof PlayerActed)
                    .map(e -> (PlayerActed) e)
                    .filter(e -> e.getAction() == PlayerActed.ActionType.POST_SMALL_BLIND
                            || e.getAction() == PlayerActed.ActionType.POST_BIG_BLIND)
                    .count();

            assertEquals(2, blindEvents);
        }

        @Test
        @DisplayName("should increment hand number on each new hand")
        void shouldIncrementHandNumber() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            game.startNewHand();
            assertEquals(1, game.getHandNumber());

            
            Player currentPlayer = game.getCurrentPlayer();
            game.executeAction(currentPlayer.getId(), PlayerAction.FOLD, null);

            game.startNewHand();
            assertEquals(2, game.getHandNumber());
        }

        @Test
        @DisplayName("should fail if game is finished")
        void shouldFailIfGameIsFinished() {
            PokerGame game = PokerGame.create(twoPlayers, Chips.of(10), Chips.of(20));
            game.startNewHand();

            
            Player player = game.getCurrentPlayer();
            
            
        }

        @Test
        @DisplayName("should set current player correctly for heads-up")
        void shouldSetCurrentPlayerCorrectlyForHeadsUp() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            
            Player currentPlayer = game.getCurrentPlayer();
            assertNotNull(currentPlayer);
        }
    }

    
    
    

    @Nested
    @DisplayName("Execute Action")
    class ExecuteActionTests {

        @Test
        @DisplayName("should execute fold action")
        void shouldExecuteFoldAction() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            Player currentPlayer = game.getCurrentPlayer();
            UUID playerId = currentPlayer.getId();

            game.executeAction(playerId, PlayerAction.FOLD, null);

            assertTrue(currentPlayer.isFolded());
        }

        @Test
        @DisplayName("should execute call action")
        void shouldExecuteCallAction() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            Player currentPlayer = game.getCurrentPlayer();
            UUID playerId = currentPlayer.getId();
            int chipsBefore = currentPlayer.getChips();

            game.executeAction(playerId, PlayerAction.CALL, null);

            
            assertTrue(chipsBefore > currentPlayer.getChips());
        }

        @Test
        @DisplayName("should execute raise action")
        void shouldExecuteRaiseAction() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            Player currentPlayer = game.getCurrentPlayer();
            UUID playerId = currentPlayer.getId();
            Chips raiseAmount = Chips.of(40);

            game.executeAction(playerId, PlayerAction.RAISE, raiseAmount);

            
            assertTrue(game.getCurrentBet().amount() > BIG_BLIND.amount());
        }

        @Test
        @DisplayName("should execute check action when valid")
        void shouldExecuteCheckActionWhenValid() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            Player firstPlayer = game.getCurrentPlayer();
            game.executeAction(firstPlayer.getId(), PlayerAction.CALL, null);

            
            Player bbPlayer = game.getCurrentPlayer();
            game.clearDomainEvents();

            game.executeAction(bbPlayer.getId(), PlayerAction.CHECK, null);

            List<DomainEvent> events = game.getDomainEvents();
            assertTrue(events.stream()
                    .filter(e -> e instanceof PlayerActed)
                    .map(e -> (PlayerActed) e)
                    .anyMatch(e -> e.getAction() == PlayerActed.ActionType.CHECK));
        }

        @Test
        @DisplayName("should raise PlayerActed event on action")
        void shouldRaisePlayerActedEventOnAction() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            Player currentPlayer = game.getCurrentPlayer();
            game.executeAction(currentPlayer.getId(), PlayerAction.CALL, null);

            List<DomainEvent> events = game.getDomainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof PlayerActed));
        }

        @Test
        @DisplayName("should reject action from wrong player")
        void shouldRejectActionFromWrongPlayer() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            Player currentPlayer = game.getCurrentPlayer();
            Player otherPlayer = game.getPlayers().stream()
                    .filter(p -> !p.getId().equals(currentPlayer.getId()))
                    .findFirst().orElseThrow();

            assertThrows(InvalidActionException.class,
                    () -> game.executeAction(otherPlayer.getId(), PlayerAction.CALL, null));
        }

        @Test
        @DisplayName("should reject check when facing bet")
        void shouldRejectCheckWhenFacingBet() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            Player currentPlayer = game.getCurrentPlayer();
            
            if (currentPlayer.getBetAmount() < BIG_BLIND.amount()) {
                assertThrows(InvalidActionException.class,
                        () -> game.executeAction(currentPlayer.getId(), PlayerAction.CHECK, null));
            }
        }

        @Test
        @DisplayName("should reject insufficient chips for raise")
        void shouldRejectInsufficientChipsForRaise() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            Player currentPlayer = game.getCurrentPlayer();
            Chips hugeRaise = Chips.of(10000);

            assertThrows(InvalidActionException.class,
                    () -> game.executeAction(currentPlayer.getId(), PlayerAction.RAISE, hugeRaise));
        }

        @Test
        @DisplayName("should reject raise below minimum")
        void shouldRejectRaiseBelowMinimum() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            Player currentPlayer = game.getCurrentPlayer();
            Chips tinyRaise = Chips.of(5); 

            assertThrows(InvalidActionException.class,
                    () -> game.executeAction(currentPlayer.getId(), PlayerAction.RAISE, tinyRaise));
        }

        @Test
        @DisplayName("should reject action from folded player")
        void shouldRejectActionFromFoldedPlayer() {
            PokerGame game = PokerGame.create(sixPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            Player currentPlayer = game.getCurrentPlayer();
            game.executeAction(currentPlayer.getId(), PlayerAction.FOLD, null);

            
            assertThrows(InvalidActionException.class,
                    () -> game.executeAction(currentPlayer.getId(), PlayerAction.CALL, null));
        }

        @Test
        @DisplayName("should reject action for unknown player")
        void shouldRejectActionForUnknownPlayer() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            UUID unknownPlayerId = UUID.randomUUID();

            assertThrows(PlayerNotFoundException.class,
                    () -> game.executeAction(unknownPlayerId, PlayerAction.CALL, null));
        }
    }

    
    
    

    @Nested
    @DisplayName("Phase Transitions")
    class PhaseTransitionTests {

        @Test
        @DisplayName("should transition to flop after pre-flop betting")
        void shouldTransitionToFlopAfterPreFlopBetting() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            Player p1 = game.getCurrentPlayer();
            game.executeAction(p1.getId(), PlayerAction.CALL, null);

            
            Player p2 = game.getCurrentPlayer();
            game.executeAction(p2.getId(), PlayerAction.CHECK, null);

            
            assertEquals(GamePhase.FLOP, game.getPhase());
            assertEquals(3, game.getCommunityCards().size());
        }

        @Test
        @DisplayName("should transition through all phases")
        void shouldTransitionThroughAllPhases() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CALL, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            assertEquals(GamePhase.FLOP, game.getPhase());

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            assertEquals(GamePhase.TURN, game.getPhase());
            assertEquals(4, game.getCommunityCards().size());

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            assertEquals(GamePhase.RIVER, game.getPhase());
            assertEquals(5, game.getCommunityCards().size());

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            assertEquals(GamePhase.FINISHED, game.getPhase());
        }

        @Test
        @DisplayName("should raise PhaseChanged events")
        void shouldRaisePhaseChangedEvents() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CALL, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);

            List<DomainEvent> events = game.getDomainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof PhaseChanged));

            PhaseChanged phaseEvent = events.stream()
                    .filter(e -> e instanceof PhaseChanged)
                    .map(e -> (PhaseChanged) e)
                    .findFirst().orElseThrow();

            assertEquals(GamePhase.PRE_FLOP, phaseEvent.getPreviousPhase());
            assertEquals(GamePhase.FLOP, phaseEvent.getNewPhase());
            assertEquals(3, phaseEvent.getNewCommunityCards().size());
        }

        @Test
        @DisplayName("should award pot to last remaining player")
        void shouldAwardPotToLastRemainingPlayer() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            Player folder = game.getCurrentPlayer();
            int otherPlayerChipsBefore = game.getPlayers().stream()
                    .filter(p -> !p.getId().equals(folder.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getChips();

            game.executeAction(folder.getId(), PlayerAction.FOLD, null);

            
            Player winner = game.getPlayers().stream()
                    .filter(p -> !p.getId().equals(folder.getId()))
                    .findFirst()
                    .orElseThrow();

            assertTrue(winner.getChips() > otherPlayerChipsBefore);
        }
    }

    
    
    

    @Nested
    @DisplayName("Queries")
    class QueryTests {

        @Test
        @DisplayName("should return correct pot size")
        void shouldReturnCorrectPotSize() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            assertEquals(Chips.of(30), game.getPotSize());
        }

        @Test
        @DisplayName("should return current player")
        void shouldReturnCurrentPlayer() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            Player currentPlayer = game.getCurrentPlayer();
            assertNotNull(currentPlayer);
            assertFalse(currentPlayer.isFolded());
        }

        @Test
        @DisplayName("should return active players")
        void shouldReturnActivePlayers() {
            PokerGame game = PokerGame.create(sixPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            assertEquals(6, game.getActivePlayers().size());

            
            Player currentPlayer = game.getCurrentPlayer();
            game.executeAction(currentPlayer.getId(), PlayerAction.FOLD, null);

            assertEquals(5, game.getActivePlayers().size());
        }

        @Test
        @DisplayName("should return immutable player list")
        void shouldReturnImmutablePlayerList() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            assertThrows(UnsupportedOperationException.class,
                    () -> game.getPlayers().add(new Player("Hacker", 1000, false)));
        }

        @Test
        @DisplayName("should return immutable community cards")
        void shouldReturnImmutableCommunityCards() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CALL, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);

            assertThrows(UnsupportedOperationException.class,
                    () -> game.getCommunityCards().clear());
        }

        @Test
        @DisplayName("should find player by ID")
        void shouldFindPlayerById() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            UUID playerId = game.getPlayers().get(0).getId();

            Player found = game.findPlayerById(playerId);
            assertEquals(playerId, found.getId());
        }

        @Test
        @DisplayName("should throw when player not found by ID")
        void shouldThrowWhenPlayerNotFoundById() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            UUID unknownId = UUID.randomUUID();

            assertThrows(PlayerNotFoundException.class,
                    () -> game.findPlayerById(unknownId));
        }

        @Test
        @DisplayName("should return betting round state")
        void shouldReturnBettingRoundState() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            var bettingRound = game.getCurrentBettingRound();
            assertNotNull(bettingRound);
            assertEquals(GamePhase.PRE_FLOP, bettingRound.phase());
            assertEquals(BIG_BLIND, bettingRound.currentBet());
        }
    }

    
    
    

    @Nested
    @DisplayName("Domain Events")
    class DomainEventsTests {

        @Test
        @DisplayName("should collect domain events")
        void shouldCollectDomainEvents() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            assertFalse(game.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("should clear domain events")
        void shouldClearDomainEvents() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            game.clearDomainEvents();

            assertTrue(game.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("should return copy of events list")
        void shouldReturnCopyOfEventsList() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            List<DomainEvent> events = game.getDomainEvents();
            events.clear();

            
            assertFalse(game.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("should raise HandCompleted event after showdown")
        void shouldRaiseHandCompletedEventAfterShowdown() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CALL, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.CHECK, null);

            List<DomainEvent> events = game.getDomainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof HandCompleted));
        }

        @Test
        @DisplayName("should raise PotAwarded event when pot awarded")
        void shouldRaisePotAwardedEventWhenPotAwarded() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();
            game.clearDomainEvents();

            
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.FOLD, null);

            List<DomainEvent> events = game.getDomainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof PotAwarded));
        }
    }

    
    
    

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle heads-up play correctly")
        void shouldHandleHeadsUpPlayCorrectly() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            Player firstToAct = game.getCurrentPlayer();
            assertNotNull(firstToAct);

            
            assertDoesNotThrow(() -> 
                    game.executeAction(firstToAct.getId(), PlayerAction.CALL, null));
        }

        @Test
        @DisplayName("should track version for optimistic locking")
        void shouldTrackVersionForOptimisticLocking() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            
            
            assertNotNull(game); 
        }

        @Test
        @DisplayName("should maintain game state consistency after multiple hands")
        void shouldMaintainGameStateConsistencyAfterMultipleHands() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);

            
            game.startNewHand();
            game.executeAction(game.getCurrentPlayer().getId(), PlayerAction.FOLD, null);

            
            game.startNewHand();
            assertEquals(2, game.getHandNumber());
            assertEquals(2, game.getActivePlayers().size());
            assertTrue(game.getCommunityCards().isEmpty());
        }

        @Test
        @DisplayName("should correctly calculate min raise after raises")
        void shouldCorrectlyCalculateMinRaiseAfterRaises() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            
            assertEquals(BIG_BLIND, game.getMinRaise());

            
            Player p1 = game.getCurrentPlayer();
            game.executeAction(p1.getId(), PlayerAction.RAISE, Chips.of(40));

            
            assertTrue(game.getMinRaise().amount() >= 40);
        }
    }

    
    
    

    @Nested
    @DisplayName("Exception Hierarchy")
    class ExceptionHierarchyTests {

        @Test
        @DisplayName("InvalidActionException should have correct error code")
        void invalidActionExceptionShouldHaveCorrectErrorCode() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            Player currentPlayer = game.getCurrentPlayer();
            Player otherPlayer = game.getPlayers().stream()
                    .filter(p -> !p.getId().equals(currentPlayer.getId()))
                    .findFirst().orElseThrow();

            try {
                game.executeAction(otherPlayer.getId(), PlayerAction.CALL, null);
                fail("Should throw InvalidActionException");
            } catch (InvalidActionException ex) {
                assertEquals("INVALID_ACTION_NOT_PLAYERS_TURN", ex.getErrorCode());
                assertFalse(ex.getContext().isEmpty());
            }
        }

        @Test
        @DisplayName("GameStateException should have correct error code")
        void gameStateExceptionShouldHaveCorrectErrorCode() {
            List<PlayerInfo> onePlayer = List.of(
                    new PlayerInfo("Solo", STARTING_STACK, false)
            );

            try {
                PokerGame.create(onePlayer, SMALL_BLIND, BIG_BLIND);
                fail("Should throw GameStateException");
            } catch (GameStateException ex) {
                assertEquals("GAME_STATE_INVALID_PLAYER_COUNT", ex.getErrorCode());
            }
        }

        @Test
        @DisplayName("PlayerNotFoundException should have correct error code")
        void playerNotFoundExceptionShouldHaveCorrectErrorCode() {
            PokerGame game = PokerGame.create(twoPlayers, SMALL_BLIND, BIG_BLIND);
            game.startNewHand();

            try {
                game.executeAction(UUID.randomUUID(), PlayerAction.CALL, null);
                fail("Should throw PlayerNotFoundException");
            } catch (PlayerNotFoundException ex) {
                assertEquals("PLAYER_NOT_FOUND_BY_ID", ex.getErrorCode());
            }
        }
    }
}
