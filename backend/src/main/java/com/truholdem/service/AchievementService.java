package com.truholdem.service;

import com.truholdem.model.Achievement;
import com.truholdem.model.PlayerAchievement;
import com.truholdem.model.PlayerStatistics;
import com.truholdem.repository.AchievementRepository;
import com.truholdem.repository.PlayerAchievementRepository;
import com.truholdem.repository.PlayerStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerStatisticsRepository statsRepository;
    private final GameNotificationService notificationService;

    public AchievementService(
            AchievementRepository achievementRepository,
            PlayerAchievementRepository playerAchievementRepository,
            PlayerStatisticsRepository statsRepository,
            GameNotificationService notificationService) {
        this.achievementRepository = achievementRepository;
        this.playerAchievementRepository = playerAchievementRepository;
        this.statsRepository = statsRepository;
        this.notificationService = notificationService;
    }

    

    
    public List<Achievement> checkAndUnlockAchievements(String playerName) {
        Optional<PlayerStatistics> optStats = statsRepository.findByPlayerName(playerName);
        if (optStats.isEmpty()) {
            return Collections.emptyList();
        }

        PlayerStatistics stats = optStats.get();
        List<Achievement> newlyUnlocked = new ArrayList<>();


        List<Achievement> allAchievements = achievementRepository.findAll();

        Set<UUID> alreadyUnlockedIds = playerAchievementRepository.findByPlayerStats(stats)
                .stream()
                .map(pa -> pa.getAchievement().getId())
                .collect(Collectors.toSet());

        for (Achievement achievement : allAchievements) {

            if (alreadyUnlockedIds.contains(achievement.getId())) {
                continue;
            }


            if (achievement.checkRequirement(stats)) {
                unlockAchievement(stats, achievement);
                newlyUnlocked.add(achievement);
                logger.info("Player {} unlocked achievement: {}", playerName, achievement.getName());
            }
        }

        return newlyUnlocked;
    }

    
    private void unlockAchievement(PlayerStatistics stats, Achievement achievement) {
        PlayerAchievement playerAchievement = new PlayerAchievement(stats, achievement);
        playerAchievementRepository.save(playerAchievement);

        
        logger.debug("Achievement {} unlocked for {}", achievement.getCode(), stats.getPlayerName());
    }

    

    
    @Transactional(readOnly = true)
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    
    @Transactional(readOnly = true)
    public List<Achievement> getVisibleAchievements() {
        return achievementRepository.findByIsHiddenFalse();
    }

    
    @Transactional(readOnly = true)
    public List<Achievement> getAchievementsByCategory(String category) {
        return achievementRepository.findByCategory(category);
    }

    
    @Transactional(readOnly = true)
    public List<PlayerAchievement> getPlayerAchievements(String playerName) {
        Optional<PlayerStatistics> optStats = statsRepository.findByPlayerName(playerName);
        if (optStats.isEmpty()) {
            return Collections.emptyList();
        }

        return playerAchievementRepository.findByPlayerStatsIdOrderByUnlockedAtDesc(optStats.get().getId());
    }

    
    @Transactional(readOnly = true)
    public List<AchievementProgress> getPlayerProgress(String playerName) {
        Optional<PlayerStatistics> optStats = statsRepository.findByPlayerName(playerName);
        if (optStats.isEmpty()) {
            return Collections.emptyList();
        }

        PlayerStatistics stats = optStats.get();
        List<Achievement> allAchievements = achievementRepository.findByIsHiddenFalse();
        Set<UUID> unlockedIds = playerAchievementRepository.findByPlayerStats(stats).stream()
            .map(pa -> pa.getAchievement().getId())
            .collect(Collectors.toSet());

        List<AchievementProgress> progressList = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            boolean isUnlocked = unlockedIds.contains(achievement.getId());
            int currentProgress = getCurrentProgress(stats, achievement.getRequirementType());
            int required = achievement.getRequirementValue();
            int progressPercentage = required > 0
                    ? Math.min(100, (int) ((double) currentProgress / required * 100))
                    : (isUnlocked ? 100 : 0);

            progressList.add(new AchievementProgress(
                achievement,
                isUnlocked,
                currentProgress,
                required,
                progressPercentage
            ));
        }

        
        progressList.sort((a, b) -> {
            if (a.isUnlocked() != b.isUnlocked()) {
                return a.isUnlocked() ? -1 : 1;
            }
            return Integer.compare(b.progressPercentage(), a.progressPercentage());
        });

        return progressList;
    }

    private int getCurrentProgress(PlayerStatistics stats, String requirementType) {
        if (requirementType == null) return 0;

        return switch (requirementType) {
            case "HANDS_WON" -> stats.getHandsWon();
            case "HANDS_PLAYED" -> stats.getHandsPlayed();
            case "BIGGEST_POT" -> stats.getBiggestPotWon();
            case "WIN_STREAK" -> stats.getLongestWinStreak();
            case "ALL_INS_WON" -> stats.getAllInsWon();
            case "SHOWDOWNS_WON" -> stats.getShowdownsWon();
            case "TOTAL_SESSIONS" -> stats.getTotalSessions();
            default -> 0;
        };
    }

    
    @Transactional(readOnly = true)
    public int getPlayerTotalPoints(String playerName) {
        Optional<PlayerStatistics> optStats = statsRepository.findByPlayerName(playerName);
        if (optStats.isEmpty()) {
            return 0;
        }

        Integer points = playerAchievementRepository.getTotalPointsByPlayerStatsId(optStats.get().getId());
        return points != null ? points : 0;
    }

    
    @Transactional(readOnly = true)
    public List<PlayerAchievement> getRecentUnlocks() {
        return playerAchievementRepository.findTop20ByOrderByUnlockedAtDesc();
    }

    
    @Transactional(readOnly = true)
    public AchievementSummary getPlayerSummary(String playerName) {
        Optional<PlayerStatistics> optStats = statsRepository.findByPlayerName(playerName);
        if (optStats.isEmpty()) {
            return new AchievementSummary(playerName, 0, 0, 0, Collections.emptyList());
        }

        PlayerStatistics stats = optStats.get();
        long totalAchievements = achievementRepository.count();
        long unlockedCount = playerAchievementRepository.countByPlayerStatsId(stats.getId());
        int totalPoints = getPlayerTotalPoints(playerName);

        List<PlayerAchievement> recentUnlocks = playerAchievementRepository
            .findByPlayerStatsIdOrderByUnlockedAtDesc(stats.getId());
        
        List<AchievementInfo> recentInfos = recentUnlocks.stream()
            .limit(5)
            .map(pa -> new AchievementInfo(
                pa.getAchievement().getCode(),
                pa.getAchievement().getName(),
                pa.getAchievement().getIcon(),
                pa.getUnlockedAt()
            ))
            .toList();

        return new AchievementSummary(
            playerName,
            (int) unlockedCount,
            (int) totalAchievements,
            totalPoints,
            recentInfos
        );
    }

    

    public record AchievementProgress(
        Achievement achievement,
        boolean isUnlocked,
        int currentProgress,
        int requiredProgress,
        int progressPercentage
    ) {}

    public record AchievementSummary(
        String playerName,
        int unlockedCount,
        int totalAchievements,
        int totalPoints,
        List<AchievementInfo> recentUnlocks
    ) {}

    public record AchievementInfo(
        String code,
        String name,
        String icon,
        java.time.LocalDateTime unlockedAt
    ) {}
}
