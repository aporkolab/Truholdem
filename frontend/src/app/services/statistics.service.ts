import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface PlayerStatistics {
  id: string;
  userId?: string;
  playerName: string;
  handsPlayed: number;
  handsWon: number;
  totalWinnings: number;
  totalLosses: number;
  biggestPotWon: number;
  
  
  winRate?: number;
  vpip?: number;
  pfr?: number;
  aggressionFactor?: number;
  wtsd?: number;
  wonAtShowdown?: number;
  netProfit?: number;
  
  
  currentWinStreak: number;
  longestWinStreak: number;
  currentLoseStreak: number;
  longestLoseStreak: number;
  
  
  firstHandPlayed?: string;
  lastHandPlayed?: string;
  totalSessions: number;
}

export interface PlayerStatsSummary {
  playerName: string;
  handsPlayed: number;
  handsWon: number;
  winRate: number;
  netProfit: number;
  vpip: number;
  pfr: number;
  aggressionFactor: number;
  wtsd: number;
  wonAtShowdown: number;
  biggestPotWon: number;
  longestWinStreak: number;
  totalSessions: number;
}

export interface LeaderboardData {
  byWinnings: PlayerStatistics[];
  byHandsWon: PlayerStatistics[];
  byWinRate: PlayerStatistics[];
  byBiggestPot: PlayerStatistics[];
  byWinStreak: PlayerStatistics[];
  mostActive: PlayerStatistics[];
}

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private http = inject(HttpClient);
  
  private readonly apiUrl = `${environment.apiUrl}/stats`;
  
  
  private currentStatsSubject = new BehaviorSubject<PlayerStatistics | null>(null);
  public currentStats$ = this.currentStatsSubject.asObservable();
  
  
  private leaderboardSubject = new BehaviorSubject<LeaderboardData | null>(null);
  public leaderboard$ = this.leaderboardSubject.asObservable();

  

  getPlayerStats(playerName: string): Observable<PlayerStatistics> {
    return this.http.get<PlayerStatistics>(`${this.apiUrl}/player/${encodeURIComponent(playerName)}`).pipe(
      tap(stats => {
        if (stats) {
          
          this.enrichStats(stats);
        }
      }),
      catchError(error => {
        console.error('Error fetching player stats:', error);
        return of(this.createEmptyStats(playerName));
      })
    );
  }

  getPlayerStatsByUserId(userId: string): Observable<PlayerStatistics> {
    return this.http.get<PlayerStatistics>(`${this.apiUrl}/player/id/${userId}`).pipe(
      tap(stats => stats && this.enrichStats(stats)),
      catchError(error => {
        console.error('Error fetching player stats by user ID:', error);
        throw error;
      })
    );
  }

  getPlayerStatsSummary(playerName: string): Observable<PlayerStatsSummary> {
    return this.http.get<PlayerStatsSummary>(`${this.apiUrl}/player/${encodeURIComponent(playerName)}/summary`).pipe(
      catchError(error => {
        console.error('Error fetching stats summary:', error);
        throw error;
      })
    );
  }

  searchPlayers(query: string): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/search`, {
      params: { query }
    }).pipe(
      tap(results => results.forEach(s => this.enrichStats(s))),
      catchError(error => {
        console.error('Error searching players:', error);
        return of([]);
      })
    );
  }

  

  getLeaderboard(): Observable<LeaderboardData> {
    return this.http.get<LeaderboardData>(`${this.apiUrl}/leaderboard`).pipe(
      tap(data => {
        
        [data.byWinnings, data.byHandsWon, data.byWinRate, 
         data.byBiggestPot, data.byWinStreak, data.mostActive]
          .flat()
          .forEach(s => this.enrichStats(s));
        
        this.leaderboardSubject.next(data);
      }),
      catchError(error => {
        console.error('Error fetching leaderboard:', error);
        return of(this.createEmptyLeaderboard());
      })
    );
  }

  getTopByWinnings(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/winnings`).pipe(
      tap(results => results.forEach(s => this.enrichStats(s))),
      catchError(() => of([]))
    );
  }

  getTopByHandsWon(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/hands-won`).pipe(
      tap(results => results.forEach(s => this.enrichStats(s))),
      catchError(() => of([]))
    );
  }

  getTopByWinRate(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/win-rate`).pipe(
      tap(results => results.forEach(s => this.enrichStats(s))),
      catchError(() => of([]))
    );
  }

  getMostActive(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/most-active`).pipe(
      tap(results => results.forEach(s => this.enrichStats(s))),
      catchError(() => of([]))
    );
  }

  

  private enrichStats(stats: PlayerStatistics): void {
    if (!stats) return;

    
    if (stats.winRate === undefined && stats.handsPlayed > 0) {
      stats.winRate = (stats.handsWon / stats.handsPlayed) * 100;
    }

    
    if (stats.netProfit === undefined) {
      stats.netProfit = stats.totalWinnings - stats.totalLosses;
    }
  }

  private createEmptyStats(playerName: string): PlayerStatistics {
    return {
      id: '',
      playerName,
      handsPlayed: 0,
      handsWon: 0,
      totalWinnings: 0,
      totalLosses: 0,
      biggestPotWon: 0,
      winRate: 0,
      netProfit: 0,
      currentWinStreak: 0,
      longestWinStreak: 0,
      currentLoseStreak: 0,
      longestLoseStreak: 0,
      totalSessions: 0
    };
  }

  private createEmptyLeaderboard(): LeaderboardData {
    return {
      byWinnings: [],
      byHandsWon: [],
      byWinRate: [],
      byBiggestPot: [],
      byWinStreak: [],
      mostActive: []
    };
  }

  

  formatWinRate(rate: number): string {
    return `${rate.toFixed(1)}%`;
  }

  formatCurrency(amount: number): string {
    return `$${amount.toLocaleString()}`;
  }

  formatAggressionFactor(af: number): string {
    if (af >= 10) return '10+';
    return af.toFixed(2);
  }

  getPlayerRank(stats: PlayerStatistics): string {
    const winRate = stats.winRate || 0;
    const handsPlayed = stats.handsPlayed;

    if (handsPlayed < 10) return 'Newcomer';
    if (handsPlayed < 50) return 'Beginner';
    if (handsPlayed < 200) return 'Amateur';
    
    if (winRate >= 60) return 'Pro';
    if (winRate >= 50) return 'Regular';
    if (winRate >= 40) return 'Casual';
    return 'Fish';
  }

  getPlayStyleDescription(stats: PlayerStatistics): string {
    const vpip = stats.vpip || 0;
    const pfr = stats.pfr || 0;
    const af = stats.aggressionFactor || 0;

    let style = '';

    
    if (vpip < 20) style = 'Tight';
    else if (vpip < 30) style = 'Normal';
    else style = 'Loose';

    
    if (af > 2 || pfr > vpip * 0.7) style += '-Aggressive';
    else if (af < 1) style += '-Passive';
    else style += '-Balanced';

    return style;
  }
}
