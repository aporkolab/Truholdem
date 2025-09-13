package com.truholdem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player implements Persistable<UUID> {

    @Id
    private UUID id;

    @Version
    @JsonIgnore
    private Long version;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_hand", joinColumns = @JoinColumn(name = "player_id"))
    @OrderColumn
    private List<Card> hand = new ArrayList<>();

    private int chips;
    private int betAmount;          
    private int totalBetInRound;    
    @JsonProperty("folded")
    private boolean isFolded;

    @JsonProperty("isBot")
    private boolean isBot;

    private boolean hasActed;

    @JsonProperty("isAllIn")
    private boolean isAllIn;
    private int seatPosition;

    /**
     * Link to the authenticated user who owns this player.
     * Null for bot players.
     */
    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @JsonBackReference
    private Game game;

    public Player(String name, int startingChips, boolean isBot) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.chips = startingChips;
        this.isFolded = false;
        this.isBot = isBot;
        this.isAllIn = false;
        this.totalBetInRound = 0;
    }

    public Player() {
        
    }

    
    public int placeBet(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Bet amount must be positive");
        }
        
        int actualBet = Math.min(amount, chips);
        betAmount += actualBet;
        totalBetInRound += actualBet;
        chips -= actualBet;
        
        if (chips == 0) {
            isAllIn = true;
        }
        
        return actualBet;
    }

    
    public int call(int currentBet) {
        int amountToCall = currentBet - betAmount;
        if (amountToCall <= 0) {
            return 0;
        }
        return placeBet(amountToCall);
    }

    public void addWinnings(int amount) {
        this.chips += amount;
    }

    public void fold() {
        isFolded = true;
    }

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        this.hand.clear();
    }

    
    public void resetBetForNewRound() {
        this.betAmount = 0;
        this.hasActed = false;
    }

    
    public boolean canAct() {
        return !isFolded && !isAllIn;
    }

    

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("folded")
    public boolean isFolded() {
        return isFolded;
    }

    public void setFolded(boolean folded) {
        isFolded = folded;
    }

    public int getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(int betAmount) {
        this.betAmount = betAmount;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    @JsonProperty("isBot")
    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public boolean hasActed() {
        return hasActed;
    }

    public void setHasActed(boolean hasActed) {
        this.hasActed = hasActed;
    }

    @JsonProperty("isAllIn")
    public boolean isAllIn() {
        return isAllIn;
    }

    public void setAllIn(boolean allIn) {
        isAllIn = allIn;
    }

    public int getTotalBetInRound() {
        return totalBetInRound;
    }

    public void setTotalBetInRound(int totalBetInRound) {
        this.totalBetInRound = totalBetInRound;
    }

    public int getSeatPosition() {
        return seatPosition;
    }

    public void setSeatPosition(int seatPosition) {
        this.seatPosition = seatPosition;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /**
     * Checks if this player is owned by the specified user.
     * Bot players are not owned by any user.
     */
    public boolean isOwnedBy(UUID checkUserId) {
        if (isBot || userId == null) {
            return false;
        }
        return userId.equals(checkUserId);
    }

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", chips=" + chips +
                ", betAmount=" + betAmount +
                ", isFolded=" + isFolded +
                ", isAllIn=" + isAllIn +
                '}';
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return version == null;
    }
}