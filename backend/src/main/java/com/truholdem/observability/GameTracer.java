package com.truholdem.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;


@Component
@ConditionalOnBean(Tracer.class)
public class GameTracer {

    private static final Logger logger = LoggerFactory.getLogger(GameTracer.class);

    
    private static final AttributeKey<String> GAME_ID = AttributeKey.stringKey("game.id");
    private static final AttributeKey<String> PLAYER_ID = AttributeKey.stringKey("player.id");
    private static final AttributeKey<String> PLAYER_NAME = AttributeKey.stringKey("player.name");
    private static final AttributeKey<String> ACTION_TYPE = AttributeKey.stringKey("action.type");
    private static final AttributeKey<Long> ACTION_AMOUNT = AttributeKey.longKey("action.amount");
    private static final AttributeKey<String> GAME_PHASE = AttributeKey.stringKey("game.phase");
    private static final AttributeKey<String> BOT_TYPE = AttributeKey.stringKey("bot.type");
    private static final AttributeKey<String> BOT_REASONING = AttributeKey.stringKey("bot.reasoning");
    private static final AttributeKey<Long> THINKING_TIME_MS = AttributeKey.longKey("bot.thinking_time_ms");
    private static final AttributeKey<Long> POT_SIZE = AttributeKey.longKey("pot.size");
    private static final AttributeKey<Long> HAND_NUMBER = AttributeKey.longKey("hand.number");
    private static final AttributeKey<String> WINNER_NAME = AttributeKey.stringKey("winner.name");
    private static final AttributeKey<String> WINNING_HAND = AttributeKey.stringKey("winner.hand");

    private final Tracer tracer;

    public GameTracer(Tracer tracer) {
        this.tracer = tracer;
        logger.info("GameTracer initialized with OpenTelemetry tracer");
    }

    
    public Span startGameSpan(UUID gameId, String operation) {
        Span span = tracer.spanBuilder("game." + operation)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(GAME_ID, gameId.toString())
                .startSpan();
        
        logger.debug("Started span for game {} operation: {}", gameId, operation);
        return span;
    }

    
    public void recordPlayerAction(Span span, UUID playerId, String playerName, String action, int amount) {
        if (span == null || !span.isRecording()) {
            return;
        }

        span.addEvent("player_action", Attributes.builder()
                .put(PLAYER_ID, playerId.toString())
                .put(PLAYER_NAME, playerName)
                .put(ACTION_TYPE, action)
                .put(ACTION_AMOUNT, amount)
                .build());

        logger.debug("Recorded player action: {} by {} amount {}", action, playerName, amount);
    }

    
    public void recordPhaseTransition(Span span, String fromPhase, String toPhase) {
        if (span == null || !span.isRecording()) {
            return;
        }

        span.addEvent("phase_transition", Attributes.builder()
                .put("from_phase", fromPhase)
                .put("to_phase", toPhase)
                .build());

        span.setAttribute(GAME_PHASE, toPhase);
        logger.debug("Recorded phase transition: {} -> {}", fromPhase, toPhase);
    }

    
    public void recordBotDecision(Span span, UUID botId, String decision, String reasoning, long thinkingTimeMs) {
        if (span == null || !span.isRecording()) {
            return;
        }

        span.addEvent("bot_decision", Attributes.builder()
                .put(PLAYER_ID, botId.toString())
                .put(ACTION_TYPE, decision)
                .put(BOT_REASONING, reasoning != null ? reasoning : "unknown")
                .put(THINKING_TIME_MS, thinkingTimeMs)
                .build());

        logger.debug("Recorded bot decision: {} ({}ms) - {}", decision, thinkingTimeMs, reasoning);
    }

    
    public void recordHandCompletion(Span span, long handNumber, String winnerName, 
                                     String winningHand, int potSize) {
        if (span == null || !span.isRecording()) {
            return;
        }

        span.addEvent("hand_completed", Attributes.builder()
                .put(HAND_NUMBER, handNumber)
                .put(WINNER_NAME, winnerName)
                .put(WINNING_HAND, winningHand != null ? winningHand : "fold")
                .put(POT_SIZE, potSize)
                .build());

        logger.debug("Recorded hand {} completion: {} wins {} with {}", 
                handNumber, winnerName, potSize, winningHand);
    }

    
    public void recordError(Span span, Throwable error) {
        if (span == null || !span.isRecording()) {
            return;
        }

        span.setStatus(StatusCode.ERROR, error.getMessage());
        span.recordException(error);
        logger.debug("Recorded error in span: {}", error.getMessage());
    }

    
    public <T> T traceOperation(UUID gameId, String operation, Supplier<T> supplier) {
        Span span = startGameSpan(gameId, operation);
        
        try (Scope scope = span.makeCurrent()) {
            T result = supplier.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            recordError(span, e);
            throw e;
        } finally {
            span.end();
        }
    }

    
    public void traceOperation(UUID gameId, String operation, Runnable runnable) {
        traceOperation(gameId, operation, () -> {
            runnable.run();
            return null;
        });
    }

    
    public Span createChildSpan(Span parentSpan, String operation) {
        return tracer.spanBuilder(operation)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
    }

    
    public Span currentSpan() {
        return Span.current();
    }

    
    public void addGameStateAttributes(Span span, String phase, int potSize, int activePlayers) {
        if (span == null || !span.isRecording()) {
            return;
        }

        span.setAttribute(GAME_PHASE, phase);
        span.setAttribute(POT_SIZE, potSize);
        span.setAttribute("game.active_players", activePlayers);
    }
}
