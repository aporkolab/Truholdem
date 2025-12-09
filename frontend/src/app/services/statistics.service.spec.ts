import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import {
  StatisticsService,
  PlayerStatistics,
  LeaderboardData,
  PlayerStatsSummary,
} from './statistics.service';
import { environment } from '../../environments/environment';

describe('StatisticsService', () => {
  let service: StatisticsService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl + '/stats';

  const mockStats: PlayerStatistics = {
    id: 'stat-123',
    playerName: 'TestPlayer',
    handsPlayed: 100,
    handsWon: 25,
    totalWinnings: 5000,
    totalLosses: 3000,
    biggestPotWon: 500,
    currentWinStreak: 2,
    longestWinStreak: 5,
    currentLoseStreak: 0,
    longestLoseStreak: 3,
    totalSessions: 10,
    lastHandPlayed: new Date().toISOString(),
  };

  const mockSummary: PlayerStatsSummary = {
    playerName: 'TestPlayer',
    handsPlayed: 100,
    handsWon: 25,
    winRate: 25.0,
    netProfit: 2000,
    vpip: 40.0,
    pfr: 20.0,
    aggressionFactor: 1.0,
    wtsd: 30.0,
    wonAtShowdown: 50.0,
    biggestPotWon: 500,
    longestWinStreak: 5,
    totalSessions: 10,
  };

  const mockLeaderboard: LeaderboardData = {
    byWinnings: [
      { ...mockStats, playerName: 'Player1', totalWinnings: 10000 },
      { ...mockStats, playerName: 'Player2', totalWinnings: 8000 },
      { ...mockStats, playerName: 'Player3', totalWinnings: 6000 },
    ],
    byHandsWon: [
      { ...mockStats, playerName: 'Player1', handsWon: 50 },
      { ...mockStats, playerName: 'Player2', handsWon: 40 },
    ],
    byWinRate: [
      { ...mockStats, playerName: 'Player1', winRate: 60 },
      { ...mockStats, playerName: 'Player2', winRate: 55 },
    ],
    byBiggestPot: [
      { ...mockStats, playerName: 'Player1', biggestPotWon: 5000 },
    ],
    byWinStreak: [
      { ...mockStats, playerName: 'Player1', longestWinStreak: 10 },
    ],
    mostActive: [{ ...mockStats, playerName: 'Player1', handsPlayed: 500 }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [StatisticsService],
    });
    service = TestBed.inject(StatisticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Player Statistics', () => {
    it('should get player statistics by name', () => {
      service.getPlayerStats('TestPlayer').subscribe((stats) => {
        expect(stats.playerName).toBe('TestPlayer');
        expect(stats.handsPlayed).toBe(100);
      });

      const req = httpMock.expectOne(`${apiUrl}/player/TestPlayer`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });

    it('should get player stats by user ID', () => {
      service.getPlayerStatsByUserId('user-123').subscribe((stats) => {
        expect(stats).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/player/id/user-123`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });

    it('should get player stats summary', () => {
      service.getPlayerStatsSummary('TestPlayer').subscribe((summary) => {
        expect(summary.winRate).toBe(25.0);
        expect(summary.vpip).toBe(40.0);
        expect(summary.pfr).toBe(20.0);
      });

      const req = httpMock.expectOne(`${apiUrl}/player/TestPlayer/summary`);
      expect(req.request.method).toBe('GET');
      req.flush(mockSummary);
    });

    it('should search players by name', () => {
      const mockResults = [
        mockStats,
        { ...mockStats, playerName: 'TestPlayer2' },
      ];

      service.searchPlayers('Test').subscribe((results) => {
        expect(results.length).toBe(2);
        expect(results[0].playerName).toBe('TestPlayer');
      });

      const req = httpMock.expectOne(`${apiUrl}/search?query=Test`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResults);
    });

    it('should return empty stats on error', () => {
      service.getPlayerStats('UnknownPlayer').subscribe((stats) => {
        expect(stats.playerName).toBe('UnknownPlayer');
        expect(stats.handsPlayed).toBe(0);
      });

      const req = httpMock.expectOne(`${apiUrl}/player/UnknownPlayer`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('Leaderboards', () => {
    it('should get comprehensive leaderboard', () => {
      service.getLeaderboard().subscribe((data) => {
        expect(data.byWinnings.length).toBe(3);
        expect(data.byHandsWon.length).toBe(2);
        expect(data.byWinRate.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard);
    });

    it('should update leaderboard subject on fetch', (done) => {
      service.getLeaderboard().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/leaderboard`);
      req.flush(mockLeaderboard);

      service.leaderboard$.subscribe((data) => {
        if (data) {
          expect(data.byWinnings.length).toBe(3);
          done();
        }
      });
    });

    it('should get top players by winnings', () => {
      service.getTopByWinnings().subscribe((players) => {
        expect(players.length).toBe(3);
        expect(players[0].totalWinnings).toBe(10000);
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/winnings`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard.byWinnings);
    });

    it('should get top players by hands won', () => {
      service.getTopByHandsWon().subscribe((players) => {
        expect(players).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/hands-won`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard.byHandsWon);
    });

    it('should get top players by win rate', () => {
      service.getTopByWinRate().subscribe((players) => {
        expect(players).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/win-rate`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard.byWinRate);
    });

    it('should get most active players', () => {
      service.getMostActive().subscribe((players) => {
        expect(players).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/most-active`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard.mostActive);
    });

    it('should return empty array on leaderboard error', () => {
      service.getTopByWinnings().subscribe((players) => {
        expect(players).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/winnings`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('Stats Enrichment', () => {
    it('should calculate win rate when fetching stats', () => {
      const statsWithoutWinRate = { ...mockStats, winRate: undefined };

      service.getPlayerStats('TestPlayer').subscribe((stats) => {
        expect(stats.winRate).toBe(25); 
      });

      const req = httpMock.expectOne(`${apiUrl}/player/TestPlayer`);
      req.flush(statsWithoutWinRate);
    });

    it('should calculate net profit when fetching stats', () => {
      const statsWithoutNetProfit = { ...mockStats, netProfit: undefined };

      service.getPlayerStats('TestPlayer').subscribe((stats) => {
        expect(stats.netProfit).toBe(2000); 
      });

      const req = httpMock.expectOne(`${apiUrl}/player/TestPlayer`);
      req.flush(statsWithoutNetProfit);
    });
  });

  describe('Formatting', () => {
    it('should format win rate correctly', () => {
      expect(service.formatWinRate(25.5)).toBe('25.5%');
      expect(service.formatWinRate(100)).toBe('100.0%');
      expect(service.formatWinRate(0)).toBe('0.0%');
    });

    it('should format currency correctly', () => {
      expect(service.formatCurrency(1234)).toBe('$1,234');
      expect(service.formatCurrency(1000000)).toBe('$1,000,000');
    });

    it('should format aggression factor', () => {
      expect(service.formatAggressionFactor(1.5)).toBe('1.50');
      expect(service.formatAggressionFactor(10)).toBe('10+');
      expect(service.formatAggressionFactor(15)).toBe('10+');
    });
  });

  describe('Player Rank', () => {
    it('should assign correct rank based on stats', () => {
      
      expect(service.getPlayerRank({ ...mockStats, handsPlayed: 5 })).toBe(
        'Newcomer'
      );

      
      expect(service.getPlayerRank({ ...mockStats, handsPlayed: 25 })).toBe(
        'Beginner'
      );

      
      expect(service.getPlayerRank({ ...mockStats, handsPlayed: 100 })).toBe(
        'Amateur'
      );

      
      expect(
        service.getPlayerRank({ ...mockStats, handsPlayed: 200, winRate: 65 })
      ).toBe('Pro');

      
      expect(
        service.getPlayerRank({ ...mockStats, handsPlayed: 200, winRate: 55 })
      ).toBe('Regular');

      
      expect(
        service.getPlayerRank({ ...mockStats, handsPlayed: 200, winRate: 30 })
      ).toBe('Fish');
    });
  });

  describe('Play Style Description', () => {
    it('should describe tight-aggressive style', () => {
      const stats = { ...mockStats, vpip: 15, pfr: 12, aggressionFactor: 3 };
      expect(service.getPlayStyleDescription(stats)).toBe('Tight-Aggressive');
    });

    it('should describe loose-passive style', () => {
      const stats = { ...mockStats, vpip: 35, pfr: 10, aggressionFactor: 0.5 };
      expect(service.getPlayStyleDescription(stats)).toBe('Loose-Passive');
    });

    it('should describe normal-balanced style', () => {
      const stats = { ...mockStats, vpip: 25, pfr: 15, aggressionFactor: 1.5 };
      expect(service.getPlayStyleDescription(stats)).toBe('Normal-Balanced');
    });
  });

  describe('Error Handling', () => {
    it('should handle stats summary error', () => {
      let errorCaught = false;

      service.getPlayerStatsSummary('Unknown').subscribe({
        error: () => {
          errorCaught = true;
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/player/Unknown/summary`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });

      expect(errorCaught).toBe(true);
    });

    it('should return empty leaderboard on error', () => {
      service.getLeaderboard().subscribe((data) => {
        expect(data.byWinnings).toEqual([]);
        expect(data.byHandsWon).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
    });

    it('should return empty array on search error', () => {
      service.searchPlayers('test').subscribe((results) => {
        expect(results).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/search?query=test`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
    });
  });
});
