package com.truholdem.domain.event;

import com.truholdem.domain.value.Chips;
import com.truholdem.domain.value.Pot;
import com.truholdem.model.Card;
import com.truholdem.model.GamePhase;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@DisplayName("Domain Events")
class DomainEventsTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private final UUID player2Id = UUID.randomUUID();

    
    
    

    @Nested
    @DisplayName("DomainEvent Base Class")
    class DomainEventBaseTests {

        @Test
        @DisplayName("should generate unique event ID")
        void shouldGenerateUniqueEventId() {
            GameCreated event1 = new GameCreated(gameId, List.of(playerId), 
                    Chips.of(1000), Chips.of(10), Chips.of(20));
            GameCreated event2 = new GameCreated(gameId, List.of(playerId), 
                    Chips.of(1000), Chips.of(10), Chips.of(20));

            assertNotEquals(event1.getEventId(), event2.getEventId());
        }

        @Test
        @DisplayName("should set occurredAt timestamp")
        void shouldSetOccurredAtTimestamp() {
            Instant before = Instant.now();
            GameCreated event = new GameCreated(gameId, List.of(playerId), 
                    Chips.of(1000), Chips.of(10), Chips.of(20));
            Instant after = Instant.now();

            assertNotNull(event.getOccurredAt());
            assertTrue(event.getOccurredAt().compareTo(before) >= 0);
            assertTrue(event.getOccurredAt().compareTo(after) <= 0);
        }

        @Test
        @DisplayName("should return correct event type")
        void shouldReturnCorrectEventType() {
            GameCreated event = new GameCreated(gameId, List.of(playerId), 
                    Chips.of(1000), Chips.of(10), Chips.of(20));

            assertEquals("GameCreated", event.getEventType());
        }
    }

    
    
    

    @Nested
    @DisplayName("GameCreated Event")
    class GameCreatedTests {

        @Test
        @DisplayName("should create event with all properties")
        void shouldCreateEventWithAllProperties() {
            List<UUID> players = List.of(playerId, player2Id);
            Chips stack = Chips.of(1000);
            Chips sb = Chips.of(10);
            Chips bb = Chips.of(20);

            GameCreated event = new GameCreated(gameId, players, stack, sb, bb);

            assertEquals(gameId, event.getGameId());
            assertEquals(2, event.getPlayerCount());
            assertEquals(players, event.getPlayerIds());
            assertEquals(stack, event.getStartingStack());
            assertEquals(sb, event.getSmallBlind());
            assertEquals(bb, event.getBigBlind());
        }

        @Test
        @DisplayName("should calculate total chips in play")
        void shouldCalculateTotalChipsInPlay() {
            GameCreated event = new GameCreated(gameId, 
                    List.of(playerId, player2Id), 
                    Chips.of(1000), Chips.of(10), Chips.of(20));

            assertEquals(Chips.of(2000), event.getTotalChipsInPlay());
        }

        @Test
        @DisplayName("should have immutable player list")
        void shouldHaveImmutablePlayerList() {
            GameCreated event = new GameCreated(gameId, 
                    List.of(playerId), 
                    Chips.of(1000), Chips.of(10), Chips.of(20));

            assertThrows(UnsupportedOperationException.class, 
                    () -> event.getPlayerIds().add(player2Id));
        }

        @Test
        @DisplayName("should reject null parameters")
        void shouldRejectNullParameters() {
            assertThrows(NullPointerException.class, 
                    () -> new GameCreated(null, List.of(playerId), 
                            Chips.of(1000), Chips.of(10), Chips.of(20)));
            assertThrows(NullPointerException.class, 
                    () -> new GameCreated(gameId, null, 
                            Chips.of(1000), Chips.of(10), Chips.of(20)));
        }
    }

    
    
    

    @Nested
    @DisplayName("GameStarted Event")
    class GameStartedTests {

        @Test
        @DisplayName("should create event with all properties")
        void shouldCreateEventWithAllProperties() {
            GameStarted event = new GameStarted(gameId, 0, playerId, player2Id, 1);

            assertEquals(gameId, event.getGameId());
            assertEquals(0, event.getDealerPosition());
            assertEquals(playerId, event.getSmallBlindPlayerId());
            assertEquals(player2Id, event.getBigBlindPlayerId());
            assertEquals(1, event.getHandNumber());
        }
    }

    
    
    

    @Nested
    @DisplayName("PlayerActed Event")
    class PlayerActedTests {

        @Test
        @DisplayName("should create fold event")
        void shouldCreateFoldEvent() {
            PlayerActed event = new PlayerActed(gameId, playerId, "Player1",
                    PlayerActed.ActionType.FOLD, Chips.zero(), GamePhase.PRE_FLOP,
                    Chips.of(100), Chips.of(900));

            assertEquals(PlayerActed.ActionType.FOLD, event.getAction());
            assertTrue(event.isPassive());
            assertFalse(event.isAggressive());
            assertFalse(event.addedChips());
        }

        @Test
        @DisplayName("should create raise event")
        void shouldCreateRaiseEvent() {
            PlayerActed event = new PlayerActed(gameId, playerId, "Player1",
                    PlayerActed.ActionType.RAISE, Chips.of(100), GamePhase.PRE_FLOP,
                    Chips.of(150), Chips.of(800), false);

            assertEquals(PlayerActed.ActionType.RAISE, event.getAction());
            assertTrue(event.isAggressive());
            assertFalse(event.isPassive());
            assertTrue(event.addedChips());
        }

        @Test
        @DisplayName("should track all-in status")
        void shouldTrackAllInStatus() {
            PlayerActed allIn = new PlayerActed(gameId, playerId, "Player1",
                    PlayerActed.ActionType.ALL_IN, Chips.of(1000), GamePhase.PRE_FLOP,
                    Chips.of(1100), Chips.zero(), true);

            assertTrue(allIn.isAllIn());
        }

        @Test
        @DisplayName("should have correct toString")
        void shouldHaveCorrectToString() {
            PlayerActed event = new PlayerActed(gameId, playerId, "Hero",
                    PlayerActed.ActionType.RAISE, Chips.of(100), GamePhase.FLOP,
                    Chips.of(250), Chips.of(900));

            String str = event.toString();
            assertTrue(str.contains("Hero"));
            assertTrue(str.contains("RAISE"));
            assertTrue(str.contains("FLOP"));
        }
    }

    
    
    

    @Nested
    @DisplayName("PhaseChanged Event")
    class PhaseChangedTests {

        @Test
        @DisplayName("should create flop event with 3 cards")
        void shouldCreateFlopEventWith3Cards() {
            List<Card> flopCards = List.of(
                    new Card(Suit.HEARTS, Value.ACE),
                    new Card(Suit.SPADES, Value.KING),
                    new Card(Suit.DIAMONDS, Value.QUEEN)
            );

            PhaseChanged event = new PhaseChanged(gameId, GamePhase.PRE_FLOP, GamePhase.FLOP,
                    flopCards, flopCards, Chips.of(100), 3);

            assertTrue(event.isFlop());
            assertEquals(3, event.getNewCardCount());
            assertEquals(3, event.getAllCommunityCards().size());
        }

        @Test
        @DisplayName("should create turn event with 1 card")
        void shouldCreateTurnEventWith1Card() {
            List<Card> turnCard = List.of(new Card(Suit.CLUBS, Value.TEN));
            List<Card> allCards = List.of(
                    new Card(Suit.HEARTS, Value.ACE),
                    new Card(Suit.SPADES, Value.KING),
                    new Card(Suit.DIAMONDS, Value.QUEEN),
                    new Card(Suit.CLUBS, Value.TEN)
            );

            PhaseChanged event = new PhaseChanged(gameId, GamePhase.FLOP, GamePhase.TURN,
                    turnCard, allCards, Chips.of(200), 2);

            assertTrue(event.isTurn());
            assertEquals(1, event.getNewCardCount());
            assertEquals(4, event.getAllCommunityCards().size());
        }

        @Test
        @DisplayName("should identify showdown")
        void shouldIdentifyShowdown() {
            PhaseChanged event = new PhaseChanged(gameId, GamePhase.RIVER, GamePhase.SHOWDOWN,
                    List.of(), List.of(), Chips.of(500), 2);

            assertTrue(event.isShowdown());
            assertEquals(0, event.getNewCardCount());
        }

        @Test
        @DisplayName("should have immutable card lists")
        void shouldHaveImmutableCardLists() {
            PhaseChanged event = new PhaseChanged(gameId, GamePhase.PRE_FLOP, GamePhase.FLOP,
                    List.of(new Card(Suit.HEARTS, Value.ACE)), 
                    List.of(new Card(Suit.HEARTS, Value.ACE)),
                    Chips.of(100), 2);

            assertThrows(UnsupportedOperationException.class,
                    () -> event.getNewCommunityCards().add(new Card(Suit.SPADES, Value.KING)));
        }
    }

    
    
    

    @Nested
    @DisplayName("PotAwarded Event")
    class PotAwardedTests {

        @Test
        @DisplayName("should create main pot win event")
        void shouldCreateMainPotWinEvent() {
            PotAwarded event = new PotAwarded(gameId, playerId, "Winner",
                    Chips.of(500), "Full House, Kings over Tens", Pot.PotType.MAIN);

            assertTrue(event.isMainPot());
            assertFalse(event.isSidePot());
            assertFalse(event.wasWonWithoutShowdown());
            assertEquals("Full House, Kings over Tens", event.getHandDescription());
        }

        @Test
        @DisplayName("should create side pot win event")
        void shouldCreateSidePotWinEvent() {
            PotAwarded event = new PotAwarded(gameId, playerId, "Winner",
                    Chips.of(200), "Flush", Pot.PotType.SIDE);

            assertTrue(event.isSidePot());
            assertFalse(event.isMainPot());
        }

        @Test
        @DisplayName("should detect win without showdown")
        void shouldDetectWinWithoutShowdown() {
            PotAwarded event = new PotAwarded(gameId, playerId, "Winner",
                    Chips.of(100), null, Pot.PotType.MAIN);

            assertTrue(event.wasWonWithoutShowdown());
        }

        @Test
        @DisplayName("should track split pot")
        void shouldTrackSplitPot() {
            PotAwarded event = new PotAwarded(gameId, playerId, "Winner1",
                    Chips.of(250), "Straight", Pot.PotType.MAIN, true, 2);

            assertTrue(event.wasSplitPot());
            assertEquals(2, event.getSplitWinnerCount());
        }
    }

    
    
    

    @Nested
    @DisplayName("HandCompleted Event")
    class HandCompletedTests {

        @Test
        @DisplayName("should create hand completed event")
        void shouldCreateHandCompletedEvent() {
            List<HandCompleted.PotResult> results = List.of(
                    new HandCompleted.PotResult(playerId, "Winner", 
                            Chips.of(500), "Two Pair", false)
            );
            Map<UUID, Chips> chipsAfter = Map.of(
                    playerId, Chips.of(1500),
                    player2Id, Chips.of(500)
            );

            HandCompleted event = new HandCompleted(gameId, 1, results, chipsAfter,
                    Duration.ofSeconds(120), 15, true);

            assertEquals(1, event.getHandNumber());
            assertEquals(Chips.of(500), event.getTotalPotSize());
            assertTrue(event.wentToShowdown());
            assertEquals(15, event.getTotalActions());
            assertEquals(120, event.getHandDurationSeconds());
        }

        @Test
        @DisplayName("should check if player was winner")
        void shouldCheckIfPlayerWasWinner() {
            List<HandCompleted.PotResult> results = List.of(
                    new HandCompleted.PotResult(playerId, "Winner", 
                            Chips.of(500), "Flush", false)
            );

            HandCompleted event = new HandCompleted(gameId, 1, results, Map.of(),
                    Duration.ofMinutes(2), 10, true);

            assertTrue(event.isWinner(playerId));
            assertFalse(event.isWinner(player2Id));
        }

        @Test
        @DisplayName("should calculate player winnings")
        void shouldCalculatePlayerWinnings() {
            List<HandCompleted.PotResult> results = List.of(
                    new HandCompleted.PotResult(playerId, "Winner", 
                            Chips.of(300), "Pair", false),
                    new HandCompleted.PotResult(playerId, "Winner", 
                            Chips.of(200), "Pair", true)
            );

            HandCompleted event = new HandCompleted(gameId, 1, results, Map.of(),
                    Duration.ofMinutes(1), 8, true);

            assertEquals(Chips.of(500), event.getPlayerWinnings(playerId));
            assertTrue(event.hadSidePots());
        }

        @Test
        @DisplayName("should get player chips after hand")
        void shouldGetPlayerChipsAfterHand() {
            Map<UUID, Chips> chipsAfter = Map.of(
                    playerId, Chips.of(1200),
                    player2Id, Chips.of(800)
            );

            HandCompleted event = new HandCompleted(gameId, 1, List.of(), chipsAfter,
                    Duration.ofSeconds(90), 12, false);

            assertEquals(Chips.of(1200), event.getPlayerChips(playerId));
            assertEquals(Chips.of(800), event.getPlayerChips(player2Id));
            assertNull(event.getPlayerChips(UUID.randomUUID()));
        }
    }

    
    
    

    @Nested
    @DisplayName("PlayerEliminated Event")
    class PlayerEliminatedTests {

        @Test
        @DisplayName("should create elimination event")
        void shouldCreateEliminationEvent() {
            PlayerEliminated event = new PlayerEliminated(gameId, playerId, "Loser",
                    3, Chips.of(500), 25, player2Id, "Eliminator");

            assertEquals(playerId, event.getPlayerId());
            assertEquals("Loser", event.getPlayerName());
            assertEquals(3, event.getFinishPosition());
            assertEquals(Chips.of(500), event.getTotalWinnings());
            assertEquals(25, event.getHandsPlayed());
            assertTrue(event.hasEliminator());
            assertEquals(player2Id, event.getEliminatedByPlayerId());
        }

        @Test
        @DisplayName("should format position correctly")
        void shouldFormatPositionCorrectly() {
            assertEquals("1st", new PlayerEliminated(gameId, playerId, "P", 
                    1, Chips.zero(), 1).getPositionDisplay());
            assertEquals("2nd", new PlayerEliminated(gameId, playerId, "P", 
                    2, Chips.zero(), 1).getPositionDisplay());
            assertEquals("3rd", new PlayerEliminated(gameId, playerId, "P", 
                    3, Chips.zero(), 1).getPositionDisplay());
            assertEquals("4th", new PlayerEliminated(gameId, playerId, "P", 
                    4, Chips.zero(), 1).getPositionDisplay());
        }

        @Test
        @DisplayName("should check if in the money")
        void shouldCheckIfInTheMoney() {
            PlayerEliminated third = new PlayerEliminated(gameId, playerId, "P",
                    3, Chips.of(100), 10);
            PlayerEliminated fourth = new PlayerEliminated(gameId, playerId, "P",
                    4, Chips.of(50), 8);

            assertTrue(third.isInTheMoney(3));
            assertFalse(fourth.isInTheMoney(3));
        }

        @Test
        @DisplayName("should identify runner-up")
        void shouldIdentifyRunnerUp() {
            PlayerEliminated second = new PlayerEliminated(gameId, playerId, "P",
                    2, Chips.of(1000), 50);
            PlayerEliminated third = new PlayerEliminated(gameId, playerId, "P",
                    3, Chips.of(500), 40);

            assertTrue(second.wasRunnerUp());
            assertFalse(third.wasRunnerUp());
        }

        @Test
        @DisplayName("should handle missing eliminator")
        void shouldHandleMissingEliminator() {
            PlayerEliminated event = new PlayerEliminated(gameId, playerId, "P",
                    5, Chips.zero(), 15);

            assertFalse(event.hasEliminator());
            assertNull(event.getEliminatedByPlayerId());
        }
    }

    
    
    

    @Nested
    @DisplayName("DomainEventPublisher")
    class DomainEventPublisherTests {

        @Test
        @DisplayName("should publish single event")
        void shouldPublishSingleEvent() {
            ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
            DomainEventPublisher publisher = new DomainEventPublisher(mockPublisher);

            GameCreated event = new GameCreated(gameId, List.of(playerId),
                    Chips.of(1000), Chips.of(10), Chips.of(20));

            publisher.publish(event);

            verify(mockPublisher, times(1)).publishEvent(event);
        }

        @Test
        @DisplayName("should publish multiple events in order")
        void shouldPublishMultipleEventsInOrder() {
            ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
            DomainEventPublisher publisher = new DomainEventPublisher(mockPublisher);

            GameCreated event1 = new GameCreated(gameId, List.of(playerId),
                    Chips.of(1000), Chips.of(10), Chips.of(20));
            GameStarted event2 = new GameStarted(gameId, 0, playerId, player2Id, 1);

            publisher.publishAll(List.of(event1, event2));

            verify(mockPublisher, times(1)).publishEvent(event1);
            verify(mockPublisher, times(1)).publishEvent(event2);
        }

        @Test
        @DisplayName("should handle empty event list")
        void shouldHandleEmptyEventList() {
            ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
            DomainEventPublisher publisher = new DomainEventPublisher(mockPublisher);

            publisher.publishAll(List.of());

            verifyNoInteractions(mockPublisher);
        }

        @Test
        @DisplayName("should handle null event list")
        void shouldHandleNullEventList() {
            ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
            DomainEventPublisher publisher = new DomainEventPublisher(mockPublisher);

            publisher.publishAll((List<DomainEvent>) null);

            verifyNoInteractions(mockPublisher);
        }

        @Test
        @DisplayName("should reject null single event")
        void shouldRejectNullSingleEvent() {
            ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
            DomainEventPublisher publisher = new DomainEventPublisher(mockPublisher);

            assertThrows(NullPointerException.class, () -> publisher.publish((DomainEvent) null));
        }

        @Test
        @DisplayName("should return event from typed publish methods")
        void shouldReturnEventFromTypedPublishMethods() {
            ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
            DomainEventPublisher publisher = new DomainEventPublisher(mockPublisher);

            PlayerActed event = new PlayerActed(gameId, playerId, "Test",
                    PlayerActed.ActionType.CALL, Chips.of(20), GamePhase.PRE_FLOP,
                    Chips.of(60), Chips.of(980));

            PlayerActed returned = publisher.publish(event);

            assertSame(event, returned);
        }
    }

    
    
    

    @Nested
    @DisplayName("Sealed Class Hierarchy")
    class SealedClassTests {

        @Test
        @DisplayName("should support exhaustive pattern matching")
        void shouldSupportExhaustivePatternMatching() {
            DomainEvent event = new GameCreated(gameId, List.of(playerId),
                    Chips.of(1000), Chips.of(10), Chips.of(20));

            String result = switch (event) {
                case GameCreated gc -> "Created with " + gc.getPlayerCount() + " players";
                case GameStarted gs -> "Started hand " + gs.getHandNumber();
                case PlayerActed pa -> pa.getPlayerName() + " " + pa.getAction();
                case PhaseChanged pc -> pc.getPreviousPhase() + " -> " + pc.getNewPhase();
                case PotAwarded pw -> pw.getWinnerName() + " wins " + pw.getAmount();
                case HandCompleted hc -> "Hand " + hc.getHandNumber() + " completed";
                case PlayerEliminated pe -> pe.getPlayerName() + " eliminated";
            };

            assertEquals("Created with 1 players", result);
        }
    }
}
