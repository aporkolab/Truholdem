import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  GameStore,
  PlayerInfo,
  PlayerActionRecord,
  GameViewModel
} from './game.store';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { environment } from '../../environments/environment';

describe('GameStore', () => {
  let store: GameStore;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/poker`;





  const createMockPlayer = (overrides: Partial<Player> = {}): Player => ({
    id: overrides.id || 'player-1',
    name: overrides.name || 'TestPlayer',
    chips: overrides.chips ?? 1000,
    betAmount: overrides.betAmount ?? 0,
    totalBetInRound: overrides.totalBetInRound ?? 0,
    folded: overrides.folded ?? false,
    isBot: overrides.isBot ?? false,
    isAllIn: overrides.isAllIn ?? false,
    hasActed: overrides.hasActed ?? false,
    seatPosition: overrides.seatPosition ?? 0,
    hand: overrides.hand || []
  } as Player);

  const createMockGame = (overrides: Partial<Game> = {}): Game => ({
    id: overrides.id || 'game-123',
    currentPot: overrides.currentPot ?? 100,
    players: overrides.players || [
      createMockPlayer({ id: 'human-1', name: 'Alice', chips: 980, betAmount: 20 }),
      createMockPlayer({ id: 'bot-1', name: 'Bot1', chips: 970, betAmount: 10, isBot: true }),
      createMockPlayer({ id: 'bot-2', name: 'Bot2', chips: 990, betAmount: 0, isBot: true }),
    ],
    communityCards: overrides.communityCards || [],
    phase: overrides.phase || 'PRE_FLOP',
    currentBet: overrides.currentBet ?? 20,
    currentPlayerIndex: overrides.currentPlayerIndex ?? 0,
    dealerPosition: overrides.dealerPosition ?? 2,
    minRaiseAmount: overrides.minRaiseAmount ?? 20,
    bigBlind: overrides.bigBlind ?? 20,
    smallBlind: overrides.smallBlind ?? 10,
    isFinished: overrides.isFinished ?? false,
    handNumber: overrides.handNumber ?? 1,
    winnerName: overrides.winnerName,
    winningHandDescription: overrides.winningHandDescription
  } as Game);

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GameStore]
    });

    store = TestBed.inject(GameStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    store.ngOnDestroy();
  });





  describe('Initialization', () => {
    it('should be created', () => {
      expect(store).toBeTruthy();
    });

    it('should initialize with correct default state', (done) => {
      store.game$.subscribe(game => {
        expect(game).toBeNull();
        done();
      });
    });

    it('should start with loading false', (done) => {
      store.isLoading$.subscribe(loading => {
        expect(loading).toBe(false);
        done();
      });
    });

    it('should start with no error', (done) => {
      store.error$.subscribe(error => {
        expect(error).toBeNull();
        done();
      });
    });

    it('should start with disconnected status', (done) => {
      store.connectionStatus$.subscribe(status => {
        expect(status).toBe('disconnected');
        done();
      });
    });
  });





  describe('Updaters', () => {
    describe('setGame', () => {
      it('should update game state', (done) => {
        const game = createMockGame();
        store.setGame(game);

        store.game$.subscribe(g => {
          expect(g).toEqual(game);
          done();
        });
      });

      it('should clear error when setting game', (done) => {
        store.setError('Previous error');
        store.setGame(createMockGame());

        store.error$.subscribe(error => {
          expect(error).toBeNull();
          done();
        });
      });

      it('should set loading to false', (done) => {
        store.setLoading(true);
        store.setGame(createMockGame());

        store.isLoading$.subscribe(loading => {
          expect(loading).toBe(false);
          done();
        });
      });
    });

    describe('setLoading', () => {
      it('should set loading to true', (done) => {
        store.setLoading(true);
        store.isLoading$.subscribe(loading => {
          expect(loading).toBe(true);
          done();
        });
      });

      it('should set loading to false', (done) => {
        store.setLoading(true);
        store.setLoading(false);
        store.isLoading$.subscribe(loading => {
          expect(loading).toBe(false);
          done();
        });
      });
    });

    describe('setError', () => {
      it('should set error message', (done) => {
        store.setError('Test error');
        store.error$.subscribe(error => {
          expect(error).toBe('Test error');
          done();
        });
      });

      it('should set loading to false', (done) => {
        store.setLoading(true);
        store.setError('Error');
        store.isLoading$.subscribe(loading => {
          expect(loading).toBe(false);
          done();
        });
      });
    });

    describe('clearError', () => {
      it('should clear error', (done) => {
        store.setError('Test error');
        store.clearError();
        store.error$.subscribe(error => {
          expect(error).toBeNull();
          done();
        });
      });
    });

    describe('recordAction', () => {
      it('should record player action', (done) => {
        const action: PlayerActionRecord = {
          type: 'FOLD',
          playerId: 'player-1',
          playerName: 'Alice',
          timestamp: Date.now()
        };
        store.recordAction(action);

        store.lastAction$.subscribe(recorded => {
          expect(recorded).toEqual(action);
          done();
        });
      });

      it('should record action with amount', (done) => {
        const action: PlayerActionRecord = {
          type: 'RAISE',
          playerId: 'player-1',
          playerName: 'Alice',
          amount: 100,
          timestamp: Date.now()
        };
        store.recordAction(action);

        store.lastAction$.subscribe(recorded => {
          expect(recorded?.amount).toBe(100);
          done();
        });
      });
    });

    describe('setProcessingBots', () => {
      it('should set bot processing state', (done) => {
        store.setProcessingBots(true);
        store.processingBots$.subscribe(processing => {
          expect(processing).toBe(true);
          done();
        });
      });
    });

    describe('setConnectionStatus', () => {
      it('should set connected status', (done) => {
        store.setConnectionStatus('connected');
        store.connectionStatus$.subscribe(status => {
          expect(status).toBe('connected');
          done();
        });
      });

      it('should set reconnecting status', (done) => {
        store.setConnectionStatus('reconnecting');
        store.connectionStatus$.subscribe(status => {
          expect(status).toBe('reconnecting');
          done();
        });
      });
    });

    describe('addToHistory', () => {
      it('should add game to history', (done) => {
        const game = createMockGame();
        store.addToHistory(game);

        store.gameHistory$.subscribe(history => {
          expect(history.length).toBe(1);
          expect(history[0]).toEqual(game);
          done();
        });
      });

      it('should append to existing history', (done) => {
        const game1 = createMockGame({ id: 'game-1' });
        const game2 = createMockGame({ id: 'game-2' });

        store.addToHistory(game1);
        store.addToHistory(game2);

        store.gameHistory$.subscribe(history => {
          expect(history.length).toBe(2);
          done();
        });
      });
    });

    describe('reset', () => {
      it('should reset to initial state', (done) => {

        store.setGame(createMockGame());
        store.setLoading(true);
        store.setError('Error');
        store.setConnectionStatus('connected');


        store.reset();

        store.game$.subscribe(game => {
          expect(game).toBeNull();
          done();
        });
      });
    });
  });





  describe('Selectors', () => {
    beforeEach(() => {
      store.setGame(createMockGame());
    });

    describe('players$', () => {
      it('should return players array', (done) => {
        store.players$.subscribe(players => {
          expect(players.length).toBe(3);
          done();
        });
      });

      it('should return empty array when no game', (done) => {
        store.setGame(null);
        store.players$.subscribe(players => {
          expect(players).toEqual([]);
          done();
        });
      });
    });

    describe('currentPlayer$', () => {
      it('should return current player', (done) => {
        store.currentPlayer$.subscribe(player => {
          expect(player?.name).toBe('Alice');
          done();
        });
      });

      it('should return null when no game', (done) => {
        store.setGame(null);
        store.currentPlayer$.subscribe(player => {
          expect(player).toBeNull();
          done();
        });
      });
    });

    describe('humanPlayer$', () => {
      it('should return non-bot player', (done) => {
        store.humanPlayer$.subscribe(player => {
          expect(player?.name).toBe('Alice');
          expect(player?.isBot).toBe(false);
          done();
        });
      });
    });

    describe('isHumanTurn$', () => {
      it('should return true when human is current player', (done) => {
        store.isHumanTurn$.subscribe(isHumanTurn => {
          expect(isHumanTurn).toBe(true);
          done();
        });
      });

      it('should return false when bot is current player', (done) => {
        const game = createMockGame({ currentPlayerIndex: 1 });
        store.setGame(game);

        store.isHumanTurn$.subscribe(isHumanTurn => {
          expect(isHumanTurn).toBe(false);
          done();
        });
      });
    });

    describe('potSize$', () => {
      it('should return current pot', (done) => {
        store.potSize$.subscribe(pot => {
          expect(pot).toBe(100);
          done();
        });
      });

      it('should return 0 when no game', (done) => {
        store.setGame(null);
        store.potSize$.subscribe(pot => {
          expect(pot).toBe(0);
          done();
        });
      });
    });

    describe('phase$ and phaseDisplayName$', () => {
      it('should return current phase', (done) => {
        store.phase$.subscribe(phase => {
          expect(phase).toBe('PRE_FLOP');
          done();
        });
      });

      it('should return display name for phase', (done) => {
        store.phaseDisplayName$.subscribe(name => {
          expect(name).toBe('Pre-Flop');
          done();
        });
      });

      it('should handle all phases', (done) => {
        const phases = ['FLOP', 'TURN', 'RIVER', 'SHOWDOWN'];
        const names = ['Flop', 'Turn', 'River', 'Showdown'];

        store.setGame(createMockGame({ phase: phases[0] }));
        store.phaseDisplayName$.subscribe(name => {
          expect(names).toContain(name);
          done();
        });
      });
    });

    describe('canCheck$', () => {
      it('should return true when bets are equal', (done) => {
        store.canCheck$.subscribe(canCheck => {
          expect(canCheck).toBe(true);
          done();
        });
      });

      it('should return false when call is needed', (done) => {
        const game = createMockGame({ currentBet: 50 });
        store.setGame(game);

        store.canCheck$.subscribe(canCheck => {
          expect(canCheck).toBe(false);
          done();
        });
      });
    });

    describe('callAmount$', () => {
      it('should return 0 when no call needed', (done) => {
        store.callAmount$.subscribe(amount => {
          expect(amount).toBe(0);
          done();
        });
      });

      it('should calculate call amount', (done) => {
        const game = createMockGame({ currentBet: 50 });
        store.setGame(game);

        store.callAmount$.subscribe(amount => {
          expect(amount).toBe(30);
          done();
        });
      });
    });

    describe('canCall$', () => {
      it('should return false when no call needed', (done) => {
        store.canCall$.subscribe(canCall => {
          expect(canCall).toBe(false);
          done();
        });
      });

      it('should return true when call needed and affordable', (done) => {
        const game = createMockGame({ currentBet: 50 });
        store.setGame(game);

        store.canCall$.subscribe(canCall => {
          expect(canCall).toBe(true);
          done();
        });
      });
    });

    describe('minRaiseAmount$', () => {
      it('should calculate min raise', (done) => {
        store.minRaiseAmount$.subscribe(amount => {
          expect(amount).toBe(40);
          done();
        });
      });
    });

    describe('maxRaiseAmount$', () => {
      it('should return human player total chips', (done) => {
        store.maxRaiseAmount$.subscribe(amount => {
          expect(amount).toBe(1000);
          done();
        });
      });
    });

    describe('isGameFinished$', () => {
      it('should return false for active game', (done) => {
        store.isGameFinished$.subscribe(finished => {
          expect(finished).toBe(false);
          done();
        });
      });

      it('should return true when isFinished is true', (done) => {
        const game = createMockGame({ isFinished: true });
        store.setGame(game);

        store.isGameFinished$.subscribe(finished => {
          expect(finished).toBe(true);
          done();
        });
      });

      it('should return true for SHOWDOWN phase', (done) => {
        const game = createMockGame({ phase: 'SHOWDOWN' });
        store.setGame(game);

        store.isGameFinished$.subscribe(finished => {
          expect(finished).toBe(true);
          done();
        });
      });
    });

    describe('activePlayers$', () => {
      it('should return non-folded players', (done) => {
        store.activePlayers$.subscribe(players => {
          expect(players.length).toBe(3);
          done();
        });
      });

      it('should exclude folded players', (done) => {
        const players = [
          createMockPlayer({ id: 'p1', folded: false }),
          createMockPlayer({ id: 'p2', folded: true }),
          createMockPlayer({ id: 'p3', folded: false }),
        ];
        store.setGame(createMockGame({ players }));

        store.activePlayers$.subscribe(active => {
          expect(active.length).toBe(2);
          done();
        });
      });
    });

    describe('canPlayerAct$', () => {
      it('should return true when human can act', (done) => {
        store.canPlayerAct$.subscribe(canAct => {
          expect(canAct).toBe(true);
          done();
        });
      });

      it('should return false when not human turn', (done) => {
        const game = createMockGame({ currentPlayerIndex: 1 });
        store.setGame(game);

        store.canPlayerAct$.subscribe(canAct => {
          expect(canAct).toBe(false);
          done();
        });
      });

      it('should return false when human folded', (done) => {
        const players = [
          createMockPlayer({ id: 'human-1', name: 'Alice', folded: true }),
          createMockPlayer({ id: 'bot-1', name: 'Bot1', isBot: true }),
        ];
        store.setGame(createMockGame({ players, currentPlayerIndex: 0 }));

        store.canPlayerAct$.subscribe(canAct => {
          expect(canAct).toBe(false);
          done();
        });
      });

      it('should return false when action in progress', (done) => {
        store.setActionInProgress(true);

        store.canPlayerAct$.subscribe(canAct => {
          expect(canAct).toBe(false);
          done();
        });
      });
    });

    describe('activeBots$', () => {
      it('should return active bots', (done) => {
        store.activeBots$.subscribe(bots => {
          expect(bots.length).toBe(2);
          expect(bots.every(b => b.isBot)).toBe(true);
          done();
        });
      });

      it('should exclude folded bots', (done) => {
        const players = [
          createMockPlayer({ id: 'human', name: 'Alice' }),
          createMockPlayer({ id: 'bot1', name: 'Bot1', isBot: true, folded: true }),
          createMockPlayer({ id: 'bot2', name: 'Bot2', isBot: true }),
        ];
        store.setGame(createMockGame({ players }));

        store.activeBots$.subscribe(bots => {
          expect(bots.length).toBe(1);
          done();
        });
      });
    });

    describe('currentBot$', () => {
      it('should return null when human turn', (done) => {
        store.currentBot$.subscribe(bot => {
          expect(bot).toBeNull();
          done();
        });
      });

      it('should return bot when bot turn', (done) => {
        const game = createMockGame({ currentPlayerIndex: 1 });
        store.setGame(game);

        store.currentBot$.subscribe(bot => {
          expect(bot?.name).toBe('Bot1');
          done();
        });
      });
    });
  });





  describe('View Model (vm$)', () => {
    it('should combine all relevant state', (done) => {
      store.setGame(createMockGame());

      store.vm$.subscribe((vm: GameViewModel) => {
        expect(vm.game).toBeTruthy();
        expect(vm.currentPlayer).toBeTruthy();
        expect(vm.humanPlayer).toBeTruthy();
        expect(vm.isLoading).toBe(false);
        expect(vm.error).toBeNull();
        expect(vm.isHumanTurn).toBe(true);
        expect(vm.canCheck).toBe(true);
        expect(vm.potSize).toBe(100);
        expect(vm.phase).toBe('PRE_FLOP');
        expect(vm.phaseDisplayName).toBe('Pre-Flop');
        expect(vm.communityCards).toEqual([]);
        expect(vm.isGameFinished).toBe(false);
        expect(vm.activePlayers.length).toBe(3);
        done();
      });
    });

    it('should update when state changes', fakeAsync(() => {
      const vmValues: GameViewModel[] = [];
      store.vm$.subscribe(vm => vmValues.push(vm));

      tick();
      store.setGame(createMockGame());
      tick();
      store.setLoading(true);
      tick();
      store.setError('Test error');
      tick();

      expect(vmValues.length).toBeGreaterThan(1);
      flush();
    }));
  });





  describe('Effects', () => {
    describe('startGame', () => {
      it('should start game with default players', fakeAsync(() => {
        store.startGame();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/start`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body.length).toBe(3);

        const game = createMockGame();
        req.flush(game);
        tick();

        store.game$.subscribe(g => {
          expect(g).toEqual(game);
        });
        flush();
      }));

      it('should start game with custom players', fakeAsync(() => {
        const players: PlayerInfo[] = [
          { name: 'Custom', startingChips: 2000, isBot: false },
          { name: 'Bot', startingChips: 2000, isBot: true }
        ];

        store.startGame(players);
        tick();

        const req = httpMock.expectOne(`${apiUrl}/start`);
        expect(req.request.body).toEqual(players);

        req.flush(createMockGame());
        tick();
        flush();
      }));

      it('should set loading during request', fakeAsync(() => {
        const loadingStates: boolean[] = [];
        store.isLoading$.subscribe(l => loadingStates.push(l));

        store.startGame();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/start`);
        expect(loadingStates).toContain(true);

        req.flush(createMockGame());
        tick();
        flush();
      }));

      it('should handle error', fakeAsync(() => {
        store.startGame();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/start`);
        req.flush({ message: 'Server error' }, { status: 500, statusText: 'Error' });
        tick();

        store.error$.subscribe(error => {
          expect(error).toBeTruthy();
        });
        flush();
      }));
    });

    describe('refreshGame', () => {
      it('should fetch game status', fakeAsync(() => {
        store.refreshGame();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/status`);
        expect(req.request.method).toBe('GET');

        const game = createMockGame();
        req.flush(game);
        tick();

        store.game$.subscribe(g => {
          expect(g).toEqual(game);
        });
        flush();
      }));

      it('should add to history', fakeAsync(() => {
        store.refreshGame();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/status`);
        req.flush(createMockGame());
        tick();

        store.gameHistory$.subscribe(history => {
          expect(history.length).toBe(1);
        });
        flush();
      }));
    });

    describe('startNewHand', () => {
      it('should start new hand', fakeAsync(() => {
        store.startNewHand();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/new-match`);
        expect(req.request.method).toBe('POST');

        req.flush(createMockGame({ handNumber: 2 }));
        tick();

        store.game$.subscribe(g => {
          expect(g?.handNumber).toBe(2);
        });
        flush();
      }));

      it('should clear last action', fakeAsync(() => {
        store.recordAction({
          type: 'FOLD',
          playerId: 'p1',
          playerName: 'Alice',
          timestamp: Date.now()
        });

        store.startNewHand();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/new-match`);
        req.flush(createMockGame());
        tick();

        store.lastAction$.subscribe(action => {
          expect(action).toBeNull();
        });
        flush();
      }));
    });

    describe('resetGame', () => {
      it('should reset game', fakeAsync(() => {
        store.setGame(createMockGame());
        store.resetGame();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/reset`);
        req.flush('OK');
        tick();

        store.game$.subscribe(g => {
          expect(g).toBeNull();
        });
        flush();
      }));
    });

    describe('playerAction', () => {
      beforeEach(() => {
        store.setGame(createMockGame());
      });

      it('should execute fold action', fakeAsync(() => {
        store.playerAction({ playerId: 'human-1', action: 'FOLD' });
        tick();

        const foldReq = httpMock.expectOne(req =>
          req.url.includes('/fold') && req.method === 'POST'
        );
        foldReq.flush('OK');
        tick();

        const statusReq = httpMock.expectOne(`${apiUrl}/status`);
        statusReq.flush(createMockGame());
        tick();

        store.lastAction$.subscribe(action => {
          expect(action?.type).toBe('FOLD');
        });
        flush();
      }));

      it('should execute check action', fakeAsync(() => {
        store.playerAction({ playerId: 'human-1', action: 'CHECK' });
        tick();

        const checkReq = httpMock.expectOne(req =>
          req.url.includes('/check') && req.method === 'POST'
        );
        checkReq.flush('OK');
        tick();

        const statusReq = httpMock.expectOne(`${apiUrl}/status`);
        statusReq.flush(createMockGame());
        tick();
        flush();
      }));

      it('should execute call action', fakeAsync(() => {
        store.playerAction({ playerId: 'human-1', action: 'CALL' });
        tick();

        const callReq = httpMock.expectOne(req =>
          req.url.includes('/call') && req.method === 'POST'
        );
        callReq.flush('OK');
        tick();

        const statusReq = httpMock.expectOne(`${apiUrl}/status`);
        statusReq.flush(createMockGame());
        tick();
        flush();
      }));

      it('should execute bet action with amount', fakeAsync(() => {
        store.playerAction({ playerId: 'human-1', action: 'BET', amount: 50 });
        tick();

        const betReq = httpMock.expectOne(`${apiUrl}/bet`);
        expect(betReq.request.body).toEqual({ playerId: 'human-1', amount: 50 });
        betReq.flush('OK');
        tick();

        const statusReq = httpMock.expectOne(`${apiUrl}/status`);
        statusReq.flush(createMockGame());
        tick();
        flush();
      }));

      it('should execute raise action', fakeAsync(() => {
        store.playerAction({ playerId: 'human-1', action: 'RAISE', amount: 100 });
        tick();

        const raiseReq = httpMock.expectOne(`${apiUrl}/raise`);
        expect(raiseReq.request.body).toEqual({ playerId: 'human-1', amount: 100 });
        raiseReq.flush('OK');
        tick();

        const statusReq = httpMock.expectOne(`${apiUrl}/status`);
        statusReq.flush(createMockGame());
        tick();

        store.lastAction$.subscribe(action => {
          expect(action?.type).toBe('RAISE');
          expect(action?.amount).toBe(100);
        });
        flush();
      }));

      it('should set actionInProgress during action', fakeAsync(() => {
        const inProgressStates: boolean[] = [];
        store.actionInProgress$.subscribe(s => inProgressStates.push(s));

        store.playerAction({ playerId: 'human-1', action: 'FOLD' });
        tick();

        expect(inProgressStates).toContain(true);

        const foldReq = httpMock.expectOne(req => req.url.includes('/fold'));
        foldReq.flush('OK');
        tick();

        const statusReq = httpMock.expectOne(`${apiUrl}/status`);
        statusReq.flush(createMockGame());
        tick();
        flush();
      }));

      it('should handle action error', fakeAsync(() => {
        store.playerAction({ playerId: 'human-1', action: 'FOLD' });
        tick();

        const foldReq = httpMock.expectOne(req => req.url.includes('/fold'));
        foldReq.flush({ message: 'Invalid action' }, { status: 400, statusText: 'Bad Request' });
        tick();

        store.error$.subscribe(error => {
          expect(error).toBeTruthy();
        });
        flush();
      }));
    });
  });





  describe('Utility Methods', () => {
    beforeEach(() => {
      store.setGame(createMockGame());
    });

    describe('getPlayerById', () => {
      it('should return player when found', () => {
        const player = store.getPlayerById('human-1');
        expect(player?.name).toBe('Alice');
      });

      it('should return undefined when not found', () => {
        const player = store.getPlayerById('non-existent');
        expect(player).toBeUndefined();
      });
    });

    describe('isPlayerTurn', () => {
      it('should return true for current player', () => {
        expect(store.isPlayerTurn('human-1')).toBe(true);
      });

      it('should return false for non-current player', () => {
        expect(store.isPlayerTurn('bot-1')).toBe(false);
      });
    });

    describe('isDealer', () => {
      it('should return true for dealer position', () => {
        expect(store.isDealer(2)).toBe(true);
      });

      it('should return false for non-dealer', () => {
        expect(store.isDealer(0)).toBe(false);
      });
    });

    describe('getPlayerStatus', () => {
      it('should return "Folded" for folded player', () => {
        const player = createMockPlayer({ folded: true });
        expect(store.getPlayerStatus(player)).toBe('Folded');
      });

      it('should return "All-In" for all-in player', () => {
        const player = createMockPlayer({ isAllIn: true });
        expect(store.getPlayerStatus(player)).toBe('All-In');
      });

      it('should return "Out" for player with no chips', () => {
        const player = createMockPlayer({ chips: 0 });
        expect(store.getPlayerStatus(player)).toBe('Out');
      });

      it('should return empty string for active player', () => {
        const player = createMockPlayer();
        expect(store.getPlayerStatus(player)).toBe('');
      });
    });
  });





  describe('Edge Cases', () => {
    it('should handle game with empty players', (done) => {
      store.setGame(createMockGame({ players: [] }));

      store.players$.subscribe(players => {
        expect(players).toEqual([]);
        done();
      });
    });

    it('should handle rapid state updates', fakeAsync(() => {
      for (let i = 0; i < 100; i++) {
        store.setGame(createMockGame({ currentPot: i * 10 }));
      }
      tick();

      store.potSize$.subscribe(pot => {
        expect(pot).toBe(990);
      });
      flush();
    }));

    it('should handle null game gracefully', (done) => {
      store.setGame(null);

      store.vm$.subscribe(vm => {
        expect(vm.game).toBeNull();
        expect(vm.currentPlayer).toBeNull();
        expect(vm.potSize).toBe(0);
        expect(vm.isHumanTurn).toBe(false);
        done();
      });
    });
  });
});
