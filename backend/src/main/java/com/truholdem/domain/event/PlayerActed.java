package com.truholdem.domain.event;

import com.truholdem.domain.value.Chips;
import com.truholdem.model.GamePhase;

import java.util.Objects;
import java.util.UUID;


public final class PlayerActed extends DomainEvent {

    
    public enum ActionType {
        FOLD,
        CHECK,
        CALL,
        BET,
        RAISE,
        ALL_IN,
        POST_SMALL_BLIND,
        POST_BIG_BLIND
    }

    private final UUID playerId;
    private final String playerName;
    private final ActionType action;
    private final Chips amount;
    private final GamePhase phase;
    private final Chips potAfterAction;
    private final Chips playerChipsAfter;
    private final boolean isAllIn;

    
    public PlayerActed(UUID gameId, UUID playerId, String playerName, ActionType action,
                       Chips amount, GamePhase phase, Chips potAfterAction,
                       Chips playerChipsAfter, boolean isAllIn) {
        super(gameId);
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null");
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.phase = Objects.requireNonNull(phase, "Phase cannot be null");
        this.potAfterAction = Objects.requireNonNull(potAfterAction, "Pot after action cannot be null");
        this.playerChipsAfter = Objects.requireNonNull(playerChipsAfter, "Player chips after cannot be null");
        this.isAllIn = isAllIn;
    }

    
    public PlayerActed(UUID gameId, UUID playerId, String playerName, ActionType action,
                       Chips amount, GamePhase phase, Chips potAfterAction, Chips playerChipsAfter) {
        this(gameId, playerId, playerName, action, amount, phase, potAfterAction, playerChipsAfter, false);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public ActionType getAction() {
        return action;
    }

    public Chips getAmount() {
        return amount;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public Chips getPotAfterAction() {
        return potAfterAction;
    }

    public Chips getPlayerChipsAfter() {
        return playerChipsAfter;
    }

    public boolean isAllIn() {
        return isAllIn;
    }

    
    public boolean isAggressive() {
        return action == ActionType.BET || action == ActionType.RAISE;
    }

    
    public boolean isPassive() {
        return action == ActionType.CHECK || action == ActionType.CALL || action == ActionType.FOLD;
    }

    
    public boolean addedChips() {
        return amount.hasChips();
    }

    @Override
    public String toString() {
        String allInSuffix = isAllIn ? " (ALL-IN)" : "";
        if (amount.isZero()) {
            return String.format("PlayerActed[%s %s in %s%s]",
                    playerName, action, phase, allInSuffix);
        }
        return String.format("PlayerActed[%s %s %s in %s, pot=%s%s]",
                playerName, action, amount, phase, potAfterAction, allInSuffix);
    }
}
