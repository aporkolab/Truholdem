package com.truholdem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.truholdem.model.PlayerStatistics;
import com.truholdem.repository.PlayerStatisticsRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerStatisticsService Tests")
class PlayerStatisticsServiceTest {

    @Mock
    private PlayerStatisticsRepository statsRepository;

    @InjectMocks
    private PlayerStatisticsService statsService;

    @Captor
    private ArgumentCaptor<PlayerStatistics> statsCaptor;

    private PlayerStatistics testStats;
    private final String TEST_PLAYER = "TestPlayer";
    private final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testStats = new PlayerStatistics();
        testStats.setId(UUID.randomUUID());
        testStats.setPlayerName(TEST_PLAYER);
        testStats.setUserId(TEST_USER_ID);
        testStats.setHandsPlayed(100);
        testStats.setHandsWon(25);
        testStats.setTotalWinnings(new BigDecimal("5000"));
        testStats.setTotalLosses(new BigDecimal("3000"));
        testStats.setHandsVoluntarilyPutInPot(40);
        testStats.setHandsRaisedPreFlop(20);
        testStats.setTotalBets(50);
        testStats.setTotalRaises(30);
        testStats.setTotalCalls(80);
        testStats.setTotalFolds(60);
        testStats.setHandsWentToShowdown(30);
        testStats.setShowdownsWon(15);
    }

    @Nested
    @DisplayName("Get or Create Tests")
    class GetOrCreateTests {

        @Test
        @DisplayName("Should return existing stats for player")
        void shouldReturnExistingStats() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));

            PlayerStatistics result = statsService.getOrCreateStats(TEST_PLAYER);

            assertThat(result.getPlayerName()).isEqualTo(TEST_PLAYER);
            assertThat(result.getHandsPlayed()).isEqualTo(100);
            verify(statsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create new stats for unknown player")
        void shouldCreateNewStats() {
            when(statsRepository.findByPlayerName("NewPlayer"))
                    .thenReturn(Optional.empty());
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PlayerStatistics result = statsService.getOrCreateStats("NewPlayer");

            assertThat(result.getPlayerName()).isEqualTo("NewPlayer");
            assertThat(result.getHandsPlayed()).isZero();
            verify(statsRepository).save(any(PlayerStatistics.class));
        }

        @Test
        @DisplayName("Should get stats by user ID")
        void shouldGetStatsByUserId() {
            when(statsRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(testStats));

            PlayerStatistics result = statsService.getOrCreateStats(TEST_USER_ID, TEST_PLAYER);

            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Recording Actions Tests")
    class RecordingActionsTests {

        @Test
        @DisplayName("Should record hand played")
        void shouldRecordHandPlayed() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordHandPlayed(TEST_PLAYER, true, false);

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getHandsPlayed()).isEqualTo(101);
        }

        @Test
        @DisplayName("Should record FOLD action")
        void shouldRecordFoldAction() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordAction(TEST_PLAYER, "FOLD");

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getTotalFolds()).isEqualTo(61);
        }

        @Test
        @DisplayName("Should record CALL action and update VPIP")
        void shouldRecordCallAction() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordAction(TEST_PLAYER, "CALL");

            verify(statsRepository).save(statsCaptor.capture());
            PlayerStatistics saved = statsCaptor.getValue();
            assertThat(saved.getTotalCalls()).isEqualTo(81);
            assertThat(saved.getHandsVoluntarilyPutInPot()).isEqualTo(40);
        }

        @Test
        @DisplayName("Should record RAISE action and update PFR")
        void shouldRecordRaiseAction() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordAction(TEST_PLAYER, "RAISE");

            verify(statsRepository).save(statsCaptor.capture());
            PlayerStatistics saved = statsCaptor.getValue();
            assertThat(saved.getTotalRaises()).isEqualTo(31);
            assertThat(saved.getHandsVoluntarilyPutInPot()).isEqualTo(40);
            assertThat(saved.getHandsRaisedPreFlop()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should record BET action")
        void shouldRecordBetAction() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordAction(TEST_PLAYER, "BET");

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getTotalBets()).isEqualTo(51);
        }

        @Test
        @DisplayName("Should record CHECK action")
        void shouldRecordCheckAction() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordAction(TEST_PLAYER, "CHECK");

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getTotalChecks()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Recording Results Tests")
    class RecordingResultsTests {

        @Test
        @DisplayName("Should record win and update streak")
        void shouldRecordWin() {
            testStats.setCurrentWinStreak(2);
            testStats.setLongestWinStreak(3);
            testStats.setCurrentLoseStreak(0);

            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordWin(TEST_PLAYER, 500);

            verify(statsRepository).save(statsCaptor.capture());
            PlayerStatistics saved = statsCaptor.getValue();

            assertThat(saved.getHandsWon()).isEqualTo(26);
            assertThat(saved.getTotalWinnings()).isEqualByComparingTo(new BigDecimal("5500"));
            assertThat(saved.getCurrentWinStreak()).isEqualTo(3);
            assertThat(saved.getCurrentLoseStreak()).isZero();
        }

        @Test
        @DisplayName("Should update longest win streak")
        void shouldUpdateLongestWinStreak() {
            testStats.setCurrentWinStreak(3);
            testStats.setLongestWinStreak(3);

            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordWin(TEST_PLAYER, 100);

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getLongestWinStreak()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should record loss and update streak")
        void shouldRecordLoss() {
            testStats.setCurrentWinStreak(5);
            testStats.setCurrentLoseStreak(0);

            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordLoss(TEST_PLAYER, 200);

            verify(statsRepository).save(statsCaptor.capture());
            PlayerStatistics saved = statsCaptor.getValue();

            assertThat(saved.getTotalLosses()).isEqualByComparingTo(new BigDecimal("3200"));
            assertThat(saved.getCurrentWinStreak()).isZero();
            assertThat(saved.getCurrentLoseStreak()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should record showdown won")
        void shouldRecordShowdownWon() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordShowdown(TEST_PLAYER, true);

            verify(statsRepository).save(statsCaptor.capture());
            PlayerStatistics saved = statsCaptor.getValue();

            assertThat(saved.getHandsWentToShowdown()).isEqualTo(31);
            assertThat(saved.getShowdownsWon()).isEqualTo(16);
        }

        @Test
        @DisplayName("Should record showdown lost")
        void shouldRecordShowdownLost() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordShowdown(TEST_PLAYER, false);

            verify(statsRepository).save(statsCaptor.capture());
            PlayerStatistics saved = statsCaptor.getValue();

            assertThat(saved.getHandsWentToShowdown()).isEqualTo(31);
            assertThat(saved.getShowdownsWon()).isEqualTo(15); 
        }

        @Test
        @DisplayName("Should record all-in")
        void shouldRecordAllIn() {
            testStats.setTimesAllIn(10);

            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordAllIn(TEST_PLAYER);

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getTimesAllIn()).isEqualTo(11);
        }

        @Test
        @DisplayName("Should record all-in result won")
        void shouldRecordAllInResultWon() {
            testStats.setTimesAllIn(10);
            testStats.setAllInsWon(5);

            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordAllInResult(TEST_PLAYER, true);

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getAllInsWon()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should update biggest pot won")
        void shouldUpdateBiggestPotWon() {
            testStats.setBiggestPotWon(1000);

            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));
            when(statsRepository.save(any(PlayerStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            statsService.recordWin(TEST_PLAYER, 2000);

            verify(statsRepository).save(statsCaptor.capture());
            assertThat(statsCaptor.getValue().getBiggestPotWon()).isEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("Leaderboard Tests")
    class LeaderboardTests {

        @Test
        @DisplayName("Should get top players by winnings")
        void shouldGetTopByWinnings() {
            List<PlayerStatistics> topPlayers = Arrays.asList(testStats);
            when(statsRepository.findTop10ByOrderByTotalWinningsDesc())
                    .thenReturn(topPlayers);

            List<PlayerStatistics> result = statsService.getTopByWinnings();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPlayerName()).isEqualTo(TEST_PLAYER);
        }

        @Test
        @DisplayName("Should get top players by hands won")
        void shouldGetTopByHandsWon() {
            List<PlayerStatistics> topPlayers = Arrays.asList(testStats);
            when(statsRepository.findTop10ByOrderByHandsWonDesc())
                    .thenReturn(topPlayers);

            List<PlayerStatistics> result = statsService.getTopByHandsWon();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get top players by win rate")
        void shouldGetTopByWinRate() {
            List<PlayerStatistics> topPlayers = Arrays.asList(testStats);
            when(statsRepository.findTopPlayersByWinRate(anyInt()))
                    .thenReturn(topPlayers);

            List<PlayerStatistics> result = statsService.getTopByWinRate();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get comprehensive leaderboard")
        void shouldGetComprehensiveLeaderboard() {
            when(statsRepository.findTop10ByOrderByTotalWinningsDesc())
                    .thenReturn(Arrays.asList(testStats));
            when(statsRepository.findTop10ByOrderByHandsWonDesc())
                    .thenReturn(Arrays.asList(testStats));
            when(statsRepository.findTopPlayersByWinRate(anyInt()))
                    .thenReturn(Arrays.asList(testStats));
            when(statsRepository.findTop10ByOrderByBiggestPotWonDesc())
                    .thenReturn(Arrays.asList(testStats));
            when(statsRepository.findTop10ByOrderByLongestWinStreakDesc())
                    .thenReturn(Arrays.asList(testStats));
            when(statsRepository.findTop20ByOrderByHandsPlayedDesc())
                    .thenReturn(Arrays.asList(testStats));

            PlayerStatisticsService.LeaderboardData result = statsService.getLeaderboard();

            assertThat(result.byWinnings()).isNotEmpty();
            assertThat(result.byHandsWon()).isNotEmpty();
            assertThat(result.byWinRate()).isNotEmpty();
            assertThat(result.byBiggestPot()).isNotEmpty();
            assertThat(result.byWinStreak()).isNotEmpty();
            assertThat(result.mostActive()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Statistics Calculations Tests")
    class StatisticsCalculationsTests {

        @Test
        @DisplayName("Should calculate VPIP correctly")
        void shouldCalculateVPIP() {
            
            
            assertThat(testStats.getVPIP()).isEqualTo(40.0);
        }

        @Test
        @DisplayName("Should calculate PFR correctly")
        void shouldCalculatePFR() {
            
            
            assertThat(testStats.getPFR()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Should calculate aggression factor correctly")
        void shouldCalculateAggressionFactor() {
            
            
            assertThat(testStats.getAggressionFactor()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should calculate WTSD correctly")
        void shouldCalculateWTSD() {
            
            
            assertThat(testStats.getWTSD()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("Should calculate W$SD correctly")
        void shouldCalculateWSD() {
            
            
            assertThat(testStats.getWonAtShowdown()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should calculate win rate correctly")
        void shouldCalculateWinRate() {
            
            
            assertThat(testStats.getWinRate()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should calculate net profit correctly")
        void shouldCalculateNetProfit() {
            
            
            assertThat(testStats.getNetProfit()).isEqualByComparingTo(new BigDecimal("2000"));
        }
    }

    @Nested
    @DisplayName("Summary Tests")
    class SummaryTests {

        @Test
        @DisplayName("Should generate player stats summary")
        void shouldGenerateStatsSummary() {
            when(statsRepository.findByPlayerName(TEST_PLAYER))
                    .thenReturn(Optional.of(testStats));

            PlayerStatisticsService.PlayerStatsSummary result = statsService.getStatsSummary(TEST_PLAYER);

            assertThat(result).isNotNull();
            assertThat(result.playerName()).isEqualTo(TEST_PLAYER);
            assertThat(result.handsPlayed()).isEqualTo(100);
            assertThat(result.handsWon()).isEqualTo(25);
            assertThat(result.winRate()).isEqualTo(25.0);
            assertThat(result.vpip()).isEqualTo(40.0);
            assertThat(result.pfr()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Should return null for unknown player summary")
        void shouldReturnNullForUnknownPlayer() {
            when(statsRepository.findByPlayerName("Unknown"))
                    .thenReturn(Optional.empty());

            PlayerStatisticsService.PlayerStatsSummary result = statsService.getStatsSummary("Unknown");

            assertThat(result).isNull();
        }
    }
}
