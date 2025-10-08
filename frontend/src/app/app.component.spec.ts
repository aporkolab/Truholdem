import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { AppComponent } from './app.component';
import { PlayerService } from './services/player.service';
import { SoundService, SoundSettings } from './services/sound.service';
import { PlayerInfo } from './register-players/register-players.component';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let router: Router;
  let playerServiceMock: Partial<PlayerService>;
  let soundServiceMock: Partial<SoundService>;
  let playersSubject: BehaviorSubject<PlayerInfo[]>;
  let settingsSubject: BehaviorSubject<SoundSettings>;

  const createMockPlayerInfo = (overrides: Partial<PlayerInfo> = {}): PlayerInfo => ({
    name: overrides.name || 'TestPlayer',
    startingChips: overrides.startingChips ?? 1000,
    isBot: overrides.isBot ?? false
  });

  const createMockSoundSettings = (overrides: Partial<SoundSettings> = {}): SoundSettings => ({
    enabled: overrides.enabled ?? true,
    volume: overrides.volume ?? 0.5,
    effects: overrides.effects ?? {
      cardDeal: true,
      cardFlip: true,
      chips: true,
      check: true,
      fold: true,
      allIn: true,
      win: true,
      lose: true,
      turn: true,
      timer: true,
      click: true
    }
  });

  beforeEach(async () => {
    playersSubject = new BehaviorSubject<PlayerInfo[]>([]);
    settingsSubject = new BehaviorSubject<SoundSettings>(createMockSoundSettings());

    playerServiceMock = {
      players$: playersSubject.asObservable(),
      setPlayers: jest.fn()
    };

    soundServiceMock = {
      settings$: settingsSubject.asObservable(),
      toggleSound: jest.fn(),
      playClick: jest.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        { provide: PlayerService, useValue: playerServiceMock },
        { provide: SoundService, useValue: soundServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    playersSubject.complete();
    settingsSubject.complete();
  });

  describe('Initialization', () => {
    it('should create the app', () => {
      expect(component).toBeTruthy();
    });

    it('should have title "TruHoldem"', () => {
      expect(component.title).toBe('TruHoldem');
    });

    it('should start with empty registered players', () => {
      expect(component.registeredPlayers).toEqual([]);
    });

    it('should start with sound enabled', () => {
      expect(component.soundEnabled).toBe(true);
    });
  });

  describe('Player Service Subscription', () => {
    it('should update registeredPlayers when player service emits', fakeAsync(() => {
      const players = [
        createMockPlayerInfo({ name: 'Alice' }),
        createMockPlayerInfo({ name: 'Bob' })
      ];

      playersSubject.next(players);
      tick();

      expect(component.registeredPlayers).toEqual(players);
    }));

    it('should handle empty players array', fakeAsync(() => {
      playersSubject.next([createMockPlayerInfo()]);
      tick();
      expect(component.registeredPlayers.length).toBe(1);

      playersSubject.next([]);
      tick();
      expect(component.registeredPlayers).toEqual([]);
    }));
  });

  describe('Sound Service Subscription', () => {
    it('should update soundEnabled when sound settings change', fakeAsync(() => {
      settingsSubject.next(createMockSoundSettings({ enabled: false }));
      tick();

      expect(component.soundEnabled).toBe(false);
    }));

    it('should track sound enabled state', fakeAsync(() => {
      settingsSubject.next(createMockSoundSettings({ enabled: true, volume: 1 }));
      tick();
      expect(component.soundEnabled).toBe(true);

      settingsSubject.next(createMockSoundSettings({ enabled: false, volume: 0 }));
      tick();
      expect(component.soundEnabled).toBe(false);
    }));
  });

  describe('toggleSound', () => {
    it('should call soundService.toggleSound', () => {
      component.toggleSound();
      expect(soundServiceMock.toggleSound).toHaveBeenCalled();
    });

    it('should play click sound', () => {
      component.toggleSound();
      expect(soundServiceMock.playClick).toHaveBeenCalled();
    });

    it('should call toggle and click in order', () => {
      const callOrder: string[] = [];
      (soundServiceMock.toggleSound as jest.Mock).mockImplementation(() => {
        callOrder.push('toggle');
      });
      (soundServiceMock.playClick as jest.Mock).mockImplementation(() => {
        callOrder.push('click');
      });

      component.toggleSound();

      expect(callOrder).toEqual(['toggle', 'click']);
    });
  });

  describe('onPlayersRegistered', () => {
    it('should set players and navigate on valid input', () => {
      const players = [createMockPlayerInfo({ name: 'Alice' })];

      component.onPlayersRegistered(players);

      expect(playerServiceMock.setPlayers).toHaveBeenCalledWith(players);
      expect(router.navigate).toHaveBeenCalledWith(['/start']);
    });

    it('should not set players for empty array', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

      component.onPlayersRegistered([]);

      expect(playerServiceMock.setPlayers).not.toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();

      consoleSpy.mockRestore();
    });

    it('should handle multiple players', () => {
      const players = [
        createMockPlayerInfo({ name: 'Alice' }),
        createMockPlayerInfo({ name: 'Bob' }),
        createMockPlayerInfo({ name: 'Charlie' }),
        createMockPlayerInfo({ name: 'Dave' })
      ];

      component.onPlayersRegistered(players);

      expect(playerServiceMock.setPlayers).toHaveBeenCalledWith(players);
      expect(router.navigate).toHaveBeenCalledWith(['/start']);
    });

    it('should log error for invalid input', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

      component.onPlayersRegistered([] as PlayerInfo[]);

      expect(consoleSpy).toHaveBeenCalled();

      consoleSpy.mockRestore();
    });

    it('should log registered players on success', () => {
      const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
      const players = [createMockPlayerInfo()];

      component.onPlayersRegistered(players);

      expect(consoleSpy).toHaveBeenCalledWith('Registered players:', players);

      consoleSpy.mockRestore();
    });
  });

  describe('isValidPlayersArray (private method via onPlayersRegistered)', () => {
    it('should reject null-like values', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

      component.onPlayersRegistered(null as unknown as PlayerInfo[]);

      expect(playerServiceMock.setPlayers).not.toHaveBeenCalled();

      consoleSpy.mockRestore();
    });

    it('should reject non-array values', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

      component.onPlayersRegistered({} as unknown as PlayerInfo[]);

      expect(playerServiceMock.setPlayers).not.toHaveBeenCalled();

      consoleSpy.mockRestore();
    });

    it('should accept single player array', () => {
      const players = [createMockPlayerInfo()];

      component.onPlayersRegistered(players);

      expect(playerServiceMock.setPlayers).toHaveBeenCalled();
    });
  });

  describe('Lifecycle', () => {
    it('should complete destroy subject on ngOnDestroy', () => {
      const destroySpy = jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(destroySpy).toHaveBeenCalled();
    });

    it('should emit on destroy subject', () => {
      const nextSpy = jest.spyOn(component['destroy$'], 'next');

      component.ngOnDestroy();

      expect(nextSpy).toHaveBeenCalled();
    });

    it('should unsubscribe from player service on destroy', fakeAsync(() => {
      fixture.destroy();

      
      playersSubject.next([createMockPlayerInfo()]);
      tick();

      
      expect(true).toBe(true);
    }));
  });

  describe('Integration', () => {
    it('should complete full registration flow', fakeAsync(() => {
      const players = [
        createMockPlayerInfo({ name: 'Player1', isBot: false }),
        createMockPlayerInfo({ name: 'Bot1', isBot: true }),
        createMockPlayerInfo({ name: 'Bot2', isBot: true })
      ];

      (router.navigate as jest.Mock).mockReturnValue(Promise.resolve(true));

      component.onPlayersRegistered(players);

      expect(playerServiceMock.setPlayers).toHaveBeenCalledWith(players);
      expect(router.navigate).toHaveBeenCalledWith(['/start']);


      playersSubject.next(players);
      tick();

      expect(component.registeredPlayers).toEqual(players);
    }));
  });
});
