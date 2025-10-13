import { Component, signal, computed } from '@angular/core';
import { CommonModule, PercentPipe } from '@angular/common';
import { RangeMatrixComponent } from '../range-matrix/range-matrix.component';
import { SelectedCard, SUIT_SYMBOLS, Suit, PRESET_RANGES } from '../models/analysis.models';

interface Scenario {
  id: string;
  name: string;
  description: string;
  difficulty: 'easy' | 'medium' | 'hard';
  heroHand: SelectedCard[];
  communityCards: SelectedCard[];
  villainRange: Set<string>;
  potSize: number;
  betToCall: number;
  position: string;
  correctAnswer: 'fold' | 'call' | 'raise';
  explanation: string;
}

@Component({
  selector: 'app-scenarios',
  standalone: true,
  imports: [CommonModule, PercentPipe, RangeMatrixComponent],
  template: `
    <div class="scenarios-page">
      <header class="page-header">
        <h2>Practice Scenarios</h2>
        <p class="subtitle">Test your decision-making with realistic poker spots</p>
      </header>

      @if (!currentScenario()) {
        <!-- Scenario Selection -->
        <div class="scenario-selection">
          <h3>Choose a Scenario</h3>
          <div class="difficulty-filters">
            <button 
              [class.active]="difficultyFilter() === 'all'"
              (click)="setDifficultyFilter('all')">
              All
            </button>
            <button 
              [class.active]="difficultyFilter() === 'easy'"
              (click)="setDifficultyFilter('easy')">
              Easy
            </button>
            <button 
              [class.active]="difficultyFilter() === 'medium'"
              (click)="setDifficultyFilter('medium')">
              Medium
            </button>
            <button 
              [class.active]="difficultyFilter() === 'hard'"
              (click)="setDifficultyFilter('hard')">
              Hard
            </button>
          </div>

          <div class="scenario-grid">
            @for (scenario of filteredScenarios(); track scenario.id) {
              <button 
                class="scenario-card"
                [class]="scenario.difficulty"
                (click)="selectScenario(scenario)">
                <div class="scenario-header">
                  <span class="scenario-name">{{ scenario.name }}</span>
                  <span class="difficulty-badge" [class]="scenario.difficulty">
                    {{ scenario.difficulty }}
                  </span>
                </div>
                <p class="scenario-desc">{{ scenario.description }}</p>
                <div class="scenario-meta">
                  <span>Pot: {{ scenario.potSize }}</span>
                  <span>Position: {{ scenario.position }}</span>
                </div>
              </button>
            }
          </div>
        </div>
      } @else {
        <!-- Active Scenario -->
        <div class="active-scenario">
          <button class="back-btn" (click)="clearScenario()">
            ← Back to Scenarios
          </button>

          <div class="scenario-content">
            <div class="scenario-info">
              <h3>{{ currentScenario()!.name }}</h3>
              <p>{{ currentScenario()!.description }}</p>
              
              <div class="game-info">
                <div class="info-item">
                  <span class="label">Position</span>
                  <span class="value">{{ currentScenario()!.position }}</span>
                </div>
                <div class="info-item">
                  <span class="label">Pot</span>
                  <span class="value">{{ currentScenario()!.potSize }}</span>
                </div>
                <div class="info-item">
                  <span class="label">To Call</span>
                  <span class="value">{{ currentScenario()!.betToCall }}</span>
                </div>
                <div class="info-item">
                  <span class="label">Pot Odds</span>
                  <span class="value">{{ calculatePotOdds() | percent:'1.0-0' }}</span>
                </div>
              </div>

              <!-- Your Hand -->
              <div class="hand-display">
                <h4>Your Hand</h4>
                <div class="cards">
                  @for (card of currentScenario()!.heroHand; track $index) {
                    <div class="card" [style.color]="getSuitColor(card.suit)">
                      {{ card.rank }}{{ getSuitSymbol(card.suit) }}
                    </div>
                  }
                </div>
              </div>

              <!-- Board -->
              @if (currentScenario()!.communityCards.length > 0) {
                <div class="board-display">
                  <h4>Board</h4>
                  <div class="cards">
                    @for (card of currentScenario()!.communityCards; track $index) {
                      <div class="card board-card" [style.color]="getSuitColor(card.suit)">
                        {{ card.rank }}{{ getSuitSymbol(card.suit) }}
                      </div>
                    }
                  </div>
                </div>
              }
            </div>

            <div class="range-display">
              <h4>Villain's Estimated Range</h4>
              <app-range-matrix
                title=""
                [selectedRange]="currentScenario()!.villainRange"
                [readonly]="true"
              />
            </div>
          </div>

          <!-- Decision Buttons -->
          @if (!showAnswer()) {
            <div class="decision-section">
              <h4>What's your play?</h4>
              <div class="decision-buttons">
                <button 
                  class="decision-btn fold"
                  (click)="submitAnswer('fold')">
                  Fold
                </button>
                <button 
                  class="decision-btn call"
                  (click)="submitAnswer('call')">
                  Call
                </button>
                <button 
                  class="decision-btn raise"
                  (click)="submitAnswer('raise')">
                  Raise
                </button>
              </div>
            </div>
          } @else {
            <!-- Answer Reveal -->
            <div class="answer-section" [class.correct]="isCorrect()" [class.incorrect]="!isCorrect()">
              <div class="answer-header">
                @if (isCorrect()) {
                  <span class="answer-icon">✓</span>
                  <span class="answer-text">Correct!</span>
                } @else {
                  <span class="answer-icon">✗</span>
                  <span class="answer-text">
                    Not quite. The correct play is {{ currentScenario()!.correctAnswer | uppercase }}.
                  </span>
                }
              </div>
              <p class="explanation">{{ currentScenario()!.explanation }}</p>
              <button class="next-btn" (click)="nextScenario()">
                Next Scenario →
              </button>
            </div>
          }
        </div>
      }

      <!-- Stats -->
      <div class="stats-bar">
        <span>Completed: {{ completedCount() }}</span>
        <span>Correct: {{ correctCount() }}</span>
        <span>Accuracy: {{ accuracy() | percent:'1.0-0' }}</span>
      </div>
    </div>
  `,
  styles: [`
    .scenarios-page {
      padding: 1.5rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    .page-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .page-header h2 {
      margin: 0;
      color: var(--text-primary, #fff);
    }

    .subtitle {
      color: var(--text-secondary, #aaa);
    }

    .difficulty-filters {
      display: flex;
      justify-content: center;
      gap: 0.5rem;
      margin-bottom: 1.5rem;
    }

    .difficulty-filters button {
      padding: 0.5rem 1rem;
      border: 1px solid var(--border-color, #333);
      border-radius: 6px;
      background: var(--surface-secondary, #252542);
      color: var(--text-secondary, #aaa);
      cursor: pointer;
    }

    .difficulty-filters button.active {
      background: var(--primary-color, #4f46e5);
      color: white;
      border-color: var(--primary-color, #4f46e5);
    }

    .scenario-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1rem;
    }

    .scenario-card {
      padding: 1rem;
      background: var(--surface-color, #1a1a2e);
      border: 1px solid var(--border-color, #333);
      border-radius: 12px;
      text-align: left;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .scenario-card:hover {
      border-color: var(--primary-color, #4f46e5);
      transform: translateY(-2px);
    }

    .scenario-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }

    .scenario-name {
      font-weight: 600;
      color: var(--text-primary, #fff);
    }

    .difficulty-badge {
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.7rem;
      text-transform: uppercase;
    }

    .difficulty-badge.easy { background: #22c55e; color: white; }
    .difficulty-badge.medium { background: #f59e0b; color: white; }
    .difficulty-badge.hard { background: #ef4444; color: white; }

    .scenario-desc {
      font-size: 0.85rem;
      color: var(--text-secondary, #aaa);
      margin: 0.5rem 0;
    }

    .scenario-meta {
      display: flex;
      gap: 1rem;
      font-size: 0.75rem;
      color: var(--text-muted, #666);
    }

    .active-scenario {
      animation: fadeIn 0.3s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .back-btn {
      padding: 0.5rem 1rem;
      background: transparent;
      border: 1px solid var(--border-color, #333);
      border-radius: 6px;
      color: var(--text-secondary, #aaa);
      cursor: pointer;
      margin-bottom: 1.5rem;
    }

    .scenario-content {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 2rem;
      margin-bottom: 2rem;
    }

    @media (max-width: 800px) {
      .scenario-content {
        grid-template-columns: 1fr;
      }
    }

    .scenario-info h3 {
      margin: 0 0 0.5rem;
      color: var(--text-primary, #fff);
    }

    .game-info {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 0.5rem;
      margin: 1rem 0;
    }

    .info-item {
      display: flex;
      flex-direction: column;
      padding: 0.5rem;
      background: var(--surface-secondary, #252542);
      border-radius: 6px;
    }

    .info-item .label {
      font-size: 0.7rem;
      color: var(--text-muted, #666);
    }

    .info-item .value {
      font-size: 1rem;
      font-weight: 600;
      color: var(--text-primary, #fff);
    }

    .hand-display, .board-display {
      margin: 1rem 0;
    }

    .hand-display h4, .board-display h4 {
      margin: 0 0 0.5rem;
      font-size: 0.9rem;
      color: var(--text-secondary, #aaa);
    }

    .cards {
      display: flex;
      gap: 0.5rem;
    }

    .card {
      width: 50px;
      height: 70px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: white;
      border-radius: 6px;
      font-size: 1.2rem;
      font-weight: bold;
      box-shadow: 0 2px 8px rgba(0,0,0,0.2);
    }

    .decision-section {
      text-align: center;
      padding: 1.5rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 12px;
    }

    .decision-section h4 {
      margin: 0 0 1rem;
      color: var(--text-primary, #fff);
    }

    .decision-buttons {
      display: flex;
      justify-content: center;
      gap: 1rem;
    }

    .decision-btn {
      padding: 1rem 2rem;
      font-size: 1rem;
      font-weight: 600;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .decision-btn.fold { background: #6b7280; color: white; }
    .decision-btn.call { background: #3b82f6; color: white; }
    .decision-btn.raise { background: #22c55e; color: white; }

    .decision-btn:hover {
      transform: scale(1.05);
    }

    .answer-section {
      padding: 1.5rem;
      border-radius: 12px;
      text-align: center;
    }

    .answer-section.correct {
      background: rgba(34, 197, 94, 0.1);
      border: 1px solid #22c55e;
    }

    .answer-section.incorrect {
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid #ef4444;
    }

    .answer-header {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    .answer-icon {
      font-size: 1.5rem;
    }

    .answer-text {
      font-size: 1.1rem;
      font-weight: 600;
      color: var(--text-primary, #fff);
    }

    .explanation {
      color: var(--text-secondary, #aaa);
      margin-bottom: 1rem;
    }

    .next-btn {
      padding: 0.75rem 1.5rem;
      background: var(--primary-color, #4f46e5);
      border: none;
      border-radius: 6px;
      color: white;
      font-weight: 600;
      cursor: pointer;
    }

    .stats-bar {
      display: flex;
      justify-content: center;
      gap: 2rem;
      margin-top: 2rem;
      padding: 1rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 8px;
      font-size: 0.9rem;
      color: var(--text-secondary, #aaa);
    }
  `]
})
export class ScenariosComponent {
  protected currentScenario = signal<Scenario | null>(null);
  protected userAnswer = signal<string | null>(null);
  protected showAnswer = signal(false);
  protected difficultyFilter = signal<'all' | 'easy' | 'medium' | 'hard'>('all');
  protected completedCount = signal(0);
  protected correctCount = signal(0);

  protected readonly scenarios: Scenario[] = [
    {
      id: '1',
      name: 'Premium Preflop',
      description: 'You have pocket aces in early position. Villain 3-bets from the button.',
      difficulty: 'easy',
      heroHand: [
        { rank: 'A', suit: 'SPADES', display: 'A♠' },
        { rank: 'A', suit: 'HEARTS', display: 'A♥' }
      ],
      communityCards: [],
      villainRange: new Set(PRESET_RANGES['premium'].hands),
      potSize: 150,
      betToCall: 100,
      position: 'UTG',
      correctAnswer: 'raise',
      explanation: 'With pocket aces facing a 3-bet, you should 4-bet for value. AA is the best starting hand and you want to build the pot.'
    },
    {
      id: '2',
      name: 'Flush Draw on Turn',
      description: 'You have a flush draw on the turn. Villain bets half pot.',
      difficulty: 'medium',
      heroHand: [
        { rank: 'A', suit: 'HEARTS', display: 'A♥' },
        { rank: 'K', suit: 'HEARTS', display: 'K♥' }
      ],
      communityCards: [
        { rank: 'J', suit: 'HEARTS', display: 'J♥' },
        { rank: '7', suit: 'HEARTS', display: '7♥' },
        { rank: '2', suit: 'SPADES', display: '2♠' },
        { rank: '9', suit: 'CLUBS', display: '9♣' }
      ],
      villainRange: new Set(PRESET_RANGES['broadway'].hands),
      potSize: 200,
      betToCall: 100,
      position: 'Button',
      correctAnswer: 'call',
      explanation: 'With 9 outs (flush) you have ~18% equity. The pot is offering 3:1 odds (25%), making this a profitable call. Plus you have implied odds if you hit.'
    },
    {
      id: '3',
      name: 'Marginal Hand vs Aggression',
      description: 'You have middle pair on a wet board. Villain overbets the pot.',
      difficulty: 'hard',
      heroHand: [
        { rank: 'T', suit: 'CLUBS', display: 'T♣' },
        { rank: 'T', suit: 'SPADES', display: 'T♠' }
      ],
      communityCards: [
        { rank: 'K', suit: 'HEARTS', display: 'K♥' },
        { rank: 'Q', suit: 'HEARTS', display: 'Q♥' },
        { rank: '8', suit: 'DIAMONDS', display: '8♦' }
      ],
      villainRange: new Set(PRESET_RANGES['buttonOpen'].hands),
      potSize: 100,
      betToCall: 150,
      position: 'Big Blind',
      correctAnswer: 'fold',
      explanation: 'Against an overbet on a KQ high board with flush draw, TT is too weak. Villain likely has Kx, Qx, or a draw. We need 40%+ equity but have only ~25% vs this range.'
    }
  ];

  protected filteredScenarios = computed(() => {
    const filter = this.difficultyFilter();
    if (filter === 'all') return this.scenarios;
    return this.scenarios.filter(s => s.difficulty === filter);
  });

  protected accuracy = computed(() => {
    if (this.completedCount() === 0) return 0;
    return this.correctCount() / this.completedCount();
  });

  protected calculatePotOdds(): number {
    const s = this.currentScenario();
    if (!s) return 0;
    return s.betToCall / (s.potSize + s.betToCall);
  }

  protected getSuitSymbol(suit: Suit): string {
    return SUIT_SYMBOLS[suit];
  }

  protected getSuitColor(suit: Suit): string {
    return suit === 'HEARTS' || suit === 'DIAMONDS' ? '#e53935' : '#424242';
  }

  protected selectScenario(scenario: Scenario): void {
    this.currentScenario.set(scenario);
    this.userAnswer.set(null);
    this.showAnswer.set(false);
  }

  protected clearScenario(): void {
    this.currentScenario.set(null);
    this.userAnswer.set(null);
    this.showAnswer.set(false);
  }

  protected submitAnswer(answer: 'fold' | 'call' | 'raise'): void {
    this.userAnswer.set(answer);
    this.showAnswer.set(true);
    this.completedCount.update(c => c + 1);
    if (this.isCorrect()) {
      this.correctCount.update(c => c + 1);
    }
  }

  protected isCorrect(): boolean {
    return this.userAnswer() === this.currentScenario()?.correctAnswer;
  }

  protected nextScenario(): void {
    const current = this.currentScenario();
    const filtered = this.filteredScenarios();
    const currentIndex = filtered.findIndex(s => s.id === current?.id);
    const nextIndex = (currentIndex + 1) % filtered.length;
    this.selectScenario(filtered[nextIndex]);
  }

  protected setDifficultyFilter(filter: 'all' | 'easy' | 'medium' | 'hard'): void {
    this.difficultyFilter.set(filter);
  }
}
