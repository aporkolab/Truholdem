import { TestBed } from '@angular/core/testing';
import { PlayerService } from './player.service';
import { PlayerInfo } from '../register-players/register-players.component';

describe('PlayerService', () => {
  let service: PlayerService;

  const createMockPlayerInfo = (overrides: Partial<PlayerInfo> = {}): PlayerInfo => ({
    name: overrides.name || 'TestPlayer',
    startingChips: overrides.startingChips ?? 1000,
    isBot: overrides.isBot ?? false
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PlayerService]
    });
    service = TestBed.inject(PlayerService);
  });

  describe('Initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should start with empty players array', () => {
      expect(service.getPlayers()).toEqual([]);
    });

    it('should have players$ observable', () => {
      expect(service.players$).toBeDefined();
    });
  });

  describe('setPlayers', () => {
    it('should set players array', () => {
      const players: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Alice' }),
        createMockPlayerInfo({ name: 'Bob', isBot: true })
      ];

      service.setPlayers(players);

      expect(service.getPlayers()).toEqual(players);
    });

    it('should emit players through observable', (done) => {
      const players: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Alice' })
      ];

      service.players$.subscribe((emittedPlayers) => {
        if (emittedPlayers.length > 0) {
          expect(emittedPlayers).toEqual(players);
          done();
        }
      });

      service.setPlayers(players);
    });

    it('should replace existing players', () => {
      const firstPlayers: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Alice' })
      ];
      const secondPlayers: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Bob' }),
        createMockPlayerInfo({ name: 'Charlie' })
      ];

      service.setPlayers(firstPlayers);
      expect(service.getPlayers()).toEqual(firstPlayers);

      service.setPlayers(secondPlayers);
      expect(service.getPlayers()).toEqual(secondPlayers);
    });

    it('should handle empty array', () => {
      service.setPlayers([createMockPlayerInfo()]);
      service.setPlayers([]);

      expect(service.getPlayers()).toEqual([]);
    });

    it('should handle single player', () => {
      const player = createMockPlayerInfo({ name: 'SinglePlayer' });
      service.setPlayers([player]);

      expect(service.getPlayers().length).toBe(1);
      expect(service.getPlayers()[0].name).toBe('SinglePlayer');
    });

    it('should handle multiple players', () => {
      const players: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Player1' }),
        createMockPlayerInfo({ name: 'Player2' }),
        createMockPlayerInfo({ name: 'Player3' }),
        createMockPlayerInfo({ name: 'Player4' })
      ];

      service.setPlayers(players);

      expect(service.getPlayers().length).toBe(4);
    });

    it('should preserve player properties', () => {
      const player: PlayerInfo = {
        name: 'CustomPlayer',
        startingChips: 2500,
        isBot: true
      };

      service.setPlayers([player]);

      const retrieved = service.getPlayers()[0];
      expect(retrieved.name).toBe('CustomPlayer');
      expect(retrieved.startingChips).toBe(2500);
      expect(retrieved.isBot).toBe(true);
    });
  });

  describe('getPlayers', () => {
    it('should return current players', () => {
      const players: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Alice' })
      ];

      service.setPlayers(players);

      expect(service.getPlayers()).toEqual(players);
    });

    it('should return empty array initially', () => {
      expect(service.getPlayers()).toEqual([]);
    });

    it('should return reference to current value', () => {
      const players: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Test' })
      ];

      service.setPlayers(players);

      const retrieved = service.getPlayers();
      expect(retrieved).toEqual(players);
    });
  });

  describe('players$ observable', () => {
    it('should emit initial empty array', (done) => {
      let emissions = 0;
      
      service.players$.subscribe((players) => {
        emissions++;
        if (emissions === 1) {
          expect(players).toEqual([]);
          done();
        }
      });
    });

    it('should emit on every setPlayers call', (done) => {
      const emittedValues: PlayerInfo[][] = [];

      service.players$.subscribe((players) => {
        emittedValues.push([...players]);
        
        if (emittedValues.length === 3) {
          expect(emittedValues[0]).toEqual([]);
          expect(emittedValues[1].length).toBe(1);
          expect(emittedValues[2].length).toBe(2);
          done();
        }
      });

      service.setPlayers([createMockPlayerInfo({ name: 'First' })]);
      service.setPlayers([
        createMockPlayerInfo({ name: 'Second1' }),
        createMockPlayerInfo({ name: 'Second2' })
      ]);
    });

    it('should allow multiple subscribers', (done) => {
      let sub1Called = false;
      let sub2Called = false;

      const players = [createMockPlayerInfo()];

      service.players$.subscribe((p) => {
        if (p.length > 0) sub1Called = true;
      });

      service.players$.subscribe((p) => {
        if (p.length > 0) sub2Called = true;
        if (sub1Called && sub2Called) done();
      });

      service.setPlayers(players);
    });
  });

  describe('Edge Cases', () => {
    it('should handle players with special characters in name', () => {
      const player = createMockPlayerInfo({ name: "Player's Name & <test>" });
      service.setPlayers([player]);

      expect(service.getPlayers()[0].name).toBe("Player's Name & <test>");
    });

    it('should handle players with zero chips', () => {
      const player = createMockPlayerInfo({ startingChips: 0 });
      service.setPlayers([player]);

      expect(service.getPlayers()[0].startingChips).toBe(0);
    });

    it('should handle players with large chip counts', () => {
      const player = createMockPlayerInfo({ startingChips: 1000000000 });
      service.setPlayers([player]);

      expect(service.getPlayers()[0].startingChips).toBe(1000000000);
    });

    it('should handle mixed human and bot players', () => {
      const players: PlayerInfo[] = [
        createMockPlayerInfo({ name: 'Human', isBot: false }),
        createMockPlayerInfo({ name: 'Bot1', isBot: true }),
        createMockPlayerInfo({ name: 'Bot2', isBot: true })
      ];

      service.setPlayers(players);

      const humanCount = service.getPlayers().filter(p => !p.isBot).length;
      const botCount = service.getPlayers().filter(p => p.isBot).length;

      expect(humanCount).toBe(1);
      expect(botCount).toBe(2);
    });
  });
});
