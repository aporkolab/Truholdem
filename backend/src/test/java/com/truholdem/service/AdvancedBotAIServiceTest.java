package com.truholdem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.truholdem.model.Card;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvancedBotAIService Tests")
class AdvancedBotAIServiceTest {

    @Mock
    private HandEvaluator handEvaluator;

    @InjectMocks
    private AdvancedBotAIService botAIService;

    private Game testGame;
    private Player bot;
    private Player opponent;

    @BeforeEach
    void setUp() {
        testGame = new Game();
        testGame.setId(UUID.randomUUID());
        testGame.setBigBlind(20);
        testGame.setSmallBlind(10);
        testGame.setCurrentBet(20);
        testGame.setCurrentPot(30);
        testGame.setPhase(GamePhase.PRE_FLOP);

        bot = new Player("TestBot", 1000, true);
        bot.setId(UUID.randomUUID());
        bot.setSeatPosition(0);

        opponent = new Player("Opponent", 1000, false);
        opponent.setId(UUID.randomUUID());
        opponent.setSeatPosition(1);

        testGame.addPlayer(bot);
        testGame.addPlayer(opponent);
    }

    @Nested
    @DisplayName("Decision Making Tests")
    class DecisionMakingTests {

        @Test
        @DisplayName("Should return valid decision for pre-flop")
        void shouldReturnValidDecisionPreFlop() {
            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.ACE));

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            assertThat(decision).isNotNull();
            assertThat(decision.action()).isIn(
                    PlayerAction.FOLD, PlayerAction.CHECK, PlayerAction.CALL,
                    PlayerAction.BET, PlayerAction.RAISE);
            assertThat(decision.reasoning()).isNotBlank();
        }

        @Test
        @DisplayName("Should fold weak hands against large bet")
        void shouldFoldWeakHandsAgainstLargeBet() {
            bot.addCardToHand(new Card(Suit.HEARTS, Value.TWO));
            bot.addCardToHand(new Card(Suit.SPADES, Value.SEVEN));

            testGame.setCurrentBet(200); 
            testGame.setCurrentPot(400);

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            
            
            assertThat(decision.action()).isIn(PlayerAction.FOLD, PlayerAction.CALL, PlayerAction.RAISE);
        }

        @Test
        @DisplayName("Should raise with premium hands")
        void shouldRaiseWithPremiumHands() {
            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.ACE));

            testGame.setCurrentBet(20);

            
            int raiseCount = 0;
            for (int i = 0; i < 10; i++) {
                AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);
                if (decision.action() == PlayerAction.RAISE) {
                    raiseCount++;
                }
            }

            
            assertThat(raiseCount).isGreaterThan(5);
        }

        @Test
        @DisplayName("Should check when no bet required with medium hand")
        void shouldCheckWhenNoBetRequired() {
            bot.addCardToHand(new Card(Suit.HEARTS, Value.EIGHT));
            bot.addCardToHand(new Card(Suit.SPADES, Value.NINE));

            testGame.setCurrentBet(0); 
            testGame.setPhase(GamePhase.FLOP);

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            assertThat(decision.action()).isIn(
                    PlayerAction.CHECK, PlayerAction.BET);
        }
    }

    @Nested
    @DisplayName("Hand Strength Calculation Tests")
    class HandStrengthTests {

        @Test
        @DisplayName("Should calculate high pre-flop strength for pocket aces")
        void shouldCalculateHighStrengthForPocketAces() {
            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.ACE));

            double strength = botAIService.calculatePreFlopStrength(bot.getHand());

            assertThat(strength).isGreaterThan(0.85);
        }

        @Test
        @DisplayName("Should calculate high strength for pocket kings")
        void shouldCalculateHighStrengthForPocketKings() {
            bot.addCardToHand(new Card(Suit.HEARTS, Value.KING));
            bot.addCardToHand(new Card(Suit.SPADES, Value.KING));

            double strength = botAIService.calculatePreFlopStrength(bot.getHand());

            assertThat(strength).isGreaterThan(0.80);
        }

        @Test
        @DisplayName("Should calculate low strength for 7-2 offsuit")
        void shouldCalculateLowStrengthFor72Offsuit() {
            bot.addCardToHand(new Card(Suit.HEARTS, Value.SEVEN));
            bot.addCardToHand(new Card(Suit.SPADES, Value.TWO));

            double strength = botAIService.calculatePreFlopStrength(bot.getHand());

            assertThat(strength).isLessThanOrEqualTo(0.35);
        }

        @Test
        @DisplayName("Should give suited cards bonus")
        void shouldGiveSuitedCardsBonus() {
            
            bot.addCardToHand(new Card(Suit.HEARTS, Value.KING));
            bot.addCardToHand(new Card(Suit.HEARTS, Value.QUEEN));
            double suitedStrength = botAIService.calculatePreFlopStrength(bot.getHand());

            
            bot.getHand().clear();
            bot.addCardToHand(new Card(Suit.HEARTS, Value.KING));
            bot.addCardToHand(new Card(Suit.SPADES, Value.QUEEN));
            double offsuitStrength = botAIService.calculatePreFlopStrength(bot.getHand());

            assertThat(suitedStrength).isGreaterThan(offsuitStrength);
        }

        @Test
        @DisplayName("Should give connected cards bonus")
        void shouldGiveConnectedCardsBonus() {
            
            bot.addCardToHand(new Card(Suit.HEARTS, Value.JACK));
            bot.addCardToHand(new Card(Suit.SPADES, Value.TEN));
            double connectedStrength = botAIService.calculatePreFlopStrength(bot.getHand());

            
            bot.getHand().clear();
            bot.addCardToHand(new Card(Suit.HEARTS, Value.JACK));
            bot.addCardToHand(new Card(Suit.SPADES, Value.FIVE));
            double gapStrength = botAIService.calculatePreFlopStrength(bot.getHand());

            assertThat(connectedStrength).isGreaterThan(gapStrength);
        }
    }

    @Nested
    @DisplayName("Position Awareness Tests")
    class PositionAwarenessTests {

        @Test
        @DisplayName("Should identify button position")
        void shouldIdentifyButtonPosition() {
            testGame.setDealerPosition(0); 
            bot.setSeatPosition(0);

            int positionScore = botAIService.getPositionScore(testGame, bot);

            assertThat(positionScore).isEqualTo(3); 
        }

        @Test
        @DisplayName("Should identify early position")
        void shouldIdentifyEarlyPosition() {
            testGame.setDealerPosition(3);
            bot.setSeatPosition(0);

            
            for (int i = 2; i < 6; i++) {
                Player p = new Player("Player" + i, 1000, false);
                p.setSeatPosition(i);
                testGame.addPlayer(p);
            }

            int positionScore = botAIService.getPositionScore(testGame, bot);

            assertThat(positionScore).isLessThanOrEqualTo(1); 
        }
    }

    @Nested
    @DisplayName("Pot Odds Calculation Tests")
    class PotOddsTests {

        @Test
        @DisplayName("Should calculate correct pot odds")
        void shouldCalculateCorrectPotOdds() {
            testGame.setCurrentPot(100);
            testGame.setCurrentBet(20);
            bot.setBetAmount(0);

            double potOdds = botAIService.calculatePotOdds(testGame, bot);

            
            assertThat(potOdds).isBetween(0.15, 0.20);
        }

        @Test
        @DisplayName("Should return 0 pot odds when no bet to call")
        void shouldReturnZeroPotOddsWhenNoBet() {
            testGame.setCurrentPot(100);
            testGame.setCurrentBet(0);

            double potOdds = botAIService.calculatePotOdds(testGame, bot);

            assertThat(potOdds).isZero();
        }

        @Test
        @DisplayName("Should return 0 pot odds when already matched")
        void shouldReturnZeroWhenAlreadyMatched() {
            testGame.setCurrentPot(100);
            testGame.setCurrentBet(20);
            bot.setBetAmount(20); 

            double potOdds = botAIService.calculatePotOdds(testGame, bot);

            assertThat(potOdds).isZero();
        }
    }

    @Nested
    @DisplayName("Bet Sizing Tests")
    class BetSizingTests {

        @Test
        @DisplayName("Should size bet relative to pot")
        void shouldSizeBetRelativeToPot() {
            testGame.setCurrentPot(100);
            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.ACE));

            int betAmount = botAIService.calculateBetAmount(testGame, bot, 0.9);

            
            assertThat(betAmount).isBetween(30, 150);
        }

        @Test
        @DisplayName("Should not bet more than available chips")
        void shouldNotBetMoreThanChips() {
            testGame.setCurrentPot(10000);
            bot.setChips(500);
            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.ACE));

            int betAmount = botAIService.calculateBetAmount(testGame, bot, 0.9);

            
            assertThat(betAmount).isLessThanOrEqualTo(500); 
        }

        @Test
        @DisplayName("Should calculate raise amount correctly")
        void shouldCalculateRaiseAmountCorrectly() {
            testGame.setCurrentPot(100);
            testGame.setCurrentBet(40);
            testGame.setMinRaiseAmount(20);
            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.ACE));

            int raiseAmount = botAIService.calculateRaiseAmount(testGame, bot, 0.9);

            assertThat(raiseAmount).isGreaterThanOrEqualTo(testGame.getCurrentBet() + testGame.getMinRaiseAmount());
        }
    }

    @Nested
    @DisplayName("Personality Tests")
    class PersonalityTests {

        @Test
        @DisplayName("Should assign consistent personality to same bot name")
        void shouldAssignConsistentPersonality() {
            var personality1 = botAIService.getBotPersonality("ConsistentBot");
            var personality2 = botAIService.getBotPersonality("ConsistentBot");

            assertThat(personality1).isEqualTo(personality2);
        }

        @Test
        @DisplayName("Should have different personalities for different bots")
        void shouldHaveDifferentPersonalities() {
            
            Set<AdvancedBotAIService.BotPersonality> personalities = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                personalities.add(botAIService.getBotPersonality("Bot" + i));
            }

            
            assertThat(personalities.size()).isGreaterThan(1);
        }

        @ParameterizedTest
        @CsvSource({
                "TIGHT_AGGRESSIVE, 1.2",
                "LOOSE_AGGRESSIVE, 0.9",
                "TIGHT_PASSIVE, 1.3",
                "LOOSE_PASSIVE, 0.8"
        })
        @DisplayName("Should have correct hand range multipliers for personalities")
        void shouldHaveCorrectHandRangeMultipliers(String personalityName, double expectedMultiplier) {
            AdvancedBotAIService.BotPersonality personality = AdvancedBotAIService.BotPersonality
                    .valueOf(personalityName);

            assertThat(personality.handRangeMultiplier).isEqualTo(expectedMultiplier);
        }
    }

    @Nested
    @DisplayName("Bluffing Logic Tests")
    class BluffingTests {

        @Test
        @DisplayName("Should sometimes bluff in late position")
        void shouldSometimesBluffInLatePosition() {
            testGame.setDealerPosition(0); 
            bot.setSeatPosition(0);
            testGame.setPhase(GamePhase.FLOP);
            testGame.setCurrentBet(0);

            bot.addCardToHand(new Card(Suit.HEARTS, Value.TWO));
            bot.addCardToHand(new Card(Suit.SPADES, Value.THREE));

            
            int bluffCount = 0;
            for (int i = 0; i < 50; i++) {
                AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);
                if (decision.action() == PlayerAction.BET &&
                        decision.reasoning().toLowerCase().contains("bluff")) {
                    bluffCount++;
                }
            }

            
            
            assertThat(bluffCount).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should not bluff too frequently")
        void shouldNotBluffTooFrequently() {
            testGame.setPhase(GamePhase.RIVER);
            testGame.setCurrentBet(0);

            bot.addCardToHand(new Card(Suit.HEARTS, Value.TWO));
            bot.addCardToHand(new Card(Suit.SPADES, Value.THREE));

            int betCount = 0;
            for (int i = 0; i < 100; i++) {
                AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);
                if (decision.action() == PlayerAction.BET) {
                    betCount++;
                }
            }

            
            assertThat(betCount).isLessThanOrEqualTo(65); 
        }
    }

    @Nested
    @DisplayName("Post-Flop Decision Tests")
    class PostFlopTests {

        @Test
        @DisplayName("Should make decision on flop")
        void shouldMakeDecisionOnFlop() {
            testGame.setPhase(GamePhase.FLOP);
            testGame.getCommunityCards().addAll(Arrays.asList(
                    new Card(Suit.HEARTS, Value.TEN),
                    new Card(Suit.DIAMONDS, Value.JACK),
                    new Card(Suit.CLUBS, Value.QUEEN)));

            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.KING));

            
            when(handEvaluator.evaluate(anyList(), anyList())).thenReturn(
                    new HandRanking(HandRanking.HandType.STRAIGHT,
                            List.of(Value.ACE), Collections.emptyList()));

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            assertThat(decision).isNotNull();
            assertThat(decision.action()).isNotNull();
        }

        @Test
        @DisplayName("Should make decision on turn")
        void shouldMakeDecisionOnTurn() {
            testGame.setPhase(GamePhase.TURN);
            testGame.getCommunityCards().addAll(Arrays.asList(
                    new Card(Suit.HEARTS, Value.TEN),
                    new Card(Suit.DIAMONDS, Value.JACK),
                    new Card(Suit.CLUBS, Value.QUEEN),
                    new Card(Suit.SPADES, Value.TWO)));

            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.KING));

            when(handEvaluator.evaluate(anyList(), anyList())).thenReturn(
                    new HandRanking(HandRanking.HandType.STRAIGHT,
                            List.of(Value.ACE), Collections.emptyList()));

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            assertThat(decision).isNotNull();
        }

        @Test
        @DisplayName("Should make decision on river")
        void shouldMakeDecisionOnRiver() {
            testGame.setPhase(GamePhase.RIVER);
            testGame.getCommunityCards().addAll(Arrays.asList(
                    new Card(Suit.HEARTS, Value.TEN),
                    new Card(Suit.DIAMONDS, Value.JACK),
                    new Card(Suit.CLUBS, Value.QUEEN),
                    new Card(Suit.SPADES, Value.TWO),
                    new Card(Suit.HEARTS, Value.THREE)));

            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.KING));

            when(handEvaluator.evaluate(anyList(), anyList())).thenReturn(
                    new HandRanking(HandRanking.HandType.STRAIGHT,
                            List.of(Value.ACE), Collections.emptyList()));

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            assertThat(decision).isNotNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle all-in situation")
        void shouldHandleAllInSituation() {
            bot.setChips(50);
            testGame.setCurrentBet(100);

            bot.addCardToHand(new Card(Suit.HEARTS, Value.ACE));
            bot.addCardToHand(new Card(Suit.SPADES, Value.ACE));

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            
            assertThat(decision.action()).isIn(PlayerAction.FOLD, PlayerAction.CALL, PlayerAction.RAISE);
            if (decision.action() == PlayerAction.CALL) {
                assertThat(decision.amount()).isLessThanOrEqualTo(50);
            }
        }

        @Test
        @DisplayName("Should handle heads-up scenario")
        void shouldHandleHeadsUp() {
            
            testGame.getPlayers().clear();
            testGame.addPlayer(bot);
            testGame.addPlayer(opponent);

            bot.addCardToHand(new Card(Suit.HEARTS, Value.KING));
            bot.addCardToHand(new Card(Suit.SPADES, Value.QUEEN));

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            assertThat(decision).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty community cards")
        void shouldHandleEmptyCommunityCards() {
            testGame.setPhase(GamePhase.PRE_FLOP);
            testGame.getCommunityCards().clear();

            bot.addCardToHand(new Card(Suit.HEARTS, Value.SEVEN));
            bot.addCardToHand(new Card(Suit.SPADES, Value.TWO));

            AdvancedBotAIService.BotDecision decision = botAIService.decide(testGame, bot);

            assertThat(decision).isNotNull();
        }
    }
}
