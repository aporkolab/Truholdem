package com.truholdem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "hand_histories", indexes = {
    @Index(name = "idx_hand_history_game", columnList = "gameId"),
    @Index(name = "idx_hand_history_played_at", columnList = "playedAt")
})
public class HandHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID gameId;

    @Column(nullable = false)
    private int handNumber;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    
    private int smallBlind;
    private int bigBlind;
    private int dealerPosition;

    
    private String winnerName;
    private String winningHandDescription;
    private int finalPot;

    
    @ElementCollection
    @CollectionTable(name = "hand_history_players", joinColumns = @JoinColumn(name = "hand_history_id"))
    private List<HandHistoryPlayer> players = new ArrayList<>();

    
    @ElementCollection
    @CollectionTable(name = "hand_history_actions", joinColumns = @JoinColumn(name = "hand_history_id"))
    @OrderColumn(name = "action_order")
    private List<ActionRecord> actions = new ArrayList<>();

    
    @ElementCollection
    @CollectionTable(name = "hand_history_board", joinColumns = @JoinColumn(name = "hand_history_id"))
    @OrderColumn(name = "card_order")
    private List<CardRecord> board = new ArrayList<>();

    
    public HandHistory() {}

    public HandHistory(Game game) {
        this.gameId = game.getId();
        this.handNumber = game.getHandNumber();
        this.playedAt = LocalDateTime.now();
        this.smallBlind = game.getSmallBlind();
        this.bigBlind = game.getBigBlind();
        this.dealerPosition = game.getDealerPosition();

        
        for (Player player : game.getPlayers()) {
            this.players.add(new HandHistoryPlayer(player));
        }
    }

    
    public void recordAction(Player player, PlayerAction action, int amount, GamePhase phase) {
        ActionRecord record = new ActionRecord(
            player.getId(),
            player.getName(),
            action.name(),
            amount,
            phase.name(),
            LocalDateTime.now()
        );
        this.actions.add(record);
    }

    public void recordCommunityCards(List<Card> cards) {
        for (Card card : cards) {
            if (this.board.stream().noneMatch(c -> 
                c.suit().equals(card.getSuit().name()) && 
                c.value().equals(card.getValue().name()))) {
                this.board.add(new CardRecord(card.getSuit().name(), card.getValue().name()));
            }
        }
    }

    public void recordResult(String winner, String handDesc, int pot) {
        this.winnerName = winner;
        this.winningHandDescription = handDesc;
        this.finalPot = pot;
    }

    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }

    public int getHandNumber() { return handNumber; }
    public void setHandNumber(int handNumber) { this.handNumber = handNumber; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }

    public int getSmallBlind() { return smallBlind; }
    public void setSmallBlind(int smallBlind) { this.smallBlind = smallBlind; }

    public int getBigBlind() { return bigBlind; }
    public void setBigBlind(int bigBlind) { this.bigBlind = bigBlind; }

    public int getDealerPosition() { return dealerPosition; }
    public void setDealerPosition(int dealerPosition) { this.dealerPosition = dealerPosition; }

    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }

    public String getWinningHandDescription() { return winningHandDescription; }
    public void setWinningHandDescription(String winningHandDescription) { this.winningHandDescription = winningHandDescription; }

    public int getFinalPot() { return finalPot; }
    public void setFinalPot(int finalPot) { this.finalPot = finalPot; }

    public List<HandHistoryPlayer> getPlayers() { return players; }
    public void setPlayers(List<HandHistoryPlayer> players) { this.players = players; }

    public List<ActionRecord> getActions() { return actions; }
    public void setActions(List<ActionRecord> actions) { this.actions = actions; }

    public List<CardRecord> getBoard() { return board; }
    public void setBoard(List<CardRecord> board) { this.board = board; }

    

    @Embeddable
    public static class HandHistoryPlayer {
        private UUID playerId;
        private String playerName;
        private int startingChips;
        private int seatPosition;
        private String holeCard1Suit;
        private String holeCard1Value;
        private String holeCard2Suit;
        private String holeCard2Value;

        public HandHistoryPlayer() {}

        public HandHistoryPlayer(Player player) {
            this.playerId = player.getId();
            this.playerName = player.getName();
            this.startingChips = player.getChips() + player.getBetAmount(); 
            this.seatPosition = player.getSeatPosition();
            
            if (player.getHand() != null && player.getHand().size() >= 2) {
                Card c1 = player.getHand().get(0);
                Card c2 = player.getHand().get(1);
                this.holeCard1Suit = c1.getSuit().name();
                this.holeCard1Value = c1.getValue().name();
                this.holeCard2Suit = c2.getSuit().name();
                this.holeCard2Value = c2.getValue().name();
            }
        }

        
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public int getStartingChips() { return startingChips; }
        public int getSeatPosition() { return seatPosition; }
        public String getHoleCard1Suit() { return holeCard1Suit; }
        public String getHoleCard1Value() { return holeCard1Value; }
        public String getHoleCard2Suit() { return holeCard2Suit; }
        public String getHoleCard2Value() { return holeCard2Value; }

        
        public void setPlayerId(UUID playerId) { this.playerId = playerId; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public void setStartingChips(int startingChips) { this.startingChips = startingChips; }
        public void setSeatPosition(int seatPosition) { this.seatPosition = seatPosition; }
        public void setHoleCard1Suit(String holeCard1Suit) { this.holeCard1Suit = holeCard1Suit; }
        public void setHoleCard1Value(String holeCard1Value) { this.holeCard1Value = holeCard1Value; }
        public void setHoleCard2Suit(String holeCard2Suit) { this.holeCard2Suit = holeCard2Suit; }
        public void setHoleCard2Value(String holeCard2Value) { this.holeCard2Value = holeCard2Value; }
    }

    @Embeddable
    public record ActionRecord(
        UUID playerId,
        String playerName,
        String action,
        int amount,
        String phase,
        LocalDateTime timestamp
    ) {
        public ActionRecord() {
            this(null, null, null, 0, null, null);
        }
    }

    @Embeddable
    public record CardRecord(String suit, String value) {
        public CardRecord() {
            this(null, null);
        }
    }
}
