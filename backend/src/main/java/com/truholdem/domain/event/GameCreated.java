package com.truholdem.domain.event;

import com.truholdem.domain.value.Chips;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public final class GameCreated extends DomainEvent {

    private final List<UUID> playerIds;
    private final Chips startingStack;
    private final Chips smallBlind;
    private final Chips bigBlind;

    
    public GameCreated(UUID gameId, List<UUID> playerIds, Chips startingStack,
                       Chips smallBlind, Chips bigBlind) {
        super(gameId);
        this.playerIds = Collections.unmodifiableList(
                Objects.requireNonNull(playerIds, "Player IDs cannot be null"));
        this.startingStack = Objects.requireNonNull(startingStack, "Starting stack cannot be null");
        this.smallBlind = Objects.requireNonNull(smallBlind, "Small blind cannot be null");
        this.bigBlind = Objects.requireNonNull(bigBlind, "Big blind cannot be null");
    }

    public List<UUID> getPlayerIds() {
        return playerIds;
    }

    public int getPlayerCount() {
        return playerIds.size();
    }

    public Chips getStartingStack() {
        return startingStack;
    }

    public Chips getSmallBlind() {
        return smallBlind;
    }

    public Chips getBigBlind() {
        return bigBlind;
    }

    
    public Chips getTotalChipsInPlay() {
        return startingStack.multiply(playerIds.size());
    }

    @Override
    public String toString() {
        return String.format("GameCreated[gameId=%s, players=%d, stack=%s, blinds=%s/%s]",
                getGameId().toString().substring(0, 8),
                playerIds.size(),
                startingStack,
                smallBlind,
                bigBlind);
    }
}
