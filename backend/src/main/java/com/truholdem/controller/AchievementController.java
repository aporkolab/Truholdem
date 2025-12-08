package com.truholdem.controller;

import com.truholdem.model.Achievement;
import com.truholdem.model.PlayerAchievement;
import com.truholdem.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/achievements")
@Tag(name = "Achievements", description = "Achievement management")
@CrossOrigin(origins = "*")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    @Operation(summary = "Get all achievements")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @GetMapping("/visible")
    @Operation(summary = "Get visible achievements (excludes hidden)")
    public ResponseEntity<List<Achievement>> getVisibleAchievements() {
        return ResponseEntity.ok(achievementService.getVisibleAchievements());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get achievements by category")
    public ResponseEntity<List<Achievement>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(achievementService.getAchievementsByCategory(category));
    }

    @GetMapping("/player/{playerName}")
    @Operation(summary = "Get player's unlocked achievements")
    public ResponseEntity<List<PlayerAchievement>> getPlayerAchievements(@PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerAchievements(playerName));
    }

    @GetMapping("/player/{playerName}/progress")
    @Operation(summary = "Get player's achievement progress")
    public ResponseEntity<List<AchievementService.AchievementProgress>> getPlayerProgress(
            @PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerProgress(playerName));
    }

    @GetMapping("/player/{playerName}/summary")
    @Operation(summary = "Get player's achievement summary")
    public ResponseEntity<AchievementService.AchievementSummary> getPlayerSummary(
            @PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerSummary(playerName));
    }

    @GetMapping("/player/{playerName}/points")
    @Operation(summary = "Get player's total achievement points")
    public ResponseEntity<Integer> getPlayerPoints(@PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerTotalPoints(playerName));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recently unlocked achievements")
    public ResponseEntity<List<PlayerAchievement>> getRecentUnlocks() {
        return ResponseEntity.ok(achievementService.getRecentUnlocks());
    }

    @PostMapping("/check/{playerName}")
    @Operation(summary = "Check and unlock new achievements for a player")
    public ResponseEntity<List<Achievement>> checkAchievements(@PathVariable String playerName) {
        List<Achievement> newlyUnlocked = achievementService.checkAndUnlockAchievements(playerName);
        return ResponseEntity.ok(newlyUnlocked);
    }
}
