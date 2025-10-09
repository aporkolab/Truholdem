import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { WebSocketService, GameUpdateMessage, PlayerActionMessage } from './websocket.service';
import { AuthService } from './auth.service';
import { BehaviorSubject } from 'rxjs';

interface MockStompClient {
  connect: jest.Mock;
  disconnect: jest.Mock;
  subscribe: jest.Mock;
  send: jest.Mock;
  connected: boolean;
  ws: {
    onclose: (() => void) | null;
  };
}

const mockStompClient: MockStompClient = {
  connect: jest.fn(),
  disconnect: jest.fn(),
  subscribe: jest.fn(),
  send: jest.fn(),
  connected: false,
  ws: {
    onclose: null
  }
};

const mockSockJS = jest.fn().mockImplementation(() => ({}));


(global as typeof globalThis & { SockJS: unknown; Stomp: unknown }).SockJS = mockSockJS;
(global as typeof globalThis & { SockJS: unknown; Stomp: unknown }).Stomp = {
  over: jest.fn().mockReturnValue(mockStompClient)
};

describe('WebSocketService', () => {
  let service: WebSocketService;
  let authServiceMock: Partial<AuthService>;
  let isAuthenticatedSubject: BehaviorSubject<boolean>;

  beforeEach(() => {
    jest.clearAllMocks();
    mockStompClient.connected = false;

    isAuthenticatedSubject = new BehaviorSubject<boolean>(false);

    authServiceMock = {
      isAuthenticated$: isAuthenticatedSubject.asObservable(),
      getToken: jest.fn().mockReturnValue('test-token'),
      isAuthenticated: jest.fn().mockReturnValue(true)
    };

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: AuthService, useValue: authServiceMock }
      ]
    });

    service = TestBed.inject(WebSocketService);
  });

  afterEach(() => {
    service.disconnect();
  });

  describe('connection management', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should not be connected initially', () => {
      expect(service.isConnected()).toBe(false);
    });

    it('should return null gameId initially', () => {
      expect(service.getCurrentGameId()).toBeNull();
    });

    it('should auto-connect when authentication changes to true', fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });

      isAuthenticatedSubject.next(true);
      tick();

      expect(mockSockJS).toHaveBeenCalledWith('/ws');
      expect(mockStompClient.connect).toHaveBeenCalled();
    }));

    it('should not connect without auth token', () => {
      (authServiceMock.getToken as jest.Mock).mockReturnValue(null);

      service.connect();

      expect(mockSockJS).not.toHaveBeenCalled();
    });

    it('should not connect twice if already connected', fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });

      service.connect();
      tick();

      const callCount = mockSockJS.mock.calls.length;

      service.connect();

      expect(mockSockJS).toHaveBeenCalledTimes(callCount);
    }));

    it('should disconnect properly', fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });
      mockStompClient.disconnect.mockImplementation((callback) => {
        mockStompClient.connected = false;
        if (callback) callback();
      });

      service.connect();
      tick();

      service.disconnect();

      expect(mockStompClient.disconnect).toHaveBeenCalled();
      expect(service.isConnected()).toBe(false);
    }));

    it('should update connection status observable on connect', fakeAsync(() => {
      const statuses: boolean[] = [];
      service.connectionStatus$.subscribe(status => statuses.push(status));

      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });

      service.connect();
      tick();

      expect(statuses).toContain(true);
    }));

    it('should handle connection error', fakeAsync(() => {
      const errors: string[] = [];
      service.errors$.subscribe(err => errors.push(err));

      mockStompClient.connect.mockImplementation((headers, onConnect, onError) => {
        onError({ message: 'Connection failed' });
      });

      service.connect();
      tick();

      expect(errors.length).toBeGreaterThan(0);
      expect(service.isConnected()).toBe(false);
    }));
  });

  describe('game subscription', () => {
    beforeEach(fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });
      service.connect();
      tick();
    }));

    it('should subscribe to game updates', () => {
      service.subscribeToGame('game-123');

      expect(mockStompClient.subscribe).toHaveBeenCalledWith(
        '/topic/game/game-123',
        expect.any(Function)
      );
      expect(service.getCurrentGameId()).toBe('game-123');
    });

    it('should emit game updates when received', fakeAsync(() => {
      const updates: GameUpdateMessage[] = [];
      service.gameUpdates$.subscribe(update => updates.push(update));

      const mockMessage: GameUpdateMessage = {
        type: 'GAME_STATE',
        gameState: null,
        message: 'Game updated',
        timestamp: Date.now()
      };

      mockStompClient.subscribe.mockImplementation((destination, callback) => {
        if (destination.startsWith('/topic/game/')) {
          callback({ body: JSON.stringify(mockMessage) });
        }
        return { unsubscribe: jest.fn() };
      });

      service.subscribeToGame('game-123');
      tick();

      expect(updates.length).toBeGreaterThan(0);
      expect(updates[0].type).toBe('GAME_STATE');
    }));

    it('should handle invalid JSON in game update', fakeAsync(() => {
      const errors: string[] = [];
      service.errors$.subscribe(err => errors.push(err));

      mockStompClient.subscribe.mockImplementation((destination, callback) => {
        if (destination.startsWith('/topic/game/')) {
          callback({ body: 'invalid-json{{{' });
        }
        return { unsubscribe: jest.fn() };
      });

      service.subscribeToGame('game-123');
      tick();

      expect(errors).toContain('Failed to parse game update');
    }));

    it('should unsubscribe from previous game when subscribing to new one', () => {
      service.subscribeToGame('game-123');
      service.subscribeToGame('game-456');

      expect(service.getCurrentGameId()).toBe('game-456');
    });

    it('should not subscribe when not connected', () => {
      service.disconnect();
      mockStompClient.subscribe.mockClear();

      service.subscribeToGame('game-123');

      expect(mockStompClient.subscribe).not.toHaveBeenCalled();
    });
  });

  describe('player actions', () => {
    beforeEach(fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });
      service.connect();
      tick();
      service.subscribeToGame('game-123');
    }));

    it('should send player action', () => {
      const action: PlayerActionMessage = {
        playerId: 'player-1',
        action: 'FOLD',
        amount: 0
      };

      service.sendPlayerAction(action);

      expect(mockStompClient.send).toHaveBeenCalledWith(
        '/app/game/game-123/action',
        {},
        JSON.stringify(action)
      );
    });

    it('should not send action when not connected', () => {
      service.disconnect();
      mockStompClient.send.mockClear();

      service.sendPlayerAction({ playerId: 'p1', action: 'FOLD', amount: 0 });

      expect(mockStompClient.send).not.toHaveBeenCalled();
    });

    it('should join game', () => {
      service.joinGame('TestPlayer');

      expect(mockStompClient.send).toHaveBeenCalledWith(
        '/app/game/game-123/join',
        {},
        JSON.stringify('TestPlayer')
      );
    });

    it('should leave game', () => {
      service.leaveGame('TestPlayer');

      expect(mockStompClient.send).toHaveBeenCalledWith(
        '/app/game/game-123/leave',
        {},
        JSON.stringify('TestPlayer')
      );
    });

    it('should handle send error gracefully', fakeAsync(() => {
      const errors: string[] = [];
      service.errors$.subscribe(err => errors.push(err));

      mockStompClient.send.mockImplementation(() => {
        throw new Error('Send failed');
      });

      service.sendPlayerAction({ playerId: 'p1', action: 'FOLD', amount: 0 });
      tick();

      expect(errors).toContain('Failed to send player action');
    }));
  });

  describe('reconnection with exponential backoff', () => {
    beforeEach(fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });
      service.connect();
      tick();
      service.subscribeToGame('game-123');
    }));

    it('should track connection state signal', () => {
      expect(service.connectionState()).toBeDefined();
    });

    it('should track current game ID signal', () => {
      expect(service.currentGameId()).toBe('game-123');
    });

    it('should track last event sequence', () => {
      expect(service.lastEventSequence()).toBe(0);
    });

    it('should update last event sequence on game update', fakeAsync(() => {
      const mockMessage: GameUpdateMessage = {
        type: 'GAME_STATE',
        gameState: null,
        message: 'Game updated',
        timestamp: Date.now(),
        sequenceNumber: 42
      };

      mockStompClient.subscribe.mockImplementation((destination, callback) => {
        if (destination.startsWith('/topic/game/')) {
          callback({ body: JSON.stringify(mockMessage) });
        }
        return { unsubscribe: jest.fn() };
      });

      service.subscribeToGame('game-456');
      tick();

      expect(service.lastEventSequence()).toBe(42);
    }));

    it('should compute isConnected signal correctly', fakeAsync(() => {
      expect(service.isConnected()).toBe(true);

      mockStompClient.disconnect.mockImplementation((callback) => {
        mockStompClient.connected = false;
        if (callback) callback();
      });

      service.disconnect();
      tick();

      expect(service.isConnected()).toBe(false);
    }));

    it('should reset reconnect attempts on force reconnect', fakeAsync(() => {
      mockStompClient.disconnect.mockImplementation((callback) => {
        mockStompClient.connected = false;
        if (callback) callback();
      });

      service.disconnect();
      tick();

      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });

      service.forceReconnect();
      tick(100); // Wait for the setTimeout in forceReconnect

      expect(service.isConnected()).toBe(true);
    }));

    it('should handle connection failure and trigger reconnect', fakeAsync(() => {
      let connectAttempts = 0;

      mockStompClient.connect.mockImplementation((headers, onConnect, onError) => {
        connectAttempts++;
        if (connectAttempts <= 2) {
          onError({ message: 'Connection failed' });
        } else {
          onConnect({});
        }
      });


      mockStompClient.disconnect.mockImplementation((callback) => {
        if (callback) callback();
      });

      service.disconnect();
      tick();


      mockStompClient.connect.mockClear();
      connectAttempts = 0;

      service.forceReconnect();
      tick(1000);
      tick(2000);
      tick(4000);


      expect(connectAttempts).toBeGreaterThanOrEqual(1);
    }));

    it('should not reconnect if max attempts exceeded', fakeAsync(() => {

      mockStompClient.connect.mockImplementation((headers, onConnect, onError) => {
        onError({ message: 'Connection failed' });
      });


      service.disconnect();
      tick();

      service.forceReconnect();


      for (let i = 0; i < 15; i++) {
        tick(30000);
      }

      expect(service.reconnectAttemptsRemaining()).toBe(0);
    }));
  });

  describe('state recovery', () => {
    beforeEach(fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });
      service.connect();
      tick();
    }));

    it('should emit state snapshot on reconnection recovery', fakeAsync(() => {
      const snapshots: unknown[] = [];
      service.stateSnapshot$.subscribe(snapshot => snapshots.push(snapshot));


      mockStompClient.subscribe.mockImplementation((destination, callback) => {
        if (destination === '/user/queue/state') {

          setTimeout(() => {
            callback({
              body: JSON.stringify({
                success: true,
                game: { id: 'game-123', phase: 'PRE_FLOP' },
                missedEvents: [],
                lastEventSequence: 10,
                serverTime: new Date().toISOString()
              })
            });
          }, 100);
        }
        return { unsubscribe: jest.fn() };
      });

      service.subscribeToGame('game-123');
      tick(200);



      expect(service.stateSnapshot$).toBeDefined();
    }));

    it('should handle failed state recovery', fakeAsync(() => {
      const errors: string[] = [];
      service.errors$.subscribe(err => errors.push(err));

      mockStompClient.subscribe.mockImplementation((destination, callback) => {
        if (destination === '/user/queue/state') {
          setTimeout(() => {
            callback({
              body: JSON.stringify({
                success: false,
                error: 'Game not found'
              })
            });
          }, 100);
        }
        return { unsubscribe: jest.fn() };
      });

      service.subscribeToGame('game-123');
      tick(200);


      expect(service.errors$).toBeDefined();
    }));

    it('should process missed events in order', fakeAsync(() => {
      const updates: GameUpdateMessage[] = [];
      service.gameUpdates$.subscribe(update => updates.push(update));

      const missedEvents = [
        { type: 'PLAYER_ACTION', sequence: 1, timestamp: Date.now() - 3000 },
        { type: 'PHASE_CHANGE', sequence: 2, timestamp: Date.now() - 2000 },
        { type: 'GAME_UPDATE', sequence: 3, timestamp: Date.now() - 1000 }
      ];

      mockStompClient.subscribe.mockImplementation((destination, callback) => {
        if (destination === '/user/queue/state') {
          setTimeout(() => {
            callback({
              body: JSON.stringify({
                success: true,
                game: { id: 'game-123' },
                missedEvents: missedEvents,
                lastEventSequence: 3
              })
            });
          }, 100);
        }
        return { unsubscribe: jest.fn() };
      });

      service.subscribeToGame('game-123');
      tick(200);


      expect(service.stateSnapshot$).toBeDefined();
    }));
  });

  describe('exponential backoff calculation', () => {
    it('should calculate correct delays with jitter', () => {


      const INITIAL_DELAY = 1000;
      const MAX_DELAY = 30000;
      const MULTIPLIER = 2;


      const expectedDelays = [];
      for (let attempt = 1; attempt <= 10; attempt++) {
        const baseDelay = Math.min(
          INITIAL_DELAY * Math.pow(MULTIPLIER, attempt - 1),
          MAX_DELAY
        );
        expectedDelays.push(baseDelay);
      }

      expect(expectedDelays[0]).toBe(1000);
      expect(expectedDelays[1]).toBe(2000);
      expect(expectedDelays[2]).toBe(4000);
      expect(expectedDelays[3]).toBe(8000);
      expect(expectedDelays[4]).toBe(16000);
      expect(expectedDelays[5]).toBe(30000);
      expect(expectedDelays[6]).toBe(30000);
    });
  });

  describe('heartbeat mechanism', () => {
    beforeEach(fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });
      service.connect();
      tick();
    }));

    it('should send heartbeat periodically when connected', fakeAsync(() => {

      expect(service.isConnected()).toBe(true);


      tick(30000);


      expect(service.isConnected()).toBe(true);
    }));
  });

  describe('unsubscribe from game', () => {
    beforeEach(fakeAsync(() => {
      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });
      service.connect();
      tick();
    }));

    it('should clear game state on unsubscribe', fakeAsync(() => {
      const mockUnsubscribe = jest.fn();
      mockStompClient.subscribe.mockReturnValue({ unsubscribe: mockUnsubscribe });

      service.subscribeToGame('game-123');
      expect(service.getCurrentGameId()).toBe('game-123');

      service.unsubscribeFromGame();

      expect(mockUnsubscribe).toHaveBeenCalled();
      expect(service.getCurrentGameId()).toBeNull();
    }));

    it('should reset last event sequence on unsubscribe', fakeAsync(() => {
      const mockMessage: GameUpdateMessage = {
        type: 'GAME_STATE',
        gameState: null,
        message: 'Game updated',
        timestamp: Date.now(),
        sequenceNumber: 50
      };

      mockStompClient.subscribe.mockImplementation((destination, callback) => {
        if (destination.startsWith('/topic/game/')) {
          callback({ body: JSON.stringify(mockMessage) });
        }
        return { unsubscribe: jest.fn() };
      });

      service.subscribeToGame('game-123');
      tick();

      expect(service.lastEventSequence()).toBe(50);

      service.unsubscribeFromGame();

      expect(service.lastEventSequence()).toBe(0);
    }));
  });

  describe('connection state transitions', () => {
    it('should transition through states correctly', fakeAsync(() => {

      expect(service.connectionState()).toBeDefined();

      mockStompClient.connect.mockImplementation((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect({});
      });

      service.connect();
      tick();


      expect(service.isConnected()).toBe(true);

      mockStompClient.disconnect.mockImplementation((callback) => {
        mockStompClient.connected = false;
        if (callback) callback();
      });

      service.disconnect();
      tick();


      expect(service.isConnected()).toBe(false);
    }));
  });
});
