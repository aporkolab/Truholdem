package com.truholdem.service;

import com.truholdem.model.PlayerStatistics;
import com.truholdem.repository.PlayerStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@Transactional
public class PlayerStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerStatisticsService.class);
    private static final int MIN_HANDS_FOR_LEADERBOARD = 10;

    private final PlayerStatisticsRepository statsRepository;

    public PlayerStatisticsService(PlayerStatisticsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    

    
    public PlayerStatistics getOrCreateStats(String playerName) {
        return statsRepository.findByPlayerName(playerName)
            .orElseGet(() -> {
                PlayerStatistics newStats = new PlayerStatistics(playerName);
                logger.info("Created new statistics for player: {}", playerName);
                return statsRepository.save(newStats);
            });
    }

    
    public PlayerStatistics getOrCreateStats(UUID userId, String playerName) {
        return statsRepository.findByUserId(userId)
            .orElseGet(() -> {
                PlayerStatistics newStats = new PlayerStatistics(userId, playerName);
                logger.info("Created new statistics for user: {}", userId);
                return statsRepository.save(newStats);
            });
    }

    

    
    @Transactional(readOnly = true)
    public Optional<PlayerStatistics> getStatsByName(String playerName) {
        return statsRepository.findByPlayerName(playerName);
    }

    
    @Transactional(readOnly = true)
    public Optional<PlayerStatistics> getStatsByUserId(UUID userId) {
        return statsRepository.findByUserId(userId);
    }

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> searchPlayers(String nameQuery) {
        return statsRepository.findByPlayerNameContainingIgnoreCase(nameQuery);
    }

    

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> getTopByHandsWon() {
        return statsRepository.findTop10ByOrderByHandsWonDesc();
    }

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> getTopByWinnings() {
        return statsRepository.findTop10ByOrderByTotalWinningsDesc();
    }

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> getTopByBiggestPot() {
        return statsRepository.findTop10ByOrderByBiggestPotWonDesc();
    }

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> getTopByWinStreak() {
        return statsRepository.findTop10ByOrderByLongestWinStreakDesc();
    }

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> getTopByWinRate() {
        return statsRepository.findTopPlayersByWinRate(MIN_HANDS_FOR_LEADERBOARD);
    }

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> getMostActive() {
        return statsRepository.findTop20ByOrderByHandsPlayedDesc();
    }

    
    @Transactional(readOnly = true)
    public List<PlayerStatistics> getRecentlyActive() {
        return statsRepository.findTop20ByOrderByLastHandPlayedDesc();
    }

    

    
    @Transactional(readOnly = true)
    public LeaderboardData getLeaderboard() {
        return new LeaderboardData(
            getTopByWinnings(),
            getTopByHandsWon(),
            getTopByWinRate(),
            getTopByBiggestPot(),
            getTopByWinStreak(),
            getMostActive()
        );
    }

    public record LeaderboardData(
        List<PlayerStatistics> byWinnings,
        List<PlayerStatistics> byHandsWon,
        List<PlayerStatistics> byWinRate,
        List<PlayerStatistics> byBiggestPot,
        List<PlayerStatistics> byWinStreak,
        List<PlayerStatistics> mostActive
    ) {}

    

    
    @Transactional(readOnly = true)
    public PlayerStatsSummary getStatsSummary(String playerName) {
        Optional<PlayerStatistics> optStats = statsRepository.findByPlayerName(playerName);
        if (optStats.isEmpty()) {
            return null;
        }

        PlayerStatistics stats = optStats.get();
        return new PlayerStatsSummary(
            stats.getPlayerName(),
            stats.getHandsPlayed(),
            stats.getHandsWon(),
            stats.getWinRate(),
            stats.getNetProfit(),
            stats.getVPIP(),
            stats.getPFR(),
            stats.getAggressionFactor(),
            stats.getWTSD(),
            stats.getWonAtShowdown(),
            stats.getBiggestPotWon(),
            stats.getLongestWinStreak(),
            stats.getTotalSessions()
        );
    }

    public record PlayerStatsSummary(
        String playerName,
        int handsPlayed,
        int handsWon,
        double winRate,
        java.math.BigDecimal netProfit,
        double vpip,
        double pfr,
        double aggressionFactor,
        double wtsd,
        double wonAtShowdown,
        int biggestPotWon,
        int longestWinStreak,
        int totalSessions
    ) {}

    

    
    public void recordHandPlayed(String playerName, boolean voluntarilyPutIn, boolean raisedPreFlop) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        stats.recordHandPlayed(voluntarilyPutIn, raisedPreFlop);
        statsRepository.save(stats);
    }

    
    public void recordAction(String playerName, String action) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        
        switch (action.toUpperCase()) {
            case "BET" -> stats.recordBet();
            case "RAISE" -> stats.recordRaise();
            case "CALL" -> stats.recordCall();
            case "FOLD" -> stats.recordFold();
            case "CHECK" -> stats.recordCheck();
        }
        
        statsRepository.save(stats);
    }

    
    public void recordAllIn(String playerName) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        stats.recordAllIn();
        statsRepository.save(stats);
    }

    
    public void recordShowdown(String playerName, boolean won) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        stats.recordShowdown(won);
        statsRepository.save(stats);
    }

    
    public void recordWin(String playerName, int potAmount) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        stats.recordWin(potAmount);
        statsRepository.save(stats);
        logger.debug("Recorded win for {}: {} chips", playerName, potAmount);
    }

    
    public void recordLoss(String playerName, int amountLost) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        stats.recordLoss(amountLost);
        statsRepository.save(stats);
    }

    
    public void recordAllInResult(String playerName, boolean won) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        stats.recordAllInResult(won);
        statsRepository.save(stats);
    }

    
    public void startSession(String playerName) {
        PlayerStatistics stats = getOrCreateStats(playerName);
        stats.startNewSession();
        statsRepository.save(stats);
    }
}
