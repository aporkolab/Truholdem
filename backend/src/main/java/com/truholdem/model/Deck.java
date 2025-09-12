package com.truholdem.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Deck {
    private final List<Card> cards = new LinkedList<>();

    public Deck() {
        resetDeck();
    }

    // A pakli újrainicializálása és keverése
    public void resetDeck() {
        cards.clear();
        for (Suit suit : Suit.values()) {
            for (Value value : Value.values()) {
                cards.add(new Card(suit, value));
            }
        }
        shuffle(); // Keverés reset után
    }

    // Egy kártya húzása a pakliból
    public Card drawCard() {
        if (cards.isEmpty()) {
            resetDeck(); // Új pakli inicializálása és keverése, ha az aktuális pakli üres
        }
        return cards.remove(0);
    }

    // A pakliban maradt kártyák számának lekérdezése
    public int cardsLeft() {
        return cards.size();
    }

    // A pakli tartalmának kiíratása (fejlesztési célokra)
    public void printDeck() {
        cards.forEach(card -> System.out.println(card.toString()));
    }

    // A pakli keverése anélkül, hogy újrainicializálnánk
    public void shuffle() {
        Collections.shuffle(cards);
    }

    public List<Card> getCards() {
        return cards;
    }
}
