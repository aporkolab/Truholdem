package com.truholdem.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


public abstract sealed class TournamentEvent permits
        TournamentCreated,
        TournamentStarted,
        TournamentLevelAdvanced,
        TournamentPlayerRegistered,
        TournamentPlayerEliminated,
        TournamentTableCreated,
        TournamentTablesRebalanced,
        TournamentCompleted {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID tournamentId;

    protected TournamentEvent(UUID tournamentId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.tournamentId = Objects.requireNonNull(tournamentId, "Tournament ID cannot be null");
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getTournamentId() {
        return tournamentId;
    }

    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s[eventId=%s, tournamentId=%s, occurredAt=%s]",
                getEventType(),
                eventId.toString().substring(0, 8),
                tournamentId.toString().substring(0, 8),
                occurredAt);
    }
}
