import {
  Component,
  input,
  ChangeDetectionStrategy,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TournamentPlayer } from '../../model/tournament';

@Component({
  selector: 'app-tournament-leaderboard',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="leaderboard"
      [class.compact]="compact()"
      data-cy="tournament-leaderboard"
    >
      <header class="leaderboard-header">
        <h3>🏆 Leaderboard</h3>
        @if (!compact()) {
          <span class="player-count">
            {{ activePlayers().length }}/{{ allPlayers().length }} remaining
          </span>
        }
      </header>

      <!-- Top 3 Podium (only in full mode) -->
      @if (!compact() && topThree().length >= 3) {
        <div class="podium" data-cy="podium">
          <div class="podium-spot second">
            <div class="podium-player">
              <span class="player-icon">{{ topThree()[1].isBot ? '🤖' : '👤' }}</span>
              <span class="player-name">{{ topThree()[1].name }}</span>
              <span class="player-chips"><span class="dollar">$</span>{{ topThree()[1].chips | number }}</span>
            </div>
            <div class="podium-base">🥈</div>
          </div>

          <div class="podium-spot first">
            <div class="podium-player">
              <span class="player-icon">{{ topThree()[0].isBot ? '🤖' : '👤' }}</span>
              <span class="player-name">{{ topThree()[0].name }}</span>
              <span class="player-chips"><span class="dollar">$</span>{{ topThree()[0].chips | number }}</span>
            </div>
            <div class="podium-base">🥇</div>
          </div>

          <div class="podium-spot third">
            <div class="podium-player">
              <span class="player-icon">{{ topThree()[2].isBot ? '🤖' : '👤' }}</span>
              <span class="player-name">{{ topThree()[2].name }}</span>
              <span class="player-chips"><span class="dollar">$</span>{{ topThree()[2].chips | number }}</span>
            </div>
            <div class="podium-base">🥉</div>
          </div>
        </div>
      }

      <!-- Full List -->
      <div class="player-list" [class.scrollable]="compact()">
        @for (player of displayedPlayers(); track player.id; let i = $index) {
          <div
            class="player-row"
            [class.me]="player.id === myPlayerId()"
            [class.eliminated]="player.isEliminated"
            [class.top-three]="i < 3 && !player.isEliminated"
            [attr.data-cy]="'leaderboard-player-' + i"
          >
            <span class="rank">
              @if (i === 0 && !player.isEliminated) {
                🥇
              } @else if (i === 1 && !player.isEliminated) {
                🥈
              } @else if (i === 2 && !player.isEliminated) {
                🥉
              } @else {
                {{ i + 1 }}
              }
            </span>

            <span class="player-icon">{{ player.isBot ? '🤖' : '👤' }}</span>

            <span class="player-name">
              {{ player.name }}
              @if (player.id === myPlayerId()) {
                <span class="you-badge">You</span>
              }
            </span>

            <span class="player-chips" [class.eliminated]="player.isEliminated">
              @if (player.isEliminated) {
                <span class="eliminated-text">Out</span>
              } @else {
                <span class="dollar">$</span>{{ player.chips | number }}
              }
            </span>
          </div>
        }
      </div>

      <!-- Stats Section (only in full mode) -->
      @if (!compact()) {
        <div class="stats-section" data-cy="leaderboard-stats">
          <div class="stat-row">
            <span class="stat-label">Largest Stack</span>
            <span class="stat-value"><span class="dollar">$</span>{{ largestStack() | number }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">Smallest Stack</span>
            <span class="stat-value"><span class="dollar">$</span>{{ smallestStack() | number }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">Average Stack</span>
            <span class="stat-value"><span class="dollar">$</span>{{ averageStack() | number }}</span>
          </div>
        </div>
      }

      <!-- My Position Highlight (compact mode) -->
      @if (compact() && myPosition() !== null && myPosition()! > 5) {
        <div class="my-position-highlight">
          <span class="divider">⋯</span>
          <div class="player-row me">
            <span class="rank">{{ myPosition() }}</span>
            <span class="player-icon">👤</span>
            <span class="player-name">
              {{ myPlayerData()?.name ?? 'You' }}
              <span class="you-badge">You</span>
            </span>
            <span class="player-chips"><span class="dollar">$</span>{{ myPlayerData()?.chips ?? 0 | number }}</span>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .leaderboard {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
      padding: 1rem;
      color: #fff;
    }

    .leaderboard.compact {
      padding: 0.75rem;
    }

    .leaderboard-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1rem;
    }

    .leaderboard-header h3 {
      font-size: 1rem;
      margin: 0;
      color: #ffd700;
    }

    .player-count {
      font-size: 0.75rem;
      color: #94a3b8;
    }

    .podium {
      display: flex;
      align-items: flex-end;
      justify-content: center;
      gap: 0.5rem;
      margin-bottom: 1.5rem;
      padding: 1rem;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 0.5rem;
    }

    .podium-spot {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }

    .podium-spot.first {
      order: 2;
    }

    .podium-spot.second {
      order: 1;
    }

    .podium-spot.third {
      order: 3;
    }

    .podium-player {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.25rem;
      padding: 0.5rem;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 0.5rem;
      min-width: 80px;
    }

    .podium-player .player-icon {
      font-size: 1.5rem;
    }

    .podium-player .player-name {
      font-size: 0.75rem;
      font-weight: 600;
      text-align: center;
      max-width: 70px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .podium-player .player-chips {
      font-size: 0.875rem;
      color: #fbbf24;
      font-weight: 600;
    }

    .podium-base {
      font-size: 1.5rem;
      padding: 0.5rem 1rem;
      background: rgba(0, 0, 0, 0.3);
      border-radius: 0.25rem;
    }

    .podium-spot.first .podium-base {
      padding: 0.75rem 1.25rem;
    }

    .player-list {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .player-list.scrollable {
      max-height: 400px;
      overflow-y: auto;
    }

    .player-row {
      display: grid;
      grid-template-columns: 2rem 1.5rem 1fr auto;
      gap: 0.5rem;
      align-items: center;
      padding: 0.5rem 0.75rem;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 0.375rem;
      transition: all 0.2s ease;
    }

    .player-row:hover {
      background: rgba(0, 0, 0, 0.3);
    }

    .player-row.me {
      background: rgba(16, 185, 129, 0.2);
      border: 1px solid rgba(16, 185, 129, 0.3);
    }

    .player-row.eliminated {
      opacity: 0.5;
    }

    .player-row.top-three {
      background: rgba(251, 191, 36, 0.1);
    }

    .rank {
      font-weight: 600;
      text-align: center;
      font-size: 0.875rem;
    }

    .player-icon {
      font-size: 1rem;
    }

    .player-name {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .you-badge {
      padding: 0.125rem 0.375rem;
      background: rgba(16, 185, 129, 0.3);
      color: #34d399;
      border-radius: 9999px;
      font-size: 0.625rem;
      font-weight: 600;
    }

    .player-chips {
      font-size: 0.875rem;
      font-weight: 600;
      color: #e2e8f0;
      text-align: right;
    }

    .player-chips.eliminated {
      color: #ef4444;
    }

    .eliminated-text {
      font-size: 0.75rem;
      text-transform: uppercase;
    }

    .stats-section {
      margin-top: 1rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    .stat-row {
      display: flex;
      justify-content: space-between;
      padding: 0.375rem 0;
      font-size: 0.875rem;
    }

    .stat-label {
      color: #94a3b8;
    }

    .stat-value {
      font-weight: 600;
      color: #e2e8f0;
    }

    .my-position-highlight {
      margin-top: 0.5rem;
      padding-top: 0.5rem;
    }

    .divider {
      display: block;
      text-align: center;
      color: #64748b;
      font-size: 1rem;
      margin-bottom: 0.25rem;
    }

    /* Scrollbar styling */
    .player-list.scrollable::-webkit-scrollbar {
      width: 6px;
    }

    .player-list.scrollable::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 3px;
    }

    .player-list.scrollable::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.2);
      border-radius: 3px;
    }

    .player-list.scrollable::-webkit-scrollbar-thumb:hover {
      background: rgba(255, 255, 255, 0.3);
    }
  `]
})
export class TournamentLeaderboardComponent {
  players = input<TournamentPlayer[]>([]);
  myPlayerId = input<string>('');
  compact = input<boolean>(false);


  readonly allPlayers = computed(() => {
    return [...this.players()].sort((a, b) => {

      if (a.isEliminated && !b.isEliminated) return 1;
      if (!a.isEliminated && b.isEliminated) return -1;

      return (b.chips ?? 0) - (a.chips ?? 0);
    });
  });

  readonly activePlayers = computed(() =>
    this.allPlayers().filter(p => !p.isEliminated)
  );

  readonly displayedPlayers = computed(() => {
    if (this.compact()) {
      return this.allPlayers().slice(0, 10);
    }
    return this.allPlayers();
  });

  readonly topThree = computed(() =>
    this.activePlayers().slice(0, 3)
  );

  readonly myPosition = computed(() => {
    const index = this.activePlayers().findIndex(p => p.id === this.myPlayerId());
    return index >= 0 ? index + 1 : null;
  });

  readonly myPlayerData = computed(() =>
    this.players().find(p => p.id === this.myPlayerId())
  );

  readonly largestStack = computed(() => {
    const active = this.activePlayers();
    if (active.length === 0) return 0;
    const chips = active.map(p => p.chips ?? 0);
    return chips.length > 0 ? Math.max(...chips) : 0;
  });

  readonly smallestStack = computed(() => {
    const active = this.activePlayers();
    if (active.length === 0) return 0;
    const chips = active.map(p => p.chips ?? 0);
    return chips.length > 0 ? Math.min(...chips) : 0;
  });

  readonly averageStack = computed(() => {
    const active = this.activePlayers();
    if (active.length === 0) return 0;
    const total = active.reduce((sum, p) => sum + (p.chips ?? 0), 0);
    return Math.round(total / active.length);
  });
}
