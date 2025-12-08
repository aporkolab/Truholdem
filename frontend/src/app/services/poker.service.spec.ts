import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PokerService, PlayerInfo } from './poker.service';
import { Game } from '../model/game';
import { environment } from '../../environments/environment';

describe('PokerService', () => {
  let service: PokerService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl + '/poker';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PokerService]
    });
    service = TestBed.inject(PokerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Game Creation', () => {
    it('should create a new game with provided players', () => {
      const players: PlayerInfo[] = [
        { name: 'Player1', startingChips: 1000, isBot: false },
        { name: 'Bot1', startingChips: 1000, isBot: true }
      ];

      const mockGame: Game = {
        id: '123',
        currentPot: 30,
        players: [],
        communityCards: [],
        phase: 'PRE_FLOP',
        currentBet: 20,
        playerActions: {}
      };

      service.startGame(players).subscribe(game => {
        expect(game).toEqual(mockGame);
      });

      const req = httpMock.expectOne(`${apiUrl}/start`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(players);
      req.flush(mockGame);
    });

    it('should create a game with default players when none provided', () => {
      const mockGame: Game = {
        id: '123',
        currentPot: 30,
        players: [],
        communityCards: [],
        phase: 'PRE_FLOP',
        currentBet: 20,
        playerActions: {}
      };

      service.startGame().subscribe(game => {
        expect(game).toBeTruthy();
      });

      const req = httpMock.expectOne(`${apiUrl}/start`);
      expect(req.request.body.length).toBe(3); 
      req.flush(mockGame);
    });
  });

  describe('Player Actions', () => {
    it('should perform fold action', () => {
      const playerId = 'player-123';

      service.fold(playerId).subscribe(response => {
        expect(response).toBe('Fold successful');
      });

      const foldReq = httpMock.expectOne(`${apiUrl}/fold?playerId=${playerId}`);
      expect(foldReq.request.method).toBe('POST');
      foldReq.flush('Fold successful');

      
      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });

    it('should perform check action', () => {
      const playerId = 'player-123';

      service.check(playerId).subscribe(response => {
        expect(response).toBe('Check successful');
      });

      const checkReq = httpMock.expectOne(`${apiUrl}/check?playerId=${playerId}`);
      expect(checkReq.request.method).toBe('POST');
      checkReq.flush('Check successful');

      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });

    it('should perform bet action', () => {
      const playerId = 'player-123';
      const amount = 50;

      service.bet(playerId, amount).subscribe(response => {
        expect(response).toBe('Bet successful');
      });

      const betReq = httpMock.expectOne(`${apiUrl}/bet`);
      expect(betReq.request.method).toBe('POST');
      expect(betReq.request.body).toEqual({ playerId, amount });
      betReq.flush('Bet successful');

      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });

    it('should perform raise action', () => {
      const playerId = 'player-123';
      const amount = 100;

      service.raise(playerId, amount).subscribe(response => {
        expect(response).toBe('Raise successful');
      });

      const raiseReq = httpMock.expectOne(`${apiUrl}/raise`);
      expect(raiseReq.request.method).toBe('POST');
      expect(raiseReq.request.body).toEqual({ playerId, amount });
      raiseReq.flush('Raise successful');

      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });
  });

  describe('Game Status', () => {
    it('should get current game status', () => {
      const mockGame: Game = {
        id: '123',
        currentPot: 100,
        players: [],
        communityCards: [],
        phase: 'FLOP',
        currentBet: 40,
        playerActions: {}
      };

      service.getGameStatus().subscribe(game => {
        expect(game).toEqual(mockGame);
      });

      const req = httpMock.expectOne(`${apiUrl}/status`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGame);
    });

    it('should update game subject on status refresh', () => {
      const mockGame: Game = {
        id: '123',
        currentPot: 100,
        players: [],
        communityCards: [],
        phase: 'FLOP',
        currentBet: 40,
        playerActions: {}
      };

      let receivedGame: Game | null = null;
      service.game$.subscribe(game => {
        receivedGame = game;
      });

      service.getGameStatus().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/status`);
      req.flush(mockGame);

      expect(receivedGame).toEqual(mockGame);
    });
  });

  describe('Bot Actions', () => {
    it('should execute bot action', () => {
      const botId = 'bot-123';
      const mockResult = { message: 'Bot action executed successfully' };

      service.executeBotAction(botId).subscribe(result => {
        expect(result.message).toBe('Bot action executed successfully');
      });

      const req = httpMock.expectOne(`${apiUrl}/bot-action/${botId}`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResult);

      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });
  });

  describe('Helper Methods', () => {
    it('should calculate minimum raise amount', () => {
      const game: Game = {
        currentPot: 100,
        players: [],
        communityCards: [],
        phase: 'FLOP',
        currentBet: 40,
        minRaiseAmount: 20,
        playerActions: {}
      };

      const minRaise = service.getMinRaiseAmount(game);
      expect(minRaise).toBe(60); 
    });

    it('should calculate call amount for player', () => {
      const game: Game = {
        currentPot: 100,
        players: [],
        communityCards: [],
        phase: 'FLOP',
        currentBet: 40,
        playerActions: {}
      };

      const player = {
        id: '123',
        name: 'Player1',
        hand: [],
        chips: 1000,
        betAmount: 10,
        totalBetInRound: 10,
        folded: false,
        isBot: false,
        isAllIn: false,
        hasActed: false,
        seatPosition: 0,
        canAct: () => true,
        getDisplayName: () => 'Player1',
        isHuman: () => true,
        getStatusText: () => ''
      };

      const callAmount = service.getCallAmount(game, player as any);
      expect(callAmount).toBe(30); 
    });

    it('should check if player can check', () => {
      const game: Game = {
        currentPot: 100,
        players: [],
        communityCards: [],
        phase: 'FLOP',
        currentBet: 40,
        playerActions: {}
      };

      const playerCanCheck = {
        id: '123',
        name: 'Player1',
        hand: [],
        chips: 1000,
        betAmount: 40,
        folded: false,
        isBot: false
      };

      const playerCannotCheck = {
        id: '456',
        name: 'Player2',
        hand: [],
        chips: 1000,
        betAmount: 20,
        folded: false,
        isBot: false
      };

      expect(service.canCheck(game, playerCanCheck as any)).toBe(true);
      expect(service.canCheck(game, playerCannotCheck as any)).toBe(false);
    });

    it('should get phase display name', () => {
      expect(service.getPhaseDisplayName('PRE_FLOP')).toBe('Pre-Flop');
      expect(service.getPhaseDisplayName('FLOP')).toBe('Flop');
      expect(service.getPhaseDisplayName('TURN')).toBe('Turn');
      expect(service.getPhaseDisplayName('RIVER')).toBe('River');
      expect(service.getPhaseDisplayName('SHOWDOWN')).toBe('Showdown');
    });
  });

  describe('Error Handling', () => {
    it('should handle HTTP errors gracefully', () => {
      let errorMessage: string | null = null;
      service.error$.subscribe(error => {
        errorMessage = error;
      });

      service.getGameStatus().subscribe({
        error: () => {
          
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/status`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      expect(errorMessage).toBeTruthy();
    });
  });
});
