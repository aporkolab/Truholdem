package com.truholdem.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.truholdem.model.Card;
import com.truholdem.model.Game;
import com.truholdem.model.GamePhase;
import com.truholdem.model.HandRanking;
import com.truholdem.model.Player;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.Suit;
import com.truholdem.model.Value;


@Service
public class AdvancedBotAIService {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedBotAIService.class);
    private static final int MONTE_CARLO_ITERATIONS = 500;

    private final HandEvaluator handEvaluator;


    private final Map<UUID, OpponentModel> opponentModels = new ConcurrentHashMap<>();

    public AdvancedBotAIService(HandEvaluator handEvaluator) {
        this.handEvaluator = handEvaluator;
    }

    

    
    public BotDecision decide(Game game, Player bot) {
        
        double handStrength = calculateHandStrength(bot.getHand(), game.getCommunityCards(), game.getPlayers().size());
        double potOdds = calculatePotOdds(game, bot);
        int position = getPositionScore(game, bot);
        BotPersonality personality = getBotPersonality(bot.getName());

        
        double adjustedStrength = adjustForPersonality(handStrength, personality, game.getPhase());

        logger.debug("Bot {} decision: strength={:.2f}, potOdds={:.2f}, position={}",
                bot.getName(), handStrength, potOdds, position);

        
        if (game.getPhase() == GamePhase.PRE_FLOP) {
            return preFlopDecision(game, bot, adjustedStrength, position, personality);
        }

        
        return postFlopDecision(game, bot, adjustedStrength, potOdds, position, personality);
    }

    

    private BotDecision preFlopDecision(Game game, Player bot, double strength, int position,
            BotPersonality personality) {
        int currentBet = game.getCurrentBet();
        int botBet = bot.getBetAmount();
        int toCall = currentBet - botBet;
        int chips = bot.getChips();
        int pot = game.getCurrentPot();

        // Calculate pot odds for all-in decisions
        double potOdds = (toCall > 0) ? (double) toCall / (pot + toCall) : 0;
        boolean isFacingAllIn = toCall >= chips * 0.5; // Consider it all-in if need to call 50%+ of stack

        // Premium hands (AA, KK, QQ, AKs) - always play aggressively
        if (strength > 0.85) {
            if (isFacingAllIn) {
                return new BotDecision(PlayerAction.CALL, 0, "Premium hand, calling all-in");
            }
            int raiseAmount = calculateRaiseAmount(game, bot, strength, personality);
            return new BotDecision(PlayerAction.RAISE, raiseAmount, "Premium hand");
        }

        // Strong hands (JJ, TT, AK, AQs) - call all-ins, raise otherwise
        if (strength > 0.70) {
            if (isFacingAllIn) {
                // Strong hands should call all-ins most of the time
                double callChance = 0.7 + (strength - 0.70) * 2; // 70-100% chance based on strength
                if (ThreadLocalRandom.current().nextDouble() < callChance) {
                    return new BotDecision(PlayerAction.CALL, 0, "Strong hand, calling all-in");
                }
                return new BotDecision(PlayerAction.FOLD, 0, "Strong hand, folding to all-in");
            }
            if (position >= 2) {
                int raiseAmount = calculateRaiseAmount(game, bot, strength, personality);
                return new BotDecision(PlayerAction.RAISE, raiseAmount, "Strong hand, late position");
            } else {
                return toCall > 0
                        ? new BotDecision(PlayerAction.CALL, 0, "Strong hand, early position")
                        : new BotDecision(PlayerAction.CHECK, 0, "Strong hand, check");
            }
        }

        // Medium hands (99-77, AJ, KQ) - consider pot odds for all-ins
        if (strength > 0.50) {
            if (isFacingAllIn) {
                // Call if pot odds are favorable or hand is strong enough
                double requiredEquity = potOdds;
                double adjustedStrength = strength * (1 + personality.aggressionFactor * 0.1);
                if (adjustedStrength > requiredEquity) {
                    return new BotDecision(PlayerAction.CALL, 0, "Medium hand, pot odds favorable");
                }
                // Even with bad odds, sometimes call with medium+ hands (hero call)
                if (strength > 0.55 && ThreadLocalRandom.current().nextDouble() < 0.25) {
                    return new BotDecision(PlayerAction.CALL, 0, "Medium hand, hero call");
                }
                return new BotDecision(PlayerAction.FOLD, 0, "Medium hand, folding to all-in");
            }
            // Normal play - more generous call threshold
            double callThreshold = Math.max(personality.callThreshold, 0.15) * chips;
            if (toCall <= callThreshold) {
                return new BotDecision(PlayerAction.CALL, 0, "Medium hand, acceptable price");
            } else {
                return new BotDecision(PlayerAction.FOLD, 0, "Medium hand, too expensive");
            }
        }

        // Speculative hands (66-22, suited connectors)
        if (strength > 0.35) {
            if (isFacingAllIn) {
                // Occasionally call with speculative hands (gambling)
                if (strength > 0.40 && ThreadLocalRandom.current().nextDouble() < 0.15) {
                    return new BotDecision(PlayerAction.CALL, 0, "Speculative hand, gambling call");
                }
                return new BotDecision(PlayerAction.FOLD, 0, "Speculative hand, folding to all-in");
            }
            if (position >= 2 && toCall <= chips * 0.10) {
                return new BotDecision(PlayerAction.CALL, 0, "Speculative hand, implied odds");
            }
        }

        // Weak hands
        if (toCall == 0) {
            return new BotDecision(PlayerAction.CHECK, 0, "Weak hand, free play");
        }

        return new BotDecision(PlayerAction.FOLD, 0, "Weak hand");
    }

    

    private BotDecision postFlopDecision(Game game, Player bot, double strength, double potOdds,
            int position, BotPersonality personality) {
        int currentBet = game.getCurrentBet();
        int botBet = bot.getBetAmount();
        int toCall = currentBet - botBet;
        int chips = bot.getChips();
        int pot = game.getCurrentPot();
        int bigBlind = game.getBigBlind();

        // Detect if facing all-in (need to call 50%+ of remaining stack)
        boolean isFacingAllIn = toCall >= chips * 0.5;

        // Detect massive overbet (bet > 2x pot) - should fold weak/medium hands
        boolean isFacingOverbet = pot > 0 && toCall > pot * 2;

        // Facing massive overbet with weak/medium hands - usually fold
        if (isFacingOverbet && strength < 0.75) {
            // Only call overbet with very strong hands or occasionally as a hero call
            if (strength > 0.65 && ThreadLocalRandom.current().nextDouble() < 0.15) {
                return new BotDecision(PlayerAction.CALL, 0, "Hero call against overbet");
            }
            return new BotDecision(PlayerAction.FOLD, 0, "Folding to massive overbet");
        }

        // Strong made hands (two pair+, strong overpair)
        if (strength > 0.80) {
            if (isFacingAllIn) {
                // Always call all-in with strong hands
                return new BotDecision(PlayerAction.CALL, 0, "Strong hand, calling all-in");
            }
            if (currentBet == 0 || botBet == currentBet) {
                int betAmount = calculateBetAmount(pot, strength, personality, bigBlind);
                return new BotDecision(PlayerAction.BET, betAmount, "Strong made hand, value bet");
            } else {
                int raiseAmount = calculateRaiseAmount(game, bot, strength, personality);
                return new BotDecision(PlayerAction.RAISE, raiseAmount, "Strong made hand, raise for value");
            }
        }

        // Good hands (top pair good kicker, overpair)
        if (strength > 0.60) {
            if (isFacingAllIn) {
                // Usually call all-in with good hands, consider pot odds
                double callChance = 0.5 + (strength - 0.60) * 2.5; // 50-100% chance
                if (strength > potOdds || ThreadLocalRandom.current().nextDouble() < callChance) {
                    return new BotDecision(PlayerAction.CALL, 0, "Good hand, calling all-in");
                }
                return new BotDecision(PlayerAction.FOLD, 0, "Good hand, folding to all-in");
            }
            if (currentBet == 0) {
                int betAmount = calculateBetAmount(pot, strength, personality, bigBlind);
                return new BotDecision(PlayerAction.BET, betAmount, "Good hand, bet");
            } else if (strength > potOdds) {
                return new BotDecision(PlayerAction.CALL, 0, "Good hand, +EV call");
            } else {
                return shouldBluff(game, bot, position, personality)
                        ? new BotDecision(PlayerAction.RAISE, calculateBluffAmount(game, bot), "Bluff raise")
                        : new BotDecision(PlayerAction.FOLD, 0, "Good hand, bad pot odds");
            }
        }

        // Medium hands (middle pair, weak top pair)
        if (strength > 0.40) {
            if (isFacingAllIn) {
                // Sometimes call with medium hands if pot odds are good
                if (strength > potOdds * 0.9) {
                    return new BotDecision(PlayerAction.CALL, 0, "Medium hand, pot odds call");
                }
                // Occasional hero call
                if (strength > 0.50 && ThreadLocalRandom.current().nextDouble() < 0.20) {
                    return new BotDecision(PlayerAction.CALL, 0, "Medium hand, hero call");
                }
                return new BotDecision(PlayerAction.FOLD, 0, "Medium hand, folding to all-in");
            }
            double requiredEquity = potOdds;
            if (strength + getImpliedOddsBonus(game) > requiredEquity) {
                return toCall > 0
                        ? new BotDecision(PlayerAction.CALL, 0, "Drawing hand, implied odds")
                        : new BotDecision(PlayerAction.CHECK, 0, "Drawing hand, free card");
            }
        }

        // Weak hands or draws
        if (toCall == 0) {
            if (shouldBluff(game, bot, position, personality)) {
                int bluffAmount = calculateBluffAmount(game, bot);
                return new BotDecision(PlayerAction.BET, bluffAmount, "Bluff bet");
            }
            return new BotDecision(PlayerAction.CHECK, 0, "Weak hand, check");
        }

        // Facing all-in with weak hand - rarely call as a bluff catch
        if (isFacingAllIn && strength > 0.30 && ThreadLocalRandom.current().nextDouble() < 0.08) {
            return new BotDecision(PlayerAction.CALL, 0, "Weak hand, bluff catch");
        }

        return new BotDecision(PlayerAction.FOLD, 0, "Weak hand, fold to bet");
    }

    

    
    public double calculateHandStrength(List<Card> hand, List<Card> communityCards, int numOpponents) {
        if (hand == null || hand.size() < 2)
            return 0;

        
        if (communityCards == null || communityCards.isEmpty()) {
            return calculatePreFlopStrength(hand);
        }

        
        int wins = 0;
        int ties = 0;
        int losses = 0;

        Set<Card> knownCards = new HashSet<>(hand);
        knownCards.addAll(communityCards);

        List<Card> remainingDeck = createRemainingDeck(knownCards);

        for (int i = 0; i < MONTE_CARLO_ITERATIONS; i++) {
            Collections.shuffle(remainingDeck);

            
            List<Card> fullBoard = new ArrayList<>(communityCards);
            int cardsNeeded = 5 - fullBoard.size();
            int deckIndex = 0;

            for (int j = 0; j < cardsNeeded && deckIndex < remainingDeck.size(); j++) {
                fullBoard.add(remainingDeck.get(deckIndex++));
            }

            
            List<Card> opponentHand = new ArrayList<>();
            if (deckIndex + 1 < remainingDeck.size()) {
                opponentHand.add(remainingDeck.get(deckIndex++));
                opponentHand.add(remainingDeck.get(deckIndex));
            }

            if (opponentHand.size() < 2)
                continue;

            
            HandRanking myRanking = handEvaluator.evaluate(hand, fullBoard);
            HandRanking oppRanking = handEvaluator.evaluate(opponentHand, fullBoard);

            int comparison = myRanking.compareTo(oppRanking);
            if (comparison > 0)
                wins++;
            else if (comparison < 0)
                losses++;
            else
                ties++;
        }

        
        double winRate = (double) wins / MONTE_CARLO_ITERATIONS;
        double tieRate = (double) ties / MONTE_CARLO_ITERATIONS;

        
        double adjustedWinRate = Math.pow(winRate + tieRate * 0.5, Math.max(1, numOpponents - 1));

        return Math.max(0, Math.min(1, adjustedWinRate));
    }

    
    double calculatePreFlopStrength(List<Card> hand) {
        Card c1 = hand.get(0);
        Card c2 = hand.get(1);

        int v1 = c1.getValue().ordinal();
        int v2 = c2.getValue().ordinal();
        boolean suited = c1.getSuit() == c2.getSuit();
        boolean pair = v1 == v2;

        
        if (pair) {
            if (v1 >= Value.TEN.ordinal())
                return 0.85 + (v1 - Value.TEN.ordinal()) * 0.03; 
            if (v1 >= Value.SEVEN.ordinal())
                return 0.65 + (v1 - Value.SEVEN.ordinal()) * 0.05; 
            return 0.50 + v1 * 0.02; 
        }

        
        int high = Math.max(v1, v2);
        int low = Math.min(v1, v2);
        double baseStrength = 0;

        
        if (high >= Value.JACK.ordinal() && low >= Value.TEN.ordinal()) {
            baseStrength = 0.70;
        } else if (high >= Value.ACE.ordinal() && low >= Value.TEN.ordinal()) {
            baseStrength = 0.65;
        } else if (high >= Value.KING.ordinal() && low >= Value.NINE.ordinal()) {
            baseStrength = 0.55;
        } else if (high >= Value.ACE.ordinal()) {
            baseStrength = 0.50 + low * 0.01;
        } else {
            baseStrength = 0.30 + (high + low) * 0.01;
        }

        
        if (suited)
            baseStrength += 0.05;

        
        int gap = high - low;
        if (gap == 1)
            baseStrength += 0.03;
        else if (gap == 2)
            baseStrength += 0.02;

        return Math.min(0.85, baseStrength);
    }

    private List<Card> createRemainingDeck(Set<Card> usedCards) {
        List<Card> deck = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Value value : Value.values()) {
                Card card = new Card(suit, value);
                if (!usedCards.contains(card)) {
                    deck.add(card);
                }
            }
        }
        return deck;
    }

    

    
    double calculatePotOdds(Game game, Player bot) {
        int pot = game.getCurrentPot();
        int toCall = game.getCurrentBet() - bot.getBetAmount();

        if (toCall <= 0)
            return 0;

        return (double) toCall / (pot + toCall);
    }

    private double getImpliedOddsBonus(Game game) {
        
        long activePlayers = game.getPlayers().stream()
                .filter(p -> !p.isFolded() && !p.isAllIn())
                .count();

        return activePlayers * 0.02;
    }

    
    int getPositionScore(Game game, Player bot) {
        
        int dealerPos = game.getDealerPosition();
        int botIndex = -1;
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .collect(Collectors.toList());

        for (int i = 0; i < activePlayers.size(); i++) {
            if (activePlayers.get(i).getId().equals(bot.getId())) {
                botIndex = i;
                break;
            }
        }

        if (botIndex < 0)
            return 0;

        int relativePosition = (botIndex - dealerPos + activePlayers.size()) % activePlayers.size();
        int totalPositions = activePlayers.size();

        if (relativePosition == 0)
            return 3; 
        if (relativePosition >= totalPositions - 2)
            return 2; 
        if (relativePosition >= totalPositions / 2)
            return 1; 
        return 0; 
    }

    

    
    int calculateRaiseAmount(Game game, Player bot, double strength) {
        BotPersonality personality = getBotPersonality(bot.getName());
        return calculateRaiseAmount(game, bot, strength, personality);
    }

    private int calculateRaiseAmount(Game game, Player bot, double strength, BotPersonality personality) {
        int pot = game.getCurrentPot();
        int currentBet = game.getCurrentBet();
        int minRaise = currentBet + (game.getMinRaiseAmount() > 0 ? game.getMinRaiseAmount() : game.getBigBlind());
        int maxRaise = bot.getChips() + bot.getBetAmount();

        
        double potMultiplier = 0.5 + (strength * 0.5) * personality.aggressionFactor;
        int targetRaise = (int) (pot * potMultiplier);

        
        targetRaise += ThreadLocalRandom.current().nextInt(Math.max(1, pot / 10)) - pot / 20;

        return Math.max(minRaise, Math.min(maxRaise, targetRaise));
    }


    int calculateBetAmount(Game game, Player bot, double strength) {
        BotPersonality personality = getBotPersonality(bot.getName());
        int bigBlind = game.getBigBlind();
        int betAmount = calculateBetAmount(game.getCurrentPot(), strength, personality, bigBlind);

        return Math.min(betAmount, bot.getChips());
    }

    private int calculateBetAmount(int pot, double strength, BotPersonality personality, int bigBlind) {
        double betFraction = (0.3 + strength * 0.4) * personality.aggressionFactor;
        int betAmount = (int) (pot * betFraction);


        betAmount += ThreadLocalRandom.current().nextInt(Math.max(1, pot / 10));


        // Ensure bet is at least the big blind
        int minBet = Math.max(bigBlind, 1);
        return Math.max(minBet, Math.min(betAmount, Math.max(pot, minBet)));
    }

    private int calculateBluffAmount(Game game, Player bot) {
        int pot = game.getCurrentPot();
        int bigBlind = game.getBigBlind();

        double bluffFraction = 0.5 + ThreadLocalRandom.current().nextDouble() * 0.25;
        int bluffAmount = (int) (pot * bluffFraction);

        // Ensure bluff is at least the big blind
        bluffAmount = Math.max(bluffAmount, bigBlind);

        return Math.min(bluffAmount, bot.getChips());
    }

    

    private boolean shouldBluff(Game game, Player bot, int position, BotPersonality personality) {
        
        if (bot.getChips() < game.getBigBlind() * 3)
            return false;

        
        double bluffChance = personality.bluffFrequency;
        bluffChance += position * 0.05;

        
        long activePlayers = game.getPlayers().stream()
                .filter(p -> !p.isFolded())
                .count();
        if (activePlayers <= 2)
            bluffChance += 0.1;

        
        if (game.getPhase() == GamePhase.RIVER)
            bluffChance += 0.05;

        return ThreadLocalRandom.current().nextDouble() < bluffChance;
    }

    

    private double adjustForPersonality(double strength, BotPersonality personality, GamePhase phase) {
        
        
        return strength * personality.handRangeMultiplier;
    }

    
    BotPersonality getBotPersonality(String botName) {
        
        int hash = Math.abs(botName.hashCode());
        int type = hash % 4;

        return switch (type) {
            case 0 -> BotPersonality.TIGHT_AGGRESSIVE;
            case 1 -> BotPersonality.LOOSE_AGGRESSIVE;
            case 2 -> BotPersonality.TIGHT_PASSIVE;
            default -> BotPersonality.LOOSE_PASSIVE;
        };
    }

    public enum BotPersonality {
        TIGHT_AGGRESSIVE(1.2, 0.08, 0.15, 1.3), 
        LOOSE_AGGRESSIVE(0.9, 0.05, 0.25, 1.4), 
        TIGHT_PASSIVE(1.3, 0.12, 0.05, 0.8), 
        LOOSE_PASSIVE(0.8, 0.03, 0.10, 0.9); 

        final double handRangeMultiplier;
        final double callThreshold;
        final double bluffFrequency;
        final double aggressionFactor;

        BotPersonality(double handRange, double callThreshold, double bluffFreq, double aggression) {
            this.handRangeMultiplier = handRange;
            this.callThreshold = callThreshold;
            this.bluffFrequency = bluffFreq;
            this.aggressionFactor = aggression;
        }
    }

    

    public void recordOpponentAction(UUID playerId, String action, int amount, int potSize) {
        OpponentModel model = opponentModels.computeIfAbsent(playerId, id -> new OpponentModel());
        model.recordAction(action, amount, potSize);
    }

    public void resetOpponentModels() {
        opponentModels.clear();
    }

    private static class OpponentModel {
        int totalActions = 0;
        int bets = 0;
        int raises = 0;
        int calls = 0;
        int folds = 0;

        void recordAction(String action, int amount, int potSize) {
            totalActions++;
            switch (action.toUpperCase()) {
                case "BET" -> bets++;
                case "RAISE" -> raises++;
                case "CALL" -> calls++;
                case "FOLD" -> folds++;
            }
        }

        double getAggressionFactor() {
            if (calls == 0)
                return (bets + raises) > 0 ? 10 : 1;
            return (double) (bets + raises) / calls;
        }

        double getFoldFrequency() {
            if (totalActions == 0)
                return 0.5;
            return (double) folds / totalActions;
        }
    }

    

    public record BotDecision(
            PlayerAction action,
            int amount,
            String reasoning) {
    }
}
