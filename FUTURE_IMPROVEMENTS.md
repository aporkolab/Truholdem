# 🚀 Future Improvements Roadmap

## Overview

Ez a dokumentum a TruHoldem alkalmazás lehetséges továbbfejlesztéseit tartalmazza prioritás szerint rendezve.

---

## 🔴 HIGH PRIORITY (Erősen ajánlott)

### 1. WebSocket Real-Time Updates
**Jelenlegi állapot:** HTTP polling
**Cél:** Real-time game state szinkronizáció

**Implementáció:**
```java
// Backend: WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}

// GameStatePublisher.java
@Component
public class GameStatePublisher {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void publishGameUpdate(UUID gameId, Game game) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, game);
    }
}
```

**Frontend:**
```typescript
// websocket.service.ts
export class WebSocketService {
  private stompClient: Client;
  
  connect(gameId: string): Observable<Game> {
    return new Observable(observer => {
      this.stompClient.subscribe(`/topic/game/${gameId}`, message => {
        observer.next(JSON.parse(message.body));
      });
    });
  }
}
```

**Előny:** 
- Nincs polling overhead
- Instant UI update
- Skálázhatóbb

**Becsült idő:** 4-6 óra

---

### 2. Advanced Bot AI
**Jelenlegi állapot:** Random döntések egyszerű pot odds-al
**Cél:** Monte Carlo szimulációval és hand range-ekkel

**Implementáció:**
```java
@Service
public class AdvancedBotService {
    
    public PlayerAction decide(Game game, Player bot) {
        double handStrength = calculateHandStrength(bot.getHand(), game.getCommunityCards());
        double potOdds = calculatePotOdds(game);
        double impliedOdds = calculateImpliedOdds(game);
        
        // Position-based adjustment
        int positionScore = getPositionScore(game, bot);
        
        // Opponent modeling
        double aggression = getOpponentAggression(game);
        
        // Decision tree
        if (handStrength > 0.8) {
            return calculateValueBet(game, handStrength);
        } else if (handStrength > potOdds && impliedOdds > 0) {
            return PlayerAction.CALL;
        } else if (shouldBluff(positionScore, aggression)) {
            return calculateBluffBet(game);
        }
        
        return PlayerAction.FOLD;
    }
    
    private double calculateHandStrength(List<Card> hand, List<Card> community) {
        // Monte Carlo simulation - 1000 random opponent hands
        int wins = 0;
        for (int i = 0; i < 1000; i++) {
            List<Card> opponentHand = generateRandomHand(hand, community);
            if (compareHands(hand, opponentHand, community) > 0) {
                wins++;
            }
        }
        return wins / 1000.0;
    }
}
```

**Előny:**
- Reálisabb játékélmény
- Tanulási lehetőség a játékosnak
- Portfolio showcase: AI/ML

**Becsült idő:** 8-12 óra

---

### 3. Hand History & Replay
**Cél:** Lejátszott kezek visszanézése, elemzése

**Backend:**
```java
@Entity
public class HandHistory {
    @Id
    private UUID id;
    private UUID gameId;
    private int handNumber;
    
    @ElementCollection
    private List<ActionRecord> actions;
    
    @ElementCollection
    private List<Card> board;
    
    private LocalDateTime playedAt;
    private String winnerName;
    private int potSize;
}

@Embeddable
public class ActionRecord {
    private UUID playerId;
    private String playerName;
    private PlayerAction action;
    private int amount;
    private GamePhase phase;
    private LocalDateTime timestamp;
}
```

**Frontend:**
```typescript
// hand-replay.component.ts
export class HandReplayComponent {
  actions: ActionRecord[] = [];
  currentIndex = 0;
  
  play() {
    interval(1000).pipe(
      take(this.actions.length)
    ).subscribe(i => {
      this.applyAction(this.actions[i]);
    });
  }
  
  stepForward() { ... }
  stepBackward() { ... }
}
```

**Becsült idő:** 6-8 óra

---

## 🟡 MEDIUM PRIORITY (Ajánlott)

### 4. Tournament Mode
**Cél:** Multi-table tournament support

**Features:**
- Blind structure (increasing blinds)
- Table balancing
- Prize pool distribution
- Sit & Go / Scheduled tournaments

**Becsült idő:** 15-20 óra

---

### 5. Player Statistics
**Cél:** Részletes játékos statisztikák

**Metrics:**
- VPIP (Voluntarily Put In Pot)
- PFR (Pre-Flop Raise)
- AF (Aggression Factor)
- WTSD (Went to Showdown)
- Win Rate

```java
@Entity
public class PlayerStats {
    private UUID playerId;
    private int handsPlayed;
    private int handsWon;
    private BigDecimal totalWinnings;
    private double vpip;
    private double pfr;
    private double aggressionFactor;
}
```

**Becsült idő:** 8-10 óra

---

### 6. Responsive Mobile UI
**Cél:** Mobile-first design

**Feladatok:**
- Érintőképernyő-barát gombok
- Swipe akciók
- Portrait/Landscape layout
- PWA support

**Becsült idő:** 10-15 óra

---

## 🟢 LOW PRIORITY (Nice to Have)

### 7. Social Features
- Friend list
- Private tables
- Chat system
- Achievements/Badges

### 8. Customization
- Kártya design választás
- Asztal téma
- Avatar upload
- Hang effektek

### 9. Multi-Language Support
- i18n implementáció
- Magyar, English, Német

### 10. Leaderboard
- Napi/Heti/Összes idők
- Skill-based ranking (ELO)

---

## 📊 Prioritási Mátrix

| Feature | Effort | Impact | Priority |
|---------|--------|--------|----------|
| WebSocket | Medium | High | 🔴 High |
| Bot AI | High | High | 🔴 High |
| Hand History | Medium | Medium | 🔴 High |
| Tournament | High | High | 🟡 Medium |
| Statistics | Medium | Medium | 🟡 Medium |
| Mobile UI | Medium | High | 🟡 Medium |
| Social | High | Medium | 🟢 Low |
| Customization | Low | Low | 🟢 Low |
| Multi-Lang | Low | Low | 🟢 Low |
| Leaderboard | Medium | Medium | 🟢 Low |

---

## 🛠️ Technical Debt

### Prioritás szerint:

1. **Test Coverage növelése** (jelenleg ~35%)
   - Cél: 80%+
   - Controller tesztek
   - Service integration tesztek

2. **Error Handling javítása**
   - Global exception handler
   - Structured error responses
   - Frontend error boundaries

3. **Performance optimalizáció**
   - Database query optimization
   - Caching (Redis)
   - Lazy loading

4. **Security hardening**
   - Rate limiting
   - Input sanitization
   - CORS configuration review

---

## 📅 Suggested Roadmap

### Phase 1 (1-2 hét)
- [x] Bug fixes
- [x] Showdown implementation
- [x] Tests
- [ ] WebSocket basics

### Phase 2 (2-3 hét)
- [ ] Advanced Bot AI
- [ ] Hand History
- [ ] Statistics v1

### Phase 3 (3-4 hét)
- [ ] Tournament Mode
- [ ] Mobile UI
- [ ] Performance optimization

### Phase 4 (4+ hét)
- [ ] Social features
- [ ] Customization
- [ ] Production deployment

---

## 💡 Quick Wins

Kis erőfeszítéssel nagy hatás:

1. **Sound effects** - Akció hangok (fold, check, chip sounds)
2. **Animations** - Kártya animációk CSS-el
3. **Keyboard shortcuts** - F=Fold, C=Call, R=Raise
4. **Auto-muck** - Vesztes kéz automatikus eldobása
5. **Time bank** - Extra gondolkodási idő

---

## Contact

Kérdések esetén: adam@porkolab.com
