import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface HandHistory {
  id: string;
  gameId: string;
  handNumber: number;
  playedAt: string;
  smallBlind: number;
  bigBlind: number;
  dealerPosition: number;
  winnerName: string;
  winningHandDescription: string;
  finalPot: number;
  players: HandHistoryPlayer[];
  actions: ActionRecord[];
  board: CardRecord[];
}

export interface HandHistoryPlayer {
  playerId: string;
  playerName: string;
  startingChips: number;
  seatPosition: number;
  holeCard1Suit: string;
  holeCard1Value: string;
  holeCard2Suit: string;
  holeCard2Value: string;
}

export interface ActionRecord {
  playerId: string;
  playerName: string;
  action: string;
  amount: number;
  phase: string;
  timestamp: string;
}

export interface CardRecord {
  suit: string;
  value: string;
}

export interface ReplayData {
  id: string;
  handNumber: number;
  smallBlind: number;
  bigBlind: number;
  dealerPosition: number;
  players: PlayerSnapshot[];
  actions: ReplayAction[];
  board: string[];
  winnerName: string;
  winningHand: string;
  finalPot: number;
}

export interface PlayerSnapshot {
  id: string;
  name: string;
  startingChips: number;
  seatPosition: number;
  holeCard1: string;
  holeCard2: string;
}

export interface ReplayAction {
  playerName: string;
  action: string;
  amount: number;
  phase: string;
}

export interface PagedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class HandHistoryService {
  private http = inject(HttpClient);
  
  private readonly apiUrl = `${environment.apiUrl}/history`;
  
  
  private currentReplaySubject = new BehaviorSubject<ReplayData | null>(null);
  public currentReplay$ = this.currentReplaySubject.asObservable();
  
  private replayIndexSubject = new BehaviorSubject<number>(0);
  public replayIndex$ = this.replayIndexSubject.asObservable();

  

  getHandHistory(historyId: string): Observable<HandHistory> {
    return this.http.get<HandHistory>(`${this.apiUrl}/${historyId}`).pipe(
      catchError(error => {
        console.error('Error fetching hand history:', error);
        throw error;
      })
    );
  }

  getGameHistory(gameId: string): Observable<HandHistory[]> {
    return this.http.get<HandHistory[]>(`${this.apiUrl}/game/${gameId}`).pipe(
      catchError(error => {
        console.error('Error fetching game history:', error);
        return of([]);
      })
    );
  }

  getGameHistoryPaged(gameId: string, page = 0, size = 20): Observable<PagedResult<HandHistory>> {
    return this.http.get<PagedResult<HandHistory>>(`${this.apiUrl}/game/${gameId}/paged`, {
      params: { page: page.toString(), size: size.toString() }
    }).pipe(
      catchError(error => {
        console.error('Error fetching paged game history:', error);
        return of({ content: [], totalElements: 0, totalPages: 0, number: 0, size });
      })
    );
  }

  getPlayerHistory(playerId: string): Observable<HandHistory[]> {
    return this.http.get<HandHistory[]>(`${this.apiUrl}/player/${playerId}`).pipe(
      catchError(() => of([]))
    );
  }

  getPlayerWins(playerName: string): Observable<HandHistory[]> {
    return this.http.get<HandHistory[]>(`${this.apiUrl}/wins/${encodeURIComponent(playerName)}`).pipe(
      catchError(() => of([]))
    );
  }

  getRecentHands(): Observable<HandHistory[]> {
    return this.http.get<HandHistory[]>(`${this.apiUrl}/recent`).pipe(
      catchError(() => of([]))
    );
  }

  getBiggestPots(): Observable<HandHistory[]> {
    return this.http.get<HandHistory[]>(`${this.apiUrl}/biggest-pots`).pipe(
      catchError(() => of([]))
    );
  }

  getHandCount(gameId: string): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/game/${gameId}/count`).pipe(
      catchError(() => of(0))
    );
  }

  

  getReplayData(historyId: string): Observable<ReplayData> {
    return this.http.get<ReplayData>(`${this.apiUrl}/${historyId}/replay`).pipe(
      tap(data => {
        this.currentReplaySubject.next(data);
        this.replayIndexSubject.next(0);
      }),
      catchError(error => {
        console.error('Error fetching replay data:', error);
        throw error;
      })
    );
  }

  
  startReplay(historyId: string): void {
    this.getReplayData(historyId).subscribe();
  }

  nextAction(): void {
    const replay = this.currentReplaySubject.value;
    const currentIndex = this.replayIndexSubject.value;
    
    if (replay && currentIndex < replay.actions.length) {
      this.replayIndexSubject.next(currentIndex + 1);
    }
  }

  previousAction(): void {
    const currentIndex = this.replayIndexSubject.value;
    if (currentIndex > 0) {
      this.replayIndexSubject.next(currentIndex - 1);
    }
  }

  goToAction(index: number): void {
    const replay = this.currentReplaySubject.value;
    if (replay && index >= 0 && index <= replay.actions.length) {
      this.replayIndexSubject.next(index);
    }
  }

  resetReplay(): void {
    this.replayIndexSubject.next(0);
  }

  clearReplay(): void {
    this.currentReplaySubject.next(null);
    this.replayIndexSubject.next(0);
  }

  
  getCurrentReplayState(): ReplayState | null {
    const replay = this.currentReplaySubject.value;
    const actionIndex = this.replayIndexSubject.value;
    
    if (!replay) return null;

    
    const playerStates = new Map<string, PlayerReplayState>();
    replay.players.forEach(p => {
      playerStates.set(p.name, {
        name: p.name,
        chips: p.startingChips,
        bet: 0,
        folded: false,
        cards: [p.holeCard1, p.holeCard2]
      });
    });

    
    let pot = 0;
    let currentBet = 0;
    let phase = 'PRE_FLOP';
    const board: string[] = [];

    
    for (let i = 0; i < actionIndex && i < replay.actions.length; i++) {
      const action = replay.actions[i];
      const player = playerStates.get(action.playerName);
      
      if (!player) continue;

      
      if (action.phase !== phase) {
        phase = action.phase;
        currentBet = 0;
        playerStates.forEach(p => p.bet = 0);
        
        
        if (phase === 'FLOP' && replay.board.length >= 3) {
          board.push(...replay.board.slice(0, 3));
        } else if (phase === 'TURN' && replay.board.length >= 4) {
          if (board.length < 4) board.push(replay.board[3]);
        } else if (phase === 'RIVER' && replay.board.length >= 5) {
          if (board.length < 5) board.push(replay.board[4]);
        }
      }

      switch (action.action.toUpperCase()) {
        case 'FOLD':
          player.folded = true;
          break;
        case 'CHECK':

          break;
        case 'CALL': {
          const callAmount = currentBet - player.bet;
          player.chips -= callAmount;
          player.bet = currentBet;
          pot += callAmount;
          break;
        }
        case 'BET':
        case 'RAISE': {
          const betAmount = action.amount - player.bet;
          player.chips -= betAmount;
          player.bet = action.amount;
          currentBet = action.amount;
          pot += betAmount;
          break;
        }
      }
    }

    return {
      players: Array.from(playerStates.values()),
      pot,
      currentBet,
      phase,
      board,
      actionIndex,
      totalActions: replay.actions.length,
      currentAction: actionIndex > 0 ? replay.actions[actionIndex - 1] : null,
      isComplete: actionIndex >= replay.actions.length
    };
  }

  

  formatAction(action: ReplayAction): string {
    switch (action.action.toUpperCase()) {
      case 'FOLD': return `${action.playerName} folds`;
      case 'CHECK': return `${action.playerName} checks`;
      case 'CALL': return `${action.playerName} calls $${action.amount}`;
      case 'BET': return `${action.playerName} bets $${action.amount}`;
      case 'RAISE': return `${action.playerName} raises to $${action.amount}`;
      default: return `${action.playerName} ${action.action.toLowerCase()}`;
    }
  }

  formatCard(cardString: string): { suit: string, value: string, symbol: string } {
    
    const parts = cardString.split(' of ');
    if (parts.length !== 2) {
      return { suit: '', value: '', symbol: '?' };
    }

    const value = parts[0];
    const suit = parts[1];
    
    const suitSymbols: Record<string, string> = {
      'SPADES': '♠',
      'HEARTS': '♥',
      'DIAMONDS': '♦',
      'CLUBS': '♣'
    };

    const valueShort: Record<string, string> = {
      'ACE': 'A', 'KING': 'K', 'QUEEN': 'Q', 'JACK': 'J',
      'TEN': '10', 'NINE': '9', 'EIGHT': '8', 'SEVEN': '7',
      'SIX': '6', 'FIVE': '5', 'FOUR': '4', 'THREE': '3', 'TWO': '2'
    };

    return {
      suit: suit,
      value: value,
      symbol: (valueShort[value] || value) + (suitSymbols[suit] || '')
    };
  }

  formatPhase(phase: string): string {
    const phases: Record<string, string> = {
      'PRE_FLOP': 'Pre-Flop',
      'FLOP': 'Flop',
      'TURN': 'Turn',
      'RIVER': 'River',
      'SHOWDOWN': 'Showdown'
    };
    return phases[phase] || phase;
  }

  

  deleteGameHistory(gameId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/game/${gameId}`).pipe(
      catchError(error => {
        console.error('Error deleting game history:', error);
        throw error;
      })
    );
  }
}


export interface PlayerReplayState {
  name: string;
  chips: number;
  bet: number;
  folded: boolean;
  cards: string[];
}

export interface ReplayState {
  players: PlayerReplayState[];
  pot: number;
  currentBet: number;
  phase: string;
  board: string[];
  actionIndex: number;
  totalActions: number;
  currentAction: ReplayAction | null;
  isComplete: boolean;
}
