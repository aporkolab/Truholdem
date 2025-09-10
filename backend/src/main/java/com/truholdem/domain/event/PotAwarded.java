package com.truholdem.domain.event;

import com.truholdem.domain.value.Chips;
import com.truholdem.domain.value.Pot;

import java.util.Objects;
import java.util.UUID;


public final class PotAwarded extends DomainEvent {

    private final UUID winnerId;
    private final String winnerName;
    private final Chips amount;
    private final String handDescription;
    private final Pot.PotType potType;
    private final boolean wasSplitPot;
    private final int splitWinnerCount;

    
    public PotAwarded(UUID gameId, UUID winnerId, String winnerName, Chips amount,
                      String handDescription, Pot.PotType potType, 
                      boolean wasSplitPot, int splitWinnerCount) {
        super(gameId);
        this.winnerId = Objects.requireNonNull(winnerId, "Winner ID cannot be null");
        this.winnerName = Objects.requireNonNull(winnerName, "Winner name cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.handDescription = handDescription; 
        this.potType = Objects.requireNonNull(potType, "Pot type cannot be null");
        this.wasSplitPot = wasSplitPot;
        this.splitWinnerCount = splitWinnerCount;
    }

    
    public PotAwarded(UUID gameId, UUID winnerId, String winnerName, Chips amount,
                      String handDescription, Pot.PotType potType) {
        this(gameId, winnerId, winnerName, amount, handDescription, potType, false, 1);
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public Chips getAmount() {
        return amount;
    }

    public String getHandDescription() {
        return handDescription;
    }

    public Pot.PotType getPotType() {
        return potType;
    }

    public boolean wasSplitPot() {
        return wasSplitPot;
    }

    public int getSplitWinnerCount() {
        return splitWinnerCount;
    }

    
    public boolean isMainPot() {
        return potType == Pot.PotType.MAIN;
    }

    
    public boolean isSidePot() {
        return potType == Pot.PotType.SIDE;
    }

    
    public boolean wasWonWithoutShowdown() {
        return handDescription == null || handDescription.isEmpty();
    }

    @Override
    public String toString() {
        String handPart = handDescription != null 
                ? String.format(" with %s", handDescription) 
                : " (no showdown)";
        String splitPart = wasSplitPot 
                ? String.format(" (split %d ways)", splitWinnerCount) 
                : "";
        return String.format("PotAwarded[%s wins %s %s%s%s]",
                winnerName, potType, amount, handPart, splitPart);
    }
}
