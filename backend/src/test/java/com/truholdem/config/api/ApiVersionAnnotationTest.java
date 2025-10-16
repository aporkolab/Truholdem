package com.truholdem.config.api;

import com.truholdem.controller.AchievementController;
import com.truholdem.controller.AuthController;
import com.truholdem.controller.HandAnalysisController;
import com.truholdem.controller.HandHistoryController;
import com.truholdem.controller.PokerGameController;
import com.truholdem.controller.StatisticsController;
import com.truholdem.controller.TournamentController;
import com.truholdem.controller.UserController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for API versioning annotations.
 * These tests do not require Spring context.
 */
@DisplayName("API Version Annotation Tests")
class ApiVersionAnnotationTest {

    @Test
    @DisplayName("PokerGameController should have @ApiV1Config annotation")
    void pokerGameControllerShouldHaveApiV1Annotation() {
        assertThat(PokerGameController.class.isAnnotationPresent(ApiV1Config.class))
            .as("PokerGameController should be annotated with @ApiV1Config")
            .isTrue();
    }

    @Test
    @DisplayName("HandAnalysisController should have @ApiV1Config annotation")
    void handAnalysisControllerShouldHaveApiV1Annotation() {
        assertThat(HandAnalysisController.class.isAnnotationPresent(ApiV1Config.class))
            .as("HandAnalysisController should be annotated with @ApiV1Config")
            .isTrue();
    }

    @Test
    @DisplayName("TournamentController should have @ApiV1Config annotation")
    void tournamentControllerShouldHaveApiV1Annotation() {
        assertThat(TournamentController.class.isAnnotationPresent(ApiV1Config.class))
            .as("TournamentController should be annotated with @ApiV1Config")
            .isTrue();
    }

    @Test
    @DisplayName("AuthController should have @RequestMapping annotation with /auth path")
    void authControllerShouldHaveRequestMappingAnnotation() {
        RequestMapping requestMapping = AuthController.class.getAnnotation(RequestMapping.class);
        assertThat(requestMapping)
            .as("AuthController should be annotated with @RequestMapping")
            .isNotNull();
        assertThat(requestMapping.value())
            .as("AuthController should have path /auth")
            .contains("/auth");
    }

    @Test
    @DisplayName("UserController should have @ApiV1Config annotation")
    void userControllerShouldHaveApiV1Annotation() {
        assertThat(UserController.class.isAnnotationPresent(ApiV1Config.class))
            .as("UserController should be annotated with @ApiV1Config")
            .isTrue();
    }

    @Test
    @DisplayName("StatisticsController should have @RequestMapping annotation with /stats path")
    void statisticsControllerShouldHaveRequestMappingAnnotation() {
        RequestMapping requestMapping = StatisticsController.class.getAnnotation(RequestMapping.class);
        assertThat(requestMapping)
            .as("StatisticsController should be annotated with @RequestMapping")
            .isNotNull();
        assertThat(requestMapping.value())
            .as("StatisticsController should have path /stats")
            .contains("/stats");
    }

    @Test
    @DisplayName("HandHistoryController should have @ApiV1Config annotation")
    void handHistoryControllerShouldHaveApiV1Annotation() {
        assertThat(HandHistoryController.class.isAnnotationPresent(ApiV1Config.class))
            .as("HandHistoryController should be annotated with @ApiV1Config")
            .isTrue();
    }

    @Test
    @DisplayName("AchievementController should have @ApiV1Config annotation")
    void achievementControllerShouldHaveApiV1Annotation() {
        assertThat(AchievementController.class.isAnnotationPresent(ApiV1Config.class))
            .as("AchievementController should be annotated with @ApiV1Config")
            .isTrue();
    }
}
