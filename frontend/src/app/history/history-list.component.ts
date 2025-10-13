import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { HandHistoryService, HandHistory } from '../services/hand-history.service';


interface HandHistoryEntry {
  id: string;
  handNumber: number;
  isWin: boolean;
  handType: string;
  potSize: number;
  playedAt: Date;
}

@Component({
  selector: 'app-history-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="history-container" data-cy="history-page">
      <div class="history-header" data-cy="history-header">
        <h1 data-cy="history-title">📜 Hand History</h1>
        <p class="subtitle" data-cy="history-subtitle">Review your past hands and learn from them</p>
      </div>

      @if (isLoading) {
        <div class="loading-state" data-cy="loading-state">
          <div class="spinner" data-cy="spinner"></div>
          <p>Loading hand history...</p>
        </div>
      } @else if (error) {
        <div class="error-state" data-cy="error-state" role="alert">
          <span class="error-icon">⚠️</span>
          <p data-cy="error-message">{{ error }}</p>
          <button class="btn-retry" (click)="loadHistory()" data-cy="retry-btn">Try Again</button>
        </div>
      } @else if (hands.length === 0) {
        <div class="empty-state" data-cy="empty-state">
          <span class="empty-icon">🃏</span>
          <h2 data-cy="empty-title">No hands played yet</h2>
          <p>Start playing to build your hand history!</p>
          <a routerLink="/lobby" class="btn-primary" data-cy="play-now-btn">🎮 Play Now</a>
        </div>
      } @else {
        <div class="stats-summary" data-cy="stats-summary">
          <div class="stat-card" data-cy="total-hands-card">
            <span class="stat-value" data-cy="total-hands-value">{{ hands.length }}</span>
            <span class="stat-label">Total Hands</span>
          </div>
          <div class="stat-card" data-cy="total-winnings-card">
            <span class="stat-value" data-cy="total-winnings-value">{{ totalWinnings | number }}</span>
            <span class="stat-label">Total Winnings</span>
          </div>
          <div class="stat-card" data-cy="win-rate-card">
            <span class="stat-value" data-cy="win-rate-value">{{ winRate | number:'1.1-1' }}%</span>
            <span class="stat-label">Win Rate</span>
          </div>
        </div>

        <div class="history-list" data-cy="history-list" role="list">
          @for (hand of hands; track hand.id; let i = $index) {
            <a [routerLink]="['/replay', hand.id]" 
               class="hand-card" 
               [attr.data-cy]="'hand-card-' + i"
               data-cy="hand-card"
               role="listitem">
              <div class="hand-info" data-cy="hand-info">
                <span class="hand-number" data-cy="hand-number">#{{ hand.handNumber }}</span>
                <span class="hand-result" [class.win]="hand.isWin" [class.loss]="!hand.isWin" data-cy="hand-result">
                  {{ hand.isWin ? '🏆 Won' : '❌ Lost' }}
                </span>
              </div>
              <div class="hand-details" data-cy="hand-details">
                <span class="hand-type" data-cy="hand-type">{{ hand.handType || 'Unknown' }}</span>
                <span class="hand-pot" data-cy="hand-pot">
                  💰 {{ hand.potSize | number }}
                </span>
              </div>
              <div class="hand-meta" data-cy="hand-meta">
                <span class="hand-date" data-cy="hand-date">{{ hand.playedAt | date:'short' }}</span>
                <span class="replay-hint" data-cy="replay-hint">View replay →</span>
              </div>
            </a>
          }
        </div>
      }

      <div class="back-link" data-cy="back-link">
        <a routerLink="/" class="btn-back" data-cy="back-to-home-btn">← Back to Home</a>
      </div>
    </div>
  `,
  styles: [`
    .history-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
      color: #fff;
      padding: 2rem;
    }

    .history-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .history-header h1 {
      font-size: 2.5rem;
      margin: 0;
      background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .subtitle {
      color: #94a3b8;
      margin-top: 0.5rem;
    }

    .loading-state, .error-state, .empty-state {
      text-align: center;
      padding: 4rem 2rem;
    }

    .spinner {
      width: 3rem;
      height: 3rem;
      border: 3px solid rgba(255, 255, 255, 0.2);
      border-top-color: #ffd700;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 1rem;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error-icon, .empty-icon {
      font-size: 4rem;
      display: block;
      margin-bottom: 1rem;
    }

    .btn-retry, .btn-primary {
      padding: 0.75rem 1.5rem;
      font-size: 1rem;
      font-weight: 600;
      border: none;
      border-radius: 0.5rem;
      cursor: pointer;
      text-decoration: none;
      display: inline-block;
      margin-top: 1rem;
    }

    .btn-retry {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
    }

    .btn-primary {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: #fff;
    }

    .stats-summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem;
      max-width: 600px;
      margin: 0 auto 2rem;
    }

    .stat-card {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 0.75rem;
      padding: 1rem;
      text-align: center;
    }

    .stat-value {
      display: block;
      font-size: 1.5rem;
      font-weight: 700;
      color: #ffd700;
    }

    .stat-label {
      display: block;
      font-size: 0.875rem;
      color: #94a3b8;
      margin-top: 0.25rem;
    }

    .history-list {
      max-width: 800px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .hand-card {
      display: grid;
      grid-template-columns: 1fr 1fr auto;
      gap: 1rem;
      align-items: center;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 0.75rem;
      padding: 1rem 1.5rem;
      text-decoration: none;
      color: inherit;
      transition: all 0.2s ease;
    }

    .hand-card:hover {
      background: rgba(255, 255, 255, 0.08);
      transform: translateX(4px);
      border-color: rgba(255, 215, 0, 0.3);
    }

    .hand-info {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .hand-number {
      font-weight: 600;
      color: #94a3b8;
    }

    .hand-result {
      padding: 0.25rem 0.75rem;
      border-radius: 1rem;
      font-size: 0.875rem;
      font-weight: 600;
    }

    .hand-result.win {
      background: rgba(16, 185, 129, 0.2);
      color: #10b981;
    }

    .hand-result.loss {
      background: rgba(239, 68, 68, 0.2);
      color: #ef4444;
    }

    .hand-details {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .hand-type {
      color: #ffd700;
      font-weight: 500;
    }

    .hand-pot {
      color: #94a3b8;
    }

    .hand-meta {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 0.25rem;
    }

    .hand-date {
      color: #64748b;
      font-size: 0.875rem;
    }

    .replay-hint {
      color: #ffd700;
      font-size: 0.875rem;
      opacity: 0;
      transition: opacity 0.2s;
    }

    .hand-card:hover .replay-hint {
      opacity: 1;
    }

    .back-link {
      text-align: center;
      margin-top: 3rem;
    }

    .btn-back {
      color: #94a3b8;
      text-decoration: none;
    }

    .btn-back:hover {
      color: #fff;
    }

    @media (max-width: 640px) {
      .hand-card {
        grid-template-columns: 1fr;
        gap: 0.5rem;
      }

      .hand-meta {
        flex-direction: row;
        justify-content: space-between;
        align-items: center;
      }
    }
  `]
})
export class HistoryListComponent implements OnInit, OnDestroy {
  private readonly historyService = inject(HandHistoryService);
  private readonly destroy$ = new Subject<void>();

  hands: HandHistoryEntry[] = [];
  isLoading = false;
  error: string | null = null;

  get totalWinnings(): number {
    return this.hands
      .filter(h => h.isWin)
      .reduce((sum, h) => sum + (h.potSize || 0), 0);
  }

  get winRate(): number {
    if (this.hands.length === 0) return 0;
    const wins = this.hands.filter(h => h.isWin).length;
    return (wins / this.hands.length) * 100;
  }

  ngOnInit(): void {
    this.loadHistory();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadHistory(): void {
    this.isLoading = true;
    this.error = null;

    
    this.historyService.getRecentHands()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (histories) => {
          this.hands = this.mapToEntries(histories);
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Failed to load history:', err);
          this.error = 'Failed to load hand history. Please try again.';
          this.isLoading = false;
        }
      });
  }

  private mapToEntries(histories: HandHistory[]): HandHistoryEntry[] {
    
    const currentUser = localStorage.getItem('user');
    const username = currentUser ? JSON.parse(currentUser).username : '';

    return histories.map(h => ({
      id: h.id,
      handNumber: h.handNumber,
      isWin: h.winnerName === username,
      handType: h.winningHandDescription,
      potSize: h.finalPot,
      playedAt: new Date(h.playedAt)
    }));
  }
}
