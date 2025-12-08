# TruHoldem v2.0 🃏

[![Build](https://img.shields.io/github/actions/workflow/status/APorkolab/Truholdem/ci-cd.yml?branch=main)](../../actions)
[![Coverage](https://img.shields.io/badge/coverage-80%25-brightgreen)](./backend/target/site/jacoco/index.html)
[![License](https://img.shields.io/badge/license-MIT-informational.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Angular](https://img.shields.io/badge/Angular-20-red)](https://angular.io/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)

A sophisticated **Texas Hold'em poker simulator** with advanced Monte Carlo AI, real-time multiplayer, comprehensive statistics tracking, and hand replay functionality.

---

## ✨ Features

### Core Gameplay
- 🎮 **Full Texas Hold'em Implementation** - Pre-flop, Flop, Turn, River, Showdown
- 🤖 **Advanced AI Opponents** - Monte Carlo simulation with 500 iterations
- 💰 **Complex Pot Management** - Side pots, all-in situations
- 🎯 **Hand Evaluation** - Complete poker hand ranking system

### Statistics & Analytics
- 📊 **15+ Player Metrics** - VPIP, PFR, Aggression Factor, WTSD, W$SD
- 🏆 **6 Leaderboard Categories** - Winnings, Win Rate, Hands Won, Biggest Pot, Win Streak
- 📈 **Session Tracking** - Win/loss streaks, session history

### Hand History & Replay
- 📝 **Complete Hand Recording** - Every action, bet, and card dealt
- ▶️ **Interactive Replay** - Step through hands action by action
- 🔍 **Search & Filter** - Find hands by player, date, pot size

### Technical Features
- 🔄 **Real-time Updates** - WebSocket for live game state
- 🔊 **Sound Effects** - 7 distinct game sounds
- ⚙️ **Customizable Settings** - Sound, animations, table theme
- 📱 **Responsive Design** - Desktop, tablet, mobile

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Frontend (Angular 20)                      │
│  Components: GameTable, Leaderboard, HandReplay, Settings   │
│  Services: PokerService, StatisticsService, WebSocketService│
└────────────────────────────┬────────────────────────────────┘
                             │ REST API / WebSocket
┌────────────────────────────┴────────────────────────────────┐
│                  Backend (Spring Boot 3.2)                   │
│  Controllers → Services → Repositories → PostgreSQL         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Key Services:                                        │    │
│  │ • PokerGameService - Game logic & state management  │    │
│  │ • AdvancedBotAIService - Monte Carlo AI decisions   │    │
│  │ • HandEvaluator - Poker hand ranking                │    │
│  │ • PlayerStatisticsService - Stats tracking          │    │
│  │ • HandHistoryService - Recording & replay           │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Node.js 20+
- PostgreSQL 16+
- Maven 3.9+

### Installation

```bash
# Clone repository
git clone https://github.com/APorkolab/Truholdem.git
cd Truholdem

# Backend setup
cd backend
./mvnw spring-boot:run

# Frontend setup (new terminal)
cd frontend
npm install
npm run dev
```

### Docker Deployment

```bash
docker-compose up -d
```

Access the application at `http://localhost:4200`

---

## 🧪 Testing

### Backend Tests
```bash
cd backend
./mvnw test                    # Run all tests
./mvnw verify                  # Tests + coverage report
```

### Frontend Tests
```bash
cd frontend
npm run test:ci                # Unit tests with coverage
npm run e2e:headless           # E2E tests (Cypress)
```

### Test Summary
| Component | Tests | Description |
|-----------|-------|-------------|
| Backend Unit | 120+ | Service layer tests |
| Backend Integration | 45+ | API endpoint tests |
| Frontend Unit | 80+ | Component & service tests |
| E2E (Cypress) | 50+ | Full user flow tests |

---

## 📚 API Documentation

### Game Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/poker/start` | Create new game |
| GET | `/api/poker/{id}` | Get game state |
| POST | `/api/poker/{id}/action` | Player action |
| POST | `/api/poker/{id}/bot/{botId}` | Execute bot action |

### Statistics Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stats/player/{name}` | Player statistics |
| GET | `/api/stats/leaderboard` | All leaderboards |
| GET | `/api/stats/leaderboard/{type}` | Specific leaderboard |

### Hand History Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/history/{id}` | Get hand details |
| GET | `/api/history/{id}/replay` | Replay data |
| GET | `/api/history/game/{gameId}` | Game history |
| GET | `/api/history/recent` | Recent hands |

### Monitoring
| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health status |
| `/actuator/metrics` | Metrics |
| `/actuator/prometheus` | Prometheus format |

---

## 🤖 Bot AI System

The bot AI uses **Monte Carlo simulation** with:

- **500 iterations** per decision for hand strength
- **Position-aware** play (early, middle, late, button)
- **Pot odds** calculation
- **4 Personality Types**: TAG, LAG, Rock, Fish

---

## 📊 Statistics Tracked

| Stat | Description |
|------|-------------|
| **VPIP** | Voluntarily Put $ In Pot |
| **PFR** | Pre-Flop Raise % |
| **AF** | Aggression Factor |
| **WTSD** | Went To Showdown % |
| **W$SD** | Won $ At Showdown % |

---

## 🛠️ Tech Stack

### Backend
- Java 21, Spring Boot 3.2, PostgreSQL 16
- Spring Data JPA, Liquibase, WebSocket (STOMP)
- JUnit 5, Mockito, JaCoCo

### Frontend
- Angular 20, TypeScript 5.9, RxJS
- Bootstrap 5, Jasmine/Karma, Cypress

### DevOps
- Docker, GitHub Actions, Trivy, Prometheus

---

## 📁 Project Structure

```
truholdem/
├── backend/
│   └── src/main/java/com/truholdem/
│       ├── controller/      # REST endpoints
│       ├── service/         # Business logic
│       ├── model/           # Domain entities
│       └── repository/      # Data access
├── frontend/
│   └── src/app/
│       ├── game-table/      # Main game UI
│       ├── leaderboard/     # Statistics
│       ├── hand-replay/     # Replay viewer
│       └── services/        # API clients
├── .github/workflows/       # CI/CD
└── docker-compose.yml
```

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file.

---

## 👤 Author

**Adam Dr. Porkolab**
- GitHub: [@APorkolab](https://github.com/APorkolab)
- Website: [www.aporkolab.com](https://www.aporkolab.com)

---

<p align="center">Made with ❤️ and ♠️♥️♦️♣️</p>
