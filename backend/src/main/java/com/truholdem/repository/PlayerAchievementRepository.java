package com.truholdem.repository;

import com.truholdem.model.PlayerAchievement;
import com.truholdem.model.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, UUID> {

    List<PlayerAchievement> findByPlayerStats(PlayerStatistics playerStats);

    List<PlayerAchievement> findByPlayerStatsIdOrderByUnlockedAtDesc(UUID playerStatsId);

    Optional<PlayerAchievement> findByPlayerStatsAndAchievementId(PlayerStatistics playerStats, UUID achievementId);

    @Query("SELECT pa FROM PlayerAchievement pa WHERE pa.playerStats.id = :statsId AND pa.achievement.code = :code")
    Optional<PlayerAchievement> findByPlayerStatsIdAndAchievementCode(
        @Param("statsId") UUID statsId, 
        @Param("code") String achievementCode
    );

    boolean existsByPlayerStatsAndAchievementId(PlayerStatistics playerStats, UUID achievementId);

    @Query("SELECT COUNT(pa) FROM PlayerAchievement pa WHERE pa.playerStats.id = :statsId")
    long countByPlayerStatsId(@Param("statsId") UUID statsId);

    @Query("SELECT SUM(pa.achievement.points) FROM PlayerAchievement pa WHERE pa.playerStats.id = :statsId")
    Integer getTotalPointsByPlayerStatsId(@Param("statsId") UUID statsId);

    
    List<PlayerAchievement> findTop20ByOrderByUnlockedAtDesc();
}
