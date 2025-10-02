package com.truholdem.dto;

import com.truholdem.model.Game;

public class GameUpdateMessage {
    private String type;
    private Game gameState;
    private String message;
    private long timestamp;

    public GameUpdateMessage() {}

    public GameUpdateMessage(String type, Game gameState, String message, long timestamp) {
        this.type = type;
        this.gameState = gameState;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Game getGameState() {
        return gameState;
    }

    public void setGameState(Game gameState) {
        this.gameState = gameState;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GameUpdateMessage{" +
                "type='" + type + '\'' +
                ", gameState=" + (gameState != null ? gameState.getId() : null) +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
