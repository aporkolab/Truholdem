package com.truholdem.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.WebSocketClusterConfig;
import com.truholdem.model.GameUpdateType;
import com.truholdem.dto.PlayerActionMessageDto;
import com.truholdem.dto.ShowdownResult;
import com.truholdem.dto.WebSocketGameUpdateMessage;
import com.truholdem.model.Game;
import com.truholdem.model.Player;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Component
@ConditionalOnProperty(name = "app.websocket.cluster.enabled", havingValue = "true")
public class RedisGameEventBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(RedisGameEventBroadcaster.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;


    private final Counter eventsPublished;
    private final Counter eventsReceived;
    private final Counter eventsForwarded;
    private final Counter eventsDropped;


    private final ConcurrentHashMap<UUID, AtomicLong> gameSequences = new ConcurrentHashMap<>();


    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> processedEvents = new ConcurrentHashMap<>();
    private static final int DEDUP_WINDOW_SIZE = 1000;
    private static final long DEDUP_WINDOW_MS = 60_000;

    public RedisGameEventBroadcaster(
        @Qualifier("webSocketRedisTemplate") RedisTemplate<String, Object> redisTemplate,
        SimpMessagingTemplate messagingTemplate,
        ObjectMapper objectMapper,
        @Qualifier("clusterInstanceId") String instanceId,
        MeterRegistry meterRegistry) {

        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;


        this.eventsPublished = Counter.builder("websocket.cluster.events.published")
            .description("Number of events published to Redis")
            .tag("instance", instanceId)
            .register(meterRegistry);

        this.eventsReceived = Counter.builder("websocket.cluster.events.received")
            .description("Number of events received from Redis")
            .tag("instance", instanceId)
            .register(meterRegistry);

        this.eventsForwarded = Counter.builder("websocket.cluster.events.forwarded")
            .description("Number of events forwarded to local clients")
            .tag("instance", instanceId)
            .register(meterRegistry);

        this.eventsDropped = Counter.builder("websocket.cluster.events.dropped")
            .description("Number of events dropped (echo/duplicate)")
            .tag("instance", instanceId)
            .register(meterRegistry);

        logger.info("RedisGameEventBroadcaster initialized for instance: {}", instanceId);
    }


    public void broadcastGameUpdate(Game game) {
        if (game == null || game.getId() == null) return;

        WebSocketGameUpdateMessage message = new WebSocketGameUpdateMessage(
            GameUpdateType.GAME_STATE,
            game,
            null,
            "Game state updated"
        );

        publishEvent(GameEvent.builder()
            .gameId(game.getId())
            .type(GameUpdateType.GAME_STATE)
            .destination("/topic/game/" + game.getId())
            .payload(message)
            .build());
    }


    public void broadcastPlayerAction(Game game, Player player, String action, int amount) {
        if (game == null || game.getId() == null) return;

        PlayerActionMessageDto actionMessage = new PlayerActionMessageDto(
            player.getId(),
            player.getName(),
            action,
            amount,
            player.getChips(),
            player.getBetAmount()
        );

        WebSocketGameUpdateMessage message = new WebSocketGameUpdateMessage(
            GameUpdateType.PLAYER_ACTION,
            game,
            actionMessage,
            player.getName() + " performed " + action
        );

        publishEvent(GameEvent.builder()
            .gameId(game.getId())
            .type(GameUpdateType.PLAYER_ACTION)
            .destination("/topic/game/" + game.getId())
            .payload(message)
            .build());
    }


    public void broadcastPhaseChange(Game game) {
        if (game == null || game.getId() == null) return;

        WebSocketGameUpdateMessage message = new WebSocketGameUpdateMessage(
            GameUpdateType.PHASE_CHANGE,
            game,
            null,
            "Phase changed to " + game.getPhase()
        );

        publishEvent(GameEvent.builder()
            .gameId(game.getId())
            .type(GameUpdateType.PHASE_CHANGE)
            .destination("/topic/game/" + game.getId())
            .payload(message)
            .build());
    }


    public void broadcastShowdown(Game game, ShowdownResult result) {
        if (game == null || game.getId() == null) return;

        WebSocketGameUpdateMessage message = new WebSocketGameUpdateMessage(
            GameUpdateType.SHOWDOWN,
            game,
            result,
            result.getMessage()
        );

        publishEvent(GameEvent.builder()
            .gameId(game.getId())
            .type(GameUpdateType.SHOWDOWN)
            .destination("/topic/game/" + game.getId())
            .payload(message)
            .build());
    }


    public void broadcastGameEnded(Game game, String winnerName) {
        if (game == null || game.getId() == null) return;

        WebSocketGameUpdateMessage message = new WebSocketGameUpdateMessage(
            GameUpdateType.GAME_ENDED,
            game,
            Map.of("winner", winnerName),
            "Game ended. Winner: " + winnerName
        );

        publishEvent(GameEvent.builder()
            .gameId(game.getId())
            .type(GameUpdateType.GAME_ENDED)
            .destination("/topic/game/" + game.getId())
            .payload(message)
            .build());
    }


    private void publishEvent(GameEvent event) {

        event.setSourceInstanceId(instanceId);
        event.setSequenceNumber(getNextSequence(event.getGameId()));

        try {
            redisTemplate.convertAndSend(WebSocketClusterConfig.GAME_EVENTS_CHANNEL, event);
            eventsPublished.increment();
            logger.debug("Published event to Redis: {}", event);
        } catch (Exception e) {
            logger.error("Failed to publish event to Redis: {}", event, e);
        }


        forwardToLocalClients(event);
    }


    public void handleRedisMessage(Message message) {
        eventsReceived.increment();

        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            GameEvent event = objectMapper.readValue(body, GameEvent.class);


            if (instanceId.equals(event.getSourceInstanceId())) {
                eventsDropped.increment();
                logger.trace("Skipping own event: {}", event.getEventId());
                return;
            }


            if (isDuplicate(event)) {
                eventsDropped.increment();
                logger.trace("Skipping duplicate event: {}", event.getEventId());
                return;
            }


            forwardToLocalClients(event);

        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize Redis message", e);
        } catch (Exception e) {
            logger.error("Error handling Redis message", e);
        }
    }


    private void forwardToLocalClients(GameEvent event) {
        if (event.getDestination() == null || event.getPayload() == null) {
            logger.warn("Invalid event - missing destination or payload: {}", event);
            return;
        }

        try {
            messagingTemplate.convertAndSend(event.getDestination(), event.getPayload());
            eventsForwarded.increment();
            logger.debug("Forwarded event to local clients: {} -> {}",
                event.getEventId(), event.getDestination());
        } catch (Exception e) {
            logger.error("Failed to forward event to local clients: {}", event, e);
        }
    }


    private boolean isDuplicate(GameEvent event) {
        UUID gameId = event.getGameId();
        String eventId = event.getEventId();

        if (gameId == null || eventId == null) {
            return false;
        }

        ConcurrentHashMap<String, Long> gameEvents = processedEvents
            .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());


        cleanOldEntries(gameEvents);


        Long previousTimestamp = gameEvents.putIfAbsent(eventId, System.currentTimeMillis());
        return previousTimestamp != null;
    }


    private void cleanOldEntries(ConcurrentHashMap<String, Long> gameEvents) {
        if (gameEvents.size() < DEDUP_WINDOW_SIZE) {
            return;
        }

        long cutoff = System.currentTimeMillis() - DEDUP_WINDOW_MS;
        gameEvents.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }


    private long getNextSequence(UUID gameId) {
        return gameSequences
            .computeIfAbsent(gameId, k -> new AtomicLong(0))
            .incrementAndGet();
    }


    public void sendToUser(String username, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(username, destination, payload);
    }


    public void broadcastError(UUID gameId, String errorMessage) {
        if (gameId == null) return;

        Map<String, Object> errorDetails = Map.of(
            "type", "ERROR",
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        );

        GameEvent event = GameEvent.builder()
            .gameId(gameId)
            .type(null)
            .destination("/topic/game/" + gameId + "/errors")
            .payload(errorDetails)
            .build();

        publishEvent(event);
    }


    public String getInstanceId() {
        return instanceId;
    }
}
