package com.truholdem.controller;

import com.truholdem.config.api.ApiV1Config;
import com.truholdem.model.Achievement;
import com.truholdem.model.PlayerAchievement;
import com.truholdem.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@ApiV1Config
@RequestMapping("/achievements")
@Tag(name = "Achievements", description = "Achievement system - badges, progress tracking, and rewards")
@SecurityRequirement(name = "bearerAuth")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    @Operation(
        summary = "Get all achievements",
        description = "Retrieve all available achievements including hidden ones"
    )
    @ApiResponse(responseCode = "200", description = "Achievements retrieved successfully")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @GetMapping("/visible")
    @Operation(
        summary = "Get visible achievements",
        description = "Retrieve all achievements excluding hidden/secret achievements"
    )
    @ApiResponse(responseCode = "200", description = "Visible achievements retrieved successfully")
    public ResponseEntity<List<Achievement>> getVisibleAchievements() {
        return ResponseEntity.ok(achievementService.getVisibleAchievements());
    }

    @GetMapping("/category/{category}")
    @Operation(
        summary = "Get achievements by category",
        description = "Retrieve achievements filtered by category (e.g., BEGINNER, EXPERT, SOCIAL)"
    )
    @ApiResponse(responseCode = "200", description = "Category achievements retrieved successfully")
    public ResponseEntity<List<Achievement>> getByCategory(
            @Parameter(description = "Achievement category", example = "EXPERT")
            @PathVariable String category) {
        return ResponseEntity.ok(achievementService.getAchievementsByCategory(category));
    }

    @GetMapping("/player/{playerName}")
    @Operation(
        summary = "Get player's unlocked achievements",
        description = "Retrieve all achievements that a player has earned"
    )
    @ApiResponse(responseCode = "200", description = "Player achievements retrieved successfully")
    public ResponseEntity<List<PlayerAchievement>> getPlayerAchievements(
            @Parameter(description = "Player username") @PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerAchievements(playerName));
    }

    @GetMapping("/player/{playerName}/progress")
    @Operation(
        summary = "Get player's achievement progress",
        description = """
            Retrieve progress on all achievements for a player.
            
            **Includes:**
            - Unlocked achievements with completion date
            - In-progress achievements with current progress
            - Locked achievements with requirements
            """
    )
    @ApiResponse(responseCode = "200", description = "Progress retrieved successfully")
    public ResponseEntity<List<AchievementService.AchievementProgress>> getPlayerProgress(
            @Parameter(description = "Player username") @PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerProgress(playerName));
    }

    @GetMapping("/player/{playerName}/summary")
    @Operation(
        summary = "Get player's achievement summary",
        description = "Get aggregated achievement statistics for a player"
    )
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    public ResponseEntity<AchievementService.AchievementSummary> getPlayerSummary(
            @Parameter(description = "Player username") @PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerSummary(playerName));
    }

    @GetMapping("/player/{playerName}/points")
    @Operation(
        summary = "Get player's total achievement points",
        description = "Get the total points earned from all unlocked achievements"
    )
    @ApiResponse(responseCode = "200", description = "Points retrieved successfully")
    public ResponseEntity<Integer> getPlayerPoints(
            @Parameter(description = "Player username") @PathVariable String playerName) {
        return ResponseEntity.ok(achievementService.getPlayerTotalPoints(playerName));
    }

    @GetMapping("/recent")
    @Operation(
        summary = "Get recently unlocked achievements",
        description = "Retrieve achievements that were recently unlocked by any player"
    )
    @ApiResponse(responseCode = "200", description = "Recent achievements retrieved successfully")
    public ResponseEntity<List<PlayerAchievement>> getRecentUnlocks() {
        return ResponseEntity.ok(achievementService.getRecentUnlocks());
    }

    @PostMapping("/check/{playerName}")
    @Operation(
        summary = "Check and unlock achievements",
        description = """
            Evaluate player's current stats and unlock any newly earned achievements.
            
            **Returns:** List of achievements that were just unlocked (empty if none)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Achievements checked successfully",
            content = @Content(schema = @Schema(implementation = Achievement.class))
        )
    })
    public ResponseEntity<List<Achievement>> checkAchievements(
            @Parameter(description = "Player username") @PathVariable String playerName) {
        List<Achievement> newlyUnlocked = achievementService.checkAndUnlockAchievements(playerName);
        return ResponseEntity.ok(newlyUnlocked);
    }
}
