package com.truholdem.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("Observability Integration Tests")
class ObservabilityIntegrationTest {

    private InMemorySpanExporter spanExporter;
    private SdkTracerProvider tracerProvider;
    private SdkMeterProvider meterProvider;
    private OpenTelemetry openTelemetry;
    private Tracer tracer;
    private Meter meter;
    private GameTracer gameTracer;
    private GameMetrics gameMetrics;

    @BeforeEach
    void setUp() {
        
        spanExporter = InMemorySpanExporter.create();

        
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();

        
        meterProvider = SdkMeterProvider.builder().build();

        
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        
        tracer = openTelemetry.getTracer("test-tracer", "1.0.0");
        meter = openTelemetry.getMeter("test-meter");

        
        gameTracer = new GameTracer(tracer);
        gameMetrics = new GameMetrics(meter);
        gameMetrics.initializeMetrics();
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
        meterProvider.close();
        spanExporter.reset();
    }

    @Nested
    @DisplayName("Distributed Tracing Tests")
    class DistributedTracingTests {

        @Test
        @DisplayName("Should create and export spans for game operations")
        void shouldCreateAndExportSpans() {
            
            UUID gameId = UUID.randomUUID();

            
            Span span = gameTracer.startGameSpan(gameId, "create-game");
            span.setAttribute("test.attribute", "value");
            span.end();

            
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).hasSize(1);
            
            SpanData spanData = spans.get(0);
            assertThat(spanData.getName()).isEqualTo("game.create-game");
            assertThat(spanData.getAttributes().get(
                    io.opentelemetry.api.common.AttributeKey.stringKey("game.id")))
                    .isEqualTo(gameId.toString());
        }

        @Test
        @DisplayName("Should create parent-child span relationships")
        void shouldCreateParentChildSpanRelationships() {
            
            UUID gameId = UUID.randomUUID();

            
            Span parentSpan = gameTracer.startGameSpan(gameId, "hand");
            try (Scope scope = parentSpan.makeCurrent()) {
                Span childSpan = gameTracer.createChildSpan(parentSpan, "deal-cards");
                childSpan.end();
            }
            parentSpan.end();

            
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).hasSize(2);

            SpanData childSpanData = spans.get(0);
            SpanData parentSpanData = spans.get(1);
            
            assertThat(childSpanData.getParentSpanId())
                    .isEqualTo(parentSpanData.getSpanId());
        }

        @Test
        @DisplayName("Should record events within spans")
        void shouldRecordEventsWithinSpans() {
            
            UUID gameId = UUID.randomUUID();
            UUID playerId = UUID.randomUUID();

            
            Span span = gameTracer.startGameSpan(gameId, "player-action");
            gameTracer.recordPlayerAction(span, playerId, "TestPlayer", "RAISE", 100);
            gameTracer.recordPhaseTransition(span, "PRE_FLOP", "FLOP");
            span.end();

            
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).hasSize(1);
            
            SpanData spanData = spans.get(0);
            assertThat(spanData.getEvents()).hasSize(2);
            assertThat(spanData.getEvents().get(0).getName()).isEqualTo("player_action");
            assertThat(spanData.getEvents().get(1).getName()).isEqualTo("phase_transition");
        }

        @Test
        @DisplayName("Should handle errors in spans correctly")
        void shouldHandleErrorsInSpans() {
            
            UUID gameId = UUID.randomUUID();
            RuntimeException error = new RuntimeException("Test error message");

            
            Span span = gameTracer.startGameSpan(gameId, "failing-operation");
            gameTracer.recordError(span, error);
            span.end();

            
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).hasSize(1);
            
            SpanData spanData = spans.get(0);
            assertThat(spanData.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
            assertThat(spanData.getStatus().getDescription()).isEqualTo("Test error message");
        }

        @Test
        @DisplayName("Should trace complete game flow")
        void shouldTraceCompleteGameFlow() {
            
            UUID gameId = UUID.randomUUID();
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();

            
            Span gameSpan = gameTracer.startGameSpan(gameId, "hand");
            try (Scope scope = gameSpan.makeCurrent()) {
                
                gameTracer.recordPlayerAction(gameSpan, player1, "Player1", "RAISE", 100);
                gameTracer.recordPlayerAction(gameSpan, player2, "Player2", "CALL", 100);
                
                
                gameTracer.recordPhaseTransition(gameSpan, "PRE_FLOP", "FLOP");
                gameTracer.recordPlayerAction(gameSpan, player1, "Player1", "CHECK", 0);
                gameTracer.recordPlayerAction(gameSpan, player2, "Player2", "BET", 50);
                
                
                gameTracer.recordPhaseTransition(gameSpan, "FLOP", "SHOWDOWN");
                gameTracer.recordHandCompletion(gameSpan, 1, "Player2", "Pair of Aces", 300);
            }
            gameSpan.end();

            
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).hasSize(1);
            
            SpanData spanData = spans.get(0);
            
            assertThat(spanData.getEvents()).hasSize(7);
        }
    }

    @Nested
    @DisplayName("Metrics Collection Tests")
    class MetricsCollectionTests {

        @Test
        @DisplayName("Should track game lifecycle metrics")
        void shouldTrackGameLifecycleMetrics() {
            
            gameMetrics.recordGameStarted("cash", 6);
            gameMetrics.recordGameStarted("cash", 4);
            gameMetrics.recordGameCompleted("cash", 10);

            
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            assertThat(snapshot.activeGames()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track hand metrics with pot sizes")
        void shouldTrackHandMetricsWithPotSizes() {
            
            gameMetrics.recordHandCompleted(Duration.ofMillis(5000), 500, "SHOWDOWN");
            gameMetrics.recordHandCompleted(Duration.ofMillis(3000), 2000, "SHOWDOWN");
            gameMetrics.recordHandCompleted(Duration.ofMillis(1000), 1000, "FOLD");

            
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            assertThat(snapshot.largestPot()).isEqualTo(2000);
        }

        @Test
        @DisplayName("Should track tournament metrics independently")
        void shouldTrackTournamentMetricsIndependently() {
            
            gameMetrics.recordTournamentCreated("SNG", 100);
            gameMetrics.recordTournamentCreated("MTT", 50);
            gameMetrics.recordTournamentCompleted("SNG", 9);

            
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            assertThat(snapshot.activeTournaments()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track active players correctly")
        void shouldTrackActivePlayersCorrectly() {
            
            gameMetrics.recordPlayerJoined();
            gameMetrics.recordPlayerJoined();
            gameMetrics.recordPlayerJoined();
            gameMetrics.recordPlayerLeft();

            
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            assertThat(snapshot.activePlayers()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent span creation")
        void shouldHandleConcurrentSpanCreation() throws InterruptedException {
            
            int threadCount = 10;
            int spansPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < spansPerThread; j++) {
                            UUID gameId = UUID.randomUUID();
                            Span span = gameTracer.startGameSpan(gameId, "concurrent-test");
                            span.end();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            
            assertThat(completed).isTrue();
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).hasSize(threadCount * spansPerThread);
        }

        @Test
        @DisplayName("Should handle concurrent metrics recording")
        void shouldHandleConcurrentMetricsRecording() throws InterruptedException {
            
            int threadCount = 10;
            int operationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            gameMetrics.recordPlayerAction("RAISE", false);
                            gameMetrics.recordHandCompleted(
                                    Duration.ofMillis(j * 10), 
                                    j * 100, 
                                    "SHOWDOWN"
                            );
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            
            assertThat(completed).isTrue();
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            
            assertThat(snapshot.largestPot()).isEqualTo(9900);
        }
    }

    @Nested
    @DisplayName("Combined Tracing and Metrics Tests")
    class CombinedTracingMetricsTests {

        @Test
        @DisplayName("Should correlate traces with metrics")
        void shouldCorrelateTracesWithMetrics() {
            
            UUID gameId = UUID.randomUUID();

            
            gameMetrics.recordGameStarted("cash", 6);
            
            Span gameSpan = gameTracer.startGameSpan(gameId, "game-session");
            try (Scope scope = gameSpan.makeCurrent()) {
                
                gameMetrics.recordPlayerAction("RAISE", false);
                gameTracer.recordPlayerAction(gameSpan, UUID.randomUUID(), "P1", "RAISE", 100);
                gameMetrics.recordHandCompleted(Duration.ofMillis(5000), 500, "SHOWDOWN");
                
                
                gameMetrics.recordPlayerAction("FOLD", false);
                gameTracer.recordPlayerAction(gameSpan, UUID.randomUUID(), "P2", "FOLD", 0);
                gameMetrics.recordHandCompleted(Duration.ofMillis(2000), 200, "FOLD");
            }
            gameSpan.end();
            
            gameMetrics.recordGameCompleted("cash", 2);

            
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            assertThat(spans).hasSize(1);
            assertThat(spans.get(0).getEvents()).hasSize(2);

            
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            assertThat(snapshot.activeGames()).isEqualTo(0);
            assertThat(snapshot.largestPot()).isEqualTo(500);
        }
    }
}
