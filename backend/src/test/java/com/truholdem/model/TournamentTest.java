package com.truholdem.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;


@DisplayName("Tournament Domain Tests")
class TournamentTest {

    private Tournament tournament;
    
    @BeforeEach
    void setUp() {
        tournament = Tournament.builder("Sunday Million")
            .type(TournamentType.FREEZEOUT)
            .startingChips(1500)
            .players(2, 9)
            .buyIn(100)
            .build();
    }

    @Nested
    @DisplayName("Tournament Creation")
    class CreationTests {

        @Test
        @DisplayName("should create tournament with default values")
        void shouldCreateTournamentWithDefaults() {
            Tournament t = new Tournament("Test Tournament", TournamentType.FREEZEOUT);
            
            assertThat(t.getName()).isEqualTo("Test Tournament");
            assertThat(t.getTournamentType()).isEqualTo(TournamentType.FREEZEOUT);
            assertThat(t.getStatus()).isEqualTo(TournamentStatus.REGISTERING);
            assertThat(t.getBlindStructure()).isNotNull();
            assertThat(t.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should create tournament using builder")
        void shouldCreateTournamentWithBuilder() {
            Tournament t = Tournament.builder("Deep Stack")
                .type(TournamentType.REBUY)
                .startingChips(3000)
                .players(3, 50)
                .buyIn(200)
                .rebuy(200, 6, 3)
                .blindStructure(BlindStructure.deep())
                .build();
            
            assertThat(t.getName()).isEqualTo("Deep Stack");
            assertThat(t.getTournamentType()).isEqualTo(TournamentType.REBUY);
            assertThat(t.getStartingChips()).isEqualTo(3000);
            assertThat(t.getMaxPlayers()).isEqualTo(50);
            assertThat(t.getRebuyAmount()).isEqualTo(200);
            assertThat(t.getMaxRebuys()).isEqualTo(3);
        }

        @Test
        @DisplayName("should configure rebuy tournament with defaults")
        void shouldConfigureRebuyTournament() {
            Tournament t = new Tournament("Rebuy Madness", TournamentType.REBUY);
            
            assertThat(t.getRebuyAmount()).isGreaterThan(0);
            assertThat(t.getRebuyDeadlineLevel()).isGreaterThan(0);
            assertThat(t.getMaxRebuys()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should configure bounty tournament with bounty amount")
        void shouldConfigureBountyTournament() {
            Tournament t = new Tournament("Knockout", TournamentType.BOUNTY);
            
            assertThat(t.getBountyAmount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should configure SNG with turbo structure")
        void shouldConfigureSitAndGo() {
            Tournament t = new Tournament("Turbo SNG", TournamentType.SIT_AND_GO);
            
            assertThat(t.getMaxPlayers()).isEqualTo(9);
            assertThat(t.getBlindStructure().getLevelDurationMinutes()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Player Registration")
    class RegistrationTests {

        @Test
        @DisplayName("should register player successfully")
        void shouldRegisterPlayer() {
            UUID playerId = UUID.randomUUID();
            
            TournamentRegistration reg = tournament.registerPlayer(playerId, "Hero");
            
            assertThat(reg).isNotNull();
            assertThat(reg.getPlayerId()).isEqualTo(playerId);
            assertThat(reg.getPlayerName()).isEqualTo("Hero");
            assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
            assertThat(tournament.getRegistrations()).hasSize(1);
        }

        @Test
        @DisplayName("should reject duplicate registration")
        void shouldRejectDuplicateRegistration() {
            UUID playerId = UUID.randomUUID();
            tournament.registerPlayer(playerId, "Hero");
            
            assertThatThrownBy(() -> tournament.registerPlayer(playerId, "Hero2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("should reject registration when tournament is full")
        void shouldRejectWhenFull() {
            
            for (int i = 0; i < tournament.getMaxPlayers(); i++) {
                tournament.registerPlayer(UUID.randomUUID(), "Player" + i);
            }
            
            assertThatThrownBy(() -> tournament.registerPlayer(UUID.randomUUID(), "Extra"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full");
        }

        @Test
        @DisplayName("should unregister player before tournament starts")
        void shouldUnregisterPlayer() {
            UUID playerId = UUID.randomUUID();
            tournament.registerPlayer(playerId, "Hero");
            
            boolean removed = tournament.unregisterPlayer(playerId);
            
            assertThat(removed).isTrue();
            assertThat(tournament.getRegistrations()).isEmpty();
        }

        @Test
        @DisplayName("should reject unregister after tournament starts")
        void shouldRejectUnregisterAfterStart() {
            registerMinimumPlayers();
            tournament.start();
            
            UUID playerId = tournament.getRegistrations().get(0).getPlayerId();
            
            assertThatThrownBy(() -> tournament.unregisterPlayer(playerId))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Tournament Lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("should start tournament when minimum players registered")
        void shouldStartTournament() {
            registerMinimumPlayers();
            
            tournament.start();
            
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.RUNNING);
            assertThat(tournament.getStartTime()).isNotNull();
            assertThat(tournament.getCurrentLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("should initialize player chips on start")
        void shouldInitializeChipsOnStart() {
            registerMinimumPlayers();
            
            tournament.start();
            
            tournament.getRegistrations().forEach(reg -> {
                assertThat(reg.getCurrentChips()).isEqualTo(tournament.getStartingChips());
                assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.PLAYING);
            });
        }

        @Test
        @DisplayName("should create tables on start")
        void shouldCreateTablesOnStart() {
            registerMinimumPlayers();
            
            tournament.start();
            
            assertThat(tournament.getTables()).isNotEmpty();
            assertThat(tournament.getActiveTables()).isNotEmpty();
        }

        @Test
        @DisplayName("should reject start without minimum players")
        void shouldRejectStartWithoutMinimumPlayers() {
            tournament.registerPlayer(UUID.randomUUID(), "Solo");

            assertThatThrownBy(() -> tournament.start())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough players");
        }

        @Test
        @DisplayName("should advance blind level")
        void shouldAdvanceBlindLevel() {
            registerMinimumPlayers();
            tournament.start();
            
            BlindLevel levelBefore = tournament.getCurrentBlindLevel();
            tournament.advanceLevel();
            BlindLevel levelAfter = tournament.getCurrentBlindLevel();
            
            assertThat(tournament.getCurrentLevel()).isEqualTo(2);
            assertThat(levelAfter.getBigBlind()).isGreaterThanOrEqualTo(levelBefore.getBigBlind());
        }
    }

    @Nested
    @DisplayName("Player Elimination")
    class EliminationTests {

        @BeforeEach
        void startTournament() {
            registerMinimumPlayers();
            tournament.start();
        }

        @Test
        @DisplayName("should eliminate player")
        void shouldEliminatePlayer() {
            UUID playerId = tournament.getRegistrations().get(0).getPlayerId();
            
            tournament.eliminatePlayer(playerId);
            
            TournamentRegistration reg = tournament.findRegistration(playerId).orElseThrow();
            assertThat(reg.getStatus()).isEqualTo(RegistrationStatus.ELIMINATED);
            assertThat(reg.getFinishPosition()).isNotNull();
        }

        @Test
        @DisplayName("should complete tournament when one player remains")
        void shouldCompleteTournamentWithWinner() {
            
            List<TournamentRegistration> regs = tournament.getActiveRegistrations();
            for (int i = 0; i < regs.size() - 1; i++) {
                tournament.eliminatePlayer(regs.get(i).getPlayerId());
            }
            
            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
            assertThat(tournament.getEndTime()).isNotNull();
        }

        @Test
        @DisplayName("should transition to HEADS_UP with two players")
        void shouldTransitionToHeadsUp() {
            // Create a fresh tournament with 4 players
            tournament = Tournament.builder("HeadsUp Test")
                .type(TournamentType.FREEZEOUT)
                .players(2, 9)
                .build();
            for (int i = 0; i < 4; i++) {
                tournament.registerPlayer(UUID.randomUUID(), "Player" + i);
            }
            tournament.start();

            // Snapshot player IDs before elimination to avoid modification issues
            List<UUID> playerIds = tournament.getActiveRegistrations().stream()
                .map(TournamentRegistration::getPlayerId)
                .toList();

            // Eliminate players until 2 remain (eliminate first 2 of 4)
            for (int i = 0; i < playerIds.size() - 2; i++) {
                tournament.eliminatePlayer(playerIds.get(i));
            }

            assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.HEADS_UP);
        }
    }

    @Nested
    @DisplayName("Prize Calculation")
    class PrizeTests {

        @Test
        @DisplayName("should calculate prize pool correctly")
        void shouldCalculatePrizePool() {
            for (int i = 0; i < 6; i++) {
                tournament.registerPlayer(UUID.randomUUID(), "Player" + i);
            }
            
            int expectedPrizePool = tournament.getBuyIn() * 6;
            assertThat(tournament.getPrizePool()).isEqualTo(expectedPrizePool);
        }

        @Test
        @DisplayName("should calculate prize for position")
        void shouldCalculatePrizeForPosition() {
            for (int i = 0; i < 6; i++) {
                tournament.registerPlayer(UUID.randomUUID(), "Player" + i);
            }
            
            int firstPrize = tournament.calculatePrizeForPosition(1);
            int secondPrize = tournament.calculatePrizeForPosition(2);
            int thirdPrize = tournament.calculatePrizeForPosition(3);
            
            assertThat(firstPrize).isGreaterThan(secondPrize);
            assertThat(secondPrize).isGreaterThan(thirdPrize);
            assertThat(firstPrize + secondPrize + thirdPrize).isEqualTo(tournament.getPrizePool());
        }

        @Test
        @DisplayName("should return zero for unpaid positions")
        void shouldReturnZeroForUnpaidPositions() {
            registerMinimumPlayers();
            
            int fourthPrize = tournament.calculatePrizeForPosition(4);
            
            assertThat(fourthPrize).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Blind Structure")
    class BlindStructureTests {

        @Test
        @DisplayName("should create turbo structure")
        void shouldCreateTurboStructure() {
            BlindStructure turbo = BlindStructure.turbo();
            
            assertThat(turbo.getLevelDurationMinutes()).isEqualTo(5);
            assertThat(turbo.getTotalLevels()).isGreaterThan(10);
        }

        @Test
        @DisplayName("should create standard structure")
        void shouldCreateStandardStructure() {
            BlindStructure standard = BlindStructure.standard();
            
            assertThat(standard.getLevelDurationMinutes()).isEqualTo(15);
        }

        @Test
        @DisplayName("should create deep structure with antes")
        void shouldCreateDeepStructure() {
            BlindStructure deep = BlindStructure.deep();
            
            assertThat(deep.getLevelDurationMinutes()).isEqualTo(20);
            assertThat(deep.getAnteStartLevel()).isGreaterThan(1);
        }

        @Test
        @DisplayName("should get blind level by number")
        void shouldGetBlindLevelByNumber() {
            BlindStructure structure = BlindStructure.standard();
            
            BlindLevel level1 = structure.getLevelAt(1);
            BlindLevel level5 = structure.getLevelAt(5);
            
            assertThat(level1.getSmallBlind()).isLessThan(level5.getSmallBlind());
        }

        @Test
        @DisplayName("should return last level for overflow")
        void shouldReturnLastLevelForOverflow() {
            BlindStructure structure = BlindStructure.turbo();
            int totalLevels = structure.getTotalLevels();
            
            BlindLevel lastLevel = structure.getLevelAt(totalLevels);
            BlindLevel overflowLevel = structure.getLevelAt(totalLevels + 10);
            
            assertThat(overflowLevel).isEqualTo(lastLevel);
        }
    }

    @Nested
    @DisplayName("Blind Level")
    class BlindLevelTests {

        @Test
        @DisplayName("should create blind level without ante")
        void shouldCreateLevelWithoutAnte() {
            BlindLevel level = BlindLevel.of(1, 10, 20);
            
            assertThat(level.getLevel()).isEqualTo(1);
            assertThat(level.getSmallBlind()).isEqualTo(10);
            assertThat(level.getBigBlind()).isEqualTo(20);
            assertThat(level.getAnte()).isEqualTo(0);
            assertThat(level.hasAnte()).isFalse();
        }

        @Test
        @DisplayName("should create blind level with ante")
        void shouldCreateLevelWithAnte() {
            BlindLevel level = BlindLevel.withAnte(5, 50, 100, 10);
            
            assertThat(level.getAnte()).isEqualTo(10);
            assertThat(level.hasAnte()).isTrue();
        }

        @Test
        @DisplayName("should calculate total forced bets")
        void shouldCalculateTotalForcedBets() {
            BlindLevel level = BlindLevel.withAnte(5, 50, 100, 10);
            
            int forcedBets = level.totalForcedBets(6);
            
            
            assertThat(forcedBets).isEqualTo(210);
        }

        @Test
        @DisplayName("should reject invalid blind values")
        void shouldRejectInvalidBlinds() {
            assertThatThrownBy(() -> BlindLevel.of(0, 10, 20))
                .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> BlindLevel.of(1, -10, 20))
                .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> BlindLevel.of(1, 30, 20))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Tournament Registration")
    class TournamentRegistrationTests {

        @Test
        @DisplayName("should track rebuy usage")
        void shouldTrackRebuyUsage() {
            Tournament rebuyTournament = new Tournament("Rebuy", TournamentType.REBUY);
            TournamentRegistration reg = new TournamentRegistration(
                rebuyTournament, UUID.randomUUID(), "Player1");
            
            reg.rebuy(1500);
            reg.rebuy(1500);
            
            assertThat(reg.getRebuysUsed()).isEqualTo(2);
            assertThat(reg.getCurrentChips()).isEqualTo(rebuyTournament.getStartingChips() + 3000);
        }

        @Test
        @DisplayName("should track bounty collection")
        void shouldTrackBountyCollection() {
            Tournament bountyTournament = new Tournament("Bounty", TournamentType.BOUNTY);
            TournamentRegistration reg = new TournamentRegistration(
                bountyTournament, UUID.randomUUID(), "Hunter");
            
            reg.collectBounty(50);
            reg.collectBounty(50);
            
            assertThat(reg.getBountiesCollected()).isEqualTo(2);
            assertThat(reg.getPrizeWon()).isEqualTo(100);
        }

        @Test
        @DisplayName("should calculate M-ratio")
        void shouldCalculateMRatio() {
            registerMinimumPlayers();
            tournament.start();
            
            TournamentRegistration reg = tournament.getRegistrations().get(0);
            
            
            double mRatio = reg.getMRatio(10, 20, 0, 2);
            
            
            assertThat(mRatio).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("Tournament Table")
    class TournamentTableTests {

        @Test
        @DisplayName("should seat and remove players")
        void shouldSeatAndRemovePlayers() {
            TournamentTable table = new TournamentTable(tournament, 1);
            UUID playerId = UUID.randomUUID();
            
            table.seatPlayer(playerId);
            assertThat(table.hasPlayer(playerId)).isTrue();
            assertThat(table.getPlayerCount()).isEqualTo(1);
            
            table.removePlayer(playerId);
            assertThat(table.hasPlayer(playerId)).isFalse();
        }

        @Test
        @DisplayName("should reject seating when table is full")
        void shouldRejectWhenFull() {
            TournamentTable table = new TournamentTable(tournament, 1);
            
            
            for (int i = 0; i < 9; i++) {
                table.seatPlayer(UUID.randomUUID());
            }
            
            assertThat(table.isFull()).isTrue();
            assertThatThrownBy(() -> table.seatPlayer(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full");
        }

        @Test
        @DisplayName("should create final table")
        void shouldCreateFinalTable() {
            TournamentTable finalTable = TournamentTable.createFinalTable(tournament);
            
            assertThat(finalTable.isFinalTable()).isTrue();
            assertThat(finalTable.getTableNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should track seat positions")
        void shouldTrackSeatPositions() {
            TournamentTable table = new TournamentTable(tournament, 1);
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            
            table.seatPlayer(player1);
            table.seatPlayer(player2);
            
            assertThat(table.getSeatPosition(player1)).isEqualTo(0);
            assertThat(table.getSeatPosition(player2)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryTests {

        @Test
        @DisplayName("should find chip leader")
        void shouldFindChipLeader() {
            registerMinimumPlayers();
            tournament.start();
            
            
            TournamentRegistration leader = tournament.getRegistrations().get(0);
            leader.updateChips(5000);
            
            assertThat(tournament.getChipLeader())
                .isPresent()
                .hasValueSatisfying(l -> assertThat(l.getCurrentChips()).isEqualTo(5000));
        }

        @Test
        @DisplayName("should calculate average stack")
        void shouldCalculateAverageStack() {
            registerMinimumPlayers();
            tournament.start();
            
            int avgStack = tournament.getAverageStack();
            
            assertThat(avgStack).isEqualTo(tournament.getStartingChips());
        }

        @Test
        @DisplayName("should find registration by player ID")
        void shouldFindRegistrationByPlayerId() {
            UUID playerId = UUID.randomUUID();
            tournament.registerPlayer(playerId, "Hero");
            
            assertThat(tournament.findRegistration(playerId)).isPresent();
            assertThat(tournament.findRegistration(UUID.randomUUID())).isEmpty();
        }
    }

    

    private void registerMinimumPlayers() {
        for (int i = 0; i < tournament.getMinPlayers(); i++) {
            tournament.registerPlayer(UUID.randomUUID(), "Player" + i);
        }
    }
}
