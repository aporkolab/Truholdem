import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HandHistoryService, HandHistory, ReplayData } from './hand-history.service';
import { environment } from '../../environments/environment';

describe('HandHistoryService', () => {
  let service: HandHistoryService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl + '/history';

  const mockHandHistory: HandHistory = {
    id: 'history-123',
    gameId: 'game-456',
    handNumber: 1,
    playedAt: new Date().toISOString(),
    smallBlind: 10,
    bigBlind: 20,
    dealerPosition: 0,
    winnerName: 'Alice',
    winningHandDescription: 'Pair of Aces',
    finalPot: 150,
    players: [
      {
        playerId: 'player-1',
        playerName: 'Alice',
        startingChips: 1000,
        seatPosition: 0,
        holeCard1Suit: 'HEARTS',
        holeCard1Value: 'ACE',
        holeCard2Suit: 'DIAMONDS',
        holeCard2Value: 'ACE'
      },
      {
        playerId: 'player-2',
        playerName: 'Bob',
        startingChips: 1000,
        seatPosition: 1,
        holeCard1Suit: 'CLUBS',
        holeCard1Value: 'KING',
        holeCard2Suit: 'SPADES',
        holeCard2Value: 'QUEEN'
      }
    ],
    actions: [
      { playerName: 'Alice', action: 'RAISE', amount: 60, phase: 'PRE_FLOP' },
      { playerName: 'Bob', action: 'CALL', amount: 60, phase: 'PRE_FLOP' }
    ],
    board: [
      { suit: 'HEARTS', value: 'TEN' },
      { suit: 'CLUBS', value: 'JACK' },
      { suit: 'DIAMONDS', value: 'TWO' }
    ]
  };

  const mockReplayData: ReplayData = {
    id: 'history-123',
    handNumber: 1,
    smallBlind: 10,
    bigBlind: 20,
    dealerPosition: 0,
    players: [
      {
        id: 'player-1',
        name: 'Alice',
        startingChips: 1000,
        seatPosition: 0,
        holeCard1: 'ACE of HEARTS',
        holeCard2: 'ACE of DIAMONDS'
      }
    ],
    actions: [
      { playerName: 'Alice', action: 'RAISE', amount: 60, phase: 'PRE_FLOP' }
    ],
    board: ['TEN of HEARTS', 'JACK of CLUBS', 'TWO of DIAMONDS'],
    winnerName: 'Alice',
    winningHand: 'Pair of Aces',
    finalPot: 150
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [HandHistoryService]
    });
    service = TestBed.inject(HandHistoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Get Hand History', () => {
    it('should get hand history by ID', () => {
      service.getHandHistory('history-123').subscribe(history => {
        expect(history).toEqual(mockHandHistory);
        expect(history.winnerName).toBe('Alice');
      });

      const req = httpMock.expectOne(`${apiUrl}/history-123`);
      expect(req.request.method).toBe('GET');
      req.flush(mockHandHistory);
    });

    it('should get all hands for a game', () => {
      const mockHistories = [mockHandHistory, { ...mockHandHistory, handNumber: 2 }];

      service.getGameHistory('game-456').subscribe(histories => {
        expect(histories.length).toBe(2);
        expect(histories[0].handNumber).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/game/game-456`);
      expect(req.request.method).toBe('GET');
      req.flush(mockHistories);
    });

    it('should get paged game history', () => {
      const mockPage = {
        content: [mockHandHistory],
        totalElements: 10,
        totalPages: 5,
        number: 0,
        size: 2
      };

      service.getGameHistoryPaged('game-456', 0, 2).subscribe(page => {
        expect(page.content.length).toBe(1);
        expect(page.totalElements).toBe(10);
        expect(page.totalPages).toBe(5);
      });

      const req = httpMock.expectOne(`${apiUrl}/game/game-456/paged?page=0&size=2`);
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });

    it('should get player hand history', () => {
      service.getPlayerHistory('player-1').subscribe(histories => {
        expect(histories).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/player/player-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockHandHistory]);
    });

    it('should get player wins by name', () => {
      service.getPlayerWins('Alice').subscribe(wins => {
        expect(wins).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/wins/Alice`);
      expect(req.request.method).toBe('GET');
      req.flush([mockHandHistory]);
    });
  });

  describe('Recent and Notable Hands', () => {
    it('should get recent hands', () => {
      const mockRecent = [mockHandHistory, { ...mockHandHistory, id: 'history-456' }];

      service.getRecentHands().subscribe(hands => {
        expect(hands.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/recent`);
      expect(req.request.method).toBe('GET');
      req.flush(mockRecent);
    });

    it('should get biggest pots', () => {
      const mockBigPots = [
        { ...mockHandHistory, finalPot: 10000 },
        { ...mockHandHistory, finalPot: 8000 }
      ];

      service.getBiggestPots().subscribe(hands => {
        expect(hands[0].finalPot).toBe(10000);
        expect(hands[1].finalPot).toBe(8000);
      });

      const req = httpMock.expectOne(`${apiUrl}/biggest-pots`);
      expect(req.request.method).toBe('GET');
      req.flush(mockBigPots);
    });

    it('should get hand count for a game', () => {
      service.getHandCount('game-456').subscribe(count => {
        expect(count).toBe(25);
      });

      const req = httpMock.expectOne(`${apiUrl}/game/game-456/count`);
      expect(req.request.method).toBe('GET');
      req.flush(25);
    });
  });

  describe('Replay Data', () => {
    it('should get replay data for a hand', () => {
      service.getReplayData('history-123').subscribe(replay => {
        expect(replay).toEqual(mockReplayData);
        expect(replay.winnerName).toBe('Alice');
        expect(replay.actions.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      expect(req.request.method).toBe('GET');
      req.flush(mockReplayData);
    });

    it('should handle replay data with full board', () => {
      const fullBoardReplay = {
        ...mockReplayData,
        board: [
          'TEN of HEARTS',
          'JACK of CLUBS',
          'TWO of DIAMONDS',
          'FIVE of SPADES',
          'ACE of CLUBS'
        ]
      };

      service.getReplayData('history-123').subscribe(replay => {
        expect(replay.board.length).toBe(5);
      });

      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(fullBoardReplay);
    });
  });

  describe('Delete Operations', () => {
    it('should delete game history', () => {
      service.deleteGameHistory('game-456').subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/game/game-456`);
      expect(req.request.method).toBe('DELETE');
      req.flush({ success: true });
    });
  });

  describe('Formatting Helpers', () => {
    it('should format card correctly', () => {
      expect(service.formatCard('HEARTS', 'ACE')).toBe('A♥');
      expect(service.formatCard('SPADES', 'KING')).toBe('K♠');
      expect(service.formatCard('DIAMONDS', 'QUEEN')).toBe('Q♦');
      expect(service.formatCard('CLUBS', 'TEN')).toBe('10♣');
    });

    it('should format action correctly', () => {
      expect(service.formatAction('FOLD', 0)).toBe('Fold');
      expect(service.formatAction('CALL', 50)).toBe('Call $50');
      expect(service.formatAction('RAISE', 100)).toBe('Raise to $100');
      expect(service.formatAction('CHECK', 0)).toBe('Check');
      expect(service.formatAction('BET', 75)).toBe('Bet $75');
      expect(service.formatAction('ALL_IN', 500)).toBe('All-In $500');
    });

    it('should format phase correctly', () => {
      expect(service.formatPhase('PRE_FLOP')).toBe('Pre-Flop');
      expect(service.formatPhase('FLOP')).toBe('Flop');
      expect(service.formatPhase('TURN')).toBe('Turn');
      expect(service.formatPhase('RIVER')).toBe('River');
      expect(service.formatPhase('SHOWDOWN')).toBe('Showdown');
    });

    it('should get card color class', () => {
      expect(service.getCardColorClass('HEARTS')).toBe('red');
      expect(service.getCardColorClass('DIAMONDS')).toBe('red');
      expect(service.getCardColorClass('SPADES')).toBe('black');
      expect(service.getCardColorClass('CLUBS')).toBe('black');
    });
  });

  describe('Replay State Management', () => {
    it('should track current replay position', () => {
      service.setReplayPosition(5);
      expect(service.getReplayPosition()).toBe(5);
    });

    it('should advance replay position', () => {
      service.setReplayPosition(0);
      service.advanceReplay();
      expect(service.getReplayPosition()).toBe(1);
    });

    it('should rewind replay position', () => {
      service.setReplayPosition(5);
      service.rewindReplay();
      expect(service.getReplayPosition()).toBe(4);
    });

    it('should not rewind below zero', () => {
      service.setReplayPosition(0);
      service.rewindReplay();
      expect(service.getReplayPosition()).toBe(0);
    });

    it('should reset replay position', () => {
      service.setReplayPosition(10);
      service.resetReplay();
      expect(service.getReplayPosition()).toBe(0);
    });
  });

  describe('Error Handling', () => {
    it('should handle 404 for non-existent history', () => {
      let errorResponse: any;

      service.getHandHistory('non-existent').subscribe({
        error: (error) => {
          errorResponse = error;
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/non-existent`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });

      expect(errorResponse.status).toBe(404);
    });

    it('should handle server errors', () => {
      let errorResponse: any;

      service.getRecentHands().subscribe({
        error: (error) => {
          errorResponse = error;
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/recent`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      expect(errorResponse.status).toBe(500);
    });
  });

  describe('Hand Analysis', () => {
    it('should identify winning hand type', () => {
      expect(service.getHandTypeRank('Royal Flush')).toBe(10);
      expect(service.getHandTypeRank('Straight Flush')).toBe(9);
      expect(service.getHandTypeRank('Four of a Kind')).toBe(8);
      expect(service.getHandTypeRank('Full House')).toBe(7);
      expect(service.getHandTypeRank('Flush')).toBe(6);
      expect(service.getHandTypeRank('Straight')).toBe(5);
      expect(service.getHandTypeRank('Three of a Kind')).toBe(4);
      expect(service.getHandTypeRank('Two Pair')).toBe(3);
      expect(service.getHandTypeRank('Pair')).toBe(2);
      expect(service.getHandTypeRank('High Card')).toBe(1);
    });

    it('should calculate pot odds from actions', () => {
      const actions = [
        { playerName: 'Alice', action: 'RAISE', amount: 60, phase: 'PRE_FLOP' },
        { playerName: 'Bob', action: 'CALL', amount: 60, phase: 'PRE_FLOP' }
      ];

      const totalPot = service.calculateTotalFromActions(actions);
      expect(totalPot).toBe(120);
    });
  });
});
