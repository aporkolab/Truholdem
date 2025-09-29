package com.truholdem.controller;

import com.truholdem.config.api.ApiV1Config;
import com.truholdem.dto.*;
import com.truholdem.model.*;
import com.truholdem.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;


@RestController
@ApiV1Config
@RequestMapping("/tournaments")
@Tag(name = "Tournaments", description = "Tournament management - create, register, control MTTs")
@SecurityRequirement(name = "bearerAuth")
public class TournamentController {

    private static final Logger log = LoggerFactory.getLogger(TournamentController.class);

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    

    @PostMapping
    @Operation(summary = "Create a new tournament", description = "Creates a new poker tournament with the specified configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tournament created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid tournament configuration")
    })
    public ResponseEntity<TournamentDetailResponse> createTournament(
            @Valid @RequestBody CreateTournamentRequest request) {
        
        log.info("Creating tournament: {} (type: {})", request.name(), request.type());
        
        Tournament tournament = tournamentService.createTournament(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(TournamentDetailResponse.from(tournament));
    }

    

    @GetMapping
    @Operation(summary = "List tournaments", description = "Returns a list of tournaments, optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of tournaments")
    })
    public ResponseEntity<List<TournamentSummaryResponse>> listTournaments(
            @Parameter(description = "Filter by status: OPEN, RUNNING, PAUSED, COMPLETED, or ALL")
            @RequestParam(required = false, defaultValue = "all") String status) {
        
        log.debug("Listing tournaments with status filter: {}", status);
        
        List<Tournament> tournaments = tournamentService.getTournamentsByStatus(status);
        
        List<TournamentSummaryResponse> response = tournaments.stream()
            .map(TournamentSummaryResponse::from)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament details", description = "Returns detailed information about a specific tournament")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tournament details"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<TournamentDetailResponse> getTournament(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id) {
        
        log.debug("Fetching tournament: {}", id);
        
        Tournament tournament = tournamentService.getTournament(id);
        
        return ResponseEntity.ok(TournamentDetailResponse.from(tournament));
    }

    

    @PostMapping("/{id}/register")
    @Operation(summary = "Register for tournament", description = "Registers a player for the tournament")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Registration not allowed"),
        @ApiResponse(responseCode = "404", description = "Tournament not found"),
        @ApiResponse(responseCode = "409", description = "Player already registered")
    })
    public ResponseEntity<TournamentDetailResponse> registerForTournament(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id,
            @Valid @RequestBody RegisterForTournamentRequest request) {
        
        log.info("Registering player {} ({}) for tournament {}", 
                 request.playerName(), request.playerId(), id);
        
        tournamentService.registerPlayer(id, request.playerId(), request.playerName());
        
        Tournament tournament = tournamentService.getTournament(id);
        
        return ResponseEntity.ok(TournamentDetailResponse.from(tournament));
    }

    @DeleteMapping("/{id}/register/{playerId}")
    @Operation(summary = "Unregister from tournament", description = "Removes a player's registration from the tournament")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Unregistration successful"),
        @ApiResponse(responseCode = "400", description = "Cannot unregister"),
        @ApiResponse(responseCode = "404", description = "Tournament or player not found")
    })
    public ResponseEntity<Void> unregisterFromTournament(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id,
            @Parameter(description = "Player ID")
            @PathVariable UUID playerId) {
        
        log.info("Unregistering player {} from tournament {}", playerId, id);
        
        tournamentService.unregisterPlayer(id, playerId);
        
        return ResponseEntity.noContent().build();
    }

    

    @PostMapping("/{id}/start")
    @Operation(summary = "Start tournament", description = "Starts the tournament (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tournament started"),
        @ApiResponse(responseCode = "400", description = "Cannot start tournament"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<TournamentDetailResponse> startTournament(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id) {
        
        log.info("Starting tournament: {}", id);
        
        tournamentService.startTournament(id);
        
        Tournament tournament = tournamentService.getTournament(id);
        
        return ResponseEntity.ok(TournamentDetailResponse.from(tournament));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause tournament", description = "Pauses a running tournament (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tournament paused"),
        @ApiResponse(responseCode = "400", description = "Cannot pause tournament"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<TournamentDetailResponse> pauseTournament(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id) {
        
        log.info("Pausing tournament: {}", id);
        
        tournamentService.pauseTournament(id);
        
        Tournament tournament = tournamentService.getTournament(id);
        
        return ResponseEntity.ok(TournamentDetailResponse.from(tournament));
    }

    

    @GetMapping("/{id}/tables")
    @Operation(summary = "Get tournament tables", description = "Returns all active tables in the tournament")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of tables"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<List<TournamentDetailResponse.TableSummary>> getTournamentTables(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id) {
        
        log.debug("Fetching tables for tournament: {}", id);
        
        List<TournamentTable> tables = tournamentService.getTournamentTables(id);
        
        List<TournamentDetailResponse.TableSummary> response = tables.stream()
            .map(TournamentDetailResponse.TableSummary::from)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/tables/{tableId}")
    @Operation(summary = "Get specific table", description = "Returns details of a specific table in the tournament")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Table details"),
        @ApiResponse(responseCode = "404", description = "Tournament or table not found")
    })
    public ResponseEntity<TableDetailResponse> getTournamentTable(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id,
            @Parameter(description = "Table ID")
            @PathVariable UUID tableId) {
        
        log.debug("Fetching table {} for tournament {}", tableId, id);
        
        TournamentTable table = tournamentService.getTournamentTable(id, tableId);
        
        return ResponseEntity.ok(TableDetailResponse.from(table));
    }

    

    @GetMapping("/{id}/leaderboard")
    @Operation(summary = "Get tournament leaderboard", description = "Returns player standings ordered by chip count")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leaderboard entries"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id) {
        
        log.debug("Fetching leaderboard for tournament: {}", id);
        
        List<TournamentRegistration> registrations = tournamentService.getLeaderboard(id);
        
        List<LeaderboardEntryDto> leaderboard = IntStream.range(0, registrations.size())
            .mapToObj(i -> LeaderboardEntryDto.from(registrations.get(i), i + 1))
            .toList();
        
        return ResponseEntity.ok(leaderboard);
    }

    

    @GetMapping("/{id}/blinds")
    @Operation(summary = "Get blind levels", description = "Returns current and next blind levels with time remaining")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Blind level information"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<BlindInfoResponse> getBlindInfo(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id) {
        
        log.debug("Fetching blind info for tournament: {}", id);
        
        Tournament tournament = tournamentService.getTournament(id);
        
        return ResponseEntity.ok(BlindInfoResponse.from(tournament));
    }

    

    @PostMapping("/{id}/rebuy")
    @Operation(summary = "Request rebuy", description = "Process a rebuy request for a player in a rebuy tournament")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rebuy successful"),
        @ApiResponse(responseCode = "400", description = "Rebuy not allowed"),
        @ApiResponse(responseCode = "404", description = "Tournament or player not found")
    })
    public ResponseEntity<RebuyResponse> requestRebuy(
            @Parameter(description = "Tournament ID")
            @PathVariable UUID id,
            @Valid @RequestBody RebuyRequest request) {
        
        log.info("Processing rebuy for player {} in tournament {}", request.playerId(), id);
        
        TournamentRegistration registration = tournamentService.processRebuy(id, request.playerId());
        
        return ResponseEntity.ok(RebuyResponse.from(registration));
    }

    

    
    public record TableDetailResponse(
        UUID id,
        int tableNumber,
        List<UUID> playerIds,
        int playerCount,
        boolean isFinalTable,
        boolean isActive
    ) {
        public static TableDetailResponse from(TournamentTable table) {
            return new TableDetailResponse(
                table.getId(),
                table.getTableNumber(),
                table.getPlayerIds(),
                table.getPlayerCount(),
                table.isFinalTable(),
                table.isActive()
            );
        }
    }

    
    public record BlindInfoResponse(
        int currentLevel,
        TournamentDetailResponse.BlindLevelInfo currentBlinds,
        TournamentDetailResponse.BlindLevelInfo nextBlinds,
        long secondsToNextLevel,
        int levelDurationMinutes
    ) {
        public static BlindInfoResponse from(Tournament tournament) {
            BlindLevel current = tournament.getCurrentBlindLevel();
            BlindLevel next = tournament.getBlindStructure().getLevelAt(tournament.getCurrentLevel() + 1);
            
            long secondsToNext = 0;
            if (tournament.getLevelStartTime() != null && tournament.getStatus().isPlayable()) {
                java.time.Duration elapsed = java.time.Duration.between(
                    tournament.getLevelStartTime(), java.time.Instant.now());
                long levelDuration = tournament.getBlindStructure().getLevelDurationMinutes() * 60L;
                secondsToNext = Math.max(0, levelDuration - elapsed.toSeconds());
            }
            
            return new BlindInfoResponse(
                tournament.getCurrentLevel(),
                TournamentDetailResponse.BlindLevelInfo.from(current, tournament.getCurrentLevel()),
                TournamentDetailResponse.BlindLevelInfo.from(next, tournament.getCurrentLevel() + 1),
                secondsToNext,
                tournament.getBlindStructure().getLevelDurationMinutes()
            );
        }
    }

    
    public record RebuyResponse(
        UUID playerId,
        String playerName,
        int newChipCount,
        int rebuysUsed,
        int rebuysRemaining,
        boolean canRebuyAgain
    ) {
        public static RebuyResponse from(TournamentRegistration reg) {
            int maxRebuys = reg.getTournament().getMaxRebuys();
            return new RebuyResponse(
                reg.getPlayerId(),
                reg.getPlayerName(),
                reg.getCurrentChips(),
                reg.getRebuysUsed(),
                maxRebuys - reg.getRebuysUsed(),
                reg.canRebuy()
            );
        }
    }
}
