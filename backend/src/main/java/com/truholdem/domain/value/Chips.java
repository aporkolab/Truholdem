package com.truholdem.domain.value;

import java.util.Objects;


public record Chips(int amount) implements Comparable<Chips> {

    
    public Chips {
        if (amount < 0) {
            throw new IllegalArgumentException("Chips cannot be negative: " + amount);
        }
    }

    
    public static Chips of(int amount) {
        return new Chips(amount);
    }

    
    public static Chips zero() {
        return new Chips(0);
    }

    
    public Chips add(Chips other) {
        Objects.requireNonNull(other, "Cannot add null chips");
        return new Chips(this.amount + other.amount);
    }

    
    public Chips subtract(Chips other) {
        Objects.requireNonNull(other, "Cannot subtract null chips");
        int result = this.amount - other.amount;
        if (result < 0) {
            throw new IllegalArgumentException(
                "Cannot subtract " + other.amount + " from " + this.amount + ": would result in negative chips"
            );
        }
        return new Chips(result);
    }

    
    public Chips subtractOrZero(Chips other) {
        Objects.requireNonNull(other, "Cannot subtract null chips");
        return new Chips(Math.max(0, this.amount - other.amount));
    }

    
    public Chips min(Chips other) {
        Objects.requireNonNull(other, "Cannot compare with null chips");
        return this.amount <= other.amount ? this : other;
    }

    
    public Chips max(Chips other) {
        Objects.requireNonNull(other, "Cannot compare with null chips");
        return this.amount >= other.amount ? this : other;
    }

    
    public boolean isGreaterThan(Chips other) {
        Objects.requireNonNull(other, "Cannot compare with null chips");
        return this.amount > other.amount;
    }

    
    public boolean isGreaterThanOrEqual(Chips other) {
        Objects.requireNonNull(other, "Cannot compare with null chips");
        return this.amount >= other.amount;
    }

    
    public boolean isLessThan(Chips other) {
        Objects.requireNonNull(other, "Cannot compare with null chips");
        return this.amount < other.amount;
    }

    
    public boolean canAfford(Chips cost) {
        Objects.requireNonNull(cost, "Cost cannot be null");
        return this.amount >= cost.amount;
    }

    
    public boolean isZero() {
        return this.amount == 0;
    }

    
    public boolean hasChips() {
        return this.amount > 0;
    }

    
    public Chips multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Cannot multiply by negative factor: " + factor);
        }
        return new Chips(this.amount * factor);
    }

    
    public Chips percentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be 0-100: " + percentage);
        }
        return new Chips((this.amount * percentage) / 100);
    }

    @Override
    public int compareTo(Chips other) {
        return Integer.compare(this.amount, other.amount);
    }

    @Override
    public String toString() {
        return amount + " chips";
    }
}
