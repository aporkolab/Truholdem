package com.truholdem.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truholdem.config.api.ApiV1Config;
import com.truholdem.dto.ErrorResponse;
import com.truholdem.dto.PlayerActionRequest;
import com.truholdem.model.Game;
import com.truholdem.model.PlayerInfo;
import com.truholdem.service.GameAuthorizationService;
import com.truholdem.service.PokerGameService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;


@RestController
@ApiV1Config
@RequestMapping("/poker/game")
@Tag(name = "Poker Game", description = "Core poker game operations - create games, execute actions, manage hands")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PokerGameController {

    private final PokerGameService pokerGameService;
    private final GameAuthorizationService authorizationService;

    public PokerGameController(PokerGameService pokerGameService, GameAuthorizationService authorizationService) {
        this.pokerGameService = pokerGameService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/start")
    @Operation(
        summary = "Start a new game",
        description = """
            Initializes a new Texas Hold'em poker game.
            
            **Player Configuration:**
            - Minimum 2 players, maximum 10
            - Mix of human and AI players supported
            - Each player starts with specified chip count
            
            **Initial State:**
            - Dealer position randomly assigned
            - Blinds posted automatically
            - Cards dealt to all players
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Game created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Game.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid player configuration",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<Game> startGame(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "List of players to join the game",
                required = true
            )
            @RequestBody @NotEmpty(message = "Players list cannot be empty") @Valid List<PlayerInfo> playersInfo) {
        Game newGame = pokerGameService.createNewGame(playersInfo);
        return ResponseEntity.status(HttpStatus.CREATED).body(newGame);
    }

    @GetMapping("/{gameId}")
    @Operation(
        summary = "Get game status",
        description = "Fetches the current state of a poker game including players, pot, community cards, and current action"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Game found",
            content = @Content(schema = @Schema(implementation = Game.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Game not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Game> getGameStatus(
            @Parameter(description = "UUID of the game", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID gameId) {
        Optional<Game> game = pokerGameService.getGame(gameId);
        return game.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{gameId}/player/{playerId}/action")
    @Operation(
        summary = "Execute player action",
        description = """
            Process a player's action in the game.
            
            **Valid Actions:**
            - FOLD: Surrender hand
            - CHECK: Pass action (when no bet to call)
            - CALL: Match current bet
            - RAISE: Increase bet (amount required)
            - ALL_IN: Bet all remaining chips
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Action executed successfully",
            content = @Content(schema = @Schema(implementation = Game.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid action or amount",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Game or player not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Not player's turn or invalid game state",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> playerAction(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId,
            @Parameter(description = "UUID of the acting player") @PathVariable UUID playerId,
            @RequestBody @Valid PlayerActionRequest request) {

        try {
            // Validate that the authenticated user can control this player
            authorizationService.validatePlayerAction(gameId, playerId);

            Game updated = pokerGameService.playerAct(
                    gameId,
                    playerId,
                    request.getAction(),
                    request.getAmount());
            return ResponseEntity.ok(updated);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", e.getMessage()));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", e.getMessage()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    @PostMapping("/{gameId}/bot/{botId}/action")
    @Operation(
        summary = "Execute bot action",
        description = """
            Triggers AI bot to make a decision and execute an action.
            
            **Bot AI Features:**
            - Monte Carlo simulation for hand strength
            - Position-aware play
            - Opponent modeling
            - GTO-influenced decisions
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bot action executed",
            content = @Content(schema = @Schema(implementation = Game.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Player is not a bot or invalid game state",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Game or bot not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> executeBotAction(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId,
            @Parameter(description = "UUID of the bot player") @PathVariable UUID botId) {
        try {
            // Validate that the authenticated user can trigger this bot action
            authorizationService.validateBotAction(gameId, botId);

            Game updatedGame = pokerGameService.executeBotAction(gameId, botId);
            return ResponseEntity.ok(updatedGame);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", e.getMessage()));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    @PostMapping("/{gameId}/new-hand")
    @Operation(
        summary = "Start new hand",
        description = """
            Starts a new hand in an existing game.
            
            **Prerequisites:**
            - Previous hand must be complete
            - At least 2 players with chips remaining
            
            **Actions:**
            - Rotates dealer button
            - Posts blinds
            - Deals new cards
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "New hand started",
            content = @Content(schema = @Schema(implementation = Game.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Not enough players to continue",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Game not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> startNewHand(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId) {
        try {
            // Validate that the authenticated user can start a new hand
            authorizationService.validateNewHandAction(gameId);

            Game game = pokerGameService.startNewHand(gameId);
            return ResponseEntity.ok(game);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", e.getMessage()));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }
}