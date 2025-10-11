import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RegisterPlayersComponent } from './register-players.component';
import { PlayerService } from '../services/player.service';
import { FormsModule } from '@angular/forms';

describe('RegisterPlayersComponent', () => {
  let component: RegisterPlayersComponent;
  let fixture: ComponentFixture<RegisterPlayersComponent>;
  let httpMock: HttpTestingController;
  let routerMock: jest.Mocked<Router>;
  let playerServiceMock: jest.Mocked<PlayerService>;

  beforeEach(async () => {
    routerMock = {
      navigate: jest.fn().mockReturnValue(Promise.resolve(true))
    } as unknown as jest.Mocked<Router>;

    playerServiceMock = {
      setPlayers: jest.fn()
    } as unknown as jest.Mocked<PlayerService>;

    await TestBed.configureTestingModule({
      imports: [RegisterPlayersComponent, HttpClientTestingModule, FormsModule],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: PlayerService, useValue: playerServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterPlayersComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with 4 players', () => {
      expect(component.players.length).toBe(4);
    });

    it('should have 1 human player and 3 bots', () => {
      const humanPlayers = component.players.filter(p => !p.isBot);
      const botPlayers = component.players.filter(p => p.isBot);
      
      expect(humanPlayers.length).toBe(1);
      expect(botPlayers.length).toBe(3);
    });

    it('should set default starting chips to 1000', () => {
      component.players.forEach(player => {
        expect(player.startingChips).toBe(1000);
      });
    });

    it('should set max bot players to 3', () => {
      expect(component.maxBotPlayers).toBe(3);
    });

    it('should set max human players to 1', () => {
      expect(component.maxHumanPlayers).toBe(1);
    });
  });

  describe('Add Player', () => {
    beforeEach(() => {
      component.players = [
        { name: 'Player1', startingChips: 1000, isBot: false },
        { name: 'Bot1', startingChips: 1000, isBot: true },
        { name: 'Bot2', startingChips: 1000, isBot: true }
      ];
    });

    it('should add a bot player', () => {
      component.addPlayer();
      
      expect(component.players.length).toBe(4);
      expect(component.players[3].isBot).toBe(true);
    });

    it('should generate random name for new player', () => {
      component.addPlayer();
      
      expect(component.players[3].name).toBeTruthy();
      expect(typeof component.players[3].name).toBe('string');
    });

    it('should set default chips for new player', () => {
      component.addPlayer();
      
      expect(component.players[3].startingChips).toBe(1000);
    });

    it('should not add more than 4 players', () => {
      component.players = [
        { name: 'P1', startingChips: 1000, isBot: false },
        { name: 'P2', startingChips: 1000, isBot: true },
        { name: 'P3', startingChips: 1000, isBot: true },
        { name: 'P4', startingChips: 1000, isBot: true }
      ];
      
      const alertSpy = jest.spyOn(window, 'alert').mockImplementation();
      
      component.addPlayer();
      
      expect(component.players.length).toBe(4);
      expect(alertSpy).toHaveBeenCalledWith("You can't have more than 4 players.");
      
      alertSpy.mockRestore();
    });
  });

  describe('Remove Player', () => {
    it('should remove player at index', () => {
      const initialCount = component.players.length;
      const removedName = component.players[1].name;
      
      component.removePlayer(1);
      
      expect(component.players.length).toBe(initialCount - 1);
      expect(component.players.find(p => p.name === removedName)).toBeUndefined();
    });

    it('should remove first player', () => {
      const secondPlayerName = component.players[1].name;
      
      component.removePlayer(0);
      
      expect(component.players[0].name).toBe(secondPlayerName);
    });

    it('should remove last player', () => {
      const initialLength = component.players.length;
      
      component.removePlayer(initialLength - 1);
      
      expect(component.players.length).toBe(initialLength - 1);
    });
  });

  describe('Form Submission', () => {
    it('should reset game before registering', fakeAsync(() => {
      component.onSubmit();
      
      const resetReq = httpMock.expectOne('/api/poker/reset');
      expect(resetReq.request.method).toBe('POST');
      
      resetReq.flush({});
      tick();
      
      const startReq = httpMock.expectOne('/api/poker/start');
      startReq.flush({ players: [] });
    }));

    it('should handle reset error', fakeAsync(() => {
      const alertSpy = jest.spyOn(window, 'alert').mockImplementation();
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      
      component.onSubmit();
      
      const resetReq = httpMock.expectOne('/api/poker/reset');
      resetReq.flush('Error', { status: 500, statusText: 'Server Error' });
      tick();
      
      expect(alertSpy).toHaveBeenCalled();
      
      alertSpy.mockRestore();
      consoleSpy.mockRestore();
    }));
  });

  describe('Register Players', () => {
    it('should send players to server', fakeAsync(() => {
      component.registerPlayers();
      
      const req = httpMock.expectOne('/api/poker/start');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(component.players);
      
      req.flush({ players: [] });
    }));

    it('should handle registration error', fakeAsync(() => {
      const alertSpy = jest.spyOn(window, 'alert').mockImplementation();
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      
      component.registerPlayers();
      
      const req = httpMock.expectOne('/api/poker/start');
      req.flush('Error', { status: 500, statusText: 'Server Error' });
      tick();
      
      expect(alertSpy).toHaveBeenCalled();
      
      alertSpy.mockRestore();
      consoleSpy.mockRestore();
    }));

    it('should handle unexpected response format', fakeAsync(() => {
      const alertSpy = jest.spyOn(window, 'alert').mockImplementation();
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      
      component.registerPlayers();
      
      const req = httpMock.expectOne('/api/poker/start');
      req.flush({ unexpected: 'format' });
      tick();
      
      expect(alertSpy).toHaveBeenCalledWith('Unexpected response from server.');
      
      alertSpy.mockRestore();
      consoleSpy.mockRestore();
    }));
  });

  describe('Change Player Names', () => {
    const serverPlayers = [
      { id: 'id-1', name: 'Server1' },
      { id: 'id-2', name: 'Server2' }
    ];

    beforeEach(() => {
      component.players = [
        { name: 'Client1', startingChips: 1000, isBot: false },
        { name: 'Client2', startingChips: 1000, isBot: true }
      ];
    });

    it('should change names when different', fakeAsync(() => {
      component.changePlayerNames(serverPlayers);
      
      const req1 = httpMock.expectOne('/api/poker/change-name');
      expect(req1.request.body).toEqual({ playerId: 'id-1', newName: 'Client1' });
      req1.flush({});
      tick();
      
      const req2 = httpMock.expectOne('/api/poker/change-name');
      expect(req2.request.body).toEqual({ playerId: 'id-2', newName: 'Client2' });
      req2.flush({});
      tick();
      
      expect(playerServiceMock.setPlayers).toHaveBeenCalled();
      expect(routerMock.navigate).toHaveBeenCalledWith(['/start']);
    }));

    it('should skip name change when names match', fakeAsync(() => {
      component.players[0].name = 'Server1';
      
      component.changePlayerNames(serverPlayers);
      
      const req = httpMock.expectOne('/api/poker/change-name');
      expect(req.request.body.playerId).toBe('id-2');
      req.flush({});
      tick();
      
      expect(playerServiceMock.setPlayers).toHaveBeenCalled();
    }));

    it('should handle name change error', fakeAsync(() => {
      const alertSpy = jest.spyOn(window, 'alert').mockImplementation();
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      
      component.changePlayerNames(serverPlayers);
      
      const req = httpMock.expectOne('/api/poker/change-name');
      req.flush('Error', { status: 500, statusText: 'Server Error' });
      tick();
      
      expect(alertSpy).toHaveBeenCalled();
      
      alertSpy.mockRestore();
      consoleSpy.mockRestore();
    }));

    it('should navigate to start after all names changed', fakeAsync(() => {
      component.players[0].name = 'Server1';
      component.players[1].name = 'Server2';
      
      component.changePlayerNames(serverPlayers);
      tick();
      
      expect(routerMock.navigate).toHaveBeenCalledWith(['/start']);
    }));
  });

  describe('Finalize Names', () => {
    it('should add Bot prefix to bot names', fakeAsync(() => {
      component.players = [
        { name: 'John', startingChips: 1000, isBot: true }
      ];
      
      component.onSubmit();
      
      const resetReq = httpMock.expectOne('/api/poker/reset');
      resetReq.flush({});
      tick();
      
      expect(component.players[0].name).toBe('Bot John');
      
      const startReq = httpMock.expectOne('/api/poker/start');
      startReq.flush({ players: [] });
    }));

    it('should not double prefix Bot names', fakeAsync(() => {
      component.players = [
        { name: 'Bot John', startingChips: 1000, isBot: true }
      ];
      
      component.onSubmit();
      
      const resetReq = httpMock.expectOne('/api/poker/reset');
      resetReq.flush({});
      tick();
      
      expect(component.players[0].name).toBe('Bot John');
      
      const startReq = httpMock.expectOne('/api/poker/start');
      startReq.flush({ players: [] });
    }));

    it('should generate name for empty name', fakeAsync(() => {
      component.players = [
        { name: '   ', startingChips: 1000, isBot: false }
      ];
      
      component.onSubmit();
      
      const resetReq = httpMock.expectOne('/api/poker/reset');
      resetReq.flush({});
      tick();
      
      expect(component.players[0].name.trim().length).toBeGreaterThan(0);
      
      const startReq = httpMock.expectOne('/api/poker/start');
      startReq.flush({ players: [] });
    }));
  });
});
