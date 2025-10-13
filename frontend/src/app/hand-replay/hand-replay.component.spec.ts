import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { HandReplayComponent } from './hand-replay.component';
import { HandHistoryService, ReplayData, ReplayState, ReplayAction } from '../services/hand-history.service';
import { of, throwError, BehaviorSubject, Observable } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

describe('HandReplayComponent', () => {
  let component: HandReplayComponent;
  let fixture: ComponentFixture<HandReplayComponent>;
  let historyServiceMock: Partial<HandHistoryService>;
  let replayIndexSubject: BehaviorSubject<number>;

  const createMockReplayAction = (overrides: Partial<ReplayAction> = {}): ReplayAction => ({
    playerName: overrides.playerName || 'TestPlayer',
    action: overrides.action || 'CALL',
    amount: overrides.amount ?? 100,
    phase: overrides.phase || 'PRE_FLOP'
  });

  const createMockReplayState = (overrides: Partial<ReplayState> = {}): ReplayState => ({
    actionIndex: overrides.actionIndex ?? 0,
    phase: overrides.phase || 'PRE_FLOP',
    pot: overrides.pot ?? 100,
    board: overrides.board || [],
    currentBet: overrides.currentBet ?? 50,
    totalActions: overrides.totalActions ?? 5,
    isComplete: overrides.isComplete ?? false,
    players: overrides.players || [
      { name: 'Player1', chips: 1000, bet: 50, folded: false, cards: ['ACE of SPADES', 'KING of SPADES'] },
      { name: 'Player2', chips: 950, bet: 0, folded: false, cards: ['QUEEN of HEARTS', 'JACK of HEARTS'] }
    ],
    currentAction: overrides.currentAction || null
  });

  const createMockReplayData = (): ReplayData => ({
    id: 'hand-123',
    handNumber: 1,
    smallBlind: 5,
    bigBlind: 10,
    dealerPosition: 0,
    players: [
      { id: 'p1', name: 'Player1', startingChips: 1000, seatPosition: 0, holeCard1: 'ACE of SPADES', holeCard2: 'KING of SPADES' },
      { id: 'p2', name: 'Player2', startingChips: 1000, seatPosition: 1, holeCard1: 'QUEEN of HEARTS', holeCard2: 'JACK of HEARTS' },
      { id: 'p3', name: 'Player3', startingChips: 1000, seatPosition: 2, holeCard1: 'TEN of CLUBS', holeCard2: 'NINE of CLUBS' }
    ],
    board: ['ACE of CLUBS', 'KING of DIAMONDS', 'SEVEN of SPADES', 'TWO of HEARTS', 'NINE of CLUBS'],
    actions: [
      createMockReplayAction({ playerName: 'Player1', action: 'RAISE', amount: 100 }),
      createMockReplayAction({ playerName: 'Player2', action: 'CALL', amount: 100 }),
      createMockReplayAction({ playerName: 'Player3', action: 'FOLD' }),
      createMockReplayAction({ playerName: 'Player1', action: 'CHECK', phase: 'FLOP' }),
      createMockReplayAction({ playerName: 'Player2', action: 'BET', amount: 50, phase: 'FLOP' }),
    ],
    winnerName: 'Player1',
    winningHand: 'Two Pair',
    finalPot: 250
  });

  beforeEach(async () => {
    replayIndexSubject = new BehaviorSubject<number>(0);

    historyServiceMock = {
      getReplayData: jest.fn().mockReturnValue(of(createMockReplayData())),
      getCurrentReplayState: jest.fn().mockReturnValue(createMockReplayState()),
      replayIndex$: replayIndexSubject.asObservable(),
      nextAction: jest.fn(),
      previousAction: jest.fn(),
      goToAction: jest.fn(),
      resetReplay: jest.fn(),
      clearReplay: jest.fn(),
      formatAction: jest.fn((action: ReplayAction) => `${action.playerName} ${action.action}`),
      formatCard: jest.fn(() => ({ suit: 'SPADES', value: 'ACE', symbol: 'A♠' })),
      formatPhase: jest.fn((phase: string) => phase.replace('_', ' '))
    };

    await TestBed.configureTestingModule({
      imports: [HandReplayComponent, FormsModule],
      providers: [
        { provide: HandHistoryService, useValue: historyServiceMock },
        provideNoopAnimations()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HandReplayComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should not load replay without handId', () => {
      fixture.detectChanges();
      expect(historyServiceMock.getReplayData).not.toHaveBeenCalled();
    });

    it('should load replay with handId', fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();

      expect(historyServiceMock.getReplayData).toHaveBeenCalledWith('hand-123');
      expect(component.replay).toBeTruthy();
    }));

    it('should initialize with default playback speed', () => {
      expect(component.playbackSpeed).toBe(1);
    });

    it('should initialize with cards visible', () => {
      expect(component.showCards()).toBe(true);
    });

    it('should initialize not playing', () => {
      expect(component.isPlaying()).toBe(false);
    });
  });

  describe('Loading State', () => {
    it('should set loading while fetching data', fakeAsync(() => {
      const mockReplayData = createMockReplayData();
      let subscribeCallback: ((value: ReplayData) => void) | undefined;

      historyServiceMock.getReplayData = jest.fn().mockImplementation(() => {
        return {
          subscribe: (observer: { next?: (value: ReplayData) => void; error?: (error: unknown) => void; complete?: () => void }) => {
            subscribeCallback = observer.next;
            return { unsubscribe: jest.fn() };
          }
        } as unknown as Observable<ReplayData>;
      });

      component.handId = 'hand-123';
      expect(component.isLoading()).toBe(false);

      component.loadReplay();
      tick();
      expect(component.isLoading()).toBe(true);

      if (subscribeCallback) {
        subscribeCallback(mockReplayData);
      }
      tick();
      expect(component.isLoading()).toBe(false);
    }));

    it('should handle load error', fakeAsync(() => {
      historyServiceMock.getReplayData = jest.fn().mockReturnValue(
        throwError(() => new Error('Failed to load'))
      );

      component.handId = 'hand-123';
      component.loadReplay();
      tick();

      expect(component.isLoading()).toBe(false);
      expect(component.error()).toBe('Failed to load hand history');
    }));
  });

  describe('Playback Controls', () => {
    beforeEach(fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();
    }));

    it('should call nextAction on next step', () => {
      component.nextStep();
      expect(historyServiceMock.nextAction).toHaveBeenCalled();
    });

    it('should call previousAction on previous step', () => {
      component.previousStep();
      expect(historyServiceMock.previousAction).toHaveBeenCalled();
    });

    it('should go to specific step', () => {
      component.goToStep(3);
      expect(historyServiceMock.goToAction).toHaveBeenCalledWith(3);
    });

    it('should stop autoplay when going to step', fakeAsync(() => {
      component.play();
      tick();
      component.goToStep(2);
      expect(component.isPlaying()).toBe(false);
      discardPeriodicTasks();
    }));

    it('should go to end', () => {
      component.goToEnd();
      expect(historyServiceMock.goToAction).toHaveBeenCalledWith(5);
    });

    it('should reset replay', () => {
      component.reset();
      expect(historyServiceMock.resetReplay).toHaveBeenCalled();
    });

    it('should stop autoplay on reset', fakeAsync(() => {
      component.play();
      tick();
      component.reset();
      expect(component.isPlaying()).toBe(false);
      discardPeriodicTasks();
    }));

    it('should pause playback', fakeAsync(() => {
      component.play();
      tick();
      component.pause();
      expect(component.isPlaying()).toBe(false);
      discardPeriodicTasks();
    }));
  });

  describe('Auto Play', () => {
    beforeEach(fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();
    }));

    it('should start auto play', fakeAsync(() => {
      component.play();
      expect(component.isPlaying()).toBe(true);


      component.pause();
      discardPeriodicTasks();
    }));

    it('should not start if already playing', fakeAsync(() => {
      component.play();
      const firstPlayState = component.isPlaying();

      component.play();

      expect(component.isPlaying()).toBe(firstPlayState);
      component.pause();
      discardPeriodicTasks();
    }));

    it('should not start without replay data', () => {
      component.replay = null;
      component.play();
      expect(component.isPlaying()).toBe(false);
    });
  });

  describe('Speed Control', () => {
    it('should set playback speed', () => {
      component.setSpeed(0.5);
      expect(component.playbackSpeed).toBe(0.5);
    });

    it('should set slow speed', () => {
      component.setSpeed(2);
      expect(component.playbackSpeed).toBe(2);
    });
  });

  describe('Card Toggle', () => {
    it('should toggle cards visibility', () => {
      expect(component.showCards()).toBe(true);

      component.toggleCards();
      expect(component.showCards()).toBe(false);

      component.toggleCards();
      expect(component.showCards()).toBe(true);
    });
  });

  describe('Formatting', () => {
    beforeEach(fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();
    }));

    it('should format action', () => {
      const action = createMockReplayAction({ playerName: 'Alice', action: 'RAISE' });
      component.formatAction(action);
      expect(historyServiceMock.formatAction).toHaveBeenCalledWith(action);
    });

    it('should format card with correct color', () => {
      historyServiceMock.formatCard = jest.fn().mockReturnValue({ suit: 'HEARTS', value: 'KING', symbol: 'K♥' });

      const result = component.formatCard('KING of HEARTS');

      expect(result.isRed).toBe(true);
      expect(result.symbol).toBe('K♥');
    });

    it('should format black card', () => {
      historyServiceMock.formatCard = jest.fn().mockReturnValue({ suit: 'SPADES', value: 'ACE', symbol: 'A♠' });

      const result = component.formatCard('ACE of SPADES');

      expect(result.isRed).toBe(false);
    });

    it('should format phase', () => {
      component.formatPhase('PRE_FLOP');
      expect(historyServiceMock.formatPhase).toHaveBeenCalledWith('PRE_FLOP');
    });
  });

  describe('Progress Calculation', () => {
    beforeEach(fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();
    }));

    it('should calculate progress percentage', fakeAsync(() => {
      historyServiceMock.getCurrentReplayState = jest.fn().mockReturnValue(
        createMockReplayState({ actionIndex: 2 })
      );
      component['updateReplayState']();
      tick();
      fixture.detectChanges();

      const progress = component.getProgressPercentage();
      expect(progress).toBe(40);
    }));

    it('should return 0 without replay', () => {
      component.replay = null;
      expect(component.getProgressPercentage()).toBe(0);
    });

    it('should return 0 without replay state', () => {
      component.replayState = null;
      expect(component.getProgressPercentage()).toBe(0);
    });
  });

  describe('Action Styling', () => {
    it('should return correct class for FOLD', () => {
      const action = createMockReplayAction({ action: 'FOLD' });
      expect(component.getActionClass(action)).toBe('action-fold');
    });

    it('should return correct class for CHECK', () => {
      const action = createMockReplayAction({ action: 'CHECK' });
      expect(component.getActionClass(action)).toBe('action-check');
    });

    it('should return correct class for CALL', () => {
      const action = createMockReplayAction({ action: 'call' });
      expect(component.getActionClass(action)).toBe('action-call');
    });

    it('should return correct class for BET', () => {
      const action = createMockReplayAction({ action: 'BET' });
      expect(component.getActionClass(action)).toBe('action-bet');
    });

    it('should return correct class for RAISE', () => {
      const action = createMockReplayAction({ action: 'RAISE' });
      expect(component.getActionClass(action)).toBe('action-raise');
    });

    it('should return empty for unknown action', () => {
      const action = createMockReplayAction({ action: 'UNKNOWN' });
      expect(component.getActionClass(action)).toBe('');
    });
  });

  describe('Phase Styling', () => {
    it('should generate phase class', () => {
      expect(component.getPhaseClass('PRE_FLOP')).toBe('phase-pre-flop');
      expect(component.getPhaseClass('FLOP')).toBe('phase-flop');
      expect(component.getPhaseClass('TURN')).toBe('phase-turn');
      expect(component.getPhaseClass('RIVER')).toBe('phase-river');
    });
  });

  describe('Player State Helpers', () => {
    beforeEach(fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();
    }));

    it('should check if player is active', () => {
      historyServiceMock.getCurrentReplayState = jest.fn().mockReturnValue(
        createMockReplayState({
          currentAction: createMockReplayAction({ playerName: 'Player1' })
        })
      );
      component['updateReplayState']();

      expect(component.isPlayerActiveLegacy('Player1')).toBe(true);
      expect(component.isPlayerActiveLegacy('Player2')).toBe(false);
    });

    it('should check if player is folded', () => {
      historyServiceMock.getCurrentReplayState = jest.fn().mockReturnValue(
        createMockReplayState({
          players: [
            { name: 'Player1', chips: 1000, bet: 0, folded: true, cards: ['ACE of SPADES', 'KING of SPADES'] },
            { name: 'Player2', chips: 950, bet: 0, folded: false, cards: ['QUEEN of HEARTS', 'JACK of HEARTS'] }
          ]
        })
      );
      component['updateReplayState']();

      expect(component.isPlayerFolded('Player1')).toBe(true);
      expect(component.isPlayerFolded('Player2')).toBe(false);
    });

    it('should get player chips', () => {
      expect(component.getPlayerChips('Player1')).toBe(1000);
      expect(component.getPlayerChips('Player2')).toBe(950);
    });

    it('should return 0 for unknown player chips', () => {
      expect(component.getPlayerChips('Unknown')).toBe(0);
    });

    it('should get player bet', () => {
      expect(component.getPlayerBet('Player1')).toBe(50);
      expect(component.getPlayerBet('Player2')).toBe(0);
    });
  });

  describe('Actions By Phase', () => {
    beforeEach(fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();
    }));

    it('should group actions by phase', () => {
      const byPhase = component.getActionsByPhase();

      expect(byPhase.has('PRE_FLOP')).toBe(true);
      expect(byPhase.has('FLOP')).toBe(true);
      expect(byPhase.get('PRE_FLOP')?.length).toBe(3);
      expect(byPhase.get('FLOP')?.length).toBe(2);
    });

    it('should return empty map without replay', () => {
      component.replay = null;
      expect(component.getActionsByPhase().size).toBe(0);
    });
  });

  describe('Actions Up To Index', () => {
    beforeEach(fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();
    }));

    it('should get actions up to current index', () => {
      historyServiceMock.getCurrentReplayState = jest.fn().mockReturnValue(
        createMockReplayState({ actionIndex: 3 })
      );
      component['updateReplayState']();

      const actions = component.getActionsUpToIndex();
      expect(actions.length).toBe(3);
    });

    it('should return empty array without replay', () => {
      component.replay = null;
      expect(component.getActionsUpToIndex()).toEqual([]);
    });
  });

  describe('Cleanup', () => {
    it('should clean up on destroy', fakeAsync(() => {
      component.handId = 'hand-123';
      fixture.detectChanges();
      tick();

      component.play();
      tick();

      fixture.destroy();

      expect(historyServiceMock.clearReplay).toHaveBeenCalled();
      discardPeriodicTasks();
    }));
  });
});
