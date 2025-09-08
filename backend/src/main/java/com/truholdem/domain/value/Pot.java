package com.truholdem.domain.value;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


public record Pot(Chips amount, Set<UUID> eligiblePlayerIds, PotType type) {

    
    public enum PotType {
        
        MAIN,
        
        SIDE
    }

    
    public Pot {
        Objects.requireNonNull(amount, "Pot amount cannot be null");
        Objects.requireNonNull(type, "Pot type cannot be null");
        
        eligiblePlayerIds = eligiblePlayerIds != null 
            ? Collections.unmodifiableSet(new HashSet<>(eligiblePlayerIds))
            : Collections.emptySet();
    }

    
    public static Pot emptyMain() {
        return new Pot(Chips.zero(), Set.of(), PotType.MAIN);
    }

    
    public static Pot emptySide(Set<UUID> eligiblePlayerIds) {
        return new Pot(Chips.zero(), eligiblePlayerIds, PotType.SIDE);
    }

    
    public static Pot main(Chips amount, Set<UUID> eligiblePlayerIds) {
        return new Pot(amount, eligiblePlayerIds, PotType.MAIN);
    }

    
    public static Pot side(Chips amount, Set<UUID> eligiblePlayerIds) {
        return new Pot(amount, eligiblePlayerIds, PotType.SIDE);
    }

    
    public Pot addContribution(Chips contribution) {
        Objects.requireNonNull(contribution, "Contribution cannot be null");
        return new Pot(amount.add(contribution), eligiblePlayerIds, type);
    }

    
    public Pot addPlayerContribution(Chips contribution, UUID playerId) {
        Objects.requireNonNull(contribution, "Contribution cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        Set<UUID> newEligible = new HashSet<>(eligiblePlayerIds);
        newEligible.add(playerId);
        
        return new Pot(amount.add(contribution), newEligible, type);
    }

    
    public boolean isPlayerEligible(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return eligiblePlayerIds.contains(playerId);
    }

    
    public Pot removePlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        Set<UUID> newEligible = new HashSet<>(eligiblePlayerIds);
        newEligible.remove(playerId);
        
        return new Pot(amount, newEligible, type);
    }

    
    public int eligiblePlayerCount() {
        return eligiblePlayerIds.size();
    }

    
    public boolean isMain() {
        return type == PotType.MAIN;
    }

    
    public boolean isSide() {
        return type == PotType.SIDE;
    }

    
    public boolean isEmpty() {
        return amount.isZero();
    }

    
    public boolean hasChips() {
        return amount.hasChips();
    }

    
    public Chips splitShare(int winnerCount) {
        if (winnerCount <= 0) {
            throw new IllegalArgumentException("Winner count must be positive: " + winnerCount);
        }
        return Chips.of(amount.amount() / winnerCount);
    }

    
    public Chips splitRemainder(int winnerCount) {
        if (winnerCount <= 0) {
            throw new IllegalArgumentException("Winner count must be positive: " + winnerCount);
        }
        return Chips.of(amount.amount() % winnerCount);
    }

    @Override
    public String toString() {
        return type + " pot: " + amount + " (" + eligiblePlayerIds.size() + " eligible players)";
    }
}
