import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry, tap, finalize } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { GameStateService } from './game-state.service';


export interface PlayerInfo {
  name: string;
  startingChips: number;
  isBot: boolean;
}


export interface BetRequest {
  playerId: string;
  amount: number;
}


export interface GameResult {
  message: string;
  winnerName?: string;
  winningHand?: string;
}


@Injectable({
  providedIn: 'root'
})
export class PokerService {
  private readonly http = inject(HttpClient);
  private readonly gameState = inject(GameStateService);
  private readonly apiUrl = `${environment.apiUrl}/poker`;

  
  
  

  
  startGame(players?: PlayerInfo[]): Observable<Game> {
    this.gameState.setLoading(true);
    this.gameState.clearError();

    const playersToSend = players || this.getDefaultPlayers();

    return this.http.post<Game>(`${this.apiUrl}/start`, playersToSend).pipe(
      tap(game => {
        this.gameState.setGame(game);
        this.logDebug('Game started', game);
      }),
      catchError(error => this.handleError(error)),
      finalize(() => this.gameState.setLoading(false))
    );
  }

  
  getGameStatus(): Observable<Game> {
    return this.http.get<Game>(`${this.apiUrl}/status`).pipe(
      tap(game => {
        this.gameState.setGame(game);
        this.logDebug('Game status updated', game);
      }),
      catchError(error => this.handleError(error))
    );
  }

  
  refreshGame(): Observable<Game> {
    return this.getGameStatus().pipe(
      retry({ count: 3, delay: 1000 })
    );
  }

  
  startNewHand(): Observable<Game> {
    this.gameState.setLoading(true);

    return this.http.post<Game>(`${this.apiUrl}/new-match`, {}).pipe(
      tap(game => {
        this.gameState.setGame(game);
        this.gameState.setLastAction(null);
        this.logDebug('New hand started', game);
      }),
      catchError(error => this.handleError(error)),
      finalize(() => this.gameState.setLoading(false))
    );
  }

  
  endGame(): Observable<GameResult> {
    return this.http.get<GameResult>(`${this.apiUrl}/end`).pipe(
      tap(result => {
        this.logDebug('Game ended', result);
      }),
      catchError(error => this.handleError(error))
    );
  }

  
  resetGame(): Observable<string> {
    return this.http.post(`${this.apiUrl}/reset`, {}, { responseType: 'text' }).pipe(
      tap(() => {
        this.gameState.reset();
        this.logDebug('Game reset');
      }),
      catchError(error => this.handleError(error))
    );
  }

  
  
  

  
  fold(playerId: string): Observable<string> {
    const player = this.gameState.getPlayerById(playerId);
    
    return this.performAction('fold', { playerId }).pipe(
      tap(() => {
        if (player) {
          this.gameState.recordAction('FOLD', playerId, player.name);
        }
        this.refreshGameAfterAction();
      })
    );
  }

  
  check(playerId: string): Observable<string> {
    const player = this.gameState.getPlayerById(playerId);
    
    return this.performAction('check', { playerId }).pipe(
      tap(() => {
        if (player) {
          this.gameState.recordAction('CHECK', playerId, player.name);
        }
        this.refreshGameAfterAction();
      })
    );
  }

  
  call(playerId: string): Observable<string> {
    const player = this.gameState.getPlayerById(playerId);
    const callAmount = this.gameState.callAmount();
    
    return this.performAction('call', { playerId }).pipe(
      tap(() => {
        if (player) {
          this.gameState.recordAction('CALL', playerId, player.name, callAmount);
        }
        this.refreshGameAfterAction();
      })
    );
  }

  
  bet(playerId: string, amount: number): Observable<string> {
    const player = this.gameState.getPlayerById(playerId);
    
    return this.http.post(
      `${this.apiUrl}/bet`, 
      { playerId, amount }, 
      { responseType: 'text' }
    ).pipe(
      tap(() => {
        if (player) {
          this.gameState.recordAction('BET', playerId, player.name, amount);
        }
        this.refreshGameAfterAction();
      }),
      catchError(error => this.handleError(error))
    );
  }

  
  raise(playerId: string, amount: number): Observable<string> {
    const player = this.gameState.getPlayerById(playerId);
    
    return this.http.post(
      `${this.apiUrl}/raise`, 
      { playerId, amount }, 
      { responseType: 'text' }
    ).pipe(
      tap(() => {
        if (player) {
          const isAllIn = amount >= ((player.chips ?? 0) + (player.betAmount ?? 0));
          this.gameState.recordAction(
            isAllIn ? 'ALL_IN' : 'RAISE',
            playerId,
            player.name,
            amount
          );
        }
        this.refreshGameAfterAction();
      }),
      catchError(error => this.handleError(error))
    );
  }

  
  executeBotAction(botId: string): Observable<GameResult> {
    return this.http.post<GameResult>(`${this.apiUrl}/bot-action/${botId}`, {}).pipe(
      tap(result => {
        this.logDebug('Bot action executed', { botId, result });
        
        this.refreshGameAfterAction();
      }),
      catchError(error => this.handleError(error))
    );
  }

  
  
  

  
  getMinRaiseAmount(game: Game): number {
    if (!game) return environment.defaultBigBlind;
    return game.currentBet + (game.minRaiseAmount || environment.defaultBigBlind);
  }

  
  getCallAmount(game: Game, player: Player): number {
    if (!game || !player) return 0;
    return Math.max(0, game.currentBet - (player.betAmount || 0));
  }

  
  canCheck(game: Game, player: Player): boolean {
    if (!game || !player) return false;
    return (player.betAmount || 0) >= game.currentBet;
  }

  
  getBotsToAct(game: Game): Player[] {
    if (!game || !game.players) return [];
    
    return game.players.filter(p =>
      p.name?.startsWith('Bot') &&
      !p.folded &&
      p.chips > 0
    );
  }

  
  getPhaseDisplayName(phase: string): string {
    const phases: Record<string, string> = {
      'PRE_FLOP': 'Pre-Flop',
      'FLOP': 'Flop',
      'TURN': 'Turn',
      'RIVER': 'River',
      'SHOWDOWN': 'Showdown'
    };
    return phases[phase] || phase;
  }

  
  
  

  
  private performAction(action: string, params: Record<string, string>): Observable<string> {
    const queryParams = new URLSearchParams(params).toString();
    return this.http.post(
      `${this.apiUrl}/${action}?${queryParams}`, 
      null, 
      { responseType: 'text' }
    ).pipe(
      catchError(error => this.handleError(error))
    );
  }

  
  private refreshGameAfterAction(): void {
    this.refreshGame().subscribe({
      error: err => this.logDebug('Failed to refresh after action', err)
    });
  }

  
  private getDefaultPlayers(): PlayerInfo[] {
    return [
      { name: 'Player', startingChips: environment.defaultStartingChips, isBot: false },
      { name: 'Bot1', startingChips: environment.defaultStartingChips, isBot: true },
      { name: 'Bot2', startingChips: environment.defaultStartingChips, isBot: true }
    ];
  }

  
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unknown error occurred';

    if (error.error instanceof ErrorEvent) {
      
      errorMessage = `Error: ${error.error.message}`;
    } else {
      
      errorMessage = error.error?.message || 
                    `Error Code: ${error.status}, Message: ${error.message}`;
    }

    this.gameState.setError(errorMessage);
    this.logDebug('API Error', { error, errorMessage });

    return throwError(() => new Error(errorMessage));
  }

  
  private logDebug(message: string, data?: unknown): void {
    if (environment.logApiCalls) {
      console.log(`[PokerService] ${message}`, data);
    }
  }
}
