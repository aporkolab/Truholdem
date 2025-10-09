import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type SoundEffect =
  | 'cardDeal'
  | 'cardFlip'
  | 'chips'
  | 'check'
  | 'fold'
  | 'allIn'
  | 'win'
  | 'lose'
  | 'turn'
  | 'timer'
  | 'click';

export interface SoundSettings {
  enabled: boolean;
  volume: number;
  effects: Record<SoundEffect, boolean>;
}

// Extend Window interface for webkit compatibility
declare global {
  interface Window {
    webkitAudioContext?: typeof AudioContext;
  }
}

@Injectable({
  providedIn: 'root',
})
export class SoundService {
  private audioContext: AudioContext | null = null;
  private sounds = new Map<SoundEffect, AudioBuffer>();
  private isInitialized = false;


  private settingsSubject = new BehaviorSubject<SoundSettings>(
    this.loadSettings()
  );
  public settings$ = this.settingsSubject.asObservable();


  // Sound files from Kenney.nl (CC0 licensed)
  // Casino Audio: https://kenney.nl/assets/casino-audio
  // Digital Audio: https://kenney.nl/assets/digital-audio
  private readonly soundUrls: Record<SoundEffect, string> = {
    cardDeal: '/assets/sounds/card-deal.ogg',
    cardFlip: '/assets/sounds/card-flip.ogg',
    chips: '/assets/sounds/chips.ogg',
    check: '/assets/sounds/check.ogg',
    fold: '/assets/sounds/fold.ogg',
    allIn: '/assets/sounds/all-in.ogg',
    win: '/assets/sounds/win.ogg',
    lose: '/assets/sounds/lose.ogg',
    turn: '/assets/sounds/turn.ogg',
    timer: '/assets/sounds/timer.ogg',
    click: '/assets/sounds/click.ogg',
  };

  constructor() {

    if (typeof window !== 'undefined') {
      const initAudio = () => {
        this.initialize();
        document.removeEventListener('click', initAudio);
        document.removeEventListener('keydown', initAudio);
      };
      document.addEventListener('click', initAudio, { once: true });
      document.addEventListener('keydown', initAudio, { once: true });
    }
  }



  async initialize(): Promise<void> {
    if (this.isInitialized) return;

    try {
      const AudioContextConstructor = window.AudioContext || window.webkitAudioContext;
      if (AudioContextConstructor) {
        this.audioContext = new AudioContextConstructor();
      }
      await this.preloadSounds();
      this.isInitialized = true;
      console.log('Sound system initialized');
    } catch (error) {
      console.warn('Failed to initialize sound system:', error);
    }
  }

  private async preloadSounds(): Promise<void> {
    if (!this.audioContext) return;

    const loadPromises = Object.entries(this.soundUrls).map(
      async ([key, url]) => {
        try {
          const response = await fetch(url);
          if (response.ok) {
            const arrayBuffer = await response.arrayBuffer();
            const audioBuffer = await this.audioContext!.decodeAudioData(
              arrayBuffer
            );
            this.sounds.set(key as SoundEffect, audioBuffer);
          }
        } catch {
          // Fallback to synthetic sound on load error
          this.sounds.set(
            key as SoundEffect,
            this.generateSyntheticSound(key as SoundEffect)
          );
        }
      }
    );

    await Promise.allSettled(loadPromises);
  }


  private generateSyntheticSound(type: SoundEffect): AudioBuffer {
    if (!this.audioContext) {
      throw new Error('AudioContext not initialized');
    }

    const sampleRate = this.audioContext.sampleRate;
    let duration = 0.1;
    let frequency = 440;

    switch (type) {
      case 'cardDeal':
        duration = 0.05;
        frequency = 800;
        break;
      case 'cardFlip':
        duration = 0.08;
        frequency = 600;
        break;
      case 'chips':
        duration = 0.15;
        frequency = 1200;
        break;
      case 'check':
        duration = 0.05;
        frequency = 500;
        break;
      case 'fold':
        duration = 0.1;
        frequency = 300;
        break;
      case 'allIn':
        duration = 0.3;
        frequency = 880;
        break;
      case 'win':
        duration = 0.5;
        frequency = 523;
        break;
      case 'lose':
        duration = 0.3;
        frequency = 220;
        break;
      case 'turn':
        duration = 0.1;
        frequency = 660;
        break;
      case 'timer':
        duration = 0.05;
        frequency = 1000;
        break;
      case 'click':
        duration = 0.03;
        frequency = 700;
        break;
    }

    const buffer = this.audioContext.createBuffer(
      1,
      sampleRate * duration,
      sampleRate
    );
    const data = buffer.getChannelData(0);

    for (let i = 0; i < data.length; i++) {
      const t = i / sampleRate;

      const decay = Math.exp(-t * 10);
      data[i] = Math.sin(2 * Math.PI * frequency * t) * decay * 0.3;
    }

    return buffer;
  }



  play(effect: SoundEffect): void {
    const settings = this.settingsSubject.value;

    if (!settings.enabled || !settings.effects[effect]) {
      return;
    }

    if (!this.audioContext || !this.sounds.has(effect)) {

      this.initialize().then(() => this.playSound(effect, settings.volume));
      return;
    }

    this.playSound(effect, settings.volume);
  }

  private playSound(effect: SoundEffect, volume: number): void {
    if (!this.audioContext) return;

    const buffer = this.sounds.get(effect);
    if (!buffer) return;

    try {
      const source = this.audioContext.createBufferSource();
      const gainNode = this.audioContext.createGain();

      source.buffer = buffer;
      gainNode.gain.value = volume;

      source.connect(gainNode);
      gainNode.connect(this.audioContext.destination);

      source.start(0);
    } catch (error) {
      console.warn(`Failed to play sound: ${effect}`, error);
    }
  }


  playCardDeal(): void {
    this.play('cardDeal');
  }
  playCardFlip(): void {
    this.play('cardFlip');
  }
  playChips(): void {
    this.play('chips');
  }
  playCheck(): void {
    this.play('check');
  }
  playFold(): void {
    this.play('fold');
  }
  playAllIn(): void {
    this.play('allIn');
  }
  playWin(): void {
    this.play('win');
  }
  playLose(): void {
    this.play('lose');
  }
  playTurn(): void {
    this.play('turn');
  }
  playTimer(): void {
    this.play('timer');
  }
  playClick(): void {
    this.play('click');
  }

  // Aliases for backward compatibility
  playWinSound(): void {
    this.playWin();
  }
  playLoseSound(): void {
    this.playLose();
  }
  playActionSound(actionType: string): void {
    this.playForAction(actionType);
  }


  playForAction(action: string): void {
    switch (action.toUpperCase()) {
      case 'FOLD':
        this.playFold();
        break;
      case 'CHECK':
        this.playCheck();
        break;
      case 'CALL':
        this.playChips();
        break;
      case 'BET':
        this.playChips();
        break;
      case 'RAISE':
        this.playChips();
        break;
      case 'ALL_IN':
        this.playAllIn();
        break;
    }
  }



  get settings(): SoundSettings {
    return this.settingsSubject.value;
  }

  setEnabled(enabled: boolean): void {
    const settings = { ...this.settingsSubject.value, enabled };
    this.settingsSubject.next(settings);
    this.saveSettings(settings);
  }

  setVolume(volume: number): void {
    const clampedVolume = Math.max(0, Math.min(1, volume));
    const settings = { ...this.settingsSubject.value, volume: clampedVolume };
    this.settingsSubject.next(settings);
    this.saveSettings(settings);
  }

  setEffectEnabled(effect: SoundEffect, enabled: boolean): void {
    const settings = {
      ...this.settingsSubject.value,
      effects: {
        ...this.settingsSubject.value.effects,
        [effect]: enabled,
      },
    };
    this.settingsSubject.next(settings);
    this.saveSettings(settings);
  }

  toggleSound(): void {
    this.setEnabled(!this.settingsSubject.value.enabled);
  }

  isEnabled(): boolean {
    return this.settingsSubject.value.enabled;
  }

  private loadSettings(): SoundSettings {
    const defaultSettings: SoundSettings = {
      enabled: true,
      volume: 0.5,
      effects: {
        cardDeal: true,
        cardFlip: true,
        chips: true,
        check: true,
        fold: true,
        allIn: true,
        win: true,
        lose: true,
        turn: true,
        timer: true,
        click: true,
      },
    };

    if (typeof localStorage === 'undefined') {
      return defaultSettings;
    }

    try {
      const saved = localStorage.getItem('truholdem_sound_settings');
      if (saved) {
        return { ...defaultSettings, ...JSON.parse(saved) };
      }
    } catch {
      console.warn('Failed to load sound settings');
    }

    return defaultSettings;
  }

  private saveSettings(settings: SoundSettings): void {
    if (typeof localStorage === 'undefined') return;

    try {
      localStorage.setItem(
        'truholdem_sound_settings',
        JSON.stringify(settings)
      );
    } catch {
      console.warn('Failed to save sound settings');
    }
  }



  destroy(): void {
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
    this.sounds.clear();
    this.isInitialized = false;
  }
}
