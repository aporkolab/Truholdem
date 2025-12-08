import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, timer } from 'rxjs';
import { catchError, map, retry, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Game } from '../model/game';
import { Player } from '../model/player';

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
  private http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl + '/poker';

  
  private gameSubject = new BehaviorSubject<Game | null>(null);
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);

  
  game$ = this.gameSubject.asObservable();
  loading$ = this.loadingSubject.asObservable();
  error$ = this.errorSubject.asObservable();

  
  currentPlayer$: Observable<Player | undefined> = this.game$.pipe(
    map(game => game?.players.find(p => !p.name?.startsWith('Bot')))
  );

  isPlayerTurn$: Observable<boolean> = this.game$.pipe(
    map(game => {
      if (!game) return false;
      const currentPlayer = game.players[game.currentPlayerIndex];
      return currentPlayer && !currentPlayer.name?.startsWith('Bot');
    })
  );

  
  startGame(players?: PlayerInfo[]): Observable<Game> {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);

    const playersToSend = players || this.getDefaultPlayers();

    return this.http.post<Game>(`${this.apiUrl}/start`, playersToSend).pipe(
      tap(game => {
        this.gameSubject.next(game);
        this.logDebug('Game started', game);
      }),
      catchError(this.handleError.bind(this)),
      tap(() => this.loadingSubject.next(false))
    );
  }

  
  getGameStatus(): Observable<Game> {
    return this.http.get<Game>(`${this.apiUrl}/status`).pipe(
      tap(game => {
        this.gameSubject.next(game);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  
  refreshGame(): Observable<Game> {
    return this.getGameStatus().pipe(
      retry({ count: 3, delay: 1000 })
    );
  }

  
  fold(playerId: string): Observable<string> {
    return this.performAction('fold', { playerId }).pipe(
      tap(() => this.refreshGame().subscribe())
    );
  }

  check(playerId: string): Observable<string> {
    return this.performAction('check', { playerId }).pipe(
      tap(() => this.refreshGame().subscribe())
    );
  }

  call(playerId: string): Observable<string> {
    return this.performAction('call', { playerId }).pipe(
      tap(() => this.refreshGame().subscribe())
    );
  }

  bet(playerId: string, amount: number): Observable<string> {
    return this.http.post(`${this.apiUrl}/bet`, { playerId, amount }, { responseType: 'text' }).pipe(
      tap(() => this.refreshGame().subscribe()),
      catchError(this.handleError.bind(this))
    );
  }

  raise(playerId: string, amount: number): Observable<string> {
    return this.http.post(`${this.apiUrl}/raise`, { playerId, amount }, { responseType: 'text' }).pipe(
      tap(() => this.refreshGame().subscribe()),
      catchError(this.handleError.bind(this))
    );
  }

  
  executeBotAction(botId: string): Observable<GameResult> {
    return this.http.post<GameResult>(`${this.apiUrl}/bot-action/${botId}`, {}).pipe(
      tap(() => this.refreshGame().subscribe()),
      catchError(this.handleError.bind(this))
    );
  }

  
  endGame(): Observable<GameResult> {
    return this.http.get<GameResult>(`${this.apiUrl}/end`).pipe(
      tap(result => {
        this.logDebug('Game ended', result);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  
  startNewHand(): Observable<Game> {
    return this.http.post<Game>(`${this.apiUrl}/new-match`, {}).pipe(
      tap(game => {
        this.gameSubject.next(game);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  
  resetGame(): Observable<string> {
    return this.http.post(`${this.apiUrl}/reset`, {}, { responseType: 'text' }).pipe(
      tap(() => {
        this.gameSubject.next(null);
      }),
      catchError(this.handleError.bind(this))
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
    return this.http.post(`${this.apiUrl}/${action}?${queryParams}`, null, { responseType: 'text' }).pipe(
      catchError(this.handleError.bind(this))
    );
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
      
      errorMessage = error.error?.message || `Error Code: ${error.status}, Message: ${error.message}`;
    }

    this.errorSubject.next(errorMessage);
    this.loadingSubject.next(false);
    this.logDebug('API Error', { error, errorMessage });

    return throwError(() => new Error(errorMessage));
  }

  private logDebug(message: string, data?: unknown): void {
    if (environment.logApiCalls) {
      console.log(`[PokerService] ${message}`, data);
    }
  }
}
