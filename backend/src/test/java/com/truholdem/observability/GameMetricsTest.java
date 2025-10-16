package com.truholdem.observability;

import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("GameMetrics Tests")
class GameMetricsTest {

    @Mock
    private Meter meter;

    @Mock
    private LongCounterBuilder longCounterBuilder;

    @Mock
    private LongCounter longCounter;

    @Mock
    private DoubleHistogramBuilder doubleHistogramBuilder;

    @Mock
    private DoubleHistogram doubleHistogram;

    @Mock
    private LongUpDownCounterBuilder longUpDownCounterBuilder;

    @Mock
    private LongUpDownCounter longUpDownCounter;

    @Mock
    private DoubleGaugeBuilder doubleGaugeBuilder;

    private GameMetrics gameMetrics;

    @BeforeEach
    void setUp() {
        
        when(meter.counterBuilder(anyString())).thenReturn(longCounterBuilder);
        when(longCounterBuilder.setDescription(anyString())).thenReturn(longCounterBuilder);
        when(longCounterBuilder.setUnit(anyString())).thenReturn(longCounterBuilder);
        when(longCounterBuilder.build()).thenReturn(longCounter);

        
        when(meter.histogramBuilder(anyString())).thenReturn(doubleHistogramBuilder);
        when(doubleHistogramBuilder.setDescription(anyString())).thenReturn(doubleHistogramBuilder);
        when(doubleHistogramBuilder.setUnit(anyString())).thenReturn(doubleHistogramBuilder);
        when(doubleHistogramBuilder.build()).thenReturn(doubleHistogram);

        
        when(meter.upDownCounterBuilder(anyString())).thenReturn(longUpDownCounterBuilder);
        when(longUpDownCounterBuilder.setDescription(anyString())).thenReturn(longUpDownCounterBuilder);
        when(longUpDownCounterBuilder.setUnit(anyString())).thenReturn(longUpDownCounterBuilder);
        when(longUpDownCounterBuilder.build()).thenReturn(longUpDownCounter);

        
        when(meter.gaugeBuilder(anyString())).thenReturn(doubleGaugeBuilder);
        when(doubleGaugeBuilder.setDescription(anyString())).thenReturn(doubleGaugeBuilder);
        when(doubleGaugeBuilder.setUnit(anyString())).thenReturn(doubleGaugeBuilder);
        when(doubleGaugeBuilder.buildWithCallback(any())).thenReturn(mock(ObservableDoubleGauge.class));

        gameMetrics = new GameMetrics(meter);
        gameMetrics.initializeMetrics();
    }

    @Nested
    @DisplayName("Metric Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize all counters")
        void shouldInitializeAllCounters() {
            verify(meter).counterBuilder("truholdem.games.started");
            verify(meter).counterBuilder("truholdem.games.completed");
            verify(meter).counterBuilder("truholdem.hands.played");
            verify(meter).counterBuilder("truholdem.player.actions");
            verify(meter).counterBuilder("truholdem.bot.decisions");
            verify(meter).counterBuilder("truholdem.tournaments.created");
            verify(meter).counterBuilder("truholdem.tournaments.completed");
            verify(meter).counterBuilder("truholdem.tournament.eliminations");
        }

        @Test
        @DisplayName("Should initialize all histograms")
        void shouldInitializeAllHistograms() {
            verify(meter).histogramBuilder("truholdem.hand.duration");
            verify(meter).histogramBuilder("truholdem.bot.decision_time");
            verify(meter).histogramBuilder("truholdem.pot.size");
            verify(meter).histogramBuilder("truholdem.action.processing_time");
            verify(meter).histogramBuilder("truholdem.game.creation_time");
            verify(meter).histogramBuilder("truholdem.tournament.duration");
        }

        @Test
        @DisplayName("Should initialize all up-down counters")
        void shouldInitializeAllUpDownCounters() {
            verify(meter).upDownCounterBuilder("truholdem.games.active");
            verify(meter).upDownCounterBuilder("truholdem.tournaments.active");
            verify(meter).upDownCounterBuilder("truholdem.players.active");
            verify(meter).upDownCounterBuilder("truholdem.tournament.players.registered");
            verify(meter).upDownCounterBuilder("truholdem.tournament.players.remaining");
        }

        @Test
        @DisplayName("Should initialize all gauges")
        void shouldInitializeAllGauges() {
            verify(meter).gaugeBuilder("truholdem.games.total_active");
            verify(meter).gaugeBuilder("truholdem.tournaments.total_active");
            verify(meter).gaugeBuilder("truholdem.pot.largest_seen");
            verify(meter).gaugeBuilder("truholdem.tournament.players.registered.total");
            verify(meter).gaugeBuilder("truholdem.tournament.players.remaining.total");
        }
    }

    @Nested
    @DisplayName("Game Lifecycle Metrics Tests")
    class GameLifecycleTests {

        @Test
        @DisplayName("Should record game started with attributes")
        void shouldRecordGameStarted() {
            
            gameMetrics.recordGameStarted("cash", 6);

            
            verify(longCounter).add(eq(1L), any());
            verify(longUpDownCounter).add(1);
        }

        @Test
        @DisplayName("Should record game completed and decrement active count")
        void shouldRecordGameCompleted() {
            
            gameMetrics.recordGameCompleted("cash", 10);

            
            verify(longCounter, atLeastOnce()).add(eq(1L), any());
            verify(longUpDownCounter).add(-1);
        }
    }

    @Nested
    @DisplayName("Hand Metrics Tests")
    class HandMetricsTests {

        @Test
        @DisplayName("Should record hand completed with duration and pot size")
        void shouldRecordHandCompleted() {

            gameMetrics.recordHandCompleted(Duration.ofMillis(5000), 1500, "SHOWDOWN");


            verify(longCounter).add(eq(1L), any());
            // handDurationHistogram.record(duration, attributes) - 2 args
            verify(doubleHistogram).record(anyDouble(), any());
            // potSizeHistogram.record(potSize) - 1 arg only
            verify(doubleHistogram).record(anyDouble());
        }

        @Test
        @DisplayName("Should track largest pot seen")
        void shouldTrackLargestPot() {
            
            gameMetrics.recordHandCompleted(Duration.ofMillis(1000), 500, "SHOWDOWN");
            gameMetrics.recordHandCompleted(Duration.ofMillis(2000), 2000, "SHOWDOWN");
            gameMetrics.recordHandCompleted(Duration.ofMillis(1500), 1000, "SHOWDOWN");

            
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            assertThat(snapshot.largestPot()).isEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("Player Action Metrics Tests")
    class PlayerActionTests {

        @Test
        @DisplayName("Should record player action with type")
        void shouldRecordPlayerAction() {
            
            gameMetrics.recordPlayerAction("RAISE", false);

            
            verify(longCounter).add(eq(1L), any());
        }

        @Test
        @DisplayName("Should record bot action separately")
        void shouldRecordBotAction() {
            
            gameMetrics.recordPlayerAction("CALL", true);

            
            verify(longCounter).add(eq(1L), any());
        }

        @Test
        @DisplayName("Should record action processing time")
        void shouldRecordActionProcessingTime() {
            
            gameMetrics.recordActionProcessingTime(50L, "RAISE");

            
            verify(doubleHistogram).record(eq(50.0), any());
        }
    }

    @Nested
    @DisplayName("Bot Metrics Tests")
    class BotMetricsTests {

        @Test
        @DisplayName("Should record bot decision with timing")
        void shouldRecordBotDecision() {
            
            gameMetrics.recordBotDecision("advanced", 150L, "RAISE");

            
            verify(longCounter).add(eq(1L), any());
            verify(doubleHistogram).record(eq(150.0), any());
        }
    }

    @Nested
    @DisplayName("Tournament Metrics Tests")
    class TournamentMetricsTests {

        @Test
        @DisplayName("Should record tournament created")
        void shouldRecordTournamentCreated() {
            
            gameMetrics.recordTournamentCreated("SNG", 100);

            
            verify(longCounter).add(eq(1L), any());
            verify(longUpDownCounter).add(1);
        }

        @Test
        @DisplayName("Should record tournament completed")
        void shouldRecordTournamentCompleted() {
            
            gameMetrics.recordTournamentCompleted("MTT", 50);

            
            verify(longCounter, atLeastOnce()).add(eq(1L), any());
            verify(longUpDownCounter).add(-1);
        }
    }

    @Nested
    @DisplayName("Player Session Metrics Tests")
    class PlayerSessionTests {

        @Test
        @DisplayName("Should track player join")
        void shouldTrackPlayerJoin() {
            
            gameMetrics.recordPlayerJoined();

            
            verify(longUpDownCounter).add(1);
        }

        @Test
        @DisplayName("Should track player leave")
        void shouldTrackPlayerLeave() {
            
            gameMetrics.recordPlayerLeft();

            
            verify(longUpDownCounter).add(-1);
        }
    }

    @Nested
    @DisplayName("Metrics Snapshot Tests")
    class SnapshotTests {

        @Test
        @DisplayName("Should return correct snapshot after operations")
        void shouldReturnCorrectSnapshot() {
            
            gameMetrics.recordGameStarted("cash", 4);
            gameMetrics.recordGameStarted("cash", 6);
            gameMetrics.recordTournamentCreated("SNG", 50);
            gameMetrics.recordPlayerJoined();
            gameMetrics.recordPlayerJoined();
            gameMetrics.recordPlayerJoined();
            gameMetrics.recordHandCompleted(Duration.ofMillis(1000), 5000, "SHOWDOWN");

            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();

            
            assertThat(snapshot.activeGames()).isEqualTo(2);
            assertThat(snapshot.activeTournaments()).isEqualTo(1);
            assertThat(snapshot.activePlayers()).isEqualTo(3);
            assertThat(snapshot.largestPot()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Should decrement values correctly")
        void shouldDecrementValuesCorrectly() {
            
            gameMetrics.recordGameStarted("cash", 4);
            gameMetrics.recordGameStarted("cash", 6);

            
            gameMetrics.recordGameCompleted("cash", 5);

            
            GameMetrics.MetricsSnapshot snapshot = gameMetrics.getSnapshot();
            assertThat(snapshot.activeGames()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Performance Metrics Tests")
    class PerformanceMetricsTests {

        @Test
        @DisplayName("Should record game creation time")
        void shouldRecordGameCreationTime() {
            
            gameMetrics.recordGameCreationTime(250L, 6);

            
            verify(doubleHistogram).record(eq(250.0), any());
        }
    }
}
