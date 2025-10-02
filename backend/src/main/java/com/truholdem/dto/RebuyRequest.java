package com.truholdem.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;


public record RebuyRequest(
    @NotNull(message = "Player ID is required")
    UUID playerId
) {
}
