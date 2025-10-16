package com.truholdem.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.WebSocketClusterConfig;
import com.truholdem.dto.WebSocketGameUpdateMessage;
import com.truholdem.model.GameUpdateType;
import com.truholdem.dto.ShowdownResult;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.Player;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RedisGameEventBroadcasterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private RedisGameEventBroadcaster broadcaster;

    private static final String INSTANCE_ID = "test-instance-1";
    private static final String OTHER_INSTANCE_ID = "test-instance-2";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        meterRegistry = new SimpleMeterRegistry();

        broadcaster = new RedisGameEventBroadcaster(
            redisTemplate,
            messagingTemplate,
            objectMapper,
            INSTANCE_ID,
            meterRegistry
        );
    }

    @Nested
    @DisplayName("Event Publishing Tests")
    class EventPublishingTests {

        @Test
        @DisplayName("Should publish game update to Redis and local clients")
        void shouldPublishGameUpdate() {

            Game game = createTestGame();


            broadcaster.broadcastGameUpdate(game);


            verify(redisTemplate).convertAndSend(
                eq(WebSocketClusterConfig.GAME_EVENTS_CHANNEL),
                any(GameEvent.class)
            );
            verify(messagingTemplate).convertAndSend(
                argThat((String s) -> s != null && s.startsWith("/topic/game/")),
                any(WebSocketGameUpdateMessage.class)
            );
        }

        @Test
        @DisplayName("Should publish player action to Redis")
        void shouldPublishPlayerAction() {

            Game game = createTestGame();
            Player player = game.getPlayers().get(0);


            broadcaster.broadcastPlayerAction(game, player, "RAISE", 100);


            ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);
            verify(redisTemplate).convertAndSend(
                eq(WebSocketClusterConfig.GAME_EVENTS_CHANNEL),
                eventCaptor.capture()
            );

            GameEvent event = eventCaptor.getValue();
            assertThat(event.getGameId()).isEqualTo(game.getId());
            assertThat(event.getType()).isEqualTo(GameUpdateType.PLAYER_ACTION);
            assertThat(event.getSourceInstanceId()).isEqualTo(INSTANCE_ID);
        }

        @Test
        @DisplayName("Should publish phase change to Redis")
        void shouldPublishPhaseChange() {

            Game game = createTestGame();
            game.setPhase(GamePhase.FLOP);


            broadcaster.broadcastPhaseChange(game);


            ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);
            verify(redisTemplate).convertAndSend(
                eq(WebSocketClusterConfig.GAME_EVENTS_CHANNEL),
                eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue().getType()).isEqualTo(GameUpdateType.PHASE_CHANGE);
        }

        @Test
        @DisplayName("Should publish showdown result to Redis")
        void shouldPublishShowdown() {

            Game game = createTestGame();
            ShowdownResult.WinnerInfo winnerInfo = new ShowdownResult.WinnerInfo(
                game.getPlayers().get(0).getId(),
                game.getPlayers().get(0).getName(),
                1000,
                "Royal Flush",
                List.of()
            );
            ShowdownResult result = new ShowdownResult(
                List.of(winnerInfo),
                1000,
                "Player1 wins with Royal Flush"
            );


            broadcaster.broadcastShowdown(game, result);


            verify(redisTemplate).convertAndSend(
                eq(WebSocketClusterConfig.GAME_EVENTS_CHANNEL),
                any(GameEvent.class)
            );
        }

        @Test
        @DisplayName("Should publish game ended event to Redis")
        void shouldPublishGameEnded() {

            Game game = createTestGame();


            broadcaster.broadcastGameEnded(game, "Winner");


            ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);
            verify(redisTemplate).convertAndSend(
                eq(WebSocketClusterConfig.GAME_EVENTS_CHANNEL),
                eventCaptor.capture()
            );

            assertThat(eventCaptor.getValue().getType()).isEqualTo(GameUpdateType.GAME_ENDED);
        }

        @Test
        @DisplayName("Should not publish when game is null")
        void shouldNotPublishWhenGameIsNull() {

            broadcaster.broadcastGameUpdate(null);


            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("Should not publish when game ID is null")
        void shouldNotPublishWhenGameIdIsNull() {

            Game game = new Game();
            game.setId(null);


            broadcaster.broadcastGameUpdate(game);


            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("Event Receiving Tests")
    class EventReceivingTests {

        @Test
        @DisplayName("Should forward events from other instances to local clients")
        void shouldForwardEventsFromOtherInstances() throws Exception {

            UUID gameId = UUID.randomUUID();
            GameEvent event = GameEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceInstanceId(OTHER_INSTANCE_ID)
                .gameId(gameId)
                .type(GameUpdateType.GAME_STATE)
                .destination("/topic/game/" + gameId)
                .payload("test payload")
                .build();

            String eventJson = objectMapper.writeValueAsString(event);
            Message message = createRedisMessage(eventJson);


            broadcaster.handleRedisMessage(message);


            verify(messagingTemplate).convertAndSend(
                eq("/topic/game/" + gameId),
                eq("test payload")
            );
        }

        @Test
        @DisplayName("Should NOT forward own events (echo prevention)")
        void shouldNotForwardOwnEvents() throws Exception {

            UUID gameId = UUID.randomUUID();
            GameEvent event = GameEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceInstanceId(INSTANCE_ID)
                .gameId(gameId)
                .type(GameUpdateType.GAME_STATE)
                .destination("/topic/game/" + gameId)
                .payload("test payload")
                .build();

            String eventJson = objectMapper.writeValueAsString(event);
            Message message = createRedisMessage(eventJson);


            broadcaster.handleRedisMessage(message);


            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("Should not forward duplicate events")
        void shouldNotForwardDuplicateEvents() throws Exception {

            UUID gameId = UUID.randomUUID();
            String eventId = UUID.randomUUID().toString();

            GameEvent event = GameEvent.builder()
                .eventId(eventId)
                .sourceInstanceId(OTHER_INSTANCE_ID)
                .gameId(gameId)
                .type(GameUpdateType.GAME_STATE)
                .destination("/topic/game/" + gameId)
                .payload("test payload")
                .build();

            String eventJson = objectMapper.writeValueAsString(event);
            Message message = createRedisMessage(eventJson);


            broadcaster.handleRedisMessage(message);
            broadcaster.handleRedisMessage(message);


            verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/game/" + gameId),
                eq("test payload")
            );
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJson() {

            Message message = createRedisMessage("not valid json");


            assertThatNoException().isThrownBy(() ->
                broadcaster.handleRedisMessage(message)
            );

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("Error Broadcasting Tests")
    class ErrorBroadcastingTests {

        @Test
        @DisplayName("Should broadcast error messages")
        void shouldBroadcastErrors() {

            UUID gameId = UUID.randomUUID();


            broadcaster.broadcastError(gameId, "Test error message");


            verify(redisTemplate).convertAndSend(
                eq(WebSocketClusterConfig.GAME_EVENTS_CHANNEL),
                any(GameEvent.class)
            );
        }

        @Test
        @DisplayName("Should not broadcast error when game ID is null")
        void shouldNotBroadcastErrorWithNullGameId() {

            broadcaster.broadcastError(null, "Test error");


            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should increment published events counter")
        void shouldIncrementPublishedCounter() {

            Game game = createTestGame();
            double initialCount = meterRegistry.counter("websocket.cluster.events.published", "instance", INSTANCE_ID).count();


            broadcaster.broadcastGameUpdate(game);


            double newCount = meterRegistry.counter("websocket.cluster.events.published", "instance", INSTANCE_ID).count();
            assertThat(newCount).isGreaterThan(initialCount);
        }

        @Test
        @DisplayName("Should increment forwarded events counter")
        void shouldIncrementForwardedCounter() throws Exception {

            UUID gameId = UUID.randomUUID();
            GameEvent event = GameEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sourceInstanceId(OTHER_INSTANCE_ID)
                .gameId(gameId)
                .type(GameUpdateType.GAME_STATE)
                .destination("/topic/game/" + gameId)
                .payload("test")
                .build();

            String eventJson = objectMapper.writeValueAsString(event);
            Message message = createRedisMessage(eventJson);
            double initialCount = meterRegistry.counter("websocket.cluster.events.forwarded", "instance", INSTANCE_ID).count();


            broadcaster.handleRedisMessage(message);


            double newCount = meterRegistry.counter("websocket.cluster.events.forwarded", "instance", INSTANCE_ID).count();
            assertThat(newCount).isGreaterThan(initialCount);
        }
    }


    private Game createTestGame() {
        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setPhase(GamePhase.PRE_FLOP);

        Player player1 = new Player();
        player1.setId(UUID.randomUUID());
        player1.setName("Player1");
        player1.setChips(1000);

        Player player2 = new Player();
        player2.setId(UUID.randomUUID());
        player2.setName("Player2");
        player2.setChips(1000);

        game.setPlayers(List.of(player1, player2));
        return game;
    }

    private Message createRedisMessage(String body) {
        return new Message() {
            @Override
            public byte[] getBody() {
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public byte[] getChannel() {
                return WebSocketClusterConfig.GAME_EVENTS_CHANNEL.getBytes(StandardCharsets.UTF_8);
            }
        };
    }
}
