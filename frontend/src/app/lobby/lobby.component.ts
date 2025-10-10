import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { PlayerService } from '../services/player.service';
import { AuthService } from '../services/auth.service';
import { ApiConfigService } from '../services/api-config.service';

export interface PlayerInfo {
  name: string;
  startingChips: number;
  isBot: boolean;
}

@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="lobby-container" data-cy="lobby-page">
      <div class="lobby-header">
        <h1 data-cy="lobby-title">🎰 Game Lobby</h1>
        <p class="subtitle">Set up your poker table</p>
      </div>

      <div class="setup-section">
        <div class="players-card" data-cy="players-section">
          <h2>👥 Players</h2>
          
          <div class="player-list" data-cy="player-list">
            @for (player of players; track $index) {
              <div class="player-item" 
                   [class.human]="!player.isBot"
                   [attr.data-cy]="player.isBot ? 'bot-player-' + $index : 'human-player'"
                   [attr.data-player-type]="player.isBot ? 'bot' : 'human'">
                <div class="player-info">
                  <span class="player-icon">{{ player.isBot ? '🤖' : '👤' }}</span>
                  <input 
                    type="text" 
                    [(ngModel)]="player.name" 
                    [readonly]="!player.isBot"
                    class="player-name-input"
                    [class.readonly]="!player.isBot"
                    [attr.data-cy]="'player-name-input-' + $index">
                </div>
                <div class="player-chips">
                  <span class="chip-icon">💰</span>
                  <input 
                    type="number" 
                    [(ngModel)]="player.startingChips" 
                    min="100" 
                    max="10000" 
                    step="100"
                    class="chips-input"
                    [attr.data-cy]="'player-chips-input-' + $index"
                    data-cy="player-chips-input">
                </div>
                @if (players.length > 2 && player.isBot) {
                  <button class="btn-remove" 
                          (click)="removePlayer($index)" 
                          title="Remove player"
                          [attr.data-cy]="'remove-player-' + $index">
                    ❌
                  </button>
                }
              </div>
            }
          </div>

          @if (players.length < maxPlayers) {
            <button class="btn-add-player" (click)="addBot()" data-cy="add-bot-btn">
              ➕ Add Bot Player
            </button>
          }
        </div>

        <div class="game-settings-card" data-cy="settings-section">
          <h2>⚙️ Game Settings</h2>
          
          <div class="setting-item">
            <label for="small-blind-input">Small Blind</label>
            <div class="setting-value">
              <span class="chip-icon">💰</span>
              <input
                type="number"
                [(ngModel)]="smallBlind"
                min="5"
                max="100"
                step="5"
                id="small-blind-input"
                data-cy="small-blind-input">
            </div>
          </div>

          <div class="setting-item">
            <span class="setting-label" aria-hidden="true">Big Blind</span>
            <div class="setting-value">
              <span class="chip-icon">💰</span>
              <span class="calculated" data-cy="big-blind-display" aria-label="Big Blind value">{{ smallBlind * 2 }}</span>
            </div>
          </div>

          <div class="setting-item">
            <label for="difficulty-select">Bot Difficulty</label>
            <select [(ngModel)]="botDifficulty" id="difficulty-select" data-cy="difficulty-select">
              <option value="easy">Easy 🌱</option>
              <option value="medium">Medium 🎯</option>
              <option value="hard">Hard 🔥</option>
            </select>
          </div>
        </div>
      </div>

      <div class="action-section" data-cy="action-section">
        <button 
          class="btn-start-game" 
          (click)="startGame()"
          [disabled]="isStarting || players.length < 2"
          data-cy="start-game-btn">
          @if (isStarting) {
            <span class="spinner"></span> Starting...
          } @else {
            🎮 Start Game
          }
        </button>
        
        @if (errorMessage) {
          <div class="error-message" data-cy="error-message" role="alert">
            ⚠️ {{ errorMessage }}
          </div>
        }
      </div>

      <div class="back-link">
        <button class="btn-back" (click)="goBack()" data-cy="back-btn">← Back to Home</button>
      </div>
    </div>
  `,
  styles: [`
    .lobby-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
      color: #fff;
      padding: 2rem;
    }

    .lobby-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .lobby-header h1 {
      font-size: 2.5rem;
      margin: 0;
      background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .subtitle {
      color: #94a3b8;
      margin-top: 0.5rem;
    }

    .setup-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
      gap: 2rem;
      max-width: 900px;
      margin: 0 auto;
    }

    .players-card, .game-settings-card {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 1rem;
      padding: 1.5rem;
    }

    h2 {
      font-size: 1.25rem;
      margin: 0 0 1rem;
      color: #ffd700;
    }

    .player-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .player-item {
      display: flex;
      align-items: center;
      gap: 1rem;
      background: rgba(0, 0, 0, 0.2);
      padding: 0.75rem 1rem;
      border-radius: 0.5rem;
      border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .player-item.human {
      border-color: #10b981;
      background: rgba(16, 185, 129, 0.1);
    }

    .player-info {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex: 1;
    }

    .player-icon {
      font-size: 1.5rem;
    }

    .player-name-input {
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 0.25rem;
      padding: 0.5rem;
      color: #fff;
      width: 120px;
    }

    .player-name-input.readonly {
      background: transparent;
      border-color: transparent;
    }

    .player-chips {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .chip-icon {
      font-size: 1.25rem;
    }

    .chips-input {
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 0.25rem;
      padding: 0.5rem;
      color: #fff;
      width: 80px;
      text-align: right;
    }

    .btn-remove {
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.25rem;
      font-size: 1rem;
      opacity: 0.6;
      transition: opacity 0.2s;
    }

    .btn-remove:hover {
      opacity: 1;
    }

    .btn-add-player {
      width: 100%;
      margin-top: 1rem;
      padding: 0.75rem;
      background: rgba(255, 255, 255, 0.1);
      border: 1px dashed rgba(255, 255, 255, 0.3);
      border-radius: 0.5rem;
      color: #94a3b8;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-add-player:hover {
      background: rgba(255, 255, 255, 0.15);
      color: #fff;
    }

    .setting-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 0;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .setting-item:last-child {
      border-bottom: none;
    }

    .setting-value {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .setting-value input, .setting-value select {
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 0.25rem;
      padding: 0.5rem;
      color: #fff;
      width: 80px;
    }

    .setting-value select {
      width: auto;
    }

    .calculated {
      color: #10b981;
      font-weight: 600;
    }

    .action-section {
      text-align: center;
      margin-top: 2rem;
    }

    .btn-start-game {
      padding: 1rem 3rem;
      font-size: 1.25rem;
      font-weight: 600;
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: #fff;
      border: none;
      border-radius: 0.5rem;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-start-game:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
    }

    .btn-start-game:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .spinner {
      display: inline-block;
      width: 1rem;
      height: 1rem;
      border: 2px solid #fff;
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .error-message {
      margin-top: 1rem;
      padding: 0.75rem 1rem;
      background: rgba(239, 68, 68, 0.2);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 0.5rem;
      color: #fca5a5;
      display: inline-block;
    }

    .back-link {
      text-align: center;
      margin-top: 2rem;
    }

    .btn-back {
      background: none;
      border: none;
      color: #94a3b8;
      cursor: pointer;
      font-size: 1rem;
    }

    .btn-back:hover {
      color: #fff;
    }
  `]
})
export class LobbyComponent {
  private readonly http = inject(HttpClient);
  private readonly playerService = inject(PlayerService);
  private readonly authService = inject(AuthService);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly router = inject(Router);

  readonly maxPlayers = 4;
  readonly minPlayers = 2;

  players: PlayerInfo[] = [];
  smallBlind = 10;
  botDifficulty: 'easy' | 'medium' | 'hard' = 'medium';
  isStarting = false;
  errorMessage = '';

  private readonly hungarianNames = [
    'Béla', 'István', 'László', 'József', 'Ferenc', 'Zoltán', 'Gábor',
    'Attila', 'Tamás', 'Péter', 'András', 'Sándor', 'Tibor', 'Imre'
  ];

  constructor() {
    this.initializePlayers();
  }

  private initializePlayers(): void {
    const user = this.authService.getCurrentUserValue();
    const humanName = user?.username ?? this.generateRandomName();

    this.players = [
      { name: humanName, startingChips: 1000, isBot: false },
      { name: `Bot ${this.generateRandomName()}`, startingChips: 1000, isBot: true },
      { name: `Bot ${this.generateRandomName()}`, startingChips: 1000, isBot: true },
    ];
  }

  private generateRandomName(): string {
    return this.hungarianNames[Math.floor(Math.random() * this.hungarianNames.length)];
  }

  addBot(): void {
    if (this.players.length >= this.maxPlayers) {
      return;
    }
    this.players.push({
      name: `Bot ${this.generateRandomName()}`,
      startingChips: 1000,
      isBot: true
    });
  }

  removePlayer(index: number): void {
    if (this.players.length <= this.minPlayers) {
      return;
    }
    if (!this.players[index].isBot) {
      return; 
    }
    this.players.splice(index, 1);
  }

  startGame(): void {
    if (this.players.length < this.minPlayers) {
      this.errorMessage = 'Need at least 2 players to start a game';
      return;
    }

    this.isStarting = true;
    this.errorMessage = '';

    this.playerService.setPlayers(this.players);

    this.http.post(`${this.apiConfig.apiUrl}/poker/start`, this.players)
      .subscribe({
        next: () => {
          this.router.navigate(['/game']);
        },
        error: (error: HttpErrorResponse) => {
          this.isStarting = false;
          console.error('Failed to create game:', error);
          this.errorMessage = error.error?.message ?? 'Failed to create game. Please try again.';
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
