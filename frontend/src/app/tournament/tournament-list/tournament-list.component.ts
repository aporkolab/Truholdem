import { 
  Component, 
  inject, 
  OnInit, 
  OnDestroy, 
  ChangeDetectionStrategy,
  computed 
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject, takeUntil, interval } from 'rxjs';

import { TournamentStore } from '../../store/tournament.store';
import { TournamentCardComponent } from '../tournament-card/tournament-card.component';
import { TournamentListItem } from '../../model/tournament';

@Component({
  selector: 'app-tournament-list',
  standalone: true,
  imports: [CommonModule, TournamentCardComponent],
  providers: [TournamentStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tournament-list-container" data-cy="tournament-list">
      <header class="list-header">
        <h1 data-cy="list-title">🏆 Tournaments</h1>
        <p class="subtitle">Join a tournament and compete for prizes</p>
      </header>

      @if (isLoading()) {
        <div class="loading-state" data-cy="loading-indicator">
          <div class="spinner"></div>
          <p>Loading tournaments...</p>
        </div>
      }

      @if (error()) {
        <div class="error-state" role="alert" data-cy="error-message">
          <span class="error-icon">⚠️</span>
          <p>{{ error() }}</p>
          <button class="btn-retry" (click)="refresh()" data-cy="retry-btn">
            🔄 Try Again
          </button>
        </div>
      }

      @if (!isLoading() && !error()) {
        <!-- Open Tournaments -->
        <section class="tournament-section" data-cy="open-tournaments-section">
          <h2 class="section-title">
            <span class="icon">📝</span>
            Open for Registration
            <span class="count">({{ openTournaments().length }})</span>
          </h2>
          
          @if (openTournaments().length === 0) {
            <div class="empty-state">
              <p>No tournaments open for registration right now.</p>
            </div>
          } @else {
            <div class="tournament-grid">
              @for (tournament of openTournaments(); track tournament.id) {
                <app-tournament-card
                  [tournament]="tournament"
                  (register)="onRegister($event)"
                  (viewDetails)="onViewDetails($event)"
                  data-cy="tournament-card"
                />
              }
            </div>
          }
        </section>

        <!-- Running Tournaments -->
        <section class="tournament-section" data-cy="running-tournaments-section">
          <h2 class="section-title">
            <span class="icon">🎮</span>
            In Progress
            <span class="count">({{ runningTournaments().length }})</span>
          </h2>
          
          @if (runningTournaments().length === 0) {
            <div class="empty-state">
              <p>No tournaments currently running.</p>
            </div>
          } @else {
            <div class="tournament-grid">
              @for (tournament of runningTournaments(); track tournament.id) {
                <app-tournament-card
                  [tournament]="tournament"
                  [showJoin]="false"
                  (viewDetails)="onViewDetails($event)"
                  data-cy="tournament-card-running"
                />
              }
            </div>
          }
        </section>
      }

      <div class="refresh-info">
        <button class="btn-refresh" (click)="refresh()" data-cy="refresh-btn">
          🔄 Refresh
        </button>
        <span class="auto-refresh-note">Auto-refreshes every 30s</span>
      </div>
    </div>
  `,
  styles: [`
    .tournament-list-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
      color: #fff;
      padding: 2rem;
    }

    .list-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .list-header h1 {
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

    .loading-state, .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 4rem;
      text-align: center;
    }

    .spinner {
      width: 48px;
      height: 48px;
      border: 4px solid rgba(255, 255, 255, 0.1);
      border-top-color: #ffd700;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error-state {
      color: #fca5a5;
    }

    .error-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .btn-retry {
      margin-top: 1rem;
      padding: 0.75rem 1.5rem;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 0.5rem;
      color: #fff;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-retry:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    .tournament-section {
      margin-bottom: 3rem;
      max-width: 1200px;
      margin-left: auto;
      margin-right: auto;
    }

    .section-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.5rem;
      margin-bottom: 1.5rem;
      color: #e2e8f0;
    }

    .section-title .icon {
      font-size: 1.5rem;
    }

    .section-title .count {
      font-size: 1rem;
      color: #94a3b8;
      font-weight: normal;
    }

    .empty-state {
      background: rgba(255, 255, 255, 0.05);
      border: 1px dashed rgba(255, 255, 255, 0.2);
      border-radius: 1rem;
      padding: 2rem;
      text-align: center;
      color: #94a3b8;
    }

    .tournament-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: 1.5rem;
    }

    .refresh-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      margin-top: 2rem;
    }

    .btn-refresh {
      padding: 0.5rem 1rem;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 0.25rem;
      color: #fff;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-refresh:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    .auto-refresh-note {
      color: #64748b;
      font-size: 0.875rem;
    }
  `]
})
export class TournamentListComponent implements OnInit, OnDestroy {
  protected readonly store = inject(TournamentStore);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  
  private readonly vm = toSignal(this.store.tournamentListVm$, {
    initialValue: {
      tournaments: [],
      openTournaments: [],
      runningTournaments: [],
      isLoading: true,
      error: null
    }
  });

  
  readonly tournaments = computed(() => this.vm().tournaments);
  readonly openTournaments = computed(() => this.vm().openTournaments);
  readonly runningTournaments = computed(() => this.vm().runningTournaments);
  readonly isLoading = computed(() => this.vm().isLoading);
  readonly error = computed(() => this.vm().error);

  ngOnInit(): void {
    this.store.loadTournaments();
    this.setupAutoRefresh();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupAutoRefresh(): void {
    
    interval(30000).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.store.loadTournaments();
    });
  }

  refresh(): void {
    this.store.loadTournaments();
  }

  onRegister(tournament: TournamentListItem): void {
    this.router.navigate(['/tournament', tournament.id]);
  }

  onViewDetails(tournament: TournamentListItem): void {
    this.router.navigate(['/tournament', tournament.id]);
  }

  trackByTournamentId(index: number, tournament: TournamentListItem): string {
    return tournament.id;
  }
}
