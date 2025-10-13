import { 
  Component, 
  OnInit, 
  OnDestroy, 
  Input, 
  inject,
  signal,
  computed,
  effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, interval, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { 
  HandHistoryService, 
  ReplayData, 
  ReplayState, 
  ReplayAction 
} from '../services/hand-history.service';
import { EquityDisplayComponent, EquityData } from './components/equity-display/equity-display.component';
import { ActionAnalysisComponent, ActionAnalysisData } from './components/action-analysis/action-analysis.component';
import { 
  cardRevealAnimation,
  cardDealAnimation,
  potChangeAnimation,
  actionPopAnimation,
  playerHighlightAnimation,
  winnerAnimation,
  analysisOverlayAnimation,
  timelineItemAnimation
} from './animations/hand-replay.animations';

interface PlayerAtStep {
  id: string;
  name: string;
  chips: number;
  bet: number;
  folded: boolean;
  holeCard1: string;
  holeCard2: string;
  isDealer: boolean;
  lastAction?: string;
}

@Component({
  selector: 'app-hand-replay',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    EquityDisplayComponent,
    ActionAnalysisComponent
  ],
  templateUrl: './hand-replay.component.html',
  styleUrls: ['./hand-replay.component.scss'],
  animations: [
    cardRevealAnimation,
    cardDealAnimation,
    potChangeAnimation,
    actionPopAnimation,
    playerHighlightAnimation,
    winnerAnimation,
    analysisOverlayAnimation,
    timelineItemAnimation
  ]
})
export class HandReplayComponent implements OnInit, OnDestroy {
  private historyService = inject(HandHistoryService);
  private destroy$ = new Subject<void>();
  private playbackSubscription?: Subscription;

  @Input() handId!: string;

  
  replay: ReplayData | null = null;
  replayState: ReplayState | null = null;
  
  
  private _currentStep = signal(0);
  private _isPlaying = signal(false);
  private _playbackSpeed = signal(1);
  private _showAnalysisOverlay = signal(false);
  private _showCards = signal(true);
  private _isLoading = signal(false);
  private _error = signal<string | null>(null);
  private _replayData = signal<ReplayData | null>(null);
  private _replayStateSignal = signal<ReplayState | null>(null);
  private _lastRevealedCardIndex = signal(-1);

  
  currentStep = this._currentStep.asReadonly();
  isPlaying = this._isPlaying.asReadonly();
  playbackSpeedValue = this._playbackSpeed.asReadonly();
  showAnalysisOverlay = this._showAnalysisOverlay.asReadonly();
  showCards = this._showCards;
  isLoading = this._isLoading.asReadonly();
  error = this._error.asReadonly();

  
  totalSteps = computed(() => this._replayData()?.actions.length ?? 0);
  currentPot = computed(() => this._replayStateSignal()?.pot ?? 0);
  
  visibleCommunityCards = computed(() => {
    const state = this._replayStateSignal();
    return state?.board ?? [];
  });

  revealedCardsCount = computed(() => this.visibleCommunityCards().length);

  playersAtStep = computed((): PlayerAtStep[] => {
    const data = this._replayData();
    const state = this._replayStateSignal();
    if (!data || !state) return [];

    return data.players.map((player, index) => {
      const statePlayer = state.players.find(p => p.name === player.name);
      const lastAction = this.getPlayerLastAction(player.name);
      
      return {
        id: `player-${index}`,
        name: player.name,
        chips: statePlayer?.chips ?? player.startingChips,
        bet: statePlayer?.bet ?? 0,
        folded: statePlayer?.folded ?? false,
        holeCard1: player.holeCard1,
        holeCard2: player.holeCard2,
        isDealer: index === data.dealerPosition,
        lastAction
      };
    });
  });

  currentActionPlayer = computed(() => {
    const state = this._replayStateSignal();
    return state?.currentAction?.playerName ?? null;
  });

  progressPercentage = computed(() => {
    const total = this.totalSteps();
    if (total === 0) return 0;
    return (this._currentStep() / total) * 100;
  });

  
  currentEquity = computed((): EquityData | null => {
    const state = this._replayStateSignal();
    if (!state || !this._showAnalysisOverlay()) return null;
    
    const baseEquity = state.phase === 'PRE_FLOP' ? 0.5 : 
                       state.phase === 'FLOP' ? 0.45 :
                       state.phase === 'TURN' ? 0.4 : 0.35;
    
    return {
      winProbability: baseEquity + Math.random() * 0.2,
      tieProbability: 0.02 + Math.random() * 0.03,
      loseProbability: 1 - baseEquity - 0.02,
      handStrength: baseEquity > 0.5 ? 'MEDIUM_STRENGTH' : 'DRAWING',
      potOdds: state.pot > 0 ? 0.25 : undefined
    };
  });

  currentActionAnalysis = computed((): ActionAnalysisData | null => {
    const state = this._replayStateSignal();
    if (!state?.currentAction || !this._showAnalysisOverlay()) return null;

    const action = state.currentAction;
    return {
      action: action.action,
      assessment: this.mockAssessment(action),
      evActual: action.amount ? action.amount * 0.3 : 5,
      evOptimal: action.amount ? action.amount * 0.4 : 8,
      evLost: action.amount ? action.amount * 0.1 : 3,
      optimalAction: action.action === 'CALL' ? 'RAISE' : action.action,
      reasoning: this.mockReasoning(action),
      alternatives: [
        { action: 'FOLD', ev: 0 },
        { action: 'CALL', ev: action.amount ? action.amount * 0.2 : 3 },
        { action: 'RAISE', ev: action.amount ? action.amount * 0.4 : 8 }
      ],
      gtoFrequency: {
        fold: 0.15,
        check: 0,
        call: 0.45,
        bet: 0,
        raise: 0.40
      }
    };
  });

  actionsUpToCurrentStep = computed(() => {
    const data = this._replayData();
    const step = this._currentStep();
    if (!data) return [];
    return data.actions.slice(0, step);
  });

  playbackInterval = computed(() => {
    const speed = this._playbackSpeed();
    return 1000 / speed;
  });

  get playbackSpeed(): number {
    return this._playbackSpeed();
  }
  set playbackSpeed(value: number) {
    this._playbackSpeed.set(value);
    if (this._isPlaying()) {
      this.restartPlayback();
    }
  }

  get showAnalysis(): boolean {
    return this._showAnalysisOverlay();
  }
  set showAnalysis(value: boolean) {
    this._showAnalysisOverlay.set(value);
  }

  constructor() {
    effect(() => {
      const state = this._replayStateSignal();
      if (state) {
        this._currentStep.set(state.actionIndex);
      }
    });
  }

  ngOnInit(): void {
    if (this.handId) {
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
    this.stopPlayback();
    this.historyService.clearReplay();
  }

  loadReplay(): void {
    if (!this.handId) return;

    this._isLoading.set(true);
    this._error.set(null);

    this.historyService.getReplayData(this.handId).subscribe({
      next: (data) => {
        this.replay = data;
        this._replayData.set(data);
        this.updateReplayState();
        this._isLoading.set(false);
      },
      error: (err) => {
        this._error.set('Failed to load hand history');
        this._isLoading.set(false);
        console.error('Error loading replay:', err);
      }
    });
  }

  private updateReplayState(): void {
    const state = this.historyService.getCurrentReplayState();
    this.replayState = state;
    this._replayStateSignal.set(state);
    
    if (state) {
      this._lastRevealedCardIndex.set(state.board.length - 1);
    }
  }

  togglePlay(): void {
    if (this._isPlaying()) {
      this.pause();
    } else {
      this.play();
    }
  }

  play(): void {
    if (this._isPlaying() || !this.replay) return;
    
    if (this.replayState?.isComplete) {
      this.goToStart();
    }
    
    this._isPlaying.set(true);
    this.startPlayback();
  }

  pause(): void {
    this._isPlaying.set(false);
    this.stopPlayback();
  }

  private startPlayback(): void {
    this.stopPlayback();
    
    this.playbackSubscription = interval(this.playbackInterval()).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      if (this._isPlaying() && !this.replayState?.isComplete) {
        this.stepForward();
      } else {
        this.pause();
      }
    });
  }

  private stopPlayback(): void {
    this.playbackSubscription?.unsubscribe();
    this.playbackSubscription = undefined;
  }

  private restartPlayback(): void {
    if (this._isPlaying()) {
      this.startPlayback();
    }
  }

  goToStart(): void {
    this.stopPlayback();
    this._isPlaying.set(false);
    this.historyService.resetReplay();
  }

  goToEnd(): void {
    if (this.replay) {
      this.stopPlayback();
      this._isPlaying.set(false);
      this.historyService.goToAction(this.replay.actions.length);
    }
  }

  stepForward(): void {
    this.historyService.nextAction();
  }

  stepBack(): void {
    this.historyService.previousAction();
  }

  goToStep(index: number): void {
    this.stopPlayback();
    this._isPlaying.set(false);
    this.historyService.goToAction(index);
  }

  setSpeed(speed: number): void {
    this._playbackSpeed.set(speed);
    if (this._isPlaying()) {
      this.restartPlayback();
    }
  }

  toggleCards(): void {
    this._showCards.update(v => !v);
  }

  toggleAnalysis(): void {
    this._showAnalysisOverlay.update(v => !v);
  }

  isNewlyRevealedCard(index: number): boolean {
    return index === this._lastRevealedCardIndex();
  }

  isActivePlayer(player: PlayerAtStep): boolean {
    return player.name === this.currentActionPlayer();
  }

  getPlayerAnimationState(player: PlayerAtStep): string {
    if (player.folded) return 'folded';
    if (this.isActivePlayer(player)) return 'active';
    return 'inactive';
  }

  private getPlayerLastAction(playerName: string): string | undefined {
    const actions = this.actionsUpToCurrentStep();
    for (let i = actions.length - 1; i >= 0; i--) {
      if (actions[i].playerName === playerName) {
        return this.formatAction(actions[i]);
      }
    }
    return undefined;
  }

  private mockAssessment(action: ReplayAction): ActionAnalysisData['assessment'] {
    const assessments: ActionAnalysisData['assessment'][] = [
      'OPTIMAL', 'GOOD', 'ACCEPTABLE', 'QUESTIONABLE', 'MISTAKE'
    ];
    if (action.action === 'FOLD') return 'ACCEPTABLE';
    if (action.action === 'RAISE') return 'GOOD';
    return assessments[Math.floor(Math.random() * 3)];
  }

  private mockReasoning(action: ReplayAction): string {
    const reasons: Record<string, string> = {
      'FOLD': 'Pot odds insufficient for the draw. Discipline is key.',
      'CALL': 'Getting proper odds to continue. Consider raising for value.',
      'CHECK': 'Good pot control with medium strength hand.',
      'BET': 'Value betting thin. Consider sizing for max extraction.',
      'RAISE': 'Building pot with strong equity. Well played.'
    };
    return reasons[action.action.toUpperCase()] ?? 'Standard play given stack depths.';
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
    return this.progressPercentage();
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

  isPlayerActiveLegacy(playerName: string): boolean {
    return playerName === this.currentActionPlayer();
  }

  isPlayerFolded(playerName: string): boolean {
    const player = this.replayState?.players.find(p => p.name === playerName);
    return player?.folded ?? false;
  }

  getPlayerChips(playerName: string): number {
    const player = this.replayState?.players.find(p => p.name === playerName);
    return player?.chips ?? 0;
  }

  getPlayerBet(playerName: string): number {
    const player = this.replayState?.players.find(p => p.name === playerName);
    return player?.bet ?? 0;
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

  reset(): void {
    this.goToStart();
  }

  nextStep(): void {
    this.stepForward();
  }

  previousStep(): void {
    this.stepBack();
  }

  trackByPlayerId(index: number, player: PlayerAtStep): string {
    return player.id;
  }
}