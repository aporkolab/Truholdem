package com.truholdem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Card {
    @Enumerated(EnumType.STRING)
    private Suit suit;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_value")
    private Value value;

    public Card(Suit suit, Value value) {
        this.suit = suit;
        this.value = value;
    }

    
    public Card() {
    }

    
    public Suit getSuit() {
        return suit;
    }

    
    public void setSuit(Suit suit) {
        this.suit = suit;
    }

    
    public Value getValue() {
        return value;
    }

    
    public void setValue(Value value) {
        this.value = value;
    }

    
    @Override
    public String toString() {
        return value + " of " + suit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return suit == card.suit && value == card.value;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(suit, value);
    }
}

