package com.truholdem.service;

import com.truholdem.model.Game;
import com.truholdem.model.Player;
import com.truholdem.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for authorizing game-related operations.
 *
 * Validates that authenticated users can only:
 * - Control their own human players
 * - Trigger bot actions only for bots in games they participate in
 * - View games they are part of
 *
 * Authorization can be disabled for testing via the app.security.authorization.enabled property.
 */
@Service
public class GameAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(GameAuthorizationService.class);

    private final PokerGameService pokerGameService;
    private final boolean authorizationEnabled;

    public GameAuthorizationService(
            PokerGameService pokerGameService,
            @Value("${app.security.authorization.enabled:true}") boolean authorizationEnabled) {
        this.pokerGameService = pokerGameService;
        this.authorizationEnabled = authorizationEnabled;
        if (!authorizationEnabled) {
            logger.warn("Game authorization is DISABLED - this should only be used in test environments");
        }
    }

    /**
     * Validates that the authenticated user can perform actions for the specified player.
     *
     * @param gameId   The game UUID
     * @param playerId The player UUID
     * @throws AccessDeniedException if the user is not authorized
     */
    public void validatePlayerAction(UUID gameId, UUID playerId) {
        if (!authorizationEnabled) {
            logger.debug("Authorization disabled - skipping player action validation");
            return;
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Game game = pokerGameService.getGame(gameId)
                .orElseThrow(() -> new AccessDeniedException("Game not found"));

        Player player = game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Player not found in game"));

        // Bot players can be controlled by anyone in the game (for triggering AI actions)
        if (player.isBot()) {
            validateUserIsInGame(currentUser, game);
            return;
        }

        // Human players can only be controlled by their owner
        if (!player.isOwnedBy(currentUser.getId())) {
            logger.warn("User {} attempted to control player {} owned by {}",
                    currentUser.getUsername(), player.getName(), player.getUserId());
            throw new AccessDeniedException("You can only control your own player");
        }
    }

    /**
     * Validates that the authenticated user can trigger a bot action.
     *
     * @param gameId The game UUID
     * @param botId  The bot player UUID
     * @throws AccessDeniedException if the user is not authorized
     */
    public void validateBotAction(UUID gameId, UUID botId) {
        if (!authorizationEnabled) {
            logger.debug("Authorization disabled - skipping bot action validation");
            return;
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Game game = pokerGameService.getGame(gameId)
                .orElseThrow(() -> new AccessDeniedException("Game not found"));

        Player bot = game.getPlayers().stream()
                .filter(p -> p.getId().equals(botId))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Bot not found in game"));

        if (!bot.isBot()) {
            throw new AccessDeniedException("Player is not a bot");
        }

        // Any authenticated user in the game can trigger bot actions
        validateUserIsInGame(currentUser, game);
    }

    /**
     * Validates that the authenticated user can start a new hand.
     *
     * @param gameId The game UUID
     * @throws AccessDeniedException if the user is not authorized
     */
    public void validateNewHandAction(UUID gameId) {
        if (!authorizationEnabled) {
            logger.debug("Authorization disabled - skipping new hand validation");
            return;
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Game game = pokerGameService.getGame(gameId)
                .orElseThrow(() -> new AccessDeniedException("Game not found"));

        validateUserIsInGame(currentUser, game);
    }

    /**
     * Validates that the authenticated user can view a game.
     *
     * @param gameId The game UUID
     * @throws AccessDeniedException if the user is not authorized
     */
    public void validateGameView(UUID gameId) {
        if (!authorizationEnabled) {
            logger.debug("Authorization disabled - skipping game view validation");
            return;
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Game game = pokerGameService.getGame(gameId)
                .orElseThrow(() -> new AccessDeniedException("Game not found"));

        // For now, allow any authenticated user to view any game
        // In a production system, you might restrict this to participants only
        logger.debug("User {} viewing game {}", currentUser.getUsername(), gameId);
    }

    /**
     * Gets the current authenticated user.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        return null;
    }

    /**
     * Validates that the user is a participant in the game.
     */
    private void validateUserIsInGame(User user, Game game) {
        boolean isInGame = game.getPlayers().stream()
                .anyMatch(p -> !p.isBot() && p.isOwnedBy(user.getId()));

        if (!isInGame) {
            logger.warn("User {} is not a participant in game {}", user.getUsername(), game.getId());
            throw new AccessDeniedException("You are not a participant in this game");
        }
    }
}
