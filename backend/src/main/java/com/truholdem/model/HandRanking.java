package com.truholdem.model;

import com.truholdem.model.HandType;
import com.truholdem.model.Value;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class HandRanking implements Comparable<HandRanking> {

    private final HandType handType;
    private final List<Value> rankValues;
    private final List<Value> kickerValues;

    public HandRanking(HandType handType, List<Value> rankValues, List<Value> kickerValues) {
        this.handType = handType;
        
        
        this.rankValues = List.copyOf(rankValues);
        this.kickerValues = kickerValues.stream().sorted(Comparator.reverseOrder()).toList();
    }

    public HandType getHandType() {
        return handType;
    }

    public List<Value> getRankValues() {
        return rankValues;
    }

    public List<Value> getKickerValues() {
        return kickerValues;
    }

    
    public String getDescription() {
        StringBuilder sb = new StringBuilder(handType.getDisplayName());
        
        switch (handType) {
            case HIGH_CARD:
                if (!kickerValues.isEmpty()) {
                    sb.append(" (").append(formatValue(kickerValues.get(0))).append(" high)");
                }
                break;
            case ONE_PAIR:
                if (!rankValues.isEmpty()) {
                    sb.append(" of ").append(formatValue(rankValues.get(0))).append("s");
                }
                break;
            case TWO_PAIR:
                if (rankValues.size() >= 2) {
                    sb.append(" (").append(formatValue(rankValues.get(0))).append("s and ")
                      .append(formatValue(rankValues.get(1))).append("s)");
                }
                break;
            case THREE_OF_A_KIND:
                if (!rankValues.isEmpty()) {
                    sb.append(" (").append(formatValue(rankValues.get(0))).append("s)");
                }
                break;
            case STRAIGHT:
            case STRAIGHT_FLUSH:
                if (!rankValues.isEmpty()) {
                    sb.append(" (").append(formatValue(rankValues.get(0))).append(" high)");
                }
                break;
            case FLUSH:
                if (!kickerValues.isEmpty()) {
                    sb.append(" (").append(formatValue(kickerValues.get(0))).append(" high)");
                }
                break;
            case FULL_HOUSE:
                if (rankValues.size() >= 2) {
                    sb.append(" (").append(formatValue(rankValues.get(0))).append("s full of ")
                      .append(formatValue(rankValues.get(1))).append("s)");
                }
                break;
            case FOUR_OF_A_KIND:
                if (!rankValues.isEmpty()) {
                    sb.append(" (").append(formatValue(rankValues.get(0))).append("s)");
                }
                break;
            case ROYAL_FLUSH:
                
                break;
        }
        
        return sb.toString();
    }

    private String formatValue(Value value) {
        return switch (value) {
            case ACE -> "Ace";
            case KING -> "King";
            case QUEEN -> "Queen";
            case JACK -> "Jack";
            case TEN -> "Ten";
            case NINE -> "Nine";
            case EIGHT -> "Eight";
            case SEVEN -> "Seven";
            case SIX -> "Six";
            case FIVE -> "Five";
            case FOUR -> "Four";
            case THREE -> "Three";
            case TWO -> "Two";
        };
    }

    @Override
    public int compareTo(HandRanking other) {
        int typeComparison = Integer.compare(this.handType.ordinal(), other.handType.ordinal());
        if (typeComparison != 0) {
            return typeComparison;
        }

        for (int i = 0; i < Math.min(this.rankValues.size(), other.rankValues.size()); i++) {
            int rankValueComparison = Integer.compare(
                this.rankValues.get(i).ordinal(),
                other.rankValues.get(i).ordinal()
            );
            if (rankValueComparison != 0) {
                return rankValueComparison;
            }
        }

        for (int i = 0; i < Math.min(this.kickerValues.size(), other.kickerValues.size()); i++) {
            int kickerComparison = Integer.compare(
                this.kickerValues.get(i).ordinal(),
                other.kickerValues.get(i).ordinal()
            );
            if (kickerComparison != 0) {
                return kickerComparison;
            }
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandRanking that = (HandRanking) o;
        return handType == that.handType &&
               Objects.equals(rankValues, that.rankValues) &&
               Objects.equals(kickerValues, that.kickerValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handType, rankValues, kickerValues);
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
