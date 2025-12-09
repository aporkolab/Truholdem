import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of } from 'rxjs';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { PokerService } from '../services/poker.service';
import { SoundService } from '../services/sound.service';
import { GameTableComponent } from './game-table.component';

describe('GameTableComponent', () => {
  let component: GameTableComponent;
  let fixture: ComponentFixture<GameTableComponent>;
  let pokerService: jasmine.SpyObj<PokerService>;
  let soundService: jasmine.SpyObj<SoundService>;

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
      createMockPlayer({
        id: 'player-1',
        name: 'Alice',
        chips: 980,
        betAmount: 20,
        totalBetInRound: 20,
      }),
      createMockPlayer({
        id: 'player-2',
        name: 'Bot1',
        chips: 970,
        betAmount: 10,
        totalBetInRound: 10,
        isBot: true,
        hasActed: true,
        seatPosition: 1,
      }),
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

  let mockGame: Game;
  let gameSubject: BehaviorSubject<Game | null>;

  beforeEach(async () => {
    mockGame = createMockGame();
    gameSubject = new BehaviorSubject<Game | null>(mockGame);

    const pokerSpy = jasmine.createSpyObj(
      'PokerService',
      [
        'startGame',
        'getGameStatus',
        'fold',
        'check',
        'call',
        'bet',
        'raise',
        'executeBotAction',
        'startNewHand',
        'resetGame',
        'endGame',
      ],
      {
        game$: gameSubject.asObservable(),
        loading$: of(false),
        error$: of(null),
      }
    );

    const soundSpy = jasmine.createSpyObj('SoundService', [
      'playChips',
      'playCardDeal',
      'playCardFlip',
      'playFold',
      'playCheck',
      'playWin',
      'playTurn',
      'setEnabled',
      'isEnabled',
      'play',
      'playForAction',
    ]);

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        GameTableComponent,
      ],
      providers: [
        { provide: PokerService, useValue: pokerSpy },
        { provide: SoundService, useValue: soundSpy },
      ],
    }).compileComponents();

    pokerService = TestBed.inject(PokerService) as jasmine.SpyObj<PokerService>;
    soundService = TestBed.inject(SoundService) as jasmine.SpyObj<SoundService>;

    pokerService.getGameStatus.and.returnValue(of(mockGame));
    pokerService.fold.and.returnValue(of('Fold successful'));
    pokerService.check.and.returnValue(of('Check successful'));
    pokerService.call.and.returnValue(of('Call successful'));
    pokerService.raise.and.returnValue(of('Raise successful'));
    pokerService.bet.and.returnValue(of('Bet successful'));
    pokerService.executeBotAction.and.returnValue(of({ message: 'Bot acted' }));
    pokerService.startGame.and.returnValue(of(mockGame));
    pokerService.startNewHand.and.returnValue(of(mockGame));

    fixture = TestBed.createComponent(GameTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Game Display', () => {
    it('should subscribe to game updates', () => {
      expect(component.game).toBeTruthy();
      expect(component.game.id).toBe('game-123');
    });

    it('should display current pot', () => {
      expect(component.game?.currentPot).toBe(100);
    });

    it('should display all players', () => {
      expect(component.game?.players.length).toBe(2);
    });

    it('should identify human player', () => {
      expect(component.humanPlayer).toBeTruthy();
      expect(component.humanPlayer?.name).toBe('Alice');
    });
  });

  describe('Player Actions', () => {
    beforeEach(() => {
      
      const humanPlayer = mockGame.players.find((p) => !p.isBot)!;
      component.humanPlayer = humanPlayer;
      component.game = mockGame;
      
      component.game.currentPlayerIndex = mockGame.players.findIndex(
        (p) => p.id === humanPlayer.id
      );
    });

    it('should call fold when fold action triggered', () => {
      component.fold();
      expect(pokerService.fold).toHaveBeenCalledWith(component.humanPlayer!.id);
    });

    it('should call check when check is valid', () => {
      component.humanPlayer!.betAmount = 20;
      component.game.currentBet = 20;
      component.check();
      expect(pokerService.check).toHaveBeenCalledWith(
        component.humanPlayer!.id
      );
    });

    it('should call call when call action triggered', () => {
      component.call();
      expect(pokerService.call).toHaveBeenCalledWith(component.humanPlayer!.id);
    });

    it('should call raise with amount', () => {
      component.raise(100);
      expect(pokerService.raise).toHaveBeenCalledWith(
        component.humanPlayer!.id,
        100
      );
    });

    it('should call bet with amount', () => {
      component.bet(50);
      expect(pokerService.bet).toHaveBeenCalledWith(
        component.humanPlayer!.id,
        50
      );
    });
  });

  describe('Game State Changes', () => {
    it('should update display when game state changes', () => {
      const updatedGame = createMockGame();
      updatedGame.currentPot = 200;
      gameSubject.next(updatedGame);
      fixture.detectChanges();
      expect(component.game?.currentPot).toBe(200);
    });

    it('should update current pot calculation', () => {
      expect(component.currentPot).toBeGreaterThanOrEqual(0);
    });
  });

  describe('UI Helpers', () => {
    beforeEach(() => {
      
      const humanPlayer = mockGame.players.find((p) => !p.isBot);
      component.humanPlayer = humanPlayer;
      component.game = mockGame;
    });

    it('should calculate call amount', () => {
      
      component.humanPlayer!.betAmount = 20;
      component.game.currentBet = 20;
      const callAmount = component.getCallAmount();
      expect(callAmount).toBe(0);
    });

    it('should calculate min raise amount', () => {
      const minRaise = component.getMinRaiseAmount();
      expect(minRaise).toBeGreaterThan(0);
    });

    it('should check if player can act', () => {
      
      const humanIndex = component.game.players.findIndex(
        (p) => p.id === component.humanPlayer?.id
      );
      component.game.currentPlayerIndex = humanIndex;
      const canAct = component.canPlayerAct();
      expect(canAct).toBe(true);
    });

    it('should determine if player can check', () => {
      component.humanPlayer!.betAmount = 20;
      component.game.currentBet = 20;
      expect(component.canCheck()).toBe(true);
    });

    it('should get phase display name', () => {
      const phaseName = component.getPhaseDisplayName();
      expect(phaseName).toBe('Pre-Flop');
    });

    it('should identify player turn', () => {
      
      const humanPlayer = component.game.players.find((p) => !p.isBot);
      const humanIndex = component.game.players.findIndex(
        (p) => p.id === humanPlayer?.id
      );
      component.game.currentPlayerIndex = humanIndex;
      const isCurrentPlayerTurn = component.isPlayerTurn(humanPlayer!);
      expect(isCurrentPlayerTurn).toBe(true);
    });

    it('should get player status', () => {
      const foldedPlayer = createMockPlayer({ folded: true });
      expect(component.getPlayerStatus(foldedPlayer)).toBe('Folded');

      const allInPlayer = createMockPlayer({ isAllIn: true });
      expect(component.getPlayerStatus(allInPlayer)).toBe('All-In');
    });
  });

  describe('Card Display', () => {
    it('should get card image path', () => {
      const card = { suit: 'HEARTS', value: 'ACE' };
      const imagePath = component.getCardImage(card as any);
      expect(imagePath).toContain('assets/cards/');
    });

    it('should get card back image', () => {
      const backImage = component.getCardBackImage();
      expect(backImage).toBe('assets/cards/back.png');
    });
  });

  describe('Modal Control', () => {
    it('should open modal', () => {
      component.openModal();
      expect(component.showModal).toBe(true);
    });

    it('should close modal', () => {
      component.showModal = true;
      component.closeModal();
      expect(component.showModal).toBe(false);
    });
  });
});
