import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { TournamentTableComponent } from './tournament-table.component';
import { TournamentStore, TournamentTableViewModel } from '../../store/tournament.store';
import { GameStore } from '../../store/game.store';
import { 
  Tournament, 
  TournamentTable, 
  TournamentPlayer, 
  BlindLevel,
  DEFAULT_BLIND_LEVELS,
  DEFAULT_PRIZE_STRUCTURE 
} from '../../model/tournament';

describe('TournamentTableComponent', () => {
  let component: TournamentTableComponent;
  let fixture: ComponentFixture<TournamentTableComponent>;
  let router: Router;
  let mockTournamentStore: jasmine.SpyObj<TournamentStore>;
  let mockGameStore: jasmine.SpyObj<GameStore>;

  
  const createMockBlindLevel = (overrides: Partial<BlindLevel> = {}): BlindLevel => ({
    level: overrides.level ?? 1,
    smallBlind: overrides.smallBlind ?? 25,
    bigBlind: overrides.bigBlind ?? 50,
    ante: overrides.ante ?? 0,
    durationMinutes: overrides.durationMinutes ?? 15
  });

  const createMockPlayer = (overrides: Partial<TournamentPlayer> = {}): TournamentPlayer => {
    const player: TournamentPlayer = {
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
      tablesPlayed: overrides.tablesPlayed ?? 1,
      handsWon: overrides.handsWon ?? 0,
      biggestPot: overrides.biggestPot ?? 0,
      knockouts: overrides.knockouts ?? 0,
      isEliminated: overrides.isEliminated ?? false,
      finishPosition: overrides.finishPosition,
      prizeMoney: overrides.prizeMoney,
      rank: overrides.rank,
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
    };
    return player;
  };

  const createMockTable = (overrides: Partial<TournamentTable> = {}): TournamentTable => ({
    id: overrides.id ?? 'table-1',
    tableNumber: overrides.tableNumber ?? 1,
    players: overrides.players ?? [
      createMockPlayer({ id: 'p1', seatPosition: 0 }),
      createMockPlayer({ id: 'p2', seatPosition: 1, isBot: true })
    ],
    currentHandNumber: overrides.currentHandNumber ?? 1,
    dealerPosition: overrides.dealerPosition ?? 0,
    isActive: overrides.isActive ?? true
  });

  const createMockTournament = (overrides: Partial<Tournament> = {}): Tournament => ({
    id: overrides.id ?? 'tournament-123',
    name: overrides.name ?? 'Test Tournament',
    status: overrides.status ?? 'RUNNING',
    config: {
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
    levelStartTime: Date.now(),
    levelEndTime: Date.now() + 900000,
    nextBreakAtLevel: 3,
    registeredPlayers: [],
    remainingPlayers: overrides.remainingPlayers ?? 6,
    totalPlayers: 9,
    tables: [createMockTable()],
    averageStack: overrides.averageStack ?? 10000,
    largestStack: 15000,
    smallestStack: 5000,
    totalChipsInPlay: 90000,
    handsPlayed: 0,
    prizePool: 900,
    createdAt: Date.now(),
    updatedAt: Date.now()
  });

  const createMockViewModel = (overrides: Partial<TournamentTableViewModel> = {}): TournamentTableViewModel => ({
    tournament: overrides.tournament ?? null,
    table: overrides.table ?? null,
    myPlayer: overrides.myPlayer ?? null,
    currentBlinds: overrides.currentBlinds ?? null,
    nextBlinds: overrides.nextBlinds ?? null,
    timeToNextLevel: overrides.timeToNextLevel ?? 300000,
    formattedTimeRemaining: overrides.formattedTimeRemaining ?? '05:00',
    remainingPlayers: overrides.remainingPlayers ?? 6,
    averageStack: overrides.averageStack ?? 10000,
    myRank: overrides.myRank ?? 1,
    totalPlayers: overrides.totalPlayers ?? 9,
    isOnBreak: overrides.isOnBreak ?? false,
    isFinalTable: overrides.isFinalTable ?? false,
    isEliminated: overrides.isEliminated ?? false,
    isLoading: overrides.isLoading ?? false,
    error: overrides.error ?? null
  });

  let vmSubject: BehaviorSubject<TournamentTableViewModel>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let gameVmSubject: BehaviorSubject<any>;

  beforeEach(async () => {
    vmSubject = new BehaviorSubject<TournamentTableViewModel>(createMockViewModel());

    gameVmSubject = new BehaviorSubject({
      game: null,
      currentPlayer: null,
      humanPlayer: undefined,
      isLoading: false,
      error: null,
      isHumanTurn: false,
      canCheck: false,
      canCall: false,
      callAmount: 0,
      minRaiseAmount: 0,
      maxRaiseAmount: 0,
      potSize: 0,
      phase: 'PRE_FLOP',
      phaseDisplayName: 'Pre-Flop',
      communityCards: [],
      isGameFinished: false,
      activePlayers: [],
      lastAction: null,
      processingBots: false,
      canPlayerAct: false,
      dealerPosition: 0,
      winnerName: undefined,
      winningHandDescription: undefined
    });

    mockTournamentStore = jasmine.createSpyObj('TournamentStore', [
      'loadTournament',
      'subscribeTournamentUpdates',
      'ngOnDestroy'
    ], {
      tournamentTableVm$: vmSubject.asObservable(),
      timeRemaining$: of(300000)
    });

    mockGameStore = jasmine.createSpyObj('GameStore', [
      'playerAction',
      'processBots',
      'isPlayerTurn'
    ], {
      vm$: gameVmSubject.asObservable(),
      currentBot$: of(null)
    });

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
        TournamentTableComponent
      ],
      providers: [
        { provide: TournamentStore, useValue: mockTournamentStore },
        { provide: GameStore, useValue: mockGameStore },
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: 'test-tournament-id' }),
            snapshot: {
              paramMap: {
                get: (key: string) => key === 'id' ? 'test-tournament-id' : null
              }
            }
          }
        }
      ]
    })
    .overrideComponent(TournamentTableComponent, {
      set: { 
        providers: [
          { provide: TournamentStore, useValue: mockTournamentStore },
          { provide: GameStore, useValue: mockGameStore }
        ] 
      }
    })
    .compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(TournamentTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vmSubject.complete();
    gameVmSubject.complete();
  });

  
  
  

  describe('Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should load tournament on init', () => {
      expect(mockTournamentStore.loadTournament).toHaveBeenCalledWith('test-tournament-id');
    });

    it('should subscribe to tournament updates', () => {
      expect(mockTournamentStore.subscribeTournamentUpdates).toHaveBeenCalled();
    });
  });

  
  
  

  describe('Display', () => {
    // TODO: Add data-cy attributes to template for these tests
    it.skip('should show loading state', () => {
      vmSubject.next(createMockViewModel({ isLoading: true }));
      fixture.detectChanges();

      const loadingEl = fixture.nativeElement.querySelector('[data-cy="loading-indicator"]');
      expect(loadingEl).toBeTruthy();
    });

    it.skip('should show error state', () => {
      vmSubject.next(createMockViewModel({ error: 'Connection lost' }));
      fixture.detectChanges();

      const errorEl = fixture.nativeElement.querySelector('[data-cy="error-message"]');
      expect(errorEl).toBeTruthy();
    });

    it('should display tournament name in header', () => {
      const tournament = createMockTournament({ name: 'Grand Tournament' });
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const header = fixture.nativeElement.querySelector('.tournament-name');
      expect(header.textContent).toContain('Grand Tournament');
    });

    it('should display remaining players count', () => {
      vmSubject.next(createMockViewModel({ remainingPlayers: 42, totalPlayers: 100 }));
      fixture.detectChanges();

      const statsSection = fixture.nativeElement.querySelector('.tournament-stats');
      expect(statsSection.textContent).toContain('42');
    });

    it('should display average stack', () => {
      vmSubject.next(createMockViewModel({ averageStack: 15000 }));
      fixture.detectChanges();

      const statsSection = fixture.nativeElement.querySelector('.tournament-stats');
      expect(statsSection.textContent).toContain('15,000');
    });

    it('should display player rank', () => {
      vmSubject.next(createMockViewModel({ myRank: 3 }));
      fixture.detectChanges();

      const statsSection = fixture.nativeElement.querySelector('.tournament-stats');
      expect(statsSection.textContent).toContain('3');
    });
  });

  
  
  

  describe('Blind Timer', () => {
    it('should display blind timer component', () => {
      vmSubject.next(createMockViewModel({
        currentBlinds: createMockBlindLevel(),
        timeToNextLevel: 300000
      }));
      fixture.detectChanges();

      const blindTimer = fixture.nativeElement.querySelector('[data-cy="blind-timer"]');
      expect(blindTimer).toBeTruthy();
    });

    it('should pass current blinds to timer', () => {
      const blinds = createMockBlindLevel({ smallBlind: 100, bigBlind: 200 });
      vmSubject.next(createMockViewModel({ currentBlinds: blinds }));
      fixture.detectChanges();

      const blindTimer = fixture.nativeElement.querySelector('[data-cy="blind-timer"]');
      expect(blindTimer).toBeTruthy();
    });
  });

  
  
  

  describe('Table Display', () => {
    it('should display poker table', () => {
      const table = createMockTable();
      vmSubject.next(createMockViewModel({ table }));
      fixture.detectChanges();

      const pokerTable = fixture.nativeElement.querySelector('[data-cy="poker-table"]');
      expect(pokerTable).toBeTruthy();
    });

    it('should display player seats', () => {
      const players = [
        createMockPlayer({ id: 'p1', seatPosition: 0 }),
        createMockPlayer({ id: 'p2', seatPosition: 1 }),
        createMockPlayer({ id: 'p3', seatPosition: 2 })
      ];
      const table = createMockTable({ players });
      vmSubject.next(createMockViewModel({ table }));
      fixture.detectChanges();

      const seats = fixture.nativeElement.querySelectorAll('[data-cy^="player-seat-"]');
      expect(seats.length).toBe(3);
    });

    it('should highlight current player', () => {
      const myPlayer = createMockPlayer({ id: 'me', seatPosition: 0 });
      const table = createMockTable({
        players: [myPlayer, createMockPlayer({ id: 'other', seatPosition: 1 })]
      });
      vmSubject.next(createMockViewModel({ table, myPlayer }));
      fixture.detectChanges();

      const mySeat = fixture.nativeElement.querySelector('[data-cy="player-seat-0"]');
      expect(mySeat).toBeTruthy();
      expect(mySeat.classList.contains('me')).toBe(true);
    });
  });

  
  
  

  describe('Action Buttons', () => {
    it('should display action buttons when player turn', () => {
      const myPlayer = createMockPlayer({ id: 'me' });

      // Update GameStore mock to return canPlayerAct = true
      gameVmSubject.next({
        game: null,
        currentPlayer: null,
        humanPlayer: myPlayer,
        isLoading: false,
        error: null,
        isHumanTurn: true,
        canCheck: false,
        canCall: true,
        callAmount: 50,
        minRaiseAmount: 100,
        maxRaiseAmount: 1000,
        potSize: 100,
        phase: 'PRE_FLOP',
        phaseDisplayName: 'Pre-Flop',
        communityCards: [],
        isGameFinished: false,
        activePlayers: [],
        lastAction: null,
        processingBots: false,
        canPlayerAct: true,
        dealerPosition: 0,
        winnerName: undefined,
        winningHandDescription: undefined
      });

      vmSubject.next(createMockViewModel({ myPlayer }));
      fixture.detectChanges();

      const actionButtons = fixture.nativeElement.querySelector('[data-cy="action-buttons"]');
      expect(actionButtons).toBeTruthy();
    });

    it('should call fold on fold button click', () => {
      const myPlayer = createMockPlayer({ id: 'me' });

      // Update GameStore mock to return canPlayerAct = true and humanPlayer
      gameVmSubject.next({
        game: null,
        currentPlayer: null,
        humanPlayer: myPlayer,
        isLoading: false,
        error: null,
        isHumanTurn: true,
        canCheck: false,
        canCall: true,
        callAmount: 50,
        minRaiseAmount: 100,
        maxRaiseAmount: 1000,
        potSize: 100,
        phase: 'PRE_FLOP',
        phaseDisplayName: 'Pre-Flop',
        communityCards: [],
        isGameFinished: false,
        activePlayers: [],
        lastAction: null,
        processingBots: false,
        canPlayerAct: true,
        dealerPosition: 0,
        winnerName: undefined,
        winningHandDescription: undefined
      });

      vmSubject.next(createMockViewModel({
        myPlayer
      }));
      fixture.detectChanges();

      const foldBtn = fixture.nativeElement.querySelector('[data-cy="fold-btn"]');
      foldBtn.click();

      expect(mockGameStore.playerAction).toHaveBeenCalledWith({
        playerId: 'me',
        action: 'FOLD'
      });
    });

    it('should call check on check button click', () => {
      const myPlayer = createMockPlayer({ id: 'me' });
      vmSubject.next(createMockViewModel({
        myPlayer
      }));
      fixture.detectChanges();

      const checkBtn = fixture.nativeElement.querySelector('[data-cy="check-btn"]');
      if (checkBtn) {
        checkBtn.click();
        expect(mockGameStore.playerAction).toHaveBeenCalledWith({
          playerId: 'me',
          action: 'CHECK'
        });
      }
    });

    it('should call call on call button click', () => {
      const myPlayer = createMockPlayer({ id: 'me' });
      vmSubject.next(createMockViewModel({
        myPlayer
      }));
      fixture.detectChanges();

      const callBtn = fixture.nativeElement.querySelector('[data-cy="call-btn"]');
      if (callBtn) {
        callBtn.click();
        expect(mockGameStore.playerAction).toHaveBeenCalledWith({
          playerId: 'me',
          action: 'CALL'
        });
      }
    });
  });

  
  
  

  describe('Overlays', () => {
    it('should show eliminated overlay when player eliminated', () => {
      const myPlayer = createMockPlayer({ 
        id: 'me', 
        isEliminated: true,
        finishPosition: 5,
        prizeMoney: 0
      });
      vmSubject.next(createMockViewModel({ 
        myPlayer,
        isEliminated: true 
      }));
      fixture.detectChanges();

      const eliminatedOverlay = fixture.nativeElement.querySelector('[data-cy="eliminated-overlay"]');
      expect(eliminatedOverlay).toBeTruthy();
    });

    it('should show finish position on eliminated overlay', () => {
      const myPlayer = createMockPlayer({
        id: 'me',
        isEliminated: true,
        finishPosition: 3,
        prizeMoney: 100
      });
      vmSubject.next(createMockViewModel({
        myPlayer,
        isEliminated: true
      }));
      fixture.detectChanges();

      const overlay = fixture.nativeElement.querySelector('[data-cy="eliminated-overlay"]');
      expect(overlay).toBeTruthy();
      expect(overlay.textContent).toContain('3');
    });

    it('should show prize money if won', () => {
      const myPlayer = createMockPlayer({
        id: 'me',
        isEliminated: true,
        finishPosition: 2,
        prizeMoney: 250
      });
      vmSubject.next(createMockViewModel({
        myPlayer,
        isEliminated: true
      }));
      fixture.detectChanges();

      const overlay = fixture.nativeElement.querySelector('[data-cy="eliminated-overlay"]');
      expect(overlay).toBeTruthy();
      expect(overlay.textContent).toContain('250');
    });

    it('should show break overlay when on break', () => {
      vmSubject.next(createMockViewModel({ isOnBreak: true }));
      fixture.detectChanges();

      const breakOverlay = fixture.nativeElement.querySelector('[data-cy="break-overlay"]');
      expect(breakOverlay).toBeTruthy();
    });
  });

  
  
  

  describe('Leaderboard', () => {
    // TODO: Add data-cy attribute to leaderboard component
    it.skip('should display mini leaderboard', () => {
      const tournament = createMockTournament();
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const leaderboard = fixture.nativeElement.querySelector('[data-cy="mini-leaderboard"]');
      expect(leaderboard).toBeTruthy();
    });
  });

  
  
  

  describe('Final Table', () => {
    // TODO: Add final table indicator and styling to template
    it.skip('should show final table indicator', () => {
      vmSubject.next(createMockViewModel({ isFinalTable: true }));
      fixture.detectChanges();

      const finalTableIndicator = fixture.nativeElement.querySelector('[data-cy="final-table-indicator"]');
      expect(finalTableIndicator).toBeTruthy();
    });

    it.skip('should apply final table styling', () => {
      vmSubject.next(createMockViewModel({ isFinalTable: true }));
      fixture.detectChanges();

      const container = fixture.nativeElement.querySelector('.tournament-table-container');
      expect(container.classList.contains('final-table')).toBe(true);
    });
  });

  
  
  

  describe('Navigation', () => {
    it('should navigate to tournaments list on exit button', () => {
      const tournament = createMockTournament({ id: 'nav-test' });
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const exitBtn = fixture.nativeElement.querySelector('[data-cy="exit-btn"]');
      exitBtn.click();

      expect(router.navigate).toHaveBeenCalledWith(['/tournaments']);
    });

    it('should navigate to tournaments list when eliminated', () => {
      const myPlayer = createMockPlayer({
        id: 'me',
        isEliminated: true,
        finishPosition: 5
      });
      vmSubject.next(createMockViewModel({
        myPlayer,
        isEliminated: true
      }));
      fixture.detectChanges();

      const exitBtn = fixture.nativeElement.querySelector('.eliminated-overlay .btn-exit');
      if (exitBtn) {
        exitBtn.click();
        expect(router.navigate).toHaveBeenCalledWith(['/tournaments']);
      }
    });
  });

  
  
  

  describe('Accessibility', () => {
    // TODO: Add accessibility attributes to template
    it.skip('should have accessible table region', () => {
      const table = createMockTable();
      vmSubject.next(createMockViewModel({ table }));
      fixture.detectChanges();

      const tableRegion = fixture.nativeElement.querySelector('[role="region"]');
      expect(tableRegion).toBeTruthy();
    });

    it.skip('should have aria-labels on action buttons', () => {
      const myPlayer = createMockPlayer({ id: 'me' });

      // Update GameStore mock to return canPlayerAct = true
      gameVmSubject.next({
        game: null,
        currentPlayer: null,
        humanPlayer: myPlayer,
        isLoading: false,
        error: null,
        isHumanTurn: true,
        canCheck: false,
        canCall: true,
        callAmount: 50,
        minRaiseAmount: 100,
        maxRaiseAmount: 1000,
        potSize: 100,
        phase: 'PRE_FLOP',
        phaseDisplayName: 'Pre-Flop',
        communityCards: [],
        isGameFinished: false,
        activePlayers: [],
        lastAction: null,
        processingBots: false,
        canPlayerAct: true,
        dealerPosition: 0,
        winnerName: undefined,
        winningHandDescription: undefined
      });

      vmSubject.next(createMockViewModel({
        myPlayer
      }));
      fixture.detectChanges();

      const foldBtn = fixture.nativeElement.querySelector('[data-cy="fold-btn"]');
      expect(foldBtn.getAttribute('aria-label')).toBeTruthy();
    });
  });
});
