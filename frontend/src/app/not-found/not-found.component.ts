import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="not-found-container" data-cy="not-found-page">
      <div class="error-content" data-cy="error-content">
        <div class="error-icon" data-cy="error-icon">
          <span class="card">🃏</span>
        </div>
        <h1 data-cy="error-code">404</h1>
        <h2 data-cy="error-title">Page Not Found</h2>
        <p data-cy="error-message">Looks like you've been dealt a bad hand. This page doesn't exist.</p>
        
        <div class="poker-message" data-cy="poker-message">
          <span class="chips">💰</span>
          <p>"Sometimes you have to fold and try a different route."</p>
        </div>

        <div class="action-buttons" data-cy="action-buttons">
          <a routerLink="/" class="btn-primary" data-cy="go-home-btn">
            🏠 Go Home
          </a>
          <a routerLink="/lobby" class="btn-secondary" data-cy="start-game-btn">
            🎮 Start New Game
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .not-found-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
      color: #fff;
      padding: 2rem;
    }

    .error-content {
      text-align: center;
      max-width: 500px;
    }

    .error-icon {
      margin-bottom: 1rem;
    }

    .card {
      font-size: 6rem;
      display: block;
      animation: float 3s ease-in-out infinite;
    }

    @keyframes float {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-10px); }
    }

    h1 {
      font-size: 8rem;
      font-weight: 800;
      margin: 0;
      background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      line-height: 1;
    }

    h2 {
      font-size: 2rem;
      margin: 0.5rem 0 1rem;
      color: #94a3b8;
    }

    p {
      font-size: 1.125rem;
      color: #64748b;
      margin-bottom: 2rem;
    }

    .poker-message {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
      padding: 1.5rem;
      margin-bottom: 2rem;
    }

    .chips {
      font-size: 2rem;
      display: block;
      margin-bottom: 0.5rem;
    }

    .poker-message p {
      margin: 0;
      font-style: italic;
      color: #ffd700;
    }

    .action-buttons {
      display: flex;
      gap: 1rem;
      justify-content: center;
      flex-wrap: wrap;
    }

    .btn-primary, .btn-secondary {
      padding: 1rem 2rem;
      font-size: 1rem;
      font-weight: 600;
      border: none;
      border-radius: 0.5rem;
      cursor: pointer;
      text-decoration: none;
      transition: all 0.2s ease;
      display: inline-block;
    }

    .btn-primary {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: #fff;
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
    }

    .btn-secondary {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
      border: 1px solid rgba(255, 255, 255, 0.2);
    }

    .btn-secondary:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  `]
})
export class NotFoundComponent {}
