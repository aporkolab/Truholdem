package com.truholdem.dto;

import com.truholdem.model.PlayerInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;


public class CreateGameRequest {

    @NotNull(message = "Players list is required")
    @Size(min = 2, max = 10, message = "Player count must be between 2 and 10")
    @Valid
    private List<PlayerInfoDto> players;

    @Min(value = 1, message = "Small blind must be at least 1")
    @Max(value = 10000, message = "Small blind cannot exceed 10000")
    private int smallBlind = 10;

    @Min(value = 2, message = "Big blind must be at least 2")
    @Max(value = 20000, message = "Big blind cannot exceed 20000")
    private int bigBlind = 20;

    public CreateGameRequest() {
    }

    public CreateGameRequest(List<PlayerInfoDto> players) {
        this.players = players;
    }

    public CreateGameRequest(List<PlayerInfoDto> players, int smallBlind, int bigBlind) {
        this.players = players;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    public List<PlayerInfoDto> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfoDto> players) {
        this.players = players;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public void setSmallBlind(int smallBlind) {
        this.smallBlind = smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    public void setBigBlind(int bigBlind) {
        this.bigBlind = bigBlind;
    }

    
    public List<PlayerInfo> toPlayerInfoList() {
        return players.stream()
                .map(dto -> new PlayerInfo(dto.getName(), dto.getStartingChips(), dto.isBot()))
                .toList();
    }

    @AssertTrue(message = "Big blind must be at least double the small blind")
    public boolean isBigBlindValid() {
        return bigBlind >= smallBlind * 2;
    }

    
    public static class PlayerInfoDto {

        @NotBlank(message = "Player name is required")
        @Size(min = 1, max = 50, message = "Player name must be between 1 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_\\-\\s]+$", message = "Player name can only contain letters, numbers, underscores, hyphens, and spaces")
        private String name;

        @Min(value = 100, message = "Starting chips must be at least 100")
        @Max(value = 1000000, message = "Starting chips cannot exceed 1000000")
        private int startingChips = 1000;

        private boolean bot = false;

        public PlayerInfoDto() {
        }

        public PlayerInfoDto(String name, int startingChips, boolean bot) {
            this.name = name;
            this.startingChips = startingChips;
            this.bot = bot;
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

        public boolean isBot() {
            return bot;
        }

        public void setBot(boolean bot) {
            this.bot = bot;
        }
    }

    @Override
    public String toString() {
        return "CreateGameRequest{" +
                "players=" + players.size() +
                ", smallBlind=" + smallBlind +
                ", bigBlind=" + bigBlind +
                '}';
    }
}
