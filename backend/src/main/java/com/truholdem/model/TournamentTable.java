package com.truholdem.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Entity
@Table(name = "tournament_tables", indexes = {
    @Index(name = "idx_tournament_table_tournament", columnList = "tournament_id"),
    @Index(name = "idx_tournament_table_number", columnList = "tableNumber")
})
public class TournamentTable {
    
    private static final int MAX_PLAYERS_PER_TABLE = 9;
    private static final int MIN_PLAYERS_TO_PLAY = 2;
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Version
    private Long version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "current_game_id")
    private Game currentGame;
    
    @Column(nullable = false)
    private int tableNumber;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tournament_table_players",
        joinColumns = @JoinColumn(name = "table_id")
    )
    @OrderColumn(name = "seat_position")
    @Column(name = "player_id")
    private List<UUID> playerIds = new ArrayList<>();
    
    @Column(nullable = false)
    private boolean isFinalTable = false;
    
    @Column(nullable = false)
    private boolean isActive = true;
    
    private Instant createdAt;
    
    private Instant closedAt;
    
    
    protected TournamentTable() {
        this.createdAt = Instant.now();
    }
    
    public TournamentTable(Tournament tournament, int tableNumber) {
        this();
        this.tournament = Objects.requireNonNull(tournament, "Tournament cannot be null");
        this.tableNumber = tableNumber;
    }
    
    
    public static TournamentTable createFinalTable(Tournament tournament) {
        TournamentTable table = new TournamentTable(tournament, 1);
        table.isFinalTable = true;
        return table;
    }
    
    
    
    
    public void seatPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        if (!isActive) {
            throw new IllegalStateException("Cannot seat player at closed table");
        }
        if (isFull()) {
            throw new IllegalStateException("Table is full");
        }
        if (playerIds.contains(playerId)) {
            throw new IllegalStateException("Player already seated at this table");
        }
        
        playerIds.add(playerId);
    }
    
    
    public boolean removePlayer(UUID playerId) {
        return playerIds.remove(playerId);
    }
    
    
    public boolean hasPlayer(UUID playerId) {
        return playerIds.contains(playerId);
    }
    
    
    public int getSeatPosition(UUID playerId) {
        return playerIds.indexOf(playerId);
    }
    
    
    
    
    public boolean isFull() {
        return playerIds.size() >= MAX_PLAYERS_PER_TABLE;
    }
    
    
    public boolean canStartHand() {
        return isActive && playerIds.size() >= MIN_PLAYERS_TO_PLAY;
    }
    
    
    public int getEmptySeats() {
        return MAX_PLAYERS_PER_TABLE - playerIds.size();
    }
    
    
    public int getPlayerCount() {
        return playerIds.size();
    }
    
    
    public void close() {
        this.isActive = false;
        this.closedAt = Instant.now();
        this.currentGame = null;
    }
    
    
    public void startNewGame(Game game) {
        if (!canStartHand()) {
            throw new IllegalStateException("Cannot start game: table inactive or insufficient players");
        }
        this.currentGame = game;
    }
    
    
    public void clearCurrentGame() {
        this.currentGame = null;
    }
    
    
    
    public UUID getId() {
        return id;
    }
    
    public Tournament getTournament() {
        return tournament;
    }
    
    public Game getCurrentGame() {
        return currentGame;
    }
    
    public int getTableNumber() {
        return tableNumber;
    }
    
    public List<UUID> getPlayerIds() {
        return Collections.unmodifiableList(playerIds);
    }
    
    public boolean isFinalTable() {
        return isFinalTable;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getClosedAt() {
        return closedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    
    
    protected void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
    
    protected void setFinalTable(boolean finalTable) {
        isFinalTable = finalTable;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TournamentTable that = (TournamentTable) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("TournamentTable{id=%s, number=%d, players=%d, final=%s, active=%s}",
                             id, tableNumber, playerIds.size(), isFinalTable, isActive);
    }
}
