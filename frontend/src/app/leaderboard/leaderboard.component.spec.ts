import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LeaderboardComponent } from './leaderboard.component';
import { StatisticsService, PlayerStatistics, LeaderboardData } from '../services/statistics.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';

describe('LeaderboardComponent', () => {
  let component: LeaderboardComponent;
  let fixture: ComponentFixture<LeaderboardComponent>;
  let statisticsServiceMock: jest.Mocked<StatisticsService>;

  const createMockPlayerStats = (overrides: Partial<PlayerStatistics> = {}): PlayerStatistics => ({
    id: overrides.id || 'player-1',
    playerName: overrides.playerName || 'TestPlayer',
    handsPlayed: overrides.handsPlayed ?? 100,
    handsWon: overrides.handsWon ?? 25,
    totalWinnings: overrides.totalWinnings ?? 5000,
    totalLosses: overrides.totalLosses ?? 2000,
    netProfit: overrides.netProfit ?? 3000,
    biggestPotWon: overrides.biggestPotWon ?? 1000,
    currentWinStreak: overrides.currentWinStreak ?? 3,
    longestWinStreak: overrides.longestWinStreak ?? 10,
    currentLoseStreak: overrides.currentLoseStreak ?? 0,
    longestLoseStreak: overrides.longestLoseStreak ?? 5,
    vpip: overrides.vpip ?? 25.5,
    pfr: overrides.pfr ?? 18.3,
    aggressionFactor: overrides.aggressionFactor ?? 2.5,
    winRate: overrides.winRate ?? 25.0,
    totalSessions: overrides.totalSessions ?? 20,
    lastHandPlayed: overrides.lastHandPlayed || new Date().toISOString(),
    ...overrides
  });

  const createMockLeaderboardData = (): LeaderboardData => ({
    byWinnings: [
      createMockPlayerStats({ playerName: 'Winner1', totalWinnings: 10000 }),
      createMockPlayerStats({ playerName: 'Winner2', totalWinnings: 8000 }),
      createMockPlayerStats({ playerName: 'Winner3', totalWinnings: 5000 }),
    ],
    byHandsWon: [
      createMockPlayerStats({ playerName: 'HandWinner1', handsWon: 100 }),
      createMockPlayerStats({ playerName: 'HandWinner2', handsWon: 80 }),
    ],
    byWinRate: [
      createMockPlayerStats({ playerName: 'RateWinner1', winRate: 45.0 }),
      createMockPlayerStats({ playerName: 'RateWinner2', winRate: 40.0 }),
    ],
    byBiggestPot: [
      createMockPlayerStats({ playerName: 'BigPot1', biggestPotWon: 5000 }),
      createMockPlayerStats({ playerName: 'BigPot2', biggestPotWon: 3000 }),
    ],
    byWinStreak: [
      createMockPlayerStats({ playerName: 'Streak1', longestWinStreak: 15 }),
      createMockPlayerStats({ playerName: 'Streak2', longestWinStreak: 12 }),
    ],
    mostActive: [
      createMockPlayerStats({ playerName: 'Active1', handsPlayed: 500 }),
      createMockPlayerStats({ playerName: 'Active2', handsPlayed: 400 }),
    ]
  });

  beforeEach(async () => {
    statisticsServiceMock = {
      getLeaderboard: jest.fn().mockReturnValue(of(createMockLeaderboardData())),
      searchPlayers: jest.fn().mockReturnValue(of([])),
      getPlayerRank: jest.fn().mockReturnValue('Gold'),
      formatWinRate: jest.fn((rate: number) => `${rate.toFixed(1)}%`),
      formatCurrency: jest.fn((amount: number) => `$${amount.toLocaleString()}`),
      formatAggressionFactor: jest.fn((af: number) => af.toFixed(2)),
      getPlayStyleDescription: jest.fn().mockReturnValue('Tight-Aggressive'),
    } as unknown as jest.Mocked<StatisticsService>;

    await TestBed.configureTestingModule({
      imports: [LeaderboardComponent, FormsModule],
      providers: [
        { provide: StatisticsService, useValue: statisticsServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LeaderboardComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should start with loading state', () => {
      expect(component.isLoading).toBe(true);
    });

    it('should default to winnings category', () => {
      expect(component.selectedCategory).toBe('winnings');
    });

    it('should have all category options', () => {
      expect(component.categories.length).toBe(6);
      expect(component.categories.map(c => c.key)).toEqual([
        'winnings', 'handsWon', 'winRate', 'biggestPot', 'winStreak', 'mostActive'
      ]);
    });

    it('should load leaderboard on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(statisticsServiceMock.getLeaderboard).toHaveBeenCalled();
      expect(component.isLoading).toBe(false);
      expect(component.leaderboardData).toBeTruthy();
    }));
  });

  describe('Category Selection', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should switch to handsWon category', () => {
      component.selectCategory('handsWon');
      
      expect(component.selectedCategory).toBe('handsWon');
      expect(component.currentList[0].playerName).toBe('HandWinner1');
    });

    it('should switch to winRate category', () => {
      component.selectCategory('winRate');
      
      expect(component.selectedCategory).toBe('winRate');
      expect(component.currentList[0].playerName).toBe('RateWinner1');
    });

    it('should switch to biggestPot category', () => {
      component.selectCategory('biggestPot');
      
      expect(component.selectedCategory).toBe('biggestPot');
      expect(component.currentList[0].playerName).toBe('BigPot1');
    });

    it('should switch to winStreak category', () => {
      component.selectCategory('winStreak');
      
      expect(component.selectedCategory).toBe('winStreak');
      expect(component.currentList[0].playerName).toBe('Streak1');
    });

    it('should switch to mostActive category', () => {
      component.selectCategory('mostActive');
      
      expect(component.selectedCategory).toBe('mostActive');
      expect(component.currentList[0].playerName).toBe('Active1');
    });
  });

  describe('Column Configuration', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should return correct columns for winnings', () => {
      component.selectCategory('winnings');
      const columns = component.getColumns();
      
      expect(columns.map(c => c.key)).toContain('totalWinnings');
      expect(columns.map(c => c.key)).toContain('handsPlayed');
    });

    it('should return correct columns for handsWon', () => {
      component.selectCategory('handsWon');
      const columns = component.getColumns();
      
      expect(columns.map(c => c.key)).toContain('handsWon');
      expect(columns.map(c => c.key)).toContain('handsPlayed');
    });

    it('should return correct columns for biggestPot', () => {
      component.selectCategory('biggestPot');
      const columns = component.getColumns();
      
      expect(columns.map(c => c.key)).toContain('biggestPotWon');
    });

    it('should return correct columns for winStreak', () => {
      component.selectCategory('winStreak');
      const columns = component.getColumns();
      
      expect(columns.map(c => c.key)).toContain('longestWinStreak');
      expect(columns.map(c => c.key)).toContain('currentWinStreak');
    });
  });

  describe('Value Formatting', () => {
    const mockPlayer = createMockPlayerStats({
      totalWinnings: 5000,
      biggestPotWon: 1000,
      winRate: 25.5,
      handsPlayed: 100
    });

    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should format currency values', () => {
      component.formatValue(mockPlayer, 'totalWinnings');
      expect(statisticsServiceMock.formatCurrency).toHaveBeenCalledWith(5000);
    });

    it('should format biggestPotWon as currency', () => {
      component.formatValue(mockPlayer, 'biggestPotWon');
      expect(statisticsServiceMock.formatCurrency).toHaveBeenCalledWith(1000);
    });

    it('should format winRate as percentage', () => {
      component.formatValue(mockPlayer, 'winRate');
      expect(statisticsServiceMock.formatWinRate).toHaveBeenCalledWith(25.5);
    });

    it('should return raw number for other values', () => {
      const result = component.formatValue(mockPlayer, 'handsPlayed');
      expect(result).toBe('100');
    });
  });

  describe('Rank Emoji', () => {
    it('should return gold medal for first place', () => {
      expect(component.getRankEmoji(0)).toBe('🥇');
    });

    it('should return silver medal for second place', () => {
      expect(component.getRankEmoji(1)).toBe('🥈');
    });

    it('should return bronze medal for third place', () => {
      expect(component.getRankEmoji(2)).toBe('🥉');
    });

    it('should return empty string for other positions', () => {
      expect(component.getRankEmoji(3)).toBe('');
      expect(component.getRankEmoji(10)).toBe('');
    });
  });

  describe('Player Search', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should search for players', fakeAsync(() => {
      const searchResults = [createMockPlayerStats({ playerName: 'SearchResult' })];
      statisticsServiceMock.searchPlayers.mockReturnValue(of(searchResults));

      component.searchQuery = 'test';
      component.searchPlayer();
      tick();

      expect(statisticsServiceMock.searchPlayers).toHaveBeenCalledWith('test');
      expect(component.searchResults).toEqual(searchResults);
    }));

    it('should clear results for empty query', () => {
      component.searchQuery = '';
      component.searchPlayer();

      expect(component.searchResults).toEqual([]);
      expect(statisticsServiceMock.searchPlayers).not.toHaveBeenCalled();
    });

    it('should clear results for whitespace only query', () => {
      component.searchQuery = '   ';
      component.searchPlayer();

      expect(component.searchResults).toEqual([]);
    });
  });

  describe('Player Detail Modal', () => {
    const mockPlayer = createMockPlayerStats({ playerName: 'DetailPlayer' });

    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should select player for detail view', () => {
      component.selectPlayer(mockPlayer);
      
      expect(component.selectedPlayer).toBe(mockPlayer);
    });

    it('should close player detail modal', () => {
      component.selectPlayer(mockPlayer);
      expect(component.selectedPlayer).toBeTruthy();

      component.closePlayerDetail();
      expect(component.selectedPlayer).toBeNull();
    });
  });

  describe('Error Handling', () => {
    it('should handle leaderboard load error', fakeAsync(() => {
      statisticsServiceMock.getLeaderboard.mockReturnValue(throwError(() => new Error('Network error')));

      fixture.detectChanges();
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.currentList).toEqual([]);
    }));
  });

  describe('Empty State', () => {
    it('should handle null leaderboard data', () => {
      component.leaderboardData = null;
      component['updateCurrentList']();

      expect(component.currentList).toEqual([]);
    });
  });

  describe('Cleanup', () => {
    it('should complete destroy subject on ngOnDestroy', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      const destroySpy = jest.spyOn(component['destroy$'], 'complete');
      
      fixture.destroy();
      
      expect(destroySpy).toHaveBeenCalled();
    }));
  });
});
