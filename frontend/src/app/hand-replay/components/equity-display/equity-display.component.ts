import { Component, Input, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { equityFillAnimation, pulseAnimation } from '../../animations/hand-replay.animations';

export interface EquityData {
  winProbability: number;
  tieProbability: number;
  loseProbability: number;
  confidenceInterval?: {
    lower: number;
    upper: number;
  };
  handStrength?: string;
  outs?: number;
  potOdds?: number;
  impliedOdds?: number;
}

@Component({
  selector: 'app-equity-display',
  standalone: true,
  imports: [CommonModule],
  animations: [equityFillAnimation, pulseAnimation],
  template: `
    <div class="equity-display" [class.compact]="compact()" data-cy="equity-display">
      <!-- Main equity bar -->
      <div class="equity-bar-container" data-cy="equity-bar-container">
        <div class="equity-label">
          <span class="label-text">Equity</span>
          <span class="equity-value" 
                [class.strong]="isStrong()" 
                [class.weak]="isWeak()"
                [@pulse]="pulseState()">
            {{ displayEquity() }}%
          </span>
        </div>
        
        <div class="equity-bar" data-cy="equity-bar">
          <div class="equity-segment win" 
               [style.width.%]="winWidth()"
               [@equityFill]="equity()?.winProbability"
               data-cy="equity-win-segment">
          </div>
          <div class="equity-segment tie" 
               [style.width.%]="tieWidth()"
               [style.left.%]="winWidth()"
               data-cy="equity-tie-segment">
          </div>
          <div class="equity-segment lose" 
               [style.width.%]="loseWidth()"
               data-cy="equity-lose-segment">
          </div>
        </div>
        
        <div class="equity-legend" *ngIf="!compact()">
          <span class="legend-item win">
            <span class="dot"></span>
            Win: {{ winPercent() }}%
          </span>
          <span class="legend-item tie" *ngIf="hasTie()">
            <span class="dot"></span>
            Tie: {{ tiePercent() }}%
          </span>
          <span class="legend-item lose">
            <span class="dot"></span>
            Lose: {{ losePercent() }}%
          </span>
        </div>
      </div>

      <!-- Confidence interval -->
      <div class="confidence-interval" *ngIf="equity()?.confidenceInterval && !compact()">
        <span class="ci-label">95% CI:</span>
        <span class="ci-range">
          {{ formatPercent(equity()?.confidenceInterval?.lower) }}% - 
          {{ formatPercent(equity()?.confidenceInterval?.upper) }}%
        </span>
      </div>

      <!-- Additional info -->
      <div class="equity-details" *ngIf="!compact()">
        <!-- Hand strength indicator -->
        <div class="detail-row" *ngIf="equity()?.handStrength">
          <span class="detail-label">Hand Strength:</span>
          <span class="detail-value hand-strength" 
                [class]="getStrengthClass()">
            {{ equity()?.handStrength }}
          </span>
        </div>

        <!-- Outs -->
        <div class="detail-row" *ngIf="equity()?.outs !== undefined && equity()?.outs! > 0">
          <span class="detail-label">Outs:</span>
          <span class="detail-value">{{ equity()?.outs }}
            <span class="outs-equity">(~{{ outsEquity() }}%)</span>
          </span>
        </div>

        <!-- Pot odds comparison -->
        <div class="detail-row pot-odds" *ngIf="equity()?.potOdds !== undefined">
          <span class="detail-label">Pot Odds:</span>
          <span class="detail-value" [class.profitable]="isProfitable()" [class.unprofitable]="!isProfitable()">
            {{ formatPercent(equity()?.potOdds) }}%
            <span class="odds-indicator">
              {{ isProfitable() ? '✓ +EV' : '✗ -EV' }}
            </span>
          </span>
        </div>
      </div>

      <!-- Quick indicator for compact mode -->
      <div class="quick-indicator" *ngIf="compact()">
        <span class="indicator" [class]="getQuickIndicatorClass()">
          {{ getQuickIndicator() }}
        </span>
      </div>
    </div>
  `,
  styles: [`
    .equity-display {
      background: rgba(0, 0, 0, 0.7);
      border-radius: 8px;
      padding: 12px;
      color: white;
      min-width: 200px;

      &.compact {
        min-width: 120px;
        padding: 8px;
      }
    }

    .equity-bar-container {
      margin-bottom: 8px;
    }

    .equity-label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
      font-size: 0.9rem;

      .label-text {
        color: #a0a0a0;
      }

      .equity-value {
        font-weight: bold;
        font-size: 1.1rem;
        transition: color 0.3s;

        &.strong { color: #4ade80; }
        &.weak { color: #f87171; }
      }
    }

    .equity-bar {
      height: 12px;
      background: #374151;
      border-radius: 6px;
      overflow: hidden;
      position: relative;
      display: flex;
    }

    .equity-segment {
      height: 100%;
      transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;

      &.win {
        background: linear-gradient(90deg, #22c55e, #4ade80);
      }

      &.tie {
        background: linear-gradient(90deg, #eab308, #facc15);
        position: absolute;
      }

      &.lose {
        background: linear-gradient(90deg, #dc2626, #f87171);
        flex-grow: 1;
      }
    }

    .equity-legend {
      display: flex;
      gap: 12px;
      margin-top: 6px;
      font-size: 0.75rem;
      color: #9ca3af;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;

      .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
      }

      &.win .dot { background: #4ade80; }
      &.tie .dot { background: #facc15; }
      &.lose .dot { background: #f87171; }
    }

    .confidence-interval {
      font-size: 0.75rem;
      color: #9ca3af;
      margin-bottom: 8px;
      padding: 4px 8px;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 4px;
    }

    .equity-details {
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      padding-top: 8px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
      font-size: 0.85rem;
    }

    .detail-label {
      color: #9ca3af;
    }

    .detail-value {
      font-weight: 500;

      &.profitable { color: #4ade80; }
      &.unprofitable { color: #f87171; }
    }

    .hand-strength {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      text-transform: uppercase;

      &.value-heavy { background: #166534; color: #4ade80; }
      &.medium-strength { background: #854d0e; color: #facc15; }
      &.drawing { background: #1e40af; color: #60a5fa; }
      &.bluff-candidate { background: #7c2d12; color: #fb923c; }
      &.trash { background: #374151; color: #9ca3af; }
    }

    .outs-equity {
      color: #60a5fa;
      font-size: 0.8em;
    }

    .odds-indicator {
      margin-left: 4px;
      font-size: 0.85em;
    }

    .quick-indicator {
      text-align: center;
      margin-top: 4px;

      .indicator {
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 0.75rem;
        font-weight: bold;

        &.strong { background: #166534; color: #4ade80; }
        &.medium { background: #854d0e; color: #facc15; }
        &.weak { background: #7f1d1d; color: #f87171; }
      }
    }
  `]
})
export class EquityDisplayComponent {
  @Input() set equityData(value: EquityData | null) {
    this._equity.set(value);
  }
  @Input() set isCompact(value: boolean) {
    this._compact.set(value);
  }

  private _equity = signal<EquityData | null>(null);
  private _compact = signal(false);
  private _pulseState = signal<string>('idle');

  equity = this._equity.asReadonly();
  compact = this._compact.asReadonly();
  pulseState = this._pulseState.asReadonly();

  constructor() {
    effect(() => {
      const eq = this._equity();
      if (eq) {
        this._pulseState.set('pulse');
        setTimeout(() => this._pulseState.set('idle'), 300);
      }
    });
  }

  displayEquity = computed(() => {
    const eq = this._equity();
    if (!eq) return 0;
    return this.formatPercent(eq.winProbability + (eq.tieProbability / 2));
  });

  winWidth = computed(() => {
    const eq = this._equity();
    return eq ? eq.winProbability * 100 : 0;
  });

  tieWidth = computed(() => {
    const eq = this._equity();
    return eq ? eq.tieProbability * 100 : 0;
  });

  loseWidth = computed(() => {
    const eq = this._equity();
    return eq ? eq.loseProbability * 100 : 0;
  });

  winPercent = computed(() => this.formatPercent(this._equity()?.winProbability));
  tiePercent = computed(() => this.formatPercent(this._equity()?.tieProbability));
  losePercent = computed(() => this.formatPercent(this._equity()?.loseProbability));

  hasTie = computed(() => (this._equity()?.tieProbability ?? 0) > 0.01);

  isStrong = computed(() => {
    const eq = this._equity();
    return eq ? (eq.winProbability + eq.tieProbability / 2) >= 0.6 : false;
  });

  isWeak = computed(() => {
    const eq = this._equity();
    return eq ? (eq.winProbability + eq.tieProbability / 2) < 0.4 : false;
  });

  outsEquity = computed(() => {
    const outs = this._equity()?.outs ?? 0;
    return Math.min(outs * 2, 100);
  });

  isProfitable = computed(() => {
    const eq = this._equity();
    if (!eq || eq.potOdds === undefined) return false;
    const totalEquity = eq.winProbability + eq.tieProbability / 2;
    return totalEquity > eq.potOdds;
  });

  formatPercent(value: number | undefined): number {
    if (value === undefined) return 0;
    return Math.round(value * 1000) / 10;
  }

  getStrengthClass(): string {
    const strength = this._equity()?.handStrength?.toLowerCase().replace(/_/g, '-');
    return strength ?? '';
  }

  getQuickIndicatorClass(): string {
    const eq = this._equity();
    if (!eq) return '';
    const total = eq.winProbability + eq.tieProbability / 2;
    if (total >= 0.6) return 'strong';
    if (total >= 0.4) return 'medium';
    return 'weak';
  }

  getQuickIndicator(): string {
    const eq = this._equity();
    if (!eq) return '-';
    const total = eq.winProbability + eq.tieProbability / 2;
    if (total >= 0.6) return 'AHEAD';
    if (total >= 0.4) return 'FLIP';
    return 'BEHIND';
  }
}