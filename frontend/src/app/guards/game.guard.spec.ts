import { TestBed } from '@angular/core/testing';
import { UrlTree, ActivatedRouteSnapshot, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { gameGuard, replayGuard } from './game.guard';
import { PlayerService } from '../services/player.service';

describe('Game Guards', () => {
  let playerService: jasmine.SpyObj<PlayerService>;

  beforeEach(() => {
    const playerSpy = jasmine.createSpyObj('PlayerService', ['getPlayers']);

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        { provide: PlayerService, useValue: playerSpy }
      ]
    });

    playerService = TestBed.inject(PlayerService) as jasmine.SpyObj<PlayerService>;
  });

  describe('gameGuard', () => {
    it('should allow access when there are at least 2 players', () => {
      playerService.getPlayers.and.returnValue([
        { name: 'Player 1', startingChips: 1000, isBot: false },
        { name: 'Bot 1', startingChips: 1000, isBot: true }
      ]);

      const result = TestBed.runInInjectionContext(() =>
        gameGuard({} as never, {} as never)
      );

      expect(result).toBe(true);
    });

    it('should redirect to lobby when there are less than 2 players', () => {
      playerService.getPlayers.and.returnValue([
        { name: 'Player 1', startingChips: 1000, isBot: false }
      ]);

      const result = TestBed.runInInjectionContext(() =>
        gameGuard({} as never, {} as never)
      );

      expect(result instanceof UrlTree).toBe(true);
      expect((result as UrlTree).toString()).toBe('/lobby');
    });

    it('should redirect to lobby when there are no players', () => {
      playerService.getPlayers.and.returnValue([]);

      const result = TestBed.runInInjectionContext(() =>
        gameGuard({} as never, {} as never)
      );

      expect(result instanceof UrlTree).toBe(true);
      expect((result as UrlTree).toString()).toBe('/lobby');
    });

    it('should allow access with exactly 2 players', () => {
      playerService.getPlayers.and.returnValue([
        { name: 'Player 1', startingChips: 1000, isBot: false },
        { name: 'Bot 1', startingChips: 1000, isBot: true }
      ]);

      const result = TestBed.runInInjectionContext(() =>
        gameGuard({} as never, {} as never)
      );

      expect(result).toBe(true);
    });

    it('should allow access with 4 players', () => {
      playerService.getPlayers.and.returnValue([
        { name: 'Player 1', startingChips: 1000, isBot: false },
        { name: 'Bot 1', startingChips: 1000, isBot: true },
        { name: 'Bot 2', startingChips: 1000, isBot: true },
        { name: 'Bot 3', startingChips: 1000, isBot: true }
      ]);

      const result = TestBed.runInInjectionContext(() =>
        gameGuard({} as never, {} as never)
      );

      expect(result).toBe(true);
    });
  });

  describe('replayGuard', () => {
    function createRouteSnapshot(handId: string | null): ActivatedRouteSnapshot {
      return {
        paramMap: convertToParamMap(handId ? { handId } : {})
      } as ActivatedRouteSnapshot;
    }

    it('should allow access when handId is provided', () => {
      const route = createRouteSnapshot('abc123');

      const result = TestBed.runInInjectionContext(() =>
        replayGuard(route, {} as never)
      );

      expect(result).toBe(true);
    });

    it('should redirect to history when handId is missing', () => {
      const route = createRouteSnapshot(null);

      const result = TestBed.runInInjectionContext(() =>
        replayGuard(route, {} as never)
      );

      expect(result instanceof UrlTree).toBe(true);
      expect((result as UrlTree).toString()).toBe('/history');
    });

    it('should redirect to history when handId is empty string', () => {
      const route = createRouteSnapshot('');

      const result = TestBed.runInInjectionContext(() =>
        replayGuard(route, {} as never)
      );

      expect(result instanceof UrlTree).toBe(true);
      expect((result as UrlTree).toString()).toBe('/history');
    });

    it('should allow access with UUID-style handId', () => {
      const route = createRouteSnapshot('550e8400-e29b-41d4-a716-446655440000');

      const result = TestBed.runInInjectionContext(() =>
        replayGuard(route, {} as never)
      );

      expect(result).toBe(true);
    });

    it('should allow access with numeric handId', () => {
      const route = createRouteSnapshot('12345');

      const result = TestBed.runInInjectionContext(() =>
        replayGuard(route, {} as never)
      );

      expect(result).toBe(true);
    });
  });
});
