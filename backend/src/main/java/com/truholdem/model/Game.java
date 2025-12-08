package com.truholdem.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "poker_games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Player> players = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_community_cards", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn
    private List<Card> communityCards = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_deck", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn
    private List<Card> deck = new ArrayList<>();

    private int currentPot;

    @Enumerated(EnumType.STRING)
    private GamePhase phase;

    private int currentPlayerIndex;

    private int currentBet;

    private int smallBlind;

    private int bigBlind;

    private int dealerPosition;

    private String winnerName;

    private String winningHandDescription;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_winner_ids", joinColumns = @JoinColumn(name = "game_id"))
    private List<UUID> winnerIds = new ArrayList<>();

    private boolean isFinished;

    private int handNumber;

    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_side_pots", joinColumns = @JoinColumn(name = "game_id"))
    @OrderColumn
    private List<SidePot> sidePots = new ArrayList<>();

    private int lastRaiseAmount;

    private int minRaiseAmount;

    public Game() {
        this.smallBlind = 10;
        this.bigBlind = 20;
        this.phase = GamePhase.PRE_FLOP;
        this.dealerPosition = 0;
        this.isFinished = false;
        this.handNumber = 1;
        this.minRaiseAmount = bigBlind;
    }

    

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public void addPlayer(Player player) {
        players.add(player);
        player.setGame(this);
    }

    public List<Card> getCommunityCards() {
        return communityCards;
    }

    public void setCommunityCards(List<Card> communityCards) {
        this.communityCards = communityCards;
    }

    public void addCommunityCard(Card card) {
        this.communityCards.add(card);
    }

    public int getCurrentPot() {
        return currentPot;
    }

    public void setCurrentPot(int currentPot) {
        this.currentPot = currentPot;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public void setSmallBlind(int smallBlind) {
        this.smallBlind = smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    public void setBigBlind(int bigBlind) {
        this.bigBlind = bigBlind;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public int getDealerPosition() {
        return dealerPosition;
    }

    public void setDealerPosition(int dealerPosition) {
        this.dealerPosition = dealerPosition;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public String getWinningHandDescription() {
        return winningHandDescription;
    }

    public void setWinningHandDescription(String winningHandDescription) {
        this.winningHandDescription = winningHandDescription;
    }

    public List<UUID> getWinnerIds() {
        return winnerIds;
    }

    public void setWinnerIds(List<UUID> winnerIds) {
        this.winnerIds = winnerIds;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public int getHandNumber() {
        return handNumber;
    }

    public void setHandNumber(int handNumber) {
        this.handNumber = handNumber;
    }

    public List<SidePot> getSidePots() {
        return sidePots;
    }

    public void setSidePots(List<SidePot> sidePots) {
        this.sidePots = sidePots;
    }

    public void addSidePot(SidePot sidePot) {
        this.sidePots.add(sidePot);
    }

    public int getLastRaiseAmount() {
        return lastRaiseAmount;
    }

    public void setLastRaiseAmount(int lastRaiseAmount) {
        this.lastRaiseAmount = lastRaiseAmount;
    }

    public int getMinRaiseAmount() {
        return minRaiseAmount;
    }

    public void setMinRaiseAmount(int minRaiseAmount) {
        this.minRaiseAmount = minRaiseAmount;
    }

    

    public Player getCurrentPlayer() {
        if (players.isEmpty() || currentPlayerIndex >= players.size()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    public List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> !p.isFolded() && p.getChips() >= 0)
                .toList();
    }

    public List<Player> getPlayersStillInHand() {
        return players.stream()
                .filter(p -> !p.isFolded())
                .toList();
    }

    public int getTotalPot() {
        int total = currentPot;
        for (SidePot sidePot : sidePots) {
            total += sidePot.getAmount();
        }
        return total;
    }

    public void resetForNewHand() {
        this.communityCards.clear();
        this.currentPot = 0;
        this.currentBet = 0;
        this.phase = GamePhase.PRE_FLOP;
        this.winnerName = null;
        this.winningHandDescription = null;
        this.winnerIds.clear();
        this.isFinished = false;
        this.sidePots.clear();
        this.lastRaiseAmount = bigBlind;
        this.minRaiseAmount = bigBlind;
        this.handNumber++;

        
        this.dealerPosition = (this.dealerPosition + 1) % players.size();

        
        for (Player player : players) {
            player.clearHand();
            player.setFolded(false);
            player.setBetAmount(0);
            player.setHasActed(false);
            player.setAllIn(false);
            player.setTotalBetInRound(0);
        }
    }
}
