package com.truholdem.domain.value;

import com.truholdem.model.GamePhase;
import com.truholdem.model.HandRanking;
import com.truholdem.model.HandType;
import com.truholdem.model.PositionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Domain Value Objects")
class ValueObjectsTest {





    @Nested
    @DisplayName("Chips Value Object")
    class ChipsTests {

        @Test
        @DisplayName("should create chips with valid amount")
        void shouldCreateChipsWithValidAmount() {
            Chips chips = Chips.of(100);

            assertEquals(100, chips.amount());
        }

        @Test
        @DisplayName("should create zero chips")
        void shouldCreateZeroChips() {
            Chips chips = Chips.zero();

            assertEquals(0, chips.amount());
            assertTrue(chips.isZero());
            assertFalse(chips.hasChips());
        }

        @Test
        @DisplayName("should reject negative chips")
        void shouldRejectNegativeChips() {
            assertThrows(IllegalArgumentException.class, () -> Chips.of(-1));
            assertThrows(IllegalArgumentException.class, () -> new Chips(-100));
        }

        @Test
        @DisplayName("should add chips correctly")
        void shouldAddChipsCorrectly() {
            Chips a = Chips.of(100);
            Chips b = Chips.of(50);

            Chips result = a.add(b);

            assertEquals(150, result.amount());

            assertEquals(100, a.amount());
            assertEquals(50, b.amount());
        }

        @Test
        @DisplayName("should subtract chips correctly")
        void shouldSubtractChipsCorrectly() {
            Chips a = Chips.of(100);
            Chips b = Chips.of(30);

            Chips result = a.subtract(b);

            assertEquals(70, result.amount());
        }

        @Test
        @DisplayName("should throw when subtraction results in negative")
        void shouldThrowWhenSubtractionResultsInNegative() {
            Chips a = Chips.of(50);
            Chips b = Chips.of(100);

            assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
        }

        @Test
        @DisplayName("should subtract or return zero")
        void shouldSubtractOrReturnZero() {
            Chips a = Chips.of(50);
            Chips b = Chips.of(100);

            Chips result = a.subtractOrZero(b);

            assertEquals(0, result.amount());
        }

        @Test
        @DisplayName("should compare chips correctly")
        void shouldCompareChipsCorrectly() {
            Chips small = Chips.of(50);
            Chips large = Chips.of(100);
            Chips equal = Chips.of(50);

            assertTrue(large.isGreaterThan(small));
            assertFalse(small.isGreaterThan(large));
            assertFalse(small.isGreaterThan(equal));

            assertTrue(small.isLessThan(large));
            assertTrue(small.canAfford(equal));
            assertFalse(small.canAfford(large));
        }

        @Test
        @DisplayName("should find min and max")
        void shouldFindMinAndMax() {
            Chips a = Chips.of(100);
            Chips b = Chips.of(50);

            assertEquals(50, a.min(b).amount());
            assertEquals(100, a.max(b).amount());
        }

        @Test
        @DisplayName("should multiply correctly")
        void shouldMultiplyCorrectly() {
            Chips chips = Chips.of(50);

            assertEquals(150, chips.multiply(3).amount());
            assertEquals(0, chips.multiply(0).amount());
        }

        @Test
        @DisplayName("should reject negative multiplier")
        void shouldRejectNegativeMultiplier() {
            Chips chips = Chips.of(50);

            assertThrows(IllegalArgumentException.class, () -> chips.multiply(-1));
        }

        @Test
        @DisplayName("should calculate percentage")
        void shouldCalculatePercentage() {
            Chips chips = Chips.of(100);

            assertEquals(50, chips.percentage(50).amount());
            assertEquals(25, chips.percentage(25).amount());
            assertEquals(0, chips.percentage(0).amount());
            assertEquals(100, chips.percentage(100).amount());
        }

        @Test
        @DisplayName("should implement Comparable correctly")
        void shouldImplementComparableCorrectly() {
            Chips a = Chips.of(100);
            Chips b = Chips.of(50);
            Chips c = Chips.of(100);

            assertTrue(a.compareTo(b) > 0);
            assertTrue(b.compareTo(a) < 0);
            assertEquals(0, a.compareTo(c));
        }

        @Test
        @DisplayName("should have correct equality")
        void shouldHaveCorrectEquality() {
            Chips a = Chips.of(100);
            Chips b = Chips.of(100);
            Chips c = Chips.of(50);

            assertEquals(a, b);
            assertNotEquals(a, c);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("should reject null in operations")
        void shouldRejectNullInOperations() {
            Chips chips = Chips.of(100);

            assertThrows(NullPointerException.class, () -> chips.add(null));
            assertThrows(NullPointerException.class, () -> chips.subtract(null));
            assertThrows(NullPointerException.class, () -> chips.min(null));
            assertThrows(NullPointerException.class, () -> chips.canAfford(null));
        }
    }





    @Nested
    @DisplayName("Pot Value Object")
    class PotTests {

        private final UUID player1 = UUID.randomUUID();
        private final UUID player2 = UUID.randomUUID();
        private final UUID player3 = UUID.randomUUID();

        @Test
        @DisplayName("should create empty main pot")
        void shouldCreateEmptyMainPot() {
            Pot pot = Pot.emptyMain();

            assertTrue(pot.isEmpty());
            assertTrue(pot.isMain());
            assertFalse(pot.isSide());
            assertEquals(0, pot.eligiblePlayerCount());
        }

        @Test
        @DisplayName("should create main pot with players")
        void shouldCreateMainPotWithPlayers() {
            Set<UUID> players = Set.of(player1, player2);
            Pot pot = Pot.main(Chips.of(100), players);

            assertEquals(100, pot.amount().amount());
            assertTrue(pot.isMain());
            assertTrue(pot.isPlayerEligible(player1));
            assertTrue(pot.isPlayerEligible(player2));
            assertFalse(pot.isPlayerEligible(player3));
        }

        @Test
        @DisplayName("should create side pot")
        void shouldCreateSidePot() {
            Set<UUID> players = Set.of(player1, player2);
            Pot pot = Pot.side(Chips.of(50), players);

            assertTrue(pot.isSide());
            assertFalse(pot.isMain());
            assertEquals(Pot.PotType.SIDE, pot.type());
        }

        @Test
        @DisplayName("should add contribution to pot")
        void shouldAddContributionToPot() {
            Pot pot = Pot.main(Chips.of(100), Set.of(player1));

            Pot updated = pot.addContribution(Chips.of(50));

            assertEquals(150, updated.amount().amount());

            assertEquals(100, pot.amount().amount());
        }

        @Test
        @DisplayName("should add player contribution and eligibility")
        void shouldAddPlayerContributionAndEligibility() {
            Pot pot = Pot.main(Chips.of(100), Set.of(player1));

            Pot updated = pot.addPlayerContribution(Chips.of(50), player2);

            assertEquals(150, updated.amount().amount());
            assertTrue(updated.isPlayerEligible(player1));
            assertTrue(updated.isPlayerEligible(player2));
        }

        @Test
        @DisplayName("should remove player from eligibility")
        void shouldRemovePlayerFromEligibility() {
            Pot pot = Pot.main(Chips.of(100), Set.of(player1, player2));

            Pot updated = pot.removePlayer(player1);

            assertFalse(updated.isPlayerEligible(player1));
            assertTrue(updated.isPlayerEligible(player2));
        }

        @Test
        @DisplayName("should calculate split share")
        void shouldCalculateSplitShare() {
            Pot pot = Pot.main(Chips.of(100), Set.of(player1, player2));

            assertEquals(50, pot.splitShare(2).amount());
            assertEquals(33, pot.splitShare(3).amount());
            assertEquals(100, pot.splitShare(1).amount());
        }

        @Test
        @DisplayName("should calculate split remainder")
        void shouldCalculateSplitRemainder() {
            Pot pot = Pot.main(Chips.of(100), Set.of(player1, player2, player3));

            assertEquals(1, pot.splitRemainder(3).amount());
            assertEquals(0, pot.splitRemainder(2).amount());
            assertEquals(0, pot.splitRemainder(1).amount());
        }

        @Test
        @DisplayName("should reject invalid split winner count")
        void shouldRejectInvalidSplitWinnerCount() {
            Pot pot = Pot.main(Chips.of(100), Set.of(player1));

            assertThrows(IllegalArgumentException.class, () -> pot.splitShare(0));
            assertThrows(IllegalArgumentException.class, () -> pot.splitShare(-1));
        }

        @Test
        @DisplayName("should have immutable eligible players set")
        void shouldHaveImmutableEligiblePlayersSet() {
            Set<UUID> players = Set.of(player1, player2);
            Pot pot = Pot.main(Chips.of(100), players);

            assertThrows(UnsupportedOperationException.class,
                () -> pot.eligiblePlayerIds().add(player3));
        }

        @Test
        @DisplayName("should handle null eligible players")
        void shouldHandleNullEligiblePlayers() {
            Pot pot = new Pot(Chips.of(100), null, Pot.PotType.MAIN);

            assertNotNull(pot.eligiblePlayerIds());
            assertTrue(pot.eligiblePlayerIds().isEmpty());
        }
    }





    @Nested
    @DisplayName("BettingRound Value Object")
    class BettingRoundTests {

        private final UUID player1 = UUID.randomUUID();
        private final UUID player2 = UUID.randomUUID();

        @Test
        @DisplayName("should start pre-flop round with big blind as current bet")
        void shouldStartPreFlopRoundWithBigBlindAsCurrentBet() {
            BettingRound round = BettingRound.start(GamePhase.PRE_FLOP, Chips.of(20));

            assertEquals(GamePhase.PRE_FLOP, round.phase());
            assertEquals(20, round.currentBet().amount());
            assertEquals(20, round.minRaise().amount());
            assertEquals(0, round.actionsThisRound());
            assertNull(round.lastAggressor());
        }

        @Test
        @DisplayName("should start post-flop round with zero bet")
        void shouldStartPostFlopRoundWithZeroBet() {
            BettingRound round = BettingRound.startPostFlop(GamePhase.FLOP, Chips.of(20));

            assertEquals(GamePhase.FLOP, round.phase());
            assertEquals(0, round.currentBet().amount());
            assertEquals(20, round.minRaise().amount());
        }

        @Test
        @DisplayName("should record bet and update state")
        void shouldRecordBetAndUpdateState() {
            BettingRound round = BettingRound.startPostFlop(GamePhase.FLOP, Chips.of(20));

            BettingRound updated = round.withBet(Chips.of(50), player1);

            assertEquals(50, updated.currentBet().amount());
            assertEquals(1, updated.actionsThisRound());
            assertEquals(player1, updated.lastAggressor());

            assertEquals(0, round.currentBet().amount());
        }

        @Test
        @DisplayName("should record raise and update minimum raise")
        void shouldRecordRaiseAndUpdateMinimumRaise() {
            BettingRound round = BettingRound.start(GamePhase.PRE_FLOP, Chips.of(20));


            BettingRound raised = round.withRaise(Chips.of(60), Chips.of(40), player1);

            assertEquals(60, raised.currentBet().amount());
            assertEquals(40, raised.minRaise().amount());
            assertEquals(player1, raised.lastAggressor());
        }

        @Test
        @DisplayName("should calculate amount to call")
        void shouldCalculateAmountToCall() {
            BettingRound round = BettingRound.start(GamePhase.PRE_FLOP, Chips.of(20))
                .withRaise(Chips.of(60), Chips.of(40), player1);

            assertEquals(60, round.amountToCall(Chips.zero()).amount());
            assertEquals(40, round.amountToCall(Chips.of(20)).amount());
            assertEquals(0, round.amountToCall(Chips.of(60)).amount());
        }

        @Test
        @DisplayName("should calculate minimum raise total")
        void shouldCalculateMinimumRaiseTotal() {
            BettingRound round = BettingRound.start(GamePhase.PRE_FLOP, Chips.of(20))
                .withRaise(Chips.of(60), Chips.of(40), player1);


            assertEquals(100, round.minimumRaiseTotal().amount());
        }

        @Test
        @DisplayName("should validate raise amounts")
        void shouldValidateRaiseAmounts() {
            BettingRound round = BettingRound.start(GamePhase.PRE_FLOP, Chips.of(20));

            assertTrue(round.isValidRaise(Chips.of(20)));
            assertTrue(round.isValidRaise(Chips.of(50)));
            assertFalse(round.isValidRaise(Chips.of(10)));
        }

        @Test
        @DisplayName("should check round completion")
        void shouldCheckRoundCompletion() {
            BettingRound round = BettingRound.start(GamePhase.PRE_FLOP, Chips.of(20));

            assertFalse(round.isComplete(3, 0));
            assertFalse(round.isComplete(3, 2));
            assertTrue(round.isComplete(3, 3));
            assertTrue(round.isComplete(3, 4));
        }

        @Test
        @DisplayName("should track betting action state")
        void shouldTrackBettingActionState() {
            BettingRound round = BettingRound.startPostFlop(GamePhase.FLOP, Chips.of(20));

            assertTrue(round.isFirstAction());
            assertFalse(round.hasBettingAction());
            assertFalse(round.hasAggressor());

            BettingRound afterBet = round.withBet(Chips.of(50), player1);

            assertFalse(afterBet.isFirstAction());
            assertTrue(afterBet.hasBettingAction());
            assertTrue(afterBet.hasAggressor());
        }

        @Test
        @DisplayName("should reject invalid construction")
        void shouldRejectInvalidConstruction() {
            assertThrows(NullPointerException.class,
                () -> new BettingRound(null, Chips.of(20), Chips.of(20), 0, null));
            assertThrows(NullPointerException.class,
                () -> new BettingRound(GamePhase.FLOP, null, Chips.of(20), 0, null));
            assertThrows(IllegalArgumentException.class,
                () -> new BettingRound(GamePhase.FLOP, Chips.of(20), Chips.of(20), -1, null));
        }
    }





    @Nested
    @DisplayName("Position Value Object")
    class PositionTests {

        @Test
        @DisplayName("should calculate dealer position")
        void shouldCalculateDealerPosition() {
            Position pos = Position.calculate(0, 0, 6);

            assertEquals(PositionType.DEALER, pos.type());
            assertTrue(pos.isDealer());
            assertTrue(pos.isInPosition());
        }

        @Test
        @DisplayName("should calculate small blind position")
        void shouldCalculateSmallBlindPosition() {
            Position pos = Position.calculate(1, 0, 6);

            assertEquals(PositionType.SMALL_BLIND, pos.type());
            assertTrue(pos.isSmallBlind());
            assertTrue(pos.isBlind());
            assertTrue(pos.isEarlyPosition());
        }

        @Test
        @DisplayName("should calculate big blind position")
        void shouldCalculateBigBlindPosition() {
            Position pos = Position.calculate(2, 0, 6);

            assertEquals(PositionType.BIG_BLIND, pos.type());
            assertTrue(pos.isBigBlind());
            assertTrue(pos.isBlind());
        }

        @ParameterizedTest
        @CsvSource({
            "0, 0, 6, DEALER",
            "1, 0, 6, SMALL_BLIND",
            "2, 0, 6, BIG_BLIND",
            "5, 0, 6, CUTOFF",
            "3, 0, 9, EARLY",
            "5, 0, 9, MIDDLE",
        })
        @DisplayName("should calculate various positions correctly")
        void shouldCalculateVariousPositionsCorrectly(
            int seatNumber, int dealerPosition, int totalPlayers,
            PositionType expectedType) {

            Position pos = Position.calculate(seatNumber, dealerPosition, totalPlayers);

            assertEquals(expectedType, pos.type());
        }

        @Test
        @DisplayName("should handle wraparound positions")
        void shouldHandleWraparoundPositions() {

            Position dealer = Position.calculate(4, 4, 6);
            Position smallBlind = Position.calculate(5, 4, 6);
            Position bigBlind = Position.calculate(0, 4, 6);

            assertEquals(PositionType.DEALER, dealer.type());
            assertEquals(PositionType.SMALL_BLIND, smallBlind.type());
            assertEquals(PositionType.BIG_BLIND, bigBlind.type());
        }

        @Test
        @DisplayName("should create positions with factory methods")
        void shouldCreatePositionsWithFactoryMethods() {
            Position dealer = Position.dealer(0);
            Position sb = Position.smallBlind(1);
            Position bb = Position.bigBlind(2);

            assertTrue(dealer.isDealer());
            assertTrue(sb.isSmallBlind());
            assertTrue(bb.isBigBlind());
        }

        @Test
        @DisplayName("should compare positions by strategic value")
        void shouldComparePositionsByStrategicValue() {
            Position sb = Position.smallBlind(0);
            Position bb = Position.bigBlind(1);
            Position btn = Position.dealer(5);

            assertTrue(btn.hasAdvantageOver(sb));
            assertTrue(btn.hasAdvantageOver(bb));
            assertTrue(btn.compareTo(sb) > 0);
        }

        @Test
        @DisplayName("should have correct position values")
        void shouldHaveCorrectPositionValues() {
            assertEquals(1, PositionType.SMALL_BLIND.getPositionValue());
            assertEquals(2, PositionType.BIG_BLIND.getPositionValue());
            assertEquals(6, PositionType.DEALER.getPositionValue());
        }

        @Test
        @DisplayName("should reject invalid inputs")
        void shouldRejectInvalidInputs() {
            assertThrows(IllegalArgumentException.class,
                () -> Position.calculate(-1, 0, 6));
            assertThrows(IllegalArgumentException.class,
                () -> Position.calculate(0, -1, 6));
            assertThrows(IllegalArgumentException.class,
                () -> Position.calculate(0, 0, 1));
        }

        @Test
        @DisplayName("should have correct abbreviations")
        void shouldHaveCorrectAbbreviations() {
            assertEquals("BTN", PositionType.DEALER.getAbbreviation());
            assertEquals("SB", PositionType.SMALL_BLIND.getAbbreviation());
            assertEquals("BB", PositionType.BIG_BLIND.getAbbreviation());
            assertEquals("CO", PositionType.CUTOFF.getAbbreviation());
        }
    }





    @Nested
    @DisplayName("HandStrength Value Object")
    class HandStrengthTests {

        @Test
        @DisplayName("should create hand strength with valid equity")
        void shouldCreateHandStrengthWithValidEquity() {
            HandStrength hs = new HandStrength(0.75,
                HandType.TWO_PAIR, "Two Pair (Aces and Kings)");

            assertEquals(0.75, hs.equity());
            assertEquals(HandType.TWO_PAIR, hs.ranking());
            assertEquals("Two Pair (Aces and Kings)", hs.description());
        }

        @Test
        @DisplayName("should reject invalid equity values")
        void shouldRejectInvalidEquityValues() {
            assertThrows(IllegalArgumentException.class,
                () -> new HandStrength(-0.1, HandType.HIGH_CARD, "Test"));
            assertThrows(IllegalArgumentException.class,
                () -> new HandStrength(1.1, HandType.HIGH_CARD, "Test"));
        }

        @Test
        @DisplayName("should categorize hand strength correctly")
        void shouldCategorizeHandStrengthCorrectly() {
            HandStrength premium = new HandStrength(0.85,
                HandType.FULL_HOUSE, "Full House");
            HandStrength strong = new HandStrength(0.65,
                HandType.TWO_PAIR, "Two Pair");
            HandStrength medium = new HandStrength(0.45,
                HandType.ONE_PAIR, "One Pair");
            HandStrength weak = new HandStrength(0.20,
                HandType.HIGH_CARD, "High Card");

            assertTrue(premium.isPremium());
            assertTrue(premium.isStrong());
            assertFalse(premium.isWeak());

            assertFalse(strong.isPremium());
            assertTrue(strong.isStrong());

            assertFalse(medium.isStrong());
            assertTrue(medium.isMedium());

            assertTrue(weak.isWeak());
            assertFalse(weak.isPlayable());
        }

        @Test
        @DisplayName("should check pot odds correctly")
        void shouldCheckPotOddsCorrectly() {
            HandStrength hs = new HandStrength(0.35,
                HandType.ONE_PAIR, "One Pair");

            assertTrue(hs.hasPotOdds(0.25));
            assertFalse(hs.hasPotOdds(0.40));
        }

        @Test
        @DisplayName("should calculate pot odds")
        void shouldCalculatePotOdds() {
            double potOdds = HandStrength.calculatePotOdds(
                Chips.of(100), Chips.of(25));

            assertEquals(0.2, potOdds, 0.001);
        }

        @Test
        @DisplayName("should compare hand strengths correctly")
        void shouldCompareHandStrengthsCorrectly() {
            HandStrength stronger = new HandStrength(0.80,
                HandType.FLUSH, "Flush");
            HandStrength weaker = new HandStrength(0.50,
                HandType.ONE_PAIR, "One Pair");

            assertTrue(stronger.beats(weaker));
            assertFalse(weaker.beats(stronger));
            assertTrue(stronger.compareTo(weaker) > 0);
        }

        @Test
        @DisplayName("should identify made hands and monsters")
        void shouldIdentifyMadeHandsAndMonsters() {
            HandStrength highCard = HandStrength.of(
                HandType.HIGH_CARD, 0.25, "High Card");
            HandStrength pair = HandStrength.of(
                HandType.ONE_PAIR, 0.45, "One Pair");
            HandStrength fullHouse = HandStrength.of(
                HandType.FULL_HOUSE, 0.95, "Full House");

            assertFalse(highCard.isMadeHand());
            assertTrue(pair.isMadeHand());
            assertTrue(fullHouse.isMadeHand());

            assertFalse(pair.isMonster());
            assertTrue(fullHouse.isMonster());
        }

        @Test
        @DisplayName("should get strength category")
        void shouldGetStrengthCategory() {
            assertEquals("Premium",
                new HandStrength(0.85, HandType.FULL_HOUSE, "FH").getStrengthCategory());
            assertEquals("Strong",
                new HandStrength(0.65, HandType.FLUSH, "F").getStrengthCategory());
            assertEquals("Medium",
                new HandStrength(0.45, HandType.TWO_PAIR, "2P").getStrengthCategory());
            assertEquals("Marginal",
                new HandStrength(0.30, HandType.ONE_PAIR, "P").getStrengthCategory());
            assertEquals("Weak",
                new HandStrength(0.20, HandType.HIGH_CARD, "HC").getStrengthCategory());
        }

        @Test
        @DisplayName("should format equity percentage")
        void shouldFormatEquityPercentage() {
            HandStrength hs = new HandStrength(0.654,
                HandType.TWO_PAIR, "Two Pair");

            assertEquals("65.4%", hs.getEquityPercentage());
        }

        @Test
        @DisplayName("should create from HandRanking")
        void shouldCreateFromHandRanking() {
            HandRanking ranking = new HandRanking(
                HandType.FLUSH,
                Collections.emptyList(),
                Collections.emptyList()
            );

            HandStrength hs = HandStrength.fromRanking(ranking);

            assertEquals(HandType.FLUSH, hs.ranking());
            assertEquals(0.85, hs.equity(), 0.01);
        }
    }





    @Nested
    @DisplayName("Edge Cases and Integration")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle zero chips in all operations")
        void shouldHandleZeroChipsInAllOperations() {
            Chips zero = Chips.zero();
            Chips hundred = Chips.of(100);

            assertEquals(hundred, zero.add(hundred));
            assertEquals(zero, hundred.subtract(hundred));
            assertTrue(zero.isZero());
            assertTrue(zero.canAfford(zero));
            assertFalse(zero.canAfford(hundred));
        }

        @Test
        @DisplayName("should maintain immutability in pot operations")
        void shouldMaintainImmutabilityInPotOperations() {
            UUID player = UUID.randomUUID();
            Pot original = Pot.emptyMain();

            Pot after1 = original.addContribution(Chips.of(50));
            Pot after2 = after1.addPlayerContribution(Chips.of(50), player);

            assertTrue(original.isEmpty());
            assertEquals(50, after1.amount().amount());
            assertEquals(100, after2.amount().amount());
            assertFalse(original.isPlayerEligible(player));
            assertFalse(after1.isPlayerEligible(player));
            assertTrue(after2.isPlayerEligible(player));
        }

        @Test
        @DisplayName("should handle heads-up position calculation")
        void shouldHandleHeadsUpPositionCalculation() {

            Position pos1 = Position.calculate(0, 0, 2);
            Position pos2 = Position.calculate(1, 0, 2);


            assertTrue(pos1.isDealer() || pos1.isSmallBlind());
            assertTrue(pos2.isBigBlind() || pos2.isSmallBlind());
        }

        @Test
        @DisplayName("should handle betting round state transitions")
        void shouldHandleBettingRoundStateTransitions() {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();

            BettingRound round = BettingRound.startPostFlop(GamePhase.FLOP, Chips.of(20))
                .withBet(Chips.of(50), player1)
                .withRaise(Chips.of(120), Chips.of(70), player2)
                .withAction();

            assertEquals(120, round.currentBet().amount());
            assertEquals(70, round.minRaise().amount());
            assertEquals(player2, round.lastAggressor());
            assertEquals(3, round.actionsThisRound());
        }

        @Test
        @DisplayName("should handle boundary equity values")
        void shouldHandleBoundaryEquityValues() {
            HandStrength zero = new HandStrength(0.0,
                HandType.HIGH_CARD, "Zero equity");
            HandStrength one = new HandStrength(1.0,
                HandType.ROYAL_FLUSH, "Max equity");

            assertTrue(zero.isWeak());
            assertTrue(one.isPremium());
        }
    }
}
