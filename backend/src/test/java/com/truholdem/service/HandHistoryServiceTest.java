package com.truholdem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.truholdem.model.Card;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.HandHistory;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;
import com.truholdem.repository.HandHistoryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("HandHistoryService Tests")
class HandHistoryServiceTest {

    @Mock
    private HandHistoryRepository handHistoryRepository;

    @InjectMocks
    private HandHistoryService handHistoryService;

    @Captor
    private ArgumentCaptor<HandHistory> handHistoryCaptor;

    private Game testGame;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        testGame = new Game();
        testGame.setId(UUID.randomUUID());
        testGame.setHandNumber(1);
        testGame.setSmallBlind(10);
        testGame.setBigBlind(20);
        testGame.setDealerPosition(0);

        player1 = new Player("Alice", 1000, false);
        player1.setId(UUID.randomUUID());
        player1.setSeatPosition(0);
        player1.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
        player1.addCardToHand(new Card(Suit.HEARTS, Value.KING));

        player2 = new Player("Bob", 1000, true);
        player2.setId(UUID.randomUUID());
        player2.setSeatPosition(1);
        player2.addCardToHand(new Card(Suit.SPADES, Value.QUEEN));
        player2.addCardToHand(new Card(Suit.SPADES, Value.JACK));

        testGame.addPlayer(player1);
        testGame.addPlayer(player2);
    }

    @Nested
    @DisplayName("Recording Tests")
    class RecordingTests {

        @Test
        @DisplayName("Should start recording a new hand")
        void shouldStartRecording() {
            
            handHistoryService.startRecording(testGame);

            
            handHistoryService.recordAction(
                    testGame.getId(), player1, PlayerAction.CALL, 20, GamePhase.PRE_FLOP);

            
            when(handHistoryRepository.save(any(HandHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            handHistoryService.finishRecording(
                    testGame.getId(), "Alice", "Pair of Aces", 100);

            verify(handHistoryRepository).save(handHistoryCaptor.capture());
            HandHistory saved = handHistoryCaptor.getValue();

            assertThat(saved.getGameId()).isEqualTo(testGame.getId());
            assertThat(saved.getHandNumber()).isEqualTo(1);
            assertThat(saved.getSmallBlind()).isEqualTo(10);
            assertThat(saved.getBigBlind()).isEqualTo(20);
            assertThat(saved.getPlayers()).hasSize(2);
        }

        @Test
        @DisplayName("Should record player actions")
        void shouldRecordActions() {
            handHistoryService.startRecording(testGame);

            
            handHistoryService.recordAction(
                    testGame.getId(), player1, PlayerAction.RAISE, 60, GamePhase.PRE_FLOP);
            handHistoryService.recordAction(
                    testGame.getId(), player2, PlayerAction.CALL, 60, GamePhase.PRE_FLOP);
            handHistoryService.recordAction(
                    testGame.getId(), player1, PlayerAction.BET, 100, GamePhase.FLOP);

            when(handHistoryRepository.save(any(HandHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            handHistoryService.finishRecording(
                    testGame.getId(), "Alice", "Two Pair", 220);

            verify(handHistoryRepository).save(handHistoryCaptor.capture());
            HandHistory saved = handHistoryCaptor.getValue();

            assertThat(saved.getActions()).hasSize(3);
            assertThat(saved.getActions().get(0).action()).isEqualTo("RAISE");
            assertThat(saved.getActions().get(0).amount()).isEqualTo(60);
            assertThat(saved.getActions().get(1).playerName()).isEqualTo("Bob");
            assertThat(saved.getActions().get(2).phase()).isEqualTo("FLOP");
        }

        @Test
        @DisplayName("Should record community cards")
        void shouldRecordCommunityCards() {
            handHistoryService.startRecording(testGame);

            List<Card> communityCards = Arrays.asList(
                    new Card(Suit.DIAMONDS, Value.TEN),
                    new Card(Suit.CLUBS, Value.NINE),
                    new Card(Suit.HEARTS, Value.EIGHT),
                    new Card(Suit.SPADES, Value.SEVEN),
                    new Card(Suit.DIAMONDS, Value.SIX));

            handHistoryService.recordCommunityCards(testGame.getId(), communityCards);

            when(handHistoryRepository.save(any(HandHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            handHistoryService.finishRecording(
                    testGame.getId(), "Alice", "Straight", 500);

            verify(handHistoryRepository).save(handHistoryCaptor.capture());
            HandHistory saved = handHistoryCaptor.getValue();

            assertThat(saved.getBoard()).hasSize(5);
        }

        @Test
        @DisplayName("Should handle recording for non-existent game gracefully")
        void shouldHandleNonExistentGame() {
            UUID nonExistentId = UUID.randomUUID();

            
            assertThatCode(() -> handHistoryService.recordAction(
                    nonExistentId, player1, PlayerAction.FOLD, 0, GamePhase.PRE_FLOP)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should finish recording with winner info")
        void shouldFinishRecordingWithWinnerInfo() {
            handHistoryService.startRecording(testGame);

            when(handHistoryRepository.save(any(HandHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            handHistoryService.finishRecording(
                    testGame.getId(), "Alice", "Royal Flush", 10000);

            verify(handHistoryRepository).save(handHistoryCaptor.capture());
            HandHistory saved = handHistoryCaptor.getValue();

            assertThat(saved.getWinnerName()).isEqualTo("Alice");
            assertThat(saved.getWinningHandDescription()).isEqualTo("Royal Flush");
            assertThat(saved.getFinalPot()).isEqualTo(10000);
            assertThat(saved.getPlayedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should get hand history by ID")
        void shouldGetHandHistoryById() {
            UUID historyId = UUID.randomUUID();
            HandHistory history = createTestHistory(historyId);

            when(handHistoryRepository.findById(historyId))
                    .thenReturn(Optional.of(history));

            Optional<HandHistory> result = handHistoryService.getHandHistory(historyId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(historyId);
        }

        @Test
        @DisplayName("Should get all hands for a game")
        void shouldGetGameHistory() {
            UUID gameId = UUID.randomUUID();
            List<HandHistory> histories = Arrays.asList(
                    createTestHistory(UUID.randomUUID()),
                    createTestHistory(UUID.randomUUID()),
                    createTestHistory(UUID.randomUUID()));

            when(handHistoryRepository.findByGameIdOrderByHandNumberDesc(gameId))
                    .thenReturn(histories);

            List<HandHistory> result = handHistoryService.getGameHistory(gameId);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Should get paged game history")
        void shouldGetPagedGameHistory() {
            UUID gameId = UUID.randomUUID();
            Page<HandHistory> page = new PageImpl<>(
                    Collections.singletonList(createTestHistory(UUID.randomUUID())));

            when(handHistoryRepository.findByGameId(eq(gameId), any(PageRequest.class)))
                    .thenReturn(page);

            Page<HandHistory> result = handHistoryService.getGameHistory(gameId, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get recent hands")
        void shouldGetRecentHands() {
            List<HandHistory> recentHands = Arrays.asList(
                    createTestHistory(UUID.randomUUID()),
                    createTestHistory(UUID.randomUUID()));

            when(handHistoryRepository.findTop50ByOrderByPlayedAtDesc())
                    .thenReturn(recentHands);

            List<HandHistory> result = handHistoryService.getRecentHands();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should get biggest pots")
        void shouldGetBiggestPots() {
            List<HandHistory> bigPots = Collections.singletonList(
                    createTestHistory(UUID.randomUUID()));

            when(handHistoryRepository.findTop10ByOrderByFinalPotDesc())
                    .thenReturn(bigPots);

            List<HandHistory> result = handHistoryService.getBiggestPots();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Replay Tests")
    class ReplayTests {

        @Test
        @DisplayName("Should generate replay data")
        void shouldGenerateReplayData() {
            UUID historyId = UUID.randomUUID();
            HandHistory history = createTestHistoryWithActions(historyId);

            when(handHistoryRepository.findById(historyId))
                    .thenReturn(Optional.of(history));

            HandHistoryService.ReplayData result = handHistoryService.generateReplayData(historyId);

            assertThat(result).isNotNull();
            assertThat(result.handNumber()).isEqualTo(history.getHandNumber());
            assertThat(result.actions()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return null for non-existent replay")
        void shouldReturnNullForNonExistentReplay() {
            UUID historyId = UUID.randomUUID();

            when(handHistoryRepository.findById(historyId))
                    .thenReturn(Optional.empty());

            HandHistoryService.ReplayData result = handHistoryService.generateReplayData(historyId);

            assertThat(result).isNull();
        }
    }

    
    private HandHistory createTestHistory(UUID id) {
        HandHistory history = new HandHistory();
        history.setId(id);
        history.setGameId(UUID.randomUUID());
        history.setHandNumber(1);
        history.setSmallBlind(10);
        history.setBigBlind(20);
        history.setPlayedAt(LocalDateTime.now());
        history.setWinnerName("TestWinner");
        history.setFinalPot(100);
        return history;
    }

    private HandHistory createTestHistoryWithActions(UUID id) {
        HandHistory history = createTestHistory(id);

        HandHistory.HandHistoryPlayer player = new HandHistory.HandHistoryPlayer();
        player.setPlayerId(UUID.randomUUID());
        player.setPlayerName("Alice");
        player.setStartingChips(1000);
        history.getPlayers().add(player);

        
        HandHistory.ActionRecord action = new HandHistory.ActionRecord(
                UUID.randomUUID(), 
                "Alice", 
                "RAISE", 
                60, 
                "PRE_FLOP", 
                LocalDateTime.now() 
        );
        history.getActions().add(action);

        return history;
    }
}
