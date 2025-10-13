import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-analysis-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet
  ],
  template: `
    <div class="analysis-page">
      <!-- Navigation Tabs -->
      <nav class="analysis-nav" role="tablist" aria-label="Analysis tools">
        <a 
          routerLink="/analysis/equity"
          routerLinkActive="active"
          [routerLinkActiveOptions]="{ exact: true }"
          role="tab"
          class="nav-tab">
          <span class="tab-icon">📊</span>
          Equity Calculator
        </a>
        <a 
          routerLink="/analysis/ranges"
          routerLinkActive="active"
          role="tab"
          class="nav-tab">
          <span class="tab-icon">🎯</span>
          Range Builder
        </a>
        <a 
          routerLink="/analysis/scenarios"
          routerLinkActive="active"
          role="tab"
          class="nav-tab">
          <span class="tab-icon">🧪</span>
          Scenarios
        </a>
      </nav>

      <!-- Content -->
      <main class="analysis-content">
        <router-outlet />
      </main>

      <!-- Quick Tips -->
      <aside class="tips-panel" aria-label="Analysis tips">
        <h3>Quick Tips</h3>
        <ul class="tips-list">
          <li>
            <strong>Premium Hands:</strong> AA, KK, QQ, AKs - Raise from any position
          </li>
          <li>
            <strong>Position Matters:</strong> Play wider from the button, tighter from early position
          </li>
          <li>
            <strong>Pot Odds:</strong> Need 33% equity to profitably call a pot-sized bet
          </li>
          <li>
            <strong>Range Advantage:</strong> The player with more strong hands in their range has the advantage
          </li>
        </ul>
      </aside>
    </div>
  `,
  styles: [`
    .analysis-page {
      min-height: 100vh;
      background: var(--bg-primary, #0f0f1a);
      padding: 1rem;
    }

    .analysis-nav {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1.5rem;
      padding: 0.5rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 12px;
      max-width: 600px;
      margin-left: auto;
      margin-right: auto;
    }

    .nav-tab {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      text-decoration: none;
      color: var(--text-secondary, #aaa);
      font-size: 0.9rem;
      font-weight: 500;
      transition: all 0.2s ease;
    }

    .nav-tab:hover {
      background: var(--surface-secondary, #252542);
      color: var(--text-primary, #fff);
    }

    .nav-tab.active {
      background: var(--primary-color, #4f46e5);
      color: white;
    }

    .tab-icon {
      font-size: 1.1rem;
    }

    .analysis-content {
      max-width: 1200px;
      margin: 0 auto;
    }

    .tips-panel {
      max-width: 600px;
      margin: 2rem auto 0;
      padding: 1.25rem;
      background: var(--surface-color, #1a1a2e);
      border-radius: 12px;
      border-left: 3px solid var(--primary-color, #4f46e5);
    }

    .tips-panel h3 {
      margin: 0 0 0.75rem;
      font-size: 1rem;
      color: var(--text-primary, #fff);
    }

    .tips-list {
      margin: 0;
      padding-left: 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .tips-list li {
      font-size: 0.85rem;
      color: var(--text-secondary, #aaa);
      line-height: 1.5;
    }

    .tips-list strong {
      color: var(--text-primary, #fff);
    }

    @media (max-width: 600px) {
      .analysis-nav {
        flex-wrap: wrap;
      }
      
      .nav-tab {
        flex-basis: calc(50% - 0.25rem);
      }
      
      .tab-icon {
        display: none;
      }
    }
  `]
})
export class AnalysisPageComponent {}
