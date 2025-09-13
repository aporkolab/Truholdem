package com.truholdem.model;

import java.util.*;


public class HandRange {

    private final Set<HandCombo> includedHands;
    private final String description;

    private HandRange(Set<HandCombo> hands, String description) {
        this.includedHands = Collections.unmodifiableSet(new HashSet<>(hands));
        this.description = description;
    }




    public static HandRange premiumRange() {
        Set<HandCombo> hands = new HashSet<>();

        hands.add(HandCombo.pocketPair(Value.ACE));
        hands.add(HandCombo.pocketPair(Value.KING));
        hands.add(HandCombo.pocketPair(Value.QUEEN));

        hands.add(HandCombo.suited(Value.ACE, Value.KING));
        return new HandRange(hands, "Premium (AA, KK, QQ, AKs)");
    }


    public static HandRange earlyPositionOpen() {
        Set<HandCombo> hands = new HashSet<>();

        for (Value v : List.of(Value.ACE, Value.KING, Value.QUEEN, Value.JACK, Value.TEN)) {
            hands.add(HandCombo.pocketPair(v));
        }

        hands.add(HandCombo.any(Value.ACE, Value.KING));
        hands.add(HandCombo.any(Value.ACE, Value.QUEEN));
        hands.add(HandCombo.suited(Value.ACE, Value.JACK));
        hands.add(HandCombo.suited(Value.ACE, Value.TEN));

        hands.add(HandCombo.suited(Value.KING, Value.QUEEN));
        return new HandRange(hands, "Early Position Open (~12%)");
    }


    public static HandRange middlePositionOpen() {
        Set<HandCombo> hands = new HashSet<>(earlyPositionOpen().includedHands);

        hands.add(HandCombo.pocketPair(Value.NINE));
        hands.add(HandCombo.pocketPair(Value.EIGHT));

        hands.add(HandCombo.any(Value.ACE, Value.JACK));
        hands.add(HandCombo.suited(Value.ACE, Value.TEN));
        hands.add(HandCombo.any(Value.KING, Value.QUEEN));
        hands.add(HandCombo.suited(Value.KING, Value.JACK));
        hands.add(HandCombo.suited(Value.QUEEN, Value.JACK));

        hands.add(HandCombo.suited(Value.JACK, Value.TEN));
        hands.add(HandCombo.suited(Value.TEN, Value.NINE));
        return new HandRange(hands, "Middle Position Open (~18%)");
    }


    public static HandRange buttonOpen() {
        Set<HandCombo> hands = new HashSet<>();

        for (Value v : Value.values()) {
            hands.add(HandCombo.pocketPair(v));
        }

        for (Value v : Value.values()) {
            if (v != Value.ACE) {
                hands.add(HandCombo.any(Value.ACE, v));
            }
        }

        for (Value v : Value.values()) {
            if (v.ordinal() < Value.KING.ordinal()) {
                hands.add(HandCombo.suited(Value.KING, v));
                if (v.ordinal() >= Value.SEVEN.ordinal()) {
                    hands.add(HandCombo.offsuit(Value.KING, v));
                }
            }
        }

        for (Value v : List.of(Value.JACK, Value.TEN, Value.NINE, Value.EIGHT)) {
            hands.add(HandCombo.any(Value.QUEEN, v));
        }

        hands.add(HandCombo.suited(Value.JACK, Value.TEN));
        hands.add(HandCombo.suited(Value.TEN, Value.NINE));
        hands.add(HandCombo.suited(Value.NINE, Value.EIGHT));
        hands.add(HandCombo.suited(Value.EIGHT, Value.SEVEN));
        hands.add(HandCombo.suited(Value.SEVEN, Value.SIX));
        hands.add(HandCombo.suited(Value.SIX, Value.FIVE));
        hands.add(HandCombo.suited(Value.FIVE, Value.FOUR));
        return new HandRange(hands, "Button Open (~45%)");
    }


    public static HandRange bigBlindDefend() {
        Set<HandCombo> hands = new HashSet<>();

        for (Value v : Value.values()) {
            hands.add(HandCombo.pocketPair(v));
        }

        for (Value v : Value.values()) {
            if (v != Value.ACE) {
                hands.add(HandCombo.suited(Value.ACE, v));
                if (v.ordinal() >= Value.FOUR.ordinal()) {
                    hands.add(HandCombo.offsuit(Value.ACE, v));
                }
            }
        }

        for (Value high : List.of(Value.KING, Value.QUEEN, Value.JACK)) {
            for (Value low : Value.values()) {
                if (low.ordinal() < high.ordinal() && low.ordinal() >= Value.EIGHT.ordinal()) {
                    hands.add(HandCombo.any(high, low));
                }
            }
        }

        hands.add(HandCombo.suited(Value.TEN, Value.NINE));
        hands.add(HandCombo.suited(Value.NINE, Value.EIGHT));
        hands.add(HandCombo.suited(Value.EIGHT, Value.SEVEN));
        hands.add(HandCombo.suited(Value.SEVEN, Value.SIX));
        hands.add(HandCombo.suited(Value.SIX, Value.FIVE));
        hands.add(HandCombo.suited(Value.FIVE, Value.FOUR));
        return new HandRange(hands, "Big Blind Defend (~38%)");
    }


    public static HandRange threeBetRange() {
        Set<HandCombo> hands = new HashSet<>();

        hands.add(HandCombo.pocketPair(Value.ACE));
        hands.add(HandCombo.pocketPair(Value.KING));
        hands.add(HandCombo.pocketPair(Value.QUEEN));
        hands.add(HandCombo.pocketPair(Value.JACK));
        hands.add(HandCombo.any(Value.ACE, Value.KING));
        hands.add(HandCombo.any(Value.ACE, Value.QUEEN));

        hands.add(HandCombo.suited(Value.ACE, Value.FIVE));
        hands.add(HandCombo.suited(Value.ACE, Value.FOUR));
        hands.add(HandCombo.suited(Value.SEVEN, Value.SIX));
        hands.add(HandCombo.suited(Value.SIX, Value.FIVE));
        return new HandRange(hands, "3-Bet Range (~7%)");
    }


    public static HandRange allInCallingRange() {
        Set<HandCombo> hands = new HashSet<>();

        hands.add(HandCombo.pocketPair(Value.ACE));
        hands.add(HandCombo.pocketPair(Value.KING));
        hands.add(HandCombo.pocketPair(Value.QUEEN));
        hands.add(HandCombo.pocketPair(Value.JACK));
        hands.add(HandCombo.pocketPair(Value.TEN));
        hands.add(HandCombo.any(Value.ACE, Value.KING));
        hands.add(HandCombo.any(Value.ACE, Value.QUEEN));
        hands.add(HandCombo.suited(Value.ACE, Value.JACK));
        return new HandRange(hands, "All-In Calling Range (~9%)");
    }


    public static HandRange forPositionType(PositionType positionType) {
        return switch (positionType) {
            case EARLY -> earlyPositionOpen();
            case MIDDLE -> middlePositionOpen();
            case CUTOFF, LATE, DEALER -> buttonOpen();
            case SMALL_BLIND -> middlePositionOpen();
            case BIG_BLIND -> bigBlindDefend();
        };
    }


    public static HandRange fromNotation(String notation) {
        Set<HandCombo> hands = new HashSet<>();
        String[] parts = notation.split(",");

        for (String part : parts) {
            String hand = part.trim().toUpperCase();
            if (hand.length() < 2) continue;

            Value v1 = parseValue(hand.charAt(0));
            Value v2 = parseValue(hand.charAt(1));

            if (v1 == null || v2 == null) continue;

            if (v1 == v2) {

                hands.add(HandCombo.pocketPair(v1));
            } else if (hand.length() >= 3 && hand.charAt(2) == 'S') {
                hands.add(HandCombo.suited(v1, v2));
            } else if (hand.length() >= 3 && hand.charAt(2) == 'O') {
                hands.add(HandCombo.offsuit(v1, v2));
            } else {
                hands.add(HandCombo.any(v1, v2));
            }
        }

        return new HandRange(hands, "Custom: " + notation);
    }

    private static Value parseValue(char c) {
        return switch (c) {
            case 'A' -> Value.ACE;
            case 'K' -> Value.KING;
            case 'Q' -> Value.QUEEN;
            case 'J' -> Value.JACK;
            case 'T' -> Value.TEN;
            case '9' -> Value.NINE;
            case '8' -> Value.EIGHT;
            case '7' -> Value.SEVEN;
            case '6' -> Value.SIX;
            case '5' -> Value.FIVE;
            case '4' -> Value.FOUR;
            case '3' -> Value.THREE;
            case '2' -> Value.TWO;
            default -> null;
        };
    }




    public List<List<Card>> generateHands(Set<Card> deadCards) {
        List<List<Card>> result = new ArrayList<>();

        for (HandCombo combo : includedHands) {
            result.addAll(combo.generateConcreteHands(deadCards));
        }

        return result;
    }


    public double getRangePercentage() {
        int totalCombos = 0;
        for (HandCombo combo : includedHands) {
            totalCombos += combo.getComboCount();
        }
        return (totalCombos / 1326.0) * 100;
    }


    public boolean containsHand(Card card1, Card card2) {
        Value v1 = card1.getValue();
        Value v2 = card2.getValue();
        boolean suited = card1.getSuit() == card2.getSuit();

        for (HandCombo combo : includedHands) {
            if (combo.matches(v1, v2, suited)) {
                return true;
            }
        }
        return false;
    }


    public HandRange union(HandRange other) {
        Set<HandCombo> combined = new HashSet<>(this.includedHands);
        combined.addAll(other.includedHands);
        return new HandRange(combined, this.description + " + " + other.description);
    }


    public HandRange intersect(HandRange other) {
        Set<HandCombo> intersection = new HashSet<>(this.includedHands);
        intersection.retainAll(other.includedHands);
        return new HandRange(intersection, this.description + " ∩ " + other.description);
    }



    public Set<HandCombo> getIncludedHands() {
        return includedHands;
    }

    public String getDescription() {
        return description;
    }

    public int getComboCount() {
        return includedHands.stream()
            .mapToInt(HandCombo::getComboCount)
            .sum();
    }

    @Override
    public String toString() {
        return String.format("HandRange[%s, %.1f%% of hands, %d combos]",
            description, getRangePercentage(), getComboCount());
    }




    public static class HandCombo {
        private final Value highCard;
        private final Value lowCard;
        private final ComboType type;

        public enum ComboType {
            POCKET_PAIR,
            SUITED,
            OFFSUIT,
            ANY
        }

        private HandCombo(Value v1, Value v2, ComboType type) {

            if (v1.ordinal() >= v2.ordinal()) {
                this.highCard = v1;
                this.lowCard = v2;
            } else {
                this.highCard = v2;
                this.lowCard = v1;
            }
            this.type = type;
        }

        public static HandCombo pocketPair(Value value) {
            return new HandCombo(value, value, ComboType.POCKET_PAIR);
        }

        public static HandCombo suited(Value v1, Value v2) {
            return new HandCombo(v1, v2, ComboType.SUITED);
        }

        public static HandCombo offsuit(Value v1, Value v2) {
            return new HandCombo(v1, v2, ComboType.OFFSUIT);
        }

        public static HandCombo any(Value v1, Value v2) {
            if (v1 == v2) {
                return pocketPair(v1);
            }
            return new HandCombo(v1, v2, ComboType.ANY);
        }

        public boolean matches(Value v1, Value v2, boolean suited) {
            Value high = v1.ordinal() >= v2.ordinal() ? v1 : v2;
            Value low = v1.ordinal() < v2.ordinal() ? v1 : v2;

            if (high != this.highCard || low != this.lowCard) {
                return false;
            }

            return switch (type) {
                case POCKET_PAIR -> true;
                case SUITED -> suited;
                case OFFSUIT -> !suited;
                case ANY -> true;
            };
        }

        public int getComboCount() {
            return switch (type) {
                case POCKET_PAIR -> 6;
                case SUITED -> 4;
                case OFFSUIT -> 12;
                case ANY -> highCard == lowCard ? 6 : 16;
            };
        }


        public List<List<Card>> generateConcreteHands(Set<Card> deadCards) {
            List<List<Card>> hands = new ArrayList<>();
            List<Suit> suits = Arrays.asList(Suit.values());

            if (type == ComboType.POCKET_PAIR) {

                for (int i = 0; i < suits.size(); i++) {
                    for (int j = i + 1; j < suits.size(); j++) {
                        Card c1 = new Card(suits.get(i), highCard);
                        Card c2 = new Card(suits.get(j), highCard);
                        if (!deadCards.contains(c1) && !deadCards.contains(c2)) {
                            hands.add(List.of(c1, c2));
                        }
                    }
                }
            } else {

                for (Suit s1 : suits) {
                    for (Suit s2 : suits) {
                        boolean isSuited = s1 == s2;
                        boolean shouldInclude = switch (type) {
                            case SUITED -> isSuited;
                            case OFFSUIT -> !isSuited;
                            case ANY -> true;
                            default -> false;
                        };

                        if (shouldInclude) {
                            Card c1 = new Card(s1, highCard);
                            Card c2 = new Card(s2, lowCard);
                            if (!deadCards.contains(c1) && !deadCards.contains(c2)) {
                                hands.add(List.of(c1, c2));
                            }
                        }
                    }
                }
            }

            return hands;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HandCombo handCombo = (HandCombo) o;
            return highCard == handCombo.highCard &&
                lowCard == handCombo.lowCard &&
                type == handCombo.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(highCard, lowCard, type);
        }

        @Override
        public String toString() {
            String valueStr = highCard.name().charAt(0) + "" + lowCard.name().charAt(0);
            if (highCard == lowCard) {
                return valueStr;
            }
            return switch (type) {
                case SUITED -> valueStr + "s";
                case OFFSUIT -> valueStr + "o";
                case ANY -> valueStr;
                default -> valueStr;
            };
        }
    }
}
