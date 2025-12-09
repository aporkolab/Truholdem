import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { Game } from '../model/game';


declare const SockJS: any;

declare const Stomp: any;

export interface GameUpdateMessage {
  type: string;
  gameState: Game | null;
  message: string;
  timestamp: number;
}

export interface PlayerActionMessage {
  playerId: string;
  action: string;
  amount: number;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private readonly authService = inject(AuthService);
  
  private readonly WEBSOCKET_URL = 'http://localhost:8080/ws';
  
  
  private stompClient: any = null;
  private connected = false;
  private gameId: string | null = null;

  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  private gameUpdatesSubject = new Subject<GameUpdateMessage>();
  public gameUpdates$ = this.gameUpdatesSubject.asObservable();

  private errorSubject = new Subject<string>();
  public errors$ = this.errorSubject.asObservable();

  constructor() {
    
    this.authService.isAuthenticated$.subscribe(isAuth => {
      if (isAuth && !this.connected) {
        this.connect();
      } else if (!isAuth && this.connected) {
        this.disconnect();
      }
    });
  }

  connect(): void {
    if (this.connected) {
      return;
    }

    const token = this.authService.getToken();
    if (!token) {
      console.warn('Cannot connect WebSocket: No authentication token');
      return;
    }

    try {
      const socket = new SockJS(this.WEBSOCKET_URL);
      this.stompClient = Stomp.over(socket);

      
      const headers = {
        'Authorization': `Bearer ${token}`
      };

      this.stompClient.connect(headers, 
        (frame: unknown) => {
          console.log('WebSocket connected:', frame);
          this.connected = true;
          this.connectionStatusSubject.next(true);
        },
        (error: unknown) => {
          console.error('WebSocket connection error:', error);
          this.handleConnectionError(error);
        }
      );

      
      if (this.stompClient.ws) {
        this.stompClient.ws.onclose = () => {
          console.log('WebSocket disconnected');
          this.connected = false;
          this.connectionStatusSubject.next(false);
          
          
          if (this.authService.isAuthenticated()) {
            setTimeout(() => {
              if (!this.connected) {
                this.connect();
              }
            }, 5000);
          }
        };
      }

    } catch (error: unknown) {
      console.error('Failed to create WebSocket connection:', error);
      this.handleConnectionError(error);
    }
  }

  disconnect(): void {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect(() => {
        console.log('WebSocket disconnected');
      });
      
      this.connected = false;
      this.gameId = null;
      this.connectionStatusSubject.next(false);
    }
  }

  subscribeToGame(gameId: string): void {
    if (!this.connected || !this.stompClient) {
      console.warn('Cannot subscribe to game: WebSocket not connected');
      return;
    }

    
    if (this.gameId && this.gameId !== gameId) {
      this.unsubscribeFromGame();
    }

    this.gameId = gameId;

    
    this.stompClient.subscribe(`/topic/game/${gameId}`, (message: { body: string }) => {
      try {
        const gameUpdate: GameUpdateMessage = JSON.parse(message.body);
        this.gameUpdatesSubject.next(gameUpdate);
      } catch (error: unknown) {
        console.error('Error parsing game update message:', error);
        this.errorSubject.next('Failed to parse game update');
      }
    });

    
    this.stompClient.subscribe('/user/queue/messages', (message: { body: string }) => {
      try {
        const userMessage = JSON.parse(message.body);
        console.log('Received personal message:', userMessage);
        
      } catch (error: unknown) {
        console.error('Error parsing personal message:', error);
      }
    });

    console.log(`Subscribed to game: ${gameId}`);
  }

  unsubscribeFromGame(): void {
    if (this.gameId && this.stompClient && this.connected) {
      
      this.gameId = null;
      console.log('Unsubscribed from game');
    }
  }

  sendPlayerAction(action: PlayerActionMessage): void {
    if (!this.connected || !this.stompClient || !this.gameId) {
      console.warn('Cannot send player action: WebSocket not connected or no game selected');
      return;
    }

    try {
      this.stompClient.send(
        `/app/game/${this.gameId}/action`,
        {},
        JSON.stringify(action)
      );
      console.log('Player action sent:', action);
    } catch (error: unknown) {
      console.error('Failed to send player action:', error);
      this.errorSubject.next('Failed to send player action');
    }
  }

  joinGame(playerName: string): void {
    if (!this.connected || !this.stompClient || !this.gameId) {
      console.warn('Cannot join game: WebSocket not connected or no game selected');
      return;
    }

    try {
      this.stompClient.send(
        `/app/game/${this.gameId}/join`,
        {},
        JSON.stringify(playerName)
      );
      console.log('Joined game:', this.gameId);
    } catch (error: unknown) {
      console.error('Failed to join game:', error);
      this.errorSubject.next('Failed to join game');
    }
  }

  leaveGame(playerName: string): void {
    if (!this.connected || !this.stompClient || !this.gameId) {
      return;
    }

    try {
      this.stompClient.send(
        `/app/game/${this.gameId}/leave`,
        {},
        JSON.stringify(playerName)
      );
      console.log('Left game:', this.gameId);
    } catch (error: unknown) {
      console.error('Failed to leave game:', error);
    } finally {
      this.unsubscribeFromGame();
    }
  }

  private handleConnectionError(error: unknown): void {
    this.connected = false;
    this.connectionStatusSubject.next(false);
    
    let errorMessage = 'WebSocket connection failed';
    if (error && typeof error === 'object' && error !== null && 'message' in error) {
      errorMessage += ': ' + (error as { message: string }).message;
    }
    
    this.errorSubject.next(errorMessage);
  }

  
  isConnected(): boolean {
    return this.connected;
  }

  getCurrentGameId(): string | null {
    return this.gameId;
  }
}
