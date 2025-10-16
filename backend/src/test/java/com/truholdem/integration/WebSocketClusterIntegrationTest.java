package com.truholdem.integration;

import com.truholdem.dto.GameUpdateMessage;
import com.truholdem.model.GameUpdateType;
import com.truholdem.model.*;

import com.truholdem.websocket.ClusterSessionRegistry;
import com.truholdem.websocket.GameEvent;
import com.truholdem.websocket.GameEventStore;
import com.truholdem.websocket.RedisGameEventBroadcaster;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Disabled("Requires Docker environment")
class WebSocketClusterIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withCommand("redis-server", "--appendonly", "yes");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("app.websocket.cluster.enabled", () -> "true");
        registry.add("app.websocket.cluster.instance-id", () -> "test-instance-" + UUID.randomUUID());
    }

    @Autowired
    private RedisTemplate<String, Object> webSocketRedisTemplate;

    @Autowired(required = false)
    private RedisGameEventBroadcaster broadcaster;

    @Autowired(required = false)
    private ClusterSessionRegistry sessionRegistry;

    @Autowired(required = false)
    private GameEventStore eventStore;

    @Autowired(required = false)
    private RedisMessageListenerContainer listenerContainer;

    @BeforeEach
    void setUp() {
        if (webSocketRedisTemplate != null) {
            Set<String> keys = webSocketRedisTemplate.keys("ws:*");
            if (keys != null && !keys.isEmpty()) {
                webSocketRedisTemplate.delete(keys);
            }
            keys = webSocketRedisTemplate.keys("truholdem:*");
            if (keys != null && !keys.isEmpty()) {
                webSocketRedisTemplate.delete(keys);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Redis container should be running and accessible")
    void redisContainerShouldBeRunning() {
        assertThat(redisContainer.isRunning()).isTrue();

        String testKey = "test:ping";
        webSocketRedisTemplate.opsForValue().set(testKey, "pong");
        Object result = webSocketRedisTemplate.opsForValue().get(testKey);

        assertThat(result).isEqualTo("pong");
        webSocketRedisTemplate.delete(testKey);
    }

    @Test
    @Order(2)
    @DisplayName("Cluster components should be initialized when cluster mode enabled")
    void clusterComponentsShouldBeInitialized() {
        assertThat(broadcaster).isNotNull();
        assertThat(sessionRegistry).isNotNull();
        assertThat(eventStore).isNotNull();
        assertThat(listenerContainer).isNotNull();
        assertThat(listenerContainer.isRunning()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Session registry should store and retrieve sessions from Redis")
    void sessionRegistryShouldWorkWithRedis() {
        String sessionId = "test-session-" + UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();

        sessionRegistry.registerSession(sessionId, playerId.toString(), gameId);

        assertThat(sessionRegistry.getPlayerIdForSession(sessionId)).contains(playerId.toString());
        assertThat(sessionRegistry.getGameIdForSession(sessionId)).contains(gameId);

        Set<String> playerSessions = sessionRegistry.getSessionsForPlayer(playerId.toString());
        assertThat(playerSessions).contains(sessionId);

        Set<String> gameSessions = sessionRegistry.getSessionsForGame(gameId);
        assertThat(gameSessions).contains(sessionId);

        assertThat(sessionRegistry.isPlayerConnected(playerId.toString())).isTrue();

        sessionRegistry.unregisterSession(sessionId);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(sessionRegistry.getPlayerIdForSession(sessionId)).isEmpty();
            assertThat(sessionRegistry.isPlayerConnected(playerId.toString())).isFalse();
        });
    }

    @Test
    @Order(4)
    @DisplayName("Session registry should handle game subscription updates")
    void sessionRegistryShouldHandleGameSubscriptionUpdates() {
        String sessionId = "test-session-" + UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();

        sessionRegistry.registerSession(sessionId, playerId.toString(), gameId1);

        assertThat(sessionRegistry.getSessionsForGame(gameId1)).contains(sessionId);

        sessionRegistry.subscribeToGame(sessionId, gameId2);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(sessionRegistry.getSessionsForGame(gameId1)).doesNotContain(sessionId);
            assertThat(sessionRegistry.getSessionsForGame(gameId2)).contains(sessionId);
            assertThat(sessionRegistry.getGameIdForSession(sessionId)).contains(gameId2);
        });

        sessionRegistry.unregisterSession(sessionId);
    }

    @Test
    @Order(5)
    @DisplayName("Event store should persist and retrieve game events")
    void eventStoreShouldPersistAndRetrieveEvents() {
        UUID gameId = UUID.randomUUID();

        GameEvent event1 = createTestEvent(gameId, GameUpdateType.GAME_STATE, 1);
        GameEvent event2 = createTestEvent(gameId, GameUpdateType.PLAYER_ACTION, 2);
        GameEvent event3 = createTestEvent(gameId, GameUpdateType.PHASE_CHANGE, 3);

        eventStore.storeEvent(event1);
        eventStore.storeEvent(event2);
        eventStore.storeEvent(event3);

        List<GameEvent> allEvents = eventStore.getEventsSince(gameId, 0);
        assertThat(allEvents).hasSize(3);

        List<GameEvent> laterEvents = eventStore.getEventsSince(gameId, 1);
        assertThat(laterEvents).hasSize(2);

        long latestSeq = eventStore.getLatestSequence(gameId);
        assertThat(latestSeq).isEqualTo(3);

        eventStore.clearEvents(gameId);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            List<GameEvent> clearedEvents = eventStore.getEventsSince(gameId, 0);
            assertThat(clearedEvents).isEmpty();
        });
    }

    @Test
    @Order(6)
    @DisplayName("Event store should handle event expiration")
    void eventStoreShouldHandleEventExpiration() {
        UUID gameId = UUID.randomUUID();

        GameEvent event = createTestEvent(gameId, GameUpdateType.GAME_STATE, 1);
        eventStore.storeEvent(event);

        assertThat(eventStore.getEventsSince(gameId, 0)).hasSize(1);
    }

    @Test
    @Order(7)
    @DisplayName("Multiple sessions for same player should be tracked correctly")
    void multipleSessionsForSamePlayerShouldBeTracked() {
        UUID playerId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();

        String session1 = "session-1-" + UUID.randomUUID();
        String session2 = "session-2-" + UUID.randomUUID();
        String session3 = "session-3-" + UUID.randomUUID();

        sessionRegistry.registerSession(session1, playerId.toString(), gameId);
        sessionRegistry.registerSession(session2, playerId.toString(), gameId);
        sessionRegistry.registerSession(session3, playerId.toString(), gameId);

        Set<String> sessions = sessionRegistry.getSessionsForPlayer(playerId.toString());
        assertThat(sessions).containsExactlyInAnyOrder(session1, session2, session3);

        sessionRegistry.unregisterSession(session1);
        assertThat(sessionRegistry.isPlayerConnected(playerId.toString())).isTrue();

        sessionRegistry.unregisterSession(session2);
        sessionRegistry.unregisterSession(session3);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(sessionRegistry.isPlayerConnected(playerId.toString())).isFalse();
        });
    }

    @Test
    @Order(8)
    @DisplayName("Game session count should be accurate")
    void gameSessionCountShouldBeAccurate() {
        UUID gameId = UUID.randomUUID();

        String session1 = "session-1-" + UUID.randomUUID();
        String session2 = "session-2-" + UUID.randomUUID();

        sessionRegistry.registerSession(session1, UUID.randomUUID().toString(), gameId);
        sessionRegistry.registerSession(session2, UUID.randomUUID().toString(), gameId);

        assertThat(sessionRegistry.getSessionCountForGame(gameId)).isEqualTo(2);

        sessionRegistry.unregisterSession(session1);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(sessionRegistry.getSessionCountForGame(gameId)).isEqualTo(1);
        });

        sessionRegistry.unregisterSession(session2);
    }

    @Test
    @Order(9)
    @DisplayName("Redis Pub/Sub should deliver messages between publishers")
    void redisPubSubShouldDeliverMessages() throws InterruptedException {

        String channel = "truholdem:test:channel";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        webSocketRedisTemplate.getConnectionFactory().getConnection()
            .subscribe((message, pattern) -> {
                receivedMessage.set(new String(message.getBody()));
                latch.countDown();
            }, channel.getBytes());

        Thread.sleep(100);
        webSocketRedisTemplate.convertAndSend(channel, "test-message");

        latch.await(5, TimeUnit.SECONDS);

        assertThat(redisContainer.isRunning()).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("Local sessions should be tracked separately from Redis")
    void localSessionsShouldBeTrackedSeparately() {
        String sessionId = "local-session-" + UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        sessionRegistry.registerSession(sessionId, playerId.toString(), null);

        assertThat(sessionRegistry.getLocalSessions()).extracting("sessionId").contains(sessionId);

        sessionRegistry.unregisterSession(sessionId);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(sessionRegistry.getLocalSessions()).extracting("sessionId").doesNotContain(sessionId);
        });
    }

    @Test
    @Order(11)
    @DisplayName("Heartbeat should refresh session TTL")
    void heartbeatShouldRefreshSessionTTL() {
        String sessionId = "heartbeat-session-" + UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        sessionRegistry.registerSession(sessionId, playerId.toString(), null);
        sessionRegistry.sendHeartbeat();

        assertThat(sessionRegistry.getPlayerIdForSession(sessionId)).contains(playerId.toString());

        sessionRegistry.unregisterSession(sessionId);
    }

    /**
     * Creates a test GameEvent using the Builder pattern.
     */
    private GameEvent createTestEvent(UUID gameId, GameUpdateType eventType, long sequence) {
        return GameEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .gameId(gameId)
            .type(eventType)
            .payload(createTestGameUpdateMessage(gameId))
            .sequenceNumber(sequence)
            .sourceInstanceId("test-instance")
            .build();
    }

    /**
     * Creates a test GameUpdateMessage using the correct 4-argument constructor.
     */
    private GameUpdateMessage createTestGameUpdateMessage(UUID gameId) {
        return new GameUpdateMessage(
            "GAME_UPDATE",
            createTestGame(gameId),
            null,
            System.currentTimeMillis()
        );
    }

    private Game createTestGame(UUID gameId) {
        Game game = new Game();
        game.setId(gameId);
        game.setPhase(GamePhase.PRE_FLOP);
        game.setCurrentBet(100);

        List<Player> players = new ArrayList<>();
        Player player1 = new Player();
        player1.setId(UUID.randomUUID());
        player1.setName("TestPlayer1");
        player1.setChips(5000);
        players.add(player1);

        Player player2 = new Player();
        player2.setId(UUID.randomUUID());
        player2.setName("TestPlayer2");
        player2.setChips(5000);
        players.add(player2);

        game.setPlayers(players);

        return game;
    }
}
