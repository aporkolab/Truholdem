package com.truholdem.dto;

import com.truholdem.model.Game;
import com.truholdem.model.GameUpdateType;

/**
 * DTO representing a game update message for WebSocket broadcasts.
 * Uses GameUpdateType enum for type-safe update categorization.
 */
public record WebSocketGameUpdateMessage(
    GameUpdateType type,
    Game game,
    Object payload,
    String message
) {}
