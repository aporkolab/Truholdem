import { Component, input, output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  SelectedCard, 
  Rank, 
  Suit, 
  RANKS, 
  SUITS,
  SUIT_SYMBOLS,
  SUIT_COLORS
} from '../models/analysis.models';

@Component({
  selector: 'app-card-selector',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card-selector" [attr.aria-label]="label()">
      <!-- Selected Cards Display -->
      <div class="selected-cards">
        <h4 class="section-label">{{ label() }}</h4>
        <div class="cards-display" role="list">
          @for (card of selectedCards(); track $index; let i = $index) {
            <div 
              class="selected-card"
              [style.color]="getSuitColor(card.suit)"
              role="listitem"
              tabindex="0"
              (click)="removeCard(i)"
              (keydown.enter)="removeCard(i)"
              (keydown.backspace)="removeCard(i)"
              [attr.aria-label]="card.display + ', click to remove'">
              <span class="card-rank">{{ card.rank }}</span>
              <span class="card-suit">{{ getSuitSymbol(card.suit) }}</span>
              <button 
                type="button" 
                class="remove-btn" 
                aria-label="Remove card"
                (click)="removeCard(i); $event.stopPropagation()">
                ×
              </button>
            </div>
          }
          @for (slot of emptySlots(); track $index) {
            <div class="empty-slot" role="listitem" aria-label="Empty card slot">
              <span class="slot-icon">?</span>
            </div>
          }
        </div>
      </div>

      <!-- Card Picker -->
      @if (showPicker()) {
        <div class="card-picker" role="grid" aria-label="Card selection grid">
          <!-- Suit Headers -->
          <div class="suit-headers">
            @for (suit of suits; track suit) {
              <div 
                class="suit-header"
                [style.color]="getSuitColor(suit)">
                {{ getSuitSymbol(suit) }}
              </div>
            }
          </div>
          
          <!-- Card Grid -->
          <div class="picker-grid">
            @for (rank of ranks; track rank) {
              <div class="rank-row">
                <span class="rank-label">{{ rank }}</span>
                @for (suit of suits; track suit) {
                  <button
                    type="button"
                    class="card-btn"
                    [class.selected]="isCardSelected(rank, suit)"
                    [class.disabled]="isCardDisabled(rank, suit)"
                    [style.color]="getSuitColor(suit)"
                    [disabled]="isCardDisabled(rank, suit) || !canSelectMore()"
                    (click)="selectCard(rank, suit)"
                    [attr.aria-label]="rank + ' of ' + suit"
                    [attr.aria-pressed]="isCardSelected(rank, suit)">
                    {{ getSuitSymbol(suit) }}
                  </button>
                }
              </div>
            }
          </div>
        </div>
      }

      <!-- Toggle Picker Button -->
      <button 
        type="button"
        class="toggle-picker-btn"
        (click)="togglePicker()"
        [attr.aria-expanded]="showPicker()">
        {{ showPicker() ? 'Hide Cards' : 'Select Cards' }}
        <span class="toggle-icon">{{ showPicker() ? '▲' : '▼' }}</span>
      </button>

      <!-- Quick Actions -->
      @if (selectedCards().length > 0) {
        <button 
          type="button"
          class="clear-btn"
          (click)="clearAll()">
          Clear All
        </button>
      }
    </div>
  `,
  styles: [`
    .card-selector {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      padding: 1rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 12px;
    }

    .section-label {
      margin: 0 0 0.5rem 0;
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-secondary, #aaa);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .selected-cards {
      display: flex;
      flex-direction: column;
    }

    .cards-display {
      display: flex;
      gap: 0.5rem;
      min-height: 60px;
    }

    .selected-card {
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      width: 45px;
      height: 60px;
      background: var(--card-bg, #fff);
      border-radius: 6px;
      font-weight: bold;
      cursor: pointer;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .selected-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }

    .selected-card:focus {
      outline: 2px solid var(--focus-color, #60a5fa);
      outline-offset: 2px;
    }

    .card-rank {
      font-size: 1.2rem;
      line-height: 1;
    }

    .card-suit {
      font-size: 1rem;
      margin-left: 1px;
    }

    .remove-btn {
      position: absolute;
      top: -6px;
      right: -6px;
      width: 18px;
      height: 18px;
      border: none;
      border-radius: 50%;
      background: var(--danger-color, #ef4444);
      color: white;
      font-size: 14px;
      line-height: 1;
      cursor: pointer;
      opacity: 0;
      transition: opacity 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .selected-card:hover .remove-btn,
    .selected-card:focus .remove-btn {
      opacity: 1;
    }

    .empty-slot {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 45px;
      height: 60px;
      background: var(--surface-secondary, #252542);
      border: 2px dashed var(--border-color, #333);
      border-radius: 6px;
    }

    .slot-icon {
      font-size: 1.5rem;
      color: var(--text-muted, #666);
    }

    .card-picker {
      background: var(--surface-secondary, #252542);
      border-radius: 8px;
      padding: 0.75rem;
      animation: slideDown 0.2s ease-out;
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .suit-headers {
      display: grid;
      grid-template-columns: 30px repeat(4, 1fr);
      gap: 4px;
      margin-bottom: 0.5rem;
      padding-left: 30px;
    }

    .suit-header {
      text-align: center;
      font-size: 1.2rem;
      font-weight: bold;
    }

    .picker-grid {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .rank-row {
      display: grid;
      grid-template-columns: 30px repeat(4, 1fr);
      gap: 4px;
      align-items: center;
    }

    .rank-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-secondary, #aaa);
      text-align: center;
    }

    .card-btn {
      aspect-ratio: 1;
      border: 1px solid var(--border-color, #333);
      border-radius: 4px;
      background: var(--card-bg-dark, #1a1a2e);
      font-size: 1rem;
      cursor: pointer;
      transition: all 0.15s ease;
      min-height: 32px;
    }

    .card-btn:hover:not(:disabled) {
      background: var(--surface-hover, #333);
      transform: scale(1.05);
    }

    .card-btn:focus {
      outline: 2px solid var(--focus-color, #60a5fa);
      outline-offset: 1px;
    }

    .card-btn.selected {
      background: var(--selected-bg, #22c55e);
      border-color: var(--selected-border, #16a34a);
      color: white !important;
    }

    .card-btn.disabled,
    .card-btn:disabled {
      opacity: 0.3;
      cursor: not-allowed;
      background: var(--disabled-bg, #2d2d2d);
    }

    .toggle-picker-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 0.5rem 1rem;
      border: 1px solid var(--border-color, #333);
      border-radius: 6px;
      background: var(--surface-secondary, #252542);
      color: var(--text-secondary, #aaa);
      font-size: 0.85rem;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .toggle-picker-btn:hover {
      background: var(--surface-hover, #333);
      color: var(--text-primary, #fff);
    }

    .toggle-icon {
      font-size: 0.7rem;
    }

    .clear-btn {
      padding: 0.4rem 0.8rem;
      border: 1px solid var(--danger-color, #ef4444);
      border-radius: 6px;
      background: transparent;
      color: var(--danger-color, #ef4444);
      font-size: 0.8rem;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .clear-btn:hover {
      background: var(--danger-color, #ef4444);
      color: white;
    }

    @media (max-width: 360px) {
      .card-btn {
        font-size: 0.85rem;
        min-height: 28px;
      }
      
      .selected-card {
        width: 38px;
        height: 52px;
      }
      
      .card-rank {
        font-size: 1rem;
      }
    }
  `]
})
export class CardSelectorComponent {
  
  label = input<string>('Cards');
  maxCards = input<number>(2);
  selectedCards = input<SelectedCard[]>([]);
  deadCards = input<SelectedCard[]>([]); 

  
  cardsChange = output<SelectedCard[]>();

  
  protected showPicker = signal(false);
  protected readonly ranks = RANKS;
  protected readonly suits = SUITS;

  
  protected readonly emptySlots = computed(() => {
    const count = this.maxCards() - this.selectedCards().length;
    return Array(Math.max(0, count)).fill(null);
  });

  protected readonly canSelectMore = computed(() => {
    return this.selectedCards().length < this.maxCards();
  });

  
  protected getSuitSymbol(suit: Suit): string {
    return SUIT_SYMBOLS[suit];
  }

  protected getSuitColor(suit: Suit): string {
    return SUIT_COLORS[suit];
  }

  protected isCardSelected(rank: Rank, suit: Suit): boolean {
    return this.selectedCards().some(c => c.rank === rank && c.suit === suit);
  }

  protected isCardDisabled(rank: Rank, suit: Suit): boolean {
    
    if (this.isCardSelected(rank, suit)) return true;
    
    
    return this.deadCards().some(c => c.rank === rank && c.suit === suit);
  }

  protected selectCard(rank: Rank, suit: Suit): void {
    if (!this.canSelectMore() || this.isCardDisabled(rank, suit)) return;

    const card: SelectedCard = {
      rank,
      suit,
      display: `${rank}${SUIT_SYMBOLS[suit]}`
    };

    this.cardsChange.emit([...this.selectedCards(), card]);
  }

  protected removeCard(index: number): void {
    const newCards = this.selectedCards().filter((_, i) => i !== index);
    this.cardsChange.emit(newCards);
  }

  protected clearAll(): void {
    this.cardsChange.emit([]);
  }

  protected togglePicker(): void {
    this.showPicker.update(v => !v);
  }
}
