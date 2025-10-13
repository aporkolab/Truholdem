import { Component, inject, signal, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RangeMatrixComponent } from '../range-matrix/range-matrix.component';
import { PRESET_RANGES, calculateRangePercentage } from '../models/analysis.models';

@Component({
  selector: 'app-range-builder',
  standalone: true,
  imports: [CommonModule, RangeMatrixComponent],
  template: `
    <div class="range-builder">
      <header class="builder-header">
        <h2>Range Builder</h2>
        <p class="subtitle">Build and analyze hand ranges for different positions</p>
      </header>

      <div class="builder-layout">
        <!-- Range Matrix -->
        <div class="matrix-container">
          <app-range-matrix
            title="Build Your Range"
            [selectedRange]="selectedRange()"
            (rangeChange)="onRangeChange($event)"
            (presetSelected)="onPresetSelected($event)"
          />
        </div>

        <!-- Analysis Panel -->
        <div class="analysis-panel">
          <section class="stats-section">
            <h3>Range Statistics</h3>
            <div class="stat-grid">
              <div class="stat-item">
                <span class="stat-label">Total Combos</span>
                <span class="stat-value">{{ comboCount() }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Range %</span>
                <span class="stat-value">{{ rangePercentage() }}%</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Hands</span>
                <span class="stat-value">{{ handCount() }}</span>
              </div>
            </div>
          </section>

          <section class="presets-section">
            <h3>Position Presets</h3>
            <div class="preset-cards">
              @for (preset of presetList; track preset.key) {
                <button 
                  class="preset-card"
                  [class.active]="activePreset() === preset.key"
                  (click)="loadPreset(preset.key)">
                  <span class="preset-name">{{ preset.name }}</span>
                  <span class="preset-pct">{{ preset.percentage }}%</span>
                  <span class="preset-desc">{{ preset.description }}</span>
                </button>
              }
            </div>
          </section>

          <section class="breakdown-section">
            <h3>Range Breakdown</h3>
            <div class="breakdown-bars">
              <div class="breakdown-item">
                <span class="breakdown-label">Pairs</span>
                <div class="bar-container">
                  <div class="bar pairs" [style.width.%]="pairsPercentage()"></div>
                </div>
                <span class="breakdown-value">{{ pairCount() }}</span>
              </div>
              <div class="breakdown-item">
                <span class="breakdown-label">Suited</span>
                <div class="bar-container">
                  <div class="bar suited" [style.width.%]="suitedPercentage()"></div>
                </div>
                <span class="breakdown-value">{{ suitedCount() }}</span>
              </div>
              <div class="breakdown-item">
                <span class="breakdown-label">Offsuit</span>
                <div class="bar-container">
                  <div class="bar offsuit" [style.width.%]="offsuitPercentage()"></div>
                </div>
                <span class="breakdown-value">{{ offsuitCount() }}</span>
              </div>
            </div>
          </section>

          <section class="export-section">
            <h3>Range Notation</h3>
            <textarea 
              class="notation-display"
              [value]="rangeNotation()"
              readonly
              aria-label="Range notation"
            ></textarea>
            <button class="copy-btn" (click)="copyNotation()">
              {{ copied() ? '✓ Copied!' : 'Copy Notation' }}
            </button>
          </section>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .range-builder {
      padding: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    .builder-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .builder-header h2 {
      margin: 0;
      font-size: 1.75rem;
      color: var(--text-primary, #fff);
    }

    .subtitle {
      margin: 0.5rem 0 0;
      color: var(--text-secondary, #aaa);
    }

    .builder-layout {
      display: grid;
      grid-template-columns: 1fr 350px;
      gap: 2rem;
      align-items: start;
    }

    @media (max-width: 900px) {
      .builder-layout {
        grid-template-columns: 1fr;
      }
    }

    .matrix-container {
      /* Handled by range-matrix */
    }

    .analysis-panel {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .analysis-panel section {
      padding: 1rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 12px;
    }

    .analysis-panel h3 {
      margin: 0 0 1rem;
      font-size: 1rem;
      color: var(--text-primary, #fff);
    }

    .stat-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
    }

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 0.75rem;
      background: var(--surface-secondary, #252542);
      border-radius: 8px;
    }

    .stat-label {
      font-size: 0.7rem;
      color: var(--text-muted, #666);
      text-transform: uppercase;
    }

    .stat-value {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--primary-color, #22c55e);
    }

    .preset-cards {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .preset-card {
      display: grid;
      grid-template-columns: 1fr auto;
      grid-template-rows: auto auto;
      gap: 0.25rem 1rem;
      padding: 0.75rem;
      background: var(--surface-secondary, #252542);
      border: 1px solid transparent;
      border-radius: 8px;
      cursor: pointer;
      text-align: left;
      transition: all 0.2s ease;
    }

    .preset-card:hover {
      border-color: var(--border-color, #333);
      background: var(--surface-hover, #333);
    }

    .preset-card.active {
      border-color: var(--primary-color, #4f46e5);
      background: rgba(79, 70, 229, 0.1);
    }

    .preset-name {
      font-weight: 600;
      color: var(--text-primary, #fff);
    }

    .preset-pct {
      font-weight: 700;
      color: var(--primary-color, #22c55e);
    }

    .preset-desc {
      grid-column: 1 / -1;
      font-size: 0.75rem;
      color: var(--text-muted, #666);
    }

    .breakdown-bars {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .breakdown-item {
      display: grid;
      grid-template-columns: 60px 1fr 40px;
      align-items: center;
      gap: 0.75rem;
    }

    .breakdown-label {
      font-size: 0.8rem;
      color: var(--text-secondary, #aaa);
    }

    .bar-container {
      height: 10px;
      background: var(--surface-secondary, #252542);
      border-radius: 5px;
      overflow: hidden;
    }

    .bar {
      height: 100%;
      border-radius: 5px;
      transition: width 0.3s ease-out;
    }

    .bar.pairs { background: #a855f7; }
    .bar.suited { background: #3b82f6; }
    .bar.offsuit { background: #6b7280; }

    .breakdown-value {
      font-size: 0.8rem;
      color: var(--text-primary, #fff);
      text-align: right;
    }

    .notation-display {
      width: 100%;
      min-height: 80px;
      padding: 0.75rem;
      background: var(--surface-secondary, #252542);
      border: 1px solid var(--border-color, #333);
      border-radius: 6px;
      color: var(--text-primary, #fff);
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 0.8rem;
      resize: vertical;
    }

    .copy-btn {
      margin-top: 0.5rem;
      padding: 0.5rem 1rem;
      background: var(--primary-color, #4f46e5);
      border: none;
      border-radius: 6px;
      color: white;
      font-size: 0.85rem;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .copy-btn:hover {
      background: var(--primary-hover, #4338ca);
    }
  `]
})
export class RangeBuilderComponent {
  private ngZone = inject(NgZone);
  private copiedTimeout: ReturnType<typeof setTimeout> | undefined;

  protected selectedRange = signal<Set<string>>(new Set());
  protected activePreset = signal<string | null>(null);
  protected copied = signal(false);

  protected readonly presetList = Object.entries(PRESET_RANGES).map(([key, preset]) => ({
    key,
    ...preset
  }));

  
  protected comboCount = () => {
    let count = 0;
    for (const notation of this.selectedRange()) {
      count += this.getComboCount(notation);
    }
    return count;
  };

  protected rangePercentage = () => {
    const pct = calculateRangePercentage(this.selectedRange());
    return (pct * 100).toFixed(1);
  };

  protected handCount = () => this.selectedRange().size;

  protected rangeNotation = () => Array.from(this.selectedRange()).sort().join(', ');

  protected pairCount = () => {
    let count = 0;
    for (const n of this.selectedRange()) {
      if (n.length === 2 || n[0] === n[1]) count++;
    }
    return count;
  };

  protected suitedCount = () => {
    let count = 0;
    for (const n of this.selectedRange()) {
      if (n.endsWith('s')) count++;
    }
    return count;
  };

  protected offsuitCount = () => {
    let count = 0;
    for (const n of this.selectedRange()) {
      if (n.endsWith('o')) count++;
    }
    return count;
  };

  protected pairsPercentage = () => (this.pairCount() / Math.max(1, this.handCount())) * 100;
  protected suitedPercentage = () => (this.suitedCount() / Math.max(1, this.handCount())) * 100;
  protected offsuitPercentage = () => (this.offsuitCount() / Math.max(1, this.handCount())) * 100;

  onRangeChange(range: Set<string>): void {
    this.selectedRange.set(range);
    this.activePreset.set(null);
  }

  onPresetSelected(preset: string): void {
    this.activePreset.set(preset);
  }

  loadPreset(presetKey: string): void {
    const preset = PRESET_RANGES[presetKey];
    if (preset) {
      this.selectedRange.set(new Set(preset.hands));
      this.activePreset.set(presetKey);
    }
  }

  copyNotation(): void {
    // Clear any existing timeout
    if (this.copiedTimeout) {
      clearTimeout(this.copiedTimeout);
    }

    navigator.clipboard.writeText(this.rangeNotation()).then(() => {
      this.ngZone.run(() => {
        this.copied.set(true);
        this.copiedTimeout = setTimeout(() => {
          this.copied.set(false);
        }, 2000);
      });
    });
  }

  private getComboCount(notation: string): number {
    if (notation.length === 2 || notation[0] === notation[1]) return 6;
    if (notation.endsWith('s')) return 4;
    return 12;
  }
}
