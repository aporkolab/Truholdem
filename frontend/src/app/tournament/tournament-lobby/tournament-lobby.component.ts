import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';

import { TournamentStore } from '../../store/tournament.store';
import { PrizeStructure } from '../../model/tournament';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-tournament-lobby',
  standalone: true,
  imports: [CommonModule],
  providers: [TournamentStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tournament-lobby" data-cy="tournament-lobby">
      @if (isLoading()) {
        <div class="loading-state" data-cy="loading">
          <div class="spinner"></div>
          <p>Loading tournament...</p>
        </div>
      }

      @if (error()) {
        <div class="error-state" role="alert" data-cy="error-message">
          <span class="error-icon">⚠️</span>
          <p>{{ error() }}</p>
          <button class="btn-back" (click)="goBack()">← Back to Tournaments</button>
        </div>
      }

      @if (!tournament() && !isLoading() && !error()) {
        <div class="not-found-state" data-cy="not-found">
          <span class="error-icon">🔍</span>
          <p>Tournament not found</p>
          <button class="btn-back" (click)="goBack()">← Back to Tournaments</button>
        </div>
      }

      @if (tournament() && !isLoading()) {
        <header class="lobby-header">
          <button class="btn-back-link" (click)="goBack()" data-cy="back-btn">← Back</button>
          <h1 data-cy="tournament-name">{{ tournament()!.name }}</h1>
          <div class="status-badge" [class]="tournament()!.status.toLowerCase()" data-cy="status-badge">
            @if (tournament()!.status === 'REGISTERING') {
              Registration Open
            } @else if (tournament()!.status === 'RUNNING') {
              In Progress
            } @else if (tournament()!.status === 'FINISHED') {
              Finished
            } @else {
              {{ tournament()!.status }}
            }
          </div>
        </header>

        <div class="lobby-content">
          <!-- Tournament Info Panel -->
          <section class="info-panel" data-cy="info-panel">
            <h2>Tournament Info</h2>
            
            <div class="info-grid">
              <div class="info-item">
                <span class="label">Buy-In</span>
                <span class="value gold" data-cy="buy-in">💰 {{ tournament()!.config.buyIn | number }}</span>
              </div>

              <div class="info-item">
                <span class="label">Starting Stack</span>
                <span class="value" data-cy="starting-chips">🎰 {{ tournament()!.config.startingChips | number }}</span>
              </div>

              <div class="info-item">
                <span class="label">Prize Pool</span>
                <span class="value green" data-cy="prize-pool">🏆 {{ tournament()!.prizePool | number }}</span>
              </div>

              <div class="info-item">
                <span class="label">Players</span>
                <span class="value" data-cy="player-count">
                  👥 {{ tournament()!.registeredPlayers.length }}/{{ tournament()!.config.maxPlayers }}
                </span>
              </div>

              <div class="info-item">
                <span class="label">Level Duration</span>
                <span class="value" data-cy="level-duration">⏱️ {{ tournament()!.config.levelDurationMinutes }} min</span>
              </div>

              <div class="info-item">
                <span class="label">Starting Blinds</span>
                <span class="value">
                  {{ tournament()!.config.blindLevels[0].smallBlind }}/{{ tournament()!.config.blindLevels[0].bigBlind }}
                </span>
              </div>

              @if (spotsRemaining() > 0) {
                <div class="info-item">
                  <span class="label">Spots Remaining</span>
                  <span class="value" data-cy="spots-remaining">{{ spotsRemaining() }}</span>
                </div>
              }
            </div>

            <!-- Prize Structure -->
            <div class="prize-structure" data-cy="prize-structure">
              <h3>Prize Structure</h3>
              <div class="prize-list">
                @for (prize of tournament()!.config.prizeStructure; track prize.position) {
                  <div class="prize-item">
                    <span class="position">{{ prize.position }}{{ getOrdinalSuffix(prize.position) }}</span>
                    <span class="percentage">{{ prize.percentage }}%</span>
                    <span class="amount" [attr.data-cy]="'prize-' + prize.position + getOrdinalSuffix(prize.position)">{{ calculatePrize(prize) | number }}</span>
                  </div>
                }
              </div>
            </div>
          </section>

          <!-- Players List Panel -->
          <section class="players-panel" data-cy="players-panel">
            <h2>
              Registered Players 
              <span class="count">({{ tournament()!.registeredPlayers.length }})</span>
            </h2>
            
            <div class="players-list">
              @for (player of registeredPlayers(); track player.id; let i = $index) {
                <div
                  class="player-row player-item"
                  [class.me]="player.id === myPlayer()?.id"
                  [attr.data-cy]="'player-' + i"
                >
                  <span class="player-index">{{ i + 1 }}</span>
                  <span class="player-icon">{{ player.isBot ? '🤖' : '👤' }}</span>
                  <span class="player-name">{{ player.name }}</span>
                  @if (player.id === myPlayer()?.id) {
                    <span class="you-badge" data-cy="you-badge">You</span>
                  }
                </div>
              } @empty {
                <div class="empty-players" data-cy="no-players">
                  <p>No players registered yet. Be the first!</p>
                </div>
              }
            </div>

            <!-- Registration Action -->
            <div class="registration-action">
              @if (!isRegistered() && canRegister()) {
                <button 
                  class="btn-register"
                  (click)="register()"
                  [disabled]="isRegistering()"
                  data-cy="register-btn"
                >
                  @if (isRegistering()) {
                    <span class="spinner-small"></span> Registering...
                  } @else {
                    📝 Register ({{ tournament()!.config.buyIn | number }})
                  }
                </button>
              }

              @if (isRegistered() && canUnregister()) {
                <div class="registered-info">
                  <span class="registered-badge">✅ You are registered!</span>
                  <button 
                    class="btn-unregister"
                    (click)="unregister()"
                    data-cy="unregister-btn"
                  >
                    ❌ Unregister
                  </button>
                </div>
              }

              @if (isRegistered() && tournament()!.status === 'RUNNING') {
                <button
                  class="btn-play"
                  (click)="goToTable()"
                  data-cy="go-to-table-btn"
                >
                  🎮 Go to Table
                </button>
              }

              @if (tournament()!.status === 'FINISHED') {
                <div class="tournament-finished">
                  <p>🏁 This tournament has ended</p>
                </div>
              }

              @if (!canRegister() && spotsRemaining() === 0 && tournament()!.status === 'REGISTERING') {
                <div class="tournament-full" data-cy="tournament-full">
                  <p>❌ Tournament is full</p>
                </div>
              }
            </div>
          </section>

          <!-- Blind Structure Panel -->
          <section class="blinds-panel" data-cy="blinds-panel">
            <h2>Blind Structure</h2>

            <div class="blinds-table" data-cy="blind-structure">
              <div class="blinds-header">
                <span>Level</span>
                <span>Blinds</span>
                <span>Ante</span>
                <span>Duration</span>
              </div>

              @for (level of tournament()!.config.blindLevels; track level.level; let i = $index) {
                <div
                  class="blinds-row"
                  [class.current]="tournament()!.currentLevel === level.level"
                  [class.passed]="tournament()!.currentLevel > level.level"
                  [attr.data-cy]="'blind-level-' + level.level"
                >
                  <span class="level-num">{{ level.level }}</span>
                  <span class="blinds">{{ level.smallBlind }}/{{ level.bigBlind }}</span>
                  <span class="ante">{{ level.ante || '-' }}</span>
                  <span class="duration">{{ level.durationMinutes }}m</span>
                </div>

                <!-- Break indicator -->
                @if (shouldShowBreak(level.level)) {
                  <div class="break-row" data-cy="break-indicator">
                    ☕ Break ({{ tournament()!.config.breakDurationMinutes }} min)
                  </div>
                }
              }
            </div>
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    .tournament-lobby {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
      color: #fff;
      padding: 2rem;
    }

    .loading-state, .error-state, .not-found-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 50vh;
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

    .spinner-small {
      display: inline-block;
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: #fff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .lobby-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 2rem;
      max-width: 1200px;
      margin-left: auto;
      margin-right: auto;
    }

    .btn-back-link {
      background: none;
      border: none;
      color: #94a3b8;
      cursor: pointer;
      font-size: 1rem;
      padding: 0;
    }

    .btn-back-link:hover {
      color: #fff;
    }

    .lobby-header h1 {
      flex: 1;
      font-size: 2rem;
      margin: 0;
      background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .status-badge {
      padding: 0.5rem 1rem;
      border-radius: 9999px;
      font-size: 0.875rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-badge.registering { background: rgba(16, 185, 129, 0.2); color: #34d399; }
    .status-badge.running { background: rgba(59, 130, 246, 0.2); color: #60a5fa; }
    .status-badge.finished { background: rgba(107, 114, 128, 0.2); color: #9ca3af; }

    .lobby-content {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    @media (max-width: 1024px) {
      .lobby-content {
        grid-template-columns: 1fr 1fr;
      }
    }

    @media (max-width: 768px) {
      .lobby-content {
        grid-template-columns: 1fr;
      }
    }

    .info-panel, .players-panel, .blinds-panel {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
      padding: 1.5rem;
    }

    h2 {
      font-size: 1.25rem;
      margin: 0 0 1rem;
      color: #ffd700;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    h2 .count {
      color: #94a3b8;
      font-weight: normal;
    }

    .info-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .info-item .label {
      font-size: 0.75rem;
      color: #94a3b8;
      text-transform: uppercase;
    }

    .info-item .value {
      font-size: 1rem;
      color: #fff;
    }

    .info-item .value.gold { color: #fbbf24; }
    .info-item .value.green { color: #34d399; }

    .prize-structure h3 {
      font-size: 1rem;
      margin: 0 0 0.75rem;
      color: #e2e8f0;
    }

    .prize-list {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .prize-item {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.5rem;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 0.25rem;
    }

    .prize-item .position {
      font-weight: 600;
      min-width: 3rem;
    }

    .prize-item .percentage {
      color: #94a3b8;
      min-width: 3rem;
    }

    .prize-item .amount {
      color: #34d399;
      font-weight: 600;
      margin-left: auto;
    }

    .players-list {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-height: 300px;
      overflow-y: auto;
      margin-bottom: 1rem;
    }

    .player-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.5rem 0.75rem;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 0.5rem;
      transition: background 0.2s;
    }

    .player-row.me {
      background: rgba(16, 185, 129, 0.2);
      border: 1px solid rgba(16, 185, 129, 0.3);
    }

    .player-index {
      width: 1.5rem;
      text-align: center;
      color: #94a3b8;
      font-size: 0.875rem;
    }

    .player-icon {
      font-size: 1.25rem;
    }

    .player-name {
      flex: 1;
    }

    .you-badge {
      padding: 0.125rem 0.5rem;
      background: rgba(16, 185, 129, 0.3);
      color: #34d399;
      border-radius: 9999px;
      font-size: 0.75rem;
    }

    .empty-players {
      text-align: center;
      padding: 2rem;
      color: #94a3b8;
    }

    .registration-action {
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    .btn-register, .btn-play {
      width: 100%;
      padding: 1rem;
      font-size: 1.1rem;
      font-weight: 600;
      border: none;
      border-radius: 0.5rem;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-register {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: #fff;
    }

    .btn-register:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
    }

    .btn-register:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-play {
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
      color: #fff;
    }

    .btn-play:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
    }

    .registered-info {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .registered-badge {
      color: #34d399;
      font-weight: 600;
    }

    .btn-unregister {
      padding: 0.5rem 1rem;
      background: rgba(239, 68, 68, 0.2);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 0.5rem;
      color: #f87171;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-unregister:hover {
      background: rgba(239, 68, 68, 0.3);
    }

    .tournament-finished, .tournament-full {
      text-align: center;
      color: #94a3b8;
    }

    .blinds-table {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      max-height: 400px;
      overflow-y: auto;
    }

    .blinds-header, .blinds-row {
      display: grid;
      grid-template-columns: 3rem 1fr 3rem 4rem;
      gap: 0.5rem;
      padding: 0.5rem 0.75rem;
      font-size: 0.875rem;
    }

    .blinds-header {
      color: #94a3b8;
      text-transform: uppercase;
      font-size: 0.75rem;
      position: sticky;
      top: 0;
      background: rgba(22, 33, 62, 0.95);
    }

    .blinds-row {
      background: rgba(0, 0, 0, 0.2);
      border-radius: 0.25rem;
      transition: all 0.2s;
    }

    .blinds-row.current {
      background: rgba(59, 130, 246, 0.3);
      border: 1px solid rgba(59, 130, 246, 0.5);
    }

    .blinds-row.passed {
      opacity: 0.5;
    }

    .break-row {
      text-align: center;
      padding: 0.5rem;
      background: rgba(245, 158, 11, 0.1);
      color: #fbbf24;
      border-radius: 0.25rem;
      font-size: 0.875rem;
    }
  `]
})
export class TournamentLobbyComponent implements OnInit, OnDestroy {
  protected readonly store = inject(TournamentStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  
  private readonly vm = toSignal(this.store.tournamentLobbyVm$, {
    initialValue: {
      tournament: null,
      registeredPlayers: [],
      canRegister: false,
      isRegistered: false,
      isLoading: true,
      error: null,
      spotsRemaining: 0,
      prizePool: 0
    }
  });

  readonly tournament = computed(() => this.vm().tournament);
  readonly registeredPlayers = computed(() => this.vm().registeredPlayers);
  readonly canRegister = computed(() => this.vm().canRegister);
  readonly isRegistered = computed(() => this.vm().isRegistered);
  readonly isLoading = computed(() => this.vm().isLoading);
  readonly error = computed(() => this.vm().error);
  readonly spotsRemaining = computed(() => this.vm().spotsRemaining);

  readonly myPlayer = toSignal(this.store.myPlayer$, { initialValue: null });
  readonly isRegistering = toSignal(this.store.isRegistering$, { initialValue: false });

  ngOnInit(): void {
    const tournamentId = this.route.snapshot.paramMap.get('id');
    if (tournamentId) {
      this.store.loadTournament(tournamentId);
      this.store.subscribeTournamentUpdates(tournamentId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  register(): void {
    const tournament = this.tournament();
    const user = this.authService.getCurrentUserValue();
    
    if (tournament && user) {
      this.store.registerForTournament({
        tournamentId: tournament.id,
        playerName: user.username
      });
    }
  }

  unregister(): void {
    const tournament = this.tournament();
    if (tournament) {
      this.store.unregisterFromTournament(tournament.id);
    }
  }

  canUnregister(): boolean {
    const status = this.tournament()?.status;
    return status === 'REGISTERING';
  }

  goToTable(): void {
    const tournament = this.tournament();
    if (tournament) {
      this.router.navigate(['/tournament', tournament.id, 'play']);
    }
  }

  goBack(): void {
    this.router.navigate(['/tournaments']);
  }

  getOrdinalSuffix(n: number): string {
    if (n > 3 && n < 21) return 'th';
    switch (n % 10) {
      case 1: return 'st';
      case 2: return 'nd';
      case 3: return 'rd';
      default: return 'th';
    }
  }

  calculatePrize(prize: PrizeStructure): number {
    const pool = this.tournament()?.prizePool ?? 0;
    return Math.floor(pool * (prize.percentage / 100));
  }

  shouldShowBreak(level: number): boolean {
    const config = this.tournament()?.config;
    if (!config || config.breakAfterLevels <= 0) return false;
    return level % config.breakAfterLevels === 0 && 
           level < config.blindLevels.length;
  }
}
