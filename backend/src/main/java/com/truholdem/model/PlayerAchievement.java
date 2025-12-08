package com.truholdem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "player_achievements", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_stats_id", "achievement_id"}))
public class PlayerAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_stats_id", nullable = false)
    private PlayerStatistics playerStats;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(nullable = false)
    private LocalDateTime unlockedAt;

    private int progress = 0;

    
    public PlayerAchievement() {}

    public PlayerAchievement(PlayerStatistics playerStats, Achievement achievement) {
        this.playerStats = playerStats;
        this.achievement = achievement;
        this.unlockedAt = LocalDateTime.now();
        this.progress = achievement.getRequirementValue();
    }

    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PlayerStatistics getPlayerStats() { return playerStats; }
    public void setPlayerStats(PlayerStatistics playerStats) { this.playerStats = playerStats; }

    public Achievement getAchievement() { return achievement; }
    public void setAchievement(Achievement achievement) { this.achievement = achievement; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}
