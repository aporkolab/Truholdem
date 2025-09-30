package com.truholdem.controller;

import com.truholdem.config.api.ApiV1Config;
import com.truholdem.dto.ErrorResponse;
import com.truholdem.model.HandHistory;
import com.truholdem.service.HandHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@ApiV1Config
@RequestMapping("/history")
@Tag(name = "Hand History", description = "Hand history retrieval, replay data, and history management")
@SecurityRequirement(name = "bearerAuth")
public class HandHistoryController {

    private final HandHistoryService handHistoryService;

    public HandHistoryController(HandHistoryService handHistoryService) {
        this.handHistoryService = handHistoryService;
    }

    @GetMapping("/{historyId}")
    @Operation(
        summary = "Get a specific hand history",
        description = "Retrieve detailed history for a specific hand by its UUID"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Hand history retrieved successfully",
            content = @Content(schema = @Schema(implementation = HandHistory.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Hand history not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<HandHistory> getHandHistory(
            @Parameter(description = "UUID of the hand history") @PathVariable UUID historyId) {
        return handHistoryService.getHandHistory(historyId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/game/{gameId}")
    @Operation(
        summary = "Get all hands for a game",
        description = "Retrieve all hand histories for a specific game"
    )
    @ApiResponse(responseCode = "200", description = "Hand histories retrieved successfully")
    public ResponseEntity<List<HandHistory>> getGameHistory(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId) {
        List<HandHistory> history = handHistoryService.getGameHistory(gameId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/game/{gameId}/paged")
    @Operation(
        summary = "Get hands for a game with pagination",
        description = "Retrieve paginated hand histories for a specific game"
    )
    @ApiResponse(responseCode = "200", description = "Paginated hand histories retrieved successfully")
    public ResponseEntity<Page<HandHistory>> getGameHistoryPaged(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Page<HandHistory> history = handHistoryService.getGameHistory(gameId, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/player/{playerId}")
    @Operation(
        summary = "Get hands where a player participated",
        description = "Retrieve all hand histories where the specified player was involved"
    )
    @ApiResponse(responseCode = "200", description = "Hand histories retrieved successfully")
    public ResponseEntity<List<HandHistory>> getPlayerHistory(
            @Parameter(description = "UUID of the player") @PathVariable UUID playerId) {
        List<HandHistory> history = handHistoryService.getPlayerHistory(playerId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/wins/{playerName}")
    @Operation(
        summary = "Get hands where a player won",
        description = "Retrieve all hand histories where the specified player won the pot"
    )
    @ApiResponse(responseCode = "200", description = "Winning hand histories retrieved successfully")
    public ResponseEntity<List<HandHistory>> getPlayerWins(
            @Parameter(description = "Player username") @PathVariable String playerName) {
        List<HandHistory> history = handHistoryService.getPlayerWins(playerName);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/recent")
    @Operation(
        summary = "Get recent hands",
        description = "Retrieve the most recently played hands across all games"
    )
    @ApiResponse(responseCode = "200", description = "Recent hand histories retrieved successfully")
    public ResponseEntity<List<HandHistory>> getRecentHands() {
        List<HandHistory> history = handHistoryService.getRecentHands();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/biggest-pots")
    @Operation(
        summary = "Get hands with biggest pots",
        description = "Retrieve hand histories ranked by pot size"
    )
    @ApiResponse(responseCode = "200", description = "Biggest pot hand histories retrieved successfully")
    public ResponseEntity<List<HandHistory>> getBiggestPots() {
        List<HandHistory> history = handHistoryService.getBiggestPots();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{historyId}/replay")
    @Operation(
        summary = "Get replay data for a hand",
        description = """
            Generate replay data for hand analysis and visualization.
            
            **Includes:**
            - Action-by-action sequence
            - Board state at each street
            - Pot sizes throughout the hand
            - Player stack changes
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Replay data generated successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Hand history not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<HandHistoryService.ReplayData> getReplayData(
            @Parameter(description = "UUID of the hand history") @PathVariable UUID historyId) {
        HandHistoryService.ReplayData replayData = handHistoryService.generateReplayData(historyId);
        if (replayData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(replayData);
    }

    @GetMapping("/game/{gameId}/count")
    @Operation(
        summary = "Get hand count for a game",
        description = "Returns the total number of hands played in a game"
    )
    @ApiResponse(responseCode = "200", description = "Hand count retrieved successfully")
    public ResponseEntity<Long> getHandCount(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId) {
        long count = handHistoryService.getHandCount(gameId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/game/{gameId}")
    @Operation(
        summary = "Delete all history for a game",
        description = "Permanently delete all hand histories for a specific game. **This action cannot be undone.**"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "History deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Game not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Void> deleteGameHistory(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId) {
        handHistoryService.deleteGameHistory(gameId);
        return ResponseEntity.noContent().build();
    }
}
