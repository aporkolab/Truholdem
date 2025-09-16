package com.truholdem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


@Entity
@Table(name = "tournament_registrations", indexes = {
    @Index(name = "idx_reg_tournament", columnList = "tournament_id"),
    @Index(name = "idx_reg_player", columnList = "playerId"),
    @Index(name = "idx_reg_status", columnList = "status"),
    @Index(name = "idx_reg_position", columnList = "finishPosition")
})
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private UUID playerId;
    
    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private int currentChips;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;
    
    
    private int rebuysUsed = 0;
    private int addOnsUsed = 0;
    
    
    private int bountiesCollected = 0;
    private int bountyValue = 0;
    
    
    private Integer finishPosition;
    private int prizeWon = 0;
    
    
    @Column(nullable = false, updatable = false)
    private Instant registeredAt;
    
    private Instant startedPlayingAt;
    private Instant eliminatedAt;

    
    protected TournamentRegistration() {
        this.registeredAt = Instant.now();
    }

    public TournamentRegistration(Tournament tournament, UUID playerId, String playerName) {
        this();
        this.tournament = Objects.requireNonNull(tournament, "Tournament cannot be null");
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null");
        this.currentChips = tournament.getStartingChips();
        
        
        if (tournament.getTournamentType() == TournamentType.BOUNTY) {
            this.bountyValue = tournament.getBountyAmount();
        }
    }

    
    
    
    public void updateChips(int newChips) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot update chips for eliminated/finished player");
        }
        
        this.currentChips = Math.max(0, newChips);
        
        if (this.currentChips == 0 && !canRebuy()) {
            
        }
    }
    
    
    public void rebuy(int chips) {
        if (!canRebuy()) {
            throw new IllegalStateException("Player cannot rebuy");
        }
        this.currentChips += chips;
        this.rebuysUsed++;
    }
    
    
    public void addOn(int chips) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot add-on for eliminated player");
        }
        this.currentChips += chips;
        this.addOnsUsed++;
    }
    
    
    public boolean canRebuy() {
        if (tournament.getTournamentType() != TournamentType.REBUY) {
            return false;
        }
        if (status.isTerminal()) {
            return false;
        }
        
        return tournament.getCurrentLevel() <= tournament.getRebuyDeadlineLevel() 
               && rebuysUsed < tournament.getMaxRebuys();
    }
    
    
    
    
    public void collectBounty(int bountyAmount) {
        this.bountiesCollected++;
        this.prizeWon += bountyAmount;
    }
    
    
    public int getBountyValue() {
        return bountyValue;
    }
    
    
    
    
    public void startPlaying() {
        if (status != RegistrationStatus.REGISTERED) {
            throw new IllegalStateException("Player not in REGISTERED status");
        }
        this.status = RegistrationStatus.PLAYING;
        this.startedPlayingAt = Instant.now();
    }
    
    
    public void eliminate(int position, int prize) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Player already eliminated/finished");
        }
        
        this.status = RegistrationStatus.ELIMINATED;
        this.finishPosition = position;
        this.prizeWon += prize;
        this.currentChips = 0;
        this.eliminatedAt = Instant.now();
    }
    
    
    public void finish(int position, int prize) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Player already eliminated/finished");
        }
        
        this.status = RegistrationStatus.FINISHED;
        this.finishPosition = position;
        this.prizeWon += prize;
        this.eliminatedAt = Instant.now();
    }
    
    
    public void withdraw() {
        if (status.isTerminal()) {
            throw new IllegalStateException("Player already eliminated/finished");
        }
        
        this.status = RegistrationStatus.WITHDRAWN;
        this.currentChips = 0;
        this.eliminatedAt = Instant.now();
    }
    
    
    
    
    public boolean canPlay() {
        return status == RegistrationStatus.PLAYING && currentChips > 0;
    }
    
    
    public boolean isActive() {
        return !status.isTerminal() && currentChips > 0;
    }
    
    
    public boolean isEliminated() {
        return status == RegistrationStatus.ELIMINATED;
    }
    
    
    public double getMRatio(int smallBlind, int bigBlind, int ante, int playersAtTable) {
        int totalBlinds = smallBlind + bigBlind + (ante * playersAtTable);
        return totalBlinds > 0 ? (double) currentChips / totalBlinds : 0;
    }
    
    

    public UUID getId() { 
        return id; 
    }

    public Tournament getTournament() { 
        return tournament; 
    }

    public UUID getPlayerId() { 
        return playerId; 
    }

    public String getPlayerName() { 
        return playerName; 
    }

    public int getCurrentChips() { 
        return currentChips; 
    }
    
    
    public int getChips() { 
        return currentChips; 
    }

    public RegistrationStatus getStatus() { 
        return status; 
    }

    public int getRebuysUsed() { 
        return rebuysUsed; 
    }

    public int getAddOnsUsed() { 
        return addOnsUsed; 
    }

    public int getBountiesCollected() { 
        return bountiesCollected; 
    }

    public Integer getFinishPosition() { 
        return finishPosition; 
    }

    public int getPrizeWon() { 
        return prizeWon; 
    }

    public Instant getRegisteredAt() { 
        return registeredAt; 
    }

    public Instant getStartedPlayingAt() { 
        return startedPlayingAt; 
    }

    public Instant getEliminatedAt() { 
        return eliminatedAt; 
    }

    public Long getVersion() { 
        return version; 
    }
    
    
    
    protected void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
    
    public void setChips(int chips) {
        this.currentChips = chips;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TournamentRegistration that = (TournamentRegistration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("TournamentRegistration{id=%s, player=%s, chips=%d, status=%s, position=%s}",
                             id, playerName, currentChips, status, finishPosition);
    }
}
