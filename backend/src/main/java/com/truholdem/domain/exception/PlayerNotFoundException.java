package com.truholdem.domain.exception;

import java.util.Map;
import java.util.UUID;


public final class PlayerNotFoundException extends GameDomainException {

    private PlayerNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }

    private PlayerNotFoundException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }

    
    
    

    
    public static PlayerNotFoundException byId(UUID playerId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_BY_ID",
                String.format("Player not found with ID: %s", playerId))
                .withPlayerId(playerId);
    }

    
    public static PlayerNotFoundException byIdInGame(UUID playerId, UUID gameId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_BY_ID",
                String.format("Player %s not found in game %s", 
                        abbreviate(playerId), abbreviate(gameId)))
                .withPlayerId(playerId)
                .withGameId(gameId);
    }

    
    public static PlayerNotFoundException byName(String playerName) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_BY_NAME",
                String.format("Player not found with name: %s", playerName))
                .withContext("playerName", playerName);
    }

    
    public static PlayerNotFoundException byNameInGame(String playerName, UUID gameId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_BY_NAME",
                String.format("Player '%s' not found in game %s", playerName, abbreviate(gameId)))
                .withContext("playerName", playerName)
                .withGameId(gameId);
    }

    
    public static PlayerNotFoundException notInGame(UUID playerId, UUID gameId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_IN_GAME",
                String.format("Player %s is not a participant in game %s", 
                        abbreviate(playerId), abbreviate(gameId)))
                .withPlayerId(playerId)
                .withGameId(gameId);
    }

    
    public static PlayerNotFoundException noCurrentPlayer(UUID gameId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_NO_CURRENT",
                "No current player - game may not be in progress")
                .withGameId(gameId);
    }

    
    public static PlayerNotFoundException atSeat(int seatPosition, UUID gameId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_AT_SEAT",
                String.format("No player at seat position %d", seatPosition))
                .withContext("seatPosition", seatPosition)
                .withGameId(gameId);
    }

    
    public static PlayerNotFoundException dealerNotFound(UUID gameId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_DEALER",
                "Dealer position player not found")
                .withGameId(gameId);
    }

    
    public static PlayerNotFoundException winnerNotFound(UUID gameId) {
        return (PlayerNotFoundException) new PlayerNotFoundException(
                "PLAYER_NOT_FOUND_WINNER",
                "Could not determine winner")
                .withGameId(gameId);
    }

    private static String abbreviate(UUID uuid) {
        return uuid != null ? uuid.toString().substring(0, 8) : "null";
    }
}
