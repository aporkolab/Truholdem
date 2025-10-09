import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, EMPTY } from 'rxjs';
import { switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { environment } from '../../environments/environment';






export interface PlayerStatistics {
  id: string;
  playerId: string;
  playerName: string;
  totalGames: number;
  totalHands: number;
  handsWon: number;
  handsLost: number;
  totalWinnings: number;
  totalLosses: number;
  biggestPotWon: number;
  vpip: number;           
  pfr: number;            
  aggression: number;     
  winRate: number;        
  showdownWinRate: number;
  createdAt?: string;
  updatedAt?: string;
}


export interface LeaderboardEntry {
  rank: number;
  playerId: string;
  playerName: string;
  totalWinnings: number;
  handsWon: number;
  winRate: number;
}


export interface HandHistoryEntry {
  id: string;
  handNumber: number;
  players: HandHistoryPlayer[];
  communityCards: string[];
  potSize: number;
  winnerId: string;
  winnerName: string;
  winningHand: string;
  timestamp: string;
  actions: HandHistoryAction[];
}

export interface HandHistoryPlayer {
  playerId: string;
  playerName: string;
  startingChips: number;
  endingChips: number;
  holeCards?: string[];
  position: string;
}

export interface HandHistoryAction {
  phase: string;
  playerId: string;
  playerName: string;
  actionType: string;
  amount?: number;
  timestamp: string;
}


export interface StatisticsStoreState {
  playerStats: PlayerStatistics | null;
  allPlayersStats: PlayerStatistics[];
  leaderboard: LeaderboardEntry[];
  handHistory: HandHistoryEntry[];
  selectedHand: HandHistoryEntry | null;
  isLoading: boolean;
  error: string | null;
  currentPage: number;
  totalPages: number;
  pageSize: number;
}


export interface StatisticsViewModel {
  playerStats: PlayerStatistics | null;
  leaderboard: LeaderboardEntry[];
  handHistory: HandHistoryEntry[];
  isLoading: boolean;
  error: string | null;
  winRate: string;
  profitLoss: number;
  handsPlayed: number;
  handsWon: number;
  winPercentage: string;
  avgPotWon: number;
  currentPage: number;
  totalPages: number;
  hasNextPage: boolean;
  hasPrevPage: boolean;
}





const initialState: StatisticsStoreState = {
  playerStats: null,
  allPlayersStats: [],
  leaderboard: [],
  handHistory: [],
  selectedHand: null,
  isLoading: false,
  error: null,
  currentPage: 0,
  totalPages: 0,
  pageSize: 20
};






@Injectable()
export class StatisticsStore extends ComponentStore<StatisticsStoreState> {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  constructor() {
    super(initialState);
  }

  
  
  

  readonly playerStats$ = this.select(state => state.playerStats);
  readonly allPlayersStats$ = this.select(state => state.allPlayersStats);
  readonly leaderboard$ = this.select(state => state.leaderboard);
  readonly handHistory$ = this.select(state => state.handHistory);
  readonly selectedHand$ = this.select(state => state.selectedHand);
  readonly isLoading$ = this.select(state => state.isLoading);
  readonly error$ = this.select(state => state.error);
  readonly currentPage$ = this.select(state => state.currentPage);
  readonly totalPages$ = this.select(state => state.totalPages);
  readonly pageSize$ = this.select(state => state.pageSize);

  
  
  

  
  readonly winRateFormatted$ = this.select(
    this.playerStats$,
    stats => stats ? `${stats.winRate.toFixed(2)} BB/100` : 'N/A'
  );

  
  readonly profitLoss$ = this.select(
    this.playerStats$,
    stats => stats ? (stats.totalWinnings - stats.totalLosses) : 0
  );

  
  readonly handsPlayed$ = this.select(
    this.playerStats$,
    stats => stats?.totalHands ?? 0
  );

  
  readonly winPercentage$ = this.select(
    this.playerStats$,
    stats => {
      if (!stats || stats.totalHands === 0) return '0%';
      const percentage = (stats.handsWon / stats.totalHands) * 100;
      return `${percentage.toFixed(1)}%`;
    }
  );

  
  readonly avgPotWon$ = this.select(
    this.playerStats$,
    stats => {
      if (!stats || stats.handsWon === 0) return 0;
      return Math.round(stats.totalWinnings / stats.handsWon);
    }
  );

  
  readonly hasNextPage$ = this.select(
    this.currentPage$,
    this.totalPages$,
    (current, total) => current < total - 1
  );

  
  readonly hasPrevPage$ = this.select(
    this.currentPage$,
    current => current > 0
  );

  
  readonly topThree$ = this.select(
    this.leaderboard$,
    leaderboard => leaderboard.slice(0, 3)
  );

  
  readonly playerRank$ = this.select(
    this.leaderboard$,
    this.playerStats$,
    (leaderboard, stats) => {
      if (!stats) return null;
      const entry = leaderboard.find(e => e.playerId === stats.playerId);
      return entry?.rank ?? null;
    }
  );

  
  
  

  readonly vm$: Observable<StatisticsViewModel> = this.select(
    this.playerStats$,
    this.leaderboard$,
    this.handHistory$,
    this.isLoading$,
    this.error$,
    this.winRateFormatted$,
    this.profitLoss$,
    this.handsPlayed$,
    this.playerStats$,
    this.winPercentage$,
    this.avgPotWon$,
    this.currentPage$,
    this.totalPages$,
    this.hasNextPage$,
    this.hasPrevPage$,
    (
      playerStats, leaderboard, handHistory, isLoading, error,
      winRate, profitLoss, handsPlayed, stats, winPercentage, avgPotWon,
      currentPage, totalPages, hasNextPage, hasPrevPage
    ): StatisticsViewModel => ({
      playerStats,
      leaderboard,
      handHistory,
      isLoading,
      error,
      winRate,
      profitLoss,
      handsPlayed,
      handsWon: stats?.handsWon ?? 0,
      winPercentage,
      avgPotWon,
      currentPage,
      totalPages,
      hasNextPage,
      hasPrevPage
    }),
    { debounce: true }
  );

  
  
  

  readonly setPlayerStats = this.updater((state, stats: PlayerStatistics | null) => ({
    ...state,
    playerStats: stats,
    error: null
  }));

  readonly setAllPlayersStats = this.updater((state, stats: PlayerStatistics[]) => ({
    ...state,
    allPlayersStats: stats
  }));

  readonly setLeaderboard = this.updater((state, leaderboard: LeaderboardEntry[]) => ({
    ...state,
    leaderboard
  }));

  readonly setHandHistory = this.updater((state, handHistory: HandHistoryEntry[]) => ({
    ...state,
    handHistory
  }));

  readonly appendHandHistory = this.updater((state, newHands: HandHistoryEntry[]) => ({
    ...state,
    handHistory: [...state.handHistory, ...newHands]
  }));

  readonly setSelectedHand = this.updater((state, hand: HandHistoryEntry | null) => ({
    ...state,
    selectedHand: hand
  }));

  readonly setLoading = this.updater((state, isLoading: boolean) => ({
    ...state,
    isLoading
  }));

  readonly setError = this.updater((state, error: string | null) => ({
    ...state,
    error,
    isLoading: false
  }));

  readonly setPagination = this.updater((state, pagination: { currentPage: number; totalPages: number }) => ({
    ...state,
    currentPage: pagination.currentPage,
    totalPages: pagination.totalPages
  }));

  readonly setPageSize = this.updater((state, pageSize: number) => ({
    ...state,
    pageSize
  }));

  readonly reset = this.updater(() => initialState);

  
  
  

  
  readonly loadPlayerStats = this.effect<string>(playerId$ =>
    playerId$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(playerId =>
        this.http.get<PlayerStatistics>(`${this.apiUrl}/statistics/player/${playerId}`).pipe(
          tapResponse(
            stats => this.setPlayerStats(stats),
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );

  
  readonly loadAllPlayersStats = this.effect<void>(trigger$ =>
    trigger$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(() =>
        this.http.get<PlayerStatistics[]>(`${this.apiUrl}/statistics/all`).pipe(
          tapResponse(
            stats => {
              this.setAllPlayersStats(stats);
              this.setLoading(false);
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );

  
  readonly loadLeaderboard = this.effect<{ limit?: number } | void>(params$ =>
    params$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(params => {
        const limit = (params && typeof params === 'object') ? params.limit : 10;
        return this.http.get<LeaderboardEntry[]>(
          `${this.apiUrl}/statistics/leaderboard`,
          { params: { limit: limit?.toString() ?? '10' } }
        ).pipe(
          tapResponse(
            leaderboard => {
              this.setLeaderboard(leaderboard);
              this.setLoading(false);
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        );
      })
    )
  );

  
  readonly loadHandHistory = this.effect<{ playerId: string; page?: number }>(params$ =>
    params$.pipe(
      tap(() => this.setLoading(true)),
      withLatestFrom(this.pageSize$),
      switchMap(([{ playerId, page = 0 }, pageSize]) =>
        this.http.get<{ content: HandHistoryEntry[]; totalPages: number; number: number }>(
          `${this.apiUrl}/hand-history/player/${playerId}`,
          { params: { page: page.toString(), size: pageSize.toString() } }
        ).pipe(
          tapResponse(
            response => {
              this.setHandHistory(response.content);
              this.setPagination({
                currentPage: response.number,
                totalPages: response.totalPages
              });
              this.setLoading(false);
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );

  
  readonly loadMoreHistory = this.effect<string>(playerId$ =>
    playerId$.pipe(
      withLatestFrom(this.currentPage$, this.hasNextPage$, this.pageSize$),
      tap(([, , hasNext]) => {
        if (hasNext) this.setLoading(true);
      }),
      switchMap(([playerId, currentPage, hasNext, pageSize]) => {
        if (!hasNext) return EMPTY;
        
        const nextPage = currentPage + 1;
        return this.http.get<{ content: HandHistoryEntry[]; totalPages: number; number: number }>(
          `${this.apiUrl}/hand-history/player/${playerId}`,
          { params: { page: nextPage.toString(), size: pageSize.toString() } }
        ).pipe(
          tapResponse(
            response => {
              this.appendHandHistory(response.content);
              this.setPagination({
                currentPage: response.number,
                totalPages: response.totalPages
              });
              this.setLoading(false);
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        );
      })
    )
  );

  
  readonly loadHandDetails = this.effect<string>(handId$ =>
    handId$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(handId =>
        this.http.get<HandHistoryEntry>(`${this.apiUrl}/hand-history/${handId}`).pipe(
          tapResponse(
            hand => {
              this.setSelectedHand(hand);
              this.setLoading(false);
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );

  
  readonly initializeDashboard = this.effect<string>(playerId$ =>
    playerId$.pipe(
      tap(() => this.setLoading(true)),
      tap(playerId => {
        this.loadPlayerStats(playerId);
        this.loadLeaderboard({ limit: 10 });
        this.loadHandHistory({ playerId, page: 0 });
      })
    )
  );

  
  
  

  private handleError(error: HttpErrorResponse): void {
    const errorMessage = error.error instanceof ErrorEvent
      ? `Error: ${error.error.message}`
      : error.status === 404
        ? 'Statistics not found'
        : error.error?.message || `Error Code: ${error.status}, Message: ${error.message}`;

    this.setError(errorMessage);
    console.error('[StatisticsStore]', errorMessage, error);
  }
}
