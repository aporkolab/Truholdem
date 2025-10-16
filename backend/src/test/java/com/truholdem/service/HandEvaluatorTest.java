package com.truholdem.service;

import com.truholdem.TestConstants;
import com.truholdem.TestFixtures;
import com.truholdem.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;


@DisplayName("HandEvaluator - Poker Hand Evaluation Tests")
class HandEvaluatorTest {

    private HandEvaluator handEvaluator;

    @BeforeEach
    void setUp() {
        handEvaluator = new HandEvaluator();
    }

    
    
    

    
    private Card card(String str) {
        if (str == null || str.length() != 2) {
            throw new IllegalArgumentException("Invalid card string: " + str);
        }
        Value value = switch (str.charAt(0)) {
            case '2' -> Value.TWO;
            case '3' -> Value.THREE;
            case '4' -> Value.FOUR;
            case '5' -> Value.FIVE;
            case '6' -> Value.SIX;
            case '7' -> Value.SEVEN;
            case '8' -> Value.EIGHT;
            case '9' -> Value.NINE;
            case 'T' -> Value.TEN;
            case 'J' -> Value.JACK;
            case 'Q' -> Value.QUEEN;
            case 'K' -> Value.KING;
            case 'A' -> Value.ACE;
            default -> throw new IllegalArgumentException("Invalid value: " + str.charAt(0));
        };
        Suit suit = switch (str.charAt(1)) {
            case 'h' -> Suit.HEARTS;
            case 'd' -> Suit.DIAMONDS;
            case 'c' -> Suit.CLUBS;
            case 's' -> Suit.SPADES;
            default -> throw new IllegalArgumentException("Invalid suit: " + str.charAt(1));
        };
        return new Card(suit, value);
    }

    
    private List<Card> cards(String... strs) {
        List<Card> result = new ArrayList<>();
        for (String str : strs) {
            result.add(card(str));
        }
        return result;
    }

    
    private HandRanking evaluate(String... cardStrs) {
        if (cardStrs.length != 7) {
            throw new IllegalArgumentException("Need exactly 7 cards");
        }
        List<Card> holeCards = cards(cardStrs[0], cardStrs[1]);
        List<Card> community = cards(cardStrs[2], cardStrs[3], cardStrs[4], cardStrs[5], cardStrs[6]);
        return handEvaluator.evaluate(holeCards, community);
    }

    
    
    

    @Nested
    @DisplayName("Royal Flush Tests")
    class RoyalFlushTests {

        @Test
        @DisplayName("Royal Flush in Spades - A♠ K♠ Q♠ J♠ T♠")
        void royalFlushSpades() {
            HandRanking result = evaluate("As", "Ks", "Qs", "Js", "Ts", "4c", "3d");
            
            assertThat(result.getHandType()).isEqualTo(HandType.ROYAL_FLUSH);
            assertThat(result.getRankValues()).containsExactly(Value.ACE);
            assertThat(result.getDescription()).isEqualTo("Royal Flush");
        }

        @Test
        @DisplayName("Royal Flush in Hearts - A♥ K♥ Q♥ J♥ T♥")
        void royalFlushHearts() {
            HandRanking result = evaluate("Ah", "Kh", "Qh", "Jh", "Th", "2s", "3c");
            
            assertThat(result.getHandType()).isEqualTo(HandType.ROYAL_FLUSH);
        }

        @Test
        @DisplayName("Royal Flush in Diamonds - A♦ K♦ Q♦ J♦ T♦")
        void royalFlushDiamonds() {
            HandRanking result = evaluate("Ad", "Kd", "Qd", "Jd", "Td", "9h", "8c");
            
            assertThat(result.getHandType()).isEqualTo(HandType.ROYAL_FLUSH);
        }

        @Test
        @DisplayName("Royal Flush in Clubs - A♣ K♣ Q♣ J♣ T♣")
        void royalFlushClubs() {
            HandRanking result = evaluate("Ac", "Kc", "Qc", "Jc", "Tc", "2h", "2d");
            
            assertThat(result.getHandType()).isEqualTo(HandType.ROYAL_FLUSH);
        }

        @ParameterizedTest(name = "Royal Flush in {0}")
        @EnumSource(Suit.class)
        @DisplayName("Royal Flush detection in all suits")
        void royalFlushAllSuits(Suit suit) {
            char s = switch (suit) {
                case HEARTS -> 'h';
                case DIAMONDS -> 'd';
                case CLUBS -> 'c';
                case SPADES -> 's';
            };
            
            HandRanking result = evaluate(
                "A" + s, "K" + s, "Q" + s, "J" + s, "T" + s, "2h", "3c"
            );
            
            assertThat(result.getHandType()).isEqualTo(HandType.ROYAL_FLUSH);
        }

        @Test
        @DisplayName("Royal Flush beats Straight Flush")
        void royalFlushBeatsStraightFlush() {
            HandRanking royalFlush = evaluate("As", "Ks", "Qs", "Js", "Ts", "4c", "3d");
            HandRanking straightFlush = evaluate("Ks", "Qs", "Js", "Ts", "9s", "4c", "3d");
            
            assertThat(royalFlush.compareTo(straightFlush)).isPositive();
        }

        @Test
        @DisplayName("Royal Flush ties with another Royal Flush (split pot)")
        void royalFlushTie() {
            
            HandRanking rf1 = evaluate("As", "Ks", "Qs", "Js", "Ts", "2h", "3c");
            HandRanking rf2 = evaluate("As", "Ks", "Qs", "Js", "Ts", "4h", "5c");
            
            assertThat(rf1.compareTo(rf2)).isZero();
        }
    }

    
    
    

    @Nested
    @DisplayName("Straight Flush Tests")
    class StraightFlushTests {

        @Test
        @DisplayName("King-high Straight Flush - K♥ Q♥ J♥ T♥ 9♥")
        void kingHighStraightFlush() {
            HandRanking result = evaluate("Kh", "Qh", "Jh", "Th", "9h", "2c", "3d");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT_FLUSH);
            assertThat(result.getRankValues()).containsExactly(Value.KING);
        }

        @Test
        @DisplayName("Queen-high Straight Flush - Q♠ J♠ T♠ 9♠ 8♠")
        void queenHighStraightFlush() {
            HandRanking result = evaluate("Qs", "Js", "Ts", "9s", "8s", "2c", "3d");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT_FLUSH);
            assertThat(result.getRankValues()).containsExactly(Value.QUEEN);
        }

        @Test
        @DisplayName("Steel Wheel (5-high Straight Flush) - 5♣ 4♣ 3♣ 2♣ A♣")
        void steelWheel() {
            HandRanking result = evaluate("Ac", "2c", "3c", "4c", "5c", "Kh", "Qd");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT_FLUSH);
            assertThat(result.getRankValues()).containsExactly(Value.FIVE);
        }

        @Test
        @DisplayName("Six-high Straight Flush - 6♦ 5♦ 4♦ 3♦ 2♦")
        void sixHighStraightFlush() {
            HandRanking result = evaluate("6d", "5d", "4d", "3d", "2d", "Kh", "Qc");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT_FLUSH);
            assertThat(result.getRankValues()).containsExactly(Value.SIX);
        }

        @Test
        @DisplayName("Middle Straight Flush - 9♥ 8♥ 7♥ 6♥ 5♥")
        void middleStraightFlush() {
            HandRanking result = evaluate("9h", "8h", "7h", "6h", "5h", "Ac", "Kd");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT_FLUSH);
            assertThat(result.getRankValues()).containsExactly(Value.NINE);
        }

        @Test
        @DisplayName("King-high beats Queen-high Straight Flush")
        void straightFlushComparison() {
            HandRanking kingHigh = evaluate("Ks", "Qs", "Js", "Ts", "9s", "2c", "3d");
            HandRanking queenHigh = evaluate("Qh", "Jh", "Th", "9h", "8h", "2c", "3d");
            
            assertThat(kingHigh.compareTo(queenHigh)).isPositive();
        }

        @Test
        @DisplayName("Steel Wheel loses to Six-high Straight Flush")
        void steelWheelLowestStraightFlush() {
            HandRanking steelWheel = evaluate("Ac", "2c", "3c", "4c", "5c", "Kh", "Qd");
            HandRanking sixHigh = evaluate("6d", "5d", "4d", "3d", "2d", "Kh", "Qc");
            
            assertThat(steelWheel.compareTo(sixHigh)).isNegative();
        }

        @Test
        @DisplayName("Same height Straight Flush ties")
        void straightFlushTie() {
            HandRanking sf1 = evaluate("9s", "8s", "7s", "6s", "5s", "Ac", "Kd");
            HandRanking sf2 = evaluate("9h", "8h", "7h", "6h", "5h", "Ac", "Kd");
            
            assertThat(sf1.compareTo(sf2)).isZero();
        }

        @ParameterizedTest(name = "{0}-high Straight Flush")
        @CsvSource({
            "9, 9h, 8h, 7h, 6h, 5h, Ac, Kd",
            "8, 8s, 7s, 6s, 5s, 4s, Ah, Kc",
            "7, 7c, 6c, 5c, 4c, 3c, Ah, Kd"
        })
        @DisplayName("Straight Flush at various heights")
        void straightFlushVariousHeights(int expectedHigh, String c1, String c2, String c3, 
                                          String c4, String c5, String c6, String c7) {
            HandRanking result = evaluate(c1, c2, c3, c4, c5, c6, c7);
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT_FLUSH);
        }
    }

    
    
    

    @Nested
    @DisplayName("Four of a Kind Tests")
    class FourOfAKindTests {

        @Test
        @DisplayName("Quad Aces with King kicker")
        void quadAcesKingKicker() {
            HandRanking result = evaluate("Ah", "Ad", "Ac", "As", "Kh", "2c", "3d");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FOUR_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.ACE);
            assertThat(result.getKickerValues()).containsExactly(Value.KING);
        }

        @Test
        @DisplayName("Quad Kings with Ace kicker")
        void quadKingsAceKicker() {
            HandRanking result = evaluate("Kh", "Kd", "Kc", "Ks", "Ah", "2c", "3d");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FOUR_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.KING);
            assertThat(result.getKickerValues()).containsExactly(Value.ACE);
        }

        @Test
        @DisplayName("Quad Twos (lowest quads) with Ace kicker")
        void quadTwosAceKicker() {
            HandRanking result = evaluate("2h", "2d", "2c", "2s", "Ah", "Kc", "Qd");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FOUR_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.TWO);
            assertThat(result.getKickerValues()).containsExactly(Value.ACE);
        }

        @Test
        @DisplayName("Quad Aces beats Quad Kings")
        void quadAcesBeatQuadKings() {
            HandRanking quadAces = evaluate("Ah", "Ad", "Ac", "As", "2h", "3c", "4d");
            HandRanking quadKings = evaluate("Kh", "Kd", "Kc", "Ks", "Ah", "2c", "3d");
            
            assertThat(quadAces.compareTo(quadKings)).isPositive();
        }

        @Test
        @DisplayName("Same quads - higher kicker wins")
        void sameQuadsKickerDecides() {
            HandRanking quadsAceKicker = evaluate("Kh", "Kd", "Kc", "Ks", "Ah", "2c", "3d");
            HandRanking quadsQueenKicker = evaluate("Kh", "Kd", "Kc", "Ks", "Qh", "2c", "3d");
            
            assertThat(quadsAceKicker.compareTo(quadsQueenKicker)).isPositive();
        }

        @Test
        @DisplayName("Quads from pocket pair hitting board")
        void quadsFromPocketPair() {
            HandRanking result = evaluate("7h", "7d", "7c", "7s", "Ah", "Kc", "Qd");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FOUR_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.SEVEN);
        }

        @Test
        @DisplayName("Four of a Kind beats Full House")
        void fourOfAKindBeatsFullHouse() {
            HandRanking quads = evaluate("Ah", "Ad", "Ac", "As", "Kh", "2c", "3d");
            HandRanking fullHouse = evaluate("Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s");
            
            assertThat(quads.compareTo(fullHouse)).isPositive();
        }
    }

    
    
    

    @Nested
    @DisplayName("Full House Tests")
    class FullHouseTests {

        @Test
        @DisplayName("Aces full of Kings - A♠ A♥ A♦ K♠ K♥")
        void acesFullOfKings() {
            HandRanking result = evaluate("Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FULL_HOUSE);
            assertThat(result.getRankValues()).containsExactly(Value.ACE, Value.KING);
            assertThat(result.getDescription()).contains("Aces full of Kings");
        }

        @Test
        @DisplayName("Kings full of Aces - K♠ K♥ K♦ A♠ A♥")
        void kingsFullOfAces() {
            HandRanking result = evaluate("Kh", "Kd", "Kc", "Ah", "Ad", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FULL_HOUSE);
            
            assertThat(result.getRankValues()).containsExactly(Value.KING, Value.ACE);
        }

        @Test
        @DisplayName("Twos full of Threes - lowest full house")
        void twosFullOfThrees() {
            HandRanking result = evaluate("2h", "2d", "2c", "3h", "3d", "Ac", "Ks");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FULL_HOUSE);
            
            assertThat(result.getRankValues()).containsExactly(Value.TWO, Value.THREE);
        }

        @Test
        @DisplayName("Aces full of Kings beats Kings full of Aces")
        void acesFullBeatKingsFull() {
            HandRanking acesFullKings = evaluate("Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s");
            HandRanking kingsFullAces = evaluate("Kh", "Kd", "Kc", "Ah", "Ad", "2c", "3s");
            
            
            assertThat(acesFullKings.compareTo(kingsFullAces)).isPositive();
        }

        @Test
        @DisplayName("Same trips - higher pair wins")
        void sameTripsHigherPairWins() {
            HandRanking acesFullKings = evaluate("Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s");
            HandRanking acesFullQueens = evaluate("Ah", "Ad", "Ac", "Qh", "Qd", "2c", "3s");
            
            
            assertThat(acesFullKings.compareTo(acesFullQueens)).isPositive();
        }

        @Test
        @DisplayName("Two pair on board with matching hole card")
        void twoPairWithMatchingHoleCard() {
            
            HandRanking result = evaluate("Ah", "7c", "Ad", "Kh", "Kd", "3c", "2s");
            
            
            assertThat(result.getHandType()).isEqualTo(HandType.TWO_PAIR);
        }

        @Test
        @DisplayName("Full House beats Flush")
        void fullHouseBeatsFlush() {
            HandRanking fullHouse = evaluate("Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s");
            HandRanking flush = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s");
            
            assertThat(fullHouse.compareTo(flush)).isPositive();
        }

        @Test
        @DisplayName("Full House with trips on board")
        void fullHouseTripsOnBoard() {
            
            HandRanking result = evaluate("Ah", "Ad", "Kh", "Kd", "Kc", "7s", "2c");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FULL_HOUSE);
            
            assertThat(result.getRankValues()).containsExactly(Value.KING, Value.ACE);
        }

        @Test
        @DisplayName("Two possible full houses - picks best")
        void twoPossibleFullHousesPicksBest() {
            
            HandRanking result = evaluate("Kh", "Kd", "Ah", "Ad", "Ac", "2s", "3c");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FULL_HOUSE);
            assertThat(result.getRankValues()).containsExactly(Value.ACE, Value.KING);
        }
    }

    
    
    

    @Nested
    @DisplayName("Flush Tests")
    class FlushTests {

        @Test
        @DisplayName("Ace-high Flush")
        void aceHighFlush() {
            HandRanking result = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FLUSH);
            assertThat(result.getKickerValues())
                .containsExactly(Value.ACE, Value.KING, Value.QUEEN, Value.JACK, Value.NINE);
        }

        @Test
        @DisplayName("King-high Flush")
        void kingHighFlush() {
            HandRanking result = evaluate("Kh", "Qh", "Jh", "9h", "7h", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FLUSH);
            assertThat(result.getKickerValues().get(0)).isEqualTo(Value.KING);
        }

        @Test
        @DisplayName("Six suited cards - picks best 5")
        void sixSuitedCardsBestFive() {
            
            HandRanking result = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "7h", "2c");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FLUSH);
            assertThat(result.getKickerValues())
                .containsExactly(Value.ACE, Value.KING, Value.QUEEN, Value.JACK, Value.NINE);
        }

        @Test
        @DisplayName("Seven suited cards - picks best 5")
        void sevenSuitedCardsBestFive() {
            
            HandRanking result = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "7h", "5h");
            
            assertThat(result.getHandType()).isEqualTo(HandType.FLUSH);
            assertThat(result.getKickerValues())
                .containsExactly(Value.ACE, Value.KING, Value.QUEEN, Value.JACK, Value.NINE);
        }

        @Test
        @DisplayName("Ace-high Flush beats King-high Flush")
        void aceHighBeatsKingHigh() {
            HandRanking aceHigh = evaluate("Ah", "Qh", "Jh", "9h", "7h", "2c", "3s");
            HandRanking kingHigh = evaluate("Kh", "Qh", "Jh", "9h", "7h", "2c", "3s");
            
            assertThat(aceHigh.compareTo(kingHigh)).isPositive();
        }

        @Test
        @DisplayName("Same high card - second kicker decides")
        void secondKickerDecides() {
            HandRanking aceKing = evaluate("Ah", "Kh", "Jh", "9h", "7h", "2c", "3s");
            HandRanking aceQueen = evaluate("Ah", "Qh", "Jh", "9h", "7h", "2c", "3s");
            
            assertThat(aceKing.compareTo(aceQueen)).isPositive();
        }

        @Test
        @DisplayName("Fifth kicker decides tie")
        void fifthKickerDecides() {
            HandRanking flush1 = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s");
            HandRanking flush2 = evaluate("Ah", "Kh", "Qh", "Jh", "8h", "2c", "3s");
            
            assertThat(flush1.compareTo(flush2)).isPositive();
        }

        @Test
        @DisplayName("Identical Flushes tie")
        void identicalFlushesTie() {
            HandRanking flush1 = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s");
            HandRanking flush2 = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "4c", "5s");
            
            assertThat(flush1.compareTo(flush2)).isZero();
        }

        @Test
        @DisplayName("Flush beats Straight")
        void flushBeatsStraight() {
            HandRanking flush = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s");
            HandRanking straight = evaluate("Ah", "Kd", "Qc", "Js", "Th", "2c", "3s");
            
            assertThat(flush.compareTo(straight)).isPositive();
        }
    }

    
    
    

    @Nested
    @DisplayName("Straight Tests")
    class StraightTests {

        @Test
        @DisplayName("Broadway Straight - A K Q J T")
        void broadwayStraight() {
            HandRanking result = evaluate("Ah", "Kd", "Qc", "Js", "Th", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT);
            assertThat(result.getRankValues()).containsExactly(Value.ACE);
        }

        @Test
        @DisplayName("Wheel (5-high) - 5 4 3 2 A")
        void wheelStraight() {
            HandRanking result = evaluate("Ah", "2d", "3c", "4s", "5h", "Kc", "Qs");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT);
            assertThat(result.getRankValues()).containsExactly(Value.FIVE);
        }

        @Test
        @DisplayName("King-high Straight - K Q J T 9")
        void kingHighStraight() {
            HandRanking result = evaluate("Kh", "Qd", "Jc", "Ts", "9h", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT);
            assertThat(result.getRankValues()).containsExactly(Value.KING);
        }

        @Test
        @DisplayName("Middle Straight - 8 7 6 5 4")
        void middleStraight() {
            HandRanking result = evaluate("8h", "7d", "6c", "5s", "4h", "Ac", "Ks");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT);
            assertThat(result.getRankValues()).containsExactly(Value.EIGHT);
        }

        @Test
        @DisplayName("Six-high Straight - 6 5 4 3 2")
        void sixHighStraight() {
            HandRanking result = evaluate("6h", "5d", "4c", "3s", "2h", "Ac", "Ks");
            
            assertThat(result.getHandType()).isEqualTo(HandType.STRAIGHT);
            assertThat(result.getRankValues()).containsExactly(Value.SIX);
        }

        @Test
        @DisplayName("Broadway beats King-high Straight")
        void broadwayBeatsKingHigh() {
            HandRanking broadway = evaluate("Ah", "Kd", "Qc", "Js", "Th", "2c", "3s");
            HandRanking kingHigh = evaluate("Kh", "Qd", "Jc", "Ts", "9h", "2c", "3s");
            
            assertThat(broadway.compareTo(kingHigh)).isPositive();
        }

        @Test
        @DisplayName("Wheel loses to Six-high Straight")
        void wheelLowestStraight() {
            HandRanking wheel = evaluate("Ah", "2d", "3c", "4s", "5h", "Kc", "Qs");
            HandRanking sixHigh = evaluate("6h", "5d", "4c", "3s", "2h", "Ac", "Ks");
            
            assertThat(wheel.compareTo(sixHigh)).isNegative();
        }

        @Test
        @DisplayName("Wrap-around NOT allowed - K A 2 3 4 is NOT a straight")
        void wrapAroundNotAllowed() {
            
            HandRanking result = evaluate("Kh", "Ad", "2c", "3s", "4h", "7c", "8s");
            
            assertThat(result.getHandType()).isNotEqualTo(HandType.STRAIGHT);
        }

        @Test
        @DisplayName("Q K A 2 3 is NOT a straight")
        void anotherWrapAroundNotAllowed() {
            HandRanking result = evaluate("Qh", "Kd", "Ac", "2s", "3h", "7c", "8s");
            
            assertThat(result.getHandType()).isNotEqualTo(HandType.STRAIGHT);
        }

        @Test
        @DisplayName("Same height Straights tie")
        void sameHeightStraightsTie() {
            HandRanking straight1 = evaluate("Ah", "Kd", "Qc", "Js", "Th", "2c", "3s");
            HandRanking straight2 = evaluate("As", "Kh", "Qd", "Jc", "Ts", "4c", "5s");
            
            assertThat(straight1.compareTo(straight2)).isZero();
        }

        @Test
        @DisplayName("Straight beats Three of a Kind")
        void straightBeatsTrips() {
            HandRanking straight = evaluate("Ah", "Kd", "Qc", "Js", "Th", "2c", "3s");
            HandRanking trips = evaluate("Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s");
            
            assertThat(straight.compareTo(trips)).isPositive();
        }
    }

    
    
    

    @Nested
    @DisplayName("Three of a Kind Tests")
    class ThreeOfAKindTests {

        @Test
        @DisplayName("Trip Aces with King-Queen kickers")
        void tripAcesKingQueenKickers() {
            HandRanking result = evaluate("Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.THREE_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.ACE);
            assertThat(result.getKickerValues()).containsExactly(Value.KING, Value.QUEEN);
        }

        @Test
        @DisplayName("Trip Twos (lowest trips)")
        void tripTwos() {
            HandRanking result = evaluate("2h", "2d", "2c", "As", "Kh", "Qc", "Js");
            
            assertThat(result.getHandType()).isEqualTo(HandType.THREE_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.TWO);
        }

        @Test
        @DisplayName("Set (pocket pair hits board)")
        void setFromPocketPair() {
            
            HandRanking result = evaluate("7h", "7d", "7c", "As", "Kh", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.THREE_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.SEVEN);
        }

        @Test
        @DisplayName("Trips on board (one hole card plays)")
        void tripsOnBoard() {
            
            HandRanking result = evaluate("Ah", "2d", "8c", "8s", "8h", "Kc", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.THREE_OF_A_KIND);
            assertThat(result.getRankValues()).containsExactly(Value.EIGHT);
            assertThat(result.getKickerValues()).containsExactly(Value.ACE, Value.KING);
        }

        @Test
        @DisplayName("Trip Aces beats Trip Kings")
        void tripAcesBeatTripKings() {
            HandRanking tripAces = evaluate("Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s");
            HandRanking tripKings = evaluate("Kh", "Kd", "Kc", "As", "Qh", "2c", "3s");
            
            assertThat(tripAces.compareTo(tripKings)).isPositive();
        }

        @Test
        @DisplayName("Same trips - higher kicker wins")
        void sameTripsKickerWins() {
            HandRanking tripsAceKicker = evaluate("7h", "7d", "7c", "As", "Kh", "2c", "3s");
            HandRanking tripsKingKicker = evaluate("7h", "7d", "7c", "Ks", "Qh", "2c", "3s");
            
            assertThat(tripsAceKicker.compareTo(tripsKingKicker)).isPositive();
        }

        @Test
        @DisplayName("Same trips and first kicker - second kicker decides")
        void secondKickerDecides() {
            HandRanking trips1 = evaluate("7h", "7d", "7c", "As", "Kh", "2c", "3s");
            HandRanking trips2 = evaluate("7h", "7d", "7c", "As", "Qh", "2c", "3s");
            
            assertThat(trips1.compareTo(trips2)).isPositive();
        }

        @Test
        @DisplayName("Three of a Kind beats Two Pair")
        void tripsBeatsTwoPair() {
            HandRanking trips = evaluate("Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s");
            HandRanking twoPair = evaluate("Ah", "Ad", "Kc", "Ks", "Qh", "2c", "3s");
            
            assertThat(trips.compareTo(twoPair)).isPositive();
        }
    }

    
    
    

    @Nested
    @DisplayName("Two Pair Tests")
    class TwoPairTests {

        @Test
        @DisplayName("Aces and Kings with Queen kicker")
        void acesAndKingsQueenKicker() {
            HandRanking result = evaluate("Ah", "Ad", "Kc", "Ks", "Qh", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.TWO_PAIR);
            assertThat(result.getRankValues()).containsExactly(Value.ACE, Value.KING);
            assertThat(result.getKickerValues()).containsExactly(Value.QUEEN);
        }

        @Test
        @DisplayName("Twos and Threes (lowest two pair)")
        void twosAndThrees() {
            HandRanking result = evaluate("2h", "2d", "3c", "3s", "Ah", "Kc", "Qs");
            
            assertThat(result.getHandType()).isEqualTo(HandType.TWO_PAIR);
            assertThat(result.getRankValues()).containsExactly(Value.THREE, Value.TWO);
        }

        @Test
        @DisplayName("Higher top pair wins")
        void higherTopPairWins() {
            HandRanking acesAndKings = evaluate("Ah", "Ad", "Kc", "Ks", "2h", "3c", "4s");
            HandRanking kingsAndQueens = evaluate("Kh", "Kd", "Qc", "Qs", "Ah", "2c", "3s");
            
            assertThat(acesAndKings.compareTo(kingsAndQueens)).isPositive();
        }

        @Test
        @DisplayName("Same top pair - higher bottom pair wins")
        void sameTopPairHigherBottomWins() {
            HandRanking acesAndKings = evaluate("Ah", "Ad", "Kc", "Ks", "2h", "3c", "4s");
            HandRanking acesAndQueens = evaluate("Ah", "Ad", "Qc", "Qs", "2h", "3c", "4s");
            
            assertThat(acesAndKings.compareTo(acesAndQueens)).isPositive();
        }

        @Test
        @DisplayName("Same two pairs - kicker decides")
        void sameTwoPairsKickerDecides() {
            HandRanking kingKicker = evaluate("Ah", "Ad", "Qc", "Qs", "Kh", "2c", "3s");
            HandRanking jackKicker = evaluate("Ah", "Ad", "Qc", "Qs", "Jh", "2c", "3s");
            
            assertThat(kingKicker.compareTo(jackKicker)).isPositive();
        }

        @Test
        @DisplayName("Three pairs on board - picks best two")
        void threePairsPicksBestTwo() {
            
            HandRanking result = evaluate("Ah", "Ad", "Kc", "Ks", "Qh", "Qc", "Js");
            
            assertThat(result.getHandType()).isEqualTo(HandType.TWO_PAIR);
            assertThat(result.getRankValues()).containsExactly(Value.ACE, Value.KING);
        }

        @Test
        @DisplayName("Two Pair beats One Pair")
        void twoPairBeatsOnePair() {
            HandRanking twoPair = evaluate("Ah", "Ad", "Kc", "Ks", "Qh", "2c", "3s");
            HandRanking onePair = evaluate("Ah", "Ad", "Kc", "Qs", "Jh", "2c", "3s");
            
            assertThat(twoPair.compareTo(onePair)).isPositive();
        }

        @Test
        @DisplayName("Identical Two Pairs tie")
        void identicalTwoPairsTie() {
            HandRanking tp1 = evaluate("Ah", "Ad", "Kc", "Ks", "Qh", "2c", "3s");
            HandRanking tp2 = evaluate("As", "Ac", "Kh", "Kd", "Qd", "4c", "5s");
            
            assertThat(tp1.compareTo(tp2)).isZero();
        }
    }

    
    
    

    @Nested
    @DisplayName("One Pair Tests")
    class OnePairTests {

        @Test
        @DisplayName("Pocket Aces with K Q J kickers")
        void pocketAces() {
            HandRanking result = evaluate("Ah", "Ad", "Kc", "Qs", "Jh", "2c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.ONE_PAIR);
            assertThat(result.getRankValues()).containsExactly(Value.ACE);
            assertThat(result.getKickerValues()).containsExactly(Value.KING, Value.QUEEN, Value.JACK);
        }

        @Test
        @DisplayName("Pocket Twos (lowest pair)")
        void pocketTwos() {
            HandRanking result = evaluate("2h", "2d", "Ac", "Ks", "Qh", "Jc", "9s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.ONE_PAIR);
            assertThat(result.getRankValues()).containsExactly(Value.TWO);
        }

        @Test
        @DisplayName("Board pair with hole card kicker")
        void boardPairHoleCardKicker() {
            
            HandRanking result = evaluate("Ah", "2d", "Kc", "Ks", "Qh", "Jc", "9s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.ONE_PAIR);
            assertThat(result.getRankValues()).containsExactly(Value.KING);
            assertThat(result.getKickerValues().get(0)).isEqualTo(Value.ACE);
        }

        @Test
        @DisplayName("Higher pair wins")
        void higherPairWins() {
            HandRanking pairAces = evaluate("Ah", "Ad", "Kc", "Qs", "Jh", "2c", "3s");
            HandRanking pairKings = evaluate("Kh", "Kd", "Ac", "Qs", "Jh", "2c", "3s");
            
            assertThat(pairAces.compareTo(pairKings)).isPositive();
        }

        @Test
        @DisplayName("Same pair - first kicker decides")
        void samePairFirstKickerDecides() {
            HandRanking aceKicker = evaluate("Kh", "Kd", "Ac", "Qs", "Jh", "2c", "3s");
            HandRanking queenKicker = evaluate("Kh", "Kd", "Qc", "Js", "Th", "2c", "3s");
            
            assertThat(aceKicker.compareTo(queenKicker)).isPositive();
        }

        @Test
        @DisplayName("Same pair and kickers - third kicker decides")
        void thirdKickerDecides() {
            HandRanking pair1 = evaluate("Kh", "Kd", "Ac", "Qs", "Jh", "2c", "3s");
            HandRanking pair2 = evaluate("Kh", "Kd", "Ac", "Qs", "Th", "2c", "3s");
            
            assertThat(pair1.compareTo(pair2)).isPositive();
        }

        @Test
        @DisplayName("One Pair beats High Card")
        void onePairBeatsHighCard() {
            HandRanking onePair = evaluate("2h", "2d", "Ac", "Ks", "Qh", "Jc", "9s");
            HandRanking highCard = evaluate("Ah", "Kd", "Qc", "Js", "9h", "7c", "5s");
            
            assertThat(onePair.compareTo(highCard)).isPositive();
        }

        @Test
        @DisplayName("Identical Pairs tie")
        void identicalPairsTie() {
            HandRanking pair1 = evaluate("Ah", "Ad", "Kc", "Qs", "Jh", "2c", "3s");
            HandRanking pair2 = evaluate("As", "Ac", "Kh", "Qd", "Jd", "4c", "5s");
            
            assertThat(pair1.compareTo(pair2)).isZero();
        }
    }

    
    
    

    @Nested
    @DisplayName("High Card Tests")
    class HighCardTests {

        @Test
        @DisplayName("Ace-high with K Q J 9")
        void aceHighKQJ9() {
            HandRanking result = evaluate("Ah", "Kd", "Qc", "Js", "9h", "7c", "5s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.HIGH_CARD);
            assertThat(result.getKickerValues())
                .containsExactly(Value.ACE, Value.KING, Value.QUEEN, Value.JACK, Value.NINE);
        }

        @Test
        @DisplayName("King-high")
        void kingHigh() {
            HandRanking result = evaluate("Kh", "Qd", "Jc", "9s", "7h", "5c", "3s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.HIGH_CARD);
            assertThat(result.getKickerValues().get(0)).isEqualTo(Value.KING);
        }

        @Test
        @DisplayName("Seven-high (near worst possible)")
        void sevenHigh() {
            HandRanking result = evaluate("7h", "5d", "4c", "3s", "2h", "9c", "8s");
            
            assertThat(result.getHandType()).isEqualTo(HandType.HIGH_CARD);
            
            assertThat(result.getKickerValues().get(0)).isEqualTo(Value.NINE);
        }

        @Test
        @DisplayName("Ace-high beats King-high")
        void aceHighBeatsKingHigh() {
            HandRanking aceHigh = evaluate("Ah", "2d", "4c", "6s", "8h", "Tc", "Qs");
            HandRanking kingHigh = evaluate("Kh", "3d", "5c", "7s", "9h", "Jc", "2s");
            
            assertThat(aceHigh.compareTo(kingHigh)).isPositive();
        }

        @Test
        @DisplayName("Same high card - second kicker decides")
        void secondKickerDecides() {
            HandRanking aceKing = evaluate("Ah", "Kd", "4c", "6s", "8h", "2c", "3s");
            HandRanking aceQueen = evaluate("Ah", "Qd", "4c", "6s", "8h", "2c", "3s");
            
            assertThat(aceKing.compareTo(aceQueen)).isPositive();
        }

        @Test
        @DisplayName("Full 5-kicker comparison")
        void fullKickerComparison() {
            HandRanking hc1 = evaluate("Ah", "Kd", "Qc", "Js", "9h", "2c", "3s");
            HandRanking hc2 = evaluate("Ah", "Kd", "Qc", "Js", "8h", "2c", "3s");
            
            assertThat(hc1.compareTo(hc2)).isPositive();
        }

        @Test
        @DisplayName("Identical High Cards tie (split pot)")
        void identicalHighCardsTie() {
            HandRanking hc1 = evaluate("Ah", "Kd", "Qc", "Js", "9h", "2c", "3s");
            HandRanking hc2 = evaluate("As", "Kh", "Qd", "Jc", "9d", "4c", "5s");
            
            assertThat(hc1.compareTo(hc2)).isZero();
        }
    }

    
    
    

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Null hole cards throws NullPointerException")
        void nullHoleCardsThrowsException() {
            List<Card> community = cards("Ah", "Kd", "Qc", "Js", "Th");
            
            
            assertThatThrownBy(() -> handEvaluator.evaluate(null, community))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Null community cards throws NullPointerException")
        void nullCommunityCardsThrowsException() {
            List<Card> holeCards = cards("Ah", "Kd");
            
            
            assertThatThrownBy(() -> handEvaluator.evaluate(holeCards, null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Empty hole cards returns null - not exactly 7 cards")
        void emptyHoleCardsReturnsNull() {
            List<Card> holeCards = new ArrayList<>();
            List<Card> community = cards("Ah", "Kd", "Qc", "Js", "Th"); 
            
            
            HandRanking result = handEvaluator.evaluate(holeCards, community);
            
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Less than 7 cards returns null")
        void lessThan7CardsReturnsNull() {
            List<Card> holeCards = cards("Ah", "Kd"); 
            List<Card> community = cards("Qc", "Js", "Th", "9s"); 
            
            
            HandRanking result = handEvaluator.evaluate(holeCards, community);
            
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("More than 7 cards returns null")
        void moreThan7CardsReturnsNull() {
            List<Card> holeCards = cards("Ah", "Kd", "Qc"); 
            List<Card> community = cards("Js", "Th", "9s", "8h", "7d"); 
            
            
            HandRanking result = handEvaluator.evaluate(holeCards, community);
            
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Tie scenario - exact same hands")
        void exactTieScenario() {
            
            List<Card> hero = cards("2h", "3d");
            List<Card> villain = cards("2s", "3c");
            List<Card> board = cards("Ah", "Kd", "Qc", "Js", "Th"); 
            
            HandRanking heroRanking = handEvaluator.evaluate(hero, board);
            HandRanking villainRanking = handEvaluator.evaluate(villain, board);
            
            assertThat(heroRanking.compareTo(villainRanking)).isZero();
            assertThat(heroRanking.getHandType()).isEqualTo(HandType.STRAIGHT);
        }

        @Test
        @DisplayName("Chop pot - both have same flush from board")
        void chopPotSameFlushOnBoard() {
            
            List<Card> hero = cards("2c", "3d");
            List<Card> villain = cards("4c", "5d");
            List<Card> board = cards("Ah", "Kh", "Qh", "Jh", "9h");
            
            HandRanking heroRanking = handEvaluator.evaluate(hero, board);
            HandRanking villainRanking = handEvaluator.evaluate(villain, board);
            
            assertThat(heroRanking.compareTo(villainRanking)).isZero();
        }

        @Test
        @DisplayName("Hand description is correct for Royal Flush")
        void handDescriptionRoyalFlush() {
            HandRanking result = evaluate("As", "Ks", "Qs", "Js", "Ts", "2c", "3d");
            
            assertThat(result.getDescription()).isEqualTo("Royal Flush");
        }

        @Test
        @DisplayName("Hand description is correct for Full House")
        void handDescriptionFullHouse() {
            HandRanking result = evaluate("Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s");
            
            assertThat(result.getDescription()).contains("Full House");
            assertThat(result.getDescription()).contains("Ace");
            assertThat(result.getDescription()).contains("King");
        }
    }

    
    
    

    @Nested
    @DisplayName("Hand Type Hierarchy Tests")
    class HandTypeHierarchyTests {

        @Test
        @DisplayName("Royal Flush > Straight Flush > Four of a Kind")
        void topThreeHierarchy() {
            HandRanking royalFlush = evaluate("As", "Ks", "Qs", "Js", "Ts", "2c", "3d");
            HandRanking straightFlush = evaluate("Ks", "Qs", "Js", "Ts", "9s", "2c", "3d");
            HandRanking quads = evaluate("Ah", "Ad", "Ac", "As", "Kh", "2c", "3d");
            
            assertThat(royalFlush.compareTo(straightFlush)).isPositive();
            assertThat(straightFlush.compareTo(quads)).isPositive();
        }

        @Test
        @DisplayName("Four of a Kind > Full House > Flush")
        void middleHighHierarchy() {
            HandRanking quads = evaluate("Ah", "Ad", "Ac", "As", "Kh", "2c", "3d");
            HandRanking fullHouse = evaluate("Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s");
            HandRanking flush = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s");
            
            assertThat(quads.compareTo(fullHouse)).isPositive();
            assertThat(fullHouse.compareTo(flush)).isPositive();
        }

        @Test
        @DisplayName("Flush > Straight > Three of a Kind")
        void middleLowHierarchy() {
            HandRanking flush = evaluate("Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s");
            HandRanking straight = evaluate("Ah", "Kd", "Qc", "Js", "Th", "2c", "3s");
            HandRanking trips = evaluate("Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s");
            
            assertThat(flush.compareTo(straight)).isPositive();
            assertThat(straight.compareTo(trips)).isPositive();
        }

        @Test
        @DisplayName("Three of a Kind > Two Pair > One Pair > High Card")
        void bottomHierarchy() {
            HandRanking trips = evaluate("Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s");
            HandRanking twoPair = evaluate("Ah", "Ad", "Kc", "Ks", "Qh", "2c", "3s");
            HandRanking onePair = evaluate("Ah", "Ad", "Kc", "Qs", "Jh", "2c", "3s");
            HandRanking highCard = evaluate("Ah", "Kd", "Qc", "Js", "9h", "7c", "5s");
            
            assertThat(trips.compareTo(twoPair)).isPositive();
            assertThat(twoPair.compareTo(onePair)).isPositive();
            assertThat(onePair.compareTo(highCard)).isPositive();
        }

        @ParameterizedTest(name = "{0} beats {1}")
        @MethodSource("handTypeComparisonProvider")
        @DisplayName("Complete hand type hierarchy verification")
        void completeHierarchy(String betterHand, String worseHand, 
                                String[] betterCards, String[] worseCards) {
            HandRanking better = evaluate(betterCards[0], betterCards[1], betterCards[2],
                                          betterCards[3], betterCards[4], betterCards[5], betterCards[6]);
            HandRanking worse = evaluate(worseCards[0], worseCards[1], worseCards[2],
                                         worseCards[3], worseCards[4], worseCards[5], worseCards[6]);
            
            assertThat(better.compareTo(worse))
                .as("%s should beat %s", betterHand, worseHand)
                .isPositive();
        }

        static Stream<Arguments> handTypeComparisonProvider() {
            return Stream.of(
                Arguments.of("Royal Flush", "Straight Flush",
                    new String[]{"As", "Ks", "Qs", "Js", "Ts", "2c", "3d"},
                    new String[]{"Ks", "Qs", "Js", "Ts", "9s", "2c", "3d"}),
                Arguments.of("Straight Flush", "Four of a Kind",
                    new String[]{"9h", "8h", "7h", "6h", "5h", "2c", "3d"},
                    new String[]{"Ah", "Ad", "Ac", "As", "Kh", "2c", "3d"}),
                Arguments.of("Four of a Kind", "Full House",
                    new String[]{"Ah", "Ad", "Ac", "As", "Kh", "2c", "3d"},
                    new String[]{"Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s"}),
                Arguments.of("Full House", "Flush",
                    new String[]{"Ah", "Ad", "Ac", "Kh", "Kd", "2c", "3s"},
                    new String[]{"Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s"}),
                Arguments.of("Flush", "Straight",
                    new String[]{"Ah", "Kh", "Qh", "Jh", "9h", "2c", "3s"},
                    new String[]{"Ah", "Kd", "Qc", "Js", "Th", "2c", "3s"}),
                Arguments.of("Straight", "Three of a Kind",
                    new String[]{"Ah", "Kd", "Qc", "Js", "Th", "2c", "3s"},
                    new String[]{"Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s"}),
                Arguments.of("Three of a Kind", "Two Pair",
                    new String[]{"Ah", "Ad", "Ac", "Ks", "Qh", "2c", "3s"},
                    new String[]{"Ah", "Ad", "Kc", "Ks", "Qh", "2c", "3s"}),
                Arguments.of("Two Pair", "One Pair",
                    new String[]{"Ah", "Ad", "Kc", "Ks", "Qh", "2c", "3s"},
                    new String[]{"Ah", "Ad", "Kc", "Qs", "Jh", "2c", "3s"}),
                Arguments.of("One Pair", "High Card",
                    new String[]{"2h", "2d", "Ac", "Ks", "Qh", "Jc", "9s"},
                    new String[]{"Ah", "Kd", "Qc", "Js", "9h", "7c", "5s"})
            );
        }
    }

    
    
    

    @Nested
    @DisplayName("TestFixtures Integration Tests")
    class TestFixturesIntegrationTests {

        @Test
        @DisplayName("Pocket Aces from TestFixtures")
        void pocketAcesFromFixtures() {
            List<Card> holeCards = TestFixtures.POCKET_ACES;
            List<Card> community = cards("Kh", "Qd", "Jc", "9s", "7h");
            
            HandRanking result = handEvaluator.evaluate(holeCards, community);
            
            assertThat(result.getHandType()).isEqualTo(HandType.ONE_PAIR);
            assertThat(result.getRankValues()).containsExactly(Value.ACE);
        }

        @Test
        @DisplayName("Pocket Kings vs Pocket Aces (cooler)")
        void coolersFromFixtures() {
            
            List<Card> community = cards("7h", "8d", "2c", "3s", "5h");
            
            HandRanking aaRanking = handEvaluator.evaluate(TestFixtures.POCKET_ACES, community);
            HandRanking kkRanking = handEvaluator.evaluate(TestFixtures.POCKET_KINGS, community);
            
            assertThat(aaRanking.compareTo(kkRanking)).isPositive();
        }

        @Test
        @DisplayName("Seven-Two offsuit is indeed terrible")
        void sevenTwoOffsuitIsTerrible() {
            List<Card> community = cards("Ah", "Kd", "Qc", "Js", "9h");
            
            HandRanking result = handEvaluator.evaluate(TestFixtures.SEVEN_TWO_OFFSUIT, community);
            
            
            assertThat(result.getHandType()).isEqualTo(HandType.HIGH_CARD);
        }

        @Test
        @DisplayName("Create cards using TestFixtures.card() helper")
        void useCardHelper() {
            Card aceSpades = TestFixtures.card(Suit.SPADES, Value.ACE);
            
            assertThat(aceSpades.getSuit()).isEqualTo(Suit.SPADES);
            assertThat(aceSpades.getValue()).isEqualTo(Value.ACE);
        }
    }
}
