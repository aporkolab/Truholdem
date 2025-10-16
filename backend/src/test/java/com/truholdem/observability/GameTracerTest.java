package com.truholdem.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GameTracer Tests")
class GameTracerTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Scope scope;

    private GameTracer gameTracer;

    @BeforeEach
    void setUp() {
        gameTracer = new GameTracer(tracer);
    }

    @Nested
    @DisplayName("Span Creation Tests")
    class SpanCreationTests {

        @Test
        @DisplayName("Should create span with game ID attribute")
        @SuppressWarnings("unchecked")
        void shouldCreateSpanWithGameId() {

            UUID gameId = UUID.randomUUID();
            when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
            when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
            when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
            when(spanBuilder.setAttribute(any(AttributeKey.class), anyString())).thenReturn(spanBuilder);
            when(spanBuilder.startSpan()).thenReturn(span);


            Span result = gameTracer.startGameSpan(gameId, "test-operation");


            assertThat(result).isNotNull();
            verify(tracer).spanBuilder("game.test-operation");
            verify(spanBuilder).setSpanKind(SpanKind.INTERNAL);
            // Uses AttributeKey<String> for game.id
            verify(spanBuilder).setAttribute(any(AttributeKey.class), eq(gameId.toString()));
            verify(spanBuilder).startSpan();
        }

        @Test
        @DisplayName("Should create spans with different operation names")
        void shouldCreateSpansWithDifferentOperations() {
            
            UUID gameId = UUID.randomUUID();
            setupSpanBuilderMock();

            
            gameTracer.startGameSpan(gameId, "create");
            gameTracer.startGameSpan(gameId, "player-action");
            gameTracer.startGameSpan(gameId, "showdown");

            
            verify(tracer).spanBuilder("game.create");
            verify(tracer).spanBuilder("game.player-action");
            verify(tracer).spanBuilder("game.showdown");
        }
    }

    @Nested
    @DisplayName("Event Recording Tests")
    class EventRecordingTests {

        @Test
        @DisplayName("Should record player action event")
        void shouldRecordPlayerAction() {
            
            when(span.isRecording()).thenReturn(true);
            UUID playerId = UUID.randomUUID();

            
            gameTracer.recordPlayerAction(span, playerId, "TestPlayer", "RAISE", 100);

            
            verify(span).addEvent(eq("player_action"), any(Attributes.class));
        }

        @Test
        @DisplayName("Should not record player action when span is not recording")
        void shouldNotRecordPlayerActionWhenNotRecording() {
            
            when(span.isRecording()).thenReturn(false);

            
            gameTracer.recordPlayerAction(span, UUID.randomUUID(), "TestPlayer", "FOLD", 0);

            
            verify(span, never()).addEvent(anyString(), any(Attributes.class));
        }

        @Test
        @DisplayName("Should handle null span gracefully")
        void shouldHandleNullSpan() {
            
            gameTracer.recordPlayerAction(null, UUID.randomUUID(), "Test", "FOLD", 0);
            gameTracer.recordPhaseTransition(null, "PRE_FLOP", "FLOP");
            gameTracer.recordBotDecision(null, UUID.randomUUID(), "CALL", "reason", 100);
        }

        @Test
        @DisplayName("Should record phase transition")
        @SuppressWarnings("unchecked")
        void shouldRecordPhaseTransition() {

            when(span.isRecording()).thenReturn(true);


            gameTracer.recordPhaseTransition(span, "PRE_FLOP", "FLOP");


            verify(span).addEvent(eq("phase_transition"), any(Attributes.class));
            // Uses AttributeKey<String> for game.phase
            verify(span).setAttribute(any(AttributeKey.class), eq("FLOP"));
        }

        @Test
        @DisplayName("Should record bot decision with timing")
        void shouldRecordBotDecision() {
            
            when(span.isRecording()).thenReturn(true);
            UUID botId = UUID.randomUUID();

            
            gameTracer.recordBotDecision(span, botId, "RAISE", "Strong hand detected", 150L);

            
            verify(span).addEvent(eq("bot_decision"), any(Attributes.class));
        }

        @Test
        @DisplayName("Should record hand completion")
        void shouldRecordHandCompletion() {
            
            when(span.isRecording()).thenReturn(true);

            
            gameTracer.recordHandCompletion(span, 5L, "Player1", "Flush", 1000);

            
            verify(span).addEvent(eq("hand_completed"), any(Attributes.class));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should record error with exception details")
        void shouldRecordError() {
            
            when(span.isRecording()).thenReturn(true);
            RuntimeException error = new RuntimeException("Test error");

            
            gameTracer.recordError(span, error);

            
            verify(span).setStatus(StatusCode.ERROR, "Test error");
            verify(span).recordException(error);
        }

        @Test
        @DisplayName("Should not record error when span is not recording")
        void shouldNotRecordErrorWhenNotRecording() {
            
            when(span.isRecording()).thenReturn(false);

            
            gameTracer.recordError(span, new RuntimeException("Test"));

            
            verify(span, never()).setStatus(any(), anyString());
            verify(span, never()).recordException(any());
        }
    }

    @Nested
    @DisplayName("Traced Operation Tests")
    class TracedOperationTests {

        @Test
        @DisplayName("Should execute traced operation and return result")
        void shouldExecuteTracedOperationWithResult() {
            
            UUID gameId = UUID.randomUUID();
            setupSpanBuilderMock();
            when(span.makeCurrent()).thenReturn(scope);

            
            String result = gameTracer.traceOperation(gameId, "test", () -> "success");

            
            assertThat(result).isEqualTo("success");
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }

        @Test
        @DisplayName("Should record error and rethrow exception in traced operation")
        void shouldRecordErrorAndRethrowInTracedOperation() {
            
            UUID gameId = UUID.randomUUID();
            setupSpanBuilderMock();
            when(span.makeCurrent()).thenReturn(scope);
            when(span.isRecording()).thenReturn(true);

            
            assertThatThrownBy(() -> 
                gameTracer.traceOperation(gameId, "test", () -> {
                    throw new RuntimeException("Test error");
                })
            ).isInstanceOf(RuntimeException.class)
             .hasMessage("Test error");

            verify(span).setStatus(StatusCode.ERROR, "Test error");
            verify(span).recordException(any(RuntimeException.class));
            verify(span).end();
        }

        @Test
        @DisplayName("Should execute void traced operation")
        void shouldExecuteVoidTracedOperation() {
            
            UUID gameId = UUID.randomUUID();
            setupSpanBuilderMock();
            when(span.makeCurrent()).thenReturn(scope);
            AtomicBoolean executed = new AtomicBoolean(false);

            
            gameTracer.traceOperation(gameId, "test", () -> executed.set(true));

            
            assertThat(executed).isTrue();
            verify(span).end();
        }
    }

    @Nested
    @DisplayName("Game State Attributes Tests")
    class GameStateAttributesTests {

        @Test
        @DisplayName("Should add game state attributes to span")
        @SuppressWarnings("unchecked")
        void shouldAddGameStateAttributes() {

            when(span.isRecording()).thenReturn(true);
            when(span.setAttribute(any(AttributeKey.class), any())).thenReturn(span);
            when(span.setAttribute(anyString(), anyLong())).thenReturn(span);


            gameTracer.addGameStateAttributes(span, "FLOP", 500, 4);


            // Uses AttributeKey<String> for game.phase
            verify(span).setAttribute(any(AttributeKey.class), eq("FLOP"));
            // Uses AttributeKey<Long> for pot.size (int is auto-cast to long)
            verify(span, atLeast(1)).setAttribute(any(AttributeKey.class), any());
            // Uses String key for active_players, passed as long
            verify(span).setAttribute(eq("game.active_players"), eq(4L));
        }

        @Test
        @DisplayName("Should not add attributes when span is not recording")
        @SuppressWarnings("unchecked")
        void shouldNotAddAttributesWhenNotRecording() {

            when(span.isRecording()).thenReturn(false);


            gameTracer.addGameStateAttributes(span, "FLOP", 500, 4);


            verify(span, never()).setAttribute(any(AttributeKey.class), any());
            verify(span, never()).setAttribute(anyString(), anyLong());
        }
    }

    
    @SuppressWarnings("unchecked")
    private void setupSpanBuilderMock() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(any(AttributeKey.class), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
    }
}
