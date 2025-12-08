package com.truholdem.service;

import com.truholdem.dto.ShowdownResult;
import com.truholdem.model.Game;
import com.truholdem.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;


@Service
public class GameNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(GameNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public GameNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    
    public void broadcastGameUpdate(Game game) {
        if (game == null || game.getId() == null) return;

        String destination = "/topic/game/" + game.getId();
        
        GameUpdateMessage message = new GameUpdateMessage(
            GameUpdateType.GAME_STATE,
            game,
            null,
            "Game state updated"
        );

        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Broadcast game update to {}", destination);
    }

    
    public void broadcastPlayerAction(Game game, Player player, String action, int amount) {
        if (game == null || game.getId() == null) return;

        String destination = "/topic/game/" + game.getId();

        PlayerActionMessage actionMessage = new PlayerActionMessage(
            player.getId(),
            player.getName(),
            action,
            amount,
            player.getChips(),
            player.getBetAmount()
        );

        GameUpdateMessage message = new GameUpdateMessage(
            GameUpdateType.PLAYER_ACTION,
            game,
            actionMessage,
            player.getName() + " performed " + action
        );

        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Broadcast player action: {} {} {}", player.getName(), action, amount);
    }

    
    public void broadcastPhaseChange(Game game) {
        if (game == null || game.getId() == null) return;

        String destination = "/topic/game/" + game.getId();

        GameUpdateMessage message = new GameUpdateMessage(
            GameUpdateType.PHASE_CHANGE,
            game,
            null,
            "Phase changed to " + game.getPhase()
        );

        messagingTemplate.convertAndSend(destination, message);
        logger.info("Broadcast phase change to {}: {}", destination, game.getPhase());
    }

    
    public void broadcastShowdown(Game game, ShowdownResult result) {
        if (game == null || game.getId() == null) return;

        String destination = "/topic/game/" + game.getId();

        GameUpdateMessage message = new GameUpdateMessage(
            GameUpdateType.SHOWDOWN,
            game,
            result,
            result.getMessage()
        );

        messagingTemplate.convertAndSend(destination, message);
        logger.info("Broadcast showdown result: {}", result.getMessage());
    }

    
    public void broadcastGameEnded(Game game, String winnerName) {
        if (game == null || game.getId() == null) return;

        String destination = "/topic/game/" + game.getId();

        GameUpdateMessage message = new GameUpdateMessage(
            GameUpdateType.GAME_ENDED,
            game,
            Map.of("winner", winnerName),
            "Game ended. Winner: " + winnerName
        );

        messagingTemplate.convertAndSend(destination, message);
        logger.info("Broadcast game ended: Winner {}", winnerName);
    }

    
    public void sendToUser(String username, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(username, destination, payload);
    }

    
    public void broadcastError(UUID gameId, String errorMessage) {
        if (gameId == null) return;

        String destination = "/topic/game/" + gameId + "/errors";

        messagingTemplate.convertAndSend(destination, Map.of(
            "type", "ERROR",
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        ));
    }

    

    public enum GameUpdateType {
        GAME_STATE,
        PLAYER_ACTION,
        PHASE_CHANGE,
        SHOWDOWN,
        GAME_ENDED,
        NEW_HAND,
        PLAYER_JOINED,
        PLAYER_LEFT
    }

    public record GameUpdateMessage(
        GameUpdateType type,
        Game game,
        Object payload,
        String message
    ) {}

    public record PlayerActionMessage(
        UUID playerId,
        String playerName,
        String action,
        int amount,
        int remainingChips,
        int totalBet
    ) {}
}
