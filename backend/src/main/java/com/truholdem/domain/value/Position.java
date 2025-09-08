package com.truholdem.domain.value;

import com.truholdem.model.PositionType;
import java.util.Objects;


public record Position(int seatNumber, PositionType type) implements Comparable<Position> {


    public Position {
        if (seatNumber < 0) {
            throw new IllegalArgumentException("Seat number cannot be negative: " + seatNumber);
        }
        Objects.requireNonNull(type, "Position type cannot be null");
    }


    public static Position calculate(int seatNumber, int dealerPosition, int totalPlayers) {
        if (seatNumber < 0) {
            throw new IllegalArgumentException("Seat number cannot be negative: " + seatNumber);
        }
        if (dealerPosition < 0) {
            throw new IllegalArgumentException("Dealer position cannot be negative: " + dealerPosition);
        }
        if (totalPlayers < 2) {
            throw new IllegalArgumentException("Need at least 2 players: " + totalPlayers);
        }

        PositionType positionType = calculatePositionType(seatNumber, dealerPosition, totalPlayers);
        return new Position(seatNumber, positionType);
    }


    private static PositionType calculatePositionType(int seatNumber, int dealerPosition, int totalPlayers) {

        int relativePosition = (seatNumber - dealerPosition + totalPlayers) % totalPlayers;


        if (relativePosition == 0) {
            return PositionType.DEALER;
        }


        if (relativePosition == 1) {
            return PositionType.SMALL_BLIND;
        }


        if (relativePosition == 2) {
            return PositionType.BIG_BLIND;
        }


        if (totalPlayers == 2) {
            return relativePosition == 0 ? PositionType.SMALL_BLIND : PositionType.BIG_BLIND;
        }


        int positionsAfterBlinds = totalPlayers - 3;
        int positionFromBlinds = relativePosition - 3;

        if (positionsAfterBlinds <= 0) {

            return PositionType.EARLY;
        }


        if (relativePosition == totalPlayers - 1) {
            return PositionType.CUTOFF;
        }


        int earlyPositions = (positionsAfterBlinds + 1) / 3;
        int middlePositions = (positionsAfterBlinds + 1) / 3;

        if (positionFromBlinds < earlyPositions) {
            return PositionType.EARLY;
        } else if (positionFromBlinds < earlyPositions + middlePositions) {
            return PositionType.MIDDLE;
        } else {
            return PositionType.LATE;
        }
    }


    public static Position dealer(int seatNumber) {
        return new Position(seatNumber, PositionType.DEALER);
    }


    public static Position smallBlind(int seatNumber) {
        return new Position(seatNumber, PositionType.SMALL_BLIND);
    }


    public static Position bigBlind(int seatNumber) {
        return new Position(seatNumber, PositionType.BIG_BLIND);
    }


    public boolean isInPosition() {
        return type == PositionType.DEALER
            || type == PositionType.CUTOFF
            || type == PositionType.LATE;
    }


    public boolean isEarlyPosition() {
        return type == PositionType.EARLY
            || type == PositionType.SMALL_BLIND
            || type == PositionType.BIG_BLIND;
    }


    public boolean isBlind() {
        return type == PositionType.SMALL_BLIND || type == PositionType.BIG_BLIND;
    }


    public boolean isDealer() {
        return type == PositionType.DEALER;
    }


    public boolean isSmallBlind() {
        return type == PositionType.SMALL_BLIND;
    }


    public boolean isBigBlind() {
        return type == PositionType.BIG_BLIND;
    }


    public int getPositionValue() {
        return type.getPositionValue();
    }


    public boolean hasAdvantageOver(Position other) {
        Objects.requireNonNull(other, "Cannot compare with null position");
        return this.getPositionValue() > other.getPositionValue();
    }

    @Override
    public int compareTo(Position other) {

        return Integer.compare(this.getPositionValue(), other.getPositionValue());
    }

    @Override
    public String toString() {
        return String.format("Seat %d (%s)", seatNumber, type.getAbbreviation());
    }
}
