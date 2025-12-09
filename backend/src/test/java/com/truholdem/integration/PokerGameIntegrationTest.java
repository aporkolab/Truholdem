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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Poker Game Integration Tests")
@Import({ TestConfig.class, TestSecurityConfig.class })
class PokerGameIntegrationTest {

    @Autowired
    private PokerGameService pokerGameService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private HandEvaluator handEvaluator;

    private Game game;
    private UUID gameId;
    private UUID player1Id;
    private UUID player2Id;
    private UUID player3Id;

    @BeforeEach
    void setUp() {
        List<PlayerInfo> players = List.of(
                new PlayerInfo("Alice", 1000, false),
                new PlayerInfo("Bot1", 1000, true),
                new PlayerInfo("Bot2", 1000, true));

        game = pokerGameService.createNewGame(players);
        gameId = game.getId();
        player1Id = game.getPlayers().get(0).getId();
        player2Id = game.getPlayers().get(1).getId();
        player3Id = game.getPlayers().get(2).getId();
    }

    @Test
    @Order(1)
    @DisplayName("Should create a valid game with 3 players")
    void shouldCreateValidGame() {
        assertNotNull(game);
        assertNotNull(game.getId());
        assertEquals(3, game.getPlayers().size());
        assertEquals(GamePhase.PRE_FLOP, game.getPhase());

        assertTrue(game.getCurrentPot() > 0, "Pot should have blinds");
        assertEquals(game.getBigBlind(), game.getCurrentBet());

        for (Player player : game.getPlayers()) {
            assertEquals(2, player.getHand().size(),
                    player.getName() + " should have 2 hole cards");
        }

        assertEquals(46, game.getDeck().size());
    }

    @Test
    @Order(2)
    @DisplayName("Should complete pre-flop betting round")
    void shouldCompletePreFlopBetting() {

        Player currentPlayer = game.getCurrentPlayer();
        assertNotNull(currentPlayer);

        game = executeCallForCurrentPlayer(game);
        game = executeCallForCurrentPlayer(game);
        game = executeCheckForCurrentPlayer(game);

        assertEquals(GamePhase.FLOP, game.getPhase());
        assertEquals(3, game.getCommunityCards().size());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle fold and award pot to remaining player")
    void shouldHandleFoldCorrectly() {

        game = pokerGameService.playerAct(gameId, player1Id, PlayerAction.FOLD, 0);
        assertTrue(game.getPlayers().get(0).isFolded());

        game = pokerGameService.playerAct(gameId, player2Id, PlayerAction.RAISE, 100);

        game = pokerGameService.playerAct(gameId, player3Id, PlayerAction.FOLD, 0);

        assertTrue(game.isFinished());
        assertEquals("Bot1", game.getWinnerName());
        assertEquals("All opponents folded", game.getWinningHandDescription());
    }

    @Test
    @Order(4)
    @DisplayName("Should complete full game to showdown")
    void shouldCompleteFullGameToShowdown() {

        game = playBettingRound(game, PlayerAction.CALL);

        assertEquals(GamePhase.FLOP, game.getPhase());
        assertEquals(3, game.getCommunityCards().size());

        game = playBettingRound(game, PlayerAction.CHECK);

        assertEquals(GamePhase.TURN, game.getPhase());
        assertEquals(4, game.getCommunityCards().size());

        game = playBettingRound(game, PlayerAction.CHECK);

        assertEquals(GamePhase.RIVER, game.getPhase());
        assertEquals(5, game.getCommunityCards().size());

        game = playBettingRound(game, PlayerAction.CHECK);

        assertEquals(GamePhase.SHOWDOWN, game.getPhase());
        assertTrue(game.isFinished());
        assertNotNull(game.getWinnerName());
        assertNotNull(game.getWinningHandDescription());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle raise and re-raise correctly")
    void shouldHandleRaiseAndReRaise() {

        int raiseAmount = 60;
        game = pokerGameService.playerAct(gameId, player1Id, PlayerAction.RAISE, raiseAmount);

        assertEquals(raiseAmount, game.getCurrentBet());
        Player player1 = findPlayer(game, player1Id);
        assertEquals(raiseAmount, player1.getBetAmount());

        int reRaiseAmount = 120;
        game = pokerGameService.playerAct(gameId, player2Id, PlayerAction.RAISE, reRaiseAmount);

        assertEquals(reRaiseAmount, game.getCurrentBet());

        game = pokerGameService.playerAct(gameId, player3Id, PlayerAction.CALL, 0);

        Player player3 = findPlayer(game, player3Id);
        assertEquals(reRaiseAmount, player3.getBetAmount());
    }

    @Test
    @Order(6)
    @DisplayName("Should handle all-in correctly")
    void shouldHandleAllIn() {

        Player player1 = findPlayer(game, player1Id);
        player1.setChips(50);

        game = pokerGameService.playerAct(gameId, player1Id, PlayerAction.RAISE, 60);

        player1 = findPlayer(game, player1Id);
        assertTrue(player1.isAllIn());
        assertEquals(0, player1.getChips());
    }

    @Test
    @Order(7)
    @DisplayName("Should start new hand correctly")
    void shouldStartNewHandCorrectly() {

        game = playBettingRound(game, PlayerAction.CALL);
        game = playBettingRound(game, PlayerAction.CHECK);
        game = playBettingRound(game, PlayerAction.CHECK);
        game = playBettingRound(game, PlayerAction.CHECK);

        assertTrue(game.isFinished());
        int oldHandNumber = game.getHandNumber();
        int oldDealerPos = game.getDealerPosition();

        Game newHand = pokerGameService.startNewHand(gameId);

        assertEquals(oldHandNumber + 1, newHand.getHandNumber());
        assertEquals((oldDealerPos + 1) % 3, newHand.getDealerPosition());
        assertEquals(GamePhase.PRE_FLOP, newHand.getPhase());
        assertFalse(newHand.isFinished());
        assertTrue(newHand.getCommunityCards().isEmpty());

        for (Player player : newHand.getPlayers()) {
            assertFalse(player.isFolded());
            assertEquals(2, player.getHand().size());
            assertFalse(player.isAllIn());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Should execute bot action correctly")
    void shouldExecuteBotActionCorrectly() {

        game = pokerGameService.playerAct(gameId, player1Id, PlayerAction.CALL, 0);

        game = pokerGameService.executeBotAction(gameId, player2Id);

        Player bot1 = findPlayer(game, player2Id);
        assertTrue(bot1.hasActed() || bot1.isFolded());
    }

    @Test
    @Order(9)
    @DisplayName("Should throw exception for invalid action")
    void shouldThrowExceptionForInvalidAction() {

        assertThrows(IllegalStateException.class, () -> {
            pokerGameService.playerAct(gameId, player1Id, PlayerAction.CHECK, 0);
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should throw exception when not player's turn")
    void shouldThrowExceptionWhenNotPlayersTurn() {

        assertThrows(IllegalStateException.class, () -> {
            pokerGameService.playerAct(gameId, player2Id, PlayerAction.FOLD, 0);
        });
    }

    private Game playBettingRound(Game game, PlayerAction action) {
        int maxIterations = 10;
        int iterations = 0;
        GamePhase startPhase = game.getPhase();

        while (game.getPhase() == startPhase && !game.isFinished() && iterations < maxIterations) {
            Player currentPlayer = game.getCurrentPlayer();
            if (currentPlayer == null || currentPlayer.isFolded()) {
                break;
            }

            try {
                if (action == PlayerAction.CHECK && currentPlayer.getBetAmount() < game.getCurrentBet()) {

                    game = pokerGameService.playerAct(gameId, currentPlayer.getId(), PlayerAction.CALL, 0);
                } else {
                    game = pokerGameService.playerAct(gameId, currentPlayer.getId(), action, 0);
                }
            } catch (Exception e) {

                game = pokerGameService.playerAct(gameId, currentPlayer.getId(), PlayerAction.CALL, 0);
            }

            iterations++;
        }

        return pokerGameService.getGame(gameId).orElse(game);
    }

    private Game executeCallForCurrentPlayer(Game game) {
        Player current = game.getCurrentPlayer();
        if (current != null && !current.isFolded()) {
            return pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CALL, 0);
        }
        return game;
    }

    private Game executeCheckForCurrentPlayer(Game game) {
        Player current = game.getCurrentPlayer();
        if (current != null && !current.isFolded() && current.getBetAmount() >= game.getCurrentBet()) {
            return pokerGameService.playerAct(gameId, current.getId(), PlayerAction.CHECK, 0);
        }
        return game;
    }

    private Player findPlayer(Game game, UUID playerId) {
        return game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow();
    }
}
