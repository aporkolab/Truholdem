import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { slideInAnimation, fadeAnimation } from '../../animations/hand-replay.animations';

export interface ActionAnalysisData {
  action: string;
  assessment: 'OPTIMAL' | 'GOOD' | 'ACCEPTABLE' | 'QUESTIONABLE' | 'MISTAKE' | 'BLUNDER';
  evActual: number;
  evOptimal: number;
  evLost: number;
  optimalAction: string;
  reasoning: string;
  alternatives: AlternativeAction[];
  gtoFrequency?: GtoFrequency;
}

export interface AlternativeAction {
  action: string;
  ev: number;
  frequency?: number;
}

export interface GtoFrequency {
  fold: number;
  check: number;
  call: number;
  bet: number;
  raise: number;
}

@Component({
  selector: 'app-action-analysis',
  standalone: true,
  imports: [CommonModule],
  animations: [slideInAnimation, fadeAnimation],
  template: `
    <div class="action-analysis" 
         [class.expanded]="expanded()" 
         [@slideIn]
         data-cy="action-analysis">
      
      <!-- Header with assessment badge -->
      <div class="analysis-header" (click)="toggleExpanded()" (keydown.enter)="toggleExpanded()" tabindex="0" role="button">
        <div class="action-taken">
          <span class="action-label">Action:</span>
          <span class="action-value">{{ analysis()?.action }}</span>
        </div>
        
        <div class="assessment-badge" [class]="getAssessmentClass()">
          {{ analysis()?.assessment }}
        </div>
        
        <button class="expand-toggle" [attr.aria-expanded]="expanded()">
          {{ expanded() ? '▼' : '▶' }}
        </button>
      </div>

      <!-- EV Summary -->
      <div class="ev-summary">
        <div class="ev-item actual">
          <span class="ev-label">Your EV</span>
          <span class="ev-value" [class.positive]="isActualPositive()" [class.negative]="!isActualPositive()">
            {{ formatEV(analysis()?.evActual) }}
          </span>
        </div>
        
        <div class="ev-item optimal">
          <span class="ev-label">Optimal EV</span>
          <span class="ev-value positive">
            {{ formatEV(analysis()?.evOptimal) }}
          </span>
        </div>
        
        <div class="ev-item lost" *ngIf="hasEvLoss()">
          <span class="ev-label">EV Lost</span>
          <span class="ev-value negative">
            -{{ formatEV(Math.abs(analysis()?.evLost ?? 0)) }}
          </span>
        </div>
      </div>

      <!-- Expanded content -->
      <div class="expanded-content" *ngIf="expanded()" [@fade]>
        
        <!-- Optimal action suggestion -->
        <div class="optimal-action" *ngIf="showOptimalAction()">
          <span class="suggestion-label">💡 Better play:</span>
          <span class="suggestion-value">{{ analysis()?.optimalAction }}</span>
        </div>

        <!-- Reasoning -->
        <div class="reasoning" *ngIf="analysis()?.reasoning">
          <span class="reasoning-label">Why:</span>
          <p class="reasoning-text">{{ analysis()?.reasoning }}</p>
        </div>

        <!-- Alternatives comparison -->
        <div class="alternatives" *ngIf="hasAlternatives()">
          <span class="section-label">Alternative Actions:</span>
          <div class="alternatives-list">
            <div class="alternative-item" 
                 *ngFor="let alt of analysis()?.alternatives"
                 [class.best]="isBestAlternative(alt)">
              <span class="alt-action">{{ alt.action }}</span>
              <span class="alt-ev" [class.positive]="alt.ev > 0" [class.negative]="alt.ev < 0">
                {{ formatEV(alt.ev) }}
              </span>
              <span class="alt-frequency" *ngIf="alt.frequency !== undefined">
                ({{ formatPercent(alt.frequency) }}%)
              </span>
            </div>
          </div>
        </div>

        <!-- GTO Frequency breakdown -->
        <div class="gto-frequencies" *ngIf="analysis()?.gtoFrequency">
          <span class="section-label">GTO Mixed Strategy:</span>
          <div class="frequency-bars">
            <div class="freq-bar-row" *ngIf="analysis()!.gtoFrequency!.fold > 0">
              <span class="freq-label">Fold</span>
              <div class="freq-bar">
                <div class="freq-fill fold" [style.width.%]="analysis()!.gtoFrequency!.fold * 100"></div>
              </div>
              <span class="freq-value">{{ formatPercent(analysis()!.gtoFrequency!.fold) }}%</span>
            </div>
            
            <div class="freq-bar-row" *ngIf="analysis()!.gtoFrequency!.check > 0">
              <span class="freq-label">Check</span>
              <div class="freq-bar">
                <div class="freq-fill check" [style.width.%]="analysis()!.gtoFrequency!.check * 100"></div>
              </div>
              <span class="freq-value">{{ formatPercent(analysis()!.gtoFrequency!.check) }}%</span>
            </div>
            
            <div class="freq-bar-row" *ngIf="analysis()!.gtoFrequency!.call > 0">
              <span class="freq-label">Call</span>
              <div class="freq-bar">
                <div class="freq-fill call" [style.width.%]="analysis()!.gtoFrequency!.call * 100"></div>
              </div>
              <span class="freq-value">{{ formatPercent(analysis()!.gtoFrequency!.call) }}%</span>
            </div>
            
            <div class="freq-bar-row" *ngIf="analysis()!.gtoFrequency!.bet > 0">
              <span class="freq-label">Bet</span>
              <div class="freq-bar">
                <div class="freq-fill bet" [style.width.%]="analysis()!.gtoFrequency!.bet * 100"></div>
              </div>
              <span class="freq-value">{{ formatPercent(analysis()!.gtoFrequency!.bet) }}%</span>
            </div>
            
            <div class="freq-bar-row" *ngIf="analysis()!.gtoFrequency!.raise > 0">
              <span class="freq-label">Raise</span>
              <div class="freq-bar">
                <div class="freq-fill raise" [style.width.%]="analysis()!.gtoFrequency!.raise * 100"></div>
              </div>
              <span class="freq-value">{{ formatPercent(analysis()!.gtoFrequency!.raise) }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .action-analysis {
      background: rgba(0, 0, 0, 0.8);
      border-radius: 8px;
      padding: 12px;
      color: white;
      min-width: 280px;
      max-width: 350px;
    }

    .analysis-header {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      padding-bottom: 8px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .action-taken {
      flex: 1;
      
      .action-label {
        color: #9ca3af;
        font-size: 0.8rem;
        margin-right: 6px;
      }
      
      .action-value {
        font-weight: bold;
        text-transform: uppercase;
      }
    }

    .assessment-badge {
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: bold;
      text-transform: uppercase;

      &.optimal { background: #166534; color: #4ade80; }
      &.good { background: #14532d; color: #86efac; }
      &.acceptable { background: #854d0e; color: #fde047; }
      &.questionable { background: #9a3412; color: #fdba74; }
      &.mistake { background: #991b1b; color: #fca5a5; }
      &.blunder { background: #7f1d1d; color: #f87171; border: 1px solid #dc2626; }
    }

    .expand-toggle {
      background: none;
      border: none;
      color: #9ca3af;
      cursor: pointer;
      padding: 4px;
      font-size: 0.8rem;

      &:hover { color: white; }
    }

    .ev-summary {
      display: flex;
      justify-content: space-around;
      padding: 12px 0;
      gap: 8px;
    }

    .ev-item {
      text-align: center;

      .ev-label {
        display: block;
        font-size: 0.7rem;
        color: #9ca3af;
        margin-bottom: 2px;
      }

      .ev-value {
        font-weight: bold;
        font-size: 1rem;

        &.positive { color: #4ade80; }
        &.negative { color: #f87171; }
      }
    }

    .expanded-content {
      padding-top: 12px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    .optimal-action {
      background: rgba(34, 197, 94, 0.1);
      border: 1px solid rgba(34, 197, 94, 0.3);
      border-radius: 6px;
      padding: 8px 12px;
      margin-bottom: 12px;

      .suggestion-label {
        font-size: 0.8rem;
        color: #86efac;
      }

      .suggestion-value {
        font-weight: bold;
        color: #4ade80;
        margin-left: 8px;
        text-transform: uppercase;
      }
    }

    .reasoning {
      margin-bottom: 12px;

      .reasoning-label {
        font-size: 0.8rem;
        color: #9ca3af;
      }

      .reasoning-text {
        margin: 4px 0 0 0;
        font-size: 0.85rem;
        line-height: 1.4;
        color: #d1d5db;
      }
    }

    .section-label {
      display: block;
      font-size: 0.8rem;
      color: #9ca3af;
      margin-bottom: 8px;
    }

    .alternatives-list {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin-bottom: 12px;
    }

    .alternative-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 8px;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 4px;
      font-size: 0.85rem;

      &.best {
        background: rgba(34, 197, 94, 0.1);
        border-left: 2px solid #4ade80;
      }

      .alt-action {
        flex: 1;
        text-transform: uppercase;
      }

      .alt-ev {
        font-weight: bold;
        &.positive { color: #4ade80; }
        &.negative { color: #f87171; }
      }

      .alt-frequency {
        color: #9ca3af;
        font-size: 0.8em;
      }
    }

    .gto-frequencies {
      margin-top: 12px;
    }

    .frequency-bars {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .freq-bar-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .freq-label {
      width: 50px;
      font-size: 0.8rem;
      color: #9ca3af;
      text-transform: uppercase;
    }

    .freq-bar {
      flex: 1;
      height: 8px;
      background: #374151;
      border-radius: 4px;
      overflow: hidden;
    }

    .freq-fill {
      height: 100%;
      transition: width 0.3s ease;

      &.fold { background: #6b7280; }
      &.check { background: #60a5fa; }
      &.call { background: #34d399; }
      &.bet { background: #f59e0b; }
      &.raise { background: #ef4444; }
    }

    .freq-value {
      width: 40px;
      font-size: 0.75rem;
      color: #9ca3af;
      text-align: right;
    }
  `]
})
export class ActionAnalysisComponent {
  Math = Math;

  @Input() set analysisData(value: ActionAnalysisData | null) {
    this._analysis.set(value);
  }

  private _analysis = signal<ActionAnalysisData | null>(null);
  private _expanded = signal(false);

  analysis = this._analysis.asReadonly();
  expanded = this._expanded.asReadonly();

  toggleExpanded(): void {
    this._expanded.update(v => !v);
  }

  getAssessmentClass(): string {
    return this._analysis()?.assessment?.toLowerCase() ?? '';
  }

  isActualPositive = computed(() => {
    const a = this._analysis();
    if (!a) return false;
    return a.evActual >= 0;
  });

  hasEvLoss = computed(() => {
    const a = this._analysis();
    if (!a) return false;
    return Math.abs(a.evLost) > 0.01;
  });

  showOptimalAction = computed(() => {
    const a = this._analysis();
    if (!a) return false;
    return a.assessment !== 'OPTIMAL' && a.optimalAction !== a.action;
  });

  hasAlternatives = computed(() => {
    const a = this._analysis();
    if (!a) return false;
    return a.alternatives && a.alternatives.length > 0;
  });

  isBestAlternative(alt: AlternativeAction): boolean {
    const a = this._analysis();
    if (!a || !a.alternatives.length) return false;
    const maxEv = Math.max(...a.alternatives.map(x => x.ev));
    return alt.ev === maxEv;
  }

  formatEV(value: number | undefined | null): string {
    if (value === undefined || value === null) return '$0';
    const sign = value >= 0 ? '+' : '-';
    const absValue = Math.abs(value);
    return `${sign}$${absValue.toFixed(2)}`;
  }

  formatPercent(value: number | undefined): number {
    if (value === undefined) return 0;
    return Math.round(value * 1000) / 10;
  }
}