package com.truholdem.config;

import com.truholdem.service.AdvancedBotAIService;
import com.truholdem.service.GameMetricsService;
import com.truholdem.service.GameNotificationService;
import com.truholdem.service.HandAnalysisService;
import com.truholdem.service.HandEvaluator;
import com.truholdem.service.HandHistoryService;
import com.truholdem.service.PlayerStatisticsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
        return mock(SimpMessagingTemplate.class);
    }

    @Bean
    @Primary
    public GameNotificationService gameNotificationService() {
        return mock(GameNotificationService.class);
    }

    @Bean
    @Primary
    public HandHistoryService handHistoryService() {
        return mock(HandHistoryService.class);
    }

    @Bean
    @Primary
    public PlayerStatisticsService playerStatisticsService() {
        return mock(PlayerStatisticsService.class);
    }

    @Bean
    @Primary
    public AdvancedBotAIService advancedBotAIService(HandEvaluator handEvaluator) {
        return new AdvancedBotAIService(handEvaluator);
    }

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    public GameMetricsService gameMetricsService(MeterRegistry meterRegistry) {
        return new GameMetricsService(meterRegistry);
    }

    @Bean
    @Primary
    public HandEvaluator handEvaluator() {
        return new HandEvaluator();
    }

    @Bean
    @Primary
    public HandAnalysisService handAnalysisService(HandEvaluator handEvaluator) {
        return new HandAnalysisService(handEvaluator);
    }
}