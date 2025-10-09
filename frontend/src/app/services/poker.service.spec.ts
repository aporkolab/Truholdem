import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { PokerService, PlayerInfo } from './poker.service';
import { GameStateService } from './game-state.service';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { environment } from '../../environments/environment';

describe('PokerService', () => {
  let service: PokerService;
  let httpMock: HttpTestingController;
  let gameState: GameStateService;
  const apiUrl = environment.apiUrl + '/poker';

  const createMockGame = (overrides: Partial<Game> = {}): Game => {
    const game = new Game();
    game.id = overrides.id || '123';
    game.currentPot = overrides.currentPot ?? 30;
    game.players = overrides.players || [];
    game.communityCards = overrides.communityCards || [];
    game.phase = overrides.phase || 'PRE_FLOP';
    game.currentBet = overrides.currentBet ?? 20;
    game.currentPlayerIndex = overrides.currentPlayerIndex;
    game.minRaiseAmount = overrides.minRaiseAmount ?? 20;
    game.bigBlind = overrides.bigBlind ?? 20;
    game.smallBlind = overrides.smallBlind ?? 10;
    game.playerActions = overrides.playerActions || {};
    return game;
  };

  const createMockPlayer = (overrides: Partial<Player> = {}): Player => {
    const player = new Player();
    player.id = overrides.id || 'player-1';
    player.name = overrides.name || 'TestPlayer';
    player.chips = overrides.chips ?? 1000;
    player.betAmount = overrides.betAmount ?? 0;
    player.folded = overrides.folded ?? false;
    player.isBot = overrides.isBot ?? false;
    player.isAllIn = overrides.isAllIn ?? false;
    return player;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PokerService, GameStateService],
    });
    service = TestBed.inject(PokerService);
    httpMock = TestBed.inject(HttpTestingController);
    gameState = TestBed.inject(GameStateService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Game Creation', () => {
    it('should create a new game with provided players', () => {
      const players: PlayerInfo[] = [
        { name: 'Player1', startingChips: 1000, isBot: false },
        { name: 'Bot1', startingChips: 1000, isBot: true },
      ];

      const mockGame = createMockGame();

      service.startGame(players).subscribe((game) => {
        expect(game.id).toBe('123');
      });

      const req = httpMock.expectOne(`${apiUrl}/start`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(players);
      req.flush(mockGame);
    });

    it('should create a game with default players when none provided', () => {
      const mockGame = createMockGame();

      service.startGame().subscribe((game) => {
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

      service.fold(playerId).subscribe((response) => {
        expect(response).toBe('Fold successful');
      });

      const foldReq = httpMock.expectOne(
        `${apiUrl}/fold?playerId=${playerId}`
      );
      expect(foldReq.request.method).toBe('POST');
      foldReq.flush('Fold successful');

      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });

    it('should perform check action', () => {
      const playerId = 'player-123';

      service.check(playerId).subscribe((response) => {
        expect(response).toBe('Check successful');
      });

      const checkReq = httpMock.expectOne(
        `${apiUrl}/check?playerId=${playerId}`
      );
      expect(checkReq.request.method).toBe('POST');
      checkReq.flush('Check successful');

      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });

    it('should perform call action', () => {
      const playerId = 'player-123';

      service.call(playerId).subscribe((response) => {
        expect(response).toBe('Call successful');
      });

      const callReq = httpMock.expectOne(
        `${apiUrl}/call?playerId=${playerId}`
      );
      expect(callReq.request.method).toBe('POST');
      callReq.flush('Call successful');

      const statusReq = httpMock.expectOne(`${apiUrl}/status`);
      statusReq.flush({});
    });

    it('should perform bet action', () => {
      const playerId = 'player-123';
      const amount = 50;

      service.bet(playerId, amount).subscribe((response) => {
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

      service.raise(playerId, amount).subscribe((response) => {
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
      const mockGame = createMockGame({ currentPot: 100, phase: 'FLOP', currentBet: 40 });

      service.getGameStatus().subscribe((game) => {
        expect(game.currentPot).toBe(100);
        expect(game.phase).toBe('FLOP');
      });

      const req = httpMock.expectOne(`${apiUrl}/status`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGame);
    });

    it('should update game state on status refresh', () => {
      const mockGame = createMockGame({ currentPot: 100 });

      service.getGameStatus().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/status`);
      req.flush(mockGame);

      const receivedGame = gameState.game();
      expect(receivedGame).toBeTruthy();
      expect(receivedGame!.currentPot).toBe(100);
    });
  });

  describe('New Hand and Reset', () => {
    it('should start a new hand', () => {
      const mockGame = createMockGame({ handNumber: 2 });

      service.startNewHand().subscribe((game) => {
        expect(game.handNumber).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/new-match`);
      expect(req.request.method).toBe('POST');
      req.flush(mockGame);
    });

    it('should reset game', () => {
      service.resetGame().subscribe((response) => {
        expect(response).toBe('Game reset');
      });

      const req = httpMock.expectOne(`${apiUrl}/reset`);
      expect(req.request.method).toBe('POST');
      req.flush('Game reset');
    });

    it('should end game', () => {
      const mockResult = { message: 'Game ended', winnerName: 'Alice' };

      service.endGame().subscribe((result) => {
        expect(result.message).toBe('Game ended');
        expect(result.winnerName).toBe('Alice');
      });

      const req = httpMock.expectOne(`${apiUrl}/end`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResult);
    });
  });

  describe('Bot Actions', () => {
    it('should execute bot action', () => {
      const botId = 'bot-123';
      const mockResult = { message: 'Bot action executed successfully' };

      service.executeBotAction(botId).subscribe((result) => {
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
      const game = createMockGame({ currentBet: 40, minRaiseAmount: 20 });
      const minRaise = service.getMinRaiseAmount(game);
      expect(minRaise).toBe(60);
    });

    it('should return default big blind when no game', () => {
      const minRaise = service.getMinRaiseAmount(null as unknown as Game);
      expect(minRaise).toBe(environment.defaultBigBlind);
    });

    it('should calculate call amount for player', () => {
      const game = createMockGame({ currentBet: 40 });
      const player = createMockPlayer({ betAmount: 10 });
      const callAmount = service.getCallAmount(game, player);
      expect(callAmount).toBe(30);
    });

    it('should return 0 call amount when no game or player', () => {
      expect(service.getCallAmount(null as unknown as Game, createMockPlayer())).toBe(0);
      expect(service.getCallAmount(createMockGame(), null as unknown as Player)).toBe(0);
    });

    it('should check if player can check', () => {
      const game = createMockGame({ currentBet: 40 });

      const playerCanCheck = createMockPlayer({ betAmount: 40 });
      const playerCannotCheck = createMockPlayer({ betAmount: 20 });

      expect(service.canCheck(game, playerCanCheck)).toBe(true);
      expect(service.canCheck(game, playerCannotCheck)).toBe(false);
    });

    it('should return false for canCheck when no game or player', () => {
      expect(service.canCheck(null as unknown as Game, createMockPlayer())).toBe(false);
      expect(service.canCheck(createMockGame(), null as unknown as Player)).toBe(false);
    });

    it('should get phase display name', () => {
      expect(service.getPhaseDisplayName('PRE_FLOP')).toBe('Pre-Flop');
      expect(service.getPhaseDisplayName('FLOP')).toBe('Flop');
      expect(service.getPhaseDisplayName('TURN')).toBe('Turn');
      expect(service.getPhaseDisplayName('RIVER')).toBe('River');
      expect(service.getPhaseDisplayName('SHOWDOWN')).toBe('Showdown');
    });

    it('should return raw phase for unknown phases', () => {
      expect(service.getPhaseDisplayName('UNKNOWN')).toBe('UNKNOWN');
    });

    it('should get bots that can act', () => {
      const game = createMockGame({
        players: [
          createMockPlayer({ name: 'BotOne', isBot: true, chips: 1000, folded: false }),
          createMockPlayer({ name: 'Bot2', isBot: true, chips: 500, folded: true }),
          createMockPlayer({ name: 'Player', isBot: false, chips: 1000, folded: false })
        ]
      });

      const bots = service.getBotsToAct(game);
      expect(bots.length).toBe(1);
      expect(bots[0].name).toBe('BotOne');
    });

    it('should return empty array for bots when no game', () => {
      expect(service.getBotsToAct(null as unknown as Game)).toEqual([]);
    });
  });

  describe('Error Handling', () => {
    it('should handle HTTP errors gracefully', () => {
      service.getGameStatus().subscribe({
        error: () => {
          // Expected error
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/status`);
      req.flush('Server Error', {
        status: 500,
        statusText: 'Internal Server Error',
      });

      const errorMessage = gameState.error();
      expect(errorMessage).toBeTruthy();
    });
  });
});
