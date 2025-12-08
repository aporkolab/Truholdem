package com.truholdem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "player_statistics", indexes = {
    @Index(name = "idx_player_stats_user", columnList = "userId"),
    @Index(name = "idx_player_stats_name", columnList = "playerName")
})
public class PlayerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    
    @Column(unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String playerName;

    
    private int handsPlayed = 0;
    private int handsWon = 0;
    private BigDecimal totalWinnings = BigDecimal.ZERO;
    private BigDecimal totalLosses = BigDecimal.ZERO;
    private int biggestPotWon = 0;

    
    private int handsPlayedFromButton = 0;
    private int handsPlayedFromBlinds = 0;

    
    private int handsVoluntarilyPutInPot = 0;

    
    private int handsRaisedPreFlop = 0;

    
    private int totalBets = 0;
    private int totalRaises = 0;
    private int totalCalls = 0;
    private int totalFolds = 0;
    private int totalChecks = 0;

    
    private int handsWentToShowdown = 0;
    private int showdownsWon = 0;

    
    private int timesAllIn = 0;
    private int allInsWon = 0;

    
    private int currentWinStreak = 0;
    private int longestWinStreak = 0;
    private int currentLoseStreak = 0;
    private int longestLoseStreak = 0;

    
    private LocalDateTime firstHandPlayed;
    private LocalDateTime lastHandPlayed;
    private int totalSessions = 0;

    
    public PlayerStatistics() {}

    public PlayerStatistics(String playerName) {
        this.playerName = playerName;
        this.firstHandPlayed = LocalDateTime.now();
    }

    public PlayerStatistics(UUID userId, String playerName) {
        this.userId = userId;
        this.playerName = playerName;
        this.firstHandPlayed = LocalDateTime.now();
    }

    

    
    public double getVPIP() {
        if (handsPlayed == 0) return 0;
        return (double) handsVoluntarilyPutInPot / handsPlayed * 100;
    }

    
    public double getPFR() {
        if (handsPlayed == 0) return 0;
        return (double) handsRaisedPreFlop / handsPlayed * 100;
    }

    
    public double getAggressionFactor() {
        if (totalCalls == 0) return (totalBets + totalRaises) > 0 ? 999 : 0;
        return (double) (totalBets + totalRaises) / totalCalls;
    }

    
    public double getWTSD() {
        if (handsPlayed == 0) return 0;
        return (double) handsWentToShowdown / handsPlayed * 100;
    }

    
    public double getWonAtShowdown() {
        if (handsWentToShowdown == 0) return 0;
        return (double) showdownsWon / handsWentToShowdown * 100;
    }

    
    public double getWinRate() {
        if (handsPlayed == 0) return 0;
        return (double) handsWon / handsPlayed * 100;
    }

    
    public BigDecimal getNetProfit() {
        return totalWinnings.subtract(totalLosses);
    }

    
    public BigDecimal getAverageProfit() {
        if (handsPlayed == 0) return BigDecimal.ZERO;
        return getNetProfit().divide(BigDecimal.valueOf(handsPlayed), 2, RoundingMode.HALF_UP);
    }

    
    public double getAllInWinRate() {
        if (timesAllIn == 0) return 0;
        return (double) allInsWon / timesAllIn * 100;
    }

    
    public double getFoldPercentage() {
        int totalActions = totalBets + totalRaises + totalCalls + totalFolds + totalChecks;
        if (totalActions == 0) return 0;
        return (double) totalFolds / totalActions * 100;
    }

    

    public void recordHandPlayed(boolean voluntarilyPutIn, boolean raisedPreFlop) {
        handsPlayed++;
        lastHandPlayed = LocalDateTime.now();
        
        if (voluntarilyPutIn) {
            handsVoluntarilyPutInPot++;
        }
        if (raisedPreFlop) {
            handsRaisedPreFlop++;
        }
    }

    public void recordBet() {
        totalBets++;
    }

    public void recordRaise() {
        totalRaises++;
    }

    public void recordCall() {
        totalCalls++;
    }

    public void recordFold() {
        totalFolds++;
    }

    public void recordCheck() {
        totalChecks++;
    }

    public void recordAllIn() {
        timesAllIn++;
    }

    public void recordShowdown(boolean won) {
        handsWentToShowdown++;
        if (won) {
            showdownsWon++;
        }
    }

    public void recordWin(int potAmount) {
        handsWon++;
        totalWinnings = totalWinnings.add(BigDecimal.valueOf(potAmount));
        
        if (potAmount > biggestPotWon) {
            biggestPotWon = potAmount;
        }

        
        currentWinStreak++;
        currentLoseStreak = 0;
        if (currentWinStreak > longestWinStreak) {
            longestWinStreak = currentWinStreak;
        }
    }

    public void recordLoss(int amountLost) {
        totalLosses = totalLosses.add(BigDecimal.valueOf(amountLost));

        
        currentLoseStreak++;
        currentWinStreak = 0;
        if (currentLoseStreak > longestLoseStreak) {
            longestLoseStreak = currentLoseStreak;
        }
    }

    public void recordAllInResult(boolean won) {
        if (won) {
            allInsWon++;
        }
    }

    public void startNewSession() {
        totalSessions++;
    }

    

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getHandsPlayed() { return handsPlayed; }
    public void setHandsPlayed(int handsPlayed) { this.handsPlayed = handsPlayed; }

    public int getHandsWon() { return handsWon; }
    public void setHandsWon(int handsWon) { this.handsWon = handsWon; }

    public BigDecimal getTotalWinnings() { return totalWinnings; }
    public void setTotalWinnings(BigDecimal totalWinnings) { this.totalWinnings = totalWinnings; }

    public BigDecimal getTotalLosses() { return totalLosses; }
    public void setTotalLosses(BigDecimal totalLosses) { this.totalLosses = totalLosses; }

    public int getBiggestPotWon() { return biggestPotWon; }
    public void setBiggestPotWon(int biggestPotWon) { this.biggestPotWon = biggestPotWon; }

    public int getHandsVoluntarilyPutInPot() { return handsVoluntarilyPutInPot; }
    public void setHandsVoluntarilyPutInPot(int handsVoluntarilyPutInPot) { this.handsVoluntarilyPutInPot = handsVoluntarilyPutInPot; }
    
    public int getHandsRaisedPreFlop() { return handsRaisedPreFlop; }
    public void setHandsRaisedPreFlop(int handsRaisedPreFlop) { this.handsRaisedPreFlop = handsRaisedPreFlop; }
    
    public int getTotalBets() { return totalBets; }
    public void setTotalBets(int totalBets) { this.totalBets = totalBets; }
    
    public int getTotalRaises() { return totalRaises; }
    public void setTotalRaises(int totalRaises) { this.totalRaises = totalRaises; }
    
    public int getTotalCalls() { return totalCalls; }
    public void setTotalCalls(int totalCalls) { this.totalCalls = totalCalls; }
    
    public int getTotalFolds() { return totalFolds; }
    public void setTotalFolds(int totalFolds) { this.totalFolds = totalFolds; }
    
    public int getTotalChecks() { return totalChecks; }
    public void setTotalChecks(int totalChecks) { this.totalChecks = totalChecks; }
    
    public int getHandsWentToShowdown() { return handsWentToShowdown; }
    public void setHandsWentToShowdown(int handsWentToShowdown) { this.handsWentToShowdown = handsWentToShowdown; }
    
    public int getShowdownsWon() { return showdownsWon; }
    public void setShowdownsWon(int showdownsWon) { this.showdownsWon = showdownsWon; }
    
    public int getTimesAllIn() { return timesAllIn; }
    public void setTimesAllIn(int timesAllIn) { this.timesAllIn = timesAllIn; }
    
    public int getAllInsWon() { return allInsWon; }
    public void setAllInsWon(int allInsWon) { this.allInsWon = allInsWon; }
    
    public int getCurrentWinStreak() { return currentWinStreak; }
    public void setCurrentWinStreak(int currentWinStreak) { this.currentWinStreak = currentWinStreak; }
    
    public int getLongestWinStreak() { return longestWinStreak; }
    public void setLongestWinStreak(int longestWinStreak) { this.longestWinStreak = longestWinStreak; }
    
    public int getCurrentLoseStreak() { return currentLoseStreak; }
    public void setCurrentLoseStreak(int currentLoseStreak) { this.currentLoseStreak = currentLoseStreak; }
    
    public int getLongestLoseStreak() { return longestLoseStreak; }
    public void setLongestLoseStreak(int longestLoseStreak) { this.longestLoseStreak = longestLoseStreak; }
    
    public LocalDateTime getFirstHandPlayed() { return firstHandPlayed; }
    public void setFirstHandPlayed(LocalDateTime firstHandPlayed) { this.firstHandPlayed = firstHandPlayed; }
    
    public LocalDateTime getLastHandPlayed() { return lastHandPlayed; }
    public void setLastHandPlayed(LocalDateTime lastHandPlayed) { this.lastHandPlayed = lastHandPlayed; }
    
    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
}
