package com.truholdem.model;

/**
 * Represents the type of position at a poker table.
 * Moved to model package to avoid circular dependency between model and domain packages.
 */
public enum PositionType {

    SMALL_BLIND(1, "SB"),


    BIG_BLIND(2, "BB"),


    EARLY(3, "EP"),


    MIDDLE(4, "MP"),


    CUTOFF(5, "CO"),


    DEALER(6, "BTN"),


    LATE(5, "LP");

    private final int positionValue;
    private final String abbreviation;

    PositionType(int positionValue, String abbreviation) {
        this.positionValue = positionValue;
        this.abbreviation = abbreviation;
    }

    /**
     * Gets the numeric value representing position strength.
     * Higher values indicate more advantageous positions.
     */
    public int getPositionValue() {
        return positionValue;
    }

    /**
     * Gets the standard abbreviation for this position.
     */
    public String getAbbreviation() {
        return abbreviation;
    }
}
