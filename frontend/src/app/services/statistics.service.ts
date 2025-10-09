import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface PlayerStatistics {
  id?: string;
  playerName: string;
  handsPlayed: number;
  handsWon: number;
  totalWinnings: number;
  totalLosses: number;
  biggestPotWon: number;
  currentWinStreak: number;
  longestWinStreak: number;
  currentLoseStreak: number;
  longestLoseStreak: number;
  totalSessions: number;
  lastHandPlayed?: string;

  winRate?: number;
  netProfit?: number;
  vpip?: number;
  pfr?: number;
  aggressionFactor?: number;
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
  providedIn: 'root',
})
export class StatisticsService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/stats`;


  private leaderboardSubject = new BehaviorSubject<LeaderboardData | null>(null);
  public leaderboard$ = this.leaderboardSubject.asObservable();



  getPlayerStats(playerName: string): Observable<PlayerStatistics> {
    return this.http.get<PlayerStatistics>(`${this.apiUrl}/player/${encodeURIComponent(playerName)}`).pipe(
      map(stats => this.enrichStats(stats)),
      catchError(() => of(this.createEmptyStats(playerName)))
    );
  }

  getPlayerStatsByUserId(userId: string): Observable<PlayerStatistics> {
    return this.http.get<PlayerStatistics>(`${this.apiUrl}/player/id/${userId}`).pipe(
      map(stats => this.enrichStats(stats)),
      catchError(() => of(this.createEmptyStats('Unknown')))
    );
  }

  getPlayerStatsSummary(playerName: string): Observable<PlayerStatsSummary> {
    return this.http.get<PlayerStatsSummary>(`${this.apiUrl}/player/${encodeURIComponent(playerName)}/summary`);
  }

  searchPlayers(query: string): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/search`, {
      params: { query }
    }).pipe(
      catchError(() => of([]))
    );
  }



  getLeaderboard(): Observable<LeaderboardData> {
    return this.http.get<LeaderboardData>(`${this.apiUrl}/leaderboard`).pipe(
      tap(data => this.leaderboardSubject.next(data)),
      catchError(() => of(this.createEmptyLeaderboard()))
    );
  }

  getTopByWinnings(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/winnings`).pipe(
      catchError(() => of([]))
    );
  }

  getTopByHandsWon(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/hands-won`).pipe(
      catchError(() => of([]))
    );
  }

  getTopByWinRate(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/win-rate`).pipe(
      catchError(() => of([]))
    );
  }

  getMostActive(): Observable<PlayerStatistics[]> {
    return this.http.get<PlayerStatistics[]>(`${this.apiUrl}/leaderboard/most-active`).pipe(
      catchError(() => of([]))
    );
  }



  private enrichStats(stats: PlayerStatistics): PlayerStatistics {
    return {
      ...stats,
      winRate: stats.winRate ?? this.calculateWinRate(stats),
      netProfit: stats.netProfit ?? this.calculateNetProfit(stats),
    };
  }

  private calculateWinRate(stats: PlayerStatistics): number {
    if (stats.handsPlayed === 0) return 0;
    return Math.round((stats.handsWon / stats.handsPlayed) * 100);
  }

  private calculateNetProfit(stats: PlayerStatistics): number {
    return stats.totalWinnings - stats.totalLosses;
  }



  formatWinRate(rate: number): string {
    return `${rate.toFixed(1)}%`;
  }

  formatCurrency(amount: number): string {
    if (amount < 0) {
      return `-$${Math.abs(amount).toLocaleString()}`;
    }
    return `$${amount.toLocaleString()}`;
  }

  formatAggressionFactor(af: number): string {
    if (af >= 10) return '10+';
    return af.toFixed(2);
  }



  getPlayerRank(stats: PlayerStatistics): string {
    const { handsPlayed, winRate = 0 } = stats;

    if (handsPlayed < 10) return 'Newcomer';
    if (handsPlayed < 50) return 'Beginner';
    if (handsPlayed < 200) return 'Amateur';


    if (winRate >= 60) return 'Pro';
    if (winRate >= 50) return 'Regular';
    if (winRate >= 40) return 'Casual';
    return 'Fish';
  }



  getPlayStyleDescription(stats: PlayerStatistics & { vpip?: number; pfr?: number; aggressionFactor?: number }): string {
    const { vpip = 25, aggressionFactor = 1 } = stats;

    const tightness = vpip < 20 ? 'Tight' : vpip > 30 ? 'Loose' : 'Normal';
    const aggression = aggressionFactor < 1 ? 'Passive' : aggressionFactor > 2 ? 'Aggressive' : 'Balanced';

    return `${tightness}-${aggression}`;
  }



  private createEmptyStats(playerName: string): PlayerStatistics {
    return {
      playerName,
      handsPlayed: 0,
      handsWon: 0,
      totalWinnings: 0,
      totalLosses: 0,
      biggestPotWon: 0,
      currentWinStreak: 0,
      longestWinStreak: 0,
      currentLoseStreak: 0,
      longestLoseStreak: 0,
      totalSessions: 0,
      winRate: 0,
      netProfit: 0,
    };
  }

  private createEmptyLeaderboard(): LeaderboardData {
    return {
      byWinnings: [],
      byHandsWon: [],
      byWinRate: [],
      byBiggestPot: [],
      byWinStreak: [],
      mostActive: [],
    };
  }
}
