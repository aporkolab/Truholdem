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
import { Subject, takeUntil, filter, tap } from 'rxjs';

import { TournamentStore } from '../../store/tournament.store';
import { GameStore } from '../../store/game.store';
import { BlindTimerComponent } from '../blind-timer/blind-timer.component';
import { TournamentLeaderboardComponent } from '../tournament-leaderboard/tournament-leaderboard.component';
import { RaiseInputComponent } from '../../raise-input/raise-input.component';
import { Player } from '../../model/player';
import { UiStateService } from '../../services/ui-state.service';
import { SoundService } from '../../services/sound.service';

@Component({
  selector: 'app-tournament-table',
  standalone: true,
  imports: [
    CommonModule, 
    BlindTimerComponent, 
    TournamentLeaderboardComponent,
    RaiseInputComponent
  ],
  providers: [TournamentStore, GameStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tournament-table-container" data-cy="tournament-table">
      <!-- Tournament Header Bar -->
      <header class="tournament-header">
        <div class="header-left">
          <button class="btn-back" (click)="exitTournament()" data-cy="exit-btn">
            ← Exit
          </button>
          <h1 class="tournament-name">{{ tournamentName() }}</h1>
        </div>
        
        <div class="header-center">
          <app-blind-timer
            [currentBlinds]="currentBlinds()"
            [nextBlinds]="nextBlinds()"
            [levelEndTime]="levelEndTime()"
            [levelDurationMinutes]="levelDurationMinutes()"
            [isOnBreak]="isOnBreak()"
          />
        </div>

        <div class="header-right">
          <div class="tournament-stats">
            <div class="stat-item">
              <span class="stat-label">Players</span>
              <span class="stat-value">{{ remainingPlayers() }}/{{ totalPlayers() }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Avg Stack</span>
              <span class="stat-value"><span class="dollar">$</span>{{ averageStack() | number }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Your Rank</span>
              <span class="stat-value rank">#{{ myRank() ?? '-' }}</span>
            </div>
          </div>
        </div>
      </header>

      <!-- Main Game Area -->
      <main class="game-area">
        @if (isEliminated()) {
          <div class="eliminated-overlay" data-cy="eliminated-overlay">
            <div class="eliminated-content">
              <span class="eliminated-icon">💀</span>
              <h2>You've Been Eliminated!</h2>
              <p>You finished in position #{{ myFinishPosition() }}</p>
              @if (myPrizeMoney() > 0) {
                <p class="prize-won">Prize: 💰 <span class="dollar">$</span>{{ myPrizeMoney() | number }}</p>
              }
              <div class="eliminated-actions">
                <button class="btn-watch" (click)="watchTournament()">
                  👁️ Watch Tournament
                </button>
                <button class="btn-exit" (click)="exitTournament()">
                  🚪 Exit
                </button>
              </div>
            </div>
          </div>
        }

        @if (isOnBreak()) {
          <div class="break-overlay" data-cy="break-overlay">
            <div class="break-content">
              <span class="break-icon">☕</span>
              <h2>Tournament Break</h2>
              <p>Next level starts in {{ breakTimeRemaining() }}</p>
            </div>
          </div>
        }

        <!-- Poker Table -->
        <div class="poker-table" data-cy="poker-table">
          <!-- Community Cards -->
          <div class="community-cards" data-cy="community-cards">
            @for (card of communityCards(); track $index) {
              <div class="card community-card">
                <img [src]="getCardImage(card)" [alt]="card.value + ' of ' + card.suit">
              </div>
            }
            @for (i of emptyCardSlots(); track i) {
              <div class="card card-placeholder"></div>
            }
          </div>

          <!-- Pot Display -->
          <div class="pot-display" data-cy="pot-display">
            <span class="pot-label">Pot</span>
            <span class="pot-value"><span class="dollar">$</span>{{ potSize() | number }}</span>
          </div>

          <!-- Player Positions -->
          <div class="players-ring">
            @for (player of tablePlayers(); track player.id; let i = $index) {
              <div 
                class="player-seat"
                [class.active]="isCurrentPlayer(player)"
                [class.folded]="player.folded"
                [class.all-in]="player.isAllIn"
                [class.me]="isMyPlayer(player)"
                [style.--seat-index]="i"
                [attr.data-cy]="'player-seat-' + i"
              >
                <div class="player-avatar">
                  {{ player.isBot ? '🤖' : '👤' }}
                </div>
                <div class="player-info">
                  <span class="player-name">{{ player.name }}</span>
                  <span class="player-chips"><span class="dollar">$</span>{{ player.chips | number }}</span>
                </div>
                @if (player.betAmount > 0) {
                  <div class="player-bet">
                    <span class="dollar">$</span>{{ player.betAmount | number }}
                  </div>
                }
                @if (isDealer(i)) {
                  <div class="dealer-button">D</div>
                }
                @if (player.folded) {
                  <div class="status-badge folded">Folded</div>
                }
                @if (player.isAllIn) {
                  <div class="status-badge all-in">All-In</div>
                }
                
                <!-- Player Cards -->
                <div class="player-cards">
                  @if (isMyPlayer(player) || isShowdown()) {
                    @for (card of player.hand; track $index) {
                      <div class="card player-card">
                        <img [src]="getCardImage(card)" [alt]="card.value + ' of ' + card.suit">
                      </div>
                    }
                  } @else {
                    @for (card of player.hand; track $index) {
                      <div class="card player-card back">
                        <img [src]="getCardBackImage()" alt="Card back">
                      </div>
                    }
                  }
                </div>
              </div>
            }
          </div>
        </div>

        <!-- Action Buttons -->
        @if (canPlayerAct() && !isEliminated()) {
          <div class="action-buttons" data-cy="action-buttons">
            <button 
              class="btn-action fold"
              (click)="fold()"
              data-cy="fold-btn"
            >
              🃏 Fold
            </button>
            
            @if (canCheck()) {
              <button 
                class="btn-action check"
                (click)="check()"
                data-cy="check-btn"
              >
                ✓ Check
              </button>
            }
            
            @if (canCall()) {
              <button 
                class="btn-action call"
                (click)="call()"
                data-cy="call-btn"
              >
                📞 Call <span class="dollar">$</span>{{ callAmount() | number }}
              </button>
            }
            
            <button 
              class="btn-action raise"
              (click)="openRaiseModal()"
              data-cy="raise-btn"
            >
              ⬆️ Raise
            </button>
            
            <button 
              class="btn-action all-in"
              (click)="allIn()"
              data-cy="all-in-btn"
            >
              💰 All-In
            </button>
          </div>
        }
      </main>

      <!-- Side Panel: Mini Leaderboard -->
      <aside class="side-panel">
        <app-tournament-leaderboard
          [players]="tournamentPlayers()"
          [myPlayerId]="myPlayerId()"
          [compact]="true"
        />
      </aside>

      <!-- Raise Modal -->
      @if (showRaiseModal()) {
        <app-raise-input
          [minAmount]="minRaiseAmount()"
          [maxAmount]="maxRaiseAmount()"
          [currentBet]="currentBet()"
          (raiseConfirmed)="onRaiseConfirm($event)"
          (raiseCancel)="closeRaiseModal()"
        />
      }
    </div>
  `,
  styles: [`
    .tournament-table-container {
      display: grid;
      grid-template-rows: auto 1fr;
      grid-template-columns: 1fr 280px;
      min-height: 100vh;
      background: linear-gradient(135deg, #0d1b2a 0%, #1b2838 50%, #0d1b2a 100%);
      color: #fff;
    }

    .tournament-header {
      grid-column: 1 / -1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 2rem;
      background: rgba(0, 0, 0, 0.3);
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .header-left, .header-right {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .btn-back {
      padding: 0.5rem 1rem;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 0.5rem;
      color: #94a3b8;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-back:hover {
      background: rgba(255, 255, 255, 0.15);
      color: #fff;
    }

    .tournament-name {
      font-size: 1.25rem;
      font-weight: 600;
      margin: 0;
      color: #ffd700;
    }

    .header-center {
      flex-shrink: 0;
    }

    .tournament-stats {
      display: flex;
      gap: 1.5rem;
    }

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
    }

    .stat-label {
      font-size: 0.75rem;
      color: #94a3b8;
      text-transform: uppercase;
    }

    .stat-value {
      font-size: 1.125rem;
      font-weight: 600;
      color: #fff;
    }

    .stat-value.rank {
      color: #fbbf24;
    }

    .game-area {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
    }

    .eliminated-overlay, .break-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.85);
      z-index: 100;
    }

    .eliminated-content, .break-content {
      text-align: center;
      padding: 3rem;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
    }

    .eliminated-icon, .break-icon {
      font-size: 4rem;
      display: block;
      margin-bottom: 1rem;
    }

    .eliminated-content h2, .break-content h2 {
      font-size: 2rem;
      margin: 0 0 1rem;
    }

    .prize-won {
      font-size: 1.5rem;
      color: #34d399;
      font-weight: 600;
    }

    .eliminated-actions {
      display: flex;
      gap: 1rem;
      margin-top: 2rem;
    }

    .btn-watch, .btn-exit {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 0.5rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-watch {
      background: rgba(59, 130, 246, 0.2);
      color: #60a5fa;
      border: 1px solid rgba(59, 130, 246, 0.3);
    }

    .btn-exit {
      background: rgba(239, 68, 68, 0.2);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
    }

    .poker-table {
      position: relative;
      width: 700px;
      height: 400px;
      background: radial-gradient(ellipse at center, #1e5128 0%, #145a32 50%, #0d3d22 100%);
      border: 12px solid #5a3825;
      border-radius: 200px;
      box-shadow: 
        inset 0 0 50px rgba(0, 0, 0, 0.5),
        0 10px 30px rgba(0, 0, 0, 0.5);
    }

    .community-cards {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -80%);
      display: flex;
      gap: 0.5rem;
    }

    .card {
      width: 60px;
      height: 84px;
      border-radius: 6px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    }

    .card img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .card-placeholder {
      background: rgba(255, 255, 255, 0.05);
      border: 2px dashed rgba(255, 255, 255, 0.2);
    }

    .pot-display {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, 20%);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
      padding: 0.5rem 1rem;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 0.5rem;
    }

    .pot-label {
      font-size: 0.75rem;
      color: #94a3b8;
      text-transform: uppercase;
    }

    .pot-value {
      font-size: 1.25rem;
      font-weight: 700;
      color: #fbbf24;
    }

    .players-ring {
      position: absolute;
      inset: -60px;
    }

    .player-seat {
      --seat-angle: calc(var(--seat-index, 0) * (360deg / 6));
      position: absolute;
      top: 50%;
      left: 50%;
      transform: 
        translate(-50%, -50%)
        rotate(var(--seat-angle))
        translateY(-200px)
        rotate(calc(-1 * var(--seat-angle)));
      
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
      padding: 0.5rem;
      background: rgba(0, 0, 0, 0.6);
      border: 2px solid rgba(255, 255, 255, 0.2);
      border-radius: 0.75rem;
      min-width: 100px;
      transition: all 0.3s ease;
    }

    .player-seat.active {
      border-color: #fbbf24;
      box-shadow: 0 0 20px rgba(251, 191, 36, 0.4);
    }

    .player-seat.me {
      border-color: #34d399;
    }

    .player-seat.folded {
      opacity: 0.5;
    }

    .player-avatar {
      font-size: 1.5rem;
    }

    .player-info {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .player-name {
      font-size: 0.875rem;
      font-weight: 600;
    }

    .player-chips {
      font-size: 0.75rem;
      color: #94a3b8;
    }

    .player-bet {
      padding: 0.125rem 0.5rem;
      background: rgba(251, 191, 36, 0.2);
      color: #fbbf24;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
    }

    .dealer-button {
      position: absolute;
      top: -10px;
      right: -10px;
      width: 24px;
      height: 24px;
      background: #fff;
      color: #000;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.75rem;
      font-weight: 700;
    }

    .status-badge {
      position: absolute;
      bottom: -8px;
      padding: 0.125rem 0.5rem;
      border-radius: 9999px;
      font-size: 0.625rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-badge.folded {
      background: rgba(239, 68, 68, 0.3);
      color: #f87171;
    }

    .status-badge.all-in {
      background: rgba(245, 158, 11, 0.3);
      color: #fbbf24;
    }

    .player-cards {
      display: flex;
      gap: 0.25rem;
      margin-top: 0.25rem;
    }

    .player-card {
      width: 40px;
      height: 56px;
    }

    .action-buttons {
      display: flex;
      gap: 0.75rem;
      margin-top: 2rem;
    }

    .btn-action {
      padding: 0.75rem 1.25rem;
      border: none;
      border-radius: 0.5rem;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-action.fold {
      background: rgba(239, 68, 68, 0.2);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
    }

    .btn-action.check {
      background: rgba(59, 130, 246, 0.2);
      color: #60a5fa;
      border: 1px solid rgba(59, 130, 246, 0.3);
    }

    .btn-action.call {
      background: rgba(34, 197, 94, 0.2);
      color: #34d399;
      border: 1px solid rgba(34, 197, 94, 0.3);
    }

    .btn-action.raise {
      background: rgba(245, 158, 11, 0.2);
      color: #fbbf24;
      border: 1px solid rgba(245, 158, 11, 0.3);
    }

    .btn-action.all-in {
      background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
      color: #000;
    }

    .btn-action:hover {
      transform: translateY(-2px);
    }

    .side-panel {
      background: rgba(0, 0, 0, 0.3);
      border-left: 1px solid rgba(255, 255, 255, 0.1);
      padding: 1rem;
      overflow-y: auto;
    }
  `]
})
export class TournamentTableComponent implements OnInit, OnDestroy {
  protected readonly tournamentStore = inject(TournamentStore);
  protected readonly gameStore = inject(GameStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly uiState = inject(UiStateService);
  private readonly soundService = inject(SoundService);

  private readonly destroy$ = new Subject<void>();

  
  private readonly tournamentVm = toSignal(this.tournamentStore.tournamentTableVm$, {
    initialValue: {
      tournament: null,
      table: null,
      myPlayer: null,
      currentBlinds: null,
      nextBlinds: null,
      timeToNextLevel: 0,
      formattedTimeRemaining: '00:00',
      remainingPlayers: 0,
      averageStack: 0,
      myRank: null,
      totalPlayers: 0,
      isOnBreak: false,
      isFinalTable: false,
      isEliminated: false,
      isLoading: true,
      error: null
    }
  });

  
  private readonly gameVm = toSignal(this.gameStore.vm$, {
    initialValue: {
      game: null,
      currentPlayer: null,
      humanPlayer: undefined,
      isLoading: true,
      error: null,
      isHumanTurn: false,
      canCheck: false,
      canCall: false,
      callAmount: 0,
      minRaiseAmount: 0,
      maxRaiseAmount: 0,
      potSize: 0,
      phase: 'PRE_FLOP',
      phaseDisplayName: 'Pre-Flop',
      communityCards: [],
      isGameFinished: false,
      activePlayers: [],
      lastAction: null,
      processingBots: false,
      canPlayerAct: false,
      dealerPosition: 0,
      winnerName: undefined,
      winningHandDescription: undefined
    }
  });

  
  readonly tournamentName = computed(() => this.tournamentVm().tournament?.name ?? 'Tournament');
  readonly currentBlinds = computed(() => this.tournamentVm().currentBlinds);
  readonly nextBlinds = computed(() => this.tournamentVm().nextBlinds);
  readonly levelEndTime = computed(() => this.tournamentVm().tournament?.levelEndTime ?? 0);
  readonly levelDurationMinutes = computed(() => 
    this.tournamentVm().tournament?.config.levelDurationMinutes ?? 15
  );
  readonly isOnBreak = computed(() => this.tournamentVm().isOnBreak);
  readonly remainingPlayers = computed(() => this.tournamentVm().remainingPlayers);
  readonly totalPlayers = computed(() => this.tournamentVm().totalPlayers);
  readonly averageStack = computed(() => this.tournamentVm().averageStack);
  readonly myRank = computed(() => this.tournamentVm().myRank);
  readonly isEliminated = computed(() => this.tournamentVm().isEliminated);
  readonly myPlayerId = computed(() => this.tournamentVm().myPlayer?.id ?? '');
  readonly myFinishPosition = computed(() => this.tournamentVm().myPlayer?.finishPosition ?? 0);
  readonly myPrizeMoney = computed(() => this.tournamentVm().myPlayer?.prizeMoney ?? 0);
  readonly tournamentPlayers = computed(() => 
    this.tournamentVm().tournament?.registeredPlayers ?? []
  );
  readonly breakTimeRemaining = computed(() => this.tournamentVm().formattedTimeRemaining);

  
  readonly tablePlayers = computed(() => this.tournamentVm().table?.players ?? []);
  readonly communityCards = computed(() => this.gameVm().communityCards);
  readonly potSize = computed(() => this.gameVm().potSize);
  readonly canPlayerAct = computed(() => this.gameVm().canPlayerAct);
  readonly canCheck = computed(() => this.gameVm().canCheck);
  readonly canCall = computed(() => this.gameVm().canCall);
  readonly callAmount = computed(() => this.gameVm().callAmount);
  readonly minRaiseAmount = computed(() => this.gameVm().minRaiseAmount);
  readonly maxRaiseAmount = computed(() => this.gameVm().maxRaiseAmount);
  readonly currentBet = computed(() => this.gameVm().game?.currentBet ?? 0);
  readonly dealerPosition = computed(() => this.gameVm().dealerPosition);
  readonly isShowdown = computed(() => this.gameVm().phase === 'SHOWDOWN');
  readonly humanPlayer = computed(() => this.gameVm().humanPlayer);

  readonly showRaiseModal = this.uiState.showRaiseModal;

  readonly emptyCardSlots = computed(() => {
    const count = Math.max(0, 5 - this.communityCards().length);
    return Array(count).fill(0).map((_, i) => i);
  });

  ngOnInit(): void {
    const tournamentId = this.route.snapshot.paramMap.get('id');
    if (tournamentId) {
      this.tournamentStore.loadTournament(tournamentId);
      this.tournamentStore.subscribeTournamentUpdates(tournamentId);
    }

    this.setupSideEffects();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSideEffects(): void {
    
    this.gameStore.currentBot$.pipe(
      takeUntil(this.destroy$),
      filter(bot => bot !== null),
      tap(() => {
        setTimeout(() => this.gameStore.processBots(), 800);
      })
    ).subscribe();
  }

  
  isCurrentPlayer(player: Player): boolean {
    return this.gameStore.isPlayerTurn(player.id);
  }

  isMyPlayer(player: Player): boolean {
    return player.id === this.myPlayerId();
  }

  isDealer(seatPosition: number): boolean {
    return this.dealerPosition() === seatPosition;
  }

  
  fold(): void {
    const player = this.humanPlayer();
    if (player) {
      this.gameStore.playerAction({ playerId: player.id, action: 'FOLD' });
    }
  }

  check(): void {
    const player = this.humanPlayer();
    if (player) {
      this.gameStore.playerAction({ playerId: player.id, action: 'CHECK' });
    }
  }

  call(): void {
    const player = this.humanPlayer();
    if (player) {
      this.gameStore.playerAction({ playerId: player.id, action: 'CALL' });
    }
  }

  allIn(): void {
    const player = this.humanPlayer();
    if (player) {
      const totalAmount = player.chips + (player.betAmount ?? 0);
      this.gameStore.playerAction({ playerId: player.id, action: 'ALL_IN', amount: totalAmount });
    }
  }

  openRaiseModal(): void {
    this.uiState.openRaiseModal();
  }

  closeRaiseModal(): void {
    this.uiState.closeRaiseModal();
  }

  onRaiseConfirm(amount: number): void {
    const player = this.humanPlayer();
    if (player) {
      this.gameStore.playerAction({ playerId: player.id, action: 'RAISE', amount });
    }
    this.closeRaiseModal();
  }

  
  watchTournament(): void {
    // Placeholder for spectator mode - to be implemented
  }

  exitTournament(): void {
    this.router.navigate(['/tournaments']);
  }

  
  getCardImage(card: { suit: string; value: string }): string {
    const suitName = card.suit.toLowerCase();
    const valueName = this.getCardValueName(card.value);
    return `assets/cards/${valueName}_of_${suitName}.png`;
  }

  getCardBackImage(): string {
    return 'assets/cards/back.png';
  }

  private getCardValueName(value: string): string {
    const valueNames: Record<string, string> = {
      'TWO': '2', 'THREE': '3', 'FOUR': '4', 'FIVE': '5',
      'SIX': '6', 'SEVEN': '7', 'EIGHT': '8', 'NINE': '9', 'TEN': '10',
      'JACK': 'jack', 'QUEEN': 'queen', 'KING': 'king', 'ACE': 'ace'
    };
    return valueNames[value] || value.toLowerCase();
  }
}
