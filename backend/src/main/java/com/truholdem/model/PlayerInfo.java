package com.truholdem.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public class PlayerInfo {
    
    @NotBlank(message = "Player name is required")
    @Size(min = 1, max = 50, message = "Player name must be between 1 and 50 characters")
    private String name;
    
    @Min(value = 1, message = "Starting chips must be at least 1")
    @Max(value = 1000000, message = "Starting chips cannot exceed 1,000,000")
    private int startingChips;
    
    @JsonProperty("isBot")
    private boolean isBot;

    public PlayerInfo() {
    }

    public PlayerInfo(String name, int startingChips, boolean isBot) {
        this.name = name;
        this.startingChips = startingChips;
        this.isBot = isBot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStartingChips() {
        return startingChips;
    }

    public void setStartingChips(int startingChips) {
        this.startingChips = startingChips;
    }

    @JsonProperty("isBot")
    public boolean isBot() {
        return isBot;
    }

    @JsonProperty("isBot")
    public void setBot(boolean bot) {
        isBot = bot;
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "name='" + name + '\'' +
                ", startingChips=" + startingChips +
                ", isBot=" + isBot +
                '}';
    }
}
