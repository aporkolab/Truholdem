import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SoundService, SoundEffect } from '../services/sound.service';

interface GameSettings {
  
  showCardAnimations: boolean;
  showChipAnimations: boolean;
  tableColor: string;
  cardBackColor: string;
  
  
  autoMuck: boolean;
  showPotOdds: boolean;
  confirmActions: boolean;
  
  
  actionTimeLimit: number;
  autoFoldOnTimeout: boolean;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="settings-container" data-cy="settings-page">
      <h2 data-cy="settings-title">⚙️ Settings</h2>

      <!-- Sound Settings -->
      <section class="settings-section" data-cy="sound-settings-section">
        <h3 data-cy="sound-section-title">🔊 Sound</h3>
        
        <div class="setting-item" data-cy="sound-toggle-item">
          <label class="toggle-label">
            <span>Sound Effects</span>
            <input type="checkbox" [checked]="soundEnabled" (change)="toggleSound()" data-cy="sound-enabled-toggle">
            <span class="toggle-slider"></span>
          </label>
        </div>

        <div class="setting-item" *ngIf="soundEnabled" data-cy="volume-item">
          <label>
            <span>Volume</span>
            <input 
              type="range" 
              min="0" 
              max="1" 
              step="0.1" 
              [value]="soundVolume"
              (input)="setVolume($event)"
              data-cy="volume-slider"
              aria-label="Volume">
            <span class="volume-value" data-cy="volume-display">{{ (soundVolume * 100) | number:'1.0-0' }}%</span>
          </label>
        </div>

        <div class="setting-group" *ngIf="soundEnabled" data-cy="individual-sounds-section">
          <span class="group-label">Individual Sounds</span>
          <div class="sound-toggles" data-cy="sound-toggles">
            <label *ngFor="let effect of soundEffects" class="mini-toggle" [attr.data-cy]="'sound-toggle-' + effect">
              <input 
                type="checkbox" 
                [checked]="getSoundEffectEnabled(effect)"
                (change)="toggleSoundEffect(effect)"
                [attr.data-cy]="'sound-checkbox-' + effect">
              <span>{{ formatEffectName(effect) }}</span>
            </label>
          </div>
        </div>
      </section>

      <!-- Display Settings -->
      <section class="settings-section" data-cy="display-settings-section">
        <h3 data-cy="display-section-title">🎨 Display</h3>

        <div class="setting-item" data-cy="card-animation-item">
          <label class="toggle-label">
            <span>Card Animations</span>
            <input type="checkbox" [(ngModel)]="gameSettings.showCardAnimations" (change)="saveSettings()" data-cy="card-animation-toggle">
            <span class="toggle-slider"></span>
          </label>
        </div>

        <div class="setting-item" data-cy="chip-animation-item">
          <label class="toggle-label">
            <span>Chip Animations</span>
            <input type="checkbox" [(ngModel)]="gameSettings.showChipAnimations" (change)="saveSettings()" data-cy="chip-animation-toggle">
            <span class="toggle-slider"></span>
          </label>
        </div>

        <div class="setting-item" data-cy="table-color-item" role="group" aria-labelledby="table-color-label">
          <span id="table-color-label" class="setting-label">Table Color</span>
          <div class="color-options" data-cy="table-color-options">
            <button
              *ngFor="let color of tableColors"
              class="color-btn"
              [style.background]="color.value"
              [class.selected]="gameSettings.tableColor === color.value"
              (click)="setTableColor(color.value)"
              [title]="color.name"
              [attr.data-cy]="'table-color-' + color.name.toLowerCase().replace(' ', '-')"
              type="button">
            </button>
          </div>
        </div>

        <div class="setting-item" data-cy="card-back-item" role="group" aria-labelledby="card-back-label">
          <span id="card-back-label" class="setting-label">Card Back</span>
          <div class="card-back-options" data-cy="card-back-options">
            <button
              *ngFor="let back of cardBacks"
              class="card-back-btn"
              [style.background]="back.color"
              [class.selected]="gameSettings.cardBackColor === back.color"
              (click)="setCardBack(back.color)"
              [title]="back.name"
              [attr.data-cy]="'card-back-' + back.name.toLowerCase()"
              type="button">
              {{ back.pattern }}
            </button>
          </div>
        </div>
      </section>

      <!-- Gameplay Settings -->
      <section class="settings-section" data-cy="gameplay-settings-section">
        <h3 data-cy="gameplay-section-title">🎮 Gameplay</h3>

        <div class="setting-item" data-cy="auto-muck-item">
          <label class="toggle-label">
            <span>Auto-Muck Losing Hands</span>
            <input type="checkbox" [(ngModel)]="gameSettings.autoMuck" (change)="saveSettings()" data-cy="auto-muck-toggle">
            <span class="toggle-slider"></span>
          </label>
          <span class="setting-hint">Automatically hide your cards when you lose</span>
        </div>

        <div class="setting-item" data-cy="pot-odds-item">
          <label class="toggle-label">
            <span>Show Pot Odds</span>
            <input type="checkbox" [(ngModel)]="gameSettings.showPotOdds" (change)="saveSettings()" data-cy="pot-odds-toggle">
            <span class="toggle-slider"></span>
          </label>
          <span class="setting-hint">Display pot odds when facing a bet</span>
        </div>

        <div class="setting-item" data-cy="confirm-actions-item">
          <label class="toggle-label">
            <span>Confirm All-In</span>
            <input type="checkbox" [(ngModel)]="gameSettings.confirmActions" (change)="saveSettings()" data-cy="confirm-actions-toggle">
            <span class="toggle-slider"></span>
          </label>
          <span class="setting-hint">Ask for confirmation before going all-in</span>
        </div>
      </section>

      <!-- Timing Settings -->
      <section class="settings-section" data-cy="timing-settings-section">
        <h3 data-cy="timing-section-title">⏱️ Timing</h3>

        <div class="setting-item" data-cy="time-limit-item">
          <label>
            <span>Action Time Limit</span>
            <select [(ngModel)]="gameSettings.actionTimeLimit" (change)="saveSettings()" data-cy="time-limit-select" aria-label="Action time limit">
              <option [value]="15">15 seconds</option>
              <option [value]="30">30 seconds</option>
              <option [value]="45">45 seconds</option>
              <option [value]="60">60 seconds</option>
              <option [value]="0">No limit</option>
            </select>
          </label>
        </div>

        <div class="setting-item" *ngIf="gameSettings.actionTimeLimit > 0" data-cy="auto-fold-item">
          <label class="toggle-label">
            <span>Auto-Fold on Timeout</span>
            <input type="checkbox" [(ngModel)]="gameSettings.autoFoldOnTimeout" (change)="saveSettings()" data-cy="auto-fold-toggle">
            <span class="toggle-slider"></span>
          </label>
          <span class="setting-hint">Automatically fold when time runs out</span>
        </div>
      </section>

      <!-- Actions -->
      <div class="settings-actions" data-cy="settings-actions">
        <button class="btn btn-secondary" (click)="resetToDefaults()" data-cy="reset-defaults-btn" type="button">Reset to Defaults</button>
        <button class="btn btn-primary" (click)="testSound()" data-cy="test-sound-btn" type="button">🔊 Test Sound</button>
      </div>
    </div>
  `,
  styles: [`
    .settings-container {
      max-width: 600px;
      margin: 0 auto;
      padding: 1.5rem;
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
      border-radius: 12px;
      color: #fff;

      h2 {
        margin: 0 0 1.5rem;
        color: #ffd700;
        text-align: center;
      }
    }

    .settings-section {
      margin-bottom: 2rem;
      padding: 1rem;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 8px;

      h3 {
        margin: 0 0 1rem;
        font-size: 1rem;
        color: rgba(255, 255, 255, 0.9);
        padding-bottom: 0.5rem;
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      }
    }

    .setting-item {
      margin-bottom: 1rem;

      &:last-child {
        margin-bottom: 0;
      }

      > label {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;

        > span:first-child {
          font-size: 0.9rem;
        }
      }
    }

    .setting-hint {
      display: block;
      font-size: 0.75rem;
      color: rgba(255, 255, 255, 0.5);
      margin-top: 0.25rem;
      padding-left: 0.5rem;
    }

    // Toggle switch
    .toggle-label {
      display: flex;
      align-items: center;
      cursor: pointer;
      gap: 1rem;

      input[type="checkbox"] {
        display: none;
      }

      .toggle-slider {
        width: 48px;
        height: 24px;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 12px;
        position: relative;
        transition: background 0.3s;

        &::after {
          content: '';
          position: absolute;
          width: 20px;
          height: 20px;
          background: #fff;
          border-radius: 50%;
          top: 2px;
          left: 2px;
          transition: transform 0.3s;
        }
      }

      input:checked + .toggle-slider {
        background: #4ade80;

        &::after {
          transform: translateX(24px);
        }
      }
    }

    // Range slider
    input[type="range"] {
      flex: 1;
      height: 6px;
      -webkit-appearance: none;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 3px;
      outline: none;

      &::-webkit-slider-thumb {
        -webkit-appearance: none;
        width: 18px;
        height: 18px;
        background: #3b82f6;
        border-radius: 50%;
        cursor: pointer;
      }
    }

    .volume-value {
      min-width: 40px;
      text-align: right;
      font-size: 0.85rem;
    }

    // Sound toggles
    .setting-group {
      .group-label {
        display: block;
        font-size: 0.8rem;
        color: rgba(255, 255, 255, 0.6);
        margin-bottom: 0.5rem;
      }
    }

    .sound-toggles {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;

      .mini-toggle {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0.25rem 0.5rem;
        background: rgba(255, 255, 255, 0.05);
        border-radius: 4px;
        font-size: 0.8rem;
        cursor: pointer;

        input {
          width: 14px;
          height: 14px;
        }
      }
    }

    // Color options
    .color-options {
      display: flex;
      gap: 0.5rem;
    }

    .color-btn {
      width: 32px;
      height: 32px;
      border: 2px solid transparent;
      border-radius: 50%;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        transform: scale(1.1);
      }

      &.selected {
        border-color: #fff;
        box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.3);
      }
    }

    // Card back options
    .card-back-options {
      display: flex;
      gap: 0.5rem;
    }

    .card-back-btn {
      width: 40px;
      height: 56px;
      border: 2px solid transparent;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1.2rem;
      transition: all 0.2s;

      &:hover {
        transform: scale(1.05);
      }

      &.selected {
        border-color: #ffd700;
      }
    }

    // Select
    select {
      padding: 0.5rem;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 4px;
      color: #fff;
      cursor: pointer;

      option {
        background: #1a1a2e;
      }
    }

    // Actions
    .settings-actions {
      display: flex;
      justify-content: center;
      gap: 1rem;
      margin-top: 1.5rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    .btn {
      padding: 0.6rem 1.2rem;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.9rem;
      transition: all 0.2s;

      &.btn-primary {
        background: #3b82f6;
        color: #fff;

        &:hover { background: #2563eb; }
      }

      &.btn-secondary {
        background: rgba(255, 255, 255, 0.1);
        color: #fff;

        &:hover { background: rgba(255, 255, 255, 0.2); }
      }
    }
  `]
})
export class SettingsComponent implements OnInit, OnDestroy {
  private soundService = inject(SoundService);
  private destroy$ = new Subject<void>();

  soundEnabled = true;
  soundVolume = 0.5;

  soundEffects: SoundEffect[] = [
    'cardDeal', 'cardFlip', 'chips', 'check', 
    'fold', 'allIn', 'win', 'lose', 'turn', 'click'
  ];

  gameSettings: GameSettings = {
    showCardAnimations: true,
    showChipAnimations: true,
    tableColor: '#0d5c2e',
    cardBackColor: '#1e3a5f',
    autoMuck: true,
    showPotOdds: true,
    confirmActions: true,
    actionTimeLimit: 30,
    autoFoldOnTimeout: false
  };

  tableColors = [
    { name: 'Classic Green', value: '#0d5c2e' },
    { name: 'Navy Blue', value: '#1e3a5f' },
    { name: 'Burgundy', value: '#722f37' },
    { name: 'Purple', value: '#4a1f6f' },
    { name: 'Dark Gray', value: '#2d3436' }
  ];

  cardBacks = [
    { name: 'Blue', color: '#1e3a5f', pattern: '♠' },
    { name: 'Red', color: '#7f1d1d', pattern: '♥' },
    { name: 'Green', color: '#14532d', pattern: '♣' },
    { name: 'Purple', color: '#581c87', pattern: '♦' },
    { name: 'Gold', color: '#78350f', pattern: '★' }
  ];

  ngOnInit(): void {
    
    this.soundService.settings$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(settings => {
      this.soundEnabled = settings.enabled;
      this.soundVolume = settings.volume;
    });

    
    this.loadGameSettings();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  

  toggleSound(): void {
    this.soundService.toggleSound();
  }

  setVolume(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    if (!target) return;
    const value = parseFloat(target.value);
    this.soundService.setVolume(value);
  }

  getSoundEffectEnabled(effect: SoundEffect): boolean {
    return this.soundService.settings.effects[effect];
  }

  toggleSoundEffect(effect: SoundEffect): void {
    const currentState = this.getSoundEffectEnabled(effect);
    this.soundService.setEffectEnabled(effect, !currentState);
  }

  formatEffectName(effect: SoundEffect): string {
    return effect.replace(/([A-Z])/g, ' $1').trim();
  }

  testSound(): void {
    this.soundService.playChips();
  }

  

  setTableColor(color: string): void {
    this.gameSettings.tableColor = color;
    this.saveSettings();
  }

  setCardBack(color: string): void {
    this.gameSettings.cardBackColor = color;
    this.saveSettings();
  }

  saveSettings(): void {
    if (typeof localStorage === 'undefined') return;
    
    try {
      localStorage.setItem('truholdem_game_settings', JSON.stringify(this.gameSettings));
    } catch {
      console.warn('Failed to save game settings');
    }
  }

  loadGameSettings(): void {
    if (typeof localStorage === 'undefined') return;

    try {
      const saved = localStorage.getItem('truholdem_game_settings');
      if (saved) {
        const parsed = JSON.parse(saved) as Partial<GameSettings>;
        this.gameSettings = { ...this.gameSettings, ...parsed };
      }
    } catch {
      console.warn('Failed to load game settings');
    }
  }

  resetToDefaults(): void {
    this.gameSettings = {
      showCardAnimations: true,
      showChipAnimations: true,
      tableColor: '#0d5c2e',
      cardBackColor: '#1e3a5f',
      autoMuck: true,
      showPotOdds: true,
      confirmActions: true,
      actionTimeLimit: 30,
      autoFoldOnTimeout: false
    };

    this.soundService.setEnabled(true);
    this.soundService.setVolume(0.5);
    
    this.soundEffects.forEach(effect => {
      this.soundService.setEffectEnabled(effect, true);
    });

    this.saveSettings();
  }
}
