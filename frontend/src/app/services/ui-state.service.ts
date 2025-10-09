import { Injectable, computed, effect, signal } from '@angular/core';


export type TableTheme = 'dark' | 'light' | 'green' | 'blue' | 'red';


export type AnimationSpeed = 'off' | 'fast' | 'normal' | 'slow';


interface UiStateInternal {
  showRaiseModal: boolean;
  showSettingsModal: boolean;
  showResultModal: boolean;
  showHelpModal: boolean;
  soundEnabled: boolean;
  animationsEnabled: boolean;
  animationSpeed: AnimationSpeed;
  theme: TableTheme;
  cardBackDesign: string;
  showCardAnimations: boolean;
  showChipAnimations: boolean;
  autoMuck: boolean;
  showPotOdds: boolean;
  compactMode: boolean;
  showBetHistory: boolean;
}


@Injectable({ providedIn: 'root' })
export class UiStateService {
  
  
  

  private readonly STORAGE_KEY = 'truholdem_ui_settings';

  
  
  

  private readonly _uiState = signal<UiStateInternal>(this.loadSettings());

  
  
  

  
  readonly showRaiseModal = computed(() => this._uiState().showRaiseModal);

  
  readonly showSettingsModal = computed(() => this._uiState().showSettingsModal);

  
  readonly showResultModal = computed(() => this._uiState().showResultModal);

  
  readonly showHelpModal = computed(() => this._uiState().showHelpModal);

  
  readonly isAnyModalOpen = computed(() => 
    this.showRaiseModal() || 
    this.showSettingsModal() || 
    this.showResultModal() ||
    this.showHelpModal()
  );

  
  
  

  
  readonly soundEnabled = computed(() => this._uiState().soundEnabled);

  
  readonly animationsEnabled = computed(() => this._uiState().animationsEnabled);

  
  readonly animationSpeed = computed(() => this._uiState().animationSpeed);

  
  readonly theme = computed(() => this._uiState().theme);

  
  readonly cardBackDesign = computed(() => this._uiState().cardBackDesign);

  
  readonly showCardAnimations = computed(() => 
    this._uiState().showCardAnimations && this._uiState().animationsEnabled
  );

  
  readonly showChipAnimations = computed(() => 
    this._uiState().showChipAnimations && this._uiState().animationsEnabled
  );

  
  readonly autoMuck = computed(() => this._uiState().autoMuck);

  
  readonly showPotOdds = computed(() => this._uiState().showPotOdds);

  
  readonly compactMode = computed(() => this._uiState().compactMode);

  
  readonly showBetHistory = computed(() => this._uiState().showBetHistory);

  
  
  

  
  readonly animationDuration = computed<number>(() => {
    if (!this.animationsEnabled()) return 0;
    
    const durations: Record<AnimationSpeed, number> = {
      'off': 0,
      'fast': 150,
      'normal': 300,
      'slow': 600
    };
    return durations[this.animationSpeed()];
  });

  
  readonly themeClass = computed(() => `theme-${this.theme()}`);

  constructor() {
    
    effect(() => {
      const state = this._uiState();
      this.saveSettings(state);
    });
  }

  
  
  

  
  openRaiseModal(): void {
    this.updateState({ showRaiseModal: true });
  }

  
  closeRaiseModal(): void {
    this.updateState({ showRaiseModal: false });
  }

  
  toggleRaiseModal(): void {
    this.updateState({ showRaiseModal: !this._uiState().showRaiseModal });
  }

  
  openSettingsModal(): void {
    this.updateState({ showSettingsModal: true });
  }

  
  closeSettingsModal(): void {
    this.updateState({ showSettingsModal: false });
  }

  
  openResultModal(): void {
    this.updateState({ showResultModal: true });
  }

  
  closeResultModal(): void {
    this.updateState({ showResultModal: false });
  }

  
  openHelpModal(): void {
    this.updateState({ showHelpModal: true });
  }

  
  closeHelpModal(): void {
    this.updateState({ showHelpModal: false });
  }

  
  closeAllModals(): void {
    this.updateState({
      showRaiseModal: false,
      showSettingsModal: false,
      showResultModal: false,
      showHelpModal: false
    });
  }

  
  
  

  
  toggleSound(): void {
    this.updateState({ soundEnabled: !this._uiState().soundEnabled });
  }

  
  setSoundEnabled(enabled: boolean): void {
    this.updateState({ soundEnabled: enabled });
  }

  
  toggleAnimations(): void {
    this.updateState({ animationsEnabled: !this._uiState().animationsEnabled });
  }

  
  setAnimationsEnabled(enabled: boolean): void {
    this.updateState({ animationsEnabled: enabled });
  }

  
  setAnimationSpeed(speed: AnimationSpeed): void {
    this.updateState({ animationSpeed: speed });
  }

  
  setTheme(theme: TableTheme): void {
    this.updateState({ theme });
    
    if (typeof document !== 'undefined') {
      document.body.classList.remove('theme-dark', 'theme-light', 'theme-green', 'theme-blue', 'theme-red');
      document.body.classList.add(`theme-${theme}`);
    }
  }

  
  setCardBackDesign(design: string): void {
    this.updateState({ cardBackDesign: design });
  }

  
  toggleCardAnimations(): void {
    this.updateState({ showCardAnimations: !this._uiState().showCardAnimations });
  }

  
  toggleChipAnimations(): void {
    this.updateState({ showChipAnimations: !this._uiState().showChipAnimations });
  }

  
  toggleAutoMuck(): void {
    this.updateState({ autoMuck: !this._uiState().autoMuck });
  }

  
  togglePotOdds(): void {
    this.updateState({ showPotOdds: !this._uiState().showPotOdds });
  }

  
  toggleCompactMode(): void {
    this.updateState({ compactMode: !this._uiState().compactMode });
  }

  
  toggleBetHistory(): void {
    this.updateState({ showBetHistory: !this._uiState().showBetHistory });
  }

  
  
  

  
  resetToDefaults(): void {
    this._uiState.set(this.getDefaultSettings());
  }

  
  
  

  private updateState(partial: Partial<UiStateInternal>): void {
    this._uiState.update(state => ({
      ...state,
      ...partial
    }));
  }

  private getDefaultSettings(): UiStateInternal {
    return {
      showRaiseModal: false,
      showSettingsModal: false,
      showResultModal: false,
      showHelpModal: false,
      soundEnabled: true,
      animationsEnabled: true,
      animationSpeed: 'normal',
      theme: 'green',
      cardBackDesign: 'default',
      showCardAnimations: true,
      showChipAnimations: true,
      autoMuck: true,
      showPotOdds: false,
      compactMode: false,
      showBetHistory: false
    };
  }

  private loadSettings(): UiStateInternal {
    const defaults = this.getDefaultSettings();
    
    if (typeof localStorage === 'undefined') {
      return defaults;
    }

    try {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        
        return { ...defaults, ...parsed };
      }
    } catch (e) {
      console.warn('Failed to load UI settings:', e);
    }

    return defaults;
  }

  private saveSettings(settings: UiStateInternal): void {
    if (typeof localStorage === 'undefined') return;

    try {
      
      const settingsToSave = {
        ...settings,
        showRaiseModal: false,
        showSettingsModal: false,
        showResultModal: false,
        showHelpModal: false
      };
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(settingsToSave));
    } catch (e) {
      console.warn('Failed to save UI settings:', e);
    }
  }
}
