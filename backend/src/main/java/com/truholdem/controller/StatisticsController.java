package com.truholdem.controller;

import com.truholdem.model.PlayerStatistics;
import com.truholdem.service.PlayerStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Player statistics and leaderboards")
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final PlayerStatisticsService statsService;

    public StatisticsController(PlayerStatisticsService statsService) {
        this.statsService = statsService;
    }

    

    @GetMapping("/player/{playerName}")
    @Operation(summary = "Get statistics for a player")
    public ResponseEntity<PlayerStatistics> getPlayerStats(@PathVariable String playerName) {
        return statsService.getStatsByName(playerName)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/player/id/{userId}")
    @Operation(summary = "Get statistics by user ID")
    public ResponseEntity<PlayerStatistics> getPlayerStatsByUserId(@PathVariable UUID userId) {
        return statsService.getStatsByUserId(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/player/{playerName}/summary")
    @Operation(summary = "Get formatted statistics summary")
    public ResponseEntity<PlayerStatisticsService.PlayerStatsSummary> getPlayerStatsSummary(
            @PathVariable String playerName) {
        PlayerStatisticsService.PlayerStatsSummary summary = statsService.getStatsSummary(playerName);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/search")
    @Operation(summary = "Search players by name")
    public ResponseEntity<List<PlayerStatistics>> searchPlayers(@RequestParam String query) {
        List<PlayerStatistics> results = statsService.searchPlayers(query);
        return ResponseEntity.ok(results);
    }

    

    @GetMapping("/leaderboard")
    @Operation(summary = "Get comprehensive leaderboard")
    public ResponseEntity<PlayerStatisticsService.LeaderboardData> getLeaderboard() {
        PlayerStatisticsService.LeaderboardData leaderboard = statsService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/leaderboard/winnings")
    @Operation(summary = "Top players by total winnings")
    public ResponseEntity<List<PlayerStatistics>> getTopByWinnings() {
        List<PlayerStatistics> top = statsService.getTopByWinnings();
        return ResponseEntity.ok(top);
    }

    @GetMapping("/leaderboard/hands-won")
    @Operation(summary = "Top players by hands won")
    public ResponseEntity<List<PlayerStatistics>> getTopByHandsWon() {
        List<PlayerStatistics> top = statsService.getTopByHandsWon();
        return ResponseEntity.ok(top);
    }

    @GetMapping("/leaderboard/win-rate")
    @Operation(summary = "Top players by win rate")
    public ResponseEntity<List<PlayerStatistics>> getTopByWinRate() {
        List<PlayerStatistics> top = statsService.getTopByWinRate();
        return ResponseEntity.ok(top);
    }

    @GetMapping("/leaderboard/biggest-pot")
    @Operation(summary = "Top players by biggest pot won")
    public ResponseEntity<List<PlayerStatistics>> getTopByBiggestPot() {
        List<PlayerStatistics> top = statsService.getTopByBiggestPot();
        return ResponseEntity.ok(top);
    }

    @GetMapping("/leaderboard/win-streak")
    @Operation(summary = "Top players by longest win streak")
    public ResponseEntity<List<PlayerStatistics>> getTopByWinStreak() {
        List<PlayerStatistics> top = statsService.getTopByWinStreak();
        return ResponseEntity.ok(top);
    }

    @GetMapping("/leaderboard/most-active")
    @Operation(summary = "Most active players")
    public ResponseEntity<List<PlayerStatistics>> getMostActive() {
        List<PlayerStatistics> active = statsService.getMostActive();
        return ResponseEntity.ok(active);
    }

    @GetMapping("/leaderboard/recently-active")
    @Operation(summary = "Recently active players")
    public ResponseEntity<List<PlayerStatistics>> getRecentlyActive() {
        List<PlayerStatistics> recent = statsService.getRecentlyActive();
        return ResponseEntity.ok(recent);
    }
}
