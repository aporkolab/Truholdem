import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { TournamentListComponent } from './tournament-list.component';
import { TournamentStore, TournamentListViewModel } from '../../store/tournament.store';
import { TournamentListItem } from '../../model/tournament';

describe('TournamentListComponent', () => {
  let component: TournamentListComponent;
  let fixture: ComponentFixture<TournamentListComponent>;
  let router: Router;
  let mockStore: jasmine.SpyObj<TournamentStore>;

  
  const createMockTournamentListItem = (overrides: Partial<TournamentListItem> = {}): TournamentListItem => ({
    id: overrides.id ?? 'tournament-1',
    name: overrides.name ?? 'Test Tournament',
    status: overrides.status ?? 'REGISTERING',
    buyIn: overrides.buyIn ?? 100,
    startingChips: overrides.startingChips ?? 10000,
    currentLevel: overrides.currentLevel ?? 1,
    registeredCount: overrides.registeredCount ?? 5,
    maxPlayers: overrides.maxPlayers ?? 9,
    prizePool: overrides.prizePool ?? 500,
    smallBlind: overrides.smallBlind ?? 25,
    bigBlind: overrides.bigBlind ?? 50
  });

  const createMockViewModel = (overrides: Partial<TournamentListViewModel> = {}): TournamentListViewModel => ({
    tournaments: overrides.tournaments ?? [],
    openTournaments: overrides.openTournaments ?? [],
    runningTournaments: overrides.runningTournaments ?? [],
    isLoading: overrides.isLoading ?? false,
    error: overrides.error ?? null
  });

  
  let vmSubject: BehaviorSubject<TournamentListViewModel>;

  beforeEach(async () => {
    vmSubject = new BehaviorSubject<TournamentListViewModel>(createMockViewModel());

    const storeSpy = jasmine.createSpyObj('TournamentStore', [
      'loadTournaments',
      'ngOnDestroy'
    ]);

    Object.defineProperty(storeSpy, 'tournamentListVm$', {
      get: () => vmSubject.asObservable(),
      configurable: true
    });

    mockStore = storeSpy;

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([]),
        NoopAnimationsModule,
        TournamentListComponent
      ],
      providers: [
        { provide: TournamentStore, useValue: mockStore }
      ]
    })
    .overrideComponent(TournamentListComponent, {
      set: { providers: [{ provide: TournamentStore, useValue: mockStore }] }
    })
    .compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(TournamentListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vmSubject.complete();
  });

  
  
  

  describe('Creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should load tournaments on init', () => {
      expect(mockStore.loadTournaments).toHaveBeenCalled();
    });
  });

  
  
  

  describe('Display', () => {
    it('should show loading state', () => {
      vmSubject.next(createMockViewModel({ isLoading: true }));
      fixture.detectChanges();

      const loadingEl = fixture.nativeElement.querySelector('[data-cy="loading-indicator"]');
      expect(loadingEl).toBeTruthy();
    });

    it('should show error message', () => {
      vmSubject.next(createMockViewModel({ error: 'Test error message' }));
      fixture.detectChanges();

      const errorEl = fixture.nativeElement.querySelector('[data-cy="error-message"]');
      expect(errorEl).toBeTruthy();
      expect(errorEl.textContent).toContain('Test error message');
    });

    it('should display open tournaments section', () => {
      const openTournaments = [
        createMockTournamentListItem({ id: 't1', status: 'REGISTERING' }),
        createMockTournamentListItem({ id: 't2', status: 'REGISTERING' })
      ];

      vmSubject.next(createMockViewModel({ 
        openTournaments,
        tournaments: openTournaments 
      }));
      fixture.detectChanges();

      const section = fixture.nativeElement.querySelector('[data-cy="open-tournaments-section"]');
      expect(section).toBeTruthy();
      
      const cards = fixture.nativeElement.querySelectorAll('[data-cy="tournament-card"]');
      expect(cards.length).toBe(2);
    });

    it('should display running tournaments section', () => {
      const runningTournaments = [
        createMockTournamentListItem({ id: 't1', status: 'RUNNING' })
      ];

      vmSubject.next(createMockViewModel({ 
        runningTournaments,
        tournaments: runningTournaments 
      }));
      fixture.detectChanges();

      const section = fixture.nativeElement.querySelector('[data-cy="running-tournaments-section"]');
      expect(section).toBeTruthy();
    });

    it('should show empty state when no open tournaments', () => {
      vmSubject.next(createMockViewModel({ openTournaments: [] }));
      fixture.detectChanges();

      const emptyState = fixture.nativeElement.querySelector('[data-cy="open-tournaments-section"] .empty-state');
      expect(emptyState).toBeTruthy();
    });
  });

  
  
  

  describe('Interactions', () => {
    it('should refresh tournaments on button click', () => {
      const initialCallCount = (mockStore.loadTournaments as unknown as jest.Mock).mock.calls.length;

      const refreshBtn = fixture.nativeElement.querySelector('[data-cy="refresh-btn"]');
      refreshBtn.click();

      expect(mockStore.loadTournaments).toHaveBeenCalledTimes(initialCallCount + 1);
    });

    it('should retry on error retry button click', () => {
      vmSubject.next(createMockViewModel({ error: 'Error' }));
      fixture.detectChanges();

      const initialCallCount = (mockStore.loadTournaments as unknown as jest.Mock).mock.calls.length;

      const retryBtn = fixture.nativeElement.querySelector('[data-cy="retry-btn"]');
      retryBtn.click();

      expect(mockStore.loadTournaments).toHaveBeenCalledTimes(initialCallCount + 1);
    });

    it('should navigate to tournament on register', () => {
      const tournament = createMockTournamentListItem({ id: 'nav-test' });

      component.onRegister(tournament);

      expect(router.navigate).toHaveBeenCalledWith(['/tournament', 'nav-test']);
    });

    it('should navigate to tournament on view details', () => {
      const tournament = createMockTournamentListItem({ id: 'details-test' });

      component.onViewDetails(tournament);

      expect(router.navigate).toHaveBeenCalledWith(['/tournament', 'details-test']);
    });
  });

  
  
  

  describe('Signals', () => {
    it('should update isLoading signal', () => {
      vmSubject.next(createMockViewModel({ isLoading: true }));
      fixture.detectChanges();

      expect(component.isLoading()).toBe(true);

      vmSubject.next(createMockViewModel({ isLoading: false }));
      fixture.detectChanges();

      expect(component.isLoading()).toBe(false);
    });

    it('should update error signal', () => {
      vmSubject.next(createMockViewModel({ error: 'New error' }));
      fixture.detectChanges();

      expect(component.error()).toBe('New error');
    });

    it('should update tournaments signal', () => {
      const tournaments = [createMockTournamentListItem()];
      vmSubject.next(createMockViewModel({ tournaments }));
      fixture.detectChanges();

      expect(component.tournaments().length).toBe(1);
    });
  });

  
  
  

  describe('Accessibility', () => {
    it('should have proper ARIA role for error', () => {
      vmSubject.next(createMockViewModel({ error: 'Error' }));
      fixture.detectChanges();

      const errorEl = fixture.nativeElement.querySelector('[data-cy="error-message"]');
      expect(errorEl.getAttribute('role')).toBe('alert');
    });

    it('should have accessible title', () => {
      const title = fixture.nativeElement.querySelector('[data-cy="list-title"]');
      expect(title).toBeTruthy();
      expect(title.textContent).toContain('Tournaments');
    });
  });

  
  
  

  describe('Auto-refresh', () => {
    it('should set up auto-refresh on init', fakeAsync(() => {
      // Use Jest's fake timers in addition to fakeAsync
      jest.useFakeTimers();

      // Clear previous calls and create a fresh component inside fakeAsync zone
      (mockStore.loadTournaments as unknown as jest.Mock).mockClear();

      // Create component inside fakeAsync to ensure interval is tracked
      const testFixture = TestBed.createComponent(TournamentListComponent);
      testFixture.detectChanges();

      // Verify initial call from ngOnInit
      expect(mockStore.loadTournaments).toHaveBeenCalledTimes(1);

      // Advance time by 30 seconds to trigger the first interval emission
      jest.advanceTimersByTime(30000);
      tick(30000);

      // Verify loadTournaments was called again
      expect(mockStore.loadTournaments).toHaveBeenCalledTimes(2);

      // Cleanup
      testFixture.destroy();
      jest.useRealTimers();
    }));
  });
});
