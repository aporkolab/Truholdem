# 🏆 TruHoldem Tournament System

This document describes the multi-table tournament (MTT) system architecture, including tournament types, blind structures, prize distribution, and table management.

---

## Table of Contents
- [Overview](#overview)
- [Tournament Types](#tournament-types)
- [Tournament Lifecycle](#tournament-lifecycle)
- [Blind Structures](#blind-structures)
- [Table Management](#table-management)
- [Prize Distribution](#prize-distribution)
- [Rebuy & Add-on](#rebuy--add-on)
- [Domain Events](#domain-events)
- [API Reference](#api-reference)
- [WebSocket Events](#websocket-events)

---

## Overview

The TruHoldem tournament system supports:

- **Sit & Go** — Starts when full (typically 6-9 players)
- **Scheduled** — Starts at a specific time
- **Multi-table** — Dynamic table balancing as players eliminate
- **Rebuy/Add-on** — Optional rebuy periods with configurable limits
- **Bounty** — Optional knockout bounty rewards

### Tournament Flow

```
┌──────────────┐      ┌───────────┐      ┌─────────┐      ┌──────────┐
│ REGISTERING  │─────▶│ STARTING  │─────▶│ RUNNING │─────▶│ COMPLETED│
│              │      │           │      │         │      │          │
│ - Accept     │      │ - Create  │      │ - Play  │      │ - Award  │
│   players    │      │   tables  │      │ - Level │      │   prizes │
│ - Validate   │      │ - Seat    │      │   ups   │      │ - Record │
│   buy-ins    │      │   players │      │ - Elim  │      │   results│
└──────────────┘      └───────────┘      │ - Rebal │      └──────────┘
       │                                 └────┬────┘
       │                                      │
       │ Full (Sit&Go)                        │ One player left
       └──────────────────────────────────────┘
```

---

## Tournament Types

### Sit & Go (SNG)

```java
// Auto-starts when maxPlayers is reached
CreateTournamentRequest request = new CreateTournamentRequest(
    "Quick 6-Max",
    TournamentType.SIT_AND_GO,
    100,        // buyIn
    1500,       // startingChips
    6,          // minPlayers
    6,          // maxPlayers
    "TURBO",    // blindStructureType
    null, null, null, null, null, null
);
```

| Feature | Description |
|---------|-------------|
| **Start Trigger** | Automatically when full |
| **Player Count** | Typically 6-9 players |
| **Duration** | 15-45 minutes |
| **Best For** | Quick games, practice |

### Scheduled Tournament

```java
CreateTournamentRequest request = new CreateTournamentRequest(
    "Sunday Major",
    TournamentType.SCHEDULED,
    500,        // buyIn
    10000,      // startingChips
    20,         // minPlayers
    200,        // maxPlayers
    "STANDARD", // blindStructureType
    500,        // rebuyAmount
    6,          // rebuyDeadlineLevel
    3,          // maxRebuys
    5000,       // addOnAmount
    25,         // bountyAmount
    null        // payoutStructure (use default)
);
```

| Feature | Description |
|---------|-------------|
| **Start Trigger** | Manual or scheduled time |
| **Player Count** | 20-10,000+ |
| **Duration** | 2-8+ hours |
| **Best For** | Competitive play, large fields |

---

## Tournament Lifecycle

### States

```java
public enum TournamentStatus {
    REGISTERING,    // Accepting player registrations
    STARTING,       // Creating tables, seating players
    RUNNING,        // Active gameplay
    PAUSED,         // Temporarily halted
    ON_BREAK,       // Scheduled break
    FINAL_TABLE,    // Down to one table
    COMPLETED,      // Winner determined
    CANCELLED       // Not enough players or admin cancelled
}
```

### State Transitions

```
REGISTERING ──▶ STARTING ──▶ RUNNING ──▶ FINAL_TABLE ──▶ COMPLETED
     │              │           │              │
     │              │           ▼              │
     │              │       PAUSED ◀───────────┘
     │              │           │
     │              │       ON_BREAK
     │              │
     ▼              │
CANCELLED ◀────────┘
```

### Registration Validation

```java
private void validateCanRegister(Tournament tournament, UUID playerId) {
    // Check tournament is accepting registrations
    if (!tournament.canRegister()) {
        throw new IllegalStateException("Tournament is not accepting registrations");
    }
    
    // Prevent duplicate registration
    if (tournament.isPlayerRegistered(playerId)) {
        throw new IllegalStateException("Player already registered");
    }
    
    // Check max players
    if (tournament.getRegistrations().size() >= tournament.getMaxPlayers()) {
        throw new IllegalStateException("Tournament is full");
    }
}
```

---

## Blind Structures

### Pre-defined Structures

```java
public static BlindStructure standard() {
    return new BlindStructure(List.of(
        new BlindLevel(1, 25, 50, 0, Duration.ofMinutes(15)),
        new BlindLevel(2, 50, 100, 0, Duration.ofMinutes(15)),
        new BlindLevel(3, 75, 150, 0, Duration.ofMinutes(15)),
        new BlindLevel(4, 100, 200, 25, Duration.ofMinutes(15)),
        new BlindLevel(5, 150, 300, 25, Duration.ofMinutes(15)),
        new BlindLevel(6, 200, 400, 50, Duration.ofMinutes(15)),
        new BlindLevel(7, 300, 600, 75, Duration.ofMinutes(15)),
        new BlindLevel(8, 400, 800, 100, Duration.ofMinutes(15)),
        new BlindLevel(9, 500, 1000, 100, Duration.ofMinutes(15)),
        new BlindLevel(10, 600, 1200, 150, Duration.ofMinutes(15)),
        // ... continues
    ));
}

public static BlindStructure turbo() {
    // 8-minute levels, faster escalation
    return new BlindStructure(/* turbo levels */);
}

public static BlindStructure deep() {
    // 20-minute levels, slower escalation
    return new BlindStructure(/* deep levels */);
}
```

### Blind Level Structure

```java
public record BlindLevel(
    int level,
    int smallBlind,
    int bigBlind,
    int ante,
    Duration duration
) {}
```

### Level Progression

```
┌────────────────────────────────────────────────────────────────┐
│                    Blind Level Timeline                         │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Level 1     Level 2     Level 3     Level 4     Level 5       │
│  25/50       50/100      75/150      100/200     150/300       │
│  ├────────────┼────────────┼────────────┼────────────┤         │
│  0 min       15 min      30 min      45 min      60 min        │
│                                                                 │
│  Standard: 15 min levels                                        │
│  Turbo: 8 min levels                                           │
│  Deep: 20 min levels                                           │
└────────────────────────────────────────────────────────────────┘
```

### Scheduled Level Increases

```java
private void scheduleLevelIncrease(Tournament tournament) {
    BlindLevel currentLevel = tournament.getCurrentBlindLevel();
    
    ScheduledFuture<?> future = taskScheduler.schedule(
        () -> advanceBlindLevel(tournament.getId()),
        Instant.now().plus(currentLevel.duration())
    );
    
    scheduledLevelIncreases.put(tournament.getId(), future);
}

private void advanceBlindLevel(UUID tournamentId) {
    Tournament tournament = findTournamentOrThrow(tournamentId);
    
    int newLevel = tournament.getCurrentLevel() + 1;
    tournament.setCurrentLevel(newLevel);
    
    BlindLevel newBlindLevel = tournament.getBlindStructure().getLevel(newLevel);
    
    // Update all active tables
    for (TournamentTable table : tournament.getActiveTables()) {
        table.updateBlinds(newBlindLevel);
    }
    
    publishEvent(new TournamentLevelAdvanced(
        tournamentId, 
        newLevel, 
        newBlindLevel.smallBlind(), 
        newBlindLevel.bigBlind(),
        newBlindLevel.ante()
    ));
    
    // Schedule next level
    scheduleLevelIncrease(tournament);
}
```

---

## Table Management

### Constants

```java
private static final int MAX_PLAYERS_PER_TABLE = 9;
private static final int IDEAL_PLAYERS_PER_TABLE = 8;
private static final int MIN_PLAYERS_TO_PLAY = 2;
```

### Initial Table Creation

```java
List<TournamentTable> createInitialTables(Tournament tournament) {
    int playerCount = tournament.getRegistrations().size();
    int tableCount = calculateTableCount(playerCount);
    
    List<TournamentTable> tables = new ArrayList<>();
    for (int i = 1; i <= tableCount; i++) {
        TournamentTable table = new TournamentTable(tournament, i);
        tables.add(table);
    }
    
    return tableRepository.saveAll(tables);
}

private int calculateTableCount(int playerCount) {
    if (playerCount <= MAX_PLAYERS_PER_TABLE) {
        return 1;  // Single table
    }
    return (int) Math.ceil((double) playerCount / IDEAL_PLAYERS_PER_TABLE);
}
```

### Random Seating

```java
void seatPlayersRandomly(Tournament tournament) {
    List<TournamentRegistration> registrations = 
        new ArrayList<>(tournament.getActiveRegistrations());
    Collections.shuffle(registrations);
    
    List<TournamentTable> tables = tournament.getActiveTables();
    
    int tableIndex = 0;
    for (TournamentRegistration reg : registrations) {
        TournamentTable table = tables.get(tableIndex);
        
        // Find next table with space
        while (table.isFull() && tableIndex < tables.size() - 1) {
            tableIndex++;
            table = tables.get(tableIndex);
        }
        
        table.seatPlayer(reg.getPlayerId());
        tableIndex = (tableIndex + 1) % tables.size();  // Round-robin
    }
}
```

### Table Balancing

When a player is eliminated, tables may need rebalancing:

```java
public void rebalanceTables(UUID tournamentId) {
    Tournament tournament = findTournamentOrThrow(tournamentId);
    List<TournamentTable> tables = tournament.getActiveTables();
    
    // Check if we need to close a table
    int totalPlayers = tables.stream().mapToInt(TournamentTable::getPlayerCount).sum();
    int optimalTableCount = calculateTableCount(totalPlayers);
    
    if (tables.size() > optimalTableCount) {
        // Close smallest table and redistribute
        TournamentTable tableToClose = findSmallestTable(tables);
        redistributePlayers(tableToClose, tables);
        tableToClose.close();
    } else {
        // Balance existing tables
        balanceExistingTables(tables);
    }
    
    publishEvent(new TournamentTablesRebalanced(
        tournamentId,
        tournament.getActiveTables().size(),
        calculatePlayerDistribution(tournament.getActiveTables())
    ));
}

private void balanceExistingTables(List<TournamentTable> tables) {
    int totalPlayers = tables.stream().mapToInt(TournamentTable::getPlayerCount).sum();
    int targetPerTable = totalPlayers / tables.size();
    int remainder = totalPlayers % tables.size();
    
    // Move players from overfull tables to underfull tables
    List<UUID> playersToMove = new ArrayList<>();
    
    for (TournamentTable table : tables) {
        int target = targetPerTable + (remainder-- > 0 ? 1 : 0);
        while (table.getPlayerCount() > target) {
            playersToMove.add(table.removeRandomPlayer());
        }
    }
    
    // Seat moved players at tables with space
    for (UUID playerId : playersToMove) {
        TournamentTable targetTable = findTableWithSpace(tables, targetPerTable);
        targetTable.seatPlayer(playerId);
    }
}
```

### Final Table

```java
private void checkForFinalTable(Tournament tournament) {
    List<TournamentTable> activeTables = tournament.getActiveTables();
    int totalPlayers = activeTables.stream()
        .mapToInt(TournamentTable::getPlayerCount)
        .sum();
    
    // Transition to final table when one table can hold everyone
    if (activeTables.size() > 1 && totalPlayers <= MAX_PLAYERS_PER_TABLE) {
        consolidateToFinalTable(tournament);
    }
}

private void consolidateToFinalTable(Tournament tournament) {
    TournamentTable finalTable = tournament.getActiveTables().get(0);
    finalTable.setFinalTable(true);
    
    // Move all players to final table
    for (TournamentTable table : tournament.getActiveTables()) {
        if (!table.equals(finalTable)) {
            for (UUID playerId : table.getPlayerIds()) {
                finalTable.seatPlayer(playerId);
            }
            table.close();
        }
    }
    
    tournament.setStatus(TournamentStatus.FINAL_TABLE);
    
    publishEvent(new TournamentFinalTableReached(
        tournament.getId(),
        finalTable.getId(),
        finalTable.getPlayerIds()
    ));
}
```

---

## Prize Distribution

### Default Payout Structure

```java
private Map<Integer, Double> getDefaultPayoutStructure(int playerCount) {
    if (playerCount <= 6) {
        return Map.of(1, 0.65, 2, 0.35);  // 2 paid
    } else if (playerCount <= 10) {
        return Map.of(1, 0.50, 2, 0.30, 3, 0.20);  // 3 paid
    } else if (playerCount <= 18) {
        return Map.of(1, 0.40, 2, 0.25, 3, 0.18, 4, 0.10, 5, 0.07);
    } else if (playerCount <= 27) {
        return Map.of(
            1, 0.35, 2, 0.22, 3, 0.15, 4, 0.10, 
            5, 0.07, 6, 0.06, 7, 0.05
        );
    } else {
        // Larger tournaments pay ~10% of field
        return calculateLargeTournamentPayout(playerCount);
    }
}
```

### Prize Pool Calculation

```java
public int calculatePrizePool(Tournament tournament) {
    int basePrize = tournament.getBuyIn() * tournament.getRegistrations().size();
    int rebuyPrize = tournament.getTotalRebuys() * tournament.getRebuyAmount();
    int addOnPrize = tournament.getTotalAddOns() * tournament.getAddOnAmount();
    
    return basePrize + rebuyPrize + addOnPrize;
}

public int calculatePrizeForPosition(Tournament tournament, int position) {
    int prizePool = calculatePrizePool(tournament);
    Map<Integer, Double> payouts = tournament.getPayoutStructure();
    
    Double percentage = payouts.get(position);
    if (percentage == null) return 0;
    
    return (int) (prizePool * percentage);
}
```

### Example Payout

For a 100-player, $100 buy-in tournament:

| Position | Percentage | Payout |
|----------|------------|--------|
| 1st | 20% | $2,000 |
| 2nd | 13% | $1,300 |
| 3rd | 9% | $900 |
| 4th | 7% | $700 |
| 5th | 5.5% | $550 |
| 6th | 4.5% | $450 |
| 7th | 3.5% | $350 |
| 8th | 3% | $300 |
| 9th | 2.5% | $250 |
| 10th | 2% | $200 |
| 11-15th | 6% (split) | $120 each |

---

## Rebuy & Add-on

### Rebuy Rules

```java
public TournamentRegistration processRebuy(UUID tournamentId, UUID playerId) {
    Tournament tournament = findTournamentOrThrow(tournamentId);
    
    TournamentRegistration registration = tournament.findRegistration(playerId)
        .orElseThrow(() -> new ResourceNotFoundException("Player not found"));
    
    // Validate rebuy conditions
    if (!registration.canRebuy()) {
        throw new IllegalStateException(
            "Cannot rebuy: either past deadline level or max rebuys reached"
        );
    }
    
    registration.rebuy(tournament.getRebuyAmount());
    tournamentRepository.save(tournament);
    
    return registration;
}

// In TournamentRegistration
public boolean canRebuy() {
    // Can only rebuy during rebuy period (e.g., first 6 levels)
    if (tournament.getCurrentLevel() > tournament.getRebuyDeadlineLevel()) {
        return false;
    }
    
    // Check max rebuys
    if (rebuysUsed >= tournament.getMaxRebuys()) {
        return false;
    }
    
    // Can only rebuy when at or below starting stack
    return chips <= tournament.getStartingChips();
}
```

### Add-on

```java
public TournamentRegistration processAddOn(UUID tournamentId, UUID playerId) {
    Tournament tournament = findTournamentOrThrow(tournamentId);
    
    // Add-ons are typically only available at the break after rebuy period
    if (tournament.getStatus() != TournamentStatus.ON_BREAK) {
        throw new IllegalStateException("Add-ons only available during break");
    }
    
    TournamentRegistration registration = tournament.findRegistration(playerId)
        .orElseThrow();
    
    if (registration.hasUsedAddOn()) {
        throw new IllegalStateException("Add-on already used");
    }
    
    registration.addOn(tournament.getAddOnAmount());
    return registration;
}
```

---

## Domain Events

### Tournament Events

```java
// When tournament is created
public record TournamentCreated(
    UUID tournamentId,
    String name,
    TournamentType type,
    int buyIn,
    int startingChips,
    int maxPlayers
) implements TournamentEvent {}

// When player registers
public record TournamentPlayerRegistered(
    UUID tournamentId,
    UUID playerId,
    String playerName,
    int currentPlayers,
    int maxPlayers
) implements TournamentEvent {}

// When tournament starts
public record TournamentStarted(
    UUID tournamentId,
    int playerCount,
    int tableCount,
    int prizePool
) implements TournamentEvent {}

// When blind level increases
public record TournamentLevelAdvanced(
    UUID tournamentId,
    int newLevel,
    int smallBlind,
    int bigBlind,
    int ante
) implements TournamentEvent {}

// When player is eliminated
public record TournamentPlayerEliminated(
    UUID tournamentId,
    UUID playerId,
    String playerName,
    int finishPosition,
    int prizeWon,
    int remainingPlayers
) implements TournamentEvent {}

// When tournament completes
public record TournamentCompleted(
    UUID tournamentId,
    UUID winnerId,
    String winnerName,
    int prizePool,
    int totalPlayers,
    int finalLevel,
    Duration duration,
    List<FinishResult> topFinishers
) implements TournamentEvent {}
```

### Event Publishing

```java
private void publishEvent(TournamentEvent event) {
    log.debug("Publishing tournament event: {}", event.getEventType());
    eventPublisher.publishEvent(event);
}
```

---

## API Reference

### Create Tournament

```http
POST /api/v2/tournaments
Content-Type: application/json

{
  "name": "Sunday Major",
  "type": "SCHEDULED",
  "buyIn": 100,
  "startingChips": 5000,
  "minPlayers": 10,
  "maxPlayers": 100,
  "blindStructureType": "STANDARD",
  "rebuyAmount": 100,
  "rebuyDeadlineLevel": 6,
  "maxRebuys": 3,
  "addOnAmount": 100,
  "bountyAmount": 10
}
```

### Register for Tournament

```http
POST /api/v2/tournaments/{id}/register
Content-Type: application/json

{
  "playerId": "uuid",
  "playerName": "player1"
}
```

### Get Tournament Details

```http
GET /api/v2/tournaments/{id}

Response:
{
  "id": "uuid",
  "name": "Sunday Major",
  "status": "RUNNING",
  "currentLevel": 5,
  "currentBlinds": "150/300",
  "playersRemaining": 45,
  "prizePool": 10000,
  "averageStack": 11111,
  "tables": [...]
}
```

### Get Leaderboard

```http
GET /api/v2/tournaments/{id}/leaderboard

Response:
[
  {"rank": 1, "playerName": "Alice", "chips": 25000, "tableId": "uuid"},
  {"rank": 2, "playerName": "Bob", "chips": 22000, "tableId": "uuid"},
  ...
]
```

---

## WebSocket Events

### Subscribe to Tournament

```javascript
// STOMP subscription
stompClient.subscribe('/topic/tournament/{tournamentId}', (message) => {
  const event = JSON.parse(message.body);
  handleTournamentEvent(event);
});
```

### Event Types

```typescript
interface TournamentUpdateMessage {
  type: 'PLAYER_REGISTERED' | 'TOURNAMENT_STARTED' | 'LEVEL_UP' | 
        'PLAYER_ELIMINATED' | 'TABLE_REBALANCED' | 'FINAL_TABLE' | 
        'TOURNAMENT_COMPLETED';
  tournamentId: string;
  timestamp: string;
  data: any;
}

// Example: Level Up
{
  "type": "LEVEL_UP",
  "tournamentId": "uuid",
  "timestamp": "2024-01-15T12:30:00Z",
  "data": {
    "level": 6,
    "smallBlind": 200,
    "bigBlind": 400,
    "ante": 50,
    "nextLevelIn": 900
  }
}

// Example: Player Eliminated
{
  "type": "PLAYER_ELIMINATED",
  "tournamentId": "uuid",
  "timestamp": "2024-01-15T12:35:00Z",
  "data": {
    "playerId": "uuid",
    "playerName": "Bob",
    "finishPosition": 45,
    "prizeWon": 0,
    "remainingPlayers": 44,
    "eliminatedBy": "Alice"
  }
}
```

---

## Next Steps

- [ARCHITECTURE.md](ARCHITECTURE.md) — System architecture overview
- [BOT_AI.md](BOT_AI.md) — Bot AI documentation
- [DEPLOYMENT.md](DEPLOYMENT.md) — Production deployment guide
