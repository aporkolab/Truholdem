package com.truholdem.config;

import com.truholdem.repository.GameRepository;
import com.truholdem.repository.PlayerStatisticsRepository;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;


@Component
public class GameServiceHealthIndicator implements HealthIndicator {

    private final GameRepository gameRepository;
    private final PlayerStatisticsRepository statsRepository;
    private final long startTime;
    private final AtomicLong gamesCreated = new AtomicLong(0);
    private final AtomicLong actionsProcessed = new AtomicLong(0);

    public GameServiceHealthIndicator(
            GameRepository gameRepository,
            PlayerStatisticsRepository statsRepository) {
        this.gameRepository = gameRepository;
        this.statsRepository = statsRepository;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public Health health() {
        try {
            long activeGames = gameRepository.count();
            long totalStats = statsRepository.count();
            long uptimeMs = System.currentTimeMillis() - startTime;

            return Health.up()
                    .withDetail("activeGames", activeGames)
                    .withDetail("gamesCreated", gamesCreated.get())
                    .withDetail("actionsProcessed", actionsProcessed.get())
                    .withDetail("playersTracked", totalStats)
                    .withDetail("uptimeSeconds", uptimeMs / 1000)
                    .withDetail("status", "Game service is operational")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Game service is experiencing issues")
                    .build();
        }
    }

    
    public void recordGameCreated() {
        gamesCreated.incrementAndGet();
    }

    
    public void recordActionProcessed() {
        actionsProcessed.incrementAndGet();
    }

    public long getGamesCreated() {
        return gamesCreated.get();
    }

    public long getActionsProcessed() {
        return actionsProcessed.get();
    }
}
