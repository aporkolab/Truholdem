package com.truholdem.domain.event;

import com.truholdem.domain.value.Chips;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


public final class HandCompleted extends DomainEvent {

    
    public record PotResult(
            UUID winnerId,
            String winnerName,
            Chips amount,
            String handDescription,
            boolean isSidePot
    ) {
        public PotResult {
            Objects.requireNonNull(winnerId, "Winner ID cannot be null");
            Objects.requireNonNull(winnerName, "Winner name cannot be null");
            Objects.requireNonNull(amount, "Amount cannot be null");
        }
    }

    private final int handNumber;
    private final List<PotResult> potResults;
    private final Map<UUID, Chips> playerChipsAfter;
    private final Duration handDuration;
    private final int totalActions;
    private final boolean wentToShowdown;

    
    public HandCompleted(UUID gameId, int handNumber, List<PotResult> potResults,
                         Map<UUID, Chips> playerChipsAfter, Duration handDuration,
                         int totalActions, boolean wentToShowdown) {
        super(gameId);
        this.handNumber = handNumber;
        this.potResults = potResults != null 
                ? Collections.unmodifiableList(potResults) 
                : Collections.emptyList();
        this.playerChipsAfter = playerChipsAfter != null 
                ? Collections.unmodifiableMap(playerChipsAfter) 
                : Collections.emptyMap();
        this.handDuration = handDuration;
        this.totalActions = totalActions;
        this.wentToShowdown = wentToShowdown;
    }

    public int getHandNumber() {
        return handNumber;
    }

    public List<PotResult> getPotResults() {
        return potResults;
    }

    public Map<UUID, Chips> getPlayerChipsAfter() {
        return playerChipsAfter;
    }

    public Duration getHandDuration() {
        return handDuration;
    }

    public int getTotalActions() {
        return totalActions;
    }

    public boolean wentToShowdown() {
        return wentToShowdown;
    }

    
    public Chips getPlayerChips(UUID playerId) {
        return playerChipsAfter.get(playerId);
    }

    
    public Chips getTotalPotSize() {
        return potResults.stream()
                .map(PotResult::amount)
                .reduce(Chips.zero(), Chips::add);
    }

    
    public int getPotCount() {
        return potResults.size();
    }

    
    public boolean hadSidePots() {
        return potResults.stream().anyMatch(PotResult::isSidePot);
    }

    
    public long getHandDurationSeconds() {
        return handDuration != null ? handDuration.getSeconds() : 0;
    }

    
    public boolean isWinner(UUID playerId) {
        return potResults.stream()
                .anyMatch(pr -> pr.winnerId().equals(playerId));
    }

    
    public Chips getPlayerWinnings(UUID playerId) {
        return potResults.stream()
                .filter(pr -> pr.winnerId().equals(playerId))
                .map(PotResult::amount)
                .reduce(Chips.zero(), Chips::add);
    }

    @Override
    public String toString() {
        String showdownStr = wentToShowdown ? "showdown" : "no showdown";
        return String.format("HandCompleted[hand=#%d, pot=%s, %s, %d actions, %ds]",
                handNumber,
                getTotalPotSize(),
                showdownStr,
                totalActions,
                getHandDurationSeconds());
    }
}
