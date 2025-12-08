package com.truholdem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "tournament_registrations")
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private String playerName;

    private UUID userId;

    private int chips;

    private Integer finishPosition;

    private int prizeWon = 0;

    private boolean isEliminated = false;

    private LocalDateTime registeredAt;

    private LocalDateTime eliminatedAt;

    
    public TournamentRegistration() {
        this.registeredAt = LocalDateTime.now();
    }

    public TournamentRegistration(Tournament tournament, String playerName) {
        this();
        this.tournament = tournament;
        this.playerName = playerName;
        this.chips = tournament.getStartingChips();
    }

    public TournamentRegistration(Tournament tournament, String playerName, UUID userId) {
        this(tournament, playerName);
        this.userId = userId;
    }

    

    public void eliminate(int position, int prize) {
        this.isEliminated = true;
        this.eliminatedAt = LocalDateTime.now();
        this.finishPosition = position;
        this.prizeWon = prize;
        this.chips = 0;
    }

    public void updateChips(int newChips) {
        this.chips = newChips;
        if (newChips <= 0 && !isEliminated) {
            
            this.isEliminated = true;
            this.eliminatedAt = LocalDateTime.now();
            this.chips = 0;
        }
    }

    public boolean canPlay() {
        return !isEliminated && chips > 0;
    }

    

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public int getChips() { return chips; }
    public void setChips(int chips) { this.chips = chips; }

    public Integer getFinishPosition() { return finishPosition; }
    public void setFinishPosition(Integer finishPosition) { this.finishPosition = finishPosition; }

    public int getPrizeWon() { return prizeWon; }
    public void setPrizeWon(int prizeWon) { this.prizeWon = prizeWon; }

    public boolean isEliminated() { return isEliminated; }
    public void setEliminated(boolean eliminated) { isEliminated = eliminated; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public LocalDateTime getEliminatedAt() { return eliminatedAt; }
    public void setEliminatedAt(LocalDateTime eliminatedAt) { this.eliminatedAt = eliminatedAt; }
}
