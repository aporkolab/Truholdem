import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { Player } from '../model/player';
import { Card } from '../model/card';
import { SoundService } from '../services/sound.service';
import { UiStateService } from '../services/ui-state.service';
import { PlayerService } from '../services/player.service';
import { GameTableComponent } from './game-table.component';
import { GameStore } from '../store/game.store';

describe('GameTableComponent', () => {
  let component: GameTableComponent;
  let fixture: ComponentFixture<GameTableComponent>;
  let gameStore: jasmine.SpyObj<GameStore>;

  const createMockPlayer = (overrides: Partial<Player> = {}): Player => {
    const player = new Player();
    player.id = overrides.id || 'player-1';
    player.name = overrides.name || 'TestPlayer';
    player.chips = overrides.chips ?? 1000;
    player.betAmount = overrides.betAmount ?? 0;
    player.totalBetInRound = overrides.totalBetInRound ?? 0;
    player.folded = overrides.folded ?? false;
    player.isBot = overrides.isBot ?? false;
    player.isAllIn = overrides.isAllIn ?? false;
    player.hasActed = overrides.hasActed ?? false;
    player.seatPosition = overrides.seatPosition ?? 0;
    player.hand = overrides.hand || [];

    // Apply any additional overrides
    Object.assign(player, overrides);

    return player;
  };

  beforeEach(async () => {
    const storeSpy = jasmine.createSpyObj('GameStore', [
      'startGame',
      'refreshGame',
      'playerAction',
      'startNewHand',
      'resetGame',
      'clearError',
      'isPlayerTurn',
      'isDealer',
      'getPlayerStatus',
      'processBots'
    ]);

    const soundSpy = jasmine.createSpyObj('SoundService', [
      'playActionSound',
      'playWinSound',
      'playLoseSound',
    ]);

    const playerSpy = jasmine.createSpyObj('PlayerService', [
      'getPlayers',
      'setPlayers',
      'clearPlayers'
    ]);

    playerSpy.getPlayers.and.returnValue([]);

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        GameTableComponent,
      ],
      providers: [
        UiStateService,
        { provide: GameStore, useValue: storeSpy },
        { provide: SoundService, useValue: soundSpy },
        { provide: PlayerService, useValue: playerSpy },
      ],
    }).compileComponents();

    gameStore = TestBed.inject(GameStore) as jasmine.SpyObj<GameStore>;

    gameStore.isPlayerTurn.and.returnValue(true);
    gameStore.isDealer.and.returnValue(false);
    gameStore.getPlayerStatus.and.returnValue('');

    fixture = TestBed.createComponent(GameTableComponent);
    component = fixture.componentInstance;

    // Spy on the component's store instance methods since GameStore is provided at component level
    gameStore = component['store'] as jasmine.SpyObj<GameStore>;
    spyOn(gameStore, 'startNewHand');
    spyOn(gameStore, 'resetGame');
    spyOn(gameStore, 'clearError');
    spyOn(gameStore, 'refreshGame');
    spyOn(gameStore, 'isPlayerTurn').and.returnValue(true);
    spyOn(gameStore, 'isDealer').and.callFake((pos: number) => pos === 1);
    spyOn(gameStore, 'getPlayerStatus').and.callFake((player: Player) => {
      if (player.folded) return 'Folded';
      if (player.isAllIn) return 'All-In';
      return '';
    });

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Card Display', () => {
    it('should get card image path for ace of hearts', () => {
      const card: Card = { suit: 'HEARTS', value: 'ACE' } as Card;
      const imagePath = component.getCardImage(card);
      expect(imagePath).toBe('assets/cards/ace_of_hearts.png');
    });

    it('should get card image path for numbered card', () => {
      const card: Card = { suit: 'SPADES', value: 'TEN' } as Card;
      const imagePath = component.getCardImage(card);
      expect(imagePath).toBe('assets/cards/10_of_spades.png');
    });

    it('should get card back image', () => {
      const backImage = component.getCardBackImage();
      expect(backImage).toBe('assets/cards/back.png');
    });
  });

  describe('Modal Control', () => {
    it('should open result modal', () => {
      component.openModal();
      expect(component.showModal()).toBe(true);
    });

    it('should close result modal', () => {
      component.openModal();
      component.closeModal();
      expect(component.showModal()).toBe(false);
    });
  });

  describe('Game Lifecycle', () => {
    it('should start new hand when startNewGame is called', fakeAsync(() => {
      component.startNewGame();
      tick();
      expect(gameStore.startNewHand).toHaveBeenCalled();
    }));

    it('should start new hand', fakeAsync(() => {
      component.startNewHand();
      tick();
      expect(gameStore.startNewHand).toHaveBeenCalled();
    }));

    it('should reset game and close modal when returning to lobby', fakeAsync(() => {
      component.openModal();
      component.returnToLobby();
      tick();
      expect(component.showModal()).toBe(false);
      expect(gameStore.resetGame).toHaveBeenCalled();
    }));
  });

  describe('UI State Integration', () => {
    it('should track raise modal state via UI service', () => {
      const uiState = TestBed.inject(UiStateService);
      expect(component.showRaiseModal()).toBe(false);
      uiState.openRaiseModal();
      fixture.detectChanges();
      expect(component.showRaiseModal()).toBe(true);
    });

    it('should close raise modal', () => {
      const uiState = TestBed.inject(UiStateService);
      uiState.openRaiseModal();
      component.closeRaiseModal();
      expect(component.showRaiseModal()).toBe(false);
    });
  });

  describe('Helper Methods', () => {
    it('should identify dealer position', () => {
      expect(component.isDealer(1)).toBe(true);
      expect(component.isDealer(0)).toBe(false);
    });

    it('should check if player turn', () => {
      const player = createMockPlayer({ id: 'player-1' });
      expect(component.isPlayerTurn(player)).toBe(true);
    });

    it('should get player status', () => {
      const foldedPlayer = createMockPlayer({ folded: true });
      expect(component.getPlayerStatus(foldedPlayer)).toBe('Folded');

      const allInPlayer = createMockPlayer({ isAllIn: true });
      expect(component.getPlayerStatus(allInPlayer)).toBe('All-In');

      const activePlayer = createMockPlayer();
      expect(component.getPlayerStatus(activePlayer)).toBe('');
    });
  });

  describe('Track By Functions', () => {
    it('should track players by id', () => {
      const player = createMockPlayer({ id: 'test-123' });
      expect(component.trackByPlayerId(0, player)).toBe('test-123');
    });

    it('should track cards by index', () => {
      expect(component.trackByCardIndex(5)).toBe(5);
    });
  });

  describe('Error Handling', () => {
    it('should clear error', () => {
      component.clearError();
      expect(gameStore.clearError).toHaveBeenCalled();
    });

    it('should retry action', () => {
      component.retryAction();
      expect(gameStore.clearError).toHaveBeenCalled();
      expect(gameStore.refreshGame).toHaveBeenCalled();
    });
  });
});
