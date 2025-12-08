package com.truholdem.dto;

import com.truholdem.model.PlayerAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public class PlayerActionRequest {

    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotNull(message = "Action is required")
    private PlayerAction action;

    @Min(value = 0, message = "Amount cannot be negative")
    private int amount;

    public PlayerActionRequest() {
    }

    public PlayerActionRequest(String playerId, PlayerAction action, int amount) {
        this.playerId = playerId;
        this.action = action;
        this.amount = amount;
    }

    
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public PlayerAction getAction() {
        return action;
    }

    public void setAction(PlayerAction action) {
        this.action = action;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "PlayerActionRequest{" +
                "playerId='" + playerId + '\'' +
                ", action=" + action +
                ", amount=" + amount +
                '}';
    }
}
