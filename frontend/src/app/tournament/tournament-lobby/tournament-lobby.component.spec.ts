import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { TournamentLobbyComponent } from './tournament-lobby.component';
import { TournamentStore, TournamentLobbyViewModel } from '../../store/tournament.store';
import { Tournament, TournamentPlayer, BlindLevel, DEFAULT_BLIND_LEVELS, DEFAULT_PRIZE_STRUCTURE } from '../../model/tournament';
import { AuthService } from '../../services/auth.service';

describe('TournamentLobbyComponent', () => {
  let component: TournamentLobbyComponent;
  let fixture: ComponentFixture<TournamentLobbyComponent>;
  let router: Router;
  let mockStore: jasmine.SpyObj<TournamentStore>;
  let mockAuthService: jasmine.SpyObj<AuthService>;

  
  const createMockBlindLevel = (overrides: Partial<BlindLevel> = {}): BlindLevel => ({
    level: overrides.level ?? 1,
    smallBlind: overrides.smallBlind ?? 25,
    bigBlind: overrides.bigBlind ?? 50,
    ante: overrides.ante ?? 0,
    durationMinutes: overrides.durationMinutes ?? 15
  });

  const createMockPlayer = (overrides: Partial<TournamentPlayer> = {}): TournamentPlayer => {
    const player = {
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
      isEliminated: overrides.isEliminated ?? false,
      tablesPlayed: overrides.tablesPlayed ?? 1,
      handsWon: overrides.handsWon ?? 0,
      biggestPot: overrides.biggestPot ?? 0,
      knockouts: overrides.knockouts ?? 0,
      // Player methods
      canAct: () => !player.folded && !player.isAllIn && player.chips > 0,
      getDisplayName: () => player.name.startsWith('Bot') ? player.name.substring(3) || 'Bot' : player.name || 'Anonymous',
      isHuman: () => !player.isBot && !player.name.startsWith('Bot'),
      getStatusText: () => {
        if (player.folded) return 'Folded';
        if (player.isAllIn) return 'All-In';
        if (player.chips === 0) return 'Out';
        return '';
      }
    } as TournamentPlayer;
    return player;
  };

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
    registeredPlayers: overrides.registeredPlayers ?? [],
    remainingPlayers: overrides.remainingPlayers ?? 0,
    totalPlayers: overrides.totalPlayers ?? 0,
    tables: overrides.tables ?? [],
    averageStack: overrides.averageStack ?? 10000,
    largestStack: overrides.largestStack ?? 10000,
    smallestStack: overrides.smallestStack ?? 10000,
    totalChipsInPlay: overrides.totalChipsInPlay ?? 0,
    handsPlayed: overrides.handsPlayed ?? 0,
    prizePool: overrides.prizePool ?? 0,
    createdAt: overrides.createdAt ?? Date.now(),
    updatedAt: overrides.updatedAt ?? Date.now()
  });

  const createMockViewModel = (overrides: Partial<TournamentLobbyViewModel> = {}): TournamentLobbyViewModel => ({
    tournament: overrides.tournament ?? null,
    registeredPlayers: overrides.registeredPlayers ?? [],
    canRegister: overrides.canRegister ?? true,
    isRegistered: overrides.isRegistered ?? false,
    spotsRemaining: overrides.spotsRemaining ?? 9,
    isLoading: overrides.isLoading ?? false,
    error: overrides.error ?? null,
    prizePool: overrides.prizePool ?? 0
  });

  let vmSubject: BehaviorSubject<TournamentLobbyViewModel>;
  let myPlayerSubject: BehaviorSubject<TournamentPlayer | null>;
  let isRegisteringSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    vmSubject = new BehaviorSubject<TournamentLobbyViewModel>(createMockViewModel());
    myPlayerSubject = new BehaviorSubject<TournamentPlayer | null>(null);
    isRegisteringSubject = new BehaviorSubject<boolean>(false);

    mockStore = jasmine.createSpyObj('TournamentStore', [
      'loadTournament',
      'registerForTournament',
      'unregisterFromTournament',
      'subscribeTournamentUpdates'
    ], {
      tournamentLobbyVm$: vmSubject.asObservable(),
      myPlayer$: myPlayerSubject.asObservable(),
      isRegistering$: isRegisteringSubject.asObservable()
    });

    mockAuthService = jasmine.createSpyObj('AuthService', ['getCurrentUserValue']);
    mockAuthService.getCurrentUserValue.and.returnValue({ id: 'user-1', username: 'TestUser' });

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
        TournamentLobbyComponent
      ],
      providers: [
        { provide: TournamentStore, useValue: mockStore },
        { provide: AuthService, useValue: mockAuthService },
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
    .overrideComponent(TournamentLobbyComponent, {
      set: { providers: [{ provide: TournamentStore, useValue: mockStore }] }
    })
    .compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(TournamentLobbyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vmSubject.complete();
    myPlayerSubject.complete();
    isRegisteringSubject.complete();
  });

  
  
  

  describe('Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should load tournament on init', () => {
      expect(mockStore.loadTournament).toHaveBeenCalledWith('test-tournament-id');
    });
  });

  
  
  

  describe('Info Panel', () => {
    it('should display tournament name', () => {
      const tournament = createMockTournament({ name: 'Sunday Special' });
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const nameEl = fixture.nativeElement.querySelector('[data-cy="tournament-name"]');
      expect(nameEl.textContent).toContain('Sunday Special');
    });

    it('should display buy-in amount', () => {
      const tournament = createMockTournament();
      tournament.config.buyIn = 500;
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const buyInEl = fixture.nativeElement.querySelector('[data-cy="buy-in"]');
      expect(buyInEl.textContent).toContain('500');
    });

    it('should display starting chips', () => {
      const tournament = createMockTournament();
      tournament.config.startingChips = 15000;
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const chipsEl = fixture.nativeElement.querySelector('[data-cy="starting-chips"]');
      expect(chipsEl.textContent).toContain('15,000');
    });

    it('should display prize pool', () => {
      const tournament = createMockTournament({ prizePool: 2500 });
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const prizeEl = fixture.nativeElement.querySelector('[data-cy="prize-pool"]');
      expect(prizeEl.textContent).toContain('2,500');
    });

    it('should display level duration', () => {
      const tournament = createMockTournament();
      tournament.config.levelDurationMinutes = 20;
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const durationEl = fixture.nativeElement.querySelector('[data-cy="level-duration"]');
      expect(durationEl.textContent).toContain('20');
    });
  });

  
  
  

  describe('Player List', () => {
    it('should display registered players', () => {
      const players = [
        createMockPlayer({ id: 'p1', name: 'Player1' }),
        createMockPlayer({ id: 'p2', name: 'Player2' }),
        createMockPlayer({ id: 'p3', name: 'Player3' })
      ];
      const tournament = createMockTournament({ registeredPlayers: players });

      vmSubject.next(createMockViewModel({
        tournament,
        registeredPlayers: players,
        isLoading: false
      }));
      fixture.detectChanges();

      const playerItems = fixture.nativeElement.querySelectorAll('.player-item');
      expect(playerItems.length).toBe(3);
    });

    it('should show player count', () => {
      const players = [
        createMockPlayer({ id: 'p1' }),
        createMockPlayer({ id: 'p2' })
      ];
      const tournament = createMockTournament({ registeredPlayers: players });
      
      vmSubject.next(createMockViewModel({ 
        tournament,
        registeredPlayers: players,
        spotsRemaining: 7
      }));
      fixture.detectChanges();

      const countEl = fixture.nativeElement.querySelector('[data-cy="player-count"]');
      expect(countEl.textContent).toContain('2');
    });

    it('should highlight current player with "You" badge', () => {
      const myPlayer = createMockPlayer({ id: 'me', name: 'MyName', isBot: false });
      const players = [
        createMockPlayer({ id: 'p1', name: 'OtherPlayer' }),
        myPlayer
      ];
      const tournament = createMockTournament({ registeredPlayers: players });

      myPlayerSubject.next(myPlayer);
      vmSubject.next(createMockViewModel({
        tournament,
        registeredPlayers: players,
        isRegistered: true
      }));
      fixture.detectChanges();

      const youBadge = fixture.nativeElement.querySelector('[data-cy="you-badge"]');
      expect(youBadge).toBeTruthy();
    });

    it('should show empty state when no players', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament(),
        registeredPlayers: []
      }));
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('[data-cy="no-players"]');
      expect(emptyState).toBeTruthy();
    });
  });

  
  
  

  describe('Blind Structure', () => {
    it('should display blind levels table', () => {
      const tournament = createMockTournament();
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const blindTable = fixture.nativeElement.querySelector('[data-cy="blind-structure"]');
      expect(blindTable).toBeTruthy();
    });

    it('should highlight current blind level', () => {
      const tournament = createMockTournament({ 
        currentLevel: 3,
        status: 'RUNNING'
      });
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const currentLevel = fixture.nativeElement.querySelector('[data-cy="blind-level-3"].current');
      expect(currentLevel).toBeTruthy();
    });

    it('should show break indicators', () => {
      const tournament = createMockTournament();
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const breakIndicator = fixture.nativeElement.querySelector('[data-cy="break-indicator"]');
      expect(breakIndicator).toBeTruthy();
    });
  });

  
  
  

  describe('Prize Structure', () => {
    it('should display prize distribution', () => {
      const tournament = createMockTournament({ prizePool: 1000 });
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const prizeStructure = fixture.nativeElement.querySelector('[data-cy="prize-structure"]');
      expect(prizeStructure).toBeTruthy();
    });

    it('should calculate prize amounts correctly', () => {
      const tournament = createMockTournament({ prizePool: 1000 });
      
      vmSubject.next(createMockViewModel({ tournament }));
      fixture.detectChanges();

      const firstPrize = fixture.nativeElement.querySelector('[data-cy="prize-1st"]');
      expect(firstPrize.textContent).toContain('500');
    });
  });

  
  
  

  describe('Registration', () => {
    it('should show register button when can register', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament(),
        canRegister: true,
        isRegistered: false
      }));
      fixture.detectChanges();

      const registerBtn = fixture.nativeElement.querySelector('[data-cy="register-btn"]');
      expect(registerBtn).toBeTruthy();
    });

    it('should show unregister button when registered', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament(),
        isRegistered: true
      }));
      fixture.detectChanges();

      const unregisterBtn = fixture.nativeElement.querySelector('[data-cy="unregister-btn"]');
      expect(unregisterBtn).toBeTruthy();
    });

    it('should disable register button when registering', () => {
      vmSubject.next(createMockViewModel({
        tournament: createMockTournament(),
        canRegister: true
      }));
      isRegisteringSubject.next(true);
      fixture.detectChanges();

      const registerBtn = fixture.nativeElement.querySelector('[data-cy="register-btn"]');
      expect(registerBtn.disabled).toBe(true);
    });

    it('should call registerForTournament on register click', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament({ id: 'test-id' }),
        canRegister: true,
        isRegistered: false
      }));
      fixture.detectChanges();

      const registerBtn = fixture.nativeElement.querySelector('[data-cy="register-btn"]');
      registerBtn.click();

      expect(mockStore.registerForTournament).toHaveBeenCalled();
    });

    it('should call unregisterFromTournament on unregister click', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament({ id: 'test-id' }),
        isRegistered: true
      }));
      fixture.detectChanges();

      const unregisterBtn = fixture.nativeElement.querySelector('[data-cy="unregister-btn"]');
      unregisterBtn.click();

      expect(mockStore.unregisterFromTournament).toHaveBeenCalledWith('test-id');
    });

    it('should show spots remaining', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament(),
        spotsRemaining: 4
      }));
      fixture.detectChanges();

      const spotsEl = fixture.nativeElement.querySelector('[data-cy="spots-remaining"]');
      expect(spotsEl.textContent).toContain('4');
    });

    it('should show tournament full message when no spots', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament(),
        canRegister: false,
        spotsRemaining: 0
      }));
      fixture.detectChanges();

      const fullMsg = fixture.nativeElement.querySelector('[data-cy="tournament-full"]');
      expect(fullMsg).toBeTruthy();
    });
  });

  
  
  

  describe('Navigation', () => {
    it('should show "Go to Table" button when tournament running and registered', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament({ status: 'RUNNING' }),
        isRegistered: true
      }));
      fixture.detectChanges();

      const goToTableBtn = fixture.nativeElement.querySelector('[data-cy="go-to-table-btn"]');
      expect(goToTableBtn).toBeTruthy();
    });

    it('should navigate to table on "Go to Table" click', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament({ id: 'nav-test', status: 'RUNNING' }),
        isRegistered: true
      }));
      fixture.detectChanges();

      const goToTableBtn = fixture.nativeElement.querySelector('[data-cy="go-to-table-btn"]');
      goToTableBtn.click();

      expect(router.navigate).toHaveBeenCalledWith(['/tournament', 'nav-test', 'play']);
    });

    it('should navigate back on back button click', () => {
      vmSubject.next(createMockViewModel({
        tournament: createMockTournament()
      }));
      fixture.detectChanges();

      const backBtn = fixture.nativeElement.querySelector('[data-cy="back-btn"]');
      backBtn.click();

      expect(router.navigate).toHaveBeenCalledWith(['/tournaments']);
    });
  });

  
  
  

  describe('Loading & Error States', () => {
    it('should show loading indicator', () => {
      vmSubject.next(createMockViewModel({ isLoading: true }));
      fixture.detectChanges();

      const loadingEl = fixture.nativeElement.querySelector('[data-cy="loading"]');
      expect(loadingEl).toBeTruthy();
    });

    it('should show error message', () => {
      vmSubject.next(createMockViewModel({ error: 'Failed to load tournament' }));
      fixture.detectChanges();

      const errorEl = fixture.nativeElement.querySelector('[data-cy="error-message"]');
      expect(errorEl).toBeTruthy();
      expect(errorEl.textContent).toContain('Failed to load tournament');
    });

    it('should show not found when no tournament', () => {
      vmSubject.next(createMockViewModel({ tournament: null, isLoading: false }));
      fixture.detectChanges();

      const notFoundEl = fixture.nativeElement.querySelector('[data-cy="not-found"]');
      expect(notFoundEl).toBeTruthy();
    });
  });

  
  
  

  describe('Status Display', () => {
    it('should show REGISTERING status', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament({ status: 'REGISTERING' })
      }));
      fixture.detectChanges();

      const statusEl = fixture.nativeElement.querySelector('[data-cy="status-badge"]');
      expect(statusEl.textContent).toContain('Registration Open');
    });

    it('should show RUNNING status', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament({ status: 'RUNNING' })
      }));
      fixture.detectChanges();

      const statusEl = fixture.nativeElement.querySelector('[data-cy="status-badge"]');
      expect(statusEl.textContent).toContain('In Progress');
    });

    it('should show FINISHED status', () => {
      vmSubject.next(createMockViewModel({ 
        tournament: createMockTournament({ status: 'FINISHED' })
      }));
      fixture.detectChanges();

      const statusEl = fixture.nativeElement.querySelector('[data-cy="status-badge"]');
      expect(statusEl.textContent).toContain('Finished');
    });
  });





  describe('Cleanup', () => {
    it('should clean up on destroy', () => {
      component.ngOnDestroy();
      // Component cleans up internal subscriptions
      expect(component).toBeTruthy();
    });
  });
});
