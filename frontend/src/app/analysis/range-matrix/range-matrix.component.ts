import { Component, input, output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { 
  RANKS, 
  Rank, 
  getCellNotation, 
  getComboCount,
  calculateRangePercentage,
  PRESET_RANGES,
  TOTAL_COMBOS
} from '../models/analysis.models';

@Component({
  selector: 'app-range-matrix',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="range-matrix" aria-label="Poker Hand Range Matrix">
      <!-- Title -->
      <h3 class="matrix-title">{{ title() }}</h3>

      <!-- Quick Select Buttons -->
      <div class="quick-select" role="toolbar" aria-label="Preset range selections">
        <button
          type="button"
          class="preset-btn"
          [class.active]="activePreset() === 'premium'"
          (click)="selectPreset('premium')"
          aria-pressed="false">
          Premium
        </button>
        <button
          type="button"
          class="preset-btn"
          [class.active]="activePreset() === 'broadway'"
          (click)="selectPreset('broadway')"
          aria-pressed="false">
          Broadway
        </button>
        <button
          type="button"
          class="preset-btn"
          [class.active]="activePreset() === 'pairs'"
          (click)="selectPreset('pairs')"
          aria-pressed="false">
          All Pairs
        </button>
        <button
          type="button"
          class="preset-btn"
          [class.active]="activePreset() === 'suitedConnectors'"
          (click)="selectPreset('suitedConnectors')"
          aria-pressed="false">
          Suited Conn.
        </button>
        <button
          type="button"
          class="preset-btn"
          [class.active]="activePreset() === 'buttonOpen'"
          (click)="selectPreset('buttonOpen')"
          aria-pressed="false">
          BTN Open
        </button>
        <button
          type="button"
          class="clear-btn"
          (click)="clearAll()">
          Clear
        </button>
      </div>

      <!-- 13x13 Grid -->
      <div
        class="matrix-grid"
        role="grid"
        (mousedown)="startDrag($event)"
        (mouseup)="endDrag()"
        (mouseleave)="endDrag()">
        @for (row of ranks; track row; let rowIdx = $index) {
          @for (col of ranks; track col; let colIdx = $index) {
            <div 
              class="matrix-cell"
              [class.selected]="isCellSelected(row, col)"
              [class.suited]="rowIdx < colIdx"
              [class.pair]="rowIdx === colIdx"
              [class.offsuit]="rowIdx > colIdx"
              [class.highlighted]="highlightedCell() === getCellNotation(row, col)"
              [style.backgroundColor]="getCellColor(row, col)"
              [attr.aria-label]="getCellAriaLabel(row, col)"
              [attr.aria-pressed]="isCellSelected(row, col)"
              role="gridcell"
              tabindex="0"
              (click)="toggleCell(row, col)"
              (mouseenter)="onCellHover(row, col)"
              (mouseleave)="onCellLeave()"
              (keydown.enter)="toggleCell(row, col)"
              (keydown.space)="toggleCell(row, col); $event.preventDefault()">
              <span class="cell-text">{{ getCellText(row, col) }}</span>
              <span class="cell-combos">{{ getComboCount(row, col) }}</span>
            </div>
          }
        }
      </div>

      <!-- Legend -->
      <div class="matrix-legend" aria-hidden="true">
        <span class="legend-item suited">
          <span class="legend-color"></span> Suited
        </span>
        <span class="legend-item pair">
          <span class="legend-color"></span> Pairs
        </span>
        <span class="legend-item offsuit">
          <span class="legend-color"></span> Offsuit
        </span>
      </div>

      <!-- Stats -->
      <div class="range-stats" aria-live="polite">
        <span class="stat">
          <strong>{{ selectedComboCount() }}</strong> / {{ totalCombos }} combos
        </span>
        <span class="stat percentage">
          <strong>{{ rangePercentageDisplay() }}</strong>
        </span>
      </div>

      <!-- Notation Input -->
      <div class="notation-input">
        <label for="range-notation" class="sr-only">Range notation</label>
        <input 
          id="range-notation"
          type="text"
          [ngModel]="rangeNotation()"
          (ngModelChange)="onNotationChange($event)"
          placeholder="AA, KK, AKs, QJo..."
          aria-describedby="notation-help"
        />
        <small id="notation-help" class="notation-help">
          Format: AA, AKs (suited), AKo (offsuit), AK (both)
        </small>
      </div>
    </div>
  `,
  styles: [`
    .range-matrix {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding: 1rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 12px;
      max-width: 500px;
    }

    .matrix-title {
      margin: 0;
      font-size: 1.1rem;
      color: var(--text-primary, #fff);
      font-weight: 600;
    }

    .quick-select {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .preset-btn, .clear-btn {
      padding: 0.4rem 0.8rem;
      font-size: 0.75rem;
      border: 1px solid var(--border-color, #333);
      border-radius: 6px;
      background: var(--surface-secondary, #252542);
      color: var(--text-secondary, #aaa);
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .preset-btn:hover, .clear-btn:hover {
      background: var(--surface-hover, #333);
      color: var(--text-primary, #fff);
    }

    .preset-btn.active {
      background: var(--primary-color, #4f46e5);
      color: white;
      border-color: var(--primary-color, #4f46e5);
    }

    .clear-btn {
      background: var(--danger-bg, #3a2020);
      border-color: var(--danger-color, #ef4444);
      color: var(--danger-color, #ef4444);
    }

    .clear-btn:hover {
      background: var(--danger-color, #ef4444);
      color: white;
    }

    .matrix-grid {
      display: grid;
      grid-template-columns: repeat(13, 1fr);
      gap: 2px;
      background: var(--grid-bg, #111);
      padding: 2px;
      border-radius: 8px;
      user-select: none;
    }

    .matrix-cell {
      aspect-ratio: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-size: 0.65rem;
      font-weight: 600;
      cursor: pointer;
      border-radius: 3px;
      transition: all 0.15s ease;
      position: relative;
      min-width: 28px;
      min-height: 28px;
    }

    .matrix-cell.suited {
      background: var(--suited-bg, #1e3a5f);
    }

    .matrix-cell.pair {
      background: var(--pair-bg, #3d1f5c);
    }

    .matrix-cell.offsuit {
      background: var(--offsuit-bg, #2d3748);
    }

    .matrix-cell.selected {
      background: var(--selected-bg, #22c55e) !important;
      color: white;
      box-shadow: inset 0 0 0 2px rgba(255,255,255,0.3);
    }

    .matrix-cell.highlighted {
      box-shadow: 0 0 0 2px var(--highlight-color, #fbbf24);
    }

    .matrix-cell:hover {
      transform: scale(1.05);
      z-index: 1;
      box-shadow: 0 2px 8px rgba(0,0,0,0.3);
    }

    .matrix-cell:focus {
      outline: 2px solid var(--focus-color, #60a5fa);
      outline-offset: 1px;
    }

    .cell-text {
      color: var(--text-primary, #fff);
      line-height: 1;
    }

    .cell-combos {
      font-size: 0.5rem;
      color: var(--text-muted, #888);
      opacity: 0.7;
    }

    .matrix-cell.selected .cell-combos {
      color: rgba(255,255,255,0.8);
    }

    .matrix-legend {
      display: flex;
      justify-content: center;
      gap: 1.5rem;
      font-size: 0.75rem;
      color: var(--text-secondary, #aaa);
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }

    .legend-color {
      width: 12px;
      height: 12px;
      border-radius: 2px;
    }

    .legend-item.suited .legend-color {
      background: var(--suited-bg, #1e3a5f);
    }

    .legend-item.pair .legend-color {
      background: var(--pair-bg, #3d1f5c);
    }

    .legend-item.offsuit .legend-color {
      background: var(--offsuit-bg, #2d3748);
    }

    .range-stats {
      display: flex;
      justify-content: space-between;
      padding: 0.75rem;
      background: var(--surface-secondary, #252542);
      border-radius: 6px;
      font-size: 0.85rem;
      color: var(--text-secondary, #aaa);
    }

    .range-stats strong {
      color: var(--text-primary, #fff);
    }

    .range-stats .percentage strong {
      color: var(--primary-color, #22c55e);
    }

    .notation-input {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .notation-input input {
      padding: 0.6rem 0.8rem;
      border: 1px solid var(--border-color, #333);
      border-radius: 6px;
      background: var(--input-bg, #1a1a2e);
      color: var(--text-primary, #fff);
      font-size: 0.85rem;
      font-family: 'Monaco', 'Menlo', monospace;
    }

    .notation-input input:focus {
      outline: none;
      border-color: var(--primary-color, #4f46e5);
      box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.2);
    }

    .notation-help {
      font-size: 0.7rem;
      color: var(--text-muted, #666);
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }

    @media (max-width: 480px) {
      .matrix-cell {
        min-width: 22px;
        min-height: 22px;
        font-size: 0.55rem;
      }
      
      .cell-combos {
        display: none;
      }
      
      .quick-select {
        justify-content: center;
      }
    }
  `]
})
export class RangeMatrixComponent {
  
  title = input<string>('Hand Range');
  selectedRange = input<Set<string>>(new Set());
  readonly = input<boolean>(false);

  
  rangeChange = output<Set<string>>();
  cellHover = output<string | null>();
  presetSelected = output<string>();

  
  protected readonly ranks = RANKS;
  protected readonly totalCombos = TOTAL_COMBOS;
  protected activePreset = signal<string | null>(null);
  protected highlightedCell = signal<string | null>(null);
  
  private isDragging = false;
  private dragSelectMode = true; 

  
  protected readonly rangeNotation = computed(() => {
    return Array.from(this.selectedRange()).sort().join(', ');
  });

  protected readonly selectedComboCount = computed(() => {
    let count = 0;
    for (const notation of this.selectedRange()) {
      const parsed = this.parseNotation(notation);
      if (!parsed) continue;
      count += this.getNotationComboCount(notation);
    }
    return count;
  });

  protected readonly rangePercentageDisplay = computed(() => {
    const percentage = calculateRangePercentage(this.selectedRange());
    return `${(percentage * 100).toFixed(1)}%`;
  });

  
  protected getCellNotation(row: Rank, col: Rank): string {
    return getCellNotation(row, col);
  }

  protected getComboCount(row: Rank, col: Rank): number {
    return getComboCount(row, col);
  }

  protected getCellText(row: Rank, col: Rank): string {
    const rowIdx = RANKS.indexOf(row);
    const colIdx = RANKS.indexOf(col);
    
    if (rowIdx === colIdx) {
      return `${row}${row}`;
    } else if (rowIdx < colIdx) {
      return `${row}${col}s`;
    } else {
      return `${col}${row}o`;
    }
  }

  protected getCellColor(row: Rank, col: Rank): string {
    const notation = getCellNotation(row, col);
    if (this.selectedRange().has(notation)) {
      return ''; 
    }
    return '';
  }

  protected getCellAriaLabel(row: Rank, col: Rank): string {
    const notation = this.getCellText(row, col);
    const combos = this.getComboCount(row, col);
    const selected = this.isCellSelected(row, col) ? 'selected' : 'not selected';
    return `${notation}, ${combos} combinations, ${selected}`;
  }

  protected isCellSelected(row: Rank, col: Rank): boolean {
    const notation = getCellNotation(row, col);
    return this.selectedRange().has(notation);
  }

  
  protected toggleCell(row: Rank, col: Rank): void {
    if (this.readonly()) return;
    
    const notation = getCellNotation(row, col);
    const newRange = new Set(this.selectedRange());
    
    if (newRange.has(notation)) {
      newRange.delete(notation);
    } else {
      newRange.add(notation);
    }
    
    this.activePreset.set(null);
    this.rangeChange.emit(newRange);
  }

  protected startDrag(event: MouseEvent): void {
    if (this.readonly()) return;
    this.isDragging = true;
    
    const target = event.target as HTMLElement;
    if (target.classList.contains('matrix-cell')) {
      this.dragSelectMode = !target.classList.contains('selected');
    }
  }

  protected endDrag(): void {
    this.isDragging = false;
  }

  protected onCellHover(row: Rank, col: Rank): void {
    const notation = getCellNotation(row, col);
    this.highlightedCell.set(notation);
    this.cellHover.emit(notation);
    
    
    if (this.isDragging && !this.readonly()) {
      const newRange = new Set(this.selectedRange());
      if (this.dragSelectMode) {
        newRange.add(notation);
      } else {
        newRange.delete(notation);
      }
      this.activePreset.set(null);
      this.rangeChange.emit(newRange);
    }
  }

  protected onCellLeave(): void {
    this.highlightedCell.set(null);
    this.cellHover.emit(null);
  }

  protected selectPreset(presetKey: string): void {
    if (this.readonly()) return;
    
    const preset = PRESET_RANGES[presetKey];
    if (preset) {
      this.activePreset.set(presetKey);
      this.rangeChange.emit(new Set(preset.hands));
      this.presetSelected.emit(presetKey);
    }
  }

  protected clearAll(): void {
    if (this.readonly()) return;
    this.activePreset.set(null);
    this.rangeChange.emit(new Set());
  }

  protected onNotationChange(notation: string): void {
    if (this.readonly()) return;
    
    const hands = notation.split(',')
      .map(h => h.trim().toUpperCase())
      .filter(h => h.length >= 2);
    
    const newRange = new Set<string>();
    for (const hand of hands) {
      const parsed = this.parseNotation(hand);
      if (parsed) {
        newRange.add(parsed);
      }
    }
    
    this.activePreset.set(null);
    this.rangeChange.emit(newRange);
  }

  private parseNotation(notation: string): string | null {
    const clean = notation.trim().toUpperCase();
    if (clean.length < 2) return null;
    
    const r1 = clean[0] as Rank;
    const r2 = clean[1] as Rank;
    
    if (!RANKS.includes(r1) || !RANKS.includes(r2)) return null;
    
    if (r1 === r2) {
      return `${r1}${r2}`;
    }
    
    const highIdx = RANKS.indexOf(r1);
    const lowIdx = RANKS.indexOf(r2);
    const high = highIdx < lowIdx ? r1 : r2;
    const low = highIdx < lowIdx ? r2 : r1;
    
    if (clean.endsWith('S')) {
      return `${high}${low}s`;
    } else if (clean.endsWith('O')) {
      return `${high}${low}o`;
    }
    
    return `${high}${low}s`; 
  }

  private getNotationComboCount(notation: string): number {
    if (notation.length === 2 || notation[0] === notation[1]) {
      return 6; 
    }
    if (notation.endsWith('s')) {
      return 4; 
    }
    return 12; 
  }
}
