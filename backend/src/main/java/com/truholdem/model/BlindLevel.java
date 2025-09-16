package com.truholdem.model;

import jakarta.persistence.Embeddable;
import java.util.Objects;


@Embeddable
public class BlindLevel {
    
    private int level;
    private int smallBlind;
    private int bigBlind;
    private int ante;
    
    
    protected BlindLevel() {}
    
    public BlindLevel(int level, int smallBlind, int bigBlind, int ante) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be positive");
        }
        if (smallBlind <= 0 || bigBlind <= 0) {
            throw new IllegalArgumentException("Blinds must be positive");
        }
        if (bigBlind < smallBlind) {
            throw new IllegalArgumentException("Big blind must be >= small blind");
        }
        if (ante < 0) {
            throw new IllegalArgumentException("Ante cannot be negative");
        }
        
        this.level = level;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.ante = ante;
    }
    
    
    public static BlindLevel of(int level, int smallBlind, int bigBlind) {
        return new BlindLevel(level, smallBlind, bigBlind, 0);
    }
    
    
    public static BlindLevel withAnte(int level, int smallBlind, int bigBlind, int ante) {
        return new BlindLevel(level, smallBlind, bigBlind, ante);
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getSmallBlind() {
        return smallBlind;
    }
    
    public int getBigBlind() {
        return bigBlind;
    }
    
    public int getAnte() {
        return ante;
    }
    
    public boolean hasAnte() {
        return ante > 0;
    }
    
    
    public int totalForcedBets(int playerCount) {
        return smallBlind + bigBlind + (ante * playerCount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlindLevel that = (BlindLevel) o;
        return level == that.level && 
               smallBlind == that.smallBlind && 
               bigBlind == that.bigBlind && 
               ante == that.ante;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(level, smallBlind, bigBlind, ante);
    }
    
    @Override
    public String toString() {
        String base = String.format("Level %d: %d/%d", level, smallBlind, bigBlind);
        return ante > 0 ? base + String.format(" (ante: %d)", ante) : base;
    }
}
