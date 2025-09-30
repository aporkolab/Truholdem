package com.truholdem.controller;

import com.truholdem.model.Game;
import com.truholdem.model.Player;
import com.truholdem.service.PokerGameService;
import com.truholdem.websocket.ClusterSessionRegistry;
import com.truholdem.websocket.GameEvent;
import com.truholdem.websocket.GameEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.*;


@RestController
@ConditionalOnProperty(name = "app.websocket.cluster.enabled", havingValue = "true")
public class ReconnectionController {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectionController.class);

    private final PokerGameService gameService;
    private final ClusterSessionRegistry sessionRegistry;
    private final GameEventStore eventStore;
    private final SimpMessagingTemplate messagingTemplate;

    public ReconnectionController(
            PokerGameService gameService,
            ClusterSessionRegistry sessionRegistry,
            GameEventStore eventStore,
            SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.sessionRegistry = sessionRegistry;
        this.eventStore = eventStore;
        this.messagingTemplate = messagingTemplate;
    }


    @MessageMapping("/reconnect")
    public void handleReconnect(
            @Payload ReconnectRequest request,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        String sessionId = headerAccessor.getSessionId();
        String username = principal != null ? principal.getName() : null;

        logger.info("Reconnection request from session {} for game {} (lastSequence: {})",
                sessionId, request.gameId(), request.lastEventSequence());

        try {

            if (request.gameId() == null) {
                messagingTemplate.convertAndSendToUser(
                        sessionId, "/queue/state",
                        GameStateSnapshot.error("Missing game ID"),
                        createHeaders(sessionId));
                return;
            }


            Optional<Game> gameOpt = gameService.getGame(request.gameId());
            if (gameOpt.isEmpty()) {
                messagingTemplate.convertAndSendToUser(
                        sessionId, "/queue/state",
                        GameStateSnapshot.error("Game not found: " + request.gameId()),
                        createHeaders(sessionId));
                return;
            }

            Game game = gameOpt.get();


            Player currentPlayer = null;
            if (username != null) {
                currentPlayer = game.getPlayers().stream()
                        .filter(p -> username.equals(p.getName()))
                        .findFirst()
                        .orElse(null);
            }


            sessionRegistry.subscribeToGame(sessionId, request.gameId());


            List<GameEvent> missedEvents = Collections.emptyList();
            if (request.lastEventSequence() > 0) {
                missedEvents = eventStore.getEventsSince(
                        request.gameId(),
                        request.lastEventSequence()
                );
            }


            GameStateSnapshot snapshot = GameStateSnapshot.builder()
                    .game(game)
                    .currentPlayerId(currentPlayer != null ? currentPlayer.getId() : null)
                    .currentPlayerName(currentPlayer != null ? currentPlayer.getName() : null)
                    .missedEvents(missedEvents)
                    .lastEventSequence(eventStore.getLatestSequence(request.gameId()))
                    .serverTime(Instant.now())
                    .sessionId(sessionId)
                    .build();

            logger.info("Reconnection successful for session {} - {} missed events",
                    sessionId, missedEvents.size());

            messagingTemplate.convertAndSendToUser(
                    sessionId, "/queue/state", snapshot,
                    createHeaders(sessionId));

        } catch (Exception e) {
            logger.error("Reconnection failed for session {}", sessionId, e);
            messagingTemplate.convertAndSendToUser(
                    sessionId, "/queue/state",
                    GameStateSnapshot.error("Reconnection failed: " + e.getMessage()),
                    createHeaders(sessionId));
        }
    }

    private Map<String, Object> createHeaders(String sessionId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", sessionId);
        return headers;
    }


    public record ReconnectRequest(
            UUID gameId,
            long lastEventSequence,
            String lastKnownPhase,
            Instant disconnectedAt
    ) {}


    public record GameStateSnapshot(
            boolean success,
            String error,
            Game game,
            UUID currentPlayerId,
            String currentPlayerName,
            List<GameEvent> missedEvents,
            long lastEventSequence,
            Instant serverTime,
            String sessionId
    ) {
        public static GameStateSnapshot error(String message) {
            return new GameStateSnapshot(false, message, null, null, null,
                    Collections.emptyList(), 0, Instant.now(), null);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Game game;
            private UUID currentPlayerId;
            private String currentPlayerName;
            private List<GameEvent> missedEvents = Collections.emptyList();
            private long lastEventSequence;
            private Instant serverTime;
            private String sessionId;

            public Builder game(Game game) {
                this.game = game;
                return this;
            }

            public Builder currentPlayerId(UUID currentPlayerId) {
                this.currentPlayerId = currentPlayerId;
                return this;
            }

            public Builder currentPlayerName(String currentPlayerName) {
                this.currentPlayerName = currentPlayerName;
                return this;
            }

            public Builder missedEvents(List<GameEvent> missedEvents) {
                this.missedEvents = missedEvents;
                return this;
            }

            public Builder lastEventSequence(long lastEventSequence) {
                this.lastEventSequence = lastEventSequence;
                return this;
            }

            public Builder serverTime(Instant serverTime) {
                this.serverTime = serverTime;
                return this;
            }

            public Builder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }

            public GameStateSnapshot build() {
                return new GameStateSnapshot(
                        true, null, game, currentPlayerId, currentPlayerName,
                        missedEvents, lastEventSequence, serverTime, sessionId
                );
            }
        }
    }
}
