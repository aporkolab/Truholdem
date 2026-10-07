import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  HandHistoryEntry,
  LeaderboardEntry,
  PlayerStatistics,
  StatisticsStore,
  StatisticsStoreState
} from './statistics.store';

describe('StatisticsStore HTTP effects', () => {
  let store: StatisticsStore;
  let http: HttpTestingController;
  const apiUrl = environment.apiUrl;

  const stats: PlayerStatistics = {
    id: 'stats-1', playerId: 'player-1', playerName: 'Alice',
    totalGames: 5, totalHands: 20, handsWon: 4, handsLost: 16,
    totalWinnings: 800, totalLosses: 300, biggestPotWon: 350,
    vpip: 25, pfr: 15, aggression: 2, winRate: 2.5, showdownWinRate: 50
  };
  const leaderboard: LeaderboardEntry[] = [{
    rank: 1, playerId: stats.playerId, playerName: stats.playerName,
    totalWinnings: stats.totalWinnings, handsWon: stats.handsWon, winRate: stats.winRate
  }];
  const hand: HandHistoryEntry = {
    id: 'hand-1', handNumber: 1, players: [], communityCards: [],
    potSize: 100, winnerId: stats.playerId, winnerName: stats.playerName,
    winningHand: 'Pair', timestamp: '2026-10-07T10:00:00Z', actions: []
  };
  const previousHand: HandHistoryEntry = { ...hand, id: 'hand-0', handNumber: 0 };

  interface Scenario {
    name: string;
    prepare?: () => void;
    invoke: () => void;
    url: string;
    params?: Record<string, string>;
    response: object;
    expected: Partial<StatisticsStoreState>;
  }

  const scenarios: Scenario[] = [
    {
      name: 'player statistics',
      invoke: () => store.loadPlayerStats(stats.playerId),
      url: '/statistics/player/player-1',
      response: stats,
      expected: { playerStats: stats }
    },
    {
      name: 'all player statistics',
      invoke: () => store.loadAllPlayersStats(),
      url: '/statistics/all',
      response: [stats],
      expected: { allPlayersStats: [stats], isLoading: false }
    },
    {
      name: 'leaderboard',
      invoke: () => store.loadLeaderboard(),
      url: '/statistics/leaderboard',
      params: { limit: '10' },
      response: leaderboard,
      expected: { leaderboard, isLoading: false }
    },
    {
      name: 'first history page',
      invoke: () => store.loadHandHistory({ playerId: stats.playerId }),
      url: '/hand-history/player/player-1',
      params: { page: '0', size: '20' },
      response: { content: [hand], number: 0, totalPages: 2 },
      expected: { handHistory: [hand], currentPage: 0, totalPages: 2, isLoading: false }
    },
    {
      name: 'next history page',
      prepare: () => {
        store.setHandHistory([previousHand]);
        store.setPagination({ currentPage: 0, totalPages: 2 });
      },
      invoke: () => store.loadMoreHistory(stats.playerId),
      url: '/hand-history/player/player-1',
      params: { page: '1', size: '20' },
      response: { content: [hand], number: 1, totalPages: 2 },
      expected: {
        handHistory: [previousHand, hand], currentPage: 1, totalPages: 2, isLoading: false
      }
    },
    {
      name: 'hand details',
      invoke: () => store.loadHandDetails(hand.id),
      url: '/hand-history/hand-1',
      response: hand,
      expected: { selectedHand: hand, isLoading: false }
    }
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [StatisticsStore]
    });
    store = TestBed.inject(StatisticsStore);
    http = TestBed.inject(HttpTestingController);
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
  });

  afterEach(() => {
    http.verify();
    store.ngOnDestroy();
    jest.restoreAllMocks();
  });

  for (const scenario of scenarios) {
    it(`loads ${scenario.name} using the expected HTTP contract`, async () => {
      scenario.prepare?.();
      scenario.invoke();
      expect(await firstValueFrom(store.isLoading$)).toBe(true);

      const request = http.expectOne(req => req.url === `${apiUrl}${scenario.url}`);
      expect(request.request.method).toBe('GET');
      expect(request.request.params.keys().sort()).toEqual(Object.keys(scenario.params ?? {}).sort());
      for (const [key, value] of Object.entries(scenario.params ?? {})) {
        expect(request.request.params.get(key)).toBe(value);
      }
      request.flush(scenario.response);

      expect(await firstValueFrom(store.state$)).toMatchObject(scenario.expected);
    });

    it(`keeps the ${scenario.name} effect alive after an HTTP failure`, async () => {
      scenario.prepare?.();
      const before = await firstValueFrom(store.state$);
      scenario.invoke();
      http.expectOne(req => req.url === `${apiUrl}${scenario.url}`)
        .flush({}, { status: 404, statusText: 'Not Found' });

      expect(await firstValueFrom(store.state$)).toEqual({
        ...before, error: 'Statistics not found', isLoading: false
      });

      scenario.invoke();
      http.expectOne(req => req.url === `${apiUrl}${scenario.url}`).flush(scenario.response);
      expect(await firstValueFrom(store.state$)).toMatchObject(scenario.expected);
    });
  }

  it('uses an explicit leaderboard limit and defaults an empty options object to ten', () => {
    store.loadLeaderboard({ limit: 3 });
    http.expectOne(`${apiUrl}/statistics/leaderboard?limit=3`).flush(leaderboard);
    store.loadLeaderboard({});
    http.expectOne(`${apiUrl}/statistics/leaderboard?limit=10`).flush(leaderboard);
  });

  it('requests the selected page and page size, replacing the previous history', async () => {
    store.setHandHistory([previousHand]);
    store.setPageSize(5);
    store.loadHandHistory({ playerId: stats.playerId, page: 2 });
    http.expectOne(`${apiUrl}/hand-history/player/player-1?page=2&size=5`)
      .flush({ content: [hand], number: 2, totalPages: 3 });

    expect(await firstValueFrom(store.state$)).toMatchObject({
      handHistory: [hand], currentPage: 2, totalPages: 3, pageSize: 5
    });
    expect(await firstValueFrom(store.hasNextPage$)).toBe(false);
    expect(await firstValueFrom(store.hasPrevPage$)).toBe(true);
  });

  it('does not request another history page after the last page', async () => {
    store.setPagination({ currentPage: 1, totalPages: 2 });
    store.loadMoreHistory(stats.playerId);
    http.expectNone(req => req.url.includes('/hand-history/'));
    expect(await firstValueFrom(store.isLoading$)).toBe(false);
  });

  it('cancels stale player requests when the selected player changes', async () => {
    store.loadPlayerStats('old-player');
    const stale = http.expectOne(`${apiUrl}/statistics/player/old-player`);
    store.loadPlayerStats(stats.playerId);
    expect(stale.cancelled).toBe(true);
    http.expectOne(`${apiUrl}/statistics/player/player-1`).flush(stats);
    expect(await firstValueFrom(store.playerStats$)).toEqual(stats);
  });

  it('initializes the dashboard with player statistics, leaderboard and first history page', async () => {
    store.initializeDashboard(stats.playerId);
    http.expectOne(`${apiUrl}/statistics/player/player-1`).flush(stats);
    http.expectOne(`${apiUrl}/statistics/leaderboard?limit=10`).flush(leaderboard);
    http.expectOne(`${apiUrl}/hand-history/player/player-1?page=0&size=20`)
      .flush({ content: [hand], number: 0, totalPages: 2 });

    expect(await firstValueFrom(store.vm$)).toMatchObject({
      playerStats: stats, leaderboard, handHistory: [hand], isLoading: false, error: null,
      winRate: '2.50 BB/100', profitLoss: 500, handsPlayed: 20, handsWon: 4,
      winPercentage: '20.0%', avgPotWon: 200, currentPage: 0, totalPages: 2,
      hasNextPage: true, hasPrevPage: false
    });
    expect(await firstValueFrom(store.playerRank$)).toBe(1);
    expect(await firstValueFrom(store.topThree$)).toEqual(leaderboard);
  });

  it('keeps the default view model safe before any statistics are available', async () => {
    expect(await firstValueFrom(store.vm$)).toMatchObject({
      playerStats: null, leaderboard: [], handHistory: [], isLoading: false, error: null,
      winRate: 'N/A', profitLoss: 0, handsPlayed: 0, handsWon: 0,
      winPercentage: '0%', avgPotWon: 0, hasNextPage: false, hasPrevPage: false
    });
    expect(await firstValueFrom(store.playerRank$)).toBeNull();
  });

  it('handles a player without played hands or a leaderboard entry', async () => {
    store.setPlayerStats({ ...stats, totalHands: 0, handsWon: 0, totalWinnings: 0 });
    expect(await firstValueFrom(store.winPercentage$)).toBe('0%');
    expect(await firstValueFrom(store.avgPotWon$)).toBe(0);
    expect(await firstValueFrom(store.playerRank$)).toBeNull();
  });

  it('clears a previous player error when a later player request succeeds', async () => {
    store.setError('Previous failure');
    store.loadPlayerStats(stats.playerId);
    http.expectOne(`${apiUrl}/statistics/player/player-1`).flush(stats);
    expect(await firstValueFrom(store.error$)).toBeNull();
  });

  it('preserves the server error message and clears the loading state', async () => {
    store.loadHandDetails(hand.id);
    http.expectOne(`${apiUrl}/hand-history/hand-1`)
      .flush({ message: 'Temporarily unavailable' }, { status: 503, statusText: 'Unavailable' });
    expect(await firstValueFrom(store.error$)).toBe('Temporarily unavailable');
    expect(await firstValueFrom(store.isLoading$)).toBe(false);
  });

  it('provides a useful error when the server omits its message', async () => {
    store.loadAllPlayersStats();
    http.expectOne(`${apiUrl}/statistics/all`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    expect(await firstValueFrom(store.error$)).toContain('Error Code: 500');
  });

  it('reports client errors without terminating the effect', async () => {
    store.loadLeaderboard();
    http.expectOne(`${apiUrl}/statistics/leaderboard?limit=10`)
      .error(new ErrorEvent('error', { message: 'Connection lost' }));
    expect(await firstValueFrom(store.error$)).toBe('Error: Connection lost');

    store.loadLeaderboard();
    http.expectOne(`${apiUrl}/statistics/leaderboard?limit=10`).flush(leaderboard);
    expect(await firstValueFrom(store.leaderboard$)).toEqual(leaderboard);
  });

  it('resets cached statistics, selection, errors and pagination', async () => {
    store.setPlayerStats(stats);
    store.setSelectedHand(hand);
    store.setPagination({ currentPage: 2, totalPages: 3 });
    store.setPageSize(5);
    store.setError('Previous failure');
    store.reset();

    expect(await firstValueFrom(store.state$)).toMatchObject({
      playerStats: null, selectedHand: null, error: null,
      currentPage: 0, totalPages: 0, pageSize: 20, isLoading: false
    });
  });
});
