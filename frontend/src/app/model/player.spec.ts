import { Player } from './player';

describe('Player', () => {
  let player: Player;

  beforeEach(() => {
    player = new Player();
  });

  describe('Construction', () => {
    it('should create an instance with default values', () => {
      expect(player).toBeTruthy();
      expect(player.id).toBe('');
      expect(player.name).toBe('');
      expect(player.chips).toBe(0);
      expect(player.betAmount).toBe(0);
      expect(player.totalBetInRound).toBe(0);
      expect(player.folded).toBe(false);
      expect(player.isBot).toBe(false);
      expect(player.isAllIn).toBe(false);
      expect(player.hasActed).toBe(false);
      expect(player.seatPosition).toBe(0);
      expect(player.hand).toEqual([]);
    });
  });

  describe('canAct', () => {
    it('should return true for active player with chips', () => {
      player.chips = 1000;
      player.folded = false;
      player.isAllIn = false;

      expect(player.canAct()).toBe(true);
    });

    it('should return false for folded player', () => {
      player.chips = 1000;
      player.folded = true;

      expect(player.canAct()).toBe(false);
    });

    it('should return false for all-in player', () => {
      player.chips = 0;
      player.isAllIn = true;

      expect(player.canAct()).toBe(false);
    });

    it('should return false for player with no chips', () => {
      player.chips = 0;
      player.folded = false;
      player.isAllIn = false;

      expect(player.canAct()).toBe(false);
    });
  });

  describe('getDisplayName', () => {
    it('should return name without Bot prefix for bots', () => {
      player.name = 'Bot1';
      expect(player.getDisplayName()).toBe('1');
    });

    it('should return "Bot" for player named just "Bot"', () => {
      player.name = 'Bot';
      expect(player.getDisplayName()).toBe('Bot');
    });

    it('should return actual name for human players', () => {
      player.name = 'Alice';
      expect(player.getDisplayName()).toBe('Alice');
    });

    it('should return "Anonymous" for empty name', () => {
      player.name = '';
      expect(player.getDisplayName()).toBe('Anonymous');
    });
  });

  describe('isHuman', () => {
    it('should return true for non-bot player without Bot prefix', () => {
      player.isBot = false;
      player.name = 'Alice';

      expect(player.isHuman()).toBe(true);
    });

    it('should return false for bot player', () => {
      player.isBot = true;
      player.name = 'Bot1';

      expect(player.isHuman()).toBe(false);
    });

    it('should return false for player with Bot-prefixed name', () => {
      player.isBot = false;
      player.name = 'BotPlayer';

      expect(player.isHuman()).toBe(false);
    });
  });

  describe('getStatusText', () => {
    it('should return "Folded" for folded player', () => {
      player.folded = true;
      expect(player.getStatusText()).toBe('Folded');
    });

    it('should return "All-In" for all-in player', () => {
      player.isAllIn = true;
      expect(player.getStatusText()).toBe('All-In');
    });

    it('should return "Out" for player with no chips', () => {
      player.chips = 0;
      expect(player.getStatusText()).toBe('Out');
    });

    it('should return empty string for active player', () => {
      player.chips = 1000;
      player.folded = false;
      player.isAllIn = false;

      expect(player.getStatusText()).toBe('');
    });

    it('should prioritize folded status', () => {
      player.folded = true;
      player.isAllIn = true;
      player.chips = 0;

      expect(player.getStatusText()).toBe('Folded');
    });

    it('should prioritize all-in over out', () => {
      player.isAllIn = true;
      player.chips = 0;

      expect(player.getStatusText()).toBe('All-In');
    });
  });
});
