import { Component, OnInit, OnDestroy, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, interval } from 'rxjs';
import { takeUntil, take } from 'rxjs/operators';
import { 
  HandHistoryService, 
  ReplayData, 
  ReplayState, 
  ReplayAction 
} from '../services/hand-history.service';

@Component({
  selector: 'app-hand-replay',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './hand-replay.component.html',
  styleUrls: ['./hand-replay.component.scss']
})
export class HandReplayComponent implements OnInit, OnDestroy {
  private historyService = inject(HandHistoryService);
  private destroy$ = new Subject<void>();

  @Input() historyId!: string;

  replay: ReplayData | null = null;
  replayState: ReplayState | null = null;
  
  isPlaying = false;
  playbackSpeed = 1000; 
  
  
  showCards = true;
  isLoading = false;
  error: string | null = null;

  ngOnInit(): void {
    if (this.historyId) {
      this.loadReplay();
    }

    
    this.historyService.replayIndex$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateReplayState();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopAutoPlay();
    this.historyService.clearReplay();
  }

  loadReplay(): void {
    if (!this.historyId) return;
    
    this.isLoading = true;
    this.error = null;

    this.historyService.getReplayData(this.historyId).subscribe({
      next: (data) => {
        this.replay = data;
        this.updateReplayState();
        this.isLoading = false;
      },
      error: (err) => {
        this.error = 'Failed to load hand history';
        this.isLoading = false;
        console.error('Error loading replay:', err);
      }
    });
  }

  private updateReplayState(): void {
    this.replayState = this.historyService.getCurrentReplayState();
  }

  

  play(): void {
    if (this.isPlaying || !this.replay) return;
    
    this.isPlaying = true;
    
    const remainingActions = this.replay.actions.length - (this.replayState?.actionIndex || 0);
    
    interval(this.playbackSpeed).pipe(
      take(remainingActions),
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        if (this.isPlaying) {
          this.nextStep();
        }
      },
      complete: () => {
        this.isPlaying = false;
      }
    });
  }

  pause(): void {
    this.isPlaying = false;
  }

  stopAutoPlay(): void {
    this.isPlaying = false;
  }

  reset(): void {
    this.stopAutoPlay();
    this.historyService.resetReplay();
  }

  nextStep(): void {
    this.historyService.nextAction();
  }

  previousStep(): void {
    this.historyService.previousAction();
  }

  goToStep(index: number): void {
    this.stopAutoPlay();
    this.historyService.goToAction(index);
  }

  goToEnd(): void {
    if (this.replay) {
      this.stopAutoPlay();
      this.historyService.goToAction(this.replay.actions.length);
    }
  }

  setSpeed(speed: number): void {
    this.playbackSpeed = speed;
  }

  toggleCards(): void {
    this.showCards = !this.showCards;
  }

  

  formatAction(action: ReplayAction): string {
    return this.historyService.formatAction(action);
  }

  formatCard(cardString: string): { symbol: string, isRed: boolean } {
    const card = this.historyService.formatCard(cardString);
    const isRed = card.suit === 'HEARTS' || card.suit === 'DIAMONDS';
    return { symbol: card.symbol, isRed };
  }

  formatPhase(phase: string): string {
    return this.historyService.formatPhase(phase);
  }

  getProgressPercentage(): number {
    if (!this.replay || !this.replayState) return 0;
    return (this.replayState.actionIndex / this.replay.actions.length) * 100;
  }

  getActionClass(action: ReplayAction): string {
    switch (action.action.toUpperCase()) {
      case 'FOLD': return 'action-fold';
      case 'CHECK': return 'action-check';
      case 'CALL': return 'action-call';
      case 'BET': return 'action-bet';
      case 'RAISE': return 'action-raise';
      default: return '';
    }
  }

  getPhaseClass(phase: string): string {
    return `phase-${phase.toLowerCase().replace('_', '-')}`;
  }

  isPlayerActive(playerName: string): boolean {
    if (!this.replayState?.currentAction) return false;
    return this.replayState.currentAction.playerName === playerName;
  }

  isPlayerFolded(playerName: string): boolean {
    const player = this.replayState?.players.find(p => p.name === playerName);
    return player?.folded || false;
  }

  getPlayerChips(playerName: string): number {
    const player = this.replayState?.players.find(p => p.name === playerName);
    return player?.chips || 0;
  }

  getPlayerBet(playerName: string): number {
    const player = this.replayState?.players.find(p => p.name === playerName);
    return player?.bet || 0;
  }

  

  getActionsUpToIndex(): ReplayAction[] {
    if (!this.replay || !this.replayState) return [];
    return this.replay.actions.slice(0, this.replayState.actionIndex);
  }

  getActionsByPhase(): Map<string, ReplayAction[]> {
    if (!this.replay) return new Map();
    
    const byPhase = new Map<string, ReplayAction[]>();
    
    this.replay.actions.forEach(action => {
      const existing = byPhase.get(action.phase) || [];
      existing.push(action);
      byPhase.set(action.phase, existing);
    });
    
    return byPhase;
  }
}
