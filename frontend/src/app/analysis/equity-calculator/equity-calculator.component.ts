import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule, PercentPipe, DecimalPipe } from '@angular/common';
import { AnalysisStore } from '../store/analysis.store';
import { CardSelectorComponent } from '../card-selector/card-selector.component';
import { RangeMatrixComponent } from '../range-matrix/range-matrix.component';
import { SelectedCard, EquityResult } from '../models/analysis.models';

@Component({
  selector: 'app-equity-calculator',
  standalone: true,
  imports: [
    CommonModule,
    PercentPipe,
    DecimalPipe,
    CardSelectorComponent,
    RangeMatrixComponent
  ],
  providers: [AnalysisStore],
  template: `
    <div class="equity-calculator">
      <header class="calc-header">
        <h2>Equity Calculator</h2>
        <p class="subtitle">Calculate your hand equity against villain's range</p>
      </header>

      <div class="calc-layout">
        <!-- Left Side: Hero Hand & Board -->
        <div class="cards-section">
          <!-- Hero Hand -->
          <section class="hero-section">
            <app-card-selector
              label="Your Hand"
              [maxCards]="2"
              [selectedCards]="vm().heroHand"
              [deadCards]="vm().communityCards"
              (cardsChange)="onHeroCardsChange($event)"
            />
          </section>

          <!-- Community Cards -->
          <section class="board-section">
            <app-card-selector
              label="Board"
              [maxCards]="5"
              [selectedCards]="vm().communityCards"
              [deadCards]="vm().heroHand"
              (cardsChange)="onBoardCardsChange($event)"
            />
          </section>

          <!-- Calculate Button -->
          <div class="action-section">
            <button 
              type="button"
              class="calculate-btn"
              [disabled]="!vm().canCalculate"
              (click)="calculate()">
              @if (vm().isCalculating) {
                <span class="spinner"></span>
                Calculating...
              } @else {
                Calculate Equity
              }
            </button>
            
            @if (vm().error) {
              <div class="error-message" role="alert">
                {{ vm().error }}
              </div>
            }
          </div>

          <!-- Results -->
          @if (vm().equityResult; as result) {
            <section class="results-section" aria-live="polite">
              <h3>Results</h3>
              
              <!-- Equity Bar -->
              <div class="equity-bar-container">
                <div class="equity-bar">
                  <div 
                    class="hero-equity"
                    [style.width.%]="result.heroEquity * 100"
                    [attr.aria-label]="'Hero equity: ' + (result.heroEquity | percent:'1.1-1')">
                    <span class="equity-label">You</span>
                    <span class="equity-value">{{ result.heroEquity | percent:'1.1-1' }}</span>
                  </div>
                  @if (result.tieEquity > 0.001) {
                    <div 
                      class="tie-equity"
                      [style.width.%]="result.tieEquity * 100"
                      [attr.aria-label]="'Tie: ' + (result.tieEquity | percent:'1.1-1')">
                      <span class="equity-value">{{ result.tieEquity | percent:'1.0-0' }}</span>
                    </div>
                  }
                  <div 
                    class="villain-equity"
                    [style.width.%]="result.villainEquity * 100"
                    [attr.aria-label]="'Villain equity: ' + (result.villainEquity | percent:'1.1-1')">
                    <span class="equity-label">Villain</span>
                    <span class="equity-value">{{ result.villainEquity | percent:'1.1-1' }}</span>
                  </div>
                </div>
              </div>

              <!-- Detailed Stats -->
              <div class="stats-grid">
                <div class="stat-card win">
                  <span class="stat-label">Win</span>
                  <span class="stat-value">{{ result.heroEquity | percent:'1.1-1' }}</span>
                </div>
                <div class="stat-card tie">
                  <span class="stat-label">Tie</span>
                  <span class="stat-value">{{ result.tieEquity | percent:'1.1-1' }}</span>
                </div>
                <div class="stat-card lose">
                  <span class="stat-label">Lose</span>
                  <span class="stat-value">{{ result.villainEquity | percent:'1.1-1' }}</span>
                </div>
              </div>

              <!-- Simulations Info -->
              <p class="sim-info">
                Based on {{ result.simulationCount | number }} simulations
              </p>

              <!-- Hand Type Breakdown -->
              @if (result.handTypeBreakdown) {
                <div class="breakdown-section">
                  <h4>Your Hand Distribution</h4>
                  <div class="breakdown-list">
                    @for (entry of getBreakdownEntries(result); track entry.type) {
                      <div class="breakdown-item">
                        <span class="breakdown-type">{{ formatHandType(entry.type) }}</span>
                        <div class="breakdown-bar-container">
                          <div 
                            class="breakdown-bar"
                            [style.width.%]="entry.percentage * 100">
                          </div>
                        </div>
                        <span class="breakdown-pct">{{ entry.percentage | percent:'1.1-1' }}</span>
                      </div>
                    }
                  </div>
                </div>
              }
            </section>
          }
        </div>

        <!-- Right Side: Villain Range -->
        <div class="range-section">
          <app-range-matrix
            title="Villain's Range"
            [selectedRange]="vm().villainRange"
            (rangeChange)="onRangeChange($event)"
            (presetSelected)="onPresetSelected($event)"
          />
        </div>
      </div>
    </div>
  `,
  styles: [`
    .equity-calculator {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
      padding: 1.5rem;
    }

    .calc-header {
      text-align: center;
    }

    .calc-header h2 {
      margin: 0;
      font-size: 1.75rem;
      color: var(--text-primary, #fff);
    }

    .subtitle {
      margin: 0.5rem 0 0;
      color: var(--text-secondary, #aaa);
      font-size: 0.95rem;
    }

    .calc-layout {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 2rem;
      align-items: start;
    }

    @media (max-width: 900px) {
      .calc-layout {
        grid-template-columns: 1fr;
      }
    }

    .cards-section {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .hero-section, .board-section {
      /* Card selector styling handled by component */
    }

    .action-section {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .calculate-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 1rem 2rem;
      font-size: 1rem;
      font-weight: 600;
      color: white;
      background: linear-gradient(135deg, var(--primary-color, #4f46e5), #6366f1);
      border: none;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .calculate-btn:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 15px rgba(79, 70, 229, 0.4);
    }

    .calculate-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
      transform: none;
      box-shadow: none;
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error-message {
      padding: 0.75rem;
      background: var(--danger-bg, rgba(239, 68, 68, 0.1));
      border: 1px solid var(--danger-color, #ef4444);
      border-radius: 6px;
      color: var(--danger-color, #ef4444);
      font-size: 0.85rem;
    }

    .results-section {
      padding: 1.25rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 12px;
      animation: fadeIn 0.3s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .results-section h3 {
      margin: 0 0 1rem;
      font-size: 1.1rem;
      color: var(--text-primary, #fff);
    }

    .equity-bar-container {
      margin-bottom: 1.25rem;
    }

    .equity-bar {
      display: flex;
      height: 48px;
      border-radius: 8px;
      overflow: hidden;
      background: var(--surface-secondary, #252542);
    }

    .hero-equity, .villain-equity, .tie-equity {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-width: 60px;
      transition: width 0.5s ease-out;
    }

    .hero-equity {
      background: linear-gradient(135deg, #22c55e, #16a34a);
    }

    .tie-equity {
      background: var(--text-muted, #666);
      min-width: 40px;
    }

    .villain-equity {
      background: linear-gradient(135deg, #ef4444, #dc2626);
    }

    .equity-label {
      font-size: 0.7rem;
      text-transform: uppercase;
      opacity: 0.8;
    }

    .equity-value {
      font-size: 1rem;
      font-weight: 700;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .stat-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 0.75rem;
      border-radius: 8px;
      background: var(--surface-secondary, #252542);
    }

    .stat-card.win { border-left: 3px solid #22c55e; }
    .stat-card.tie { border-left: 3px solid #666; }
    .stat-card.lose { border-left: 3px solid #ef4444; }

    .stat-label {
      font-size: 0.75rem;
      color: var(--text-secondary, #aaa);
      text-transform: uppercase;
    }

    .stat-value {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text-primary, #fff);
    }

    .sim-info {
      margin: 0;
      font-size: 0.75rem;
      color: var(--text-muted, #666);
      text-align: center;
    }

    .breakdown-section {
      margin-top: 1.25rem;
      padding-top: 1rem;
      border-top: 1px solid var(--border-color, #333);
    }

    .breakdown-section h4 {
      margin: 0 0 0.75rem;
      font-size: 0.9rem;
      color: var(--text-secondary, #aaa);
    }

    .breakdown-list {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .breakdown-item {
      display: grid;
      grid-template-columns: 100px 1fr 50px;
      align-items: center;
      gap: 0.75rem;
    }

    .breakdown-type {
      font-size: 0.8rem;
      color: var(--text-secondary, #aaa);
    }

    .breakdown-bar-container {
      height: 8px;
      background: var(--surface-secondary, #252542);
      border-radius: 4px;
      overflow: hidden;
    }

    .breakdown-bar {
      height: 100%;
      background: linear-gradient(90deg, #4f46e5, #6366f1);
      border-radius: 4px;
      transition: width 0.3s ease-out;
    }

    .breakdown-pct {
      font-size: 0.8rem;
      color: var(--text-primary, #fff);
      text-align: right;
    }

    .range-section {
      /* Range matrix styling handled by component */
    }
  `]
})
export class EquityCalculatorComponent {
  private readonly store = inject(AnalysisStore);

  protected readonly vm = toSignal(this.store.vm$, {
    initialValue: {
      heroHand: [],
      villainRange: new Set<string>(),
      communityCards: [],
      equityResult: null,
      isCalculating: false,
      error: null,
      rangePercentage: 0,
      selectedComboCount: 0,
      rangeNotation: '',
      canCalculate: false,
      selectedPreset: null
    }
  });

  
  protected onHeroCardsChange(cards: SelectedCard[]): void {
    this.store.setHeroHand(cards);
  }

  protected onBoardCardsChange(cards: SelectedCard[]): void {
    this.store.setCommunityCards(cards);
  }

  protected onRangeChange(range: Set<string>): void {
    this.store.setVillainRange(range);
  }

  protected onPresetSelected(preset: string): void {
    this.store.setPresetRange(preset);
  }

  protected calculate(): void {
    this.store.calculateEquity();
  }

  
  protected getBreakdownEntries(result: EquityResult): { type: string; percentage: number }[] {
    if (!result.handTypeBreakdown) return [];
    
    return Object.entries(result.handTypeBreakdown)
      .map(([type, pct]) => ({ type, percentage: pct }))
      .filter(e => e.percentage > 0.001)
      .sort((a, b) => b.percentage - a.percentage);
  }

  protected formatHandType(type: string): string {
    const formatted: Record<string, string> = {
      'HIGH_CARD': 'High Card',
      'PAIR': 'One Pair',
      'TWO_PAIR': 'Two Pair',
      'THREE_OF_A_KIND': 'Three of a Kind',
      'STRAIGHT': 'Straight',
      'FLUSH': 'Flush',
      'FULL_HOUSE': 'Full House',
      'FOUR_OF_A_KIND': 'Four of a Kind',
      'STRAIGHT_FLUSH': 'Straight Flush',
      'ROYAL_FLUSH': 'Royal Flush'
    };
    return formatted[type] || type.replace(/_/g, ' ');
  }
}
