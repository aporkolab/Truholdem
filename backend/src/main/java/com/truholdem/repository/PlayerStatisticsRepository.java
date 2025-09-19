package com.truholdem.repository;

import com.truholdem.model.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, UUID> {

    Optional<PlayerStatistics> findByUserId(UUID userId);

    Optional<PlayerStatistics> findByPlayerName(String playerName);

    
    List<PlayerStatistics> findTop10ByOrderByHandsWonDesc();

    List<PlayerStatistics> findTop10ByOrderByTotalWinningsDesc();

    List<PlayerStatistics> findTop10ByOrderByBiggestPotWonDesc();

    List<PlayerStatistics> findTop10ByOrderByLongestWinStreakDesc();

    
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.handsPlayed >= :minHands ORDER BY ps.totalWinnings DESC")
    List<PlayerStatistics> findTopPlayersByWinnings(int minHands);

    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.handsPlayed >= :minHands ORDER BY (ps.handsWon * 1.0 / ps.handsPlayed) DESC")
    List<PlayerStatistics> findTopPlayersByWinRate(int minHands);

    
    List<PlayerStatistics> findByPlayerNameContainingIgnoreCase(String name);

    
    List<PlayerStatistics> findTop20ByOrderByLastHandPlayedDesc();

    List<PlayerStatistics> findTop20ByOrderByHandsPlayedDesc();
}
