package com.truholdem.controller;

import com.truholdem.dto.ShowdownResult;
import com.truholdem.model.*;
import com.truholdem.service.PokerGameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/poker")
@Tag(name = "Legacy Poker API", description = "Backward-compatible endpoints for the existing frontend")
public class LegacyPokerController {

    private static final Logger logger = LoggerFactory.getLogger(LegacyPokerController.class);

    private final PokerGameService pokerGameService;

    
    private UUID currentGameId;

    public LegacyPokerController(PokerGameService pokerGameService) {
        this.pokerGameService = pokerGameService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new game", description = "Creates a new poker game with the provided players")
    public ResponseEntity<Game> startGame(@RequestBody List<PlayerInfo> playersInfo) {
        logger.info("Starting new game with {} players", playersInfo.size());

        
        if (playersInfo == null || playersInfo.isEmpty()) {
            playersInfo = createDefaultPlayers();
        }

        Game game = pokerGameService.createNewGame(playersInfo);
        currentGameId = game.getId();

        return ResponseEntity.ok(game);
    }

    @GetMapping("/status")
    @Operation(summary = "Get current game status", description = "Returns the current game state")
    public ResponseEntity<Game> getGameStatus() {
        if (currentGameId == null) {
            return ResponseEntity.notFound().build();
        }

        return pokerGameService.getGame(currentGameId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/fold")
    @Operation(summary = "Fold", description = "Player folds their hand")
    public ResponseEntity<String> fold(@RequestParam UUID playerId) {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().body("No active game");
        }

        try {
            pokerGameService.playerAct(currentGameId, playerId, PlayerAction.FOLD, 0);
            return ResponseEntity.ok("Fold successful");
        } catch (Exception e) {
            logger.error("Error during fold", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/check")
    @Operation(summary = "Check", description = "Player checks (passes without betting)")
    public ResponseEntity<String> check(@RequestParam UUID playerId) {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().body("No active game");
        }

        try {
            pokerGameService.playerAct(currentGameId, playerId, PlayerAction.CHECK, 0);
            return ResponseEntity.ok("Check successful");
        } catch (Exception e) {
            logger.error("Error during check", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/call")
    @Operation(summary = "Call", description = "Player calls the current bet")
    public ResponseEntity<String> call(@RequestParam UUID playerId) {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().body("No active game");
        }

        try {
            pokerGameService.playerAct(currentGameId, playerId, PlayerAction.CALL, 0);
            return ResponseEntity.ok("Call successful");
        } catch (Exception e) {
            logger.error("Error during call", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/bet")
    @Operation(summary = "Bet", description = "Player places a bet")
    public ResponseEntity<String> bet(@RequestBody BetRequest request) {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().body("No active game");
        }

        try {
            Game game = pokerGameService.getGame(currentGameId).orElseThrow();
            PlayerAction action = game.getCurrentBet() > 0 ? PlayerAction.RAISE : PlayerAction.BET;

            pokerGameService.playerAct(currentGameId, request.getPlayerId(), action, request.getAmount());
            return ResponseEntity.ok("Bet successful");
        } catch (Exception e) {
            logger.error("Error during bet", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/raise")
    @Operation(summary = "Raise", description = "Player raises the bet")
    public ResponseEntity<String> raise(@RequestBody BetRequest request) {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().body("No active game");
        }

        try {
            pokerGameService.playerAct(currentGameId, request.getPlayerId(), PlayerAction.RAISE, request.getAmount());
            return ResponseEntity.ok("Raise successful");
        } catch (Exception e) {
            logger.error("Error during raise", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/bot-action/{botId}")
    @Operation(summary = "Execute bot action", description = "Triggers the AI to make a decision")
    public ResponseEntity<Game> botAction(@PathVariable UUID botId) {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Game game = pokerGameService.executeBotAction(currentGameId, botId);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            logger.error("Error during bot action", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/end")
    @Operation(summary = "End game", description = "Ends the current game and returns the winner")
    public ResponseEntity<Map<String, String>> endGame() {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No active game"));
        }

        try {
            Game game = pokerGameService.getGame(currentGameId).orElseThrow();

            
            if (!game.isFinished() && game.getPhase() == GamePhase.RIVER) {
                
                game.setPhase(GamePhase.SHOWDOWN);
                ShowdownResult result = pokerGameService.resolveShowdown(game);
                return ResponseEntity.ok(Map.of("message", "Winner: " + result.getMessage()));
            }

            String winner = game.getWinnerName() != null ? game.getWinnerName() : "Unknown";
            return ResponseEntity.ok(Map.of("message", "Winner: " + winner));
        } catch (Exception e) {
            logger.error("Error ending game", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset game", description = "Resets the current game")
    public ResponseEntity<String> resetGame(@RequestBody(required = false) Map<String, Object> options) {
        currentGameId = null;
        return ResponseEntity.ok("Game reset successful");
    }

    @PostMapping("/new-match")
    @Operation(summary = "Start new match", description = "Starts a new hand with existing players")
    public ResponseEntity<Game> newMatch() {
        if (currentGameId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Game game = pokerGameService.startNewHand(currentGameId);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            logger.error("Error starting new match", e);
            return ResponseEntity.badRequest().build();
        }
    }

    
    @GetMapping("/flop")
    @Operation(summary = "Deal flop (legacy)", description = "Kept for backward compatibility")
    public ResponseEntity<String> dealFlop() {
        return ResponseEntity.ok("Phase advancement is now automatic");
    }

    @GetMapping("/turn")
    @Operation(summary = "Deal turn (legacy)", description = "Kept for backward compatibility")
    public ResponseEntity<String> dealTurn() {
        return ResponseEntity.ok("Phase advancement is now automatic");
    }

    @GetMapping("/river")
    @Operation(summary = "Deal river (legacy)", description = "Kept for backward compatibility")
    public ResponseEntity<String> dealRiver() {
        return ResponseEntity.ok("Phase advancement is now automatic");
    }

    

    private List<PlayerInfo> createDefaultPlayers() {
        List<PlayerInfo> players = new ArrayList<>();
        players.add(new PlayerInfo("Player", 1000, false));
        players.add(new PlayerInfo("Bot1", 1000, true));
        players.add(new PlayerInfo("Bot2", 1000, true));
        return players;
    }

    

    public static class BetRequest {
        private UUID playerId;
        private int amount;

        public UUID getPlayerId() {
            return playerId;
        }

        public void setPlayerId(UUID playerId) {
            this.playerId = playerId;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }
}
