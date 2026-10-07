# 🏗 TruHoldem Architecture

This document describes the system architecture, design decisions, and technical patterns used in TruHoldem.

---

## Table of Contents
- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Backend Architecture](#backend-architecture)
- [Frontend Architecture](#frontend-architecture)
- [Data Flow](#data-flow)
- [Design Patterns](#design-patterns)
- [Database Design](#database-design)
- [Security Architecture](#security-architecture)
- [Scalability Considerations](#scalability-considerations)

---

## Overview

TruHoldem is built as a **distributed, real-time system** designed for:
- **High concurrency** — Multiple simultaneous games with real-time updates
- **Horizontal scalability** — Stateless backend with Redis-backed session management
- **Observability** — Full distributed tracing and metrics collection
- **Testability** — Clean separation of concerns enabling comprehensive testing

### Key Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| Spring Boot 4.1.1 + Java 21 | Java LTS with virtual threads for I/O efficiency |
| Angular 22.2.1 | SPA framework with reactive components |
| WebSocket (STOMP) | Real-time bidirectional communication for game state |
| Redis for sessions | Enables horizontal scaling of WebSocket connections |
| PostgreSQL | ACID compliance for game state integrity |
| Domain-Driven Design | Complex poker domain requires explicit modeling |
| NgRx ComponentStore 22.0.1 | Lightweight reactive state management for Angular |

---

## System Architecture

### High-Level Overview

```
                                    ┌─────────────────┐
                                    │   Load Balancer │
                                    │     (Nginx)     │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
              ┌─────▼─────┐           ┌──────▼─────┐           ┌──────▼─────┐
              │  Backend  │           │  Backend   │           │  Backend   │
              │ Instance 1│           │ Instance 2 │           │ Instance N │
              └─────┬─────┘           └──────┬─────┘           └──────┬─────┘
                    │                        │                        │
                    └────────────────────────┼────────────────────────┘
                                             │
              ┌──────────────────────────────┼──────────────────────────────┐
              │                              │                              │
        ┌─────▼─────┐                 ┌──────▼─────┐                ┌───────▼───────┐
        │ PostgreSQL│                 │   Redis    │                │    Jaeger     │
        │   (Data)  │                 │ (Sessions) │                │   (Tracing)   │
        └───────────┘                 └────────────┘                └───────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| **Nginx** | SSL termination, load balancing, rate limiting, static file serving |
| **Backend Instances** | Business logic, WebSocket handling, REST API |
| **PostgreSQL** | Primary data storage for games, users, tournaments |
| **Redis** | WebSocket session registry, caching, pub/sub for cluster communication |
| **Jaeger** | Distributed tracing aggregation |
| **Prometheus** | Metrics collection and alerting |
| **Grafana** | Monitoring dashboards |

---

## Backend Architecture

### Package Structure

```
com.truholdem/
├── config/                 # Spring configuration
│   ├── api/               # API versioning config
│   ├── CacheConfig        # Redis cache configuration
│   ├── WebSocketConfig    # STOMP WebSocket setup
│   └── SecurityConfig     # JWT & authentication
│
├── controller/            # REST & WebSocket endpoints
│   ├── PokerGameController    # Game operations
│   ├── TournamentController   # Tournament management
│   ├── GameWebSocketController # Real-time game events
│   └── HandAnalysisController # Equity calculations
│
├── domain/                # DDD tactical patterns
│   ├── aggregate/         # Root aggregates (PokerGame)
│   ├── event/             # Domain events
│   ├── value/             # Value objects (Chips, Position)
│   └── exception/         # Domain-specific exceptions
│
├── model/                 # JPA entities
│   ├── Game               # Game state entity
│   ├── Player             # Player entity
│   ├── Tournament         # Tournament entity
│   └── HandHistory        # Historical game data
│
├── service/               # Business logic
│   ├── PokerGameService   # Core game mechanics
│   ├── TournamentService  # Tournament orchestration
│   ├── AdvancedBotAIService   # AI decision engine
│   └── HandAnalysisService    # Equity calculations
│
├── repository/            # Data access layer
│
├── observability/         # Cross-cutting concerns
│   ├── GameMetrics        # Micrometer metrics
│   ├── GameTracer         # OpenTelemetry tracing
│   └── TracingAspect      # AOP tracing
│
└── websocket/             # WebSocket infrastructure
    ├── ClusterSessionRegistry # Redis-backed sessions
    ├── RedisGameEventBroadcaster # Pub/sub broadcaster
    └── ReconnectionHandler    # Connection recovery
```

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Controller Layer                         │
│  REST Controllers │ WebSocket Controllers │ Exception Handlers│
└─────────────────────────────────┬───────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────┐
│                      Service Layer                           │
│  Business Logic │ Transaction Boundaries │ Orchestration     │
└─────────────────────────────────┬───────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────┐
│                      Domain Layer                            │
│  Aggregates │ Value Objects │ Domain Events │ Business Rules │
└─────────────────────────────────┬───────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────┐
│                   Infrastructure Layer                       │
│  Repositories │ External Services │ Messaging │ Caching      │
└─────────────────────────────────────────────────────────────┘
```

### Domain Model

```
┌─────────────────────────────────────────────────────────────┐
│                    PokerGame (Aggregate Root)                │
├─────────────────────────────────────────────────────────────┤
│  - id: UUID                                                 │
│  - phase: GamePhase (PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN) │
│  - players: List<Player>                                    │
│  - communityCards: List<Card>                               │
│  - pot: Pot (Value Object)                                  │
│  - blindLevel: BlindLevel                                   │
│  - version: Long (Optimistic Locking)                       │
├─────────────────────────────────────────────────────────────┤
│  + processAction(playerId, action, amount)                  │
│  + advancePhase()                                           │
│  + determineWinner()                                        │
│  + distributePot()                                          │
└─────────────────────────────────────────────────────────────┘
         │
         │ Contains
         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Player (Entity)                         │
├─────────────────────────────────────────────────────────────┤
│  - id: UUID                                                 │
│  - name: String                                             │
│  - chips: Chips (Value Object)                              │
│  - hand: List<Card>                                         │
│  - betAmount: int                                           │
│  - isFolded: boolean                                        │
│  - isAllIn: boolean                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## Frontend Architecture

### Module Structure

```
src/app/
├── app.config.ts          # Application configuration
├── app.routes.ts          # Route definitions (lazy loaded)
│
├── auth/                  # Authentication feature
│   ├── login.component.ts
│   ├── register.component.ts
│   └── auth.routes.ts
│
├── game-table/            # Main game interface
│   ├── game-table.component.ts
│   ├── game-table.component.html
│   └── game-table.component.scss
│
├── tournament/            # Tournament feature (lazy loaded)
│   ├── tournament-list/
│   ├── tournament-lobby/
│   ├── tournament-table/
│   └── tournament.routes.ts
│
├── analysis/              # Hand analysis (lazy loaded)
│   ├── equity-calculator/
│   ├── range-builder/
│   └── analysis.routes.ts
│
├── store/                 # State management
│   ├── game.store.ts      # Game state (ComponentStore)
│   ├── tournament.store.ts
│   └── statistics.store.ts
│
├── services/              # API & utility services
│   ├── poker.service.ts
│   ├── websocket.service.ts
│   └── auth.service.ts
│
└── guards/                # Route guards
    ├── auth.guard.ts
    └── game.guard.ts
```

### State Management (NgRx ComponentStore)

```
┌───────────────────────────────────────────────────────────────┐
│                       GameStore                                │
├───────────────────────────────────────────────────────────────┤
│  State:                                                       │
│  ├─ game: Game | null                                         │
│  ├─ players: Player[]                                         │
│  ├─ currentPlayer: Player | null                              │
│  ├─ loading: boolean                                          │
│  └─ error: string | null                                      │
├───────────────────────────────────────────────────────────────┤
│  Selectors:                                                   │
│  ├─ selectGame$                                               │
│  ├─ selectCurrentPlayer$                                      │
│  ├─ selectValidActions$                                       │
│  └─ selectPotTotal$                                           │
├───────────────────────────────────────────────────────────────┤
│  Updaters:                                                    │
│  ├─ setGame()                                                 │
│  ├─ updatePlayer()                                            │
│  └─ setError()                                                │
├───────────────────────────────────────────────────────────────┤
│  Effects:                                                     │
│  ├─ loadGame()                                                │
│  ├─ performAction()                                           │
│  └─ subscribeToUpdates()                                      │
└───────────────────────────────────────────────────────────────┘
```

### Component Communication

```
┌────────────────────────────────────────────────────────────────┐
│                    GameTableComponent                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Player Positions                      │   │
│  │  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐   │   │
│  │  │ P1  │  │ P2  │  │ P3  │  │ P4  │  │ P5  │  │ P6  │   │   │
│  │  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Community Cards & Pot                       │   │
│  │         [🂡] [🂱] [🂿] [🃊] [🃗]    Pot: $1,250           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  ActionPanelComponent                     │  │
│  │  [Fold] [Check/Call $50] [Raise]  ├──▶ RaiseInputComponent│  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### Game Action Flow

```
┌─────────┐     ┌────────────┐     ┌─────────────┐     ┌──────────────┐
│  User   │────▶│  Angular   │────▶│  WebSocket  │────▶│   Backend    │
│ Action  │     │ Component  │     │  Service    │     │  Controller  │
└─────────┘     └────────────┘     └─────────────┘     └──────┬───────┘
                                                              │
                                                              ▼
┌─────────┐     ┌────────────┐     ┌─────────────┐     ┌──────────────┐
│  State  │◀────│   Store    │◀────│  WebSocket  │◀────│   Service    │
│ Update  │     │  Updater   │     │  Message    │     │   + Events   │
└─────────┘     └────────────┘     └─────────────┘     └──────────────┘

Timeline:
1. User clicks "Raise $100"
2. Component dispatches action to Store
3. Store effect sends STOMP message via WebSocket
4. Backend validates action, updates game state
5. Backend broadcasts state update to all subscribers
6. WebSocket service receives update
7. Store updater merges new state
8. Component re-renders with new state
```

### Tournament Event Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                     Tournament Lifecycle                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  REGISTERING ──▶ STARTING ──▶ RUNNING ──▶ FINAL_TABLE ──▶ COMPLETE│
│       │              │            │            │              │   │
│       ▼              ▼            ▼            ▼              ▼   │
│  PlayerRegistered  Started    LevelUp    TablesRebalanced  Winner│
│  PlayerUnregistered          Eliminated  PlayerMoved             │
│                              HandComplete                        │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘

Event Publishing:
┌────────────────┐     ┌───────────────┐     ┌────────────────────┐
│ TournamentSvc  │────▶│ EventPublisher│────▶│ WebSocket Listeners│
│ (Domain Logic) │     │ (Spring Events)│    │ (STOMP Broadcast)  │
└────────────────┘     └───────────────┘     └────────────────────┘
```

---

## Design Patterns

### Domain-Driven Design (DDD)

| Pattern | Implementation |
|---------|----------------|
| **Aggregate** | `PokerGame` encapsulates all game state and rules |
| **Value Object** | `Chips`, `Position`, `HandStrength` — immutable |
| **Domain Event** | `HandCompleted`, `PlayerEliminated`, `TournamentStarted` |
| **Repository** | `GameRepository`, `TournamentRepository` |

### Other Patterns

| Pattern | Usage |
|---------|-------|
| **Strategy** | `BotPersonality` enum for different AI behaviors |
| **Observer** | Spring Events for domain event propagation |
| **Builder** | `Tournament.TournamentBuilder` for complex construction |
| **Factory** | `BlindStructure.standard()`, `BlindStructure.turbo()` |

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌─────────────────────┐
│    User     │       │    Game     │       │    Tournament       │
├─────────────┤       ├─────────────┤       ├─────────────────────┤
│ id          │◀──────│ creator_id  │       │ id                  │
│ username    │       │ id          │◀──┐   │ name                │
│ email       │       │ phase       │   │   │ status              │
│ password    │       │ pot         │   │   │ type                │
│ created_at  │       │ big_blind   │   │   │ buy_in              │
└─────────────┘       └─────────────┘   │   │ starting_chips      │
      │                     │           │   │ prize_pool          │
      │                     │           │   └─────────────────────┘
      │               ┌─────┴─────┐     │             │
      │               │           │     │             │
      ▼               ▼           ▼     │             ▼
┌─────────────┐ ┌─────────────┐       ┌─────────────────────────┐
│   Player    │ │ HandHistory │       │  TournamentRegistration │
├─────────────┤ ├─────────────┤       ├─────────────────────────┤
│ id          │ │ id          │       │ id                      │
│ user_id     │ │ game_id     │───────│ tournament_id           │
│ game_id     │ │ hand_number │       │ player_id               │
│ chips       │ │ winner_id   │       │ chips                   │
│ position    │ │ pot_size    │       │ status                  │
│ is_folded   │ │ actions     │       │ finish_position         │
└─────────────┘ └─────────────┘       └─────────────────────────┘
```

### Liquibase Migrations

```
db/changelog/
├── 01-initial-schema.xml          # Core tables
├── 02-add-users-and-authentication.xml
├── 03-game-enhancements.xml       # Game state fields
├── 04-advanced-features.xml       # Analytics, history
├── 05-history-and-stats.xml       # Statistics tables
├── 06-optimistic-locking.xml      # Version columns
├── 07-tournaments.xml             # Tournament tables
└── db.changelog-master.xml        # Master changelog
```

---

## Security Architecture

### Authentication Flow

```
┌─────────┐     ┌─────────────┐     ┌──────────────┐     ┌──────────┐
│  Client │────▶│   /login    │────▶│  AuthService │────▶│   User   │
│         │     │  Endpoint   │     │              │     │   Store  │
└─────────┘     └─────────────┘     └──────┬───────┘     └──────────┘
                                           │
                                           ▼
                                    ┌──────────────┐
                                    │   JwtUtil    │
                                    │  (Generate)  │
                                    └──────┬───────┘
                                           │
                                           ▼
┌─────────┐     ┌─────────────┐     ┌──────────────┐
│  Client │◀────│  JWT Token  │◀────│ RefreshToken │
│         │     │  Response   │     │    Store     │
└─────────┘     └─────────────┘     └──────────────┘
```

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "username": "player1",
    "roles": ["ROLE_USER"],
    "iat": 1700000000,
    "exp": 1700003600
  }
}
```

---

## Scalability Considerations

### Horizontal Scaling Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                    Load Balancer (Nginx)                         │
│           Sticky Sessions by Game ID (for WebSocket)             │
└─────────────────────────────┬───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   ┌────▼────┐           ┌────▼────┐           ┌────▼────┐
   │Backend 1│           │Backend 2│           │Backend N│
   │         │           │         │           │         │
   │  Local  │           │  Local  │           │  Local  │
   │  Cache  │           │  Cache  │           │  Cache  │
   └────┬────┘           └────┬────┘           └────┬────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │       Redis       │
                    │  - Session Store  │
                    │  - Pub/Sub        │
                    │  - L2 Cache       │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼─────────┐
                    │    PostgreSQL     │
                    │  - Primary Data   │
                    │  - Read Replicas  │
                    └───────────────────┘
```

### WebSocket Cluster Mode

When running multiple backend instances, WebSocket connections are distributed:

1. **Session Registry** — Redis stores active sessions per game
2. **Event Broadcasting** — Redis Pub/Sub propagates game events to all instances
3. **Reconnection Handling** — Clients can reconnect to any instance

```java
// ClusterSessionRegistry.java
public void broadcastToGame(UUID gameId, GameEvent event) {
    // Publish to Redis channel
    redisTemplate.convertAndSend("game:" + gameId, event);
}

// All instances subscribe to relevant game channels
@RedisListener(pattern = "game:*")
public void onGameEvent(GameEvent event) {
    // Forward to local WebSocket sessions
    messagingTemplate.convertAndSend("/topic/game/" + event.gameId(), event);
}
```

---

## Performance Considerations

### Caching Strategy

| Cache Level | Technology | TTL | Use Case |
|-------------|------------|-----|----------|
| L1 (Local) | Caffeine | 5 min | Frequently accessed game state |
| L2 (Distributed) | Redis | 30 min | Session data, user profiles |
| Database | PostgreSQL | N/A | Persistent data |

### Query Optimization

- **Indexing** — Composite indexes on frequently queried columns
- **Eager/Lazy Loading** — Explicit fetch plans for JPA relationships
- **Pagination** — Cursor-based pagination for large result sets
- **Read Replicas** — For analytics and reporting queries

---

## Next Steps

- [BOT_AI.md](BOT_AI.md) — Detailed Bot AI documentation
- [TOURNAMENTS.md](TOURNAMENTS.md) — Tournament system architecture
- [DEPLOYMENT.md](DEPLOYMENT.md) — Production deployment guide
