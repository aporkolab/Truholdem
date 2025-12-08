import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { 
  StatisticsService, 
  PlayerStatistics, 
  LeaderboardData 
} from '../services/statistics.service';

type LeaderboardCategory = 'winnings' | 'handsWon' | 'winRate' | 'biggestPot' | 'winStreak' | 'mostActive';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="leaderboard">
      <div class="leaderboard-header">
        <h2>🏆 Leaderboard</h2>
        <div class="category-tabs">
          <button 
            *ngFor="let cat of categories" 
            class="tab-btn"
            [class.active]="selectedCategory === cat.key"
            (click)="selectCategory(cat.key)">
            {{ cat.icon }} {{ cat.label }}
          </button>
        </div>
      </div>

      <div class="leaderboard-content" *ngIf="!isLoading && currentList.length > 0">
        <table class="leaderboard-table">
          <thead>
            <tr>
              <th class="rank">#</th>
              <th class="player">Player</th>
              <th class="stat" *ngFor="let col of getColumns()">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr 
              *ngFor="let player of currentList; let i = index"
              [class.highlight]="i < 3"
              [class.gold]="i === 0"
              [class.silver]="i === 1"
              [class.bronze]="i === 2">
              <td class="rank">
                <span class="rank-badge" *ngIf="i < 3">{{ getRankEmoji(i) }}</span>
                <span *ngIf="i >= 3">{{ i + 1 }}</span>
              </td>
              <td class="player">
                <div class="player-info">
                  <span class="player-name">{{ player.playerName }}</span>
                  <span class="player-rank">{{ statsService.getPlayerRank(player) }}</span>
                </div>
              </td>
              <td class="stat" *ngFor="let col of getColumns()">
                {{ formatValue(player, col.key) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="loading" *ngIf="isLoading">
        <div class="spinner"></div>
        <span>Loading leaderboard...</span>
      </div>

      <div class="empty-state" *ngIf="!isLoading && currentList.length === 0">
        <span class="empty-icon">📊</span>
        <span>No data available yet</span>
        <span class="empty-hint">Play some hands to appear on the leaderboard!</span>
      </div>

      <!-- Player Search -->
      <div class="player-search">
        <h3>Find a Player</h3>
        <div class="search-box">
          <input 
            type="text" 
            [(ngModel)]="searchQuery" 
            placeholder="Enter player name..."
            (keyup.enter)="searchPlayer()">
          <button class="btn btn-primary" (click)="searchPlayer()">Search</button>
        </div>

        <div class="search-results" *ngIf="searchResults.length > 0">
          <div 
            *ngFor="let player of searchResults" 
            class="search-result-item"
            (click)="selectPlayer(player)">
            <span class="result-name">{{ player.playerName }}</span>
            <span class="result-stats">
              {{ player.handsPlayed }} hands | {{ statsService.formatWinRate(player.winRate || 0) }} win rate
            </span>
          </div>
        </div>
      </div>

      <!-- Player Detail Modal -->
      <div class="modal-overlay" *ngIf="selectedPlayer" (click)="closePlayerDetail()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ selectedPlayer.playerName }}</h3>
            <button class="close-btn" (click)="closePlayerDetail()">×</button>
          </div>
          <div class="modal-body">
            <div class="stat-grid">
              <div class="stat-item">
                <span class="stat-label">Hands Played</span>
                <span class="stat-value">{{ selectedPlayer.handsPlayed }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Hands Won</span>
                <span class="stat-value">{{ selectedPlayer.handsWon }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Win Rate</span>
                <span class="stat-value">{{ statsService.formatWinRate(selectedPlayer.winRate || 0) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Net Profit</span>
                <span class="stat-value" [class.positive]="(selectedPlayer.netProfit || 0) > 0" [class.negative]="(selectedPlayer.netProfit || 0) < 0">
                  {{ statsService.formatCurrency(selectedPlayer.netProfit || 0) }}
                </span>
              </div>
              <div class="stat-item">
                <span class="stat-label">VPIP</span>
                <span class="stat-value">{{ (selectedPlayer.vpip || 0).toFixed(1) }}%</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">PFR</span>
                <span class="stat-value">{{ (selectedPlayer.pfr || 0).toFixed(1) }}%</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Aggression</span>
                <span class="stat-value">{{ statsService.formatAggressionFactor(selectedPlayer.aggressionFactor || 0) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Biggest Pot</span>
                <span class="stat-value">{{ statsService.formatCurrency(selectedPlayer.biggestPotWon) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Longest Win Streak</span>
                <span class="stat-value">{{ selectedPlayer.longestWinStreak }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Play Style</span>
                <span class="stat-value">{{ statsService.getPlayStyleDescription(selectedPlayer) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .leaderboard {
      padding: 1.5rem;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
      border-radius: 12px;
      color: #fff;
    }

    .leaderboard-header {
      margin-bottom: 1.5rem;

      h2 {
        margin: 0 0 1rem;
        color: #ffd700;
      }
    }

    .category-tabs {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .tab-btn {
      padding: 0.5rem 1rem;
      background: rgba(255, 255, 255, 0.1);
      border: none;
      border-radius: 20px;
      color: rgba(255, 255, 255, 0.7);
      cursor: pointer;
      transition: all 0.2s;
      font-size: 0.85rem;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }

      &.active {
        background: #3b82f6;
        color: #fff;
      }
    }

    .leaderboard-table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: 0.75rem;
        text-align: left;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      }

      th {
        font-weight: 600;
        color: rgba(255, 255, 255, 0.6);
        font-size: 0.85rem;
        text-transform: uppercase;
      }

      .rank {
        width: 50px;
        text-align: center;
      }

      .rank-badge {
        font-size: 1.2rem;
      }

      tr.highlight {
        background: rgba(255, 215, 0, 0.05);
      }

      tr.gold td { border-left: 3px solid #ffd700; }
      tr.silver td { border-left: 3px solid #c0c0c0; }
      tr.bronze td { border-left: 3px solid #cd7f32; }
    }

    .player-info {
      display: flex;
      flex-direction: column;

      .player-name {
        font-weight: 600;
      }

      .player-rank {
        font-size: 0.75rem;
        color: rgba(255, 255, 255, 0.5);
      }
    }

    .loading, .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 3rem;
      gap: 1rem;
      color: rgba(255, 255, 255, 0.6);
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid rgba(255, 255, 255, 0.1);
      border-top-color: #3b82f6;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    .empty-icon { font-size: 3rem; }
    .empty-hint { font-size: 0.85rem; }

    .player-search {
      margin-top: 2rem;
      padding-top: 1.5rem;
      border-top: 1px solid rgba(255, 255, 255, 0.1);

      h3 {
        margin: 0 0 1rem;
        font-size: 1rem;
        color: rgba(255, 255, 255, 0.8);
      }
    }

    .search-box {
      display: flex;
      gap: 0.5rem;

      input {
        flex: 1;
        padding: 0.5rem 1rem;
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 4px;
        color: #fff;

        &::placeholder {
          color: rgba(255, 255, 255, 0.4);
        }
      }

      .btn {
        padding: 0.5rem 1rem;
        background: #3b82f6;
        border: none;
        border-radius: 4px;
        color: #fff;
        cursor: pointer;

        &:hover { background: #2563eb; }
      }
    }

    .search-results {
      margin-top: 1rem;
    }

    .search-result-item {
      display: flex;
      justify-content: space-between;
      padding: 0.75rem;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 4px;
      margin-bottom: 0.5rem;
      cursor: pointer;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
      }

      .result-stats {
        color: rgba(255, 255, 255, 0.5);
        font-size: 0.85rem;
      }
    }

    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .modal-content {
      background: #1a1a2e;
      border-radius: 12px;
      width: 90%;
      max-width: 500px;
      max-height: 90vh;
      overflow-y: auto;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.5rem;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      h3 { margin: 0; color: #ffd700; }

      .close-btn {
        background: none;
        border: none;
        color: #fff;
        font-size: 1.5rem;
        cursor: pointer;
      }
    }

    .modal-body {
      padding: 1.5rem;
    }

    .stat-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1rem;
    }

    .stat-item {
      background: rgba(255, 255, 255, 0.05);
      padding: 1rem;
      border-radius: 8px;

      .stat-label {
        display: block;
        font-size: 0.75rem;
        color: rgba(255, 255, 255, 0.5);
        margin-bottom: 0.25rem;
      }

      .stat-value {
        font-size: 1.1rem;
        font-weight: 600;

        &.positive { color: #4ade80; }
        &.negative { color: #f87171; }
      }
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LeaderboardComponent implements OnInit, OnDestroy {
  statsService = inject(StatisticsService);
  private destroy$ = new Subject<void>();

  categories = [
    { key: 'winnings' as LeaderboardCategory, label: 'Winnings', icon: '💰' },
    { key: 'handsWon' as LeaderboardCategory, label: 'Hands Won', icon: '🎯' },
    { key: 'winRate' as LeaderboardCategory, label: 'Win Rate', icon: '📈' },
    { key: 'biggestPot' as LeaderboardCategory, label: 'Biggest Pot', icon: '🎰' },
    { key: 'winStreak' as LeaderboardCategory, label: 'Win Streak', icon: '🔥' },
    { key: 'mostActive' as LeaderboardCategory, label: 'Most Active', icon: '⚡' }
  ];

  selectedCategory: LeaderboardCategory = 'winnings';
  leaderboardData: LeaderboardData | null = null;
  currentList: PlayerStatistics[] = [];
  isLoading = true;

  
  searchQuery = '';
  searchResults: PlayerStatistics[] = [];
  selectedPlayer: PlayerStatistics | null = null;

  ngOnInit(): void {
    this.loadLeaderboard();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadLeaderboard(): void {
    this.isLoading = true;
    this.statsService.getLeaderboard().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (data) => {
        this.leaderboardData = data;
        this.updateCurrentList();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  selectCategory(category: LeaderboardCategory): void {
    this.selectedCategory = category;
    this.updateCurrentList();
  }

  private updateCurrentList(): void {
    if (!this.leaderboardData) {
      this.currentList = [];
      return;
    }

    switch (this.selectedCategory) {
      case 'winnings': this.currentList = this.leaderboardData.byWinnings; break;
      case 'handsWon': this.currentList = this.leaderboardData.byHandsWon; break;
      case 'winRate': this.currentList = this.leaderboardData.byWinRate; break;
      case 'biggestPot': this.currentList = this.leaderboardData.byBiggestPot; break;
      case 'winStreak': this.currentList = this.leaderboardData.byWinStreak; break;
      case 'mostActive': this.currentList = this.leaderboardData.mostActive; break;
    }
  }

  getColumns(): { key: string, label: string }[] {
    switch (this.selectedCategory) {
      case 'winnings': return [
        { key: 'totalWinnings', label: 'Winnings' },
        { key: 'handsPlayed', label: 'Hands' },
        { key: 'winRate', label: 'Win %' }
      ];
      case 'handsWon': return [
        { key: 'handsWon', label: 'Wins' },
        { key: 'handsPlayed', label: 'Played' },
        { key: 'winRate', label: 'Win %' }
      ];
      case 'winRate': return [
        { key: 'winRate', label: 'Win %' },
        { key: 'handsPlayed', label: 'Hands' },
        { key: 'handsWon', label: 'Wins' }
      ];
      case 'biggestPot': return [
        { key: 'biggestPotWon', label: 'Biggest Pot' },
        { key: 'totalWinnings', label: 'Total' },
        { key: 'handsPlayed', label: 'Hands' }
      ];
      case 'winStreak': return [
        { key: 'longestWinStreak', label: 'Best Streak' },
        { key: 'currentWinStreak', label: 'Current' },
        { key: 'handsWon', label: 'Wins' }
      ];
      case 'mostActive': return [
        { key: 'handsPlayed', label: 'Hands' },
        { key: 'totalSessions', label: 'Sessions' },
        { key: 'winRate', label: 'Win %' }
      ];
    }
  }

  formatValue(player: PlayerStatistics, key: string): string {
    const value = (player as any)[key];
    
    if (key === 'totalWinnings' || key === 'biggestPotWon') {
      return this.statsService.formatCurrency(value || 0);
    }
    if (key === 'winRate') {
      return this.statsService.formatWinRate(value || 0);
    }
    return String(value || 0);
  }

  getRankEmoji(index: number): string {
    return ['🥇', '🥈', '🥉'][index] || '';
  }

  searchPlayer(): void {
    if (!this.searchQuery.trim()) {
      this.searchResults = [];
      return;
    }

    this.statsService.searchPlayers(this.searchQuery).pipe(
      takeUntil(this.destroy$)
    ).subscribe(results => {
      this.searchResults = results;
    });
  }

  selectPlayer(player: PlayerStatistics): void {
    this.selectedPlayer = player;
  }

  closePlayerDetail(): void {
    this.selectedPlayer = null;
  }
}
