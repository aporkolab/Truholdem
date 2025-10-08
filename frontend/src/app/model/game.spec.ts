import { Game } from './game';
import { Player } from './player';

describe('Game', () => {
  let game: Game;

  const createPlayer = (overrides: Partial<Player> = {}): Player => {
    const player = new Player();
    player.id = overrides.id || 'player-1';
    player.name = overrides.name || 'TestPlayer';
    player.chips = overrides.chips ?? 1000;
    player.folded = overrides.folded ?? false;
    player.isAllIn = overrides.isAllIn ?? false;
    return player;
  };

  beforeEach(() => {
    game = new Game();
  });

  describe('Construction', () => {
    it('should create an instance with default values', () => {
      expect(game).toBeTruthy();
      expect(game.currentPot).toBe(0);
      expect(game.players).toEqual([]);
      expect(game.communityCards).toEqual([]);
      expect(game.phase).toBe('PRE_FLOP');
      expect(game.currentBet).toBe(0);
      expect(game.smallBlind).toBe(10);
      expect(game.bigBlind).toBe(20);
      expect(game.minRaiseAmount).toBe(20);
      expect(game.isFinished).toBe(false);
      expect(game.handNumber).toBe(1);
    });
  });

  describe('getPhaseDisplayName', () => {
    it('should return "Pre-Flop" for PRE_FLOP phase', () => {
      game.phase = 'PRE_FLOP';
      expect(game.getPhaseDisplayName()).toBe('Pre-Flop');
    });

    it('should return "Flop" for FLOP phase', () => {
      game.phase = 'FLOP';
      expect(game.getPhaseDisplayName()).toBe('Flop');
    });

    it('should return "Turn" for TURN phase', () => {
      game.phase = 'TURN';
      expect(game.getPhaseDisplayName()).toBe('Turn');
    });

    it('should return "River" for RIVER phase', () => {
      game.phase = 'RIVER';
      expect(game.getPhaseDisplayName()).toBe('River');
    });

    it('should return "Showdown" for SHOWDOWN phase', () => {
      game.phase = 'SHOWDOWN';
      expect(game.getPhaseDisplayName()).toBe('Showdown');
    });

    it('should return raw phase for unknown phase', () => {
      game.phase = 'UNKNOWN_PHASE';
      expect(game.getPhaseDisplayName()).toBe('UNKNOWN_PHASE');
    });
  });

  describe('hasFinished', () => {
    it('should return true when isFinished is true', () => {
      game.isFinished = true;
      expect(game.hasFinished()).toBe(true);
    });

    it('should return true when phase is SHOWDOWN', () => {
      game.phase = 'SHOWDOWN';
      expect(game.hasFinished()).toBe(true);
    });

    it('should return false for ongoing game', () => {
      game.isFinished = false;
      game.phase = 'FLOP';
      expect(game.hasFinished()).toBe(false);
    });
  });

  describe('getActivePlayersCount', () => {
    it('should return 0 for empty players array', () => {
      game.players = [];
      expect(game.getActivePlayersCount()).toBe(0);
    });

    it('should count non-folded players', () => {
      game.players = [
        createPlayer({ folded: false }),
        createPlayer({ folded: false }),
        createPlayer({ folded: true })
      ];
      expect(game.getActivePlayersCount()).toBe(2);
    });

    it('should return total count when no one folded', () => {
      game.players = [
        createPlayer({ folded: false }),
        createPlayer({ folded: false }),
        createPlayer({ folded: false })
      ];
      expect(game.getActivePlayersCount()).toBe(3);
    });
  });

  describe('getCurrentPlayer', () => {
    beforeEach(() => {
      game.players = [
        createPlayer({ id: 'p1', name: 'Alice' }),
        createPlayer({ id: 'p2', name: 'Bob' }),
        createPlayer({ id: 'p3', name: 'Charlie' })
      ];
    });

    it('should return current player by index', () => {
      game.currentPlayerIndex = 1;
      const player = game.getCurrentPlayer();

      expect(player).toBeTruthy();
      expect(player!.name).toBe('Bob');
    });

    it('should return undefined when index is undefined', () => {
      game.currentPlayerIndex = undefined;
      expect(game.getCurrentPlayer()).toBeUndefined();
    });

    it('should return undefined when index is out of bounds', () => {
      game.currentPlayerIndex = 10;
      expect(game.getCurrentPlayer()).toBeUndefined();
    });

    it('should return first player for index 0', () => {
      game.currentPlayerIndex = 0;
      const player = game.getCurrentPlayer();

      expect(player).toBeTruthy();
      expect(player!.name).toBe('Alice');
    });
  });

  describe('isPlayerTurn', () => {
    beforeEach(() => {
      game.players = [
        createPlayer({ id: 'alice-id', name: 'Alice' }),
        createPlayer({ id: 'bob-id', name: 'Bob' })
      ];
      game.currentPlayerIndex = 0;
    });

    it('should return true for current player', () => {
      expect(game.isPlayerTurn('alice-id')).toBe(true);
    });

    it('should return false for non-current player', () => {
      expect(game.isPlayerTurn('bob-id')).toBe(false);
    });

    it('should return false for non-existent player', () => {
      expect(game.isPlayerTurn('unknown-id')).toBe(false);
    });

    it('should return false when no current player', () => {
      game.currentPlayerIndex = undefined;
      expect(game.isPlayerTurn('alice-id')).toBe(false);
    });
  });

  describe('Optional Properties', () => {
    it('should allow setting winner info', () => {
      game.winnerName = 'Alice';
      game.winningHandDescription = 'Royal Flush';
      game.winnerIds = ['alice-id'];

      expect(game.winnerName).toBe('Alice');
      expect(game.winningHandDescription).toBe('Royal Flush');
      expect(game.winnerIds).toContain('alice-id');
    });

    it('should allow setting betting info', () => {
      game.lastRaiseAmount = 100;
      game.minRaiseAmount = 50;

      expect(game.lastRaiseAmount).toBe(100);
      expect(game.minRaiseAmount).toBe(50);
    });
  });
});
