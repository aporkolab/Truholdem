package com.truholdem.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truholdem.dto.PlayerActionRequest;
import com.truholdem.model.Game;
import com.truholdem.model.PlayerInfo;
import com.truholdem.service.PokerGameService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@RestController
@RequestMapping("/v1/poker/game")
@Tag(name = "Poker Game API v1", description = "Operations for managing poker games")
@Validated
public class PokerGameController {

    private final PokerGameService pokerGameService;

    public PokerGameController(PokerGameService pokerGameService) {
        this.pokerGameService = pokerGameService;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new game", description = "Initializes a new poker game with the provided players")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game created successfully", content = @Content(schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "400", description = "Invalid player configuration")
    })
    public ResponseEntity<Game> startGame(
            @RequestBody @NotEmpty(message = "Players list cannot be empty") @Valid List<PlayerInfo> playersInfo) {
        Game newGame = pokerGameService.createNewGame(playersInfo);
        return ResponseEntity.ok(newGame);
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "Get game status", description = "Fetches the current state of a poker game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game found"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    public ResponseEntity<Game> getGameStatus(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId) {
        Optional<Game> game = pokerGameService.getGame(gameId);
        return game.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{gameId}/player/{playerId}/action")
    public ResponseEntity<?> playerAction(
            @PathVariable UUID gameId,
            @PathVariable UUID playerId,
            @RequestBody @Valid PlayerActionRequest request) {

        try {
            Game updated = pokerGameService.playerAct(
                    gameId,
                    playerId,
                    request.getAction(),
                    request.getAmount());
            return ResponseEntity.ok(updated);

        } catch (NoSuchElementException e) {
            
            return ResponseEntity.status(404).body(e.getMessage());

        } catch (IllegalStateException e) {
            
            return ResponseEntity.status(409).body(e.getMessage());

        } catch (IllegalArgumentException e) {
            
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{gameId}/bot/{botId}/action")
    @Operation(summary = "Execute bot action", description = "AI bot makes a decision and executes an action")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bot action executed"),
            @ApiResponse(responseCode = "400", description = "Player is not a bot"),
            @ApiResponse(responseCode = "404", description = "Game or bot not found")
    })
    public ResponseEntity<Game> executeBotAction(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId,
            @Parameter(description = "UUID of the bot player") @PathVariable UUID botId) {
        try {
            Game updatedGame = pokerGameService.executeBotAction(gameId, botId);
            return ResponseEntity.ok(updatedGame);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/new-hand")
    @Operation(summary = "Start new hand", description = "Starts a new hand in an existing game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New hand started"),
            @ApiResponse(responseCode = "400", description = "Not enough players to continue"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    public ResponseEntity<Game> startNewHand(
            @Parameter(description = "UUID of the game") @PathVariable UUID gameId) {
        Game game = pokerGameService.startNewHand(gameId);
        return ResponseEntity.ok(game);
    }
}