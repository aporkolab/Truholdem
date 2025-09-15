package com.truholdem.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "poker_games", indexes = {
        @Index(name = "idx_game_phase", columnList = "phase"),
        @Index(name = "idx_game_finished", columnList = "isFinished"),
        @Index(name = "idx_game_created_at", columnList = "createdAt")
})
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    @JsonIgnore
    private Long version;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @OrderBy("seatPosition ASC")
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

    @JsonProperty("isFinished")
    private boolean isFinished;

    private int handNumber;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SidePot> sidePots = new ArrayList<>();

    private int lastRaiseAmount;

    private int minRaiseAmount;

    // Showdown order tracking - who was the last aggressor (bet/raise)
    private UUID lastAggressorId;

    // Dead button tracking - the seat position where button should be (even if player eliminated)
    private int buttonSeatPosition;

    // Track if we're in a dead button situation
    private boolean deadButton;

    // Track missed blinds for returning players (seat position -> missed blind amount)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_missed_blinds", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyColumn(name = "seat_position")
    @Column(name = "missed_amount")
    private Map<Integer, Integer> missedBlinds = new HashMap<>();

    public Game() {
        this.smallBlind = 10;
        this.bigBlind = 20;
        this.phase = GamePhase.PRE_FLOP;
        this.dealerPosition = 0;
        this.buttonSeatPosition = 0;
        this.deadButton = false;
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

    @JsonProperty("isFinished")
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
        if (players.isEmpty() || currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    public List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> !p.isFolded() && p.getChips() >= 0)
                .collect(Collectors.toCollection(ArrayList::new));

    }

    public List<Player> getPlayersStillInHand() {
        return players.stream()
                .filter(p -> !p.isFolded())
                .collect(Collectors.toCollection(ArrayList::new));
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
        this.lastAggressorId = null;
        this.handNumber++;

        // Advance button position with dead button logic
        advanceButtonPosition();

        for (Player player : players) {
            player.clearHand();
            player.setFolded(false);
            player.setBetAmount(0);
            player.setHasActed(false);
            player.setAllIn(false);
            player.setTotalBetInRound(0);
        }
    }

    /**
     * Advances the button position according to official poker dead button rules.
     * The button moves to the next clockwise seat position that has an active player.
     * If a player was eliminated in the button position, the button is "dead" for that round.
     */
    private void advanceButtonPosition() {
        if (players.isEmpty()) {
            return;
        }

        // Move button seat position to next seat (clockwise)
        int maxSeatPosition = players.stream()
                .mapToInt(Player::getSeatPosition)
                .max()
                .orElse(0);

        int nextButtonSeat = (buttonSeatPosition + 1) % (maxSeatPosition + 1);

        // Find if there's a player at this seat position
        Player playerAtButton = players.stream()
                .filter(p -> p.getSeatPosition() == nextButtonSeat && p.getChips() > 0)
                .findFirst()
                .orElse(null);

        if (playerAtButton != null) {
            // Normal case: player exists at button position
            buttonSeatPosition = nextButtonSeat;
            dealerPosition = players.indexOf(playerAtButton);
            deadButton = false;
        } else {
            // Dead button: no player at this seat, button stays here but we use next active player
            buttonSeatPosition = nextButtonSeat;
            deadButton = true;

            // Find next active player clockwise for actual dealer duties
            for (int i = 1; i <= maxSeatPosition + 1; i++) {
                int checkSeat = (nextButtonSeat + i) % (maxSeatPosition + 1);
                Player nextPlayer = players.stream()
                        .filter(p -> p.getSeatPosition() == checkSeat && p.getChips() > 0)
                        .findFirst()
                        .orElse(null);
                if (nextPlayer != null) {
                    dealerPosition = players.indexOf(nextPlayer);
                    break;
                }
            }
        }
    }

    // Getters and setters for new poker rule fields

    public UUID getLastAggressorId() {
        return lastAggressorId;
    }

    public void setLastAggressorId(UUID lastAggressorId) {
        this.lastAggressorId = lastAggressorId;
    }

    public int getButtonSeatPosition() {
        return buttonSeatPosition;
    }

    public void setButtonSeatPosition(int buttonSeatPosition) {
        this.buttonSeatPosition = buttonSeatPosition;
    }

    public boolean isDeadButton() {
        return deadButton;
    }

    public void setDeadButton(boolean deadButton) {
        this.deadButton = deadButton;
    }

    public Map<Integer, Integer> getMissedBlinds() {
        return missedBlinds;
    }

    public void setMissedBlinds(Map<Integer, Integer> missedBlinds) {
        this.missedBlinds = missedBlinds;
    }

    public void addMissedBlind(int seatPosition, int amount) {
        this.missedBlinds.merge(seatPosition, amount, Integer::sum);
    }

    public int getMissedBlindAmount(int seatPosition) {
        return this.missedBlinds.getOrDefault(seatPosition, 0);
    }

    public void clearMissedBlind(int seatPosition) {
        this.missedBlinds.remove(seatPosition);
    }

    /**
     * Get the player who was the last aggressor (bet/raise) for showdown order.
     */
    public Player getLastAggressor() {
        if (lastAggressorId == null) {
            return null;
        }
        return players.stream()
                .filter(p -> p.getId().equals(lastAggressorId))
                .findFirst()
                .orElse(null);
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
