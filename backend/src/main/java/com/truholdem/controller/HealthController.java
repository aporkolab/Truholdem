package com.truholdem.controller;

import com.truholdem.config.GameServiceHealthIndicator;
import com.truholdem.service.HealthService;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final HealthEndpoint healthEndpoint;
    private final GameServiceHealthIndicator gameServiceHealth;
    private final DataSource dataSource;
    private final Optional<RedisConnectionFactory> redisConnectionFactory;
    private final HealthService healthService;
    private final MeterRegistry meterRegistry;
    
    private final Instant startupTime;
    
    @Value("${spring.application.name:truholdem}")
    private String applicationName;
    
    @Value("${app.version:2.0.0}")
    private String appVersion;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public HealthController(
            HealthEndpoint healthEndpoint,
            GameServiceHealthIndicator gameServiceHealth,
            DataSource dataSource,
            Optional<RedisConnectionFactory> redisConnectionFactory,
            HealthService healthService,
            MeterRegistry meterRegistry) {
        this.healthEndpoint = healthEndpoint;
        this.gameServiceHealth = gameServiceHealth;
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.healthService = healthService;
        this.meterRegistry = meterRegistry;
        this.startupTime = Instant.now();
    }

    
    @GetMapping("/live")
    @Operation(summary = "Liveness probe", description = "Simple liveness check for container orchestration")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());
        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/ready")
    @Operation(summary = "Readiness probe", description = "Checks if application is ready to serve traffic")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean allHealthy = true;

        
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(5);
            checks.put("database", Map.of("status", "UP", "type", "PostgreSQL"));
        } catch (Exception e) {
            checks.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }

        
        redisConnectionFactory.ifPresent(factory -> {
            try {
                factory.getConnection().ping();
                checks.put("redis", Map.of("status", "UP"));
            } catch (Exception e) {
                checks.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            }
        });

        response.put("status", allHealthy ? "UP" : "DOWN");
        response.put("checks", checks);
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());

        return allHealthy ? ResponseEntity.ok(response) : ResponseEntity.status(503).body(response);
    }

    
    @GetMapping("/detailed")
    @Operation(summary = "Detailed health", description = "Comprehensive health information for monitoring")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        
        response.put("application", applicationName);
        response.put("version", appVersion);
        response.put("profile", activeProfile);
        response.put("status", healthEndpoint.health().getStatus().equals(Status.UP) ? "UP" : "DOWN");
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());

        
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        
        Map<String, Object> runtimeInfo = new LinkedHashMap<>();
        Duration uptime = Duration.ofMillis(runtime.getUptime());
        runtimeInfo.put("uptimeSeconds", uptime.getSeconds());
        runtimeInfo.put("uptimeFormatted", formatDuration(uptime));
        runtimeInfo.put("startTime", LocalDateTime.ofInstant(startupTime, ZoneOffset.UTC).toString());
        runtimeInfo.put("javaVersion", runtime.getSpecVersion());
        runtimeInfo.put("vmName", runtime.getVmName());
        response.put("runtime", runtimeInfo);

        
        Map<String, Object> memoryInfo = new LinkedHashMap<>();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        memoryInfo.put("heapUsedMB", heapUsed / (1024 * 1024));
        memoryInfo.put("heapMaxMB", heapMax / (1024 * 1024));
        memoryInfo.put("heapUsagePercent", Math.round((double) heapUsed / heapMax * 100));
        memoryInfo.put("nonHeapUsedMB", memory.getNonHeapMemoryUsage().getUsed() / (1024 * 1024));
        response.put("memory", memoryInfo);

        
        Map<String, Object> gameStats = new LinkedHashMap<>();
        gameStats.put("activeGames", healthService.getActiveGamesCount());
        gameStats.put("activeTournaments", healthService.getActiveTournamentsCount());
        gameStats.put("totalUsers", healthService.getTotalUsersCount());
        gameStats.put("gamesCreatedSinceStartup", gameServiceHealth.getGamesCreated());
        gameStats.put("actionsProcessedSinceStartup", gameServiceHealth.getActionsProcessed());
        response.put("gameService", gameStats);

        
        Map<String, Object> dependencies = new LinkedHashMap<>();
        
        
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            dependencies.put("database", Map.of(
                "status", valid ? "UP" : "DOWN",
                "type", "PostgreSQL",
                "catalog", conn.getCatalog()
            ));
        } catch (Exception e) {
            dependencies.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        
        redisConnectionFactory.ifPresent(factory -> {
            try {
                String pong = factory.getConnection().ping();
                dependencies.put("redis", Map.of("status", "UP", "response", pong));
            } catch (Exception e) {
                dependencies.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            }
        });

        response.put("dependencies", dependencies);

        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/metrics/summary")
    @Operation(summary = "Metrics summary", description = "Quick overview of key metrics")
    public ResponseEntity<Map<String, Object>> metricsSummary() {
        Map<String, Object> response = new LinkedHashMap<>();
        
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());
        
        
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        try {
            
            meterRegistry.find("http.server.requests").timers().forEach(timer -> {
                String uri = timer.getId().getTag("uri");
                if (uri != null && !uri.contains("actuator")) {
                    metrics.put("http_requests_" + uri.replace("/", "_"), Map.of(
                        "count", timer.count(),
                        "meanMs", Math.round(timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
                    ));
                }
            });
            
            
            meterRegistry.find("jvm.threads.live").gauges().forEach(gauge -> {
                metrics.put("jvm_threads_live", (int) gauge.value());
            });
            
        } catch (Exception e) {
            metrics.put("error", "Could not retrieve all metrics: " + e.getMessage());
        }
        
        response.put("metrics", metrics);
        
        return ResponseEntity.ok(response);
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
