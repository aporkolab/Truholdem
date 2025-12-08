package com.truholdem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private TournamentStatus status = TournamentStatus.REGISTERING;

    @Enumerated(EnumType.STRING)
    private TournamentType tournamentType = TournamentType.SIT_AND_GO;

    private int buyIn = 100;
    private int startingChips = 1500;
    private int minPlayers = 2;
    private int maxPlayers = 9;

    
    private int currentLevel = 1;
    private int smallBlind = 10;
    private int bigBlind = 20;
    private int blindIncreaseMinutes = 15;

    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    
    private int prizePool = 0;

    
    private UUID activeGameId;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("registeredAt ASC")
    private List<TournamentRegistration> registrations = new ArrayList<>();

    
    public Tournament() {
        this.createdAt = LocalDateTime.now();
    }

    public Tournament(String name, int buyIn, int startingChips, int maxPlayers) {
        this();
        this.name = name;
        this.buyIn = buyIn;
        this.startingChips = startingChips;
        this.maxPlayers = maxPlayers;
    }

    

    public boolean canRegister() {
        return status == TournamentStatus.REGISTERING && 
               registrations.size() < maxPlayers;
    }

    public boolean canStart() {
        return status == TournamentStatus.REGISTERING && 
               registrations.size() >= minPlayers;
    }

    public void start() {
        if (!canStart()) {
            throw new IllegalStateException("Cannot start tournament: not enough players");
        }
        this.status = TournamentStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.prizePool = buyIn * registrations.size();
        
        
        for (TournamentRegistration reg : registrations) {
            reg.setChips(startingChips);
        }
    }

    public void finish(String winnerName) {
        this.status = TournamentStatus.FINISHED;
        this.endedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TournamentStatus.CANCELLED;
        this.endedAt = LocalDateTime.now();
    }

    public void increaseBlindLevel() {
        this.currentLevel++;
        this.smallBlind = calculateBlind(currentLevel, true);
        this.bigBlind = calculateBlind(currentLevel, false);
    }

    private int calculateBlind(int level, boolean isSmall) {
        
        int[] smallBlinds = {10, 15, 25, 50, 75, 100, 150, 200, 300, 400, 500, 600, 800, 1000};
        int index = Math.min(level - 1, smallBlinds.length - 1);
        int sb = smallBlinds[index];
        return isSmall ? sb : sb * 2;
    }

    public int getPlayersRemaining() {
        return (int) registrations.stream()
            .filter(r -> !r.isEliminated())
            .count();
    }

    public List<TournamentRegistration> getActivePlayers() {
        return registrations.stream()
            .filter(r -> !r.isEliminated())
            .toList();
    }

    

    public int[] getPrizeDistribution() {
        int players = registrations.size();
        if (players <= 2) {
            return new int[]{100}; 
        } else if (players <= 4) {
            return new int[]{70, 30}; 
        } else if (players <= 6) {
            return new int[]{50, 30, 20}; 
        } else {
            return new int[]{45, 25, 15, 10, 5}; 
        }
    }

    public int getPrizeForPosition(int position) {
        int[] distribution = getPrizeDistribution();
        if (position < 1 || position > distribution.length) {
            return 0;
        }
        return (prizePool * distribution[position - 1]) / 100;
    }

    

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TournamentStatus getStatus() { return status; }
    public void setStatus(TournamentStatus status) { this.status = status; }

    public TournamentType getTournamentType() { return tournamentType; }
    public void setTournamentType(TournamentType tournamentType) { this.tournamentType = tournamentType; }

    public int getBuyIn() { return buyIn; }
    public void setBuyIn(int buyIn) { this.buyIn = buyIn; }

    public int getStartingChips() { return startingChips; }
    public void setStartingChips(int startingChips) { this.startingChips = startingChips; }

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }

    public int getSmallBlind() { return smallBlind; }
    public void setSmallBlind(int smallBlind) { this.smallBlind = smallBlind; }

    public int getBigBlind() { return bigBlind; }
    public void setBigBlind(int bigBlind) { this.bigBlind = bigBlind; }

    public int getBlindIncreaseMinutes() { return blindIncreaseMinutes; }
    public void setBlindIncreaseMinutes(int blindIncreaseMinutes) { this.blindIncreaseMinutes = blindIncreaseMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public int getPrizePool() { return prizePool; }
    public void setPrizePool(int prizePool) { this.prizePool = prizePool; }

    public UUID getActiveGameId() { return activeGameId; }
    public void setActiveGameId(UUID activeGameId) { this.activeGameId = activeGameId; }

    public List<TournamentRegistration> getRegistrations() { return registrations; }
    public void setRegistrations(List<TournamentRegistration> registrations) { this.registrations = registrations; }

    

    public enum TournamentStatus {
        REGISTERING,
        RUNNING,
        PAUSED,
        FINISHED,
        CANCELLED
    }

    public enum TournamentType {
        SIT_AND_GO,      
        SCHEDULED,       
        HEADS_UP         
    }
}
