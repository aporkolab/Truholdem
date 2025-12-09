package com.truholdem.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;


@Service
public class GameMetricsService {

    private final MeterRegistry meterRegistry;

    
    private final Counter gamesCreated;
    private final Counter gamesCompleted;
    private final Counter handsPlayed;
    private final Counter playerActions;
    private final Counter showdowns;
    private final Counter folds;

    
    private final AtomicInteger activeGames;
    private final AtomicInteger activePlayers;

    
    private final Timer gameCreationTimer;
    private final Timer actionProcessingTimer;
    private final Timer showdownTimer;
    private final Timer botDecisionTimer;

    public GameMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        
        this.gamesCreated = Counter.builder("poker.games.created")
                .description("Total number of games created")
                .register(meterRegistry);

        this.gamesCompleted = Counter.builder("poker.games.completed")
                .description("Total number of games completed")
                .register(meterRegistry);

        this.handsPlayed = Counter.builder("poker.hands.played")
                .description("Total number of hands played")
                .register(meterRegistry);

        this.playerActions = Counter.builder("poker.player.actions")
                .description("Total number of player actions")
                .tag("type", "all")
                .register(meterRegistry);

        this.showdowns = Counter.builder("poker.showdowns")
                .description("Total number of showdowns")
                .register(meterRegistry);

        this.folds = Counter.builder("poker.folds")
                .description("Total number of folds")
                .register(meterRegistry);

        
        this.activeGames = new AtomicInteger(0);
        Gauge.builder("poker.games.active", activeGames, AtomicInteger::get)
                .description("Current number of active games")
                .register(meterRegistry);

        this.activePlayers = new AtomicInteger(0);
        Gauge.builder("poker.players.active", activePlayers, AtomicInteger::get)
                .description("Current number of active players")
                .register(meterRegistry);

        
        this.gameCreationTimer = Timer.builder("poker.game.creation.time")
                .description("Time to create a new game")
                .register(meterRegistry);

        this.actionProcessingTimer = Timer.builder("poker.action.processing.time")
                .description("Time to process a player action")
                .register(meterRegistry);

        this.showdownTimer = Timer.builder("poker.showdown.time")
                .description("Time to resolve a showdown")
                .register(meterRegistry);

        this.botDecisionTimer = Timer.builder("poker.bot.decision.time")
                .description("Time for bot AI to make a decision")
                .register(meterRegistry);
    }

    
    public void incrementGamesCreated() {
        gamesCreated.increment();
        activeGames.incrementAndGet();
    }

    public void incrementGamesCompleted() {
        gamesCompleted.increment();
        activeGames.decrementAndGet();
    }

    public void incrementHandsPlayed() {
        handsPlayed.increment();
    }

    public void recordPlayerAction(String actionType) {
        playerActions.increment();
        Counter.builder("poker.player.actions")
                .tag("type", actionType.toLowerCase())
                .register(meterRegistry)
                .increment();
    }

    public void incrementShowdowns() {
        showdowns.increment();
    }

    public void incrementFolds() {
        folds.increment();
    }

    public void setActivePlayers(int count) {
        activePlayers.set(count);
    }

    
    public <T> T timeGameCreation(Supplier<T> operation) {
        return gameCreationTimer.record(operation);
    }

    public <T> T timeActionProcessing(Supplier<T> operation) {
        return actionProcessingTimer.record(operation);
    }

    public <T> T timeShowdown(Supplier<T> operation) {
        return showdownTimer.record(operation);
    }

    public void recordBotDecisionTime(long durationMs) {
        botDecisionTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    
    public void recordHandOutcome(String handType) {
        Counter.builder("poker.hand.outcomes")
                .tag("hand_type", handType)
                .description("Distribution of winning hand types")
                .register(meterRegistry)
                .increment();
    }

    
    public void recordPotSize(int amount) {
        meterRegistry.summary("poker.pot.size")
                .record(amount);
    }
}
