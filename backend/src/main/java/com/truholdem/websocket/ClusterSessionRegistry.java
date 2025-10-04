package com.truholdem.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.WebSocketClusterConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component
@ConditionalOnProperty(name = "app.websocket.cluster.enabled", havingValue = "true")
public class ClusterSessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ClusterSessionRegistry.class);

    private static final String SESSION_PREFIX = "ws:sessions:";
    private static final String PLAYER_PREFIX = "ws:players:";
    private static final String GAME_PREFIX = "ws:games:";
    private static final String INSTANCE_PREFIX = "ws:instances:";

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    
    private final ConcurrentHashMap<String, SessionInfo> localSessions = new ConcurrentHashMap<>();

    public ClusterSessionRegistry(
            @Qualifier("webSocketRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("clusterInstanceId") String instanceId,
            MeterRegistry meterRegistry) {
        
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;

        
        Gauge.builder("websocket.sessions.local", localSessions, Map::size)
                .description("Number of local WebSocket sessions")
                .tag("instance", instanceId)
                .register(meterRegistry);

        logger.info("ClusterSessionRegistry initialized for instance: {}", instanceId);
    }

    
    public void registerSession(String sessionId, String playerId) {
        registerSession(sessionId, playerId, null);
    }

    
    public void registerSession(String sessionId, String playerId, UUID gameId) {
        SessionInfo sessionInfo = new SessionInfo(
                sessionId,
                playerId,
                gameId,
                instanceId,
                Instant.now()
        );

        
        localSessions.put(sessionId, sessionInfo);

        
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().putAll(sessionKey, sessionInfo.toMap());
        redisTemplate.expire(sessionKey, SESSION_TTL);

        
        if (playerId != null) {
            redisTemplate.opsForSet().add(PLAYER_PREFIX + playerId, sessionId);
            redisTemplate.expire(PLAYER_PREFIX + playerId, SESSION_TTL);
        }

        
        if (gameId != null) {
            redisTemplate.opsForSet().add(GAME_PREFIX + gameId, sessionId);
            redisTemplate.expire(GAME_PREFIX + gameId, SESSION_TTL);
        }

        
        redisTemplate.opsForSet().add(INSTANCE_PREFIX + instanceId, sessionId);
        redisTemplate.expire(INSTANCE_PREFIX + instanceId, SESSION_TTL);

        
        broadcastSessionEvent(SessionEvent.connected(sessionId, playerId, gameId, instanceId));

        logger.info("Registered session: {} for player: {} in game: {}", sessionId, playerId, gameId);
    }

    
    public void subscribeToGame(String sessionId, UUID gameId) {
        SessionInfo sessionInfo = localSessions.get(sessionId);
        if (sessionInfo == null) {
            logger.warn("Cannot subscribe to game - session not found: {}", sessionId);
            return;
        }

        UUID oldGameId = sessionInfo.gameId();

        
        if (oldGameId != null) {
            redisTemplate.opsForSet().remove(GAME_PREFIX + oldGameId, sessionId);
        }

        
        SessionInfo updatedInfo = new SessionInfo(
                sessionInfo.sessionId(),
                sessionInfo.playerId(),
                gameId,
                sessionInfo.instanceId(),
                sessionInfo.connectedAt()
        );

        localSessions.put(sessionId, updatedInfo);

        
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().put(sessionKey, "gameId", gameId != null ? gameId.toString() : "");
        redisTemplate.expire(sessionKey, SESSION_TTL);

        
        if (gameId != null) {
            redisTemplate.opsForSet().add(GAME_PREFIX + gameId, sessionId);
            redisTemplate.expire(GAME_PREFIX + gameId, SESSION_TTL);
        }

        logger.debug("Session {} subscribed to game: {}", sessionId, gameId);
    }

    
    public void unregisterSession(String sessionId) {
        SessionInfo sessionInfo = localSessions.remove(sessionId);
        
        if (sessionInfo == null) {
            
            Map<Object, Object> map = redisTemplate.opsForHash().entries(SESSION_PREFIX + sessionId);
            if (!map.isEmpty()) {
                sessionInfo = SessionInfo.fromMap(map);
            }
        }

        
        redisTemplate.delete(SESSION_PREFIX + sessionId);

        if (sessionInfo != null) {
            
            if (sessionInfo.playerId() != null) {
                redisTemplate.opsForSet().remove(PLAYER_PREFIX + sessionInfo.playerId(), sessionId);
            }

            
            if (sessionInfo.gameId() != null) {
                redisTemplate.opsForSet().remove(GAME_PREFIX + sessionInfo.gameId(), sessionId);
            }

            
            redisTemplate.opsForSet().remove(INSTANCE_PREFIX + instanceId, sessionId);

            
            broadcastSessionEvent(SessionEvent.disconnected(
                    sessionId, sessionInfo.playerId(), sessionInfo.gameId(), instanceId));

            logger.info("Unregistered session: {}", sessionId);
        }
    }

    
    public Optional<String> getPlayerIdForSession(String sessionId) {
        
        SessionInfo localInfo = localSessions.get(sessionId);
        if (localInfo != null) {
            return Optional.ofNullable(localInfo.playerId());
        }

        
        Object playerId = redisTemplate.opsForHash().get(SESSION_PREFIX + sessionId, "playerId");
        return Optional.ofNullable(playerId).map(Object::toString).filter(s -> !s.isEmpty());
    }

    
    public Optional<UUID> getGameIdForSession(String sessionId) {
        
        SessionInfo localInfo = localSessions.get(sessionId);
        if (localInfo != null) {
            return Optional.ofNullable(localInfo.gameId());
        }

        
        Object gameId = redisTemplate.opsForHash().get(SESSION_PREFIX + sessionId, "gameId");
        return Optional.ofNullable(gameId)
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString);
    }

    
    public Set<String> getSessionsForPlayer(String playerId) {
        Set<Object> sessions = redisTemplate.opsForSet().members(PLAYER_PREFIX + playerId);
        if (sessions == null) return Collections.emptySet();
        
        Set<String> result = new HashSet<>();
        for (Object session : sessions) {
            result.add(session.toString());
        }
        return result;
    }

    
    public Set<String> getSessionsForGame(UUID gameId) {
        Set<Object> sessions = redisTemplate.opsForSet().members(GAME_PREFIX + gameId);
        if (sessions == null) return Collections.emptySet();
        
        Set<String> result = new HashSet<>();
        for (Object session : sessions) {
            result.add(session.toString());
        }
        return result;
    }

    
    public long getSessionCountForGame(UUID gameId) {
        Long count = redisTemplate.opsForSet().size(GAME_PREFIX + gameId);
        return count != null ? count : 0;
    }

    
    public boolean isPlayerConnected(String playerId) {
        Long count = redisTemplate.opsForSet().size(PLAYER_PREFIX + playerId);
        return count != null && count > 0;
    }

    
    public Collection<SessionInfo> getLocalSessions() {
        return Collections.unmodifiableCollection(localSessions.values());
    }

    
    public void handleSessionEvent(Message message) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            SessionEvent event = objectMapper.readValue(body, SessionEvent.class);

            
            if (instanceId.equals(event.instanceId())) {
                return;
            }

            logger.debug("Received session event from instance {}: {} for session {}", 
                    event.instanceId(), event.type(), event.sessionId());

            
            

        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize session event", e);
        }
    }

    
    private void broadcastSessionEvent(SessionEvent event) {
        try {
            redisTemplate.convertAndSend(WebSocketClusterConfig.SESSION_EVENTS_CHANNEL, event);
        } catch (Exception e) {
            logger.error("Failed to broadcast session event", e);
        }
    }

    
    public void sendHeartbeat() {
        for (String sessionId : localSessions.keySet()) {
            redisTemplate.expire(SESSION_PREFIX + sessionId, SESSION_TTL);
        }
        redisTemplate.expire(INSTANCE_PREFIX + instanceId, SESSION_TTL);
    }

    
    public record SessionInfo(
            String sessionId,
            String playerId,
            UUID gameId,
            String instanceId,
            Instant connectedAt
    ) implements Serializable {

        public Map<String, String> toMap() {
            Map<String, String> map = new HashMap<>();
            map.put("sessionId", sessionId);
            map.put("playerId", playerId != null ? playerId : "");
            map.put("gameId", gameId != null ? gameId.toString() : "");
            map.put("instanceId", instanceId);
            map.put("connectedAt", connectedAt.toString());
            return map;
        }

        public static SessionInfo fromMap(Map<Object, Object> map) {
            String sessionId = (String) map.get("sessionId");
            String playerId = (String) map.get("playerId");
            String gameIdStr = (String) map.get("gameId");
            String instanceId = (String) map.get("instanceId");
            String connectedAtStr = (String) map.get("connectedAt");

            return new SessionInfo(
                    sessionId,
                    playerId != null && !playerId.isEmpty() ? playerId : null,
                    gameIdStr != null && !gameIdStr.isEmpty() ? UUID.fromString(gameIdStr) : null,
                    instanceId,
                    connectedAtStr != null ? Instant.parse(connectedAtStr) : Instant.now()
            );
        }
    }

    
    public record SessionEvent(
            String type,
            String sessionId,
            String playerId,
            UUID gameId,
            String instanceId,
            Instant timestamp
    ) implements Serializable {

        public static SessionEvent connected(String sessionId, String playerId, UUID gameId, String instanceId) {
            return new SessionEvent("CONNECTED", sessionId, playerId, gameId, instanceId, Instant.now());
        }

        public static SessionEvent disconnected(String sessionId, String playerId, UUID gameId, String instanceId) {
            return new SessionEvent("DISCONNECTED", sessionId, playerId, gameId, instanceId, Instant.now());
        }
    }
}
