package com.truholdem.domain.event;

import com.truholdem.domain.value.Chips;
import com.truholdem.model.Card;
import com.truholdem.model.GamePhase;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public final class PhaseChanged extends DomainEvent {

    private final GamePhase previousPhase;
    private final GamePhase newPhase;
    private final List<Card> newCommunityCards;
    private final List<Card> allCommunityCards;
    private final Chips potSize;
    private final int activePlayerCount;

    
    public PhaseChanged(UUID gameId, GamePhase previousPhase, GamePhase newPhase,
                        List<Card> newCommunityCards, List<Card> allCommunityCards,
                        Chips potSize, int activePlayerCount) {
        super(gameId);
        this.previousPhase = Objects.requireNonNull(previousPhase, "Previous phase cannot be null");
        this.newPhase = Objects.requireNonNull(newPhase, "New phase cannot be null");
        this.newCommunityCards = newCommunityCards != null 
                ? Collections.unmodifiableList(newCommunityCards) 
                : Collections.emptyList();
        this.allCommunityCards = allCommunityCards != null 
                ? Collections.unmodifiableList(allCommunityCards) 
                : Collections.emptyList();
        this.potSize = Objects.requireNonNull(potSize, "Pot size cannot be null");
        this.activePlayerCount = activePlayerCount;
    }

    public GamePhase getPreviousPhase() {
        return previousPhase;
    }

    public GamePhase getNewPhase() {
        return newPhase;
    }

    public List<Card> getNewCommunityCards() {
        return newCommunityCards;
    }

    public List<Card> getAllCommunityCards() {
        return allCommunityCards;
    }

    public Chips getPotSize() {
        return potSize;
    }

    public int getActivePlayerCount() {
        return activePlayerCount;
    }

    
    public boolean isFlop() {
        return newPhase == GamePhase.FLOP;
    }

    
    public boolean isTurn() {
        return newPhase == GamePhase.TURN;
    }

    
    public boolean isRiver() {
        return newPhase == GamePhase.RIVER;
    }

    
    public boolean isShowdown() {
        return newPhase == GamePhase.SHOWDOWN;
    }

    
    public int getNewCardCount() {
        return newCommunityCards.size();
    }

    @Override
    public String toString() {
        String cardsStr = newCommunityCards.isEmpty() 
                ? "" 
                : String.format(", cards=%s", formatCards(newCommunityCards));
        return String.format("PhaseChanged[%s -> %s%s, pot=%s, players=%d]",
                previousPhase, newPhase, cardsStr, potSize, activePlayerCount);
    }

    private String formatCards(List<Card> cards) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(" ");
            Card card = cards.get(i);
            sb.append(card.getValue().name().charAt(0))
              .append(Character.toLowerCase(card.getSuit().name().charAt(0)));
        }
        sb.append("]");
        return sb.toString();
    }
}
