package com.truholdem.dto;

import java.util.UUID;

/**
 * DTO representing a player action message for WebSocket broadcasts.
 * Extracted to dto package to avoid circular dependency between service and websocket packages.
 */
public record PlayerActionMessageDto(
    UUID playerId,
    String playerName,
    String action,
    int amount,
    int remainingChips,
    int totalBet
) {}
