package com.truholdem.domain.exception;

import com.truholdem.domain.value.Chips;

import java.util.Map;
import java.util.UUID;


public final class InvalidActionException extends GameDomainException {

    private InvalidActionException(String errorCode, String message) {
        super(errorCode, message);
    }

    private InvalidActionException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }

    
    
    

    
    public static InvalidActionException notPlayersTurn(UUID playerId, UUID currentPlayerId) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_NOT_PLAYERS_TURN",
                String.format("It's not player %s's turn. Current player is %s",
                        abbreviate(playerId), abbreviate(currentPlayerId)))
                .withContext("attemptedPlayerId", playerId)
                .withContext("currentPlayerId", currentPlayerId);
    }

    
    public static InvalidActionException cannotCheckFacingBet(UUID playerId, Chips currentBet, Chips playerBet) {
        Chips toCall = Chips.of(currentBet.amount() - playerBet.amount());
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_CANNOT_CHECK",
                String.format("Cannot check when facing a bet. Must call %s or fold", toCall))
                .withPlayerId(playerId)
                .withContext("currentBet", currentBet.amount())
                .withContext("playerBet", playerBet.amount())
                .withContext("amountToCall", toCall.amount());
    }

    
    public static InvalidActionException insufficientChips(UUID playerId, Chips required, Chips available) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_INSUFFICIENT_CHIPS",
                String.format("Insufficient chips. Required: %s, Available: %s", required, available))
                .withPlayerId(playerId)
                .withContext("requiredChips", required.amount())
                .withContext("availableChips", available.amount());
    }

    
    public static InvalidActionException invalidRaiseAmount(UUID playerId, Chips raiseAmount, Chips minimumRaise) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_RAISE_TOO_SMALL",
                String.format("Raise amount %s is below minimum raise of %s", raiseAmount, minimumRaise))
                .withPlayerId(playerId)
                .withContext("attemptedRaise", raiseAmount.amount())
                .withContext("minimumRaise", minimumRaise.amount());
    }

    
    public static InvalidActionException raiseExceedsStack(UUID playerId, Chips raiseAmount, Chips maximumRaise) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_RAISE_EXCEEDS_STACK",
                String.format("Raise amount %s exceeds available chips %s", raiseAmount, maximumRaise))
                .withPlayerId(playerId)
                .withContext("attemptedRaise", raiseAmount.amount())
                .withContext("maximumRaise", maximumRaise.amount());
    }

    
    public static InvalidActionException playerAlreadyFolded(UUID playerId) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_PLAYER_FOLDED",
                "Cannot act - player has already folded")
                .withPlayerId(playerId);
    }

    
    public static InvalidActionException playerIsAllIn(UUID playerId) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_PLAYER_ALL_IN",
                "Cannot act - player is all-in")
                .withPlayerId(playerId);
    }

    
    public static InvalidActionException cannotBetFacingBet(UUID playerId, Chips currentBet) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_CANNOT_BET",
                String.format("Cannot bet when facing a bet of %s. Must call, raise, or fold", currentBet))
                .withPlayerId(playerId)
                .withContext("currentBet", currentBet.amount());
    }

    
    public static InvalidActionException cannotRaiseNoBet(UUID playerId) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_CANNOT_RAISE",
                "Cannot raise when there is no bet. Use bet instead")
                .withPlayerId(playerId);
    }

    
    public static InvalidActionException invalidAction(UUID playerId, String action, String reason) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION",
                String.format("Invalid action '%s': %s", action, reason))
                .withPlayerId(playerId)
                .withContext("action", action)
                .withContext("reason", reason);
    }

    
    public static InvalidActionException invalidBetAmount(UUID playerId, int amount) {
        return (InvalidActionException) new InvalidActionException(
                "INVALID_ACTION_BET_AMOUNT",
                String.format("Bet amount must be positive: %d", amount))
                .withPlayerId(playerId)
                .withContext("attemptedAmount", amount);
    }

    private static String abbreviate(UUID uuid) {
        return uuid != null ? uuid.toString().substring(0, 8) : "null";
    }
}
