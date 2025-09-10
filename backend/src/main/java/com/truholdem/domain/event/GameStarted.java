package com.truholdem.domain.event;

import java.util.Objects;
import java.util.UUID;


public final class GameStarted extends DomainEvent {

    private final int dealerPosition;
    private final UUID smallBlindPlayerId;
    private final UUID bigBlindPlayerId;
    private final int handNumber;

    
    public GameStarted(UUID gameId, int dealerPosition, UUID smallBlindPlayerId,
                       UUID bigBlindPlayerId, int handNumber) {
        super(gameId);
        this.dealerPosition = dealerPosition;
        this.smallBlindPlayerId = Objects.requireNonNull(smallBlindPlayerId, 
                "Small blind player ID cannot be null");
        this.bigBlindPlayerId = Objects.requireNonNull(bigBlindPlayerId, 
                "Big blind player ID cannot be null");
        this.handNumber = handNumber;
    }

    public int getDealerPosition() {
        return dealerPosition;
    }

    public UUID getSmallBlindPlayerId() {
        return smallBlindPlayerId;
    }

    public UUID getBigBlindPlayerId() {
        return bigBlindPlayerId;
    }

    public int getHandNumber() {
        return handNumber;
    }

    @Override
    public String toString() {
        return String.format("GameStarted[gameId=%s, hand=#%d, dealer=seat%d]",
                getGameId().toString().substring(0, 8),
                handNumber,
                dealerPosition);
    }
}
