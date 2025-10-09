import { TestBed } from '@angular/core/testing';
import { GameStateService, PlayerAction } from './game-state.service';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';

describe('GameStateService', () => {
  let service: GameStateService;

  
  
  

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

  const createMockCard = (suit: Card['suit'], value: Card['value']): Card => {
    return new Card(suit, value);
  };

  const createMockGame = (overrides: Partial<Game> = {}): Game => {
    const game = new Game();
    game.id = overrides.id || 'game-123';
    game.currentPot = overrides.currentPot ?? 100;
    game.players = overrides.players || [
      createMockPlayer({ id: 'human-1', name: 'Alice', chips: 980, betAmount: 20 }),
      createMockPlayer({ id: 'bot-1', name: 'Bot1', chips: 970, betAmount: 10, isBot: true }),
      createMockPlayer({ id: 'bot-2', name: 'Bot2', chips: 990, betAmount: 0, isBot: true }),
    ];
    game.communityCards = overrides.communityCards || [];
    game.phase = overrides.phase || 'PRE_FLOP';
    game.currentBet = overrides.currentBet ?? 20;
    game.currentPlayerIndex = overrides.currentPlayerIndex ?? 0;
    game.dealerPosition = overrides.dealerPosition ?? 2;
    game.minRaiseAmount = 'minRaiseAmount' in overrides ? overrides.minRaiseAmount : 20;
    game.bigBlind = overrides.bigBlind ?? 20;
    game.smallBlind = overrides.smallBlind ?? 10;
    game.isFinished = overrides.isFinished ?? false;
    game.handNumber = overrides.handNumber ?? 1;
    game.winnerName = overrides.winnerName;
    game.winningHandDescription = overrides.winningHandDescription;
    return game;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GameStateService]
    });
    service = TestBed.inject(GameStateService);
  });

  
  
  

  describe('Initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should initialize with null game', () => {
      expect(service.game()).toBeNull();
    });

    it('should initialize with loading false', () => {
      expect(service.isLoading()).toBe(false);
    });

    it('should initialize with no error', () => {
      expect(service.error()).toBeNull();
    });

    it('should initialize with no last action', () => {
      expect(service.lastAction()).toBeNull();
    });

    it('should initialize with disconnected status', () => {
      expect(service.connectionStatus()).toBe('disconnected');
    });
  });

  
  
  

  describe('State Updates', () => {
    describe('setGame', () => {
      it('should set game state', () => {
        const game = createMockGame();
        service.setGame(game);
        expect(service.game()).toEqual(game);
      });

      it('should clear error when setting game', () => {
        service.setError('Previous error');
        service.setGame(createMockGame());
        expect(service.error()).toBeNull();
      });

      it('should handle null game', () => {
        service.setGame(createMockGame());
        service.setGame(null);
        expect(service.game()).toBeNull();
      });
    });

    describe('setLoading', () => {
      it('should set loading to true', () => {
        service.setLoading(true);
        expect(service.isLoading()).toBe(true);
      });

      it('should set loading to false', () => {
        service.setLoading(true);
        service.setLoading(false);
        expect(service.isLoading()).toBe(false);
      });
    });

    describe('setError', () => {
      it('should set error message', () => {
        service.setError('Test error');
        expect(service.error()).toBe('Test error');
      });

      it('should set loading to false when setting error', () => {
        service.setLoading(true);
        service.setError('Error occurred');
        expect(service.isLoading()).toBe(false);
      });

      it('should clear error with null', () => {
        service.setError('Test error');
        service.setError(null);
        expect(service.error()).toBeNull();
      });
    });

    describe('clearError', () => {
      it('should clear existing error', () => {
        service.setError('Test error');
        service.clearError();
        expect(service.error()).toBeNull();
      });
    });

    describe('setLastAction', () => {
      it('should set last action', () => {
        const action: PlayerAction = {
          type: 'FOLD',
          playerId: 'player-1',
          playerName: 'Alice',
          timestamp: Date.now()
        };
        service.setLastAction(action);
        expect(service.lastAction()).toEqual(action);
      });

      it('should clear last action with null', () => {
        service.setLastAction({
          type: 'CHECK',
          playerId: 'player-1',
          playerName: 'Alice',
          timestamp: Date.now()
        });
        service.setLastAction(null);
        expect(service.lastAction()).toBeNull();
      });
    });

    describe('recordAction', () => {
      it('should record action with all fields', () => {
        const beforeTime = Date.now();
        service.recordAction('RAISE', 'player-1', 'Alice', 100);
        const action = service.lastAction();
        
        expect(action).toBeTruthy();
        expect(action!.type).toBe('RAISE');
        expect(action!.playerId).toBe('player-1');
        expect(action!.playerName).toBe('Alice');
        expect(action!.amount).toBe(100);
        expect(action!.timestamp).toBeGreaterThanOrEqual(beforeTime);
      });

      it('should record action without amount', () => {
        service.recordAction('FOLD', 'player-1', 'Alice');
        const action = service.lastAction();
        
        expect(action!.type).toBe('FOLD');
        expect(action!.amount).toBeUndefined();
      });
    });

    describe('setConnectionStatus', () => {
      it('should set connected status', () => {
        service.setConnectionStatus('connected');
        expect(service.connectionStatus()).toBe('connected');
      });

      it('should set reconnecting status', () => {
        service.setConnectionStatus('reconnecting');
        expect(service.connectionStatus()).toBe('reconnecting');
      });
    });

    describe('reset', () => {
      it('should reset all state to initial values', () => {
        
        service.setGame(createMockGame());
        service.setLoading(true);
        service.setError('Some error');
        service.recordAction('FOLD', 'player-1', 'Alice');
        service.setConnectionStatus('connected');

        
        service.reset();

        
        expect(service.game()).toBeNull();
        expect(service.isLoading()).toBe(false);
        expect(service.error()).toBeNull();
        expect(service.lastAction()).toBeNull();
        expect(service.connectionStatus()).toBe('disconnected');
      });
    });

    describe('updateGame', () => {
      it('should partially update game', () => {
        const game = createMockGame({ currentPot: 100 });
        service.setGame(game);
        
        service.updateGame({ currentPot: 200 });
        
        expect(service.game()!.currentPot).toBe(200);
        expect(service.game()!.id).toBe('game-123'); 
      });

      it('should not update if no game exists', () => {
        service.updateGame({ currentPot: 200 });
        expect(service.game()).toBeNull();
      });
    });
  });

  
  
  

  describe('Computed Signals', () => {
    beforeEach(() => {
      service.setGame(createMockGame());
    });

    describe('players', () => {
      it('should return players array', () => {
        expect(service.players().length).toBe(3);
      });

      it('should return empty array when no game', () => {
        service.setGame(null);
        expect(service.players()).toEqual([]);
      });
    });

    describe('currentPlayer', () => {
      it('should return current player based on index', () => {
        const player = service.currentPlayer();
        expect(player).toBeTruthy();
        expect(player!.name).toBe('Alice');
      });

      it('should return null when no game', () => {
        service.setGame(null);
        expect(service.currentPlayer()).toBeNull();
      });

      it('should return null when currentPlayerIndex is undefined', () => {
        const game = createMockGame();
        game.currentPlayerIndex = undefined;
        service.setGame(game);
        expect(service.currentPlayer()).toBeNull();
      });

      it('should update when currentPlayerIndex changes', () => {
        expect(service.currentPlayer()!.name).toBe('Alice');
        
        service.updateGame({ currentPlayerIndex: 1 });
        expect(service.currentPlayer()!.name).toBe('Bot1');
      });
    });

    describe('humanPlayer', () => {
      it('should return non-bot player', () => {
        const human = service.humanPlayer();
        expect(human).toBeTruthy();
        expect(human!.name).toBe('Alice');
        expect(human!.isBot).toBe(false);
      });

      it('should return undefined when no human player', () => {
        const game = createMockGame({
          players: [
            createMockPlayer({ id: 'bot-1', name: 'Bot1', isBot: true }),
            createMockPlayer({ id: 'bot-2', name: 'Bot2', isBot: true }),
          ]
        });
        service.setGame(game);
        expect(service.humanPlayer()).toBeUndefined();
      });
    });

    describe('isHumanTurn', () => {
      it('should return true when human is current player', () => {
        
        expect(service.isHumanTurn()).toBe(true);
      });

      it('should return false when bot is current player', () => {
        service.updateGame({ currentPlayerIndex: 1 });
        expect(service.isHumanTurn()).toBe(false);
      });

      it('should return false when no game', () => {
        service.setGame(null);
        expect(service.isHumanTurn()).toBe(false);
      });
    });

    describe('potSize', () => {
      it('should return current pot', () => {
        expect(service.potSize()).toBe(100);
      });

      it('should return 0 when no game', () => {
        service.setGame(null);
        expect(service.potSize()).toBe(0);
      });
    });

    describe('communityCards', () => {
      it('should return empty array initially', () => {
        expect(service.communityCards()).toEqual([]);
      });

      it('should return community cards when present', () => {
        const cards = [
          createMockCard('HEARTS', 'ACE'),
          createMockCard('SPADES', 'KING'),
          createMockCard('DIAMONDS', 'QUEEN')
        ];
        const game = createMockGame({ communityCards: cards });
        service.setGame(game);
        expect(service.communityCards().length).toBe(3);
      });
    });

    describe('phase and phaseDisplayName', () => {
      it('should return current phase', () => {
        expect(service.phase()).toBe('PRE_FLOP');
      });

      it('should return display name for PRE_FLOP', () => {
        expect(service.phaseDisplayName()).toBe('Pre-Flop');
      });

      it('should return display name for FLOP', () => {
        service.updateGame({ phase: 'FLOP' });
        expect(service.phaseDisplayName()).toBe('Flop');
      });

      it('should return display name for TURN', () => {
        service.updateGame({ phase: 'TURN' });
        expect(service.phaseDisplayName()).toBe('Turn');
      });

      it('should return display name for RIVER', () => {
        service.updateGame({ phase: 'RIVER' });
        expect(service.phaseDisplayName()).toBe('River');
      });

      it('should return display name for SHOWDOWN', () => {
        service.updateGame({ phase: 'SHOWDOWN' });
        expect(service.phaseDisplayName()).toBe('Showdown');
      });

      it('should return raw phase for unknown phase', () => {
        service.updateGame({ phase: 'UNKNOWN_PHASE' });
        expect(service.phaseDisplayName()).toBe('UNKNOWN_PHASE');
      });
    });

    describe('currentBet', () => {
      it('should return current bet', () => {
        expect(service.currentBet()).toBe(20);
      });

      it('should return 0 when no game', () => {
        service.setGame(null);
        expect(service.currentBet()).toBe(0);
      });
    });

    describe('minRaiseAmount', () => {
      it('should calculate min raise as currentBet + minRaiseAmount', () => {
        
        expect(service.minRaiseAmount()).toBe(40);
      });

      it('should use bigBlind as fallback', () => {
        const game = createMockGame({ 
          currentBet: 20, 
          minRaiseAmount: undefined, 
          bigBlind: 30 
        });
        service.setGame(game);
        expect(service.minRaiseAmount()).toBe(50); 
      });

      it('should return 20 when no game', () => {
        service.setGame(null);
        expect(service.minRaiseAmount()).toBe(20);
      });
    });

    describe('canCheck', () => {
      it('should return true when human bet equals current bet', () => {
        
        expect(service.canCheck()).toBe(true);
      });

      it('should return false when human bet is less than current bet', () => {
        service.updateGame({ currentBet: 40 });
        expect(service.canCheck()).toBe(false);
      });

      it('should return true when human bet exceeds current bet', () => {
        service.updateGame({ currentBet: 10 });
        expect(service.canCheck()).toBe(true);
      });
    });

    describe('callAmount', () => {
      it('should return 0 when bets are equal', () => {
        expect(service.callAmount()).toBe(0);
      });

      it('should calculate difference when call is needed', () => {
        service.updateGame({ currentBet: 50 });
        expect(service.callAmount()).toBe(30); 
      });

      it('should never return negative', () => {
        service.updateGame({ currentBet: 10 });
        expect(service.callAmount()).toBe(0);
      });
    });

    describe('canCall', () => {
      it('should return false when no call needed', () => {
        expect(service.canCall()).toBe(false);
      });

      it('should return true when call is needed and affordable', () => {
        service.updateGame({ currentBet: 50 });
        
        expect(service.canCall()).toBe(true);
      });

      it('should return false when call exceeds chips', () => {
        service.updateGame({ currentBet: 2000 });
        expect(service.canCall()).toBe(false);
      });
    });

    describe('canPlayerAct', () => {
      it('should return true when human turn and can act', () => {
        expect(service.canPlayerAct()).toBe(true);
      });

      it('should return false when not human turn', () => {
        service.updateGame({ currentPlayerIndex: 1 });
        expect(service.canPlayerAct()).toBe(false);
      });

      it('should return false when human has folded', () => {
        const game = createMockGame();
        game.players[0].folded = true;
        service.setGame(game);
        expect(service.canPlayerAct()).toBe(false);
      });

      it('should return false when human is all-in', () => {
        const game = createMockGame();
        game.players[0].isAllIn = true;
        service.setGame(game);
        expect(service.canPlayerAct()).toBe(false);
      });

      it('should return false when game is finished', () => {
        service.updateGame({ isFinished: true });
        expect(service.canPlayerAct()).toBe(false);
      });
    });

    describe('isGameFinished', () => {
      it('should return false for active game', () => {
        expect(service.isGameFinished()).toBe(false);
      });

      it('should return true when isFinished is true', () => {
        service.updateGame({ isFinished: true });
        expect(service.isGameFinished()).toBe(true);
      });

      it('should return true when phase is SHOWDOWN', () => {
        service.updateGame({ phase: 'SHOWDOWN' });
        expect(service.isGameFinished()).toBe(true);
      });
    });

    describe('activePlayers', () => {
      it('should return all non-folded players', () => {
        expect(service.activePlayers().length).toBe(3);
      });

      it('should exclude folded players', () => {
        const game = createMockGame();
        game.players[1].folded = true;
        service.setGame(game);
        expect(service.activePlayers().length).toBe(2);
      });
    });

    describe('activePlayerCount', () => {
      it('should count active players', () => {
        expect(service.activePlayerCount()).toBe(3);
      });
    });

    describe('activeBots', () => {
      it('should return only active bots', () => {
        expect(service.activeBots().length).toBe(2);
      });

      it('should exclude folded bots', () => {
        const game = createMockGame();
        game.players[1].folded = true;
        service.setGame(game);
        expect(service.activeBots().length).toBe(1);
      });

      it('should exclude all-in bots', () => {
        const game = createMockGame();
        game.players[1].isAllIn = true;
        service.setGame(game);
        expect(service.activeBots().length).toBe(1);
      });

      it('should exclude bots with no chips', () => {
        const game = createMockGame();
        game.players[1].chips = 0;
        service.setGame(game);
        expect(service.activeBots().length).toBe(1);
      });
    });

    describe('dealerPosition', () => {
      it('should return dealer position', () => {
        expect(service.dealerPosition()).toBe(2);
      });

      it('should return 0 when no game', () => {
        service.setGame(null);
        expect(service.dealerPosition()).toBe(0);
      });
    });

    describe('winnerName and winningHandDescription', () => {
      it('should return undefined when no winner', () => {
        expect(service.winnerName()).toBeUndefined();
        expect(service.winningHandDescription()).toBeUndefined();
      });

      it('should return winner info when set', () => {
        const game = createMockGame({
          winnerName: 'Alice',
          winningHandDescription: 'Full House'
        });
        service.setGame(game);
        expect(service.winnerName()).toBe('Alice');
        expect(service.winningHandDescription()).toBe('Full House');
      });
    });

    describe('human player derived signals', () => {
      it('should compute humanChips', () => {
        expect(service.humanChips()).toBe(980);
      });

      it('should compute humanBet', () => {
        expect(service.humanBet()).toBe(20);
      });

      it('should compute humanHand', () => {
        expect(service.humanHand()).toEqual([]);
      });

      it('should compute isHumanFolded', () => {
        expect(service.isHumanFolded()).toBe(false);
      });

      it('should compute isHumanAllIn', () => {
        expect(service.isHumanAllIn()).toBe(false);
      });
    });
  });

  
  
  

  describe('Utility Methods', () => {
    beforeEach(() => {
      service.setGame(createMockGame());
    });

    describe('isPlayerTurn', () => {
      it('should return true for current player', () => {
        expect(service.isPlayerTurn('human-1')).toBe(true);
      });

      it('should return false for non-current player', () => {
        expect(service.isPlayerTurn('bot-1')).toBe(false);
      });

      it('should return false when no current player', () => {
        service.setGame(null);
        expect(service.isPlayerTurn('human-1')).toBe(false);
      });
    });

    describe('getPlayerById', () => {
      it('should return player when found', () => {
        const player = service.getPlayerById('human-1');
        expect(player).toBeTruthy();
        expect(player!.name).toBe('Alice');
      });

      it('should return undefined when not found', () => {
        expect(service.getPlayerById('non-existent')).toBeUndefined();
      });
    });

    describe('isDealer', () => {
      it('should return true for dealer position', () => {
        expect(service.isDealer(2)).toBe(true);
      });

      it('should return false for non-dealer position', () => {
        expect(service.isDealer(0)).toBe(false);
        expect(service.isDealer(1)).toBe(false);
      });
    });

    describe('getPlayerStatus', () => {
      it('should return "Folded" for folded player', () => {
        const player = createMockPlayer({ folded: true });
        expect(service.getPlayerStatus(player)).toBe('Folded');
      });

      it('should return "All-In" for all-in player', () => {
        const player = createMockPlayer({ isAllIn: true });
        expect(service.getPlayerStatus(player)).toBe('All-In');
      });

      it('should return "Out" for player with no chips', () => {
        const player = createMockPlayer({ chips: 0 });
        expect(service.getPlayerStatus(player)).toBe('Out');
      });

      it('should return empty string for active player', () => {
        const player = createMockPlayer();
        expect(service.getPlayerStatus(player)).toBe('');
      });

      it('should prioritize folded over all-in', () => {
        const player = createMockPlayer({ folded: true, isAllIn: true });
        expect(service.getPlayerStatus(player)).toBe('Folded');
      });
    });
  });

  
  
  

  describe('Signal Reactivity', () => {
    it('should update derived signals when game changes', () => {
      service.setGame(createMockGame({ currentPot: 100 }));
      expect(service.potSize()).toBe(100);

      service.updateGame({ currentPot: 500 });
      expect(service.potSize()).toBe(500);
    });

    it('should update isHumanTurn when currentPlayerIndex changes', () => {
      service.setGame(createMockGame({ currentPlayerIndex: 0 }));
      expect(service.isHumanTurn()).toBe(true);

      service.updateGame({ currentPlayerIndex: 1 });
      expect(service.isHumanTurn()).toBe(false);
    });

    it('should update canCheck when bet amounts change', () => {
      service.setGame(createMockGame({ currentBet: 20 }));
      expect(service.canCheck()).toBe(true);

      service.updateGame({ currentBet: 100 });
      expect(service.canCheck()).toBe(false);
    });

    it('should handle rapid state updates', () => {
      service.setGame(createMockGame());
      
      for (let i = 0; i < 100; i++) {
        service.updateGame({ currentPot: i * 10 });
      }

      expect(service.potSize()).toBe(990);
    });
  });

  
  
  

  describe('Edge Cases', () => {
    it('should handle game with empty players array', () => {
      const game = createMockGame({ players: [] });
      service.setGame(game);
      
      expect(service.players()).toEqual([]);
      expect(service.humanPlayer()).toBeUndefined();
      expect(service.currentPlayer()).toBeNull();
      expect(service.activePlayers()).toEqual([]);
    });

    it('should handle game with only bots', () => {
      const game = createMockGame({
        players: [
          createMockPlayer({ id: 'bot-1', name: 'Bot1', isBot: true }),
          createMockPlayer({ id: 'bot-2', name: 'Bot2', isBot: true }),
        ]
      });
      service.setGame(game);

      expect(service.humanPlayer()).toBeUndefined();
      expect(service.isHumanTurn()).toBe(false);
      expect(service.canPlayerAct()).toBe(false);
    });

    it('should handle zero chip amounts', () => {
      const game = createMockGame({
        currentPot: 0,
        currentBet: 0,
        players: [
          createMockPlayer({ chips: 0, betAmount: 0 }),
        ]
      });
      service.setGame(game);

      expect(service.potSize()).toBe(0);
      expect(service.currentBet()).toBe(0);
      expect(service.callAmount()).toBe(0);
      expect(service.canCheck()).toBe(true);
    });

    it('should handle player with Bot-like name but not marked as bot', () => {
      const game = createMockGame({
        players: [
          createMockPlayer({ id: 'human-bot', name: 'Bot-like', isBot: false }),
        ]
      });
      service.setGame(game);

      
      expect(service.humanPlayer()).toBeUndefined();
    });
  });
});
