package com.truholdem.controller;

import com.truholdem.dto.GameUpdateMessage;
import com.truholdem.dto.PlayerActionRequest;
import com.truholdem.model.Game;
import com.truholdem.service.PokerGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class GameWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketController.class);

    private final PokerGameService pokerGameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(PokerGameService pokerGameService, SimpMessagingTemplate messagingTemplate) {
        this.pokerGameService = pokerGameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/{gameId}/action")
    @SendTo("/topic/game/{gameId}")
    public GameUpdateMessage handlePlayerAction(
            @DestinationVariable UUID gameId,
            @Payload PlayerActionRequest actionRequest,
            Principal principal) {
        
        logger.info("WebSocket action received for game {}: {} by user {}", 
                gameId, actionRequest.getAction(), principal.getName());

        try {
            // Execute the action
            Game updatedGame = pokerGameService.playerAct(
                gameId, 
                UUID.fromString(actionRequest.getPlayerId()), 
                actionRequest.getAction(), 
                actionRequest.getAmount()
            );

            return new GameUpdateMessage(
                "GAME_UPDATE",
                updatedGame,
                "Player action executed: " + actionRequest.getAction(),
                System.currentTimeMillis()
            );

        } catch (Exception e) {
            logger.error("Error handling player action in game {}: {}", gameId, e.getMessage());
            
            return new GameUpdateMessage(
                "ERROR",
                null,
                "Error executing action: " + e.getMessage(),
                System.currentTimeMillis()
            );
        }
    }

    @SubscribeMapping("/topic/game/{gameId}")
    public GameUpdateMessage onGameSubscription(@DestinationVariable UUID gameId, Principal principal) {
        logger.info("User {} subscribed to game {}", principal.getName(), gameId);
        
        try {
            Game game = pokerGameService.getGame(gameId).orElse(null);
            if (game != null) {
                return new GameUpdateMessage(
                    "GAME_STATE",
                    game,
                    "Current game state",
                    System.currentTimeMillis()
                );
            } else {
                return new GameUpdateMessage(
                    "ERROR",
                    null,
                    "Game not found",
                    System.currentTimeMillis()
                );
            }
        } catch (Exception e) {
            logger.error("Error getting game state for subscription: {}", e.getMessage());
            return new GameUpdateMessage(
                "ERROR",
                null,
                "Error getting game state",
                System.currentTimeMillis()
            );
        }
    }

    @MessageMapping("/game/{gameId}/join")
    @SendTo("/topic/game/{gameId}")
    public GameUpdateMessage handlePlayerJoin(
            @DestinationVariable UUID gameId,
            @Payload String playerName,
            Principal principal) {
        
        logger.info("Player {} joining game {}", playerName, gameId);
        
        // Notify other players about the new player
        return new GameUpdateMessage(
            "PLAYER_JOINED",
            null,
            playerName + " joined the game",
            System.currentTimeMillis()
        );
    }

    @MessageMapping("/game/{gameId}/leave")
    @SendTo("/topic/game/{gameId}")
    public GameUpdateMessage handlePlayerLeave(
            @DestinationVariable UUID gameId,
            @Payload String playerName,
            Principal principal) {
        
        logger.info("Player {} leaving game {}", playerName, gameId);
        
        // Notify other players about the player leaving
        return new GameUpdateMessage(
            "PLAYER_LEFT",
            null,
            playerName + " left the game",
            System.currentTimeMillis()
        );
    }

    // Method to send game updates programmatically from services
    public void sendGameUpdate(UUID gameId, Game updatedGame, String message) {
        GameUpdateMessage updateMessage = new GameUpdateMessage(
            "GAME_UPDATE",
            updatedGame,
            message,
            System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updateMessage);
        logger.debug("Sent game update for game {}: {}", gameId, message);
    }

    // Method to send error messages
    public void sendGameError(UUID gameId, String errorMessage) {
        GameUpdateMessage errorMsg = new GameUpdateMessage(
            "ERROR",
            null,
            errorMessage,
            System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSend("/topic/game/" + gameId, errorMsg);
        logger.debug("Sent game error for game {}: {}", gameId, errorMessage);
    }

    // Method to send user-specific messages
    public void sendUserMessage(String username, String message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/messages", message);
        logger.debug("Sent personal message to user {}: {}", username, message);
    }
}
