package com.truholdem.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.truholdem.config.TestConfig;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.PlayerInfo;
import com.truholdem.repository.GameRepository;
import com.truholdem.service.HandEvaluator;
import com.truholdem.service.PokerGameService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({ TestConfig.class, TestSecurityConfig.class })
@DisplayName("Full Game Integration Tests")
public class FullGameIntegrationTest {

    @Autowired
    private PokerGameService pokerGameService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private HandEvaluator handEvaluator;

    private Game game;
    private UUID gameId;

    @BeforeEach
    void setUp() {
        List<PlayerInfo> players = List.of(
                new PlayerInfo("Alice", 1000, false),
                new PlayerInfo("Bob", 1000, false),
                new PlayerInfo("Charlie", 1000, false));
        game = pokerGameService.createNewGame(players);
        gameId = game.getId();
    }

    @Test
    @DisplayName("Should create game with correct initial state")
    void shouldCreateGameWithCorrectInitialState() {
        assertNotNull(game.getId());
        assertEquals(3, game.getPlayers().size());
        assertEquals(GamePhase.PRE_FLOP, game.getPhase());

        assertTrue(game.getCurrentPot() > 0);
        assertEquals(game.getBigBlind(), game.getCurrentBet());

        for (Player player : game.getPlayers()) {
            assertEquals(2, player.getHand().size());
        }

        assertEquals(52 - 6, game.getDeck().size());
    }

    @Test
    @DisplayName("Should handle complete hand with all players calling")
    void shouldHandleCompleteHandWithAllPlayersCalling() {

        simulateAllPlayersCall();

        assertEquals(GamePhase.FLOP, game.getPhase());
        assertEquals(3, game.getCommunityCards().size());
        simulateAllPlayersCheck();

        assertEquals(GamePhase.TURN, game.getPhase());
        assertEquals(4, game.getCommunityCards().size());
        simulateAllPlayersCheck();

        assertEquals(GamePhase.RIVER, game.getPhase());
        assertEquals(5, game.getCommunityCards().size());
        simulateAllPlayersCheck();

        assertEquals(GamePhase.SHOWDOWN, game.getPhase());
        assertTrue(game.isFinished());
        assertNotNull(game.getWinnerName());
    }

    @Test
    @DisplayName("Should award pot when all but one player folds")
    void shouldAwardPotWhenAllButOneFolds() {
        Player player1 = game.getPlayers().get(0);
        Player player2 = game.getPlayers().get(1);
        Player player3 = game.getPlayers().get(2);

        int initialPot = game.getCurrentPot();

        if (game.getCurrentPlayer().getId().equals(player1.getId())) {
            game = pokerGameService.playerAct(gameId, player1.getId(), PlayerAction.FOLD, 0);
        }

        refreshGame();
        if (game.getCurrentPlayer().getId().equals(player2.getId())) {
            game = pokerGameService.playerAct(gameId, player2.getId(), PlayerAction.FOLD, 0);
        }

        refreshGame();

        assertTrue(game.isFinished());
        assertNotNull(game.getWinnerName());

        Player winner = game.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .findFirst()
                .orElseThrow();
        assertTrue(winner.getChips() > 1000 - game.getBigBlind());
    }

    @Test
    @DisplayName("Should handle raise and re-raise correctly")
    void shouldHandleRaiseAndReRaiseCorrectly() {
        Player currentPlayer = game.getCurrentPlayer();
        int initialBet = game.getCurrentBet();

        int raiseAmount = initialBet * 2;
        game = pokerGameService.playerAct(gameId, currentPlayer.getId(), PlayerAction.RAISE, raiseAmount);

        assertEquals(raiseAmount, game.getCurrentBet());

        refreshGame();
        Player nextPlayer = game.getCurrentPlayer();
        int reRaiseAmount = raiseAmount * 2;
        game = pokerGameService.playerAct(gameId, nextPlayer.getId(), PlayerAction.RAISE, reRaiseAmount);

        assertEquals(reRaiseAmount, game.getCurrentBet());
    }

    @Test
    @DisplayName("Should handle all-in correctly")
    void shouldHandleAllInCorrectly() {
        Player player = game.getCurrentPlayer();
        int playerChips = player.getChips();
        int totalBet = playerChips + player.getBetAmount();

        game = pokerGameService.playerAct(gameId, player.getId(), PlayerAction.RAISE, totalBet);

        refreshGame();
        Player updatedPlayer = game.getPlayers().stream()
                .filter(p -> p.getId().equals(player.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(0, updatedPlayer.getChips());
        assertTrue(updatedPlayer.isAllIn());
    }

    @Test
    @DisplayName("Should correctly start new hand after completion")
    void shouldCorrectlyStartNewHandAfterCompletion() {

        completeHand();

        game = pokerGameService.startNewHand(gameId);

        assertEquals(2, game.getHandNumber());
        assertEquals(GamePhase.PRE_FLOP, game.getPhase());
        assertFalse(game.isFinished());
        assertEquals(0, game.getCommunityCards().size());

        for (Player player : game.getPlayers()) {
            assertEquals(2, player.getHand().size());
            assertFalse(player.isFolded());
            assertFalse(player.isAllIn());
        }
    }

    @Test
    @DisplayName("Should rotate dealer position between hands")
    void shouldRotateDealerPositionBetweenHands() {
        int initialDealerPosition = game.getDealerPosition();

        completeHand();
        game = pokerGameService.startNewHand(gameId);

        int newDealerPosition = game.getDealerPosition();
        assertEquals((initialDealerPosition + 1) % game.getPlayers().size(), newDealerPosition);
    }

    @Test
    @DisplayName("Should validate player turn")
    void shouldValidatePlayerTurn() {
        Player currentPlayer = game.getCurrentPlayer();
        Player otherPlayer = game.getPlayers().stream()
                .filter(p -> !p.getId().equals(currentPlayer.getId()))
                .findFirst()
                .orElseThrow();

        assertThrows(IllegalStateException.class,
                () -> pokerGameService.playerAct(gameId, otherPlayer.getId(), PlayerAction.FOLD, 0));
    }

    private void simulateAllPlayersCall() {
        int actionsNeeded = game.getPlayers().size();
        for (int i = 0; i < actionsNeeded; i++) {
            refreshGame();
            if (game.getPhase() != GamePhase.PRE_FLOP)
                break;

            Player current = game.getCurrentPlayer();
            if (current != null && !current.isFolded() && !current.isAllIn()) {
                if (current.getBetAmount() < game.getCurrentBet()) {
                    game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
                } else {
                    game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
                }
            }
        }
    }

    private void simulateAllPlayersCheck() {
        GamePhase currentPhase = game.getPhase();
        int actionsNeeded = game.getPlayers().size();

        for (int i = 0; i < actionsNeeded; i++) {
            refreshGame();
            if (game.getPhase() != currentPhase)
                break;

            Player current = game.getCurrentPlayer();
            if (current != null && !current.isFolded() && !current.isAllIn()) {
                game = pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
            }
        }
    }

    private void completeHand() {
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
    }

    private void refreshGame() {
        game = pokerGameService.getGame(gameId).orElseThrow();
    }
}
