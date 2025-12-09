import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
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
      imports: [HttpClientTestingModule, RaiseInputComponent],
    });
    fixture = TestBed.createComponent(RaiseInputComponent);
    component = fixture.componentInstance;
    component.game = createMockGame();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should calculate raise limits on init', () => {
    expect(component.minRaiseAmount).toBeGreaterThan(0);
    expect(component.maxRaiseAmount).toBeGreaterThan(0);
  });

  it('should emit action when raise is confirmed', () => {
    spyOn(component.actionTaken, 'emit');
    component.raiseAmount = 100;
    component.confirmRaise();
    expect(component.actionTaken.emit).toHaveBeenCalledWith(100);
  });

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
});
