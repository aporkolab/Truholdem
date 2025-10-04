package com.truholdem.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;


@Component
@ConditionalOnBean(Meter.class)
public class GameMetrics {

    private static final Logger logger = LoggerFactory.getLogger(GameMetrics.class);

    
    private static final AttributeKey<String> ACTION_TYPE_KEY = AttributeKey.stringKey("action.type");
    private static final AttributeKey<String> BOT_TYPE_KEY = AttributeKey.stringKey("bot.type");
    private static final AttributeKey<String> GAME_TYPE_KEY = AttributeKey.stringKey("game.type");
    private static final AttributeKey<String> PHASE_KEY = AttributeKey.stringKey("phase");
    private static final AttributeKey<Boolean> IS_BOT_KEY = AttributeKey.booleanKey("is_bot");

    private final Meter meter;

    
    private LongCounter gamesStartedCounter;
    private LongCounter gamesCompletedCounter;
    private LongCounter handsPlayedCounter;
    private LongCounter playerActionsCounter;
    private LongCounter botDecisionsCounter;
    private LongCounter tournamentsCreatedCounter;
    private LongCounter tournamentsCompletedCounter;

    
    private DoubleHistogram handDurationHistogram;
    private DoubleHistogram botDecisionTimeHistogram;
    private DoubleHistogram potSizeHistogram;
    private DoubleHistogram actionProcessingTimeHistogram;
    private DoubleHistogram gameCreationTimeHistogram;
    private DoubleHistogram tournamentDurationHistogram;

    
    private LongUpDownCounter activeGamesCounter;
    private LongUpDownCounter activeTournamentsCounter;
    private LongUpDownCounter activePlayersCounter;
    private LongUpDownCounter tournamentPlayersRegisteredCounter;
    private LongUpDownCounter tournamentPlayersRemainingCounter;

    
    private LongCounter tournamentEliminationsCounter;

    
    private final AtomicLong totalActiveGames = new AtomicLong(0);
    private final AtomicLong totalActiveTournaments = new AtomicLong(0);
    private final AtomicLong totalActivePlayers = new AtomicLong(0);
    private final AtomicLong largestPotSeen = new AtomicLong(0);
    private final AtomicLong totalTournamentPlayersRegistered = new AtomicLong(0);
    private final AtomicLong totalTournamentPlayersRemaining = new AtomicLong(0);

    public GameMetrics(Meter meter) {
        this.meter = meter;
    }

    @PostConstruct
    public void initializeMetrics() {
        logger.info("Initializing OpenTelemetry game metrics");

        
        gamesStartedCounter = meter.counterBuilder("truholdem.games.started")
                .setDescription("Total number of games started")
                .setUnit("{games}")
                .build();

        gamesCompletedCounter = meter.counterBuilder("truholdem.games.completed")
                .setDescription("Total number of games completed")
                .setUnit("{games}")
                .build();

        handsPlayedCounter = meter.counterBuilder("truholdem.hands.played")
                .setDescription("Total number of hands played")
                .setUnit("{hands}")
                .build();

        playerActionsCounter = meter.counterBuilder("truholdem.player.actions")
                .setDescription("Total number of player actions")
                .setUnit("{actions}")
                .build();

        botDecisionsCounter = meter.counterBuilder("truholdem.bot.decisions")
                .setDescription("Total number of bot decisions made")
                .setUnit("{decisions}")
                .build();

        tournamentsCreatedCounter = meter.counterBuilder("truholdem.tournaments.created")
                .setDescription("Total number of tournaments created")
                .setUnit("{tournaments}")
                .build();

        tournamentsCompletedCounter = meter.counterBuilder("truholdem.tournaments.completed")
                .setDescription("Total number of tournaments completed")
                .setUnit("{tournaments}")
                .build();

        
        handDurationHistogram = meter.histogramBuilder("truholdem.hand.duration")
                .setDescription("Duration of poker hands")
                .setUnit("ms")
                .build();

        botDecisionTimeHistogram = meter.histogramBuilder("truholdem.bot.decision_time")
                .setDescription("Time taken for bot decisions")
                .setUnit("ms")
                .build();

        potSizeHistogram = meter.histogramBuilder("truholdem.pot.size")
                .setDescription("Distribution of pot sizes")
                .setUnit("{chips}")
                .build();

        actionProcessingTimeHistogram = meter.histogramBuilder("truholdem.action.processing_time")
                .setDescription("Time to process player actions")
                .setUnit("ms")
                .build();

        gameCreationTimeHistogram = meter.histogramBuilder("truholdem.game.creation_time")
                .setDescription("Time to create a new game")
                .setUnit("ms")
                .build();

        tournamentDurationHistogram = meter.histogramBuilder("truholdem.tournament.duration")
                .setDescription("Duration of completed tournaments")
                .setUnit("minutes")
                .build();

        
        activeGamesCounter = meter.upDownCounterBuilder("truholdem.games.active")
                .setDescription("Number of currently active games")
                .setUnit("{games}")
                .build();

        activeTournamentsCounter = meter.upDownCounterBuilder("truholdem.tournaments.active")
                .setDescription("Number of currently active tournaments")
                .setUnit("{tournaments}")
                .build();

        activePlayersCounter = meter.upDownCounterBuilder("truholdem.players.active")
                .setDescription("Number of currently active players")
                .setUnit("{players}")
                .build();

        tournamentPlayersRegisteredCounter = meter.upDownCounterBuilder("truholdem.tournament.players.registered")
                .setDescription("Number of players registered in active tournaments")
                .setUnit("{players}")
                .build();

        tournamentPlayersRemainingCounter = meter.upDownCounterBuilder("truholdem.tournament.players.remaining")
                .setDescription("Number of players still active in tournaments")
                .setUnit("{players}")
                .build();

        tournamentEliminationsCounter = meter.counterBuilder("truholdem.tournament.eliminations")
                .setDescription("Total number of tournament eliminations")
                .setUnit("{eliminations}")
                .build();

        
        meter.gaugeBuilder("truholdem.games.total_active")
                .setDescription("Snapshot of total active games")
                .setUnit("{games}")
                .buildWithCallback(measurement -> 
                    measurement.record(totalActiveGames.get()));

        meter.gaugeBuilder("truholdem.tournaments.total_active")
                .setDescription("Snapshot of total active tournaments")
                .setUnit("{tournaments}")
                .buildWithCallback(measurement -> 
                    measurement.record(totalActiveTournaments.get()));

        meter.gaugeBuilder("truholdem.pot.largest_seen")
                .setDescription("Largest pot size seen")
                .setUnit("{chips}")
                .buildWithCallback(measurement -> 
                    measurement.record(largestPotSeen.get()));

        meter.gaugeBuilder("truholdem.tournament.players.registered.total")
                .setDescription("Total registered players across all active tournaments")
                .setUnit("{players}")
                .buildWithCallback(measurement -> 
                    measurement.record(totalTournamentPlayersRegistered.get()));

        meter.gaugeBuilder("truholdem.tournament.players.remaining.total")
                .setDescription("Total remaining players across all active tournaments")
                .setUnit("{players}")
                .buildWithCallback(measurement -> 
                    measurement.record(totalTournamentPlayersRemaining.get()));

        logger.info("OpenTelemetry game metrics initialized successfully");
    }

    

    
    public void recordGameStarted(String gameType, int playerCount) {
        gamesStartedCounter.add(1, Attributes.of(
                GAME_TYPE_KEY, gameType,
                AttributeKey.longKey("player_count"), (long) playerCount
        ));
        activeGamesCounter.add(1);
        totalActiveGames.incrementAndGet();
        logger.debug("Recorded game started: type={}, players={}", gameType, playerCount);
    }

    
    public void recordGameCompleted(String gameType, int handsPlayed) {
        gamesCompletedCounter.add(1, Attributes.of(
                GAME_TYPE_KEY, gameType,
                AttributeKey.longKey("hands_played"), (long) handsPlayed
        ));
        activeGamesCounter.add(-1);
        totalActiveGames.decrementAndGet();
        logger.debug("Recorded game completed: type={}, hands={}", gameType, handsPlayed);
    }

    

    
    public void recordHandCompleted(Duration duration, int potSize, String phase) {
        handsPlayedCounter.add(1, Attributes.of(PHASE_KEY, phase));
        handDurationHistogram.record(duration.toMillis(), Attributes.of(PHASE_KEY, phase));
        potSizeHistogram.record(potSize);
        
        
        largestPotSeen.updateAndGet(current -> Math.max(current, potSize));
        
        logger.debug("Recorded hand completed: duration={}ms, pot={}, phase={}", 
                duration.toMillis(), potSize, phase);
    }

    

    
    public void recordPlayerAction(String actionType, boolean isBot) {
        playerActionsCounter.add(1, Attributes.of(
                ACTION_TYPE_KEY, actionType,
                IS_BOT_KEY, isBot
        ));
        logger.debug("Recorded player action: type={}, isBot={}", actionType, isBot);
    }

    
    public void recordActionProcessingTime(long processingTimeMs, String actionType) {
        actionProcessingTimeHistogram.record(processingTimeMs, 
                Attributes.of(ACTION_TYPE_KEY, actionType));
    }

    

    
    public void recordBotDecision(String botType, long decisionTimeMs, String action) {
        botDecisionsCounter.add(1, Attributes.of(
                BOT_TYPE_KEY, botType,
                ACTION_TYPE_KEY, action
        ));
        botDecisionTimeHistogram.record(decisionTimeMs, Attributes.of(BOT_TYPE_KEY, botType));
        logger.debug("Recorded bot decision: type={}, time={}ms, action={}", 
                botType, decisionTimeMs, action);
    }

    

    
    public void recordTournamentCreated(String tournamentType, int buyIn) {
        tournamentsCreatedCounter.add(1, Attributes.of(
                AttributeKey.stringKey("tournament.type"), tournamentType,
                AttributeKey.longKey("buy_in"), (long) buyIn
        ));
        activeTournamentsCounter.add(1);
        totalActiveTournaments.incrementAndGet();
    }

    
    public void recordTournamentCompleted(String tournamentType, int totalPlayers) {
        tournamentsCompletedCounter.add(1, Attributes.of(
                AttributeKey.stringKey("tournament.type"), tournamentType,
                AttributeKey.longKey("total_players"), (long) totalPlayers
        ));
        activeTournamentsCounter.add(-1);
        totalActiveTournaments.decrementAndGet();
    }

    
    public void recordTournamentCompleted(String tournamentType, int totalPlayers, Duration duration) {
        recordTournamentCompleted(tournamentType, totalPlayers);
        tournamentDurationHistogram.record(duration.toMinutes(), Attributes.of(
                AttributeKey.stringKey("tournament.type"), tournamentType
        ));
        logger.debug("Recorded tournament completed: type={}, players={}, duration={}min", 
                tournamentType, totalPlayers, duration.toMinutes());
    }

    
    public void recordTournamentPlayerRegistered(String tournamentType) {
        tournamentPlayersRegisteredCounter.add(1);
        tournamentPlayersRemainingCounter.add(1);
        totalTournamentPlayersRegistered.incrementAndGet();
        totalTournamentPlayersRemaining.incrementAndGet();
        logger.debug("Recorded tournament player registered: type={}", tournamentType);
    }

    
    public void recordTournamentElimination(String tournamentType, int position) {
        tournamentEliminationsCounter.add(1, Attributes.of(
                AttributeKey.stringKey("tournament.type"), tournamentType,
                AttributeKey.longKey("finish_position"), (long) position
        ));
        tournamentPlayersRemainingCounter.add(-1);
        totalTournamentPlayersRemaining.decrementAndGet();
        logger.debug("Recorded tournament elimination: type={}, position={}", tournamentType, position);
    }

    
    public void recordTournamentEnded(int registeredPlayers) {
        tournamentPlayersRegisteredCounter.add(-registeredPlayers);
        totalTournamentPlayersRegistered.addAndGet(-registeredPlayers);
        
    }

    

    
    public void recordPlayerJoined() {
        activePlayersCounter.add(1);
        totalActivePlayers.incrementAndGet();
    }

    
    public void recordPlayerLeft() {
        activePlayersCounter.add(-1);
        totalActivePlayers.decrementAndGet();
    }

    

    
    public void recordGameCreationTime(long creationTimeMs, int playerCount) {
        gameCreationTimeHistogram.record(creationTimeMs, Attributes.of(
                AttributeKey.longKey("player_count"), (long) playerCount
        ));
    }

    

    
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
                totalActiveGames.get(),
                totalActiveTournaments.get(),
                totalActivePlayers.get(),
                largestPotSeen.get(),
                totalTournamentPlayersRegistered.get(),
                totalTournamentPlayersRemaining.get()
        );
    }

    
    public record MetricsSnapshot(
            long activeGames,
            long activeTournaments,
            long activePlayers,
            long largestPot,
            long tournamentPlayersRegistered,
            long tournamentPlayersRemaining
    ) {}
}
