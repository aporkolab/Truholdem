import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="home-container" data-cy="home-page">
      <div class="hero-section">
        <div class="logo" data-cy="logo">
          <span class="logo-icon">♠️</span>
          <h1 data-cy="app-title">TruHoldem</h1>
        </div>
        <p class="tagline" data-cy="tagline">The Ultimate Texas Hold'em Experience</p>
      </div>

      <div class="cta-section" data-cy="cta-section">
        @if (isLoggedIn) {
          <div class="welcome-back" data-cy="welcome-message">
            <p>Welcome back, {{ currentUsername }}!</p>
          </div>
          <div class="action-buttons" data-cy="action-buttons">
            <button class="btn-primary" data-cy="new-game-btn" (click)="navigateToLobby()">
              🎮 Play Now
            </button>
            <button class="btn-secondary" data-cy="leaderboard-btn" (click)="navigateToLeaderboard()">
              🏆 Leaderboard
            </button>
            <button class="btn-secondary" data-cy="history-btn" (click)="navigateToHistory()">
              📜 Hand History
            </button>
          </div>
        } @else {
          <div class="auth-buttons" data-cy="auth-buttons">
            <button class="btn-primary" data-cy="login-btn" (click)="navigateToLogin()">
              🔑 Login
            </button>
            <button class="btn-secondary" data-cy="register-btn" (click)="navigateToRegister()">
              ✨ Create Account
            </button>
          </div>
          <p class="guest-play">
            <button class="btn-link" data-cy="guest-play-btn" (click)="playAsGuest()">
              Or play as guest →
            </button>
          </p>
        }
      </div>

      <div class="features-section" data-cy="features-section">
        <h2>Why TruHoldem?</h2>
        <div class="feature-grid">
          <div class="feature-card" data-cy="feature-ai">
            <span class="feature-icon">🤖</span>
            <h3>Smart AI Opponents</h3>
            <p>Challenge yourself against advanced bot players with realistic strategies.</p>
          </div>
          <div class="feature-card" data-cy="feature-stats">
            <span class="feature-icon">📊</span>
            <h3>Track Your Stats</h3>
            <p>Detailed statistics and hand history to improve your game.</p>
          </div>
          <div class="feature-card" data-cy="feature-compete">
            <span class="feature-icon">🏆</span>
            <h3>Compete & Win</h3>
            <p>Climb the leaderboard and prove you're the best.</p>
          </div>
          <div class="feature-card" data-cy="feature-replay">
            <span class="feature-icon">🎬</span>
            <h3>Hand Replay</h3>
            <p>Review and learn from your previous hands.</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .home-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
      color: #fff;
      padding: 2rem;
    }

    .hero-section {
      text-align: center;
      padding: 4rem 0;
    }

    .logo {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .logo-icon {
      font-size: 4rem;
    }

    h1 {
      font-size: 3.5rem;
      font-weight: 700;
      background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin: 0;
    }

    .tagline {
      font-size: 1.5rem;
      color: #94a3b8;
      margin-top: 0.5rem;
    }

    .cta-section {
      text-align: center;
      padding: 2rem 0;
    }

    .welcome-back {
      font-size: 1.25rem;
      margin-bottom: 1.5rem;
      color: #10b981;
    }

    .action-buttons, .auth-buttons {
      display: flex;
      gap: 1rem;
      justify-content: center;
      flex-wrap: wrap;
    }

    .btn-primary, .btn-secondary {
      padding: 1rem 2rem;
      font-size: 1.125rem;
      font-weight: 600;
      border: none;
      border-radius: 0.5rem;
      cursor: pointer;
      transition: all 0.2s ease;
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

    .btn-link {
      background: none;
      border: none;
      color: #94a3b8;
      cursor: pointer;
      font-size: 1rem;
      text-decoration: underline;
    }

    .btn-link:hover {
      color: #fff;
    }

    .guest-play {
      margin-top: 1.5rem;
    }

    .features-section {
      max-width: 1200px;
      margin: 4rem auto;
    }

    .features-section h2 {
      text-align: center;
      font-size: 2rem;
      margin-bottom: 2rem;
      color: #ffd700;
    }

    .feature-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
    }

    .feature-card {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
      padding: 2rem;
      text-align: center;
      transition: transform 0.2s ease;
    }

    .feature-card:hover {
      transform: translateY(-4px);
      background: rgba(255, 255, 255, 0.08);
    }

    .feature-icon {
      font-size: 3rem;
      display: block;
      margin-bottom: 1rem;
    }

    .feature-card h3 {
      font-size: 1.25rem;
      margin-bottom: 0.5rem;
      color: #fff;
    }

    .feature-card p {
      color: #94a3b8;
      font-size: 0.95rem;
      line-height: 1.5;
    }
  `]
})
export class HomeComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  get isLoggedIn(): boolean {
    return this.authService.isAuthenticated();
  }

  get currentUsername(): string {
    return this.authService.getCurrentUserValue()?.username ?? 'Player';
  }

  navigateToLobby(): void {
    this.router.navigate(['/lobby']);
  }

  navigateToLeaderboard(): void {
    this.router.navigate(['/leaderboard']);
  }

  navigateToHistory(): void {
    this.router.navigate(['/history']);
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  playAsGuest(): void {
    
    this.router.navigate(['/lobby']);
  }
}
