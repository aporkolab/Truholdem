# 📊 TruHoldem Hand Analysis System

This document covers the hand analysis features, including equity calculation, GTO recommendations, and hand history replay.

---

## Table of Contents
- [Overview](#overview)
- [Equity Calculator](#equity-calculator)
- [Hand Evaluator](#hand-evaluator)
- [GTO Recommendations](#gto-recommendations)
- [Hand History](#hand-history)
- [Hand Replay](#hand-replay)
- [Statistics Tracking](#statistics-tracking)
- [API Reference](#api-reference)
- [Frontend Components](#frontend-components)

---

## Overview

The TruHoldem analysis system provides:

- **Equity Calculator** — Real-time hand vs. hand equity calculations
- **Hand Evaluator** — Complete poker hand ranking logic
- **GTO Recommendations** — Basic game theory optimal suggestions
- **Hand History** — Full action-by-action game records
- **Statistics** — Player performance metrics (VPIP, PFR, etc.)

---

## Equity Calculator

### Monte Carlo Simulation

The equity calculator uses Monte Carlo simulation to estimate win probabilities:

```java
@Service
public class HandAnalysisService {
    
    private static final int EQUITY_ITERATIONS = 10000;
    
    public EquityResult calculateEquity(List<Card> hand1, List<Card> hand2, 
                                        List<Card> board) {
        int wins1 = 0, wins2 = 0, ties = 0;
        
        Set<Card> knownCards = new HashSet<>();
        knownCards.addAll(hand1);
        knownCards.addAll(hand2);
        knownCards.addAll(board);
        
        List<Card> deck = createRemainingDeck(knownCards);
        
        for (int i = 0; i < EQUITY_ITERATIONS; i++) {
            Collections.shuffle(deck);
            
            // Complete the board
            List<Card> fullBoard = new ArrayList<>(board);
            int cardsNeeded = 5 - fullBoard.size();
            for (int j = 0; j < cardsNeeded; j++) {
                fullBoard.add(deck.get(j));
            }
            
            // Evaluate hands
            HandRanking ranking1 = handEvaluator.evaluate(hand1, fullBoard);
            HandRanking ranking2 = handEvaluator.evaluate(hand2, fullBoard);
            
            int comparison = ranking1.compareTo(ranking2);
            if (comparison > 0) wins1++;
            else if (comparison < 0) wins2++;
            else ties++;
        }
        
        return new EquityResult(
            (double) wins1 / EQUITY_ITERATIONS * 100,
            (double) wins2 / EQUITY_ITERATIONS * 100,
            (double) ties / EQUITY_ITERATIONS * 100
        );
    }
}
```

### Equity Result

```java
public record EquityResult(
    double hand1Equity,  // Win percentage
    double hand2Equity,  // Win percentage
    double tiePercentage // Split pot percentage
) {
    public String hand1Display() {
        return String.format("%.1f%%", hand1Equity);
    }
}
```

### Example Calculations

| Hand 1 | Hand 2 | Board | Hand 1 Equity | Hand 2 Equity |
|--------|--------|-------|---------------|---------------|
| A♠A♣ | K♠K♣ | - | 82.4% | 17.6% |
| A♠K♠ | Q♠Q♣ | - | 43.0% | 57.0% |
| 7♠7♣ | A♠K♣ | - | 52.3% | 47.7% |
| A♠K♠ | 7♠2♣ | K♣7♦2♦ | 9.5% | 90.5% |

---

## Hand Evaluator

### Hand Rankings

```java
public enum HandType {
    HIGH_CARD(0),
    PAIR(1),
    TWO_PAIR(2),
    THREE_OF_A_KIND(3),
    STRAIGHT(4),
    FLUSH(5),
    FULL_HOUSE(6),
    FOUR_OF_A_KIND(7),
    STRAIGHT_FLUSH(8),
    ROYAL_FLUSH(9);
    
    private final int rank;
}
```

### Evaluation Algorithm

```java
@Service
public class HandEvaluator {
    
    public HandRanking evaluate(List<Card> holeCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(holeCards);
        allCards.addAll(communityCards);
        
        // Generate all 5-card combinations from 7 cards
        List<List<Card>> combinations = generateCombinations(allCards, 5);
        
        // Find the best hand
        HandRanking bestRanking = null;
        for (List<Card> combo : combinations) {
            HandRanking ranking = evaluateFiveCards(combo);
            if (bestRanking == null || ranking.compareTo(bestRanking) > 0) {
                bestRanking = ranking;
            }
        }
        
        return bestRanking;
    }
    
    private HandRanking evaluateFiveCards(List<Card> cards) {
        // Sort cards by value (descending)
        cards.sort((a, b) -> b.getValue().ordinal() - a.getValue().ordinal());
        
        boolean isFlush = checkFlush(cards);
        boolean isStraight = checkStraight(cards);
        Map<Value, Integer> valueCounts = countValues(cards);
        
        // Check hand types in order of strength
        if (isFlush && isStraight) {
            if (cards.get(0).getValue() == Value.ACE) {
                return new HandRanking(HandType.ROYAL_FLUSH, cards);
            }
            return new HandRanking(HandType.STRAIGHT_FLUSH, cards);
        }
        
        if (hasFourOfAKind(valueCounts)) {
            return new HandRanking(HandType.FOUR_OF_A_KIND, cards);
        }
        
        if (hasFullHouse(valueCounts)) {
            return new HandRanking(HandType.FULL_HOUSE, cards);
        }
        
        if (isFlush) {
            return new HandRanking(HandType.FLUSH, cards);
        }
        
        if (isStraight) {
            return new HandRanking(HandType.STRAIGHT, cards);
        }
        
        if (hasThreeOfAKind(valueCounts)) {
            return new HandRanking(HandType.THREE_OF_A_KIND, cards);
        }
        
        if (hasTwoPair(valueCounts)) {
            return new HandRanking(HandType.TWO_PAIR, cards);
        }
        
        if (hasPair(valueCounts)) {
            return new HandRanking(HandType.PAIR, cards);
        }
        
        return new HandRanking(HandType.HIGH_CARD, cards);
    }
}
```

### Hand Ranking Comparison

```java
public class HandRanking implements Comparable<HandRanking> {
    private final HandType type;
    private final List<Card> cards;
    private final List<Integer> kickers;
    
    @Override
    public int compareTo(HandRanking other) {
        // First compare hand type
        int typeCompare = this.type.compareTo(other.type);
        if (typeCompare != 0) return typeCompare;
        
        // Then compare kickers
        for (int i = 0; i < kickers.size(); i++) {
            int kickerCompare = this.kickers.get(i).compareTo(other.kickers.get(i));
            if (kickerCompare != 0) return kickerCompare;
        }
        
        return 0; // Tie
    }
}
```

---

## GTO Recommendations

### Basic GTO Framework

```java
public record GTORecommendation(
    String recommendedAction,
    double confidence,
    String reasoning,
    Map<String, Double> actionBreakdown
) {}

public GTORecommendation getRecommendation(HandAnalysis analysis) {
    double equity = analysis.equity();
    double potOdds = analysis.potOdds();
    int position = analysis.position();
    
    Map<String, Double> actions = new HashMap<>();
    String recommended;
    String reasoning;
    
    // Strong hands (70%+ equity)
    if (equity > 70) {
        actions.put("RAISE", 0.7);
        actions.put("CALL", 0.25);
        actions.put("FOLD", 0.05);
        recommended = "RAISE";
        reasoning = "Strong hand equity warrants aggressive play";
    }
    // Medium strength (40-70% equity)
    else if (equity > 40) {
        if (equity > potOdds) {
            actions.put("CALL", 0.6);
            actions.put("RAISE", 0.3);
            actions.put("FOLD", 0.1);
            recommended = "CALL";
            reasoning = "Positive expected value call";
        } else {
            actions.put("FOLD", 0.5);
            actions.put("CALL", 0.35);
            actions.put("RAISE", 0.15);
            recommended = position > 2 ? "CALL" : "FOLD";
            reasoning = "Marginal hand, position-dependent";
        }
    }
    // Weak hands (<40% equity)
    else {
        actions.put("FOLD", 0.7);
        actions.put("CALL", 0.15);
        actions.put("RAISE", 0.15); // Bluff
        recommended = "FOLD";
        reasoning = "Insufficient equity to continue";
    }
    
    return new GTORecommendation(recommended, actions.get(recommended), reasoning, actions);
}
```

### Action Breakdown Visualization

```
┌────────────────────────────────────────────────────────────────┐
│                    GTO Recommendation                           │
│                                                                 │
│  Your Hand: A♠K♠    Board: Q♠J♦5♣                              │
│  Equity: 42%        Pot Odds: 25%                              │
│                                                                 │
│  Recommended: CALL (60% frequency)                              │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ RAISE ████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 30%     │  │
│  │ CALL  ████████████████████████░░░░░░░░░░░░░░░░░░ 60%     │  │
│  │ FOLD  ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 10%     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Reasoning: Positive expected value with nut flush draw        │
└────────────────────────────────────────────────────────────────┘
```

---

## Hand History

### Hand History Entity

```java
@Entity
@Table(name = "hand_histories")
public class HandHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    private Game game;
    
    private int handNumber;
    private Instant startTime;
    private Instant endTime;
    
    @ElementCollection
    private List<String> communityCards;
    
    private int potSize;
    
    @ManyToOne
    private Player winner;
    
    @Column(columnDefinition = "jsonb")
    private String actionsJson;
    
    @Column(columnDefinition = "jsonb")
    private String playerCardsJson;
}
```

### Action Recording

```java
public record HandAction(
    int sequence,
    UUID playerId,
    String playerName,
    String action,        // FOLD, CHECK, CALL, BET, RAISE, ALL_IN
    int amount,
    int potAfter,
    String phase,         // PRE_FLOP, FLOP, TURN, RIVER
    Instant timestamp
) {}

// Recording actions during game
public void recordAction(Game game, Player player, PlayerAction action, int amount) {
    HandAction handAction = new HandAction(
        game.getCurrentHand().getActionCount(),
        player.getId(),
        player.getName(),
        action.name(),
        amount,
        game.getCurrentPot(),
        game.getPhase().name(),
        Instant.now()
    );
    
    game.getCurrentHand().addAction(handAction);
}
```

### Hand History Service

```java
@Service
public class HandHistoryService {
    
    public HandHistory saveHandHistory(Game game) {
        HandHistory history = new HandHistory();
        history.setGame(game);
        history.setHandNumber(game.getHandNumber());
        history.setCommunityCards(cardListToStrings(game.getCommunityCards()));
        history.setPotSize(game.getCurrentPot());
        history.setWinner(game.getLastWinner());
        history.setActionsJson(objectMapper.writeValueAsString(game.getActions()));
        history.setPlayerCardsJson(objectMapper.writeValueAsString(game.getPlayerCards()));
        
        return handHistoryRepository.save(history);
    }
    
    public List<HandHistory> getPlayerHistory(UUID playerId, int limit) {
        return handHistoryRepository.findByPlayerIdOrderByTimestampDesc(playerId, 
            PageRequest.of(0, limit));
    }
    
    public HandHistory getHandDetails(UUID handId) {
        return handHistoryRepository.findById(handId)
            .orElseThrow(() -> new ResourceNotFoundException("Hand not found"));
    }
}
```

---

## Hand Replay

### Replay Data Structure

```typescript
interface HandReplay {
  handId: string;
  gameId: string;
  handNumber: number;
  players: PlayerSnapshot[];
  actions: ReplayAction[];
  communityCards: {
    flop: Card[];
    turn: Card | null;
    river: Card | null;
  };
  winner: {
    playerId: string;
    playerName: string;
    handRanking: string;
    potWon: number;
  };
}

interface PlayerSnapshot {
  playerId: string;
  playerName: string;
  position: number;
  startingChips: number;
  holeCards: Card[];
}

interface ReplayAction {
  sequence: number;
  playerId: string;
  action: string;
  amount: number;
  potAfter: number;
  phase: string;
  timestamp: string;
}
```

### Angular Replay Component

```typescript
@Component({
  selector: 'app-hand-replay',
  template: `
    <div class="replay-container">
      <div class="table-view">
        <!-- Animated table with current state -->
        <app-replay-table 
          [players]="currentPlayers$ | async"
          [communityCards]="visibleCommunityCards$ | async"
          [pot]="currentPot$ | async"
          [activePlayer]="activePlayer$ | async">
        </app-replay-table>
      </div>
      
      <div class="controls">
        <button (click)="stepBack()" [disabled]="atStart">⏮</button>
        <button (click)="togglePlay()">{{ isPlaying ? '⏸' : '▶' }}</button>
        <button (click)="stepForward()" [disabled]="atEnd">⏭</button>
        <input type="range" [min]="0" [max]="totalSteps" 
               [(ngModel)]="currentStep" (change)="seekTo($event)">
      </div>
      
      <div class="action-log">
        <div *ngFor="let action of displayedActions$ | async" 
             [class.current]="action.sequence === currentStep">
          {{ action.playerName }} {{ action.action }} {{ action.amount | currency }}
        </div>
      </div>
      
      <div class="analysis-panel">
        <app-equity-display [hands]="visibleHands$ | async" 
                           [board]="visibleCommunityCards$ | async">
        </app-equity-display>
      </div>
    </div>
  `
})
export class HandReplayComponent {
  private store = inject(HandReplayStore);
  
  currentStep$ = this.store.currentStep$;
  currentPlayers$ = this.store.playersAtStep$;
  visibleCommunityCards$ = this.store.communityCardsAtStep$;
  currentPot$ = this.store.potAtStep$;
  
  stepForward() {
    this.store.nextStep();
  }
  
  stepBack() {
    this.store.previousStep();
  }
  
  togglePlay() {
    this.store.toggleAutoPlay();
  }
}
```

---

## Statistics Tracking

### Player Statistics Entity

```java
@Entity
@Table(name = "player_statistics")
public class PlayerStatistics {
    @Id
    private UUID playerId;
    
    // Lifetime stats
    private int totalHands;
    private int handsWon;
    private int totalProfit;
    
    // Preflop stats
    private int vpipHands;        // Voluntarily Put $ In Pot
    private int pfrHands;         // Pre-Flop Raise
    private int threeBetHands;    // 3-bet frequency
    
    // Postflop stats
    private int cBetHands;        // Continuation bet
    private int checkRaiseHands;  // Check-raise
    private int foldToCBetHands;  // Fold to c-bet
    
    // Aggression
    private int totalBets;
    private int totalRaises;
    private int totalCalls;
    
    // Showdown
    private int showdownsReached;
    private int showdownsWon;
    
    // Tournament stats
    private int tournamentsPlayed;
    private int tournamentsWon;
    private int inTheMoneyFinishes;
    private int totalPrizeWon;
    
    // Calculated metrics
    @Transient
    public double getVPIP() {
        return totalHands > 0 ? (double) vpipHands / totalHands * 100 : 0;
    }
    
    @Transient
    public double getPFR() {
        return totalHands > 0 ? (double) pfrHands / totalHands * 100 : 0;
    }
    
    @Transient
    public double getAggressionFactor() {
        return totalCalls > 0 ? (double) (totalBets + totalRaises) / totalCalls : 0;
    }
    
    @Transient
    public double getWTSD() { // Went To ShowDown
        return totalHands > 0 ? (double) showdownsReached / totalHands * 100 : 0;
    }
    
    @Transient
    public double getWSSD() { // Won at ShowDown
        return showdownsReached > 0 ? (double) showdownsWon / showdownsReached * 100 : 0;
    }
}
```

### Statistics Service

```java
@Service
public class PlayerStatisticsService {
    
    public void updateStatistics(Game game, HandHistory hand) {
        for (Player player : game.getPlayers()) {
            PlayerStatistics stats = getOrCreateStats(player.getId());
            
            stats.incrementTotalHands();
            
            // Check VPIP
            if (playerVoluntarilyPutMoneyIn(hand, player.getId())) {
                stats.incrementVpipHands();
            }
            
            // Check PFR
            if (playerRaisedPreflop(hand, player.getId())) {
                stats.incrementPfrHands();
            }
            
            // Update aggression stats
            updateAggressionStats(stats, hand, player.getId());
            
            // Check if won
            if (hand.getWinner().getId().equals(player.getId())) {
                stats.incrementHandsWon();
                stats.addProfit(hand.getPotSize());
            }
            
            statisticsRepository.save(stats);
        }
    }
    
    public PlayerStatistics getPlayerStats(UUID playerId) {
        return statisticsRepository.findById(playerId)
            .orElse(new PlayerStatistics(playerId));
    }
    
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        return statisticsRepository.findTopByProfit(PageRequest.of(0, limit));
    }
}
```

### Key Metrics Explained

| Metric | Formula | Good Range | Description |
|--------|---------|------------|-------------|
| **VPIP** | vpipHands / totalHands | 15-25% | How often player enters pots |
| **PFR** | pfrHands / totalHands | 12-20% | How often player raises pre-flop |
| **3-Bet** | 3betHands / opportunities | 5-10% | Re-raise frequency |
| **AF** | (bets + raises) / calls | 2-4 | Aggression Factor |
| **WTSD** | showdowns / hands | 25-35% | Went to Showdown |
| **W$SD** | showdownsWon / showdowns | 50%+ | Won money at Showdown |
| **C-Bet** | cBets / opportunities | 60-75% | Continuation bet frequency |

---

## API Reference

### Calculate Equity

```http
POST /api/v2/analysis/equity
Content-Type: application/json

{
  "hand1": ["As", "Kh"],
  "hand2": ["Qd", "Qc"],
  "board": ["Jh", "Tc", "5s"]
}

Response:
{
  "hand1Equity": 42.5,
  "hand2Equity": 57.5,
  "tiePercentage": 0.0,
  "sampleSize": 10000
}
```

### Get Hand Analysis

```http
POST /api/v2/analysis/hand
Content-Type: application/json

{
  "holeCards": ["As", "Ks"],
  "communityCards": ["Qs", "Jd", "5c"],
  "potSize": 500,
  "toCall": 150,
  "position": 2
}

Response:
{
  "handRanking": "HIGH_CARD",
  "equity": 42.5,
  "potOdds": 23.1,
  "expectedValue": 35.50,
  "recommendation": {
    "action": "CALL",
    "confidence": 0.65,
    "reasoning": "Positive EV with nut flush draw",
    "breakdown": {"FOLD": 0.1, "CALL": 0.65, "RAISE": 0.25}
  },
  "outs": {
    "flush": 9,
    "straight": 4,
    "total": 12
  }
}
```

### Get Hand History

```http
GET /api/v2/hands/{handId}

Response:
{
  "id": "uuid",
  "gameId": "uuid",
  "handNumber": 15,
  "timestamp": "2024-01-15T12:30:00Z",
  "communityCards": ["Qs", "Jd", "5c", "8h", "2s"],
  "potSize": 1250,
  "winner": {
    "playerId": "uuid",
    "playerName": "Alice",
    "handRanking": "FLUSH",
    "cards": ["As", "Ks"]
  },
  "actions": [
    {"sequence": 0, "player": "Bob", "action": "POST_SB", "amount": 25},
    {"sequence": 1, "player": "Alice", "action": "POST_BB", "amount": 50},
    {"sequence": 2, "player": "Charlie", "action": "RAISE", "amount": 150},
    // ...
  ]
}
```

### Get Player Statistics

```http
GET /api/v2/statistics/player/{playerId}

Response:
{
  "playerId": "uuid",
  "totalHands": 1523,
  "handsWon": 234,
  "totalProfit": 15250,
  "metrics": {
    "vpip": 22.5,
    "pfr": 18.2,
    "threeBet": 7.8,
    "aggressionFactor": 2.8,
    "wtsd": 28.5,
    "wssd": 52.3,
    "cBet": 68.0
  },
  "tournaments": {
    "played": 45,
    "won": 3,
    "itm": 12,
    "totalPrize": 8500
  }
}
```

---

## Frontend Components

### Analysis Module Structure

```
frontend/src/app/analysis/
├── analysis-page.component.ts
├── analysis.routes.ts
├── models/
│   └── analysis.models.ts
├── services/
│   └── analysis.service.ts
├── store/
│   └── analysis.store.ts
├── card-selector/
│   └── card-selector.component.ts
├── equity-calculator/
│   └── equity-calculator.component.ts
├── range-builder/
│   └── range-builder.component.ts
├── range-matrix/
│   └── range-matrix.component.ts
└── scenarios/
    └── scenarios.component.ts
```

### Equity Calculator Component

```typescript
@Component({
  selector: 'app-equity-calculator',
  template: `
    <div class="equity-calculator">
      <div class="hands-input">
        <div class="hand hand-1">
          <h3>Hand 1</h3>
          <app-card-selector 
            [selectedCards]="hand1()"
            (cardsChange)="setHand1($event)">
          </app-card-selector>
          <div class="equity" [class.winning]="equity1() > 50">
            {{ equity1() | number:'1.1-1' }}%
          </div>
        </div>
        
        <div class="vs">VS</div>
        
        <div class="hand hand-2">
          <h3>Hand 2</h3>
          <app-card-selector 
            [selectedCards]="hand2()"
            (cardsChange)="setHand2($event)">
          </app-card-selector>
          <div class="equity" [class.winning]="equity2() > 50">
            {{ equity2() | number:'1.1-1' }}%
          </div>
        </div>
      </div>
      
      <div class="board-input">
        <h3>Board</h3>
        <app-card-selector 
          [selectedCards]="board()"
          [maxCards]="5"
          (cardsChange)="setBoard($event)">
        </app-card-selector>
      </div>
      
      <button (click)="calculate()" [disabled]="!canCalculate()">
        Calculate Equity
      </button>
    </div>
  `
})
export class EquityCalculatorComponent {
  private store = inject(AnalysisStore);
  
  hand1 = this.store.hand1;
  hand2 = this.store.hand2;
  board = this.store.board;
  equity1 = this.store.hand1Equity;
  equity2 = this.store.hand2Equity;
  
  calculate() {
    this.store.calculateEquity();
  }
}
```

---

## Next Steps

- [ARCHITECTURE.md](ARCHITECTURE.md) — System architecture
- [BOT_AI.md](BOT_AI.md) — Bot AI documentation
- [DEPLOYMENT.md](DEPLOYMENT.md) — Production deployment
