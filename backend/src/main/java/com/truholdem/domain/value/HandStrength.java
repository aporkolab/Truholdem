package com.truholdem.domain.value;

import com.truholdem.model.HandType;
import com.truholdem.model.HandRanking;

import java.util.Objects;


public record HandStrength(
    double equity,
    HandType ranking,
    String description
) implements Comparable<HandStrength> {

    
    public static final double PREMIUM_THRESHOLD = 0.80;
    
    
    public static final double STRONG_THRESHOLD = 0.60;
    
    
    public static final double MEDIUM_THRESHOLD = 0.40;
    
    
    public static final double PLAYABLE_THRESHOLD = 0.25;

    
    public HandStrength {
        if (equity < 0.0 || equity > 1.0) {
            throw new IllegalArgumentException("Equity must be between 0 and 1: " + equity);
        }
        Objects.requireNonNull(ranking, "Hand ranking cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
    }

    
    public static HandStrength of(HandRanking handRanking, double equity) {
        Objects.requireNonNull(handRanking, "Hand ranking cannot be null");
        return new HandStrength(equity, handRanking.getHandType(), handRanking.getDescription());
    }

    
    public static HandStrength of(HandType handType, double equity, String description) {
        return new HandStrength(equity, handType, description);
    }

    
    public static HandStrength fromRanking(HandRanking handRanking) {
        Objects.requireNonNull(handRanking, "Hand ranking cannot be null");
        double estimatedEquity = estimateEquityFromType(handRanking.getHandType());
        return new HandStrength(estimatedEquity, handRanking.getHandType(), handRanking.getDescription());
    }

    
    private static double estimateEquityFromType(HandType handType) {
        return switch (handType) {
            case ROYAL_FLUSH -> 1.00;
            case STRAIGHT_FLUSH -> 0.98;
            case FOUR_OF_A_KIND -> 0.95;
            case FULL_HOUSE -> 0.90;
            case FLUSH -> 0.85;
            case STRAIGHT -> 0.80;
            case THREE_OF_A_KIND -> 0.70;
            case TWO_PAIR -> 0.60;
            case ONE_PAIR -> 0.45;
            case HIGH_CARD -> 0.25;
        };
    }

    
    public boolean isPremium() {
        return equity >= PREMIUM_THRESHOLD;
    }

    
    public boolean isStrong() {
        return equity >= STRONG_THRESHOLD;
    }

    
    public boolean isMedium() {
        return equity >= MEDIUM_THRESHOLD && equity < STRONG_THRESHOLD;
    }

    
    public boolean isWeak() {
        return equity < MEDIUM_THRESHOLD;
    }

    
    public boolean isPlayable() {
        return equity >= PLAYABLE_THRESHOLD;
    }

    
    public boolean hasPotOdds(double potOdds) {
        if (potOdds < 0.0 || potOdds > 1.0) {
            throw new IllegalArgumentException("Pot odds must be between 0 and 1: " + potOdds);
        }
        return equity > potOdds;
    }

    
    public static double calculatePotOdds(Chips potSize, Chips callAmount) {
        Objects.requireNonNull(potSize, "Pot size cannot be null");
        Objects.requireNonNull(callAmount, "Call amount cannot be null");
        
        if (callAmount.isZero()) {
            return 0.0; 
        }
        
        int totalPot = potSize.amount() + callAmount.amount();
        return (double) callAmount.amount() / totalPot;
    }

    
    public boolean beats(HandStrength other) {
        Objects.requireNonNull(other, "Cannot compare with null hand strength");
        return this.compareTo(other) > 0;
    }

    
    public String getStrengthCategory() {
        if (isPremium()) return "Premium";
        if (isStrong()) return "Strong";
        if (isMedium()) return "Medium";
        if (isPlayable()) return "Marginal";
        return "Weak";
    }

    
    public String getEquityPercentage() {
        return String.format("%.1f%%", equity * 100);
    }

    
    public boolean isMadeHand() {
        return ranking.ordinal() >= HandType.ONE_PAIR.ordinal();
    }

    
    public boolean isMonster() {
        return ranking.ordinal() >= HandType.FULL_HOUSE.ordinal();
    }

    
    public boolean isPotentialDraw() {
        return ranking == HandType.HIGH_CARD || ranking == HandType.ONE_PAIR;
    }

    @Override
    public int compareTo(HandStrength other) {
        
        int rankCompare = Integer.compare(this.ranking.ordinal(), other.ranking.ordinal());
        if (rankCompare != 0) {
            return rankCompare;
        }
        
        return Double.compare(this.equity, other.equity);
    }

    @Override
    public String toString() {
        return String.format("%s (%s, equity: %s)", 
            description, getStrengthCategory(), getEquityPercentage());
    }
}
