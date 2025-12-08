package com.truholdem.exception;

import org.springframework.http.HttpStatus;


public class GameException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public GameException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "GAME_ERROR";
    }

    public GameException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "GAME_ERROR";
    }

    public GameException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    
    public static GameException gameNotFound(String gameId) {
        return new GameException(
            "Game not found: " + gameId,
            HttpStatus.NOT_FOUND,
            "GAME_NOT_FOUND"
        );
    }

    public static GameException playerNotFound(String playerId) {
        return new GameException(
            "Player not found: " + playerId,
            HttpStatus.NOT_FOUND,
            "PLAYER_NOT_FOUND"
        );
    }

    public static GameException notPlayersTurn(String playerName) {
        return new GameException(
            "It is not " + playerName + "'s turn",
            HttpStatus.CONFLICT,
            "NOT_PLAYERS_TURN"
        );
    }

    public static GameException invalidAction(String reason) {
        return new GameException(
            "Invalid action: " + reason,
            HttpStatus.BAD_REQUEST,
            "INVALID_ACTION"
        );
    }

    public static GameException insufficientChips(int required, int available) {
        return new GameException(
            "Insufficient chips. Required: " + required + ", Available: " + available,
            HttpStatus.BAD_REQUEST,
            "INSUFFICIENT_CHIPS"
        );
    }

    public static GameException gameAlreadyFinished() {
        return new GameException(
            "Game has already finished",
            HttpStatus.CONFLICT,
            "GAME_FINISHED"
        );
    }

    public static GameException invalidBetAmount(String reason) {
        return new GameException(
            "Invalid bet amount: " + reason,
            HttpStatus.BAD_REQUEST,
            "INVALID_BET"
        );
    }
}
