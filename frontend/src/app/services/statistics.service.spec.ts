import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StatisticsService, PlayerStatistics, LeaderboardData } from './statistics.service';
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
    longestWinStreak: 5,
    currentStreak: 2,
    vpipHands: 40,
    pfrHands: 20,
    totalBets: 50,
    totalRaises: 30,
    totalCalls: 80,
    totalFolds: 60,
    totalChecks: 40,
    handsWentToShowdown: 30,
    showdownsWon: 15,
    allInsWon: 5,
    allInsLost: 3,
    totalSessions: 10,
    lastHandPlayed: new Date().toISOString()
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [StatisticsService]
    });
    service = TestBed.inject(StatisticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Player Statistics', () => {
    it('should get player statistics by name', () => {
      service.getPlayerStats('TestPlayer').subscribe(stats => {
        expect(stats).toEqual(mockStats);
        expect(stats.playerName).toBe('TestPlayer');
        expect(stats.handsPlayed).toBe(100);
      });

      const req = httpMock.expectOne(`${apiUrl}/player/TestPlayer`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });

    it('should get player stats summary', () => {
      const mockSummary = {
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
        totalSessions: 10
      };

      service.getPlayerStatsSummary('TestPlayer').subscribe(summary => {
        expect(summary.winRate).toBe(25.0);
        expect(summary.vpip).toBe(40.0);
        expect(summary.pfr).toBe(20.0);
      });

      const req = httpMock.expectOne(`${apiUrl}/player/TestPlayer/summary`);
      expect(req.request.method).toBe('GET');
      req.flush(mockSummary);
    });

    it('should search players by name', () => {
      const mockResults = [mockStats, { ...mockStats, playerName: 'TestPlayer2' }];

      service.searchPlayers('Test').subscribe(results => {
        expect(results.length).toBe(2);
        expect(results[0].playerName).toBe('TestPlayer');
      });

      const req = httpMock.expectOne(`${apiUrl}/search?query=Test`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResults);
    });
  });

  describe('Leaderboards', () => {
    const mockLeaderboard: PlayerStatistics[] = [
      { ...mockStats, playerName: 'Player1', totalWinnings: 10000 },
      { ...mockStats, playerName: 'Player2', totalWinnings: 8000 },
      { ...mockStats, playerName: 'Player3', totalWinnings: 6000 }
    ];

    it('should get leaderboard by winnings', () => {
      service.getLeaderboardByWinnings().subscribe(leaderboard => {
        expect(leaderboard.length).toBe(3);
        expect(leaderboard[0].totalWinnings).toBe(10000);
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/winnings`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard);
    });

    it('should get leaderboard by hands won', () => {
      service.getLeaderboardByHandsWon().subscribe(leaderboard => {
        expect(leaderboard).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/hands-won`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard);
    });

    it('should get leaderboard by win rate', () => {
      service.getLeaderboardByWinRate().subscribe(leaderboard => {
        expect(leaderboard).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/win-rate`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard);
    });

    it('should get leaderboard by biggest pot', () => {
      service.getLeaderboardByBiggestPot().subscribe(leaderboard => {
        expect(leaderboard).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/biggest-pot`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard);
    });

    it('should get leaderboard by win streak', () => {
      service.getLeaderboardByWinStreak().subscribe(leaderboard => {
        expect(leaderboard).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/win-streak`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard);
    });

    it('should get comprehensive leaderboard', () => {
      const mockComprehensive: LeaderboardData = {
        byWinnings: mockLeaderboard,
        byHandsWon: mockLeaderboard,
        byWinRate: mockLeaderboard,
        byBiggestPot: mockLeaderboard,
        byWinStreak: mockLeaderboard,
        mostActive: mockLeaderboard
      };

      service.getComprehensiveLeaderboard().subscribe(data => {
        expect(data.byWinnings.length).toBe(3);
        expect(data.byHandsWon.length).toBe(3);
        expect(data.byWinRate).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard`);
      expect(req.request.method).toBe('GET');
      req.flush(mockComprehensive);
    });

    it('should get most active players', () => {
      service.getMostActivePlayers().subscribe(players => {
        expect(players).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/most-active`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLeaderboard);
    });
  });

  describe('Statistics Calculations', () => {
    it('should calculate win rate correctly', () => {
      const winRate = service.calculateWinRate(mockStats);
      expect(winRate).toBe(25); 
    });

    it('should calculate VPIP correctly', () => {
      const vpip = service.calculateVPIP(mockStats);
      expect(vpip).toBe(40); 
    });

    it('should calculate PFR correctly', () => {
      const pfr = service.calculatePFR(mockStats);
      expect(pfr).toBe(20); 
    });

    it('should calculate aggression factor correctly', () => {
      const af = service.calculateAggressionFactor(mockStats);
      
      expect(af).toBe(1.0);
    });

    it('should calculate WTSD correctly', () => {
      const wtsd = service.calculateWTSD(mockStats);
      expect(wtsd).toBe(30); 
    });

    it('should calculate W$SD correctly', () => {
      const wsd = service.calculateWSD(mockStats);
      expect(wsd).toBe(50); 
    });

    it('should handle zero division in calculations', () => {
      const emptyStats: PlayerStatistics = {
        ...mockStats,
        handsPlayed: 0,
        totalCalls: 0,
        handsWentToShowdown: 0
      };

      expect(service.calculateWinRate(emptyStats)).toBe(0);
      expect(service.calculateAggressionFactor(emptyStats)).toBe(0);
      expect(service.calculateWSD(emptyStats)).toBe(0);
    });

    it('should calculate net profit correctly', () => {
      const profit = service.calculateNetProfit(mockStats);
      expect(profit).toBe(2000); 
    });
  });

  describe('Caching', () => {
    it('should cache player statistics', () => {
      
      service.getPlayerStats('TestPlayer').subscribe();
      const req1 = httpMock.expectOne(`${apiUrl}/player/TestPlayer`);
      req1.flush(mockStats);

      
      service.getPlayerStats('TestPlayer').subscribe(stats => {
        expect(stats.playerName).toBe('TestPlayer');
      });

      
      httpMock.expectNone(`${apiUrl}/player/TestPlayer`);
    });

    it('should invalidate cache on refresh', () => {
      
      service.getPlayerStats('TestPlayer').subscribe();
      const req1 = httpMock.expectOne(`${apiUrl}/player/TestPlayer`);
      req1.flush(mockStats);

      
      service.invalidateCache('TestPlayer');

      
      service.getPlayerStats('TestPlayer').subscribe();
      const req2 = httpMock.expectOne(`${apiUrl}/player/TestPlayer`);
      req2.flush(mockStats);
    });
  });

  describe('Error Handling', () => {
    it('should handle 404 for unknown player', () => {
      let errorResponse: any;

      service.getPlayerStats('UnknownPlayer').subscribe({
        error: (error) => {
          errorResponse = error;
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/player/UnknownPlayer`);
      req.flush('Player not found', { status: 404, statusText: 'Not Found' });

      expect(errorResponse.status).toBe(404);
    });

    it('should handle server errors gracefully', () => {
      let errorResponse: any;

      service.getLeaderboardByWinnings().subscribe({
        error: (error) => {
          errorResponse = error;
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/leaderboard/winnings`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      expect(errorResponse.status).toBe(500);
    });
  });

  describe('Formatting', () => {
    it('should format currency correctly', () => {
      expect(service.formatCurrency(1234)).toBe('$1,234');
      expect(service.formatCurrency(1000000)).toBe('$1,000,000');
      expect(service.formatCurrency(-500)).toBe('-$500');
    });

    it('should format percentage correctly', () => {
      expect(service.formatPercentage(25.5)).toBe('25.5%');
      expect(service.formatPercentage(100)).toBe('100%');
      expect(service.formatPercentage(0)).toBe('0%');
    });

    it('should format large numbers correctly', () => {
      expect(service.formatNumber(1000)).toBe('1K');
      expect(service.formatNumber(1500000)).toBe('1.5M');
      expect(service.formatNumber(500)).toBe('500');
    });
  });
});
