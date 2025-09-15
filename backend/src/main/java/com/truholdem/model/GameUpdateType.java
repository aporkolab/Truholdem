package com.truholdem.model;

/**
 * Enum representing the type of game update being broadcast.
 * Located in model package to be accessible by all layers.
 */
public enum GameUpdateType {
    GAME_STATE,
    PLAYER_ACTION,
    PHASE_CHANGE,
    SHOWDOWN,
    GAME_ENDED,
    NEW_HAND,
    PLAYER_JOINED,
    PLAYER_LEFT
}
