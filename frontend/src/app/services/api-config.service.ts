import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';


@Injectable({
  providedIn: 'root'
})
export class ApiConfigService {
  
  
  readonly baseUrl = environment.apiUrl;
  readonly wsUrl = environment.wsUrl;
  
  // Alias for backward compatibility
  get apiUrl(): string {
    return this.baseUrl;
  }
  
  
  readonly poker = {
    base: `${this.baseUrl}/poker`,
    start: `${this.baseUrl}/poker/start`,
    status: `${this.baseUrl}/poker/status`,
    fold: `${this.baseUrl}/poker/fold`,
    check: `${this.baseUrl}/poker/check`,
    call: `${this.baseUrl}/poker/call`,
    bet: `${this.baseUrl}/poker/bet`,
    raise: `${this.baseUrl}/poker/raise`,
    end: `${this.baseUrl}/poker/end`,
    reset: `${this.baseUrl}/poker/reset`,
    newMatch: `${this.baseUrl}/poker/new-match`,
    flop: `${this.baseUrl}/poker/flop`,
    turn: `${this.baseUrl}/poker/turn`,
    river: `${this.baseUrl}/poker/river`,
    botAction: (botId: string) => `${this.baseUrl}/poker/bot-action/${botId}`
  };

  
  readonly auth = {
    base: `${this.baseUrl}/auth`,
    login: `${this.baseUrl}/auth/login`,
    register: `${this.baseUrl}/auth/register`,
    refresh: `${this.baseUrl}/auth/refresh`,
    logout: `${this.baseUrl}/auth/logout`,
    profile: `${this.baseUrl}/auth/profile`
  };

  
  readonly game = {
    create: `${this.baseUrl}/games`,
    getById: (id: string) => `${this.baseUrl}/games/${id}`,
    action: (gameId: string) => `${this.baseUrl}/games/${gameId}/action`,
    players: (gameId: string) => `${this.baseUrl}/games/${gameId}/players`
  };

  
  readonly wsTopic = {
    game: (gameId: string) => `/topic/game/${gameId}`,
    user: '/user/queue/messages',
    errors: (gameId: string) => `/topic/game/${gameId}/errors`
  };

  
  readonly wsDestination = {
    action: (gameId: string) => `/app/game/${gameId}/action`,
    botAction: (gameId: string) => `/app/game/${gameId}/bot-action`,
    join: (gameId: string) => `/app/game/${gameId}/join`,
    leave: (gameId: string) => `/app/game/${gameId}/leave`,
    newHand: (gameId: string) => `/app/game/${gameId}/new-hand`
  };

  
  isDevelopment(): boolean {
    return !environment.production;
  }

  
  isWebSocketEnabled(): boolean {
    return environment.enableWebSocket;
  }

  
  getDefaultGameSettings() {
    return {
      startingChips: environment.defaultStartingChips,
      smallBlind: environment.defaultSmallBlind,
      bigBlind: environment.defaultBigBlind,
      actionTimeout: environment.actionTimeout
    };
  }
}
