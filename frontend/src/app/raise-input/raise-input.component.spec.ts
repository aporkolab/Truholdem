import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RaiseInputComponent } from './raise-input.component';
import { Game } from '../model/game';
import { Player } from '../model/player';

describe('RaiseInputComponent', () => {
  let component: RaiseInputComponent;
  let fixture: ComponentFixture<RaiseInputComponent>;

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
    return player;
  };

  const createMockGame = (): Game => {
    const game = new Game();
    game.id = 'game-123';
    game.currentPot = 100;
    game.players = [
      createMockPlayer({ id: 'player-1', name: 'Alice', chips: 980 }),
      createMockPlayer({ id: 'player-2', name: 'Bot1', isBot: true, seatPosition: 1 }),
    ];
    game.communityCards = [];
    game.phase = 'PRE_FLOP';
    game.currentBet = 20;
    game.currentPlayerIndex = 0;
    game.dealerPosition = 1;
    game.minRaiseAmount = 20;
    game.bigBlind = 20;
    game.smallBlind = 10;
    game.isFinished = false;
    return game;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RaiseInputComponent],
    });
    fixture = TestBed.createComponent(RaiseInputComponent);
    component = fixture.componentInstance;
    component.game = createMockGame();
    fixture.detectChanges();
  });

  describe('Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should calculate raise limits on init', () => {
      expect(component.minRaiseAmount).toBeGreaterThan(0);
      expect(component.maxRaiseAmount).toBeGreaterThan(0);
    });

    it('should start with raise input hidden', () => {
      expect(component.isRaiseInputVisible).toBe(false);
    });

    it('should have no error message initially', () => {
      expect(component.errorMessage).toBe('');
    });
  });

  describe('Show/Hide Raise Input', () => {
    it('should show raise input', () => {
      component.showRaiseInput();
      expect(component.isRaiseInputVisible).toBe(true);
    });

    it('should hide raise input on cancel', () => {
      component.isRaiseInputVisible = true;
      component.cancelRaise();
      expect(component.isRaiseInputVisible).toBe(false);
    });

    it('should clear error message on cancel', () => {
      component.errorMessage = 'Some error';
      component.cancelRaise();
      expect(component.errorMessage).toBe('');
    });

    it('should reset raise amount to minimum on cancel', () => {
      component.raiseAmount = 500;
      component.cancelRaise();
      expect(component.raiseAmount).toBe(component.minRaiseAmount);
    });
  });

  describe('Confirm Raise', () => {
    it('should emit action when raise is confirmed', () => {
      jest.spyOn(component.actionTaken, 'emit');
      component.raiseAmount = 100;
      component.confirmRaise();
      expect(component.actionTaken.emit).toHaveBeenCalledWith(100);
    });

    it('should hide input after confirming raise', () => {
      component.isRaiseInputVisible = true;
      component.raiseAmount = 100;
      component.confirmRaise();
      expect(component.isRaiseInputVisible).toBe(false);
    });

    it('should show error when raise is below minimum', () => {
      component.raiseAmount = 10;
      component.minRaiseAmount = 40;
      component.confirmRaise();
      expect(component.errorMessage).toContain('Minimum raise');
    });

    it('should show error when raise exceeds maximum', () => {
      component.raiseAmount = 5000;
      component.maxRaiseAmount = 1000;
      component.confirmRaise();
      expect(component.errorMessage).toContain('Maximum');
    });

    it('should show error when no player found', () => {
      component['currentPlayer'] = undefined;
      component.confirmRaise();
      expect(component.errorMessage).toBe('No player found');
    });
  });

  describe('Quick Bet Buttons', () => {
    beforeEach(() => {
      component.game!.currentPot = 200;
      component.minRaiseAmount = 40;
      component.maxRaiseAmount = 1000;
    });

    it('should set minimum raise amount', () => {
      component.setMinRaise();
      expect(component.raiseAmount).toBe(40);
    });

    it('should set half pot', () => {
      component.setHalfPot();
      expect(component.raiseAmount).toBe(100); 
    });

    it('should set full pot', () => {
      component.setFullPot();
      expect(component.raiseAmount).toBe(200);
    });

    it('should set all-in amount', () => {
      component.setAllIn();
      expect(component.raiseAmount).toBe(1000);
    });

    it('should respect min limit for percentage bets', () => {
      component.game!.currentPot = 20;
      component.minRaiseAmount = 40;
      component.setBetPercentage(0.5);
      expect(component.raiseAmount).toBe(40);
    });

    it('should respect max limit for percentage bets', () => {
      component.game!.currentPot = 5000;
      component.maxRaiseAmount = 1000;
      component.setBetPercentage(1);
      expect(component.raiseAmount).toBe(1000);
    });
  });

  describe('Button Label', () => {
    it('should return "Raise" when there is a current bet', () => {
      component.game!.currentBet = 50;
      expect(component.getButtonLabel()).toBe('Raise');
    });

    it('should return "Bet" when no current bet', () => {
      component.game!.currentBet = 0;
      expect(component.getButtonLabel()).toBe('Bet');
    });

    it('should return "Bet" when game is undefined', () => {
      component.game = undefined;
      expect(component.getButtonLabel()).toBe('Bet');
    });
  });

  describe('Can Raise Check', () => {
    it('should return true for valid player', () => {
      expect(component.canRaise()).toBe(true);
    });

    it('should return false for folded player', () => {
      component['currentPlayer'] = createMockPlayer({ folded: true });
      expect(component.canRaise()).toBe(false);
    });

    it('should return false for all-in player', () => {
      component['currentPlayer'] = createMockPlayer({ isAllIn: true });
      expect(component.canRaise()).toBe(false);
    });

    it('should return false for player with no chips', () => {
      component['currentPlayer'] = createMockPlayer({ chips: 0 });
      expect(component.canRaise()).toBe(false);
    });

    it('should return false when no current player', () => {
      component['currentPlayer'] = undefined;
      expect(component.canRaise()).toBe(false);
    });
  });

  describe('Is Folded Check', () => {
    it('should return true for folded player', () => {
      component['currentPlayer'] = createMockPlayer({ folded: true });
      expect(component.isFolded()).toBe(true);
    });

    it('should return false for active player', () => {
      component['currentPlayer'] = createMockPlayer({ folded: false });
      expect(component.isFolded()).toBe(false);
    });

    it('should return false when no current player', () => {
      component['currentPlayer'] = undefined;
      expect(component.isFolded()).toBe(false);
    });
  });

  describe('Game Changes', () => {
    it('should update limits when game changes', () => {
      const newGame = createMockGame();
      newGame.currentBet = 50;
      newGame.minRaiseAmount = 50;
      component.game = newGame;
      component.ngOnChanges({
        game: {
          currentValue: newGame,
          previousValue: createMockGame(),
          firstChange: false,
          isFirstChange: () => false,
        },
      });
      expect(component.minRaiseAmount).toBeGreaterThanOrEqual(50);
    });

    it('should handle game with no players gracefully', () => {
      const emptyGame = createMockGame();
      emptyGame.players = [];
      component.game = emptyGame;
      component.ngOnChanges({
        game: {
          currentValue: emptyGame,
          previousValue: createMockGame(),
          firstChange: false,
          isFirstChange: () => false,
        },
      });
      expect(component['currentPlayer']).toBeUndefined();
    });
  });
});
