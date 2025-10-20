# 🤖 TruHoldem Bot AI System

This document provides comprehensive documentation of the advanced Bot AI system, including decision-making algorithms, personality types, and strategic considerations.

---

## Table of Contents
- [Overview](#overview)
- [Decision Pipeline](#decision-pipeline)
- [Hand Strength Calculation](#hand-strength-calculation)
- [Position Analysis](#position-analysis)
- [Pot Odds & Equity](#pot-odds--equity)
- [Bot Personalities](#bot-personalities)
- [Pre-Flop Strategy](#pre-flop-strategy)
- [Post-Flop Strategy](#post-flop-strategy)
- [Bluffing Logic](#bluffing-logic)
- [Opponent Modeling](#opponent-modeling)
- [Configuration & Tuning](#configuration--tuning)

---

## Overview

The TruHoldem Bot AI (`AdvancedBotAIService`) implements a sophisticated poker decision engine that combines:

- **Monte Carlo simulation** for equity estimation
- **Position-based strategy adjustments**
- **Multiple personality archetypes** for varied gameplay
- **Opponent modeling** for adaptive strategy
- **Mathematically sound pot odds calculations**

### Design Goals

| Goal | Implementation |
|------|----------------|
| **Realistic Play** | Multiple personalities simulate human-like variety |
| **Competitive** | Math-based decisions with proper equity calculations |
| **Entertaining** | Bluffing and aggressive plays create exciting games |
| **Educational** | Decision logging helps users learn poker strategy |

---

## Decision Pipeline

```
┌────────────────────────────────────────────────────────────────────┐
│                        Bot Decision Flow                            │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Input: Game State (cards, pot, bets, players, position)           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Step 1: Calculate Hand Strength                             │   │
│  │  ├─ Pre-flop: Static hand ranking (AA > KK > QQ > ...)      │   │
│  │  └─ Post-flop: Monte Carlo simulation (500 iterations)       │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Step 2: Calculate Pot Odds                                  │   │
│  │  └─ pot_odds = to_call / (pot + to_call)                    │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Step 3: Evaluate Position                                   │   │
│  │  └─ Score: 0 (early) to 3 (button)                          │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Step 4: Apply Personality Modifiers                         │   │
│  │  └─ Adjust thresholds based on TAG/LAG/TP/LP                │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             ▼                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Step 5: Select Action                                       │   │
│  │  ├─ Pre-flop decision tree                                  │   │
│  │  └─ Post-flop decision tree                                 │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             ▼                                       │
│  Output: BotDecision { action, amount, reasoning }                 │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

## Hand Strength Calculation

### Pre-Flop Hand Ranking

The bot uses a static hand strength calculation for pre-flop decisions based on:

1. **Card ranks** (higher cards = stronger)
2. **Pair bonus** (pocket pairs get significant boost)
3. **Suited bonus** (+5% equity potential)
4. **Connectivity bonus** (connected cards for straight potential)

```java
// Pre-flop strength calculation (0.0 - 1.0)
double calculatePreFlopStrength(List<Card> hand) {
    int high = Math.max(c1.getValue().ordinal(), c2.getValue().ordinal());
    int low = Math.min(c1.getValue().ordinal(), c2.getValue().ordinal());
    boolean suited = c1.getSuit() == c2.getSuit();
    boolean paired = c1.getValue() == c2.getValue();

    double baseStrength;
    
    if (paired) {
        // Pairs: 22 = ~0.50, AA = ~0.85
        baseStrength = 0.50 + (high * 0.025);
    } else if (high >= 10) {
        // Broadway cards: AK, KQ, etc.
        baseStrength = 0.45 + (high + low) * 0.015;
    } else {
        // Other hands
        baseStrength = 0.30 + (high + low) * 0.01;
    }

    // Bonuses
    if (suited) baseStrength += 0.05;
    if (gap == 1) baseStrength += 0.03;  // Connected
    if (gap == 2) baseStrength += 0.02;  // One-gapper

    return Math.min(0.85, baseStrength);
}
```

### Pre-Flop Hand Tiers

| Tier | Example Hands | Strength Range | Strategy |
|------|---------------|----------------|----------|
| **Premium** | AA, KK, QQ, AKs | 0.85+ | Raise/Re-raise |
| **Strong** | JJ, TT, AQs, AKo | 0.70-0.85 | Raise from any position |
| **Medium** | 99-77, AJs, KQs | 0.50-0.70 | Call or raise in position |
| **Speculative** | 66-22, Suited connectors | 0.35-0.50 | Call in position cheaply |
| **Weak** | Unsuited low cards | <0.35 | Fold (except free plays) |

### Post-Flop: Monte Carlo Simulation

For post-flop decisions, the bot runs **500 Monte Carlo iterations** to estimate equity:

```java
public double calculateHandStrength(List<Card> hand, List<Card> community, int numOpponents) {
    int wins = 0, ties = 0, losses = 0;
    
    Set<Card> knownCards = new HashSet<>(hand);
    knownCards.addAll(community);
    List<Card> remainingDeck = createRemainingDeck(knownCards);

    for (int i = 0; i < 500; i++) {
        Collections.shuffle(remainingDeck);
        
        // Complete the board
        List<Card> fullBoard = new ArrayList<>(community);
        int cardsNeeded = 5 - fullBoard.size();
        for (int j = 0; j < cardsNeeded; j++) {
            fullBoard.add(remainingDeck.get(j));
        }
        
        // Deal opponent hand
        List<Card> opponentHand = Arrays.asList(
            remainingDeck.get(cardsNeeded),
            remainingDeck.get(cardsNeeded + 1)
        );
        
        // Compare hands
        HandRanking myRanking = evaluate(hand, fullBoard);
        HandRanking oppRanking = evaluate(opponentHand, fullBoard);
        
        int comparison = myRanking.compareTo(oppRanking);
        if (comparison > 0) wins++;
        else if (comparison < 0) losses++;
        else ties++;
    }
    
    // Adjust for multiple opponents
    double winRate = (double) wins / 500;
    double tieRate = (double) ties / 500;
    return Math.pow(winRate + tieRate * 0.5, Math.max(1, numOpponents - 1));
}
```

---

## Position Analysis

Position is crucial in poker strategy. The bot assigns a position score:

| Position | Score | Description |
|----------|-------|-------------|
| **Early** | 0 | First to act (UTG, UTG+1) |
| **Middle** | 1 | Middle positions |
| **Late** | 2 | Cutoff, Hijack |
| **Button** | 3 | Dealer position (best) |

### Position Adjustments

```java
int getPositionScore(Game game, Player bot) {
    int dealerPos = game.getDealerPosition();
    int botIndex = findPlayerIndex(game, bot);
    int relativePosition = (botIndex - dealerPos + totalPlayers) % totalPlayers;
    
    if (relativePosition == 0) return 3;           // Button
    if (relativePosition >= totalPlayers - 2) return 2;  // Late
    if (relativePosition >= totalPlayers / 2) return 1;  // Middle
    return 0;                                      // Early
}
```

### Position-Based Strategy

| Position | Opening Range | Bluff Frequency | Raise Sizing |
|----------|---------------|-----------------|--------------|
| Early | 10% (premium only) | Low | Standard |
| Middle | 15-20% | Moderate | Standard |
| Late | 25-30% | Higher | Variable |
| Button | 35-40% | Highest | Aggressive |

---

## Pot Odds & Equity

### Pot Odds Calculation

```java
double calculatePotOdds(Game game, Player bot) {
    int pot = game.getCurrentPot();
    int toCall = game.getCurrentBet() - bot.getBetAmount();
    
    if (toCall <= 0) return 0;
    
    // Required equity to call profitably
    return (double) toCall / (pot + toCall);
}
```

### Implied Odds Bonus

The bot considers implied odds (potential future winnings):

```java
private double getImpliedOddsBonus(Game game) {
    // More active players = higher implied odds potential
    long activePlayers = game.getPlayers().stream()
        .filter(p -> !p.isFolded() && !p.isAllIn())
        .count();
    
    return activePlayers * 0.02;  // 2% per remaining player
}
```

### Decision Matrix

| Hand Equity | Pot Odds | Decision |
|-------------|----------|----------|
| 60%+ | Any | Value bet/raise |
| 40-60% | < equity | Call |
| 40-60% | > equity | Consider bluff or fold |
| < 40% | Any | Fold or bluff |

---

## Bot Personalities

Each bot is assigned a personality based on their name hash:

```java
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
```

### Personality Parameters

```java
public enum BotPersonality {
    TIGHT_AGGRESSIVE(1.2, 0.08, 0.15, 1.3),   // TAG
    LOOSE_AGGRESSIVE(0.9, 0.05, 0.25, 1.4),   // LAG
    TIGHT_PASSIVE(1.3, 0.12, 0.05, 0.8),      // TP
    LOOSE_PASSIVE(0.8, 0.03, 0.10, 0.9);      // LP

    final double handRangeMultiplier;  // Adjusts hand requirements
    final double callThreshold;         // Max % of stack to call
    final double bluffFrequency;        // Base bluffing rate
    final double aggressionFactor;      // Bet/raise sizing multiplier
}
```

### Personality Profiles

#### Tight-Aggressive (TAG)
- **Style**: Plays few hands, bets big with strong holdings
- **Hand Range**: Premium hands only (top 10-15%)
- **Characteristics**:
  - High fold rate pre-flop
  - Large bets when entering pots
  - Aggressive on all streets with value hands
  - Occasional well-timed bluffs

#### Loose-Aggressive (LAG)
- **Style**: Plays many hands with pressure
- **Hand Range**: Wide (top 30-40%)
- **Characteristics**:
  - Frequent pre-flop raises
  - Lots of continuation bets
  - High bluff frequency
  - Difficult to put on a hand

#### Tight-Passive (TP)
- **Style**: Conservative, waiting for monsters
- **Hand Range**: Very narrow (top 8-10%)
- **Characteristics**:
  - Rarely raises
  - Calls with strong hands
  - Almost never bluffs
  - Easy to read when betting

#### Loose-Passive (LP)
- **Style**: Calling station
- **Hand Range**: Very wide (top 45%+)
- **Characteristics**:
  - Calls with many hands
  - Rarely raises
  - Low aggression
  - Often gets "trapped"

---

## Pre-Flop Strategy

```java
private BotDecision preFlopDecision(Game game, Player bot, double strength, 
                                    int position, BotPersonality personality) {
    int toCall = game.getCurrentBet() - bot.getBetAmount();
    int chips = bot.getChips();

    // Premium hands (AA, KK, QQ, AKs)
    if (strength > 0.85) {
        int raiseAmount = calculateRaiseAmount(game, bot, strength, personality);
        return new BotDecision(RAISE, raiseAmount, "Premium hand");
    }

    // Strong hands (JJ, TT, AQ, etc.)
    if (strength > 0.70) {
        if (position >= 2) {
            // Raise in late position
            return new BotDecision(RAISE, calculateRaiseAmount(...), 
                                   "Strong hand, late position");
        } else {
            // Call in early position
            return toCall > 0 
                ? new BotDecision(CALL, 0, "Strong hand, early position")
                : new BotDecision(CHECK, 0, "Strong hand, check");
        }
    }

    // Medium hands (77-99, suited connectors)
    if (strength > 0.50) {
        double callThreshold = personality.callThreshold * chips;
        if (toCall <= callThreshold) {
            return new BotDecision(CALL, 0, "Medium hand, acceptable price");
        } else {
            return new BotDecision(FOLD, 0, "Medium hand, too expensive");
        }
    }

    // Speculative hands (small pairs, suited connectors)
    if (strength > 0.35 && position >= 2 && toCall <= chips * 0.05) {
        return new BotDecision(CALL, 0, "Speculative hand, implied odds");
    }

    // Weak hands
    if (toCall == 0) {
        return new BotDecision(CHECK, 0, "Weak hand, free play");
    }

    return new BotDecision(FOLD, 0, "Weak hand");
}
```

---

## Post-Flop Strategy

```java
private BotDecision postFlopDecision(Game game, Player bot, double strength,
                                     double potOdds, int position, 
                                     BotPersonality personality) {
    int currentBet = game.getCurrentBet();
    int toCall = currentBet - bot.getBetAmount();
    int pot = game.getCurrentPot();

    // Strong made hands (two pair+, top pair good kicker)
    if (strength > 0.80) {
        if (currentBet == 0) {
            int betAmount = calculateBetAmount(pot, strength, personality);
            return new BotDecision(BET, betAmount, "Strong hand, value bet");
        } else {
            return new BotDecision(RAISE, calculateRaiseAmount(...), 
                                   "Strong hand, raise for value");
        }
    }

    // Good hands (middle pair, strong draws)
    if (strength > 0.60) {
        if (currentBet == 0) {
            return new BotDecision(BET, calculateBetAmount(...), "Good hand, bet");
        } else if (strength > potOdds) {
            return new BotDecision(CALL, 0, "Good hand, +EV call");
        } else {
            return shouldBluff(...) 
                ? new BotDecision(RAISE, calculateBluffAmount(...), "Bluff raise")
                : new BotDecision(FOLD, 0, "Bad pot odds");
        }
    }

    // Drawing hands (flush/straight draws)
    if (strength > 0.40) {
        double requiredEquity = potOdds;
        if (strength + getImpliedOddsBonus(game) > requiredEquity) {
            return toCall > 0
                ? new BotDecision(CALL, 0, "Drawing hand, implied odds")
                : new BotDecision(CHECK, 0, "Drawing hand, free card");
        }
    }

    // Weak hands
    if (toCall == 0) {
        if (shouldBluff(game, bot, position, personality)) {
            return new BotDecision(BET, calculateBluffAmount(...), "Bluff bet");
        }
        return new BotDecision(CHECK, 0, "Weak hand, check");
    }

    return new BotDecision(FOLD, 0, "Weak hand, fold to bet");
}
```

---

## Bluffing Logic

### Bluff Decision Factors

```java
private boolean shouldBluff(Game game, Player bot, int position, 
                           BotPersonality personality) {
    // Don't bluff with short stack
    if (bot.getChips() < game.getBigBlind() * 3) return false;

    double bluffChance = personality.bluffFrequency;
    
    // Position bonus (more bluffs in late position)
    bluffChance += position * 0.05;

    // Fewer opponents = more bluffing opportunity
    long activePlayers = countActivePlayers(game);
    if (activePlayers <= 2) bluffChance += 0.1;

    // River bluffs are more credible
    if (game.getPhase() == GamePhase.RIVER) bluffChance += 0.05;

    return random.nextDouble() < bluffChance;
}
```

### Bluff Sizing

```java
private int calculateBluffAmount(Game game, Player bot) {
    int pot = game.getCurrentPot();
    
    // Bluff between 50-75% of pot
    double bluffFraction = 0.5 + random.nextDouble() * 0.25;
    int bluffAmount = (int) (pot * bluffFraction);
    
    return Math.min(bluffAmount, bot.getChips());
}
```

---

## Opponent Modeling

The bot tracks opponent actions to adapt its strategy:

```java
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

    // Aggression Factor = (bets + raises) / calls
    double getAggressionFactor() {
        if (calls == 0) return (bets + raises) > 0 ? 10 : 1;
        return (double) (bets + raises) / calls;
    }

    // How often opponent folds
    double getFoldFrequency() {
        if (totalActions == 0) return 0.5;
        return (double) folds / totalActions;
    }
}
```

### Using Opponent Data

| Opponent Profile | Bot Adjustment |
|-----------------|----------------|
| High AF (aggressive) | Tighten range, value-bet more |
| Low AF (passive) | Bluff more, value-bet thin |
| High fold frequency | Increase bluff frequency |
| Low fold frequency | Value-bet relentlessly |

---

## Configuration & Tuning

### Monte Carlo Iterations

```java
private static final int MONTE_CARLO_ITERATIONS = 500;
```

**Trade-offs**:
- Higher = more accurate but slower
- 500 iterations provides ~1-2% equity accuracy
- Increase to 1000+ for tournament final tables

### Randomization

All decisions include small random variations to prevent predictability:

```java
// Add 5-10% randomness to bet sizing
targetRaise += random.nextInt(Math.max(1, pot / 10)) - pot / 20;
```

### Adjustable Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `MONTE_CARLO_ITERATIONS` | 500 | Simulation count |
| `handRangeMultiplier` | 0.8-1.3 | Per personality |
| `callThreshold` | 0.03-0.12 | Max call as % of stack |
| `bluffFrequency` | 0.05-0.25 | Base bluff rate |
| `aggressionFactor` | 0.8-1.4 | Bet sizing multiplier |

---

## Testing the Bot AI

### Unit Tests

```java
@Test
void shouldCalculatePreFlopStrength_PocketAces() {
    List<Card> hand = List.of(
        new Card(Suit.HEARTS, Value.ACE),
        new Card(Suit.SPADES, Value.ACE)
    );
    
    double strength = botAIService.calculatePreFlopStrength(hand);
    
    assertThat(strength).isGreaterThan(0.80);
}

@Test
void shouldRaiseWithPremiumHand() {
    Game game = createGameWithBotHolding(List.of(ACE_HEARTS, KING_HEARTS));
    
    BotDecision decision = botAIService.decide(game, botPlayer);
    
    assertThat(decision.action()).isEqualTo(PlayerAction.RAISE);
}
```

### Integration Tests

The bot AI is tested in full game scenarios to verify:
- Correct action selection across 1000+ hands
- Profitable play against random opponents
- Personality consistency

---

## Future Improvements

- [ ] GTO (Game Theory Optimal) baseline solver
- [ ] Neural network-based hand reading
- [ ] Real-time opponent modeling across sessions
- [ ] ICM (Independent Chip Model) for tournament decisions
- [ ] Hand range visualization for debugging

---

## References

- [The Mathematics of Poker](https://www.amazon.com/Mathematics-Poker-Bill-Chen/dp/1886070253) - Bill Chen
- [Theory of Poker](https://www.amazon.com/Theory-Poker-David-Sklansky/dp/1880685000) - David Sklansky
- Monte Carlo Methods in Poker Research
