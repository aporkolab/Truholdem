package com.truholdem.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClusterSessionRegistryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private ClusterSessionRegistry registry;

    private static final String INSTANCE_ID = "test-instance-1";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        meterRegistry = new SimpleMeterRegistry();

        registry = new ClusterSessionRegistry(
                redisTemplate,
                objectMapper,
                INSTANCE_ID,
                meterRegistry
        );
    }

    @Nested
    @DisplayName("Session Registration Tests")
    class SessionRegistrationTests {

        @Test
        @DisplayName("Should register session with player ID")
        void shouldRegisterSession() {
            
            String sessionId = "session-123";
            String playerId = "player-456";

            
            registry.registerSession(sessionId, playerId);

            
            verify(hashOperations).putAll(
                    eq("ws:sessions:" + sessionId),
                    anyMap()
            );
            verify(setOperations).add(
                    eq("ws:players:" + playerId),
                    eq(sessionId)
            );
            verify(setOperations).add(
                    eq("ws:instances:" + INSTANCE_ID),
                    eq(sessionId)
            );
            verify(redisTemplate, atLeastOnce()).expire(anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("Should register session with game ID")
        void shouldRegisterSessionWithGameId() {
            
            String sessionId = "session-123";
            String playerId = "player-456";
            UUID gameId = UUID.randomUUID();

            
            registry.registerSession(sessionId, playerId, gameId);

            
            verify(setOperations).add(
                    eq("ws:games:" + gameId),
                    eq(sessionId)
            );
        }

        @Test
        @DisplayName("Should unregister session and clean up all references")
        void shouldUnregisterSession() {
            
            String sessionId = "session-123";
            String playerId = "player-456";
            UUID gameId = UUID.randomUUID();

            
            registry.registerSession(sessionId, playerId, gameId);

            
            Map<Object, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", sessionId);
            sessionData.put("playerId", playerId);
            sessionData.put("gameId", gameId.toString());
            sessionData.put("instanceId", INSTANCE_ID);
            sessionData.put("connectedAt", "2024-01-01T00:00:00Z");
            when(hashOperations.entries("ws:sessions:" + sessionId)).thenReturn(sessionData);

            
            registry.unregisterSession(sessionId);

            
            verify(redisTemplate).delete("ws:sessions:" + sessionId);
            verify(setOperations).remove("ws:players:" + playerId, sessionId);
            verify(setOperations).remove("ws:games:" + gameId, sessionId);
            verify(setOperations).remove("ws:instances:" + INSTANCE_ID, sessionId);
        }
    }

    @Nested
    @DisplayName("Game Subscription Tests")
    class GameSubscriptionTests {

        @Test
        @DisplayName("Should update game subscription")
        void shouldSubscribeToGame() {
            
            String sessionId = "session-123";
            String playerId = "player-456";
            UUID newGameId = UUID.randomUUID();

            
            registry.registerSession(sessionId, playerId);

            
            registry.subscribeToGame(sessionId, newGameId);

            
            verify(hashOperations).put(
                    eq("ws:sessions:" + sessionId),
                    eq("gameId"),
                    eq(newGameId.toString())
            );
            verify(setOperations).add(
                    eq("ws:games:" + newGameId),
                    eq(sessionId)
            );
        }

        @Test
        @DisplayName("Should remove from old game when switching games")
        void shouldRemoveFromOldGameWhenSwitching() {
            
            String sessionId = "session-123";
            String playerId = "player-456";
            UUID oldGameId = UUID.randomUUID();
            UUID newGameId = UUID.randomUUID();

            
            registry.registerSession(sessionId, playerId, oldGameId);

            
            registry.subscribeToGame(sessionId, newGameId);

            
            verify(setOperations).remove(
                    eq("ws:games:" + oldGameId),
                    eq(sessionId)
            );
        }

        @Test
        @DisplayName("Should handle subscription for unknown session")
        void shouldHandleUnknownSession() {
            
            String unknownSessionId = "unknown-session";
            UUID gameId = UUID.randomUUID();

            
            assertThatNoException().isThrownBy(() ->
                    registry.subscribeToGame(unknownSessionId, gameId)
            );
        }
    }

    @Nested
    @DisplayName("Session Lookup Tests")
    class SessionLookupTests {

        @Test
        @DisplayName("Should get player ID for session from local cache")
        void shouldGetPlayerIdFromLocalCache() {
            
            String sessionId = "session-123";
            String playerId = "player-456";
            registry.registerSession(sessionId, playerId);

            
            Optional<String> result = registry.getPlayerIdForSession(sessionId);

            
            assertThat(result).isPresent().contains(playerId);
        }

        @Test
        @DisplayName("Should get player ID from Redis when not in local cache")
        void shouldGetPlayerIdFromRedis() {
            
            String sessionId = "session-123";
            String playerId = "player-456";
            when(hashOperations.get("ws:sessions:" + sessionId, "playerId")).thenReturn(playerId);

            
            Optional<String> result = registry.getPlayerIdForSession(sessionId);

            
            assertThat(result).isPresent().contains(playerId);
        }

        @Test
        @DisplayName("Should return empty when player ID not found")
        void shouldReturnEmptyWhenPlayerNotFound() {
            
            String sessionId = "unknown-session";
            when(hashOperations.get("ws:sessions:" + sessionId, "playerId")).thenReturn(null);

            
            Optional<String> result = registry.getPlayerIdForSession(sessionId);

            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get game ID for session")
        void shouldGetGameIdForSession() {
            
            String sessionId = "session-123";
            String playerId = "player-456";
            UUID gameId = UUID.randomUUID();
            registry.registerSession(sessionId, playerId, gameId);

            
            Optional<UUID> result = registry.getGameIdForSession(sessionId);

            
            assertThat(result).isPresent().contains(gameId);
        }
    }

    @Nested
    @DisplayName("Multi-Session Lookup Tests")
    class MultiSessionLookupTests {

        @Test
        @DisplayName("Should get all sessions for a player")
        void shouldGetSessionsForPlayer() {
            
            String playerId = "player-456";
            Set<Object> sessions = Set.of("session-1", "session-2", "session-3");
            when(setOperations.members("ws:players:" + playerId)).thenReturn(sessions);

            
            Set<String> result = registry.getSessionsForPlayer(playerId);

            
            assertThat(result).containsExactlyInAnyOrder("session-1", "session-2", "session-3");
        }

        @Test
        @DisplayName("Should get all sessions for a game")
        void shouldGetSessionsForGame() {
            
            UUID gameId = UUID.randomUUID();
            Set<Object> sessions = Set.of("session-1", "session-2");
            when(setOperations.members("ws:games:" + gameId)).thenReturn(sessions);

            
            Set<String> result = registry.getSessionsForGame(gameId);

            
            assertThat(result).containsExactlyInAnyOrder("session-1", "session-2");
        }

        @Test
        @DisplayName("Should return empty set when no sessions found")
        void shouldReturnEmptySetWhenNoSessions() {
            
            String playerId = "unknown-player";
            when(setOperations.members("ws:players:" + playerId)).thenReturn(null);

            
            Set<String> result = registry.getSessionsForPlayer(playerId);

            
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get session count for game")
        void shouldGetSessionCountForGame() {
            
            UUID gameId = UUID.randomUUID();
            when(setOperations.size("ws:games:" + gameId)).thenReturn(5L);

            
            long count = registry.getSessionCountForGame(gameId);

            
            assertThat(count).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Player Connection Status Tests")
    class PlayerConnectionStatusTests {

        @Test
        @DisplayName("Should return true when player has sessions")
        void shouldReturnTrueWhenPlayerConnected() {
            
            String playerId = "player-456";
            when(setOperations.size("ws:players:" + playerId)).thenReturn(2L);

            
            boolean connected = registry.isPlayerConnected(playerId);

            
            assertThat(connected).isTrue();
        }

        @Test
        @DisplayName("Should return false when player has no sessions")
        void shouldReturnFalseWhenPlayerDisconnected() {
            
            String playerId = "player-456";
            when(setOperations.size("ws:players:" + playerId)).thenReturn(0L);

            
            boolean connected = registry.isPlayerConnected(playerId);

            
            assertThat(connected).isFalse();
        }
    }

    @Nested
    @DisplayName("Local Sessions Tests")
    class LocalSessionsTests {

        @Test
        @DisplayName("Should return local sessions collection")
        void shouldReturnLocalSessions() {
            
            registry.registerSession("session-1", "player-1");
            registry.registerSession("session-2", "player-2");

            
            Collection<ClusterSessionRegistry.SessionInfo> sessions = registry.getLocalSessions();

            
            assertThat(sessions).hasSize(2);
        }

        @Test
        @DisplayName("Local sessions should be unmodifiable")
        void localSessionsShouldBeUnmodifiable() {

            registry.registerSession("session-1", "player-1");


            Collection<ClusterSessionRegistry.SessionInfo> sessions = registry.getLocalSessions();


            assertThatThrownBy(() -> sessions.clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Heartbeat Tests")
    class HeartbeatTests {

        @Test
        @DisplayName("Should refresh session TTL on heartbeat")
        void shouldRefreshTtlOnHeartbeat() {
            
            registry.registerSession("session-1", "player-1");
            reset(redisTemplate);

            
            registry.sendHeartbeat();

            
            verify(redisTemplate, atLeastOnce()).expire(anyString(), any(Duration.class));
        }
    }
}
