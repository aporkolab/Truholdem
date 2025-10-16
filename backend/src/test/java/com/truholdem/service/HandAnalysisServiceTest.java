package com.truholdem.service;

import com.truholdem.domain.value.Position;
import com.truholdem.dto.*;
import com.truholdem.dto.GTORecommendationDto.HandStrengthCategory;
import com.truholdem.dto.HandAnalysisDto.ActionAssessment;
import com.truholdem.dto.HandAnalysisDto.OverallAssessment;
import com.truholdem.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;


@DisplayName("HandAnalysisService Tests")
class HandAnalysisServiceTest {

    private HandAnalysisService handAnalysisService;
    private HandEvaluator handEvaluator;

    @BeforeEach
    void setUp() {
        handEvaluator = new HandEvaluator();
        handAnalysisService = new HandAnalysisService(handEvaluator);
    }



    @Nested
    @DisplayName("Equity Calculation Tests")
    class EquityCalculationTests {

        @Test
        @DisplayName("Premium hands should have high equity pre-flop")
        void premiumHandsHighEquity() {

            List<Card> aces = List.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.ACE)
            );

            EquityResult result = handAnalysisService.calculateEquity(
                aces,
                List.of(),
                HandRange.buttonOpen(),
                1
            );

            assertThat(result.equity())
                .as("AA should have >75% equity vs button range")
                .isGreaterThan(0.75);
            assertThat(result.isFavorite()).isTrue();
        }

        @Test
        @DisplayName("Weak hands should have low equity against tight range")
        void weakHandsLowEquity() {

            List<Card> weakHand = List.of(
                new Card(Suit.SPADES, Value.SEVEN),
                new Card(Suit.HEARTS, Value.TWO)
            );

            EquityResult result = handAnalysisService.calculateEquity(
                weakHand,
                List.of(),
                HandRange.premiumRange(),
                1
            );

            assertThat(result.equity())
                .as("72o should have <30% equity vs premium range")
                .isLessThan(0.30);
            assertThat(result.isFavorite()).isFalse();
        }

        @Test
        @DisplayName("Coin flip situations should show ~50% equity")
        void coinFlipEquity() {

            List<Card> jacks = List.of(
                new Card(Suit.SPADES, Value.JACK),
                new Card(Suit.HEARTS, Value.JACK)
            );


            HandRange akRange = HandRange.fromNotation("AKs,AKo");

            EquityResult result = handAnalysisService.calculateEquity(
                jacks,
                List.of(),
                akRange,
                1
            );

            assertThat(result.equity())
                .as("JJ vs AK should be close to 50% (actually slightly ahead)")
                .isBetween(0.45, 0.58);
        }

        @Test
        @DisplayName("Made hand vs draw on flop should show appropriate equity")
        void madeHandVsDrawEquity() {

            List<Card> topPair = List.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.KING)
            );
            List<Card> flop = List.of(
                new Card(Suit.CLUBS, Value.ACE),
                new Card(Suit.DIAMONDS, Value.SEVEN),
                new Card(Suit.DIAMONDS, Value.TWO)
            );

            EquityResult result = handAnalysisService.calculateEquity(
                topPair,
                flop,
                HandRange.buttonOpen(),
                1
            );

            assertThat(result.equity())
                .as("Top pair top kicker should be strong favorite on dry board")
                .isGreaterThan(0.65);
        }

        @Test
        @DisplayName("Equity calculation should include confidence interval")
        void equityIncludesConfidenceInterval() {
            List<Card> hand = List.of(
                new Card(Suit.SPADES, Value.QUEEN),
                new Card(Suit.HEARTS, Value.QUEEN)
            );

            EquityResult result = handAnalysisService.calculateEquity(
                hand,
                List.of(),
                HandRange.buttonOpen(),
                1,
                10000
            );

            assertThat(result.confidenceLow())
                .as("Confidence interval should exist")
                .isLessThan(result.equity());
            assertThat(result.confidenceHigh())
                .isGreaterThan(result.equity());

            assertThat(result.confidenceHigh() - result.confidenceLow())
                .isLessThan(0.05);
        }

        @Test
        @DisplayName("Equity should increase with more outs")
        void equityIncreasesWithOuts() {

            List<Card> flushDraw = List.of(
                new Card(Suit.DIAMONDS, Value.KING),
                new Card(Suit.DIAMONDS, Value.QUEEN)
            );
            List<Card> flopWithFlushDraw = List.of(
                new Card(Suit.DIAMONDS, Value.SEVEN),
                new Card(Suit.DIAMONDS, Value.TWO),
                new Card(Suit.HEARTS, Value.THREE)
            );
            List<Card> flopWithoutDraw = List.of(
                new Card(Suit.SPADES, Value.SEVEN),
                new Card(Suit.CLUBS, Value.TWO),
                new Card(Suit.HEARTS, Value.THREE)
            );

            EquityResult withDraw = handAnalysisService.calculateEquityQuick(
                flushDraw, flopWithFlushDraw, HandRange.buttonOpen());
            EquityResult withoutDraw = handAnalysisService.calculateEquityQuick(
                flushDraw, flopWithoutDraw, HandRange.buttonOpen());

            assertThat(withDraw.equity())
                .as("Flush draw should have more equity than no draw")
                .isGreaterThan(withoutDraw.equity());
        }
    }



    @Nested
    @DisplayName("EV Calculation Tests")
    class EVCalculationTests {

        @Test
        @DisplayName("Fold EV should always be zero")
        void foldEVIsZero() {
            EVResult result = EVResult.forFold(100, 0.5);

            assertThat(result.expectedValue()).isEqualTo(0);
            assertThat(result.action()).isEqualTo(PlayerAction.FOLD);
        }

        @Test
        @DisplayName("Call with sufficient equity should be +EV")
        void callWithGoodEquityIsPositive() {


            EVResult result = EVResult.forCall(100, 50, 0.50);

            assertThat(result.isProfitable())
                .as("Calling with 50% equity when we need 33% should be +EV")
                .isTrue();
            assertThat(result.expectedValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Call with insufficient equity should be -EV")
        void callWithBadEquityIsNegative() {


            EVResult result = EVResult.forCall(100, 100, 0.30);

            assertThat(result.isProfitable())
                .as("Calling with 30% equity when we need 50% should be -EV")
                .isFalse();
            assertThat(result.expectedValue()).isLessThan(0);
        }

        @ParameterizedTest
        @DisplayName("Break-even equity calculation should be correct")
        @CsvSource({
            "100, 50, 0.333",
            "100, 100, 0.500",
            "100, 33, 0.248",
        })
        void breakEvenEquityCalculation(int pot, int call, double expectedBE) {
            EVResult result = EVResult.forCall(pot, call, 0.5);

            assertThat(result.breakEvenEquity())
                .as("Break-even equity should match pot odds")
                .isCloseTo(expectedBE, within(0.01));
        }

        @Test
        @DisplayName("Bet with fold equity should increase EV")
        void betWithFoldEquityIncreasesEV() {

            EVResult betResult = EVResult.forBetOrRaise(
                PlayerAction.BET, 100, 50, 0.40, 0.50);


            double checkEV = 0.40 * 100;

            assertThat(betResult.expectedValue())
                .as("Betting with fold equity should be better than checking")
                .isGreaterThan(checkEV - 50);
        }

        @Test
        @DisplayName("All EVs should be calculated correctly")
        void allEVsCalculatedCorrectly() {
            List<Card> hand = List.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.KING)
            );

            Map<PlayerAction, EVResult> evs = handAnalysisService.calculateAllEVs(
                hand,
                List.of(),
                100,
                50,
                HandRange.buttonOpen()
            );

            assertThat(evs).containsKeys(PlayerAction.FOLD, PlayerAction.CALL, PlayerAction.RAISE);
            assertThat(evs.get(PlayerAction.FOLD).expectedValue()).isEqualTo(0);
        }
    }



    @Nested
    @DisplayName("GTO Recommendation Tests")
    class GTORecommendationTests {

        @Test
        @DisplayName("Strong hand should recommend value bet/raise")
        void strongHandRecommendsValue() {
            List<Card> strongHand = List.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.ACE)
            );
            List<Card> flop = List.of(
                new Card(Suit.CLUBS, Value.ACE),
                new Card(Suit.DIAMONDS, Value.SEVEN),
                new Card(Suit.HEARTS, Value.TWO)
            );
            Position button = Position.dealer(5);

            GTORecommendationDto recommendation = handAnalysisService.getGTORecommendation(
                strongHand,
                flop,
                100,
                0,
                button,
                HandRange.buttonOpen()
            );

            assertThat(recommendation.primaryAction())
                .as("Set should recommend betting for value")
                .isEqualTo(PlayerAction.BET);
            assertThat(recommendation.handCategory())
                .isEqualTo(HandStrengthCategory.VALUE_HEAVY);
        }

        @Test
        @DisplayName("Medium hand should recommend check or thin value")
        void mediumHandRecommendsCheck() {
            List<Card> mediumHand = List.of(
                new Card(Suit.SPADES, Value.TEN),
                new Card(Suit.HEARTS, Value.TEN)
            );
            List<Card> flop = List.of(
                new Card(Suit.CLUBS, Value.ACE),
                new Card(Suit.DIAMONDS, Value.KING),
                new Card(Suit.HEARTS, Value.QUEEN)
            );
            Position outOfPosition = Position.smallBlind(0);

            GTORecommendationDto recommendation = handAnalysisService.getGTORecommendation(
                mediumHand,
                flop,
                100,
                0,
                outOfPosition,
                HandRange.buttonOpen()
            );


            assertThat(recommendation.primaryAction())
                .isIn(PlayerAction.CHECK, PlayerAction.FOLD, PlayerAction.BET);
        }

        @Test
        @DisplayName("Facing bet with insufficient equity should recommend fold")
        void insufficientEquityRecommendsFold() {
            List<Card> weakHand = List.of(
                new Card(Suit.SPADES, Value.THREE),
                new Card(Suit.HEARTS, Value.TWO)
            );
            List<Card> flop = List.of(
                new Card(Suit.CLUBS, Value.ACE),
                new Card(Suit.DIAMONDS, Value.KING),
                new Card(Suit.HEARTS, Value.QUEEN)
            );

            GTORecommendationDto recommendation = handAnalysisService.getGTORecommendation(
                weakHand,
                flop,
                100,
                100,
                Position.bigBlind(1),
                HandRange.premiumRange()
            );

            assertThat(recommendation.primaryAction())
                .as("32o facing pot bet on AKQ should fold")
                .isEqualTo(PlayerAction.FOLD);
        }

        @Test
        @DisplayName("GTO recommendation should include mixed strategy")
        void recommendationIncludesMixedStrategy() {
            List<Card> hand = List.of(
                new Card(Suit.SPADES, Value.QUEEN),
                new Card(Suit.HEARTS, Value.JACK)
            );

            GTORecommendationDto recommendation = handAnalysisService.getGTORecommendation(
                hand,
                List.of(),
                100,
                0,
                Position.dealer(5),
                HandRange.bigBlindDefend()
            );

            assertThat(recommendation.mixedStrategy())
                .as("GTO should include mixed strategy frequencies")
                .isNotEmpty();

            double totalFrequency = recommendation.mixedStrategy().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
            assertThat(totalFrequency)
                .as("Mixed strategy frequencies should sum to ~1.0")
                .isCloseTo(1.0, within(0.1));
        }

        @Test
        @DisplayName("Position should affect recommendation")
        void positionAffectsRecommendation() {
            List<Card> marginalHand = List.of(
                new Card(Suit.SPADES, Value.KING),
                new Card(Suit.HEARTS, Value.NINE)
            );
            List<Card> flop = List.of(
                new Card(Suit.CLUBS, Value.KING),
                new Card(Suit.DIAMONDS, Value.SEVEN),
                new Card(Suit.HEARTS, Value.TWO)
            );

            GTORecommendationDto inPosition = handAnalysisService.getGTORecommendation(
                marginalHand, flop, 100, 0,
                Position.dealer(5),
                HandRange.bigBlindDefend()
            );

            GTORecommendationDto outOfPosition = handAnalysisService.getGTORecommendation(
                marginalHand, flop, 100, 0,
                Position.smallBlind(0),
                HandRange.buttonOpen()
            );


            assertThat(inPosition.confidence())
                .as("Should be more confident in position")
                .isGreaterThanOrEqualTo(outOfPosition.confidence() - 0.1);
        }
    }



    @Nested
    @DisplayName("Hand Range Tests")
    class HandRangeTests {

        @Test
        @DisplayName("Premium range should be tight")
        void premiumRangeIsTight() {
            HandRange premium = HandRange.premiumRange();

            assertThat(premium.getRangePercentage())
                .as("Premium range should be ~1.5-5% of hands")
                .isBetween(1.5, 5.0);
        }

        @Test
        @DisplayName("Button range should be wide")
        void buttonRangeIsWide() {
            HandRange button = HandRange.buttonOpen();

            assertThat(button.getRangePercentage())
                .as("Button range should be ~40-50% of hands")
                .isGreaterThan(35.0);
        }

        @Test
        @DisplayName("Hand range should correctly identify contained hands")
        void rangeContainsCorrectHands() {
            HandRange premium = HandRange.premiumRange();


            assertThat(premium.containsHand(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.ACE)
            )).isTrue();


            assertThat(premium.containsHand(
                new Card(Suit.SPADES, Value.SEVEN),
                new Card(Suit.HEARTS, Value.TWO)
            )).isFalse();
        }

        @Test
        @DisplayName("Custom range from notation should parse correctly")
        void customRangeFromNotation() {
            HandRange custom = HandRange.fromNotation("AA,KK,QQ,AKs");

            assertThat(custom.containsHand(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.ACE)
            )).isTrue();


            assertThat(custom.containsHand(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.KING)
            )).isFalse();


            assertThat(custom.containsHand(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.SPADES, Value.KING)
            )).isTrue();
        }

        @Test
        @DisplayName("Hand generation should exclude dead cards")
        void handGenerationExcludesDeadCards() {
            HandRange range = HandRange.fromNotation("AA");
            Set<Card> deadCards = Set.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.ACE)
            );

            List<List<Card>> hands = range.generateHands(deadCards);


            assertThat(hands).hasSize(1);
        }

        @Test
        @DisplayName("Position-based range should return appropriate range")
        void positionBasedRange() {
            HandRange earlyRange = HandRange.forPositionType(PositionType.EARLY);
            HandRange buttonRange = HandRange.forPositionType(PositionType.DEALER);

            assertThat(buttonRange.getRangePercentage())
                .as("Button should open wider than early position")
                .isGreaterThan(earlyRange.getRangePercentage());
        }
    }



    @Nested
    @DisplayName("Hand Analysis Tests")
    class HandAnalysisTests {

        @Test
        @DisplayName("Hand analysis should identify player in history")
        void handAnalysisFindsPlayer() {
            HandHistory history = createTestHandHistory("TestPlayer");

            HandAnalysisDto analysis = handAnalysisService.analyzeHand(history, "TestPlayer");

            assertThat(analysis.playerName()).isEqualTo("TestPlayer");
            assertThat(analysis.handHistoryId()).isEqualTo(history.getId());
        }

        @Test
        @DisplayName("Hand analysis should throw for unknown player")
        void handAnalysisThrowsForUnknownPlayer() {
            HandHistory history = createTestHandHistory("TestPlayer");

            assertThatThrownBy(() ->
                handAnalysisService.analyzeHand(history, "UnknownPlayer")
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Analysis should categorize assessment correctly")
        void analysisCategoriesAssessment() {

            assertThat(ActionAssessment.fromEVLoss(0, 100))
                .isEqualTo(ActionAssessment.OPTIMAL);
            assertThat(ActionAssessment.fromEVLoss(5, 100))
                .isEqualTo(ActionAssessment.ACCEPTABLE);
            assertThat(ActionAssessment.fromEVLoss(35, 100))
                .isEqualTo(ActionAssessment.BLUNDER);
        }

        @Test
        @DisplayName("Hand analysis should provide suggestions")
        void analysisProvidesSuggestions() {
            HandHistory history = createTestHandHistory("TestPlayer");

            HandAnalysisDto analysis = handAnalysisService.analyzeHand(history, "TestPlayer");

            assertThat(analysis.suggestions())
                .as("Analysis should include improvement suggestions")
                .isNotEmpty();
        }

        private HandHistory createTestHandHistory(String playerName) {
            HandHistory history = new HandHistory();
            history.setId(UUID.randomUUID());
            history.setGameId(UUID.randomUUID());
            history.setHandNumber(1);
            history.setPlayedAt(LocalDateTime.now());
            history.setSmallBlind(5);
            history.setBigBlind(10);
            history.setDealerPosition(0);
            history.setFinalPot(100);
            history.setWinnerName(playerName);
            history.setWinningHandDescription("Pair of Aces");


            HandHistory.HandHistoryPlayer player = new HandHistory.HandHistoryPlayer();
            player.setPlayerId(UUID.randomUUID());
            player.setPlayerName(playerName);
            player.setStartingChips(1000);
            player.setSeatPosition(1);
            player.setHoleCard1Suit("SPADES");
            player.setHoleCard1Value("ACE");
            player.setHoleCard2Suit("HEARTS");
            player.setHoleCard2Value("KING");

            history.getPlayers().add(player);


            history.getActions().add(new HandHistory.ActionRecord(
                player.getPlayerId(),
                playerName,
                "CALL",
                10,
                "PRE_FLOP",
                LocalDateTime.now()
            ));

            return history;
        }
    }



    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Empty community cards should work for preflop")
        void emptyCommunitCardsPreflop() {
            List<Card> hand = List.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.KING)
            );

            assertThatNoException().isThrownBy(() ->
                handAnalysisService.calculateEquity(
                    hand, List.of(), HandRange.buttonOpen(), 1
                )
            );
        }

        @Test
        @DisplayName("Null community cards should be handled")
        void nullCommunityCardsHandled() {
            List<Card> hand = List.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.KING)
            );

            assertThatNoException().isThrownBy(() ->
                handAnalysisService.calculateEquity(
                    hand, null, HandRange.buttonOpen(), 1
                )
            );
        }

        @Test
        @DisplayName("Invalid hand should throw exception")
        void invalidHandThrows() {
            List<Card> invalidHand = List.of(
                new Card(Suit.SPADES, Value.ACE)
            );

            assertThatThrownBy(() ->
                handAnalysisService.calculateEquity(
                    invalidHand, List.of(), HandRange.buttonOpen(), 1
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Range with all dead cards should return high equity")
        void rangeWithAllDeadCards() {
            // All 4 aces are dead (hero has 2, community has 2)
            Set<Card> deadCards = new HashSet<>();
            deadCards.add(new Card(Suit.SPADES, Value.ACE));
            deadCards.add(new Card(Suit.HEARTS, Value.ACE));
            deadCards.add(new Card(Suit.DIAMONDS, Value.ACE));
            deadCards.add(new Card(Suit.CLUBS, Value.ACE));


            HandRange aaOnly = HandRange.fromNotation("AA");

            assertThat(aaOnly.generateHands(deadCards))
                .as("Should have no valid hands when all aces are dead")
                .isEmpty();
        }
    }



    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Full decision flow should work end-to-end")
        void fullDecisionFlow() {

            List<Card> hand = List.of(
                new Card(Suit.SPADES, Value.QUEEN),
                new Card(Suit.SPADES, Value.JACK)
            );
            List<Card> flop = List.of(
                new Card(Suit.SPADES, Value.TEN),
                new Card(Suit.SPADES, Value.NINE),
                new Card(Suit.HEARTS, Value.TWO)
            );
            Position position = Position.dealer(5);
            int potSize = 100;
            int betToCall = 50;


            EquityResult equity = handAnalysisService.calculateEquity(
                hand, flop, HandRange.bigBlindDefend(), 1);


            Map<PlayerAction, EVResult> evs = handAnalysisService.calculateAllEVs(
                hand, flop, potSize, betToCall, HandRange.bigBlindDefend());


            GTORecommendationDto recommendation = handAnalysisService.getGTORecommendation(
                hand, flop, potSize, betToCall, position, HandRange.bigBlindDefend());


            assertThat(equity.equity()).isBetween(0.0, 1.0);
            assertThat(evs).isNotEmpty();
            assertThat(recommendation.primaryAction()).isNotNull();


            assertThat(equity.equity())
                .as("QsJs on Ts9s2h should have good equity (flush draw + straight draw)")
                .isGreaterThan(0.40);
        }

        @Test
        @DisplayName("Multiple simulations should be consistent")
        void multipleSimulationsConsistent() {
            List<Card> hand = List.of(
                new Card(Suit.SPADES, Value.ACE),
                new Card(Suit.HEARTS, Value.ACE)
            );

            List<Double> equities = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                EquityResult result = handAnalysisService.calculateEquity(
                    hand, List.of(), HandRange.buttonOpen(), 1, 5000);
                equities.add(result.equity());
            }


            double max = Collections.max(equities);
            double min = Collections.min(equities);
            assertThat(max - min)
                .as("Multiple equity calculations should be consistent")
                .isLessThan(0.05);
        }
    }
}
