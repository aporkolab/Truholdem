package com.truholdem.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Entity
@Table(name = "tournaments", indexes = {
    @Index(name = "idx_tournament_status", columnList = "status"),
    @Index(name = "idx_tournament_type", columnList = "tournamentType"),
    @Index(name = "idx_tournament_start", columnList = "startTime")
})
public class Tournament {

    private static final int MAX_PLAYERS_PER_TABLE = 9;
    private static final int MIN_PLAYERS_TO_START = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentType tournamentType = TournamentType.FREEZEOUT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status = TournamentStatus.REGISTERING;

    @Embedded
    private BlindStructure blindStructure;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("tableNumber ASC")
    private List<TournamentTable> tables = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("registeredAt ASC")
    private List<TournamentRegistration> registrations = new ArrayList<>();

    
    private int startingChips = 1500;
    private int minPlayers = 2;
    private int maxPlayers = 9;
    private int currentLevel = 1;

    
    private int buyIn = 100;
    private int rebuyAmount = 0;
    private int rebuyDeadlineLevel = 0;
    private int maxRebuys = 0;
    private int addOnAmount = 0;
    private int bountyAmount = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tournament_payout_structure",
        joinColumns = @JoinColumn(name = "tournament_id")
    )
    @OrderColumn(name = "position_order")
    @Column(name = "percentage")
    private List<Integer> payoutStructure = new ArrayList<>();

    
    private Instant levelStartTime;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    private Instant startTime;
    private Instant endTime;

    
    protected Tournament() {
        this.createdAt = Instant.now();
        this.blindStructure = BlindStructure.standard();
        initializeDefaultPayoutStructure();
    }

    public Tournament(String name, TournamentType type) {
        this();
        this.name = Objects.requireNonNull(name, "Tournament name cannot be null");
        this.tournamentType = Objects.requireNonNull(type, "Tournament type cannot be null");
        configureForType(type);
    }
    
    
    public static TournamentBuilder builder(String name) {
        return new TournamentBuilder(name);
    }

    private void configureForType(TournamentType type) {
        switch (type) {
            case REBUY -> {
                this.rebuyAmount = buyIn;
                this.rebuyDeadlineLevel = 6;
                this.maxRebuys = 3;
                this.addOnAmount = buyIn * 2;
            }
            case BOUNTY -> {
                this.bountyAmount = buyIn / 2;
            }
            case SIT_AND_GO -> {
                this.maxPlayers = 9;
                this.blindStructure = BlindStructure.turbo();
            }
            case MULTI_TABLE -> {
                this.maxPlayers = 100;
                this.blindStructure = BlindStructure.standard();
            }
            default -> {
                
            }
        }
    }

    private void initializeDefaultPayoutStructure() {
        
        this.payoutStructure = new ArrayList<>(List.of(50, 30, 20));
    }

    

    
    public void start() {
        validateCanStart();
        
        this.status = TournamentStatus.RUNNING;
        this.startTime = Instant.now();
        this.levelStartTime = Instant.now();
        this.currentLevel = 1;
        
        
        registrations.forEach(reg -> {
            reg.setChips(startingChips);
            reg.startPlaying();
        });
        
        
        createInitialTables();
    }

    private void validateCanStart() {
        if (status != TournamentStatus.REGISTERING) {
            throw new IllegalStateException("Tournament not in REGISTERING status");
        }
        if (registrations.size() < minPlayers) {
            throw new IllegalStateException(
                String.format("Not enough players: %d registered, %d required", 
                              registrations.size(), minPlayers));
        }
    }

    private void createInitialTables() {
        tables.clear();
        
        List<TournamentRegistration> activeRegs = getActiveRegistrations();
        int playerCount = activeRegs.size();
        int tableCount = (int) Math.ceil((double) playerCount / MAX_PLAYERS_PER_TABLE);
        
        
        for (int i = 0; i < tableCount; i++) {
            TournamentTable table = new TournamentTable(this, i + 1);
            tables.add(table);
        }
        
        
        for (int i = 0; i < activeRegs.size(); i++) {
            TournamentTable table = tables.get(i % tableCount);
            table.seatPlayer(activeRegs.get(i).getPlayerId());
        }
    }

    
    public void advanceLevel() {
        if (!status.isPlayable()) {
            throw new IllegalStateException("Tournament not in playable state");
        }
        
        this.currentLevel++;
        this.levelStartTime = Instant.now();
        
        
        if (tournamentType == TournamentType.REBUY && currentLevel > rebuyDeadlineLevel) {
            
        }
    }

    
    public BlindLevel getCurrentBlindLevel() {
        return blindStructure.getLevelAt(currentLevel);
    }

    
    public boolean shouldAdvanceLevel() {
        if (levelStartTime == null) return false;
        
        Duration elapsed = Duration.between(levelStartTime, Instant.now());
        return elapsed.toMinutes() >= blindStructure.getLevelDurationMinutes();
    }

    

    
    public void eliminatePlayer(UUID playerId) {
        TournamentRegistration reg = findRegistration(playerId)
            .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
        
        if (!reg.isActive()) {
            throw new IllegalStateException("Player already eliminated");
        }
        
        int position = getPlayersRemaining();
        int prize = calculatePrizeForPosition(position);
        
        reg.eliminate(position, prize);
        
        
        tables.stream()
              .filter(t -> t.hasPlayer(playerId))
              .findFirst()
              .ifPresent(t -> t.removePlayer(playerId));
        
        
        updateTournamentStatus();
    }

    
    public void recordBounty(UUID eliminatorId, UUID eliminatedId) {
        if (tournamentType != TournamentType.BOUNTY) {
            return;
        }
        
        TournamentRegistration eliminator = findRegistration(eliminatorId)
            .orElseThrow(() -> new IllegalArgumentException("Eliminator not found"));
        TournamentRegistration eliminated = findRegistration(eliminatedId)
            .orElseThrow(() -> new IllegalArgumentException("Eliminated player not found"));
        
        eliminator.collectBounty(eliminated.getBountyValue());
    }

    private void updateTournamentStatus() {
        int remaining = getPlayersRemaining();
        
        if (remaining == 1) {
            
            completeWithWinner();
        } else if (remaining == 2) {
            this.status = TournamentStatus.HEADS_UP;
        } else if (remaining <= MAX_PLAYERS_PER_TABLE && tables.size() > 1) {
            consolidateToFinalTable();
        }
    }

    private void consolidateToFinalTable() {
        
        tables.forEach(TournamentTable::close);
        
        
        TournamentTable finalTable = TournamentTable.createFinalTable(this);
        
        
        getActiveRegistrations().forEach(reg -> 
            finalTable.seatPlayer(reg.getPlayerId()));
        
        tables.add(finalTable);
        this.status = TournamentStatus.FINAL_TABLE;
    }

    private void completeWithWinner() {
        TournamentRegistration winner = getActiveRegistrations().stream()
            .findFirst()
            .orElseThrow();
        
        int firstPrize = calculatePrizeForPosition(1);
        winner.finish(1, firstPrize);
        
        this.status = TournamentStatus.COMPLETED;
        this.endTime = Instant.now();
    }

    

    
    public void rebalanceTables() {
        List<TournamentTable> activeTables = getActiveTables();
        
        if (activeTables.size() <= 1) {
            return;
        }
        
        
        activeTables.sort(Comparator.comparingInt(TournamentTable::getPlayerCount));
        
        while (shouldRebalance(activeTables)) {
            TournamentTable smallest = activeTables.get(0);
            TournamentTable largest = activeTables.get(activeTables.size() - 1);
            
            if (largest.getPlayerCount() - smallest.getPlayerCount() <= 1) {
                break;
            }
            
            
            UUID playerToMove = largest.getPlayerIds().get(largest.getPlayerCount() - 1);
            largest.removePlayer(playerToMove);
            smallest.seatPlayer(playerToMove);
            
            
            activeTables.sort(Comparator.comparingInt(TournamentTable::getPlayerCount));
        }
        
        
        tables.stream()
              .filter(t -> t.isActive() && t.getPlayerCount() == 0)
              .forEach(TournamentTable::close);
    }

    private boolean shouldRebalance(List<TournamentTable> activeTables) {
        if (activeTables.size() <= 1) return false;
        
        int min = activeTables.get(0).getPlayerCount();
        int max = activeTables.get(activeTables.size() - 1).getPlayerCount();
        
        return max - min > 1;
    }

    

    
    public TournamentRegistration registerPlayer(UUID playerId, String playerName) {
        validateCanRegister();
        
        if (isPlayerRegistered(playerId)) {
            throw new IllegalStateException("Player already registered");
        }
        
        TournamentRegistration registration = new TournamentRegistration(this, playerId, playerName);
        registrations.add(registration);
        
        return registration;
    }

    private void validateCanRegister() {
        if (!status.allowsRegistration()) {
            throw new IllegalStateException("Registration not allowed in status: " + status);
        }
        if (registrations.size() >= maxPlayers) {
            throw new IllegalStateException("Tournament is full");
        }
    }

    
    public boolean unregisterPlayer(UUID playerId) {
        if (status != TournamentStatus.REGISTERING) {
            throw new IllegalStateException("Can only unregister during REGISTERING status");
        }
        
        return registrations.removeIf(r -> r.getPlayerId().equals(playerId));
    }

    

    
    public int getPrizePool() {
        int basePrizePool = buyIn * registrations.size();
        int rebuyPrizePool = rebuyAmount * getTotalRebuys();
        int addOnPrizePool = addOnAmount * getTotalAddOns();
        
        
        if (tournamentType == TournamentType.BOUNTY) {
            return basePrizePool + rebuyPrizePool + addOnPrizePool - getTotalBountyPool();
        }
        
        return basePrizePool + rebuyPrizePool + addOnPrizePool;
    }

    private int getTotalBountyPool() {
        return bountyAmount * registrations.size();
    }

    private int getTotalRebuys() {
        return registrations.stream()
                           .mapToInt(TournamentRegistration::getRebuysUsed)
                           .sum();
    }

    private int getTotalAddOns() {
        return registrations.stream()
                           .mapToInt(TournamentRegistration::getAddOnsUsed)
                           .sum();
    }

    
    public int calculatePrizeForPosition(int position) {
        if (position < 1 || position > payoutStructure.size()) {
            return 0;
        }
        
        int prizePool = getPrizePool();
        int percentage = payoutStructure.get(position - 1);
        
        return (prizePool * percentage) / 100;
    }

    
    public int getPaidPositions() {
        return payoutStructure.size();
    }

    

    public int getPlayersRemaining() {
        return (int) registrations.stream()
                                  .filter(TournamentRegistration::isActive)
                                  .count();
    }

    public List<TournamentRegistration> getActiveRegistrations() {
        return registrations.stream()
                           .filter(TournamentRegistration::isActive)
                           .collect(Collectors.toList());
    }

    public List<TournamentTable> getActiveTables() {
        return tables.stream()
                     .filter(TournamentTable::isActive)
                     .collect(Collectors.toList());
    }

    public Optional<TournamentRegistration> findRegistration(UUID playerId) {
        return registrations.stream()
                           .filter(r -> r.getPlayerId().equals(playerId))
                           .findFirst();
    }

    public boolean isPlayerRegistered(UUID playerId) {
        return registrations.stream()
                           .anyMatch(r -> r.getPlayerId().equals(playerId));
    }

    public boolean canStart() {
        return status == TournamentStatus.REGISTERING && 
               registrations.size() >= minPlayers;
    }

    public boolean canRegister() {
        return status.allowsRegistration() && 
               registrations.size() < maxPlayers;
    }

    
    public Optional<TournamentRegistration> getChipLeader() {
        return getActiveRegistrations().stream()
                                       .max(Comparator.comparingInt(TournamentRegistration::getCurrentChips));
    }

    
    public int getAverageStack() {
        List<TournamentRegistration> active = getActiveRegistrations();
        if (active.isEmpty()) return 0;
        
        int totalChips = active.stream()
                               .mapToInt(TournamentRegistration::getCurrentChips)
                               .sum();
        return totalChips / active.size();
    }

    

    public UUID getId() { return id; }
    public String getName() { return name; }
    public TournamentType getTournamentType() { return tournamentType; }
    public TournamentStatus getStatus() { return status; }
    public BlindStructure getBlindStructure() { return blindStructure; }
    public List<TournamentTable> getTables() { return Collections.unmodifiableList(tables); }
    public List<TournamentRegistration> getRegistrations() { return Collections.unmodifiableList(registrations); }
    public int getStartingChips() { return startingChips; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getCurrentLevel() { return currentLevel; }
    public int getBuyIn() { return buyIn; }
    public int getRebuyAmount() { return rebuyAmount; }
    public int getRebuyDeadlineLevel() { return rebuyDeadlineLevel; }
    public int getMaxRebuys() { return maxRebuys; }
    public int getAddOnAmount() { return addOnAmount; }
    public int getBountyAmount() { return bountyAmount; }
    public List<Integer> getPayoutStructure() { return Collections.unmodifiableList(payoutStructure); }
    public Instant getLevelStartTime() { return levelStartTime; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public Long getVersion() { return version; }

    

    protected void setName(String name) { this.name = name; }
    protected void setTournamentType(TournamentType type) { this.tournamentType = type; }
    protected void setBlindStructure(BlindStructure structure) { this.blindStructure = structure; }
    protected void setStartingChips(int chips) { this.startingChips = chips; }
    protected void setMinPlayers(int min) { this.minPlayers = min; }
    protected void setMaxPlayers(int max) { this.maxPlayers = max; }
    protected void setBuyIn(int buyIn) { this.buyIn = buyIn; }
    protected void setRebuyAmount(int amount) { this.rebuyAmount = amount; }
    protected void setRebuyDeadlineLevel(int level) { this.rebuyDeadlineLevel = level; }
    protected void setMaxRebuys(int max) { this.maxRebuys = max; }
    protected void setAddOnAmount(int amount) { this.addOnAmount = amount; }
    protected void setBountyAmount(int amount) { this.bountyAmount = amount; }
    protected void setPayoutStructure(List<Integer> structure) { 
        this.payoutStructure = new ArrayList<>(structure); 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tournament that = (Tournament) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Tournament{id=%s, name='%s', type=%s, status=%s, players=%d/%d}",
                             id, name, tournamentType, status, registrations.size(), maxPlayers);
    }

    

    public static class TournamentBuilder {
        private final Tournament tournament;

        private TournamentBuilder(String name) {
            this.tournament = new Tournament();
            this.tournament.setName(name);
        }

        public TournamentBuilder type(TournamentType type) {
            tournament.setTournamentType(type);
            tournament.configureForType(type);
            return this;
        }

        public TournamentBuilder blindStructure(BlindStructure structure) {
            tournament.setBlindStructure(structure);
            return this;
        }

        public TournamentBuilder startingChips(int chips) {
            tournament.setStartingChips(chips);
            return this;
        }

        public TournamentBuilder players(int min, int max) {
            tournament.setMinPlayers(min);
            tournament.setMaxPlayers(max);
            return this;
        }

        public TournamentBuilder buyIn(int amount) {
            tournament.setBuyIn(amount);
            return this;
        }

        public TournamentBuilder rebuy(int amount, int deadlineLevel, int maxRebuys) {
            tournament.setRebuyAmount(amount);
            tournament.setRebuyDeadlineLevel(deadlineLevel);
            tournament.setMaxRebuys(maxRebuys);
            return this;
        }

        public TournamentBuilder addOn(int amount) {
            tournament.setAddOnAmount(amount);
            return this;
        }

        public TournamentBuilder bounty(int amount) {
            tournament.setBountyAmount(amount);
            return this;
        }

        public TournamentBuilder payoutStructure(List<Integer> percentages) {
            tournament.setPayoutStructure(percentages);
            return this;
        }

        public Tournament build() {
            Objects.requireNonNull(tournament.name, "Tournament name is required");
            return tournament;
        }
    }
}
