package com.truholdem.domain.exception;

import com.truholdem.model.GamePhase;

import java.util.Map;
import java.util.UUID;


public final class GameStateException extends GameDomainException {

    private GameStateException(String errorCode, String message) {
        super(errorCode, message);
    }

    private GameStateException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }

    
    
    

    
    public static GameStateException gameAlreadyFinished(UUID gameId) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_ALREADY_FINISHED",
                "Cannot perform action - game has already finished")
                .withGameId(gameId);
    }

    
    public static GameStateException notEnoughPlayers(UUID gameId, int currentPlayers, int requiredPlayers) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_NOT_ENOUGH_PLAYERS",
                String.format("Not enough players. Current: %d, Required: %d", currentPlayers, requiredPlayers))
                .withGameId(gameId)
                .withContext("currentPlayers", currentPlayers)
                .withContext("requiredPlayers", requiredPlayers);
    }

    
    public static GameStateException invalidPhaseTransition(UUID gameId, GamePhase currentPhase, GamePhase targetPhase) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_INVALID_PHASE_TRANSITION",
                String.format("Cannot transition from %s to %s", currentPhase, targetPhase))
                .withGameId(gameId)
                .withContext("currentPhase", currentPhase.name())
                .withContext("targetPhase", targetPhase.name());
    }

    
    public static GameStateException cannotStartNewHand(UUID gameId, String reason) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_CANNOT_START_HAND",
                String.format("Cannot start new hand: %s", reason))
                .withGameId(gameId)
                .withContext("reason", reason);
    }

    
    public static GameStateException gameNotStarted(UUID gameId) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_NOT_STARTED",
                "Game has not been started yet")
                .withGameId(gameId);
    }

    
    public static GameStateException gameAlreadyStarted(UUID gameId) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_ALREADY_STARTED",
                "Game has already started")
                .withGameId(gameId);
    }

    
    public static GameStateException handInProgress(UUID gameId, GamePhase currentPhase) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_HAND_IN_PROGRESS",
                String.format("Cannot perform action - hand is in progress (phase: %s)", currentPhase))
                .withGameId(gameId)
                .withContext("currentPhase", currentPhase.name());
    }

    
    public static GameStateException bettingRoundNotComplete(UUID gameId, int playersYetToAct) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_BETTING_NOT_COMPLETE",
                String.format("Betting round not complete. %d player(s) yet to act", playersYetToAct))
                .withGameId(gameId)
                .withContext("playersYetToAct", playersYetToAct);
    }

    
    public static GameStateException cannotDealCards(UUID gameId, GamePhase currentPhase) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_CANNOT_DEAL",
                String.format("Cannot deal cards in phase: %s", currentPhase))
                .withGameId(gameId)
                .withContext("currentPhase", currentPhase.name());
    }

    
    public static GameStateException cannotDetermineWinner(UUID gameId, String reason) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_CANNOT_DETERMINE_WINNER",
                String.format("Cannot determine winner: %s", reason))
                .withGameId(gameId)
                .withContext("reason", reason);
    }

    
    public static GameStateException invalidPlayerCount(int playerCount, int minPlayers, int maxPlayers) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_INVALID_PLAYER_COUNT",
                String.format("Invalid player count: %d. Must be between %d and %d", 
                        playerCount, minPlayers, maxPlayers))
                .withContext("playerCount", playerCount)
                .withContext("minPlayers", minPlayers)
                .withContext("maxPlayers", maxPlayers);
    }

    
    public static GameStateException invalidBlinds(int smallBlind, int bigBlind) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_INVALID_BLINDS",
                String.format("Invalid blinds: SB=%d, BB=%d. Big blind must be >= small blind", 
                        smallBlind, bigBlind))
                .withContext("smallBlind", smallBlind)
                .withContext("bigBlind", bigBlind);
    }

    
    public static GameStateException noActivePlayers(UUID gameId) {
        return (GameStateException) new GameStateException(
                "GAME_STATE_NO_ACTIVE_PLAYERS",
                "No active players remaining in the hand")
                .withGameId(gameId);
    }
}
