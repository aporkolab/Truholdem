import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  TournamentStore,
  TournamentListViewModel,
  TournamentLobbyViewModel,
  TournamentTableViewModel
} from './tournament.store';
import {
  Tournament,
  TournamentListItem,
  TournamentTable,
  TournamentPlayer,
  BlindLevel,
  DEFAULT_BLIND_LEVELS,
  DEFAULT_PRIZE_STRUCTURE,
  calculateTimeRemaining,
  formatTimeRemaining,
  getNextBlindLevel
} from '../model/tournament';
import { WebSocketService } from '../services/websocket.service';
import { environment } from '../../environments/environment';

describe('TournamentStore', () => {
  let store: TournamentStore;
  let httpMock: HttpTestingController;
  let wsServiceMock: jasmine.SpyObj<WebSocketService>;
  const apiUrl = `${environment.apiUrl}/tournament`;




  const createMockBlindLevel = (overrides: Partial<BlindLevel> = {}): BlindLevel => ({
    level: overrides.level ?? 1,
    smallBlind: overrides.smallBlind ?? 25,
    bigBlind: overrides.bigBlind ?? 50,
    ante: overrides.ante ?? 0,
    durationMinutes: overrides.durationMinutes ?? 15
  });

  const createMockTournamentPlayer = (overrides: Partial<TournamentPlayer> = {}): TournamentPlayer => ({
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
    rank: overrides.rank,
    finishPosition: overrides.finishPosition,
    prizeMoney: overrides.prizeMoney,
    tablesPlayed: overrides.tablesPlayed ?? 1,
    handsWon: overrides.handsWon ?? 0,
    biggestPot: overrides.biggestPot ?? 0,
    knockouts: overrides.knockouts ?? 0,
    isEliminated: overrides.isEliminated ?? false,
    eliminatedBy: overrides.eliminatedBy,
    eliminatedAtLevel: overrides.eliminatedAtLevel,
    canAct: function() {
      return !this.folded && !this.isAllIn && this.chips > 0;
    },
    getDisplayName: function() {
      if (this.name.startsWith('Bot')) {
        return this.name.substring(3) || 'Bot';
      }
      return this.name || 'Anonymous';
    },
    isHuman: function() {
      return !this.isBot && !this.name.startsWith('Bot');
    },
    getStatusText: function() {
      if (this.folded) return 'Folded';
      if (this.isAllIn) return 'All-In';
      if (this.chips === 0) return 'Out';
      return '';
    }
  });

  const createMockTournamentTable = (overrides: Partial<TournamentTable> = {}): TournamentTable => ({
    id: overrides.id ?? 'table-1',
    tableNumber: overrides.tableNumber ?? 1,
    players: overrides.players ?? [
      createMockTournamentPlayer({ id: 'p1', name: 'Player1' }),
      createMockTournamentPlayer({ id: 'p2', name: 'Player2', isBot: true })
    ],
    currentHandNumber: overrides.currentHandNumber ?? 1,
    dealerPosition: overrides.dealerPosition ?? 0,
    isActive: overrides.isActive ?? true
  });

  const createMockTournament = (overrides: Partial<Tournament> = {}): Tournament => ({
    id: overrides.id ?? 'tournament-123',
    name: overrides.name ?? 'Test Tournament',
    status: overrides.status ?? 'REGISTERING',
    config: overrides.config ?? {
      name: 'Test Tournament',
      buyIn: 100,
      startingChips: 10000,
      maxPlayers: 9,
      minPlayers: 2,
      blindLevels: DEFAULT_BLIND_LEVELS,
      levelDurationMinutes: 15,
      breakAfterLevels: 3,
      breakDurationMinutes: 5,
      prizeStructure: DEFAULT_PRIZE_STRUCTURE,
      lateRegistrationLevels: 3,
      rebuyAllowed: false,
      rebuyLevels: 0,
      addOnAllowed: false
    },
    currentLevel: overrides.currentLevel ?? 1,
    currentBlinds: overrides.currentBlinds ?? createMockBlindLevel(),
    levelStartTime: overrides.levelStartTime ?? Date.now(),
    levelEndTime: overrides.levelEndTime ?? Date.now() + 15 * 60 * 1000,
    nextBreakAtLevel: overrides.nextBreakAtLevel ?? 3,
    registeredPlayers: overrides.registeredPlayers ?? [
      createMockTournamentPlayer({ id: 'human-1', name: 'Human', isBot: false }),
      createMockTournamentPlayer({ id: 'bot-1', name: 'Bot1', isBot: true }),
      createMockTournamentPlayer({ id: 'bot-2', name: 'Bot2', isBot: true })
    ],
    remainingPlayers: overrides.remainingPlayers ?? 3,
    totalPlayers: overrides.totalPlayers ?? 3,
    tables: overrides.tables ?? [createMockTournamentTable()],
    averageStack: overrides.averageStack ?? 10000,
    largestStack: overrides.largestStack ?? 12000,
    smallestStack: overrides.smallestStack ?? 8000,
    totalChipsInPlay: overrides.totalChipsInPlay ?? 30000,
    handsPlayed: overrides.handsPlayed ?? 0,
    prizePool: overrides.prizePool ?? 300,
    startTime: overrides.startTime,
    endTime: overrides.endTime,
    createdAt: overrides.createdAt ?? Date.now(),
    updatedAt: overrides.updatedAt ?? Date.now()
  });

  const createMockTournamentListItem = (overrides: Partial<TournamentListItem> = {}): TournamentListItem => ({
    id: overrides.id ?? 'tournament-123',
    name: overrides.name ?? 'Test Tournament',
    status: overrides.status ?? 'REGISTERING',
    buyIn: overrides.buyIn ?? 100,
    startingChips: overrides.startingChips ?? 10000,
    currentLevel: overrides.currentLevel ?? 1,
    registeredCount: overrides.registeredCount ?? 5,
    maxPlayers: overrides.maxPlayers ?? 9,
    prizePool: overrides.prizePool ?? 500,
    startTime: overrides.startTime,
    smallBlind: overrides.smallBlind ?? 25,
    bigBlind: overrides.bigBlind ?? 50
  });




  beforeEach(() => {
    wsServiceMock = jasmine.createSpyObj('WebSocketService', [
      'connect',
      'disconnect',
      'subscribeToGame',
      'isConnected'
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        TournamentStore,
        { provide: WebSocketService, useValue: wsServiceMock }
      ]
    });

    store = TestBed.inject(TournamentStore);
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

    it('should initialize with empty tournaments array', (done) => {
      store.tournaments$.subscribe(tournaments => {
        expect(tournaments).toEqual([]);
        done();
      });
    });

    it('should initialize with null activeTournament', (done) => {
      store.activeTournament$.subscribe(tournament => {
        expect(tournament).toBeNull();
        done();
      });
    });

    it('should initialize with null myTable', (done) => {
      store.myTable$.subscribe(table => {
        expect(table).toBeNull();
        done();
      });
    });

    it('should initialize with null myPlayer', (done) => {
      store.myPlayer$.subscribe(player => {
        expect(player).toBeNull();
        done();
      });
    });

    it('should start with isLoading false', (done) => {
      store.isLoading$.subscribe(loading => {
        expect(loading).toBe(false);
        done();
      });
    });

    it('should start with disconnected connectionStatus', (done) => {
      store.connectionStatus$.subscribe(status => {
        expect(status).toBe('disconnected');
        done();
      });
    });
  });




  describe('Updaters', () => {
    describe('setTournaments', () => {
      it('should update tournaments list', (done) => {
        const tournaments = [
          createMockTournamentListItem({ id: 't1' }),
          createMockTournamentListItem({ id: 't2' })
        ];

        store.setTournaments(tournaments);

        store.tournaments$.subscribe(result => {
          expect(result.length).toBe(2);
          expect(result[0].id).toBe('t1');
          done();
        });
      });

      it('should set isLoading to false', (done) => {
        store.setLoading(true);
        store.setTournaments([]);

        store.isLoading$.subscribe(loading => {
          expect(loading).toBe(false);
          done();
        });
      });
    });

    describe('setActiveTournament', () => {
      it('should update active tournament', (done) => {
        const tournament = createMockTournament();

        store.setActiveTournament(tournament);

        store.activeTournament$.subscribe(result => {
          expect(result?.id).toBe('tournament-123');
          done();
        });
      });
    });

    describe('setMyPlayer', () => {
      it('should update my player', (done) => {
        const player = createMockTournamentPlayer({ id: 'me', name: 'MyPlayer' });

        store.setMyPlayer(player);

        store.myPlayer$.subscribe(result => {
          expect(result?.name).toBe('MyPlayer');
          done();
        });
      });
    });

    describe('setMyTable', () => {
      it('should update my table', (done) => {
        const table = createMockTournamentTable({ tableNumber: 5 });

        store.setMyTable(table);

        store.myTable$.subscribe(result => {
          expect(result?.tableNumber).toBe(5);
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

      it('should set isLoading to false', (done) => {
        store.setLoading(true);
        store.setError('Error');

        store.isLoading$.subscribe(loading => {
          expect(loading).toBe(false);
          done();
        });
      });

      it('should set isRegistering to false', (done) => {
        store.setRegistering(true);
        store.setError('Error');

        store.isRegistering$.subscribe(registering => {
          expect(registering).toBe(false);
          done();
        });
      });
    });

    describe('clearError', () => {
      it('should clear error', (done) => {
        store.setError('Some error');
        store.clearError();

        store.error$.subscribe(error => {
          expect(error).toBeNull();
          done();
        });
      });
    });

    describe('reset', () => {
      it('should reset to initial state', (done) => {
        store.setTournaments([createMockTournamentListItem()]);
        store.setActiveTournament(createMockTournament());
        store.setError('Error');
        store.reset();

        store.tournaments$.subscribe(tournaments => {
          expect(tournaments).toEqual([]);
          done();
        });
      });
    });
  });




  describe('Derived Selectors', () => {
    describe('openTournaments$', () => {
      it('should filter only registering tournaments', (done) => {
        const tournaments = [
          createMockTournamentListItem({ id: 't1', status: 'REGISTERING' }),
          createMockTournamentListItem({ id: 't2', status: 'RUNNING' }),
          createMockTournamentListItem({ id: 't3', status: 'REGISTERING' })
        ];

        store.setTournaments(tournaments);

        store.openTournaments$.subscribe(result => {
          expect(result.length).toBe(2);
          expect(result.every(t => t.status === 'REGISTERING')).toBe(true);
          done();
        });
      });
    });

    describe('runningTournaments$', () => {
      it('should filter running and final table tournaments', (done) => {
        const tournaments = [
          createMockTournamentListItem({ id: 't1', status: 'REGISTERING' }),
          createMockTournamentListItem({ id: 't2', status: 'RUNNING' }),
          createMockTournamentListItem({ id: 't3', status: 'FINAL_TABLE' })
        ];

        store.setTournaments(tournaments);

        store.runningTournaments$.subscribe(result => {
          expect(result.length).toBe(2);
          done();
        });
      });
    });

    describe('currentBlinds$', () => {
      it('should return current blinds from active tournament', (done) => {
        const blinds = createMockBlindLevel({ smallBlind: 100, bigBlind: 200 });
        const tournament = createMockTournament({ currentBlinds: blinds });

        store.setActiveTournament(tournament);

        store.currentBlinds$.subscribe(result => {
          expect(result?.smallBlind).toBe(100);
          expect(result?.bigBlind).toBe(200);
          done();
        });
      });

      it('should return null when no active tournament', (done) => {
        store.currentBlinds$.subscribe(result => {
          expect(result).toBeNull();
          done();
        });
      });
    });

    describe('nextBlinds$', () => {
      it('should return next blind level', (done) => {
        const tournament = createMockTournament({ currentLevel: 1 });

        store.setActiveTournament(tournament);

        store.nextBlinds$.subscribe(result => {
          expect(result?.level).toBe(2);
          done();
        });
      });
    });

    describe('remainingPlayers$', () => {
      it('should return remaining player count', (done) => {
        const tournament = createMockTournament({ remainingPlayers: 42 });

        store.setActiveTournament(tournament);

        store.remainingPlayers$.subscribe(result => {
          expect(result).toBe(42);
          done();
        });
      });
    });

    describe('averageStack$', () => {
      it('should return average stack', (done) => {
        const tournament = createMockTournament({ averageStack: 15000 });

        store.setActiveTournament(tournament);

        store.averageStack$.subscribe(result => {
          expect(result).toBe(15000);
          done();
        });
      });
    });

    describe('isOnBreak$', () => {
      it('should return true when tournament is paused', (done) => {
        const tournament = createMockTournament({ status: 'PAUSED' });

        store.setActiveTournament(tournament);

        store.isOnBreak$.subscribe(result => {
          expect(result).toBe(true);
          done();
        });
      });

      it('should return false when tournament is running', (done) => {
        const tournament = createMockTournament({ status: 'RUNNING' });

        store.setActiveTournament(tournament);

        store.isOnBreak$.subscribe(result => {
          expect(result).toBe(false);
          done();
        });
      });
    });

    describe('isFinalTable$', () => {
      it('should return true at final table', (done) => {
        const tournament = createMockTournament({ status: 'FINAL_TABLE' });

        store.setActiveTournament(tournament);

        store.isFinalTable$.subscribe(result => {
          expect(result).toBe(true);
          done();
        });
      });
    });

    describe('isEliminated$', () => {
      it('should return true when player is eliminated', (done) => {
        const player = createMockTournamentPlayer({ isEliminated: true });

        store.setMyPlayer(player);

        store.isEliminated$.subscribe(result => {
          expect(result).toBe(true);
          done();
        });
      });
    });

    describe('myRank$', () => {
      it('should calculate player rank by chip count', (done) => {
        const players = [
          createMockTournamentPlayer({ id: 'p1', chips: 20000 }),
          createMockTournamentPlayer({ id: 'me', chips: 15000 }),
          createMockTournamentPlayer({ id: 'p3', chips: 10000 })
        ];
        const tournament = createMockTournament({ registeredPlayers: players });
        const myPlayer = createMockTournamentPlayer({ id: 'me', chips: 15000 });

        store.setActiveTournament(tournament);
        store.setMyPlayer(myPlayer);

        store.myRank$.subscribe(result => {
          expect(result).toBe(2);
          done();
        });
      });

      it('should exclude eliminated players from ranking', (done) => {
        const players = [
          createMockTournamentPlayer({ id: 'p1', chips: 20000, isEliminated: true }),
          createMockTournamentPlayer({ id: 'me', chips: 15000 }),
          createMockTournamentPlayer({ id: 'p3', chips: 10000 })
        ];
        const tournament = createMockTournament({ registeredPlayers: players });
        const myPlayer = createMockTournamentPlayer({ id: 'me', chips: 15000 });

        store.setActiveTournament(tournament);
        store.setMyPlayer(myPlayer);

        store.myRank$.subscribe(result => {
          expect(result).toBe(1);
          done();
        });
      });
    });
  });




  describe('View Models', () => {
    describe('tournamentListVm$', () => {
      it('should compose correct view model', (done) => {
        const tournaments = [
          createMockTournamentListItem({ status: 'REGISTERING' }),
          createMockTournamentListItem({ status: 'RUNNING' })
        ];

        store.setTournaments(tournaments);

        store.tournamentListVm$.subscribe((vm: TournamentListViewModel) => {
          expect(vm.tournaments.length).toBe(2);
          expect(vm.openTournaments.length).toBe(1);
          expect(vm.runningTournaments.length).toBe(1);
          expect(vm.isLoading).toBe(false);
          expect(vm.error).toBeNull();
          done();
        });
      });
    });

    describe('tournamentLobbyVm$', () => {
      it('should indicate registration is possible', (done) => {
        const tournament = createMockTournament({
          status: 'REGISTERING',
          registeredPlayers: [createMockTournamentPlayer()]
        });

        store.setActiveTournament(tournament);

        store.tournamentLobbyVm$.subscribe((vm: TournamentLobbyViewModel) => {
          expect(vm.canRegister).toBe(true);
          expect(vm.isRegistered).toBe(false);
          expect(vm.spotsRemaining).toBe(8);
          done();
        });
      });

      it('should indicate player is registered', (done) => {
        const tournament = createMockTournament();
        const myPlayer = createMockTournamentPlayer({ id: 'me' });

        store.setActiveTournament(tournament);
        store.setMyPlayer(myPlayer);

        store.tournamentLobbyVm$.subscribe((vm: TournamentLobbyViewModel) => {
          expect(vm.isRegistered).toBe(true);
          done();
        });
      });

      it('should not allow registration when full', (done) => {
        const players = Array(9).fill(null).map((_, i) =>
          createMockTournamentPlayer({ id: `p${i}` })
        );
        const tournament = createMockTournament({
          status: 'REGISTERING',
          registeredPlayers: players
        });

        store.setActiveTournament(tournament);

        store.tournamentLobbyVm$.subscribe((vm: TournamentLobbyViewModel) => {
          expect(vm.canRegister).toBe(false);
          expect(vm.spotsRemaining).toBe(0);
          done();
        });
      });
    });

    describe('tournamentTableVm$', () => {
      it('should compose complete table view model', (done) => {
        const tournament = createMockTournament({
          status: 'RUNNING',
          remainingPlayers: 6,
          averageStack: 12000,
          currentLevel: 3
        });
        const table = createMockTournamentTable();
        const myPlayer = createMockTournamentPlayer({ id: 'me' });

        store.setActiveTournament(tournament);
        store.setMyTable(table);
        store.setMyPlayer(myPlayer);

        store.tournamentTableVm$.subscribe((vm: TournamentTableViewModel) => {
          expect(vm.tournament).toBeTruthy();
          expect(vm.table).toBeTruthy();
          expect(vm.myPlayer).toBeTruthy();
          expect(vm.remainingPlayers).toBe(6);
          expect(vm.averageStack).toBe(12000);
          expect(vm.isOnBreak).toBe(false);
          expect(vm.isFinalTable).toBe(false);
          expect(vm.isEliminated).toBe(false);
          done();
        });
      });
    });
  });




  describe('Effects', () => {
    describe('loadTournaments', () => {
      it('should load tournaments from API', fakeAsync(() => {
        const tournaments = [
          createMockTournamentListItem({ id: 't1' }),
          createMockTournamentListItem({ id: 't2' })
        ];

        store.loadTournaments();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/list`);
        expect(req.request.method).toBe('GET');
        req.flush(tournaments);

        tick();

        store.tournaments$.subscribe(result => {
          expect(result.length).toBe(2);
        });

        flush();
      }));

      it('should handle API error', fakeAsync(() => {
        store.loadTournaments();
        tick();

        const req = httpMock.expectOne(`${apiUrl}/list`);
        req.flush('Error', { status: 500, statusText: 'Server Error' });

        tick();

        store.error$.subscribe(error => {
          expect(error).toBeTruthy();
        });

        flush();
      }));

      it('should set loading state during request', fakeAsync(() => {
        const loadingStates: boolean[] = [];

        store.isLoading$.subscribe(loading => {
          loadingStates.push(loading);
        });

        store.loadTournaments();
        tick();

        expect(loadingStates).toContain(true);

        const req = httpMock.expectOne(`${apiUrl}/list`);
        req.flush([]);

        tick();
        flush();
      }));
    });

    describe('loadTournament', () => {
      it('should load specific tournament by ID', fakeAsync(() => {
        const tournament = createMockTournament({ id: 'test-id' });

        store.loadTournament('test-id');
        tick();

        const req = httpMock.expectOne(`${apiUrl}/test-id`);
        expect(req.request.method).toBe('GET');
        req.flush(tournament);

        tick();

        store.activeTournament$.subscribe(result => {
          expect(result?.id).toBe('test-id');
        });

        flush();
      }));

      it('should set myPlayer when human player found', fakeAsync(() => {
        const humanPlayer = createMockTournamentPlayer({ id: 'human', isBot: false });
        const tournament = createMockTournament({
          registeredPlayers: [humanPlayer]
        });

        store.loadTournament('test-id');
        tick();

        const req = httpMock.expectOne(`${apiUrl}/test-id`);
        req.flush(tournament);

        tick();

        store.myPlayer$.subscribe(player => {
          expect(player?.id).toBe('human');
        });

        flush();
      }));
    });

    describe('registerForTournament', () => {
      it('should register player for tournament', fakeAsync(() => {
        const registeredPlayer = createMockTournamentPlayer({ id: 'new-player', isBot: false });

        store.registerForTournament({ tournamentId: 't1', playerName: 'TestPlayer' });
        tick();

        const registerReq = httpMock.expectOne(`${apiUrl}/t1/register`);
        expect(registerReq.request.method).toBe('POST');
        expect(registerReq.request.body).toEqual({ playerName: 'TestPlayer' });
        registerReq.flush(registeredPlayer);

        tick();


        const loadReq = httpMock.expectOne(`${apiUrl}/t1`);
        const tournamentWithNewPlayer = createMockTournament({
          registeredPlayers: [
            registeredPlayer,
            createMockTournamentPlayer({ id: 'bot-1', name: 'Bot1', isBot: true }),
            createMockTournamentPlayer({ id: 'bot-2', name: 'Bot2', isBot: true })
          ]
        });
        loadReq.flush(tournamentWithNewPlayer);

        tick();

        store.myPlayer$.subscribe(player => {
          expect(player?.id).toBe('new-player');
        });

        flush();
      }));

      it('should set registering state', fakeAsync(() => {
        const registeringStates: boolean[] = [];

        store.isRegistering$.subscribe(registering => {
          registeringStates.push(registering);
        });

        store.registerForTournament({ tournamentId: 't1', playerName: 'Test' });
        tick();

        expect(registeringStates).toContain(true);

        const req = httpMock.expectOne(`${apiUrl}/t1/register`);
        req.flush(createMockTournamentPlayer());

        tick();

        httpMock.expectOne(`${apiUrl}/t1`).flush(createMockTournament());

        tick();
        flush();
      }));
    });

    describe('unregisterFromTournament', () => {
      it('should unregister player from tournament', fakeAsync(() => {
        const myPlayer = createMockTournamentPlayer({ id: 'my-id' });
        store.setMyPlayer(myPlayer);

        store.unregisterFromTournament('t1');
        tick();

        const req = httpMock.expectOne(`${apiUrl}/t1/unregister`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ playerId: 'my-id' });
        req.flush('OK');

        tick();


        const tournamentWithOnlyBots = createMockTournament({
          registeredPlayers: [
            createMockTournamentPlayer({ id: 'bot-1', name: 'Bot1', isBot: true }),
            createMockTournamentPlayer({ id: 'bot-2', name: 'Bot2', isBot: true })
          ]
        });
        httpMock.expectOne(`${apiUrl}/t1`).flush(tournamentWithOnlyBots);

        tick();

        store.myPlayer$.subscribe(player => {
          expect(player).toBeNull();
        });

        flush();
      }));

      it('should not make request if no player set', fakeAsync(() => {
        store.unregisterFromTournament('t1');
        tick();

        httpMock.expectNone(`${apiUrl}/t1/unregister`);

        flush();
      }));
    });
  });




  describe('Helper Methods', () => {
    describe('getMyPlayer', () => {
      it('should return current player', () => {
        const player = createMockTournamentPlayer({ name: 'Test' });
        store.setMyPlayer(player);

        expect(store.getMyPlayer()?.name).toBe('Test');
      });

      it('should return null when no player', () => {
        expect(store.getMyPlayer()).toBeNull();
      });
    });

    describe('getCurrentTournamentId', () => {
      it('should return tournament ID', () => {
        const tournament = createMockTournament({ id: 'test-123' });
        store.setActiveTournament(tournament);

        expect(store.getCurrentTournamentId()).toBe('test-123');
      });

      it('should return null when no tournament', () => {
        expect(store.getCurrentTournamentId()).toBeNull();
      });
    });

    describe('isPlayerRegistered', () => {
      it('should return true when player is set', () => {
        store.setMyPlayer(createMockTournamentPlayer());

        expect(store.isPlayerRegistered()).toBe(true);
      });

      it('should return false when no player', () => {
        expect(store.isPlayerRegistered()).toBe(false);
      });
    });
  });
});





describe('Tournament Model Helpers', () => {
  describe('calculateTimeRemaining', () => {
    it('should calculate positive time remaining', () => {
      const futureTime = Date.now() + 60000;
      const result = calculateTimeRemaining(futureTime);

      expect(result).toBeGreaterThan(59000);
      expect(result).toBeLessThanOrEqual(60000);
    });

    it('should return 0 for past time', () => {
      const pastTime = Date.now() - 1000;
      const result = calculateTimeRemaining(pastTime);

      expect(result).toBe(0);
    });
  });

  describe('formatTimeRemaining', () => {
    it('should format minutes and seconds', () => {
      const result = formatTimeRemaining(125000);

      expect(result).toBe('02:05');
    });

    it('should handle zero', () => {
      const result = formatTimeRemaining(0);

      expect(result).toBe('00:00');
    });

    it('should handle large values', () => {
      const result = formatTimeRemaining(3661000);

      expect(result).toBe('61:01');
    });
  });

  describe('getNextBlindLevel', () => {
    it('should return next level', () => {
      const result = getNextBlindLevel(1, DEFAULT_BLIND_LEVELS);

      expect(result?.level).toBe(2);
      expect(result?.smallBlind).toBe(50);
    });

    it('should return null at max level', () => {
      const result = getNextBlindLevel(12, DEFAULT_BLIND_LEVELS);

      expect(result).toBeNull();
    });

    it('should return null for invalid level', () => {
      const result = getNextBlindLevel(100, DEFAULT_BLIND_LEVELS);

      expect(result).toBeNull();
    });
  });
});
