package com.truholdem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;


public record RegisterForTournamentRequest(
    @NotNull(message = "Player ID is required")
    UUID playerId,
    
    @NotBlank(message = "Player name is required")
    @Size(min = 2, max = 50, message = "Player name must be between 2 and 50 characters")
    String playerName
) {
}
