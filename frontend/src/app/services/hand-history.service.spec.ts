import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import {
  HandHistoryService,
  HandHistory,
  ReplayData,
  ReplayAction,
} from './hand-history.service';
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
        holeCard2Value: 'ACE',
      },
      {
        playerId: 'player-2',
        playerName: 'Bob',
        startingChips: 1000,
        seatPosition: 1,
        holeCard1Suit: 'CLUBS',
        holeCard1Value: 'KING',
        holeCard2Suit: 'SPADES',
        holeCard2Value: 'QUEEN',
      },
    ],
    actions: [
      {
        playerId: 'player-1',
        playerName: 'Alice',
        action: 'RAISE',
        amount: 60,
        phase: 'PRE_FLOP',
        timestamp: new Date().toISOString(),
      },
      {
        playerId: 'player-2',
        playerName: 'Bob',
        action: 'CALL',
        amount: 60,
        phase: 'PRE_FLOP',
        timestamp: new Date().toISOString(),
      },
    ],
    board: [
      { suit: 'HEARTS', value: 'TEN' },
      { suit: 'CLUBS', value: 'JACK' },
      { suit: 'DIAMONDS', value: 'TWO' },
    ],
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
        holeCard2: 'ACE of DIAMONDS',
      },
    ],
    actions: [
      { playerName: 'Alice', action: 'RAISE', amount: 60, phase: 'PRE_FLOP' },
    ],
    board: ['TEN of HEARTS', 'JACK of CLUBS', 'TWO of DIAMONDS'],
    winnerName: 'Alice',
    winningHand: 'Pair of Aces',
    finalPot: 150,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [HandHistoryService],
    });
    service = TestBed.inject(HandHistoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Get Hand History', () => {
    it('should get hand history by ID', () => {
      service.getHandHistory('history-123').subscribe((history) => {
        expect(history).toEqual(mockHandHistory);
        expect(history.winnerName).toBe('Alice');
      });

      const req = httpMock.expectOne(`${apiUrl}/history-123`);
      expect(req.request.method).toBe('GET');
      req.flush(mockHandHistory);
    });

    it('should get all hands for a game', () => {
      const mockHistories = [
        mockHandHistory,
        { ...mockHandHistory, handNumber: 2 },
      ];

      service.getGameHistory('game-456').subscribe((histories) => {
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
        size: 2,
      };

      service.getGameHistoryPaged('game-456', 0, 2).subscribe((page) => {
        expect(page.content.length).toBe(1);
        expect(page.totalElements).toBe(10);
        expect(page.totalPages).toBe(5);
      });

      const req = httpMock.expectOne(
        `${apiUrl}/game/game-456/paged?page=0&size=2`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPage);
    });

    it('should get player hand history', () => {
      service.getPlayerHistory('player-1').subscribe((histories) => {
        expect(histories).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/player/player-1`);
      expect(req.request.method).toBe('GET');
      req.flush([mockHandHistory]);
    });

    it('should get player wins by name', () => {
      service.getPlayerWins('Alice').subscribe((wins) => {
        expect(wins).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/wins/Alice`);
      expect(req.request.method).toBe('GET');
      req.flush([mockHandHistory]);
    });
  });

  describe('Recent and Notable Hands', () => {
    it('should get recent hands', () => {
      const mockRecent = [
        mockHandHistory,
        { ...mockHandHistory, id: 'history-456' },
      ];

      service.getRecentHands().subscribe((hands) => {
        expect(hands.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/recent`);
      expect(req.request.method).toBe('GET');
      req.flush(mockRecent);
    });

    it('should get biggest pots', () => {
      const mockBigPots = [
        { ...mockHandHistory, finalPot: 10000 },
        { ...mockHandHistory, finalPot: 8000 },
      ];

      service.getBiggestPots().subscribe((hands) => {
        expect(hands[0].finalPot).toBe(10000);
        expect(hands[1].finalPot).toBe(8000);
      });

      const req = httpMock.expectOne(`${apiUrl}/biggest-pots`);
      expect(req.request.method).toBe('GET');
      req.flush(mockBigPots);
    });

    it('should get hand count for a game', () => {
      service.getHandCount('game-456').subscribe((count) => {
        expect(count).toBe(25);
      });

      const req = httpMock.expectOne(`${apiUrl}/game/game-456/count`);
      expect(req.request.method).toBe('GET');
      req.flush(25);
    });
  });

  describe('Replay Data', () => {
    it('should get replay data for a hand', () => {
      service.getReplayData('history-123').subscribe((replay) => {
        expect(replay).toEqual(mockReplayData);
        expect(replay.winnerName).toBe('Alice');
        expect(replay.actions.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      expect(req.request.method).toBe('GET');
      req.flush(mockReplayData);
    });

    it('should update replay subject when getting replay data', (done) => {
      service.getReplayData('history-123').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(mockReplayData);

      service.currentReplay$.subscribe((replay) => {
        if (replay) {
          expect(replay.winnerName).toBe('Alice');
          done();
        }
      });
    });
  });

  describe('Replay State Management', () => {
    it('should navigate to next action', (done) => {

      service.getReplayData('history-123').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(mockReplayData);


      service.nextAction();

      service.replayIndex$.subscribe((index) => {
        if (index === 1) {
          expect(index).toBe(1);
          done();
        }
      });
    });

    it('should navigate to previous action', (done) => {

      service.getReplayData('history-123').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(mockReplayData);


      service.nextAction();
      service.previousAction();

      service.replayIndex$.subscribe((index) => {
        expect(index).toBe(0);
        done();
      });
    });

    it('should go to specific action', (done) => {
      service.getReplayData('history-123').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(mockReplayData);

      service.goToAction(1);

      service.replayIndex$.subscribe((index) => {
        if (index === 1) {
          expect(index).toBe(1);
          done();
        }
      });
    });

    it('should reset replay position', (done) => {
      service.getReplayData('history-123').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(mockReplayData);

      service.nextAction();
      service.resetReplay();

      service.replayIndex$.subscribe((index) => {
        expect(index).toBe(0);
        done();
      });
    });

    it('should clear replay state', (done) => {
      service.getReplayData('history-123').subscribe();
      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(mockReplayData);

      service.clearReplay();

      service.currentReplay$.subscribe((replay) => {
        expect(replay).toBeNull();
        done();
      });
    });
  });

  describe('Formatting Helpers', () => {
    it('should format action correctly', () => {
      const foldAction: ReplayAction = {
        playerName: 'Alice',
        action: 'FOLD',
        amount: 0,
        phase: 'PRE_FLOP',
      };
      expect(service.formatAction(foldAction)).toBe('Alice folds');

      const callAction: ReplayAction = {
        playerName: 'Bob',
        action: 'CALL',
        amount: 50,
        phase: 'PRE_FLOP',
      };
      expect(service.formatAction(callAction)).toBe('Bob calls $50');

      const raiseAction: ReplayAction = {
        playerName: 'Charlie',
        action: 'RAISE',
        amount: 100,
        phase: 'FLOP',
      };
      expect(service.formatAction(raiseAction)).toBe('Charlie raises to $100');

      const checkAction: ReplayAction = {
        playerName: 'Dave',
        action: 'CHECK',
        amount: 0,
        phase: 'TURN',
      };
      expect(service.formatAction(checkAction)).toBe('Dave checks');

      const betAction: ReplayAction = {
        playerName: 'Eve',
        action: 'BET',
        amount: 75,
        phase: 'RIVER',
      };
      expect(service.formatAction(betAction)).toBe('Eve bets $75');
    });

    it('should format phase correctly', () => {
      expect(service.formatPhase('PRE_FLOP')).toBe('Pre-Flop');
      expect(service.formatPhase('FLOP')).toBe('Flop');
      expect(service.formatPhase('TURN')).toBe('Turn');
      expect(service.formatPhase('RIVER')).toBe('River');
      expect(service.formatPhase('SHOWDOWN')).toBe('Showdown');
    });

    it('should format card string', () => {
      const card = service.formatCard('ACE of HEARTS');
      expect(card.value).toBe('ACE');
      expect(card.suit).toBe('HEARTS');
      expect(card.symbol).toBe('A♥');

      const kingCard = service.formatCard('KING of SPADES');
      expect(kingCard.symbol).toBe('K♠');

      const tenCard = service.formatCard('TEN of DIAMONDS');
      expect(tenCard.symbol).toBe('10♦');
    });
  });

  describe('Delete Operations', () => {
    it('should delete game history', () => {
      service.deleteGameHistory('game-456').subscribe();

      const req = httpMock.expectOne(`${apiUrl}/game/game-456`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('Error Handling', () => {
    it('should handle 404 for non-existent history', () => {
      let errorResponse: unknown;

      service.getHandHistory('non-existent').subscribe({
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/non-existent`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });

      expect(errorResponse).toBeTruthy();
    });

    it('should return empty array on game history error', () => {
      service.getGameHistory('invalid').subscribe((histories) => {
        expect(histories).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/game/invalid`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
    });

    it('should return empty array on player history error', () => {
      service.getPlayerHistory('invalid').subscribe((histories) => {
        expect(histories).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/player/invalid`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
    });

    it('should return empty array on recent hands error', () => {
      service.getRecentHands().subscribe((hands) => {
        expect(hands).toEqual([]);
      });

      const req = httpMock.expectOne(`${apiUrl}/recent`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
    });

    it('should return 0 on hand count error', () => {
      service.getHandCount('invalid').subscribe((count) => {
        expect(count).toBe(0);
      });

      const req = httpMock.expectOne(`${apiUrl}/game/invalid/count`);
      req.flush('Error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('Get Current Replay State', () => {
    it('should return null when no replay is active', () => {
      const state = service.getCurrentReplayState();
      expect(state).toBeNull();
    });

    it('should return state after loading replay', (done) => {
      service.getReplayData('history-123').subscribe(() => {
        const state = service.getCurrentReplayState();
        expect(state).toBeTruthy();
        if (state) {
          expect(state.players.length).toBeGreaterThan(0);
          expect(state.pot).toBe(0);
          expect(state.actionIndex).toBe(0);
          expect(state.isComplete).toBe(false);
        }
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/history-123/replay`);
      req.flush(mockReplayData);
    });
  });
});
