package com.truholdem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.truholdem.model.Card;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.HandRanking;
import com.truholdem.model.HandType;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;
import com.truholdem.service.AdvancedBotAIService.BotDecision;
import com.truholdem.service.AdvancedBotAIService.BotPersonality;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvancedBotAIService Tests")
class AdvancedBotAIServiceTest {

    @Mock
    private HandEvaluator handEvaluator;

    private AdvancedBotAIService botAIService;

    @BeforeEach
    void setUp() {
        botAIService = new AdvancedBotAIService(handEvaluator);
    }

    
    
    

    private List<Card> createHand(Value v1, Suit s1, Value v2, Suit s2) {
        List<Card> hand = new ArrayList<>();
        hand.add(new Card(s1, v1));
        hand.add(new Card(s2, v2));
        return hand;
    }

    private Game createGameWithPlayers(int count) {
        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setBigBlind(20);
        game.setSmallBlind(10);
        game.setCurrentPot(30);
        game.setCurrentBet(20);
        game.setMinRaiseAmount(20);
        game.setDealerPosition(0);
        game.setPhase(GamePhase.PRE_FLOP);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player p = new Player("Player" + (i + 1), 1000, true);
            p.setId(UUID.randomUUID());
            p.setSeatPosition(i);
            players.add(p);
        }
        game.setPlayers(players);
        return game;
    }

    
    
    

    @Nested
    @DisplayName("1. Hand Strength Tests")
    class HandStrengthTests {

        @Nested
        @DisplayName("1.1 Pre-Flop Strength")
        class PreFlopStrengthTests {

            @Test
            @DisplayName("AA should have highest pre-flop strength (0.85+)")
            void aaShouldHaveHighestPreFlopStrength() {
                List<Card> hand = createHand(Value.ACE, Suit.HEARTS, Value.ACE, Suit.SPADES);
                
                double strength = botAIService.calculatePreFlopStrength(hand);
                
                assertTrue(strength >= 0.85, "AA should be >= 0.85, was: " + strength);
            }

            @Test
            @DisplayName("KK should be second highest pre-flop strength")
            void kkShouldBeSecondHighestPreFlopStrength() {
                List<Card> aa = createHand(Value.ACE, Suit.HEARTS, Value.ACE, Suit.SPADES);
                List<Card> kk = createHand(Value.KING, Suit.HEARTS, Value.KING, Suit.SPADES);
                
                double aaStrength = botAIService.calculatePreFlopStrength(aa);
                double kkStrength = botAIService.calculatePreFlopStrength(kk);
                
                assertTrue(kkStrength >= 0.85, "KK should be >= 0.85");
                assertTrue(aaStrength > kkStrength, "AA should beat KK");
            }

            @Test
            @DisplayName("QQ should be strong premium hand")
            void qqShouldBeStrongPremiumHand() {
                List<Card> hand = createHand(Value.QUEEN, Suit.HEARTS, Value.QUEEN, Suit.SPADES);
                
                double strength = botAIService.calculatePreFlopStrength(hand);
                
                assertTrue(strength >= 0.85, "QQ should be >= 0.85, was: " + strength);
            }

            @Test
            @DisplayName("JJ should be premium hand")
            void jjShouldBePremiumHand() {
                List<Card> hand = createHand(Value.JACK, Suit.HEARTS, Value.JACK, Suit.SPADES);
                
                double strength = botAIService.calculatePreFlopStrength(hand);
                
                assertTrue(strength >= 0.85, "JJ should be >= 0.85, was: " + strength);
            }

            @Test
            @DisplayName("AKs should be stronger than AKo")
            void aksShouldBeStrongerThanAko() {
                List<Card> aks = createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.HEARTS);
                List<Card> ako = createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.SPADES);
                
                double aksStrength = botAIService.calculatePreFlopStrength(aks);
                double akoStrength = botAIService.calculatePreFlopStrength(ako);
                
                assertTrue(aksStrength > akoStrength, 
                        "AKs (" + aksStrength + ") should beat AKo (" + akoStrength + ")");
            }

            @Test
            @DisplayName("Suited connectors should have reasonable strength")
            void suitedConnectorsShouldHaveReasonableStrength() {
                List<Card> t9s = createHand(Value.TEN, Suit.HEARTS, Value.NINE, Suit.HEARTS);
                
                double strength = botAIService.calculatePreFlopStrength(t9s);
                
                assertTrue(strength >= 0.40 && strength <= 0.70, 
                        "Suited connectors should be 0.40-0.70, was: " + strength);
            }

            @Test
            @DisplayName("72o should be worst hand (lowest strength)")
            void sevenTwoOffShouldBeWorstHand() {
                List<Card> hand = createHand(Value.SEVEN, Suit.HEARTS, Value.TWO, Suit.SPADES);
                
                double strength = botAIService.calculatePreFlopStrength(hand);
                
                assertTrue(strength < 0.40, "72o should be < 0.40, was: " + strength);
            }

            @Test
            @DisplayName("Mid pairs should be medium strength")
            void midPairsShouldBeMediumStrength() {
                List<Card> sevens = createHand(Value.SEVEN, Suit.HEARTS, Value.SEVEN, Suit.SPADES);
                List<Card> eights = createHand(Value.EIGHT, Suit.HEARTS, Value.EIGHT, Suit.SPADES);
                
                double sevenStrength = botAIService.calculatePreFlopStrength(sevens);
                double eightStrength = botAIService.calculatePreFlopStrength(eights);
                
                assertTrue(sevenStrength >= 0.50 && sevenStrength <= 0.75);
                assertTrue(eightStrength > sevenStrength, "88 should beat 77");
            }

            @Test
            @DisplayName("Small pairs should be weaker than big pairs")
            void smallPairsShouldBeWeakerThanBigPairs() {
                List<Card> twos = createHand(Value.TWO, Suit.HEARTS, Value.TWO, Suit.SPADES);
                List<Card> jacks = createHand(Value.JACK, Suit.HEARTS, Value.JACK, Suit.SPADES);
                
                double twoStrength = botAIService.calculatePreFlopStrength(twos);
                double jackStrength = botAIService.calculatePreFlopStrength(jacks);
                
                assertTrue(jackStrength > twoStrength, "JJ should beat 22");
            }

            @Test
            @DisplayName("TT should be solid hand")
            void ttShouldBeSolidHand() {
                List<Card> hand = createHand(Value.TEN, Suit.HEARTS, Value.TEN, Suit.SPADES);
                
                double strength = botAIService.calculatePreFlopStrength(hand);
                
                assertTrue(strength >= 0.85, "TT should be >= 0.85, was: " + strength);
            }

            @Test
            @DisplayName("AQs should be strong")
            void aqsShouldBeStrong() {
                List<Card> hand = createHand(Value.ACE, Suit.HEARTS, Value.QUEEN, Suit.HEARTS);
                
                double strength = botAIService.calculatePreFlopStrength(hand);
                
                assertTrue(strength >= 0.65, "AQs should be >= 0.65, was: " + strength);
            }
        }

        @Nested
        @DisplayName("1.2 Post-Flop Monte Carlo Strength")
        class PostFlopStrengthTests {

            @Test
            @DisplayName("Made flush should have high strength")
            void madeFlushShouldHaveHighStrength() {
                List<Card> hand = createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.HEARTS);
                List<Card> board = List.of(
                        new Card(Suit.HEARTS, Value.TWO),
                        new Card(Suit.HEARTS, Value.FIVE),
                        new Card(Suit.HEARTS, Value.NINE),
                        new Card(Suit.SPADES, Value.JACK),
                        new Card(Suit.CLUBS, Value.THREE));

                HandRanking flushRanking = new HandRanking(
                        HandType.FLUSH,
                        List.of(Value.ACE),
                        List.of(Value.KING, Value.NINE, Value.FIVE, Value.TWO));

                when(handEvaluator.evaluate(eq(hand), anyList())).thenReturn(flushRanking);
                when(handEvaluator.evaluate(argThat(h -> h != hand), anyList()))
                        .thenReturn(new HandRanking(HandType.HIGH_CARD, 
                                List.of(Value.JACK), List.of(Value.NINE)));

                double strength = botAIService.calculateHandStrength(hand, board, 2);

                assertTrue(strength >= 0.70, "Made flush should be >= 0.70, was: " + strength);
            }

            @Test
            @DisplayName("Flush draw should have equity in range")
            void flushDrawShouldHaveEquityInRange() {
                List<Card> hand = createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.HEARTS);
                List<Card> board = List.of(
                        new Card(Suit.HEARTS, Value.TWO),
                        new Card(Suit.HEARTS, Value.FIVE),
                        new Card(Suit.SPADES, Value.NINE),
                        new Card(Suit.CLUBS, Value.JACK));

                when(handEvaluator.evaluate(any(), anyList()))
                        .thenReturn(new HandRanking(HandType.HIGH_CARD, 
                                List.of(Value.ACE), List.of(Value.KING)));

                double strength = botAIService.calculateHandStrength(hand, board, 2);

                assertTrue(strength >= 0.20 && strength <= 0.70, 
                        "Flush draw equity should be reasonable, was: " + strength);
            }

            @Test
            @DisplayName("Set should have very high strength")
            void setShouldHaveVeryHighStrength() {
                List<Card> hand = createHand(Value.SEVEN, Suit.HEARTS, Value.SEVEN, Suit.SPADES);
                List<Card> board = List.of(
                        new Card(Suit.CLUBS, Value.SEVEN),
                        new Card(Suit.DIAMONDS, Value.KING),
                        new Card(Suit.SPADES, Value.TWO));

                HandRanking setRanking = new HandRanking(
                        HandType.THREE_OF_A_KIND,
                        List.of(Value.SEVEN),
                        List.of(Value.KING, Value.TWO));

                when(handEvaluator.evaluate(eq(hand), anyList())).thenReturn(setRanking);
                when(handEvaluator.evaluate(argThat(h -> h != hand), anyList()))
                        .thenReturn(new HandRanking(HandType.ONE_PAIR, 
                                List.of(Value.KING), List.of(Value.SEVEN)));

                double strength = botAIService.calculateHandStrength(hand, board, 2);

                assertTrue(strength >= 0.75, "Set should have >= 0.75 strength, was: " + strength);
            }

            @Test
            @DisplayName("Open-ended straight draw should have ~32% equity")
            void oesdShouldHaveApproxThirtyTwoPercentEquity() {
                List<Card> hand = createHand(Value.NINE, Suit.HEARTS, Value.EIGHT, Suit.SPADES);
                List<Card> board = List.of(
                        new Card(Suit.CLUBS, Value.SEVEN),
                        new Card(Suit.DIAMONDS, Value.SIX),
                        new Card(Suit.SPADES, Value.TWO));

                when(handEvaluator.evaluate(any(), anyList()))
                        .thenReturn(new HandRanking(HandType.HIGH_CARD, 
                                List.of(Value.NINE), List.of(Value.EIGHT)));

                double strength = botAIService.calculateHandStrength(hand, board, 2);

                assertTrue(strength >= 0.15 && strength <= 0.55, 
                        "OESD should have ~32% equity, was: " + strength);
            }

            @Test
            @DisplayName("Overpair should be strong")
            void overpairShouldBeStrong() {
                List<Card> hand = createHand(Value.QUEEN, Suit.HEARTS, Value.QUEEN, Suit.SPADES);
                List<Card> board = List.of(
                        new Card(Suit.CLUBS, Value.NINE),
                        new Card(Suit.DIAMONDS, Value.FIVE),
                        new Card(Suit.SPADES, Value.TWO));

                HandRanking overpairRanking = new HandRanking(
                        HandType.ONE_PAIR,
                        List.of(Value.QUEEN),
                        List.of(Value.NINE, Value.FIVE, Value.TWO));

                when(handEvaluator.evaluate(eq(hand), anyList())).thenReturn(overpairRanking);
                when(handEvaluator.evaluate(argThat(h -> h != hand), anyList()))
                        .thenReturn(new HandRanking(HandType.HIGH_CARD, 
                                List.of(Value.NINE), List.of(Value.FIVE)));

                double strength = botAIService.calculateHandStrength(hand, board, 2);

                assertTrue(strength >= 0.60, "Overpair should have >= 0.60 strength, was: " + strength);
            }

            @Test
            @DisplayName("TPTK should have reasonable strength")
            void tptkShouldHaveReasonableStrength() {
                List<Card> hand = createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.SPADES);
                List<Card> board = List.of(
                        new Card(Suit.CLUBS, Value.ACE),
                        new Card(Suit.DIAMONDS, Value.SEVEN),
                        new Card(Suit.SPADES, Value.TWO));

                HandRanking tptkRanking = new HandRanking(
                        HandType.ONE_PAIR,
                        List.of(Value.ACE),
                        List.of(Value.KING, Value.SEVEN, Value.TWO));

                
                when(handEvaluator.evaluate(any(), anyList())).thenReturn(tptkRanking);

                double strength = botAIService.calculateHandStrength(hand, board, 2);

                
                assertTrue(strength >= 0.0 && strength <= 1.0, 
                        "TPTK strength should be valid, was: " + strength);
            }

            @Test
            @DisplayName("Null hand should return 0 strength")
            void nullHandShouldReturnZeroStrength() {
                double strength = botAIService.calculateHandStrength(null, List.of(), 2);
                assertEquals(0, strength);
            }

            @Test
            @DisplayName("Empty hand should return 0 strength")
            void emptyHandShouldReturnZeroStrength() {
                double strength = botAIService.calculateHandStrength(new ArrayList<>(), List.of(), 2);
                assertEquals(0, strength);
            }

            @Test
            @DisplayName("Single card hand should return 0 strength")
            void singleCardHandShouldReturnZeroStrength() {
                List<Card> hand = new ArrayList<>();
                hand.add(new Card(Suit.HEARTS, Value.ACE));
                
                double strength = botAIService.calculateHandStrength(hand, List.of(), 2);
                assertEquals(0, strength);
            }
        }
    }

    
    
    

    @Nested
    @DisplayName("2. Pot Odds Tests")
    class PotOddsTests {

        @Test
        @DisplayName("Should calculate basic pot odds (call 50 into 100 = 33%)")
        void shouldCalculateBasicPotOdds() {
            Game game = createGameWithPlayers(2);
            game.setCurrentPot(100);
            game.setCurrentBet(50);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertEquals(0.333, potOdds, 0.01, "Pot odds should be ~33%");
        }

        @Test
        @DisplayName("Should return 0 when no bet to call")
        void shouldReturnZeroWhenNoBetToCall() {
            Game game = createGameWithPlayers(2);
            game.setCurrentPot(100);
            game.setCurrentBet(0);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertEquals(0, potOdds, "Pot odds should be 0 when no bet");
        }

        @Test
        @DisplayName("Should return 0 when already matched bet")
        void shouldReturnZeroWhenAlreadyMatchedBet() {
            Game game = createGameWithPlayers(2);
            game.setCurrentPot(100);
            game.setCurrentBet(50);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(50);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertEquals(0, potOdds, "Pot odds should be 0 when bet matched");
        }

        @Test
        @DisplayName("Should calculate large pot small bet scenario")
        void shouldCalculateLargePotSmallBet() {
            Game game = createGameWithPlayers(2);
            game.setCurrentPot(1000);
            game.setCurrentBet(20);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertTrue(potOdds < 0.03, "Small bet into large pot should give good odds");
        }

        @Test
        @DisplayName("Should calculate small pot large bet scenario")
        void shouldCalculateSmallPotLargeBet() {
            Game game = createGameWithPlayers(2);
            game.setCurrentPot(50);
            game.setCurrentBet(200);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertEquals(0.80, potOdds, 0.01, "Large bet into small pot = bad odds");
        }

        @Test
        @DisplayName("Should calculate all-in scenario")
        void shouldCalculateAllInScenario() {
            Game game = createGameWithPlayers(2);
            game.setCurrentPot(500);
            game.setCurrentBet(500);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);
            bot.setChips(500);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertEquals(0.50, potOdds, 0.01, "All-in call should be 50% pot odds");
        }

        @Test
        @DisplayName("Should handle partial call amount")
        void shouldHandlePartialCallAmount() {
            Game game = createGameWithPlayers(2);
            game.setCurrentPot(200);
            game.setCurrentBet(100);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(30);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertEquals(0.259, potOdds, 0.01, "Partial call should factor in existing bet");
        }

        @Test
        @DisplayName("Should calculate pot odds with multiple players")
        void shouldCalculatePotOddsWithMultiplePlayers() {
            Game game = createGameWithPlayers(4);
            game.setCurrentPot(400);
            game.setCurrentBet(100);
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);

            double potOdds = botAIService.calculatePotOdds(game, bot);

            assertEquals(0.20, potOdds, 0.01, "Multi-way pot should give better odds");
        }
    }

    
    
    

    @Nested
    @DisplayName("3. Position Tests")
    class PositionTests {

        @Test
        @DisplayName("Button should have highest position score (3)")
        void buttonShouldHaveHighestPositionScore() {
            Game game = createGameWithPlayers(6);
            game.setDealerPosition(2);
            
            Player button = game.getPlayers().get(2);

            int score = botAIService.getPositionScore(game, button);

            assertEquals(3, score, "Button should have position score 3");
        }

        @Test
        @DisplayName("Cutoff should have good position score")
        void cutoffShouldHaveGoodPositionScore() {
            Game game = createGameWithPlayers(6);
            game.setDealerPosition(0);
            
            Player cutoff = game.getPlayers().get(5);

            int score = botAIService.getPositionScore(game, cutoff);

            assertTrue(score >= 2, "Cutoff should have position score >= 2");
        }

        @Test
        @DisplayName("Small blind should have low position score")
        void smallBlindShouldHaveLowPositionScore() {
            Game game = createGameWithPlayers(6);
            game.setDealerPosition(0);
            
            Player sb = game.getPlayers().get(1);

            int score = botAIService.getPositionScore(game, sb);

            assertTrue(score <= 1, "SB should have low position score");
        }

        @Test
        @DisplayName("Big blind should have low position score")
        void bigBlindShouldHaveLowPositionScore() {
            Game game = createGameWithPlayers(6);
            game.setDealerPosition(0);
            
            Player bb = game.getPlayers().get(2);

            int score = botAIService.getPositionScore(game, bb);

            assertTrue(score <= 1, "BB should have low position score");
        }

        @Test
        @DisplayName("Early position should have score 0")
        void earlyPositionShouldHaveScoreZero() {
            Game game = createGameWithPlayers(9);
            game.setDealerPosition(0);
            
            Player utg = game.getPlayers().get(3);

            int score = botAIService.getPositionScore(game, utg);

            assertEquals(0, score, "UTG should have position score 0");
        }

        @Test
        @DisplayName("Middle position should have reasonable score")
        void middlePositionShouldHaveReasonableScore() {
            Game game = createGameWithPlayers(9);
            game.setDealerPosition(0);
            
            Player mp = game.getPlayers().get(5);

            int score = botAIService.getPositionScore(game, mp);

            assertTrue(score >= 0 && score <= 2, "Middle position should have score 0-2");
        }

        @Test
        @DisplayName("Heads up button should have highest score")
        void headsUpButtonShouldHaveHighestScore() {
            Game game = createGameWithPlayers(2);
            game.setDealerPosition(0);
            
            Player dealer = game.getPlayers().get(0);
            Player other = game.getPlayers().get(1);

            int dealerScore = botAIService.getPositionScore(game, dealer);
            int otherScore = botAIService.getPositionScore(game, other);

            assertEquals(3, dealerScore, "Dealer in HU should have highest score");
            assertTrue(dealerScore > otherScore, "Dealer should beat non-dealer in HU");
        }

        @Test
        @DisplayName("6-max should calculate positions correctly")
        void sixMaxShouldCalculatePositionsCorrectly() {
            Game game = createGameWithPlayers(6);
            game.setDealerPosition(0);
            
            Player button = game.getPlayers().get(0);
            Player utg = game.getPlayers().get(3);

            assertTrue(botAIService.getPositionScore(game, button) > 
                    botAIService.getPositionScore(game, utg));
        }
    }

    
    
    

    @Nested
    @DisplayName("4. Pre-Flop Decision Tests")
    class PreFlopDecisionTests {

        @Test
        @DisplayName("Should raise with AA")
        void shouldRaiseWithAA() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.HEARTS, Value.ACE, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.RAISE, decision.action(), "Should raise with AA");
        }

        @Test
        @DisplayName("Should raise with KK")
        void shouldRaiseWithKK() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.KING, Suit.HEARTS, Value.KING, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.RAISE, decision.action(), "Should raise with KK");
        }

        @Test
        @DisplayName("Should raise with QQ")
        void shouldRaiseWithQQ() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.QUEEN, Suit.HEARTS, Value.QUEEN, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.RAISE, decision.action(), "Should raise with QQ");
        }

        @Test
        @DisplayName("Should have action with AKs")
        void shouldHaveActionWithAKs() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            game.setDealerPosition(3);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.HEARTS));

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.RAISE || decision.action() == PlayerAction.CALL,
                    "Should raise or call with AKs");
        }

        @Test
        @DisplayName("Strong hands should be position dependent")
        void strongHandsShouldBePositionDependent() {
            Game game = createGameWithPlayers(6);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            
            game.setDealerPosition(0);
            Player botEarly = game.getPlayers().get(3);
            botEarly.getHand().addAll(createHand(Value.JACK, Suit.HEARTS, Value.TEN, Suit.HEARTS));
            
            BotDecision earlyDecision = botAIService.decide(game, botEarly);
            
            assertNotNull(earlyDecision);
        }

        @Test
        @DisplayName("Weak hands should fold to raise")
        void weakHandsShouldFoldToRaise() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(150); // Higher raise that exceeds implied odds threshold (>10% of stack)

            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.SEVEN, Suit.HEARTS, Value.TWO, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.FOLD, decision.action(), "Should fold 72o to raise");
        }

        @Test
        @DisplayName("BB should defend with decent hands")
        void bbShouldDefendWithDecentHands() {
            Game game = createGameWithPlayers(3);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(40);
            game.setDealerPosition(0);
            
            Player bot = game.getPlayers().get(2);
            bot.setBetAmount(20);
            bot.getHand().addAll(createHand(Value.KING, Suit.HEARTS, Value.JACK, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.CALL || decision.action() == PlayerAction.RAISE,
                    "BB should defend KJo vs min-raise");
        }

        @Test
        @DisplayName("SB should have valid action with suited connectors")
        void sbShouldHaveValidActionWithSuitedConnectors() {
            Game game = createGameWithPlayers(3);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            game.setDealerPosition(0);
            
            Player bot = game.getPlayers().get(1);
            bot.setBetAmount(10);
            bot.getHand().addAll(createHand(Value.NINE, Suit.HEARTS, Value.EIGHT, Suit.HEARTS));

            BotDecision decision = botAIService.decide(game, bot);

            assertNotNull(decision.action());
        }

        @Test
        @DisplayName("Should have action facing 3-bet with marginal hand")
        void shouldHaveActionFacingThreeBetWithMarginalHand() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(200);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.JACK, Suit.HEARTS, Value.TEN, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            
            assertNotNull(decision.action(), "Should have valid action facing 3-bet");
        }

        @Test
        @DisplayName("Should check or call when in BB with weak hand no raise")
        void shouldCheckOrCallWhenInBbWithWeakHandNoRaise() {
            Game game = createGameWithPlayers(3);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            game.setDealerPosition(0);
            
            
            Player bot = game.getPlayers().get(2);
            bot.setBetAmount(20);
            bot.getHand().addAll(createHand(Value.EIGHT, Suit.HEARTS, Value.THREE, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            
            assertTrue(decision.action() == PlayerAction.CHECK || decision.action() == PlayerAction.CALL,
                    "Should check or call weak hand in BB, was: " + decision.action());
        }

        @Test
        @DisplayName("Should fold small pairs to large raise")
        void shouldFoldSmallPairsToLargeRaise() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(200);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.THREE, Suit.HEARTS, Value.THREE, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.FOLD, decision.action(), "Should fold 33 to big raise");
        }

        @Test
        @DisplayName("Should raise with JJ in position")
        void shouldRaiseWithJJInPosition() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.PRE_FLOP);
            game.setCurrentBet(20);
            game.setDealerPosition(0);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.JACK, Suit.HEARTS, Value.JACK, Suit.SPADES));

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.RAISE, decision.action(), "Should raise with JJ");
        }
    }

    
    
    

    @Nested
    @DisplayName("5. Post-Flop Decision Tests")
    class PostFlopDecisionTests {

        @Test
        @DisplayName("Should have action with strong made hand when no bet")
        void shouldHaveActionWithStrongMadeHand() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(0);
            game.setCurrentPot(100);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.ACE),
                    new Card(Suit.SPADES, Value.SEVEN),
                    new Card(Suit.CLUBS, Value.TWO)));
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.DIAMONDS, Value.ACE, Suit.CLUBS));

            HandRanking setRanking = new HandRanking(
                    HandType.THREE_OF_A_KIND,
                    List.of(Value.ACE),
                    List.of(Value.SEVEN, Value.TWO));
            when(handEvaluator.evaluate(any(), any())).thenReturn(setRanking);

            BotDecision decision = botAIService.decide(game, bot);

            
            assertTrue(decision.action() == PlayerAction.BET || decision.action() == PlayerAction.CHECK,
                    "Should bet or check with set, was: " + decision.action());
        }

        @Test
        @DisplayName("Should check or bet medium hand")
        void shouldCheckOrBetMediumHand() {
            Game game = createGameWithPlayers(4);
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(0);
            game.setCurrentPot(80);
            game.setDealerPosition(3);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.TEN),
                    new Card(Suit.SPADES, Value.SEVEN),
                    new Card(Suit.CLUBS, Value.TWO)));
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.TEN, Suit.DIAMONDS, Value.EIGHT, Suit.CLUBS));

            HandRanking pairRanking = new HandRanking(
                    HandType.ONE_PAIR,
                    List.of(Value.TEN),
                    List.of(Value.EIGHT, Value.SEVEN, Value.TWO));
            when(handEvaluator.evaluate(any(), any())).thenReturn(pairRanking);

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.CHECK || decision.action() == PlayerAction.BET,
                    "Should check or bet with medium hand");
        }

        @Test
        @DisplayName("Should have action as pre-flop aggressor")
        void shouldHaveActionAsPreFlopAggressor() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(0);
            game.setCurrentPot(100);
            game.setDealerPosition(0);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.KING),
                    new Card(Suit.SPADES, Value.SEVEN),
                    new Card(Suit.CLUBS, Value.TWO)));
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.HEARTS, Value.QUEEN, Suit.SPADES));

            HandRanking highCard = new HandRanking(
                    HandType.HIGH_CARD,
                    List.of(Value.ACE),
                    List.of(Value.KING, Value.QUEEN));
            when(handEvaluator.evaluate(any(), any())).thenReturn(highCard);

            BotDecision decision = botAIService.decide(game, bot);

            assertNotNull(decision.action());
        }

        @Test
        @DisplayName("Should call or raise draw with pot odds")
        void shouldCallOrRaiseDrawWithPotOdds() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(20);
            game.setCurrentPot(200);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.KING),
                    new Card(Suit.HEARTS, Value.SEVEN),
                    new Card(Suit.CLUBS, Value.TWO)));
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.HEARTS, Value.JACK, Suit.HEARTS));

            HandRanking flushDraw = new HandRanking(
                    HandType.HIGH_CARD,
                    List.of(Value.ACE),
                    List.of(Value.KING, Value.JACK));
            when(handEvaluator.evaluate(any(), any())).thenReturn(flushDraw);

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.CALL || decision.action() == PlayerAction.RAISE,
                    "Should call or raise flush draw with good pot odds");
        }

        @Test
        @DisplayName("Should fold missed draw without odds")
        void shouldFoldMissedDrawWithoutOdds() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.RIVER);
            game.setCurrentBet(300);
            game.setCurrentPot(100);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.KING),
                    new Card(Suit.SPADES, Value.SEVEN),
                    new Card(Suit.CLUBS, Value.TWO),
                    new Card(Suit.DIAMONDS, Value.THREE),
                    new Card(Suit.CLUBS, Value.NINE)));
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);
            bot.getHand().addAll(createHand(Value.FIVE, Suit.HEARTS, Value.FOUR, Suit.HEARTS));

            HandRanking missedDraw = new HandRanking(
                    HandType.HIGH_CARD,
                    List.of(Value.KING),
                    List.of(Value.NINE));
            when(handEvaluator.evaluate(any(), any())).thenReturn(missedDraw);

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.FOLD, decision.action(), "Should fold missed draw with bad odds");
        }

        @Test
        @DisplayName("Should raise or call with monster")
        void shouldRaiseOrCallWithMonster() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(50);
            game.setCurrentPot(100);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.SEVEN),
                    new Card(Suit.SPADES, Value.SEVEN),
                    new Card(Suit.CLUBS, Value.TWO)));
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);
            bot.getHand().addAll(createHand(Value.SEVEN, Suit.DIAMONDS, Value.SEVEN, Suit.CLUBS));

            HandRanking quads = new HandRanking(
                    HandType.FOUR_OF_A_KIND,
                    List.of(Value.SEVEN),
                    List.of(Value.TWO));
            when(handEvaluator.evaluate(any(), any())).thenReturn(quads);

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.RAISE || decision.action() == PlayerAction.CALL,
                    "Should raise or call with quads");
        }

        @Test
        @DisplayName("Should have action on scare card")
        void shouldHaveActionOnScareCard() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.TURN);
            game.setCurrentBet(0);
            game.setCurrentPot(150);
            game.setDealerPosition(0);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.TWO),
                    new Card(Suit.SPADES, Value.THREE),
                    new Card(Suit.CLUBS, Value.FIVE),
                    new Card(Suit.HEARTS, Value.ACE)));
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.KING, Suit.DIAMONDS, Value.QUEEN, Suit.CLUBS));

            HandRanking highCard = new HandRanking(
                    HandType.HIGH_CARD,
                    List.of(Value.ACE),
                    List.of(Value.KING, Value.QUEEN));
            when(handEvaluator.evaluate(any(), any())).thenReturn(highCard);

            BotDecision decision = botAIService.decide(game, bot);

            assertNotNull(decision.action());
        }

        @Test
        @DisplayName("Should raise or call on river with nuts")
        void shouldRaiseOrCallOnRiverWithNuts() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.RIVER);
            game.setCurrentBet(50);
            game.setCurrentPot(200);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.TEN),
                    new Card(Suit.HEARTS, Value.JACK),
                    new Card(Suit.HEARTS, Value.QUEEN),
                    new Card(Suit.SPADES, Value.TWO),
                    new Card(Suit.CLUBS, Value.THREE)));
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.HEARTS));

            HandRanking royalFlush = new HandRanking(
                    HandType.ROYAL_FLUSH,
                    List.of(Value.ACE),
                    List.of());
            when(handEvaluator.evaluate(any(), any())).thenReturn(royalFlush);

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.RAISE || decision.action() == PlayerAction.CALL,
                    "Should raise or call with royal flush, was: " + decision.action());
        }

        @Test
        @DisplayName("Should have valid action on river")
        void shouldHaveValidActionOnRiver() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.RIVER);
            game.setCurrentBet(0);
            game.setCurrentPot(100);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.ACE),
                    new Card(Suit.SPADES, Value.KING),
                    new Card(Suit.CLUBS, Value.QUEEN),
                    new Card(Suit.DIAMONDS, Value.JACK),
                    new Card(Suit.HEARTS, Value.NINE)));
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.TWO, Suit.CLUBS, Value.THREE, Suit.DIAMONDS));

            HandRanking straight = new HandRanking(
                    HandType.STRAIGHT,
                    List.of(Value.ACE),
                    List.of());
            when(handEvaluator.evaluate(any(), any())).thenReturn(straight);

            BotDecision decision = botAIService.decide(game, bot);

            assertNotNull(decision.action());
        }

        @Test
        @DisplayName("Should fold to river overbet")
        void shouldFoldToRiverOverbet() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.RIVER);
            game.setCurrentBet(500);
            game.setCurrentPot(150);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.ACE),
                    new Card(Suit.SPADES, Value.KING),
                    new Card(Suit.CLUBS, Value.SEVEN),
                    new Card(Suit.DIAMONDS, Value.TWO),
                    new Card(Suit.HEARTS, Value.THREE)));
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.CLUBS, Value.EIGHT, Suit.DIAMONDS));

            HandRanking topPair = new HandRanking(
                    HandType.ONE_PAIR,
                    List.of(Value.ACE),
                    List.of(Value.KING, Value.EIGHT));
            when(handEvaluator.evaluate(any(), any())).thenReturn(topPair);

            BotDecision decision = botAIService.decide(game, bot);

            assertEquals(PlayerAction.FOLD, decision.action(), "Should fold to massive overbet");
        }

        @Test
        @DisplayName("Should bet or check two pair for value")
        void shouldBetOrCheckTwoPairForValue() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.TURN);
            game.setCurrentBet(0);
            game.setCurrentPot(120);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.ACE),
                    new Card(Suit.SPADES, Value.KING),
                    new Card(Suit.CLUBS, Value.SEVEN),
                    new Card(Suit.DIAMONDS, Value.TWO)));
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.CLUBS, Value.KING, Suit.DIAMONDS));

            HandRanking twoPair = new HandRanking(
                    HandType.TWO_PAIR,
                    List.of(Value.ACE, Value.KING),
                    List.of(Value.SEVEN));
            when(handEvaluator.evaluate(any(), any())).thenReturn(twoPair);

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.BET || decision.action() == PlayerAction.CHECK,
                    "Should bet or check with two pair, was: " + decision.action());
        }

        @Test
        @DisplayName("Should call small bet with middle pair")
        void shouldCallSmallBetWithMiddlePair() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(15);
            game.setCurrentPot(100);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.HEARTS, Value.ACE),
                    new Card(Suit.SPADES, Value.NINE),
                    new Card(Suit.CLUBS, Value.THREE)));
            
            Player bot = game.getPlayers().get(0);
            bot.setBetAmount(0);
            bot.getHand().addAll(createHand(Value.NINE, Suit.CLUBS, Value.TEN, Suit.DIAMONDS));

            HandRanking middlePair = new HandRanking(
                    HandType.ONE_PAIR,
                    List.of(Value.NINE),
                    List.of(Value.ACE, Value.TEN, Value.THREE));
            when(handEvaluator.evaluate(any(), any())).thenReturn(middlePair);

            BotDecision decision = botAIService.decide(game, bot);

            assertTrue(decision.action() == PlayerAction.CALL || decision.action() == PlayerAction.RAISE,
                    "Should call or raise with middle pair vs small bet");
        }
    }

    
    
    

    @Nested
    @DisplayName("6. Bot Personalities Tests")
    class BotPersonalitiesTests {

        @Test
        @DisplayName("TAG should be tight pre-flop")
        void tagShouldBeTightPreFlop() {
            BotPersonality tag = BotPersonality.TIGHT_AGGRESSIVE;
            
            assertTrue(tag.handRangeMultiplier > 1.0, 
                    "TAG should have higher hand requirement multiplier");
        }

        @Test
        @DisplayName("TAG should be aggressive post-flop")
        void tagShouldBeAggressivePostFlop() {
            BotPersonality tag = BotPersonality.TIGHT_AGGRESSIVE;
            
            assertTrue(tag.aggressionFactor > 1.0, 
                    "TAG should have aggression factor > 1");
        }

        @Test
        @DisplayName("LAG should have wider pre-flop range")
        void lagShouldHaveWiderPreFlopRange() {
            BotPersonality lag = BotPersonality.LOOSE_AGGRESSIVE;
            
            assertTrue(lag.handRangeMultiplier < 1.0, 
                    "LAG should have lower hand requirement");
        }

        @Test
        @DisplayName("LAG should bluff more")
        void lagShouldBluffMore() {
            BotPersonality lag = BotPersonality.LOOSE_AGGRESSIVE;
            BotPersonality tag = BotPersonality.TIGHT_AGGRESSIVE;
            
            assertTrue(lag.bluffFrequency > tag.bluffFrequency, 
                    "LAG should bluff more than TAG");
        }

        @Test
        @DisplayName("Rock should be very tight")
        void rockShouldBeVeryTight() {
            BotPersonality rock = BotPersonality.TIGHT_PASSIVE;
            
            assertTrue(rock.handRangeMultiplier >= 1.2, 
                    "Rock should have strict hand requirements");
        }

        @Test
        @DisplayName("Rock should be passive")
        void rockShouldBePassive() {
            BotPersonality rock = BotPersonality.TIGHT_PASSIVE;
            
            assertTrue(rock.aggressionFactor < 1.0, 
                    "Rock should have aggression factor < 1");
        }

        @Test
        @DisplayName("Fish should be loose passive")
        void fishShouldBeLoosePassive() {
            BotPersonality fish = BotPersonality.LOOSE_PASSIVE;
            
            assertTrue(fish.handRangeMultiplier < 1.0, "Fish should play loose");
            assertTrue(fish.aggressionFactor < 1.0, "Fish should be passive");
        }

        @Test
        @DisplayName("Aggression factors should vary by personality")
        void aggressionFactorsShouldVaryByPersonality() {
            double tagAggression = BotPersonality.TIGHT_AGGRESSIVE.aggressionFactor;
            double lagAggression = BotPersonality.LOOSE_AGGRESSIVE.aggressionFactor;
            double rockAggression = BotPersonality.TIGHT_PASSIVE.aggressionFactor;
            double fishAggression = BotPersonality.LOOSE_PASSIVE.aggressionFactor;
            
            assertTrue(lagAggression >= tagAggression, "LAG >= TAG aggression");
            assertTrue(tagAggression > rockAggression, "TAG > Rock aggression");
            assertTrue(tagAggression > fishAggression, "TAG > Fish aggression");
        }

        @Test
        @DisplayName("Bluff frequency should be highest for LAG")
        void bluffFrequencyShouldBeHighestForLag() {
            BotPersonality lag = BotPersonality.LOOSE_AGGRESSIVE;
            
            for (BotPersonality personality : BotPersonality.values()) {
                if (personality != lag) {
                    assertTrue(lag.bluffFrequency >= personality.bluffFrequency,
                            "LAG should have highest or equal bluff frequency");
                }
            }
        }

        @Test
        @DisplayName("Personality assignment should be deterministic")
        void personalityAssignmentShouldBeDeterministic() {
            BotPersonality p1 = botAIService.getBotPersonality("TestBot");
            BotPersonality p2 = botAIService.getBotPersonality("TestBot");
            
            assertEquals(p1, p2, "Same name should get same personality");
        }
    }

    
    
    

    @Nested
    @DisplayName("7. Opponent Modeling Tests")
    class OpponentModelingTests {

        @Test
        @DisplayName("Should record bet action")
        void shouldRecordBetAction() {
            UUID playerId = UUID.randomUUID();
            
            assertDoesNotThrow(() -> 
                    botAIService.recordOpponentAction(playerId, "BET", 50, 100));
        }

        @Test
        @DisplayName("Should record raise action")
        void shouldRecordRaiseAction() {
            UUID playerId = UUID.randomUUID();
            
            assertDoesNotThrow(() -> 
                    botAIService.recordOpponentAction(playerId, "RAISE", 100, 200));
        }

        @Test
        @DisplayName("Should record call action")
        void shouldRecordCallAction() {
            UUID playerId = UUID.randomUUID();
            
            assertDoesNotThrow(() -> 
                    botAIService.recordOpponentAction(playerId, "CALL", 50, 150));
        }

        @Test
        @DisplayName("Should record fold action")
        void shouldRecordFoldAction() {
            UUID playerId = UUID.randomUUID();
            
            assertDoesNotThrow(() -> 
                    botAIService.recordOpponentAction(playerId, "FOLD", 0, 100));
        }

        @Test
        @DisplayName("Should reset opponent models")
        void shouldResetOpponentModels() {
            UUID playerId = UUID.randomUUID();
            botAIService.recordOpponentAction(playerId, "BET", 50, 100);
            
            assertDoesNotThrow(() -> botAIService.resetOpponentModels());
        }

        @Test
        @DisplayName("Should track multiple opponents")
        void shouldTrackMultipleOpponents() {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            
            assertDoesNotThrow(() -> {
                botAIService.recordOpponentAction(player1, "BET", 50, 100);
                botAIService.recordOpponentAction(player2, "CALL", 50, 150);
            });
        }
    }

    
    
    

    @Nested
    @DisplayName("8. Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete hand scenario")
        void shouldHandleCompleteHandScenario() {
            Game game = createGameWithPlayers(3);
            game.setPhase(GamePhase.PRE_FLOP);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.ACE, Suit.HEARTS, Value.ACE, Suit.SPADES));
            
            BotDecision preFlopDecision = botAIService.decide(game, bot);
            assertNotNull(preFlopDecision);
            
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(0);
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.CLUBS, Value.KING),
                    new Card(Suit.DIAMONDS, Value.SEVEN),
                    new Card(Suit.SPADES, Value.TWO)));
            
            HandRanking aces = new HandRanking(
                    HandType.ONE_PAIR,
                    List.of(Value.ACE),
                    List.of(Value.KING, Value.SEVEN, Value.TWO));
            when(handEvaluator.evaluate(any(), any())).thenReturn(aces);
            
            BotDecision flopDecision = botAIService.decide(game, bot);
            assertNotNull(flopDecision);
        }

        @Test
        @DisplayName("Should handle all-in player")
        void shouldHandleAllInPlayer() {
            Game game = createGameWithPlayers(2);
            game.setPhase(GamePhase.FLOP);
            game.setCurrentBet(100);
            
            Player bot = game.getPlayers().get(0);
            bot.setChips(0);
            bot.setAllIn(true);
            bot.getHand().addAll(createHand(Value.ACE, Suit.HEARTS, Value.KING, Suit.SPADES));
            
            game.getCommunityCards().addAll(List.of(
                    new Card(Suit.CLUBS, Value.TWO),
                    new Card(Suit.DIAMONDS, Value.THREE),
                    new Card(Suit.SPADES, Value.FOUR)));
            
            HandRanking highCard = new HandRanking(
                    HandType.HIGH_CARD,
                    List.of(Value.ACE),
                    List.of(Value.KING));
            when(handEvaluator.evaluate(any(), any())).thenReturn(highCard);
            
            BotDecision decision = botAIService.decide(game, bot);
            assertNotNull(decision);
        }

        @Test
        @DisplayName("Should make decisions across all phases")
        void shouldMakeDecisionsAcrossAllPhases() {
            Game game = createGameWithPlayers(2);
            
            Player bot = game.getPlayers().get(0);
            bot.getHand().addAll(createHand(Value.QUEEN, Suit.HEARTS, Value.JACK, Suit.HEARTS));
            
            HandRanking ranking = new HandRanking(
                    HandType.ONE_PAIR,
                    List.of(Value.QUEEN),
                    List.of(Value.JACK));
            when(handEvaluator.evaluate(any(), any())).thenReturn(ranking);
            
            for (GamePhase phase : List.of(GamePhase.PRE_FLOP, GamePhase.FLOP, 
                    GamePhase.TURN, GamePhase.RIVER)) {
                game.setPhase(phase);
                game.setCurrentBet(20);
                
                if (phase != GamePhase.PRE_FLOP) {
                    game.getCommunityCards().clear();
                    int cardCount = switch (phase) {
                        case FLOP -> 3;
                        case TURN -> 4;
                        case RIVER -> 5;
                        default -> 0;
                    };
                    for (int i = 0; i < cardCount; i++) {
                        game.getCommunityCards().add(
                                new Card(Suit.values()[i % 4], Value.values()[i + 2]));
                    }
                }
                
                BotDecision decision = botAIService.decide(game, bot);
                assertNotNull(decision, "Decision should not be null for " + phase);
            }
        }
    }
}
