package com.truholdem.service;

import com.truholdem.model.Card;
import com.truholdem.model.HandRanking;
import com.truholdem.model.HandType;
import com.truholdem.model.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HandEvaluator {

    public HandRanking evaluate(List<Card> playerHand, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>(playerHand);
        allCards.addAll(communityCards);

        if (allCards.size() != 7) {
            return null;
        }

        List<List<Card>> all5CardHands = new ArrayList<>();
        generateCombinations(allCards, 5, 0, new ArrayList<>(), all5CardHands);

        HandRanking bestHandRanking = null;

        for (List<Card> hand : all5CardHands) {
            HandRanking currentRanking = evaluate5CardHand(hand);
            if (bestHandRanking == null || currentRanking.compareTo(bestHandRanking) > 0) {
                bestHandRanking = currentRanking;
            }
        }

        return bestHandRanking;
    }

    private HandRanking evaluate5CardHand(List<Card> hand) {
        if (hand.size() != 5) throw new IllegalArgumentException("Hand must be 5 cards");

        hand.sort(Comparator.comparing(Card::getValue).reversed());

        boolean isFlush = hand.stream().map(Card::getSuit).distinct().count() == 1;

        boolean isStraight = true;
        for (int i = 0; i < 4; i++) {
            if (hand.get(i).getValue().ordinal() - hand.get(i+1).getValue().ordinal() != 1) {
                isStraight = false;
                break;
            }
        }
        boolean isWheel = !isStraight && hand.get(0).getValue() == Value.ACE && hand.get(1).getValue() == Value.FIVE && hand.get(2).getValue() == Value.FOUR && hand.get(3).getValue() == Value.THREE && hand.get(4).getValue() == Value.TWO;
        if(isWheel) isStraight = true;

        if (isStraight && isFlush) {
            Value highCard = isWheel ? Value.FIVE : hand.get(0).getValue();
            if (highCard == Value.ACE) return new HandRanking(HandType.ROYAL_FLUSH, List.of(highCard), List.of());
            return new HandRanking(HandType.STRAIGHT_FLUSH, List.of(highCard), List.of());
        }

        Map<Value, Long> counts = hand.stream().collect(Collectors.groupingBy(Card::getValue, Collectors.counting()));

        List<Value> fourOfAKindRanks = getRanksOfSize(counts, 4);
        if (!fourOfAKindRanks.isEmpty()) {
            return new HandRanking(HandType.FOUR_OF_A_KIND, fourOfAKindRanks, getKickers(counts, fourOfAKindRanks));
        }

        List<Value> threeOfAKindRanks = getRanksOfSize(counts, 3);
        List<Value> pairRanks = getRanksOfSize(counts, 2);

        if (!threeOfAKindRanks.isEmpty() && !pairRanks.isEmpty()) {
            List<Value> fullHouseRanks = new ArrayList<>(threeOfAKindRanks);
            fullHouseRanks.addAll(pairRanks);
            return new HandRanking(HandType.FULL_HOUSE, fullHouseRanks, List.of());
        }

        if (isFlush) {
            return new HandRanking(HandType.FLUSH, List.of(), hand.stream().map(Card::getValue).toList());
        }

        if (isStraight) {
            Value highCard = isWheel ? Value.FIVE : hand.get(0).getValue();
            return new HandRanking(HandType.STRAIGHT, List.of(highCard), List.of());
        }

        if (!threeOfAKindRanks.isEmpty()) {
            return new HandRanking(HandType.THREE_OF_A_KIND, threeOfAKindRanks, getKickers(counts, threeOfAKindRanks));
        }

        if (pairRanks.size() == 2) {
            return new HandRanking(HandType.TWO_PAIR, pairRanks, getKickers(counts, pairRanks));
        }

        if (pairRanks.size() == 1) {
            return new HandRanking(HandType.ONE_PAIR, pairRanks, getKickers(counts, pairRanks));
        }

        return new HandRanking(HandType.HIGH_CARD, List.of(), hand.stream().map(Card::getValue).toList());
    }

    private List<Value> getRanksOfSize(Map<Value, Long> counts, int size) {
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() == size)
                .map(Map.Entry::getKey)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    private List<Value> getKickers(Map<Value, Long> counts, List<Value> ranksToExclude) {
        return counts.entrySet().stream()
                .filter(entry -> !ranksToExclude.contains(entry.getKey()))
                .map(Map.Entry::getKey)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    private void generateCombinations(List<Card> allCards, int k, int start, List<Card> currentCombination, List<List<Card>> combinations) {
        if (k == 0) {
            combinations.add(new ArrayList<>(currentCombination));
            return;
        }
        for (int i = start; i <= allCards.size() - k; i++) {
            currentCombination.add(allCards.get(i));
            generateCombinations(allCards, k - 1, i + 1, currentCombination, combinations);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }
}
