package com.truholdem.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


public abstract sealed class DomainEvent permits
        GameCreated,
        GameStarted,
        PlayerActed,
        PhaseChanged,
        PotAwarded,
        HandCompleted,
        PlayerEliminated {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID gameId;

    
    protected DomainEvent(UUID gameId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.gameId = Objects.requireNonNull(gameId, "Game ID cannot be null");
    }

    
    protected DomainEvent(UUID gameId, Instant occurredAt) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at cannot be null");
        this.gameId = Objects.requireNonNull(gameId, "Game ID cannot be null");
    }

    
    public UUID getEventId() {
        return eventId;
    }

    
    public Instant getOccurredAt() {
        return occurredAt;
    }

    
    public UUID getGameId() {
        return gameId;
    }

    
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s[eventId=%s, gameId=%s, occurredAt=%s]",
                getEventType(),
                eventId.toString().substring(0, 8),
                gameId.toString().substring(0, 8),
                occurredAt);
    }
}
