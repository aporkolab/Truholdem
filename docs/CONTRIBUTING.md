# Contributing to TruHoldem

Thank you for your interest in contributing to TruHoldem! This guide will help you get started.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Commit Guidelines](#commit-guidelines)

---

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to:

- Be respectful and inclusive
- Accept constructive criticism gracefully
- Focus on what's best for the community
- Show empathy towards other contributors

---

## Getting Started

### Prerequisites

Ensure you have installed:

- Java 21 (Temurin/OpenJDK recommended)
- Node.js 20+
- Docker & Docker Compose
- Git

### Fork and Clone

```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/truholdem.git
cd truholdem

# Add upstream remote
git remote add upstream https://github.com/original/truholdem.git
```

### Initial Setup

```bash
# Start infrastructure
docker-compose up -d postgres redis

# Backend setup
cd backend
./mvnw clean install

# Frontend setup
cd ../frontend
npm ci
```

### Verify Setup

```bash
# Run backend tests
cd backend && ./mvnw verify

# Run frontend tests
cd frontend && npm run test:ci

# Run E2E tests
npm run e2e
```

---

## Development Workflow

### Branch Strategy

```
main           ← Production-ready code
  └── develop  ← Integration branch
       ├── feature/XXX-description
       ├── bugfix/XXX-description
       └── hotfix/XXX-description
```

### Creating a Feature Branch

```bash
# Sync with upstream
git fetch upstream
git checkout develop
git merge upstream/develop

# Create feature branch
git checkout -b feature/123-add-tournament-chat
```

### Running Locally

```bash
# Terminal 1: Infrastructure
docker-compose up -d postgres redis jaeger

# Terminal 2: Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 3: Frontend
cd frontend
npm run dev
```

Access the application at http://localhost:4200

---

## Code Standards

### Backend (Java/Spring Boot)

**Style Guide**
- Follow Google Java Style Guide
- Use meaningful variable and method names
- Maximum line length: 120 characters
- Use constructor injection for dependencies

**Example:**

```java
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerService playerService;
    private final GameMetrics metrics;

    public Game createGame(CreateGameRequest request) {
        // Validate input
        validateRequest(request);

        // Create game
        var game = Game.builder()
            .name(request.getName())
            .maxPlayers(request.getMaxPlayers())
            .smallBlind(request.getSmallBlind())
            .bigBlind(request.getBigBlind())
            .build();

        metrics.incrementGamesCreated();
        return gameRepository.save(game);
    }
}
```

**Package Structure**
```
com.truholdem/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Data access
├── model/           # Domain entities
├── dto/             # Data transfer objects
├── mapper/          # Object mappers
├── websocket/       # WebSocket handlers
└── observability/   # Metrics & tracing
```

### Frontend (Angular/TypeScript)

**Style Guide**
- Follow Angular Style Guide
- Use strict TypeScript (no `any` types)
- Prefer standalone components
- Use signals for reactive state where appropriate

**Example:**

```typescript
@Component({
  selector: 'app-game-table',
  standalone: true,
  imports: [CommonModule, CardComponent],
  templateUrl: './game-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameTableComponent {
  private readonly gameStore = inject(GameStore);

  readonly game = this.gameStore.game;
  readonly players = this.gameStore.players;
  readonly currentPlayer = this.gameStore.currentPlayer;

  onPlayerAction(action: PlayerAction): void {
    this.gameStore.submitAction(action);
  }
}
```

**File Structure**
```
src/app/
├── core/            # Singleton services, guards
├── shared/          # Shared components, pipes, directives
├── features/        # Feature modules
│   ├── game/
│   ├── tournament/
│   └── analysis/
└── store/           # State management
```

---

## Testing Requirements

All contributions must include appropriate tests.

### Backend Testing

**Unit Tests** (required for all new code)
```java
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void shouldCreateGame_WhenValidRequest() {
        // Given
        var request = new CreateGameRequest("Test Game", 6, 10, 20);
        when(gameRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        var result = gameService.createGame(request);

        // Then
        assertThat(result.getName()).isEqualTo("Test Game");
        verify(gameRepository).save(any(Game.class));
    }
}
```

**Integration Tests** (for repository and controller layers)
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GameRepositoryIntegrationTest {

    @Autowired
    private GameRepository gameRepository;

    @Test
    void shouldFindActiveGames() {
        // Test with real database
    }
}
```

### Frontend Testing

**Unit Tests** (Jest)
```typescript
describe('GameStore', () => {
  let store: GameStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GameStore],
      imports: [HttpClientTestingModule],
    });
    store = TestBed.inject(GameStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should load game successfully', () => {
    const mockGame = { id: '123', name: 'Test' };

    store.loadGame('123');

    const req = httpMock.expectOne('/api/games/123');
    req.flush(mockGame);

    expect(store.game()).toEqual(mockGame);
  });
});
```

**E2E Tests** (Cypress)
```typescript
describe('Game Flow', () => {
  beforeEach(() => {
    cy.login('testuser', 'password');
  });

  it('should create and join a game', () => {
    cy.visit('/games');
    cy.getByTestId('create-game-btn').click();
    cy.getByTestId('game-name-input').type('My Game');
    cy.getByTestId('submit-btn').click();

    cy.url().should('include', '/games/');
    cy.getByTestId('game-table').should('be.visible');
  });
});
```

### Coverage Requirements

| Type | Minimum Coverage |
|------|-----------------|
| Backend Unit Tests | 80% |
| Frontend Unit Tests | 75% |
| E2E Critical Paths | All user flows |

Run coverage reports:
```bash
# Backend
./mvnw verify jacoco:report

# Frontend
npm run test:coverage
```

---

## Pull Request Process

### Before Submitting

- [ ] Code follows project style guidelines
- [ ] All tests pass locally
- [ ] New code has appropriate test coverage
- [ ] Documentation updated if needed
- [ ] Commit messages follow conventions
- [ ] Branch is rebased on latest develop

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe testing performed

## Checklist
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Self-reviewed code
```

### Review Process

1. Create PR against `develop` branch
2. Automated CI checks must pass
3. At least one maintainer approval required
4. Address review feedback
5. Squash and merge when approved

---

## Commit Guidelines

Follow Conventional Commits specification:

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, no code change |
| `refactor` | Code restructuring |
| `test` | Adding/updating tests |
| `chore` | Maintenance tasks |
| `perf` | Performance improvement |

### Examples

```bash
# Feature
feat(tournament): add rebuy functionality

# Bug fix
fix(game): resolve pot calculation for side pots

# With scope and body
feat(bot-ai): implement Monte Carlo hand evaluation

Adds Monte Carlo simulation for post-flop hand strength
calculation with configurable iteration count.

Closes #123
```

### Commit Best Practices

- Keep commits atomic (one logical change per commit)
- Write clear, descriptive messages
- Reference issue numbers when applicable
- Avoid commits like "fix", "wip", "stuff"

---

## Questions?

- Open a GitHub Discussion for general questions
- Create an Issue for bugs or feature requests
- Tag maintainers for urgent matters

Thank you for contributing to TruHoldem! 🃏
