import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GameTableComponent } from './game-table.component';
import { PokerService } from '../services/poker.service';
import { SoundService } from '../services/sound.service';
import { WebSocketService } from '../services/websocket.service';
import { of, BehaviorSubject } from 'rxjs';
import { Game } from '../model/game';

describe('GameTableComponent', () => {
  let component: GameTableComponent;
  let fixture: ComponentFixture<GameTableComponent>;
  let pokerService: jasmine.SpyObj<PokerService>;
  let soundService: jasmine.SpyObj<SoundService>;
  
  const mockGame: Game = {
    id: 'game-123',
    currentPot: 100,
    players: [
      {
        id: 'player-1',
        name: 'Alice',
        chips: 980,
        betAmount: 20,
        folded: false,
        isBot: false,
        isAllIn: false,
        hasActed: false,
        seatPosition: 0,
        hand: []
      },
      {
        id: 'player-2',
        name: 'Bot1',
        chips: 970,
        betAmount: 10,
        folded: false,
        isBot: true,
        isAllIn: false,
        hasActed: true,
        seatPosition: 1,
        hand: []
      }
    ],
    communityCards: [],
    phase: 'PRE_FLOP',
    currentBet: 20,
    currentPlayerIndex: 0,
    dealerPosition: 1,
    smallBlindPosition: 0,
    bigBlindPosition: 1,
    minRaiseAmount: 20,
    playerActions: {}
  };

  const gameSubject = new BehaviorSubject<Game | null>(mockGame);

  beforeEach(async () => {
    const pokerSpy = jasmine.createSpyObj('PokerService', [
      'startGame', 'getGameStatus', 'fold', 'check', 'call', 'bet', 'raise',
      'executeBotAction', 'getMinRaiseAmount', 'getCallAmount', 'canCheck',
      'getPhaseDisplayName'
    ], {
      game$: gameSubject.asObservable()
    });

    const soundSpy = jasmine.createSpyObj('SoundService', [
      'playChips', 'playCard', 'playFold', 'playCheck', 'playWin',
      'playTurn', 'setEnabled', 'isEnabled'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        GameTableComponent
      ],
      providers: [
        { provide: PokerService, useValue: pokerSpy },
        { provide: SoundService, useValue: soundSpy }
      ]
    }).compileComponents();

    pokerService = TestBed.inject(PokerService) as jasmine.SpyObj<PokerService>;
    soundService = TestBed.inject(SoundService) as jasmine.SpyObj<SoundService>;

    pokerService.getMinRaiseAmount.and.returnValue(40);
    pokerService.getCallAmount.and.returnValue(10);
    pokerService.canCheck.and.returnValue(false);
    pokerService.getPhaseDisplayName.and.returnValue('Pre-Flop');
    pokerService.fold.and.returnValue(of('Fold successful'));
    pokerService.check.and.returnValue(of('Check successful'));
    pokerService.call.and.returnValue(of('Call successful'));
    pokerService.raise.and.returnValue(of('Raise successful'));
    pokerService.executeBotAction.and.returnValue(of({ message: 'Bot acted' }));

    fixture = TestBed.createComponent(GameTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Game Display', () => {
    it('should subscribe to game updates', () => {
      expect(component.game).toEqual(mockGame);
    });

    it('should display current pot', () => {
      expect(component.game?.currentPot).toBe(100);
    });

    it('should display all players', () => {
      expect(component.game?.players.length).toBe(2);
    });
  });

  describe('Player Actions', () => {
    it('should call fold when fold action triggered', fakeAsync(() => {
      component.onFold();
      tick();
      expect(pokerService.fold).toHaveBeenCalled();
    }));

    it('should call check when check action triggered', fakeAsync(() => {
      pokerService.canCheck.and.returnValue(true);
      component.onCheck();
      tick();
      expect(pokerService.check).toHaveBeenCalled();
    }));

    it('should call call when call action triggered', fakeAsync(() => {
      component.onCall();
      tick();
      expect(pokerService.call).toHaveBeenCalled();
    }));

    it('should call raise with amount when raise action triggered', fakeAsync(() => {
      component.raiseAmount = 100;
      component.onRaise();
      tick();
      expect(pokerService.raise).toHaveBeenCalledWith(jasmine.any(String), 100);
    }));
  });

  describe('Game State Changes', () => {
    it('should update display when game state changes', () => {
      const updatedGame = { ...mockGame, currentPot: 200 };
      gameSubject.next(updatedGame);
      fixture.detectChanges();
      expect(component.game?.currentPot).toBe(200);
    });
  });

  describe('Player Info', () => {
    it('should identify current player', () => {
      expect(component.getCurrentPlayer()?.name).toBe('Alice');
    });

    it('should identify human player', () => {
      const humanPlayer = component.game?.players.find(p => !p.isBot);
      expect(humanPlayer?.name).toBe('Alice');
    });
  });

  describe('Raise Slider', () => {
    it('should have valid raise amount range', () => {
      const minRaise = pokerService.getMinRaiseAmount(mockGame);
      expect(minRaise).toBe(40);
    });
  });
});
