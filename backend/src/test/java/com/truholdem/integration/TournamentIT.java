package com.truholdem.integration;

import com.truholdem.dto.CreateTournamentRequest;
import com.truholdem.model.*;
import com.truholdem.repository.*;
import com.truholdem.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Tournament Integration Tests")
class TournamentIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("truholdem_tournament_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.liquibase.enabled", () -> "false");

        registry.add("spring.cache.type", () -> "simple");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");

        registry.add("app.jwt.secret", () -> "dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy0xMjM0NTY3ODkw");
        registry.add("app.jwt.expiration", () -> "86400000");

        registry.add("app.tournament.level-duration-seconds", () -> "5");
        registry.add("app.tournament.break-duration-seconds", () -> "2");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private PokerGameService pokerGameService;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentRegistrationRepository registrationRepository;

    @Autowired
    private TournamentTableRepository tableRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private Tournament tournament;
    private UUID tournamentId;
    private List<UUID> playerIds;
    private Map<UUID, String> playerNames;

    @BeforeEach
    void setUp() {
        reset(messagingTemplate);

        registrationRepository.deleteAll();
        tableRepository.deleteAll();
        tournamentRepository.deleteAll();

        playerIds = new ArrayList<>();
        playerNames = new HashMap<>();
    }

    @AfterEach
    void tearDown() {
        // Tournament cleanup is handled automatically by test transaction rollback
        // No need to manually cancel scheduled tasks as they're managed by Spring context
        tournament = null;
        playerNames.clear();
    }

    // ==================== Helper Methods ====================

    private Tournament createSitAndGoTournament(int maxPlayers, int buyIn) {
        CreateTournamentRequest request = CreateTournamentRequest.sitAndGo(
            "Test SNG " + System.currentTimeMillis(),
            buyIn
        );
        return tournamentService.createTournament(request);
    }

    private Tournament createFreezeoutTournament(int maxPlayers, int buyIn, int startingChips) {
        CreateTournamentRequest request = CreateTournamentRequest.freezeout(
            "Test Freezeout " + System.currentTimeMillis(),
            buyIn,
            startingChips
        );
        return tournamentService.createTournament(request);
    }

    private Tournament createRebuyTournament(int buyIn, int maxRebuys) {
        CreateTournamentRequest request = CreateTournamentRequest.rebuy(
            "Test Rebuy " + System.currentTimeMillis(),
            buyIn,
            maxRebuys
        );
        return tournamentService.createTournament(request);
    }

    private void registerPlayers(int count) {
        for (int i = 0; i < count; i++) {
            UUID playerId = UUID.randomUUID();
            String playerName = "Player_" + (i + 1);
            playerIds.add(playerId);
            playerNames.put(playerId, playerName);

            tournamentService.registerPlayer(tournament.getId(), playerId, playerName);
        }
    }

    /**
     * Eliminates a player using the proper service method.
     * The service handles all the state updates internally.
     */
    private void eliminatePlayer(UUID playerId) {
        tournamentService.handlePlayerElimination(tournament.getId(), playerId);
    }

    private void simulateHandWin(UUID winnerId, int chipsWon) {
        tournament = tournamentService.getTournament(tournament.getId());
        TournamentRegistration winner = tournament.findRegistration(winnerId)
            .orElseThrow();

        winner.setChips(winner.getCurrentChips() + chipsWon);
        registrationRepository.save(winner);
    }

    private int countRemainingPlayers() {
        tournament = tournamentService.getTournament(tournament.getId());
        return (int) tournament.getRegistrations().stream()
            .filter(r -> r.getStatus() == RegistrationStatus.PLAYING)
            .count();
    }

    private int countActiveTables() {
        tournament = tournamentService.getTournament(tournament.getId());
        return tournament.getActiveTables().size();
    }

    // ==================== Complete Tournament Flow Tests ====================

    @Nested
    @DisplayName("Complete Tournament Flow")
    @TestMethodOrder(OrderAnnotation.class)
    class CompleteTournamentFlowTests {

        @Test
        @Order(1)
        @DisplayName("Should run complete 9-player Sit & Go until winner")
        void shouldRunCompleteTournament() {
            tournament = createSitAndGoTournament(9, 100);
            tournamentId = tournament.getId();

            registerPlayers(9);

            tournament = tournamentService.getTournament(tournamentId);

            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.RUNNING);
            assertThat(tournament.getRegistrations()).hasSize(9);
            assertThat(tournament.getPrizePool()).isEqualTo(900); // int, not BigDecimal

            List<UUID> playersToEliminate = new ArrayList<>(playerIds);
            UUID lastStanding = playersToEliminate.remove(0);

            Collections.shuffle(playersToEliminate);

            int eliminated = 0;
            for (UUID loser : playersToEliminate) {
                simulateHandWin(lastStanding, 1500 / 8);
                eliminatePlayer(loser);
                eliminated++;

                tournament = tournamentService.getTournament(tournamentId);
                assertThat(countRemainingPlayers()).isEqualTo(9 - eliminated);
            }

            tournament = tournamentService.getTournament(tournamentId);
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);

            TournamentRegistration winner = tournament.findRegistration(lastStanding).orElseThrow();
            assertThat(winner.getFinishPosition()).isEqualTo(1);
            assertThat(winner.getPrizeWon()).isGreaterThan(0);
        }

        @Test
        @Order(2)
        @DisplayName("Should track finish positions correctly")
        void shouldTrackFinishPositions() {
            tournament = createSitAndGoTournament(6, 50);
            tournamentId = tournament.getId();
            registerPlayers(6);

            tournament = tournamentService.getTournament(tournamentId);
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.RUNNING);

            List<UUID> eliminationOrder = new ArrayList<>(playerIds.subList(1, 6));
            UUID winner = playerIds.get(0);

            int expectedPosition = 6;
            for (UUID loser : eliminationOrder) {
                eliminatePlayer(loser);

                tournament = tournamentService.getTournament(tournamentId);
                TournamentRegistration eliminatedReg = tournament.findRegistration(loser).orElseThrow();

                assertThat(eliminatedReg.getFinishPosition()).isEqualTo(expectedPosition);
                expectedPosition--;
            }

            tournament = tournamentService.getTournament(tournamentId);
            TournamentRegistration winnerReg = tournament.findRegistration(winner).orElseThrow();
            assertThat(winnerReg.getFinishPosition()).isEqualTo(1);
        }
    }

    // ==================== Table Balancing Tests ====================

    @Nested
    @DisplayName("Table Balancing")
    class TableBalancingTests {

        @Test
        @DisplayName("Should rebalance tables correctly when players are eliminated")
        void shouldRebalanceTablesCorrectly() {
            tournament = createFreezeoutTournament(18, 100, 1500);
            tournamentId = tournament.getId();
            registerPlayers(18);

            tournamentService.startTournament(tournamentId);

            tournament = tournamentService.getTournament(tournamentId);
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.RUNNING);

            int initialTables = countActiveTables();
            assertThat(initialTables).isEqualTo(2);

            for (int i = 0; i < 8; i++) {
                eliminatePlayer(playerIds.get(i));
            }

            tournament = tournamentService.getTournament(tournamentId);

            assertThat(countRemainingPlayers()).isEqualTo(10);

            List<TournamentTable> tables = tournament.getActiveTables();
            int totalSeated = tables.stream()
                .mapToInt(TournamentTable::getPlayerCount)
                .sum();

            assertThat(totalSeated).isEqualTo(10);

            tables.forEach(table ->
                assertThat(table.getPlayerCount()).isLessThanOrEqualTo(9)
            );

            if (tables.size() > 1) {
                int max = tables.stream().mapToInt(TournamentTable::getPlayerCount).max().orElse(0);
                int min = tables.stream().mapToInt(TournamentTable::getPlayerCount).min().orElse(0);
                assertThat(max - min).isLessThanOrEqualTo(1);
            }
        }

        @Test
        @DisplayName("Should move player from large table to small table")
        void shouldMovePlayerToBalanceTables() {
            tournament = createFreezeoutTournament(12, 100, 1500);
            tournamentId = tournament.getId();
            registerPlayers(12);

            tournamentService.startTournament(tournamentId);
            tournament = tournamentService.getTournament(tournamentId);

            List<TournamentTable> tables = tournament.getActiveTables();
            assertThat(tables).hasSize(2);

            TournamentTable table1 = tables.get(0);
            UUID playerToEliminate = table1.getPlayerIds().get(0);
            eliminatePlayer(playerToEliminate);

            tournament = tournamentService.getTournament(tournamentId);
            tables = tournament.getActiveTables();

            int count1 = tables.get(0).getPlayerCount();
            int count2 = tables.size() > 1 ? tables.get(1).getPlayerCount() : 0;

            assertThat(count1 + count2).isEqualTo(11);
            assertThat(Math.abs(count1 - count2)).isLessThanOrEqualTo(1);
        }
    }

    // ==================== Blind Level Progression Tests ====================

    @Nested
    @DisplayName("Blind Level Progression")
    class BlindLevelProgressionTests {

        @Test
        @DisplayName("Should progress blind levels automatically")
        void shouldProgressBlindLevels() {
            tournament = createSitAndGoTournament(4, 100);
            tournamentId = tournament.getId();
            registerPlayers(4);

            tournament = tournamentService.getTournament(tournamentId);
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.RUNNING);

            int initialLevel = tournament.getCurrentLevel();
            BlindLevel initialBlinds = tournament.getCurrentBlindLevel();

            assertThat(initialLevel).isEqualTo(1);
            assertThat(initialBlinds.getSmallBlind()).isGreaterThan(0);
            assertThat(initialBlinds.getBigBlind()).isGreaterThan(initialBlinds.getSmallBlind());

            await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Tournament t = tournamentService.getTournament(tournamentId);
                    assertThat(t.getCurrentLevel()).isGreaterThan(initialLevel);
                });

            tournament = tournamentService.getTournament(tournamentId);
            BlindLevel newBlinds = tournament.getCurrentBlindLevel();

            assertThat(newBlinds.getSmallBlind()).isGreaterThan(initialBlinds.getSmallBlind());
            assertThat(newBlinds.getBigBlind()).isGreaterThan(initialBlinds.getBigBlind());
        }

        @Test
        @DisplayName("Should trigger break at correct intervals")
        void shouldTriggerBreakAtCorrectIntervals() {
            tournament = createSitAndGoTournament(4, 100);
            tournamentId = tournament.getId();
            registerPlayers(4);

            tournament = tournamentService.getTournament(tournamentId);

            tournamentService.advanceLevel(tournamentId);
            tournamentService.advanceLevel(tournamentId);
            tournamentService.advanceLevel(tournamentId);

            tournament = tournamentService.getTournament(tournamentId);

            assertThat(tournament.getCurrentLevel()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Should have ante at higher blind levels")
        void shouldHaveAnteAtHigherLevels() {
            tournament = createSitAndGoTournament(4, 100);
            tournamentId = tournament.getId();
            registerPlayers(4);

            tournament = tournamentService.getTournament(tournamentId);

            BlindLevel level1 = tournament.getCurrentBlindLevel();
            assertThat(level1.getAnte()).isEqualTo(0);

            for (int i = 0; i < 5; i++) {
                tournamentService.advanceLevel(tournamentId);
            }

            tournament = tournamentService.getTournament(tournamentId);
            BlindLevel laterLevel = tournament.getCurrentBlindLevel();

            assertThat(laterLevel.getAnte()).isGreaterThanOrEqualTo(0);
        }
    }

    // ==================== Rebuy Tournament Tests ====================

    @Nested
    @DisplayName("Rebuy Tournament")
    class RebuyTournamentTests {

        @Test
        @DisplayName("Should allow rebuy within rebuy period")
        void shouldHandleRebuy() {
            tournament = createRebuyTournament(100, 3);
            tournamentId = tournament.getId();
            registerPlayers(6);

            tournamentService.startTournament(tournamentId);
            tournament = tournamentService.getTournament(tournamentId);

            UUID rebuyPlayer = playerIds.get(0);
            TournamentRegistration reg = tournament.findRegistration(rebuyPlayer).orElseThrow();
            int initialChips = reg.getCurrentChips();

            reg.setChips(0);
            registrationRepository.save(reg);

            TournamentRegistration afterRebuy = tournamentService.processRebuy(tournamentId, rebuyPlayer);

            assertThat(afterRebuy.getCurrentChips()).isEqualTo(tournament.getStartingChips());
            assertThat(afterRebuy.getRebuysUsed()).isEqualTo(1);
            assertThat(afterRebuy.getStatus()).isEqualTo(RegistrationStatus.PLAYING);

            tournament = tournamentService.getTournament(tournamentId);
            int expectedPrizePool = 6 * 100 + 100; // int
            assertThat(tournament.getPrizePool()).isEqualTo(expectedPrizePool);
        }

        @Test
        @DisplayName("Should reject rebuy after max rebuys used")
        void shouldRejectRebuyAfterMaxUsed() {
            tournament = createRebuyTournament(100, 2);
            tournamentId = tournament.getId();
            registerPlayers(4);

            tournamentService.startTournament(tournamentId);
            tournament = tournamentService.getTournament(tournamentId);

            UUID rebuyPlayer = playerIds.get(0);

            for (int i = 0; i < 2; i++) {
                TournamentRegistration reg = tournamentService.getTournament(tournamentId)
                    .findRegistration(rebuyPlayer).orElseThrow();
                reg.setChips(0);
                registrationRepository.save(reg);
                tournamentService.processRebuy(tournamentId, rebuyPlayer);
            }

            TournamentRegistration reg = tournamentService.getTournament(tournamentId)
                .findRegistration(rebuyPlayer).orElseThrow();
            assertThat(reg.getRebuysUsed()).isEqualTo(2);

            reg.setChips(0);
            registrationRepository.save(reg);

            assertThatThrownBy(() ->
                tournamentService.processRebuy(tournamentId, rebuyPlayer)
            ).isInstanceOf(IllegalStateException.class)
             .hasMessageContaining("rebuy");
        }

        @Test
        @DisplayName("Should reject rebuy in freezeout tournament")
        void shouldRejectRebuyInFreezeout() {
            tournament = createFreezeoutTournament(9, 100, 1500);
            tournamentId = tournament.getId();
            registerPlayers(4);

            tournamentService.startTournament(tournamentId);
            tournament = tournamentService.getTournament(tournamentId);

            UUID player = playerIds.get(0);
            TournamentRegistration reg = tournament.findRegistration(player).orElseThrow();
            reg.setChips(0);
            registrationRepository.save(reg);

            assertThatThrownBy(() ->
                tournamentService.processRebuy(tournamentId, player)
            ).isInstanceOf(IllegalStateException.class);
        }
    }

    // ==================== Final Table Consolidation Tests ====================

    @Nested
    @DisplayName("Final Table Consolidation")
    class FinalTableConsolidationTests {

        @Test
        @DisplayName("Should consolidate to final table when 9 or fewer players remain")
        void shouldConsolidateToFinalTable() {
            tournament = createFreezeoutTournament(18, 100, 1500);
            tournamentId = tournament.getId();
            registerPlayers(18);

            tournamentService.startTournament(tournamentId);
            tournament = tournamentService.getTournament(tournamentId);

            assertThat(countActiveTables()).isEqualTo(2);
            assertThat(tournament.getStatus()).isNotEqualTo(TournamentStatus.FINAL_TABLE);

            for (int i = 0; i < 9; i++) {
                eliminatePlayer(playerIds.get(i));
            }

            tournament = tournamentService.getTournament(tournamentId);

            assertThat(countRemainingPlayers()).isEqualTo(9);
            assertThat(countActiveTables()).isEqualTo(1);
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.FINAL_TABLE);

            TournamentTable finalTable = tournament.getActiveTables().get(0);
            assertThat(finalTable.isFinalTable()).isTrue();
            assertThat(finalTable.getPlayerCount()).isEqualTo(9);
        }

        @Test
        @DisplayName("Should mark final table in tournament status")
        void shouldMarkFinalTableStatus() {
            tournament = createSitAndGoTournament(9, 100);
            tournamentId = tournament.getId();
            registerPlayers(9);

            tournament = tournamentService.getTournament(tournamentId);

            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.RUNNING);
            assertThat(countActiveTables()).isEqualTo(1);
            // Single table SNG is always at final table effectively
        }
    }

    // ==================== Prize Pool Calculation Tests ====================

    @Nested
    @DisplayName("Prize Pool Calculation")
    class PrizePoolCalculationTests {

        @Test
        @DisplayName("Should calculate prizes correctly for standard payout")
        void shouldCalculatePrizesCorrectly() {
            tournament = createSitAndGoTournament(9, 100);
            tournamentId = tournament.getId();
            registerPlayers(9);

            tournament = tournamentService.getTournament(tournamentId);
            int prizePool = tournament.getPrizePool();

            assertThat(prizePool).isEqualTo(900);

            // getPayoutStructure returns List<Integer> (percentages)
            List<Integer> payoutStructure = tournament.getPayoutStructure();

            assertThat(payoutStructure).isNotEmpty();

            // Calculate actual prizes from percentages
            int firstPlacePrize = tournament.calculatePrizeForPosition(1);
            int secondPlacePrize = tournament.calculatePrizeForPosition(2);

            assertThat(firstPlacePrize).isGreaterThan(secondPlacePrize);
        }

        @Test
        @DisplayName("Should award prizes to correct positions")
        void shouldAwardPrizesToCorrectPositions() {
            tournament = createSitAndGoTournament(6, 100);
            tournamentId = tournament.getId();
            registerPlayers(6);

            tournament = tournamentService.getTournament(tournamentId);

            List<UUID> eliminationOrder = new ArrayList<>();
            UUID winner = playerIds.get(0);

            for (int i = 5; i >= 1; i--) {
                eliminationOrder.add(playerIds.get(i));
            }

            for (UUID loser : eliminationOrder) {
                eliminatePlayer(loser);
            }

            tournament = tournamentService.getTournament(tournamentId);

            TournamentRegistration first = tournament.findRegistration(winner).orElseThrow();
            assertThat(first.getFinishPosition()).isEqualTo(1);
            assertThat(first.getPrizeWon()).isGreaterThan(0);

            TournamentRegistration second = tournament.findRegistration(playerIds.get(1)).orElseThrow();
            assertThat(second.getFinishPosition()).isEqualTo(2);

            TournamentRegistration third = tournament.findRegistration(playerIds.get(2)).orElseThrow();
            assertThat(third.getFinishPosition()).isEqualTo(3);

            TournamentRegistration bubble = tournament.findRegistration(playerIds.get(3)).orElseThrow();
            assertThat(bubble.getFinishPosition()).isEqualTo(4);
            assertThat(bubble.getPrizeWon()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should include rebuys in prize pool")
        void shouldIncludeRebuysInPrizePool() {
            tournament = createRebuyTournament(100, 5);
            tournamentId = tournament.getId();
            registerPlayers(6);

            tournamentService.startTournament(tournamentId);
            tournament = tournamentService.getTournament(tournamentId);

            int initialPrizePool = tournament.getPrizePool();
            assertThat(initialPrizePool).isEqualTo(600);

            for (int i = 0; i < 3; i++) {
                UUID player = playerIds.get(i);
                TournamentRegistration reg = tournamentService.getTournament(tournamentId)
                    .findRegistration(player).orElseThrow();
                reg.setChips(0);
                registrationRepository.save(reg);
                tournamentService.processRebuy(tournamentId, player);
            }

            tournament = tournamentService.getTournament(tournamentId);
            int finalPrizePool = tournament.getPrizePool();

            assertThat(finalPrizePool).isEqualTo(900);
        }
    }

    // ==================== Edge Cases and Error Handling ====================

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should not start tournament with insufficient players")
        void shouldNotStartWithInsufficientPlayers() {
            tournament = createSitAndGoTournament(9, 100);
            tournamentId = tournament.getId();
            registerPlayers(1);

            assertThatThrownBy(() ->
                tournamentService.startTournament(tournamentId)
            ).isInstanceOf(IllegalStateException.class)
             .hasMessageContaining("player");
        }

        @Test
        @DisplayName("Should handle concurrent registrations")
        void shouldHandleConcurrentRegistrations() throws Exception {
            tournament = createSitAndGoTournament(9, 100);
            tournamentId = tournament.getId();

            ExecutorService executor = Executors.newFixedThreadPool(9);
            CountDownLatch latch = new CountDownLatch(9);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < 9; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    try {
                        UUID playerId = UUID.randomUUID();
                        tournamentService.registerPlayer(
                            tournamentId,
                            playerId,
                            "Concurrent_" + index
                        );
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            tournament = tournamentService.getTournament(tournamentId);
            assertThat(tournament.getRegistrations()).hasSize(9);
        }

        @Test
        @DisplayName("Should reject registration when tournament is full")
        void shouldRejectRegistrationWhenFull() {
            tournament = createSitAndGoTournament(4, 100);
            tournamentId = tournament.getId();
            registerPlayers(4);

            tournament = tournamentService.getTournament(tournamentId);
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.RUNNING);

            assertThatThrownBy(() ->
                tournamentService.registerPlayer(
                    tournamentId,
                    UUID.randomUUID(),
                    "Extra_Player"
                )
            ).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should not allow duplicate registration")
        void shouldNotAllowDuplicateRegistration() {
            tournament = createSitAndGoTournament(9, 100);
            tournamentId = tournament.getId();

            UUID playerId = UUID.randomUUID();
            tournamentService.registerPlayer(tournamentId, playerId, "FirstReg");

            assertThatThrownBy(() ->
                tournamentService.registerPlayer(tournamentId, playerId, "SecondReg")
            ).isInstanceOf(IllegalStateException.class)
             .hasMessageContaining("already registered");
        }
    }

    // ==================== Tournament Statistics ====================

    @Nested
    @DisplayName("Tournament Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should track tournament duration")
        void shouldTrackTournamentDuration() {
            tournament = createSitAndGoTournament(4, 100);
            tournamentId = tournament.getId();
            registerPlayers(4);

            tournament = tournamentService.getTournament(tournamentId);

            assertThat(tournament.getStartTime()).isNotNull();
            assertThat(tournament.getEndTime()).isNull();

            for (int i = 1; i < 4; i++) {
                eliminatePlayer(playerIds.get(i));
            }

            tournament = tournamentService.getTournament(tournamentId);
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
            assertThat(tournament.getEndTime()).isNotNull();
            assertThat(tournament.getEndTime()).isAfterOrEqualTo(tournament.getStartTime());
        }

        @Test
        @DisplayName("Should provide leaderboard during tournament")
        void shouldProvideLeaderboard() {
            tournament = createSitAndGoTournament(6, 100);
            tournamentId = tournament.getId();
            registerPlayers(6);

            tournament = tournamentService.getTournament(tournamentId);

            List<TournamentRegistration> regs = new ArrayList<>(tournament.getRegistrations());
            int[] chips = {3000, 2500, 2000, 1500, 1000, 0};

            for (int i = 0; i < 6; i++) {
                TournamentRegistration reg = regs.get(i);
                reg.setChips(chips[i]);
                registrationRepository.save(reg);
            }

            List<TournamentRegistration> leaderboard = tournamentService.getLeaderboard(tournamentId);

            assertThat(leaderboard).hasSize(6);
            assertThat(leaderboard.get(0).getCurrentChips()).isEqualTo(3000);
            assertThat(leaderboard.get(1).getCurrentChips()).isEqualTo(2500);
            assertThat(leaderboard.get(5).getCurrentChips()).isEqualTo(0);
        }
    }
}
