import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TournamentLeaderboardComponent } from './tournament-leaderboard.component';
import { TournamentPlayer } from '../../model/tournament';

describe('TournamentLeaderboardComponent', () => {
  let component: TournamentLeaderboardComponent;
  let fixture: ComponentFixture<TournamentLeaderboardComponent>;

  const setPlayers = (players: TournamentPlayer[]) => {
    fixture.componentRef.setInput('players', players);
    fixture.detectChanges();
  };

  const setMyPlayerId = (id: string) => {
    fixture.componentRef.setInput('myPlayerId', id);
    fixture.detectChanges();
  };

  const setCompact = (compact: boolean) => {
    fixture.componentRef.setInput('compact', compact);
    fixture.detectChanges();
  };

  const createMockPlayer = (overrides: Partial<TournamentPlayer> = {}): TournamentPlayer => ({
    id: overrides.id ?? 'player-1',
    name: overrides.name ?? 'TestPlayer',
    chips: overrides.chips ?? 10000,
    betAmount: overrides.betAmount ?? 0,
    totalBetInRound: overrides.totalBetInRound ?? 0,
    folded: overrides.folded ?? false,
    isBot: overrides.isBot ?? false,
    isAllIn: overrides.isAllIn ?? false,
    hasActed: overrides.hasActed ?? false,
    seatPosition: overrides.seatPosition ?? 0,
    hand: overrides.hand ?? [],
    isEliminated: overrides.isEliminated ?? false,
    tablesPlayed: overrides.tablesPlayed ?? 1,
    handsWon: overrides.handsWon ?? 0,
    biggestPot: overrides.biggestPot ?? 0,
    knockouts: overrides.knockouts ?? 0,
    rank: overrides.rank,
    finishPosition: overrides.finishPosition,
    prizeMoney: overrides.prizeMoney,
    canAct: () => !overrides.folded && !overrides.isAllIn && (overrides.chips ?? 10000) > 0,
    getDisplayName: () => overrides.name ?? 'TestPlayer',
    isHuman: () => !(overrides.isBot ?? false),
    getStatusText: () => overrides.folded ? 'Folded' : overrides.isAllIn ? 'All-In' : ''
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TournamentLeaderboardComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TournamentLeaderboardComponent);
    component = fixture.componentInstance;


    fixture.componentRef.setInput('players', []);
    fixture.componentRef.setInput('myPlayerId', '');
    fixture.componentRef.setInput('compact', false);

    fixture.detectChanges();
  });





  describe('Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });





  describe('Player Ranking', () => {
    it('should sort players by chip count descending', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', name: 'Low', chips: 5000 }),
        createMockPlayer({ id: 'p2', name: 'High', chips: 20000 }),
        createMockPlayer({ id: 'p3', name: 'Mid', chips: 10000 })
      ]);

      const rankedPlayers = component.allPlayers();
      expect(rankedPlayers[0].name).toBe('High');
      expect(rankedPlayers[1].name).toBe('Mid');
      expect(rankedPlayers[2].name).toBe('Low');
    });

    it('should assign correct rank numbers', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', chips: 20000 }),
        createMockPlayer({ id: 'p2', chips: 15000 }),
        createMockPlayer({ id: 'p3', chips: 10000 })
      ]);

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items[0].querySelector('.rank').textContent).toContain('🥇');
      expect(items[1].querySelector('.rank').textContent).toContain('🥈');
      expect(items[2].querySelector('.rank').textContent).toContain('🥉');
    });

    it('should exclude eliminated players from active ranking', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', chips: 20000 }),
        createMockPlayer({ id: 'p2', chips: 0, isEliminated: true }),
        createMockPlayer({ id: 'p3', chips: 10000 })
      ]);

      const activeCount = component.activePlayers().length;
      expect(activeCount).toBe(2);
    });
  });





  describe('Display', () => {
    it('should display player names', () => {
      setPlayers([
        createMockPlayer({ name: 'Alice' }),
        createMockPlayer({ name: 'Bob' })
      ]);

      const content = fixture.nativeElement.textContent;
      expect(content).toContain('Alice');
      expect(content).toContain('Bob');
    });

    it('should display chip counts', () => {
      setPlayers([
        createMockPlayer({ chips: 12345 })
      ]);

      const content = fixture.nativeElement.textContent;
      expect(content).toContain('12,345');
    });

    it('should show rank icons for top 3', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', chips: 30000 }),
        createMockPlayer({ id: 'p2', chips: 20000 }),
        createMockPlayer({ id: 'p3', chips: 10000 }),
        createMockPlayer({ id: 'p4', chips: 5000 })
      ]);

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items[0].querySelector('.rank').textContent).toContain('🥇');
      expect(items[1].querySelector('.rank').textContent).toContain('🥈');
      expect(items[2].querySelector('.rank').textContent).toContain('🥉');
    });

    it('should highlight current player with "You" badge', () => {
      setPlayers([
        createMockPlayer({ id: 'other', name: 'Other' }),
        createMockPlayer({ id: 'me', name: 'MyName' })
      ]);
      setMyPlayerId('me');

      const youBadge = fixture.nativeElement.querySelector('.you-badge');
      expect(youBadge).toBeTruthy();
    });

    it('should show eliminated players with reduced opacity', () => {
      setPlayers([
        createMockPlayer({ id: 'active', isEliminated: false }),
        createMockPlayer({ id: 'eliminated', isEliminated: true })
      ]);

      const eliminatedItem = fixture.nativeElement.querySelector('.player-row.eliminated');
      expect(eliminatedItem).toBeTruthy();
      expect(eliminatedItem.classList.contains('eliminated')).toBe(true);
    });
  });





  describe('Full Mode', () => {
    beforeEach(() => {
      setCompact(false);
    });

    it('should show podium in full mode', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', chips: 30000 }),
        createMockPlayer({ id: 'p2', chips: 20000 }),
        createMockPlayer({ id: 'p3', chips: 10000 })
      ]);

      const podium = fixture.nativeElement.querySelector('.podium');
      expect(podium).toBeTruthy();
    });

    it('should display stats section', () => {
      setPlayers([
        createMockPlayer({ chips: 20000 }),
        createMockPlayer({ chips: 10000 }),
        createMockPlayer({ chips: 5000 })
      ]);

      const statsSection = fixture.nativeElement.querySelector('.stats-section');
      expect(statsSection).toBeTruthy();
    });

    it('should show largest stack stat', () => {
      setPlayers([
        createMockPlayer({ chips: 50000 }),
        createMockPlayer({ chips: 10000 })
      ]);

      const statsSection = fixture.nativeElement.querySelector('.stats-section');
      expect(statsSection.textContent).toContain('50,000');
    });

    it('should show smallest stack stat', () => {
      setPlayers([
        createMockPlayer({ chips: 50000 }),
        createMockPlayer({ chips: 3000 })
      ]);

      const statsSection = fixture.nativeElement.querySelector('.stats-section');
      expect(statsSection.textContent).toContain('3,000');
    });

    it('should show average stack stat', () => {
      setPlayers([
        createMockPlayer({ chips: 20000 }),
        createMockPlayer({ chips: 10000 })
      ]);

      const statsSection = fixture.nativeElement.querySelector('.stats-section');
      expect(statsSection.textContent).toContain('15,000');
    });

    it('should display all players', () => {
      setPlayers(Array(15).fill(null).map((_, i) =>
        createMockPlayer({ id: `p${i}`, chips: 10000 - i * 100 })
      ));

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items.length).toBe(15);
    });
  });





  describe('Compact Mode', () => {
    beforeEach(() => {
      setCompact(true);
    });

    it('should apply compact class', () => {
      const container = fixture.nativeElement.querySelector('.leaderboard');
      expect(container.classList.contains('compact')).toBe(true);
    });

    it('should hide podium in compact mode', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', chips: 30000 }),
        createMockPlayer({ id: 'p2', chips: 20000 }),
        createMockPlayer({ id: 'p3', chips: 10000 })
      ]);

      const podium = fixture.nativeElement.querySelector('.podium');
      expect(podium).toBeFalsy();
    });

    it('should limit displayed players to 10', () => {
      setPlayers(Array(20).fill(null).map((_, i) =>
        createMockPlayer({ id: `p${i}`, chips: 20000 - i * 100 })
      ));

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items.length).toBeLessThanOrEqual(10);
    });

    it('should always show current player even if outside top 10', () => {
      setPlayers(Array(20).fill(null).map((_, i) =>
        createMockPlayer({ id: `p${i}`, chips: 20000 - i * 1000 })
      ));
      setMyPlayerId('p15');

      const myPlayerItem = fixture.nativeElement.querySelector('.my-position-highlight');
      expect(myPlayerItem).toBeTruthy();
    });

    it('should hide stats section in compact mode', () => {
      setPlayers([createMockPlayer()]);

      const statsSection = fixture.nativeElement.querySelector('.stats-section');
      expect(statsSection).toBeFalsy();
    });
  });





  describe('Computed Values', () => {
    it('should calculate average stack correctly', () => {
      setPlayers([
        createMockPlayer({ chips: 15000 }),
        createMockPlayer({ chips: 10000 }),
        createMockPlayer({ chips: 5000 })
      ]);

      expect(component.averageStack()).toBe(10000);
    });

    it('should find largest stack', () => {
      setPlayers([
        createMockPlayer({ chips: 8000 }),
        createMockPlayer({ chips: 25000 }),
        createMockPlayer({ chips: 12000 })
      ]);

      expect(component.largestStack()).toBe(25000);
    });

    it('should find smallest stack among active players', () => {
      setPlayers([
        createMockPlayer({ chips: 8000 }),
        createMockPlayer({ chips: 0, isEliminated: true }),
        createMockPlayer({ chips: 12000 })
      ]);

      expect(component.smallestStack()).toBe(8000);
    });

    it('should handle empty player list', () => {
      setPlayers([]);

      expect(component.averageStack()).toBe(0);
      expect(component.largestStack()).toBe(0);
      expect(component.smallestStack()).toBe(0);
    });

    it('should get current player rank', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', chips: 30000 }),
        createMockPlayer({ id: 'me', chips: 20000 }),
        createMockPlayer({ id: 'p3', chips: 10000 })
      ]);
      setMyPlayerId('me');

      expect(component.myPosition()).toBe(2);
    });

    it('should return null rank when player not found', () => {
      setPlayers([createMockPlayer({ id: 'other' })]);
      setMyPlayerId('nonexistent');

      expect(component.myPosition()).toBeNull();
    });
  });





  describe('Empty State', () => {
    it('should handle empty player list', () => {
      setPlayers([]);

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items.length).toBe(0);
    });

    it('should display players when they exist', () => {
      setPlayers([createMockPlayer()]);

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items.length).toBe(1);
    });
  });





  describe('Prize Display', () => {
    it('should display eliminated players', () => {
      setPlayers([
        createMockPlayer({
          id: 'winner',
          chips: 0,
          isEliminated: true,
          finishPosition: 1,
          prizeMoney: 500
        })
      ]);

      const eliminatedText = fixture.nativeElement.querySelector('.eliminated-text');
      expect(eliminatedText).toBeTruthy();
      expect(eliminatedText.textContent).toContain('Out');
    });

    it('should show Out text for eliminated players', () => {
      setPlayers([
        createMockPlayer({
          id: 'loser',
          chips: 0,
          isEliminated: true,
          finishPosition: 5,
          prizeMoney: undefined
        })
      ]);

      const eliminatedText = fixture.nativeElement.querySelector('.eliminated-text');
      expect(eliminatedText).toBeTruthy();
    });
  });





  describe('Accessibility', () => {
    it('should display player list', () => {
      setPlayers([createMockPlayer()]);

      const list = fixture.nativeElement.querySelector('.player-list');
      expect(list).toBeTruthy();
    });

    it('should display all players in list', () => {
      setPlayers([
        createMockPlayer({ id: 'p1' }),
        createMockPlayer({ id: 'p2' })
      ]);

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items.length).toBe(2);
    });

    it('should display player names correctly', () => {
      setPlayers([
        createMockPlayer({ name: 'TestPlayer', chips: 10000 })
      ]);

      const playerName = fixture.nativeElement.querySelector('.player-name');
      expect(playerName.textContent).toContain('TestPlayer');
    });
  });





  describe('Edge Cases', () => {
    it('should handle players with same chip count', () => {
      setPlayers([
        createMockPlayer({ id: 'p1', name: 'First', chips: 10000 }),
        createMockPlayer({ id: 'p2', name: 'Second', chips: 10000 }),
        createMockPlayer({ id: 'p3', name: 'Third', chips: 10000 })
      ]);

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items.length).toBe(3);
    });

    it('should handle very large chip counts', () => {
      setPlayers([
        createMockPlayer({ chips: 1000000000 })
      ]);

      const content = fixture.nativeElement.textContent;
      expect(content).toContain('1,000,000,000');
    });

    it('should handle single player', () => {
      setPlayers([
        createMockPlayer({ id: 'solo', name: 'Solo Player' })
      ]);

      const items = fixture.nativeElement.querySelectorAll('.player-row');
      expect(items.length).toBe(1);
    });
  });
});
