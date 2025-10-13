import {
  Component,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  computed,
  input
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TournamentListItem, TournamentStatus } from '../../model/tournament';

@Component({
  selector: 'app-tournament-card',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article
      class="tournament-card"
      [class.registering]="status() === 'REGISTERING'"
      [class.running]="status() === 'RUNNING' || status() === 'FINAL_TABLE'"
      [class.finished]="status() === 'FINISHED'"
      [attr.data-cy]="'tournament-card-' + tournament().id"
      role="article"
      [attr.aria-label]="'Tournament: ' + tournament().name"
    >
      <!-- Status Badge -->
      <div class="status-badge" [class]="statusClass()">
        {{ statusIcon() }} {{ statusText() }}
      </div>

      <!-- Tournament Name -->
      <header class="card-header">
        <h3 class="tournament-name">{{ tournament().name }}</h3>
      </header>

      <!-- Tournament Info Grid -->
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">Buy-In</span>
          <span class="info-value buy-in">💰 {{ tournament().buyIn | number }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">Starting Stack</span>
          <span class="info-value">🎰 {{ tournament().startingChips | number }}</span>
        </div>

        <div class="info-item">
          <span class="info-label">Players</span>
          <span class="info-value">
            👥 {{ tournament().registeredCount }}/{{ tournament().maxPlayers }}
          </span>
        </div>

        <div class="info-item">
          <span class="info-label">Prize Pool</span>
          <span class="info-value prize">🏆 {{ tournament().prizePool | number }}</span>
        </div>
      </div>

      <!-- Blinds Info (for running tournaments) -->
      @if (isRunning()) {
        <div class="blinds-info">
          <span class="blinds-label">Current Blinds:</span>
          <span class="blinds-value">
            {{ tournament().smallBlind | number }}/{{ tournament().bigBlind | number }}
          </span>
          <span class="level-badge">Level {{ tournament().currentLevel }}</span>
        </div>
      }

      <!-- Action Buttons -->
      <footer class="card-footer">
        @if (showJoin() && canRegister()) {
          <button 
            class="btn-register"
            (click)="onRegister($event)"
            data-cy="register-btn"
          >
            📝 Register
          </button>
        }
        
        <button 
          class="btn-details"
          (click)="onViewDetails($event)"
          data-cy="details-btn"
        >
          👁️ View Details
        </button>
      </footer>

      <!-- Fill indicator -->
      @if (status() === 'REGISTERING') {
        <div class="fill-bar">
          <div 
            class="fill-progress" 
            [style.width.%]="fillPercentage()"
            [class.almost-full]="fillPercentage() >= 80"
          ></div>
        </div>
      }
    </article>
  `,
  styles: [`
    .tournament-card {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
      padding: 1.5rem;
      position: relative;
      overflow: hidden;
      transition: all 0.3s ease;
    }

    .tournament-card:hover {
      transform: translateY(-4px);
      border-color: rgba(255, 215, 0, 0.3);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    }

    .tournament-card.registering {
      border-color: rgba(16, 185, 129, 0.3);
    }

    .tournament-card.running {
      border-color: rgba(59, 130, 246, 0.3);
    }

    .tournament-card.finished {
      opacity: 0.7;
    }

    .status-badge {
      position: absolute;
      top: 1rem;
      right: 1rem;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-badge.registering {
      background: rgba(16, 185, 129, 0.2);
      color: #34d399;
    }

    .status-badge.running {
      background: rgba(59, 130, 246, 0.2);
      color: #60a5fa;
    }

    .status-badge.final-table {
      background: rgba(245, 158, 11, 0.2);
      color: #fbbf24;
    }

    .status-badge.finished {
      background: rgba(107, 114, 128, 0.2);
      color: #9ca3af;
    }

    .card-header {
      margin-bottom: 1rem;
      padding-right: 6rem; /* Space for badge */
    }

    .tournament-name {
      font-size: 1.25rem;
      font-weight: 600;
      margin: 0;
      color: #fff;
    }

    .info-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .info-label {
      font-size: 0.75rem;
      color: #94a3b8;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .info-value {
      font-size: 1rem;
      color: #e2e8f0;
    }

    .info-value.buy-in {
      color: #fbbf24;
    }

    .info-value.prize {
      color: #34d399;
      font-weight: 600;
    }

    .blinds-info {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem;
      background: rgba(0, 0, 0, 0.2);
      border-radius: 0.5rem;
      margin-bottom: 1rem;
    }

    .blinds-label {
      color: #94a3b8;
      font-size: 0.875rem;
    }

    .blinds-value {
      color: #fff;
      font-weight: 600;
    }

    .level-badge {
      margin-left: auto;
      padding: 0.25rem 0.5rem;
      background: rgba(59, 130, 246, 0.2);
      color: #60a5fa;
      border-radius: 0.25rem;
      font-size: 0.75rem;
    }

    .card-footer {
      display: flex;
      gap: 0.75rem;
    }

    .btn-register, .btn-details {
      flex: 1;
      padding: 0.75rem 1rem;
      border: none;
      border-radius: 0.5rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-register {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: #fff;
    }

    .btn-register:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
    }

    .btn-details {
      background: rgba(255, 255, 255, 0.1);
      color: #e2e8f0;
      border: 1px solid rgba(255, 255, 255, 0.2);
    }

    .btn-details:hover {
      background: rgba(255, 255, 255, 0.15);
    }

    .fill-bar {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: rgba(255, 255, 255, 0.1);
    }

    .fill-progress {
      height: 100%;
      background: linear-gradient(90deg, #10b981 0%, #059669 100%);
      transition: width 0.3s ease;
    }

    .fill-progress.almost-full {
      background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%);
    }
  `]
})
export class TournamentCardComponent {
  readonly tournament = input.required<TournamentListItem>();
  readonly showJoin = input(true);

  @Output() register = new EventEmitter<TournamentListItem>();
  @Output() viewDetails = new EventEmitter<TournamentListItem>();

  readonly status = computed(() => this.tournament()?.status);

  readonly statusClass = computed(() => {
    const status = this.status();
    if (status === 'FINAL_TABLE') return 'final-table';
    return status?.toLowerCase() ?? '';
  });

  readonly statusIcon = computed(() => {
    const icons: Record<TournamentStatus, string> = {
      'REGISTERING': '📝',
      'STARTING': '⏳',
      'RUNNING': '▶️',
      'PAUSED': '⏸️',
      'FINAL_TABLE': '🔥',
      'FINISHED': '🏁'
    };
    return icons[this.status() as TournamentStatus] ?? '';
  });

  readonly statusText = computed(() => {
    const texts: Record<TournamentStatus, string> = {
      'REGISTERING': 'Open',
      'STARTING': 'Starting',
      'RUNNING': 'Running',
      'PAUSED': 'Break',
      'FINAL_TABLE': 'Final Table',
      'FINISHED': 'Finished'
    };
    return texts[this.status() as TournamentStatus] ?? this.status();
  });

  readonly fillPercentage = computed(() => {
    const t = this.tournament();
    if (!t) return 0;
    return (t.registeredCount / t.maxPlayers) * 100;
  });

  readonly isRunning = computed(() => {
    const status = this.status();
    return status === 'RUNNING' || status === 'FINAL_TABLE';
  });

  readonly canRegister = computed(() => {
    const t = this.tournament();
    return this.status() === 'REGISTERING' && t?.registeredCount < t?.maxPlayers;
  });

  onRegister(event: Event): void {
    event.stopPropagation();
    this.register.emit(this.tournament());
  }

  onViewDetails(event: Event): void {
    event.stopPropagation();
    this.viewDetails.emit(this.tournament());
  }
}
