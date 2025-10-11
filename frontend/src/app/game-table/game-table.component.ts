import { NgFor, NgIf } from '@angular/common';
import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject, takeUntil, filter, tap } from 'rxjs';

import { Player } from '../model/player';
import { RaiseInputComponent } from '../raise-input/raise-input.component';
import { PlayerService } from '../services/player.service';
import { UiStateService } from '../services/ui-state.service';
import { SoundService } from '../services/sound.service';
import { GameStore, GameViewModel, PlayerInfo } from '../store/game.store';


@Component({
  selector: 'app-game-table',
  standalone: true,
  templateUrl: './game-table.component.html',
  styleUrls: ['./game-table.component.scss'],
  imports: [NgFor, NgIf, RaiseInputComponent],
  providers: [GameStore], 
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameTableComponent implements OnInit, OnDestroy {
  
  
  
  
  protected readonly store = inject(GameStore);
  private readonly uiState = inject(UiStateService);
  private readonly playerService = inject(PlayerService);
  private readonly soundService = inject(SoundService);
  private readonly router = inject(Router);

  private readonly destroy$ = new Subject<void>();

  
  
  

  
  readonly vm = toSignal(this.store.vm$, { 
    initialValue: this.getDefaultViewModel() 
  });

  
  
  

  
  readonly game = computed(() => this.vm().game);
  readonly isLoading = computed(() => this.vm().isLoading);
  readonly error = computed(() => this.vm().error);
  readonly currentPlayer = computed(() => this.vm().currentPlayer);
  readonly humanPlayer = computed(() => this.vm().humanPlayer);
  readonly isHumanTurn = computed(() => this.vm().isHumanTurn);
  readonly canCheck = computed(() => this.vm().canCheck);
  readonly canCall = computed(() => this.vm().canCall);
  readonly callAmount = computed(() => this.vm().callAmount);
  readonly canPlayerAct = computed(() => this.vm().canPlayerAct);
  readonly potSize = computed(() => this.vm().potSize);
  readonly communityCards = computed(() => this.vm().communityCards);
  readonly phase = computed(() => this.vm().phase);
  readonly phaseDisplayName = computed(() => this.vm().phaseDisplayName);
  readonly isGameFinished = computed(() => this.vm().isGameFinished);
  readonly winnerName = computed(() => this.vm().winnerName);
  readonly winningHandDescription = computed(() => this.vm().winningHandDescription);
  readonly lastAction = computed(() => this.vm().lastAction);
  readonly processingBots = computed(() => this.vm().processingBots);
  readonly minRaiseAmount = computed(() => this.vm().minRaiseAmount);
  readonly maxRaiseAmount = computed(() => this.vm().maxRaiseAmount);
  readonly activePlayers = computed(() => this.vm().activePlayers);
  readonly dealerPosition = computed(() => this.vm().dealerPosition);

  
  readonly currentBet = computed(() => this.game()?.currentBet ?? 0);

  
  
  

  readonly showRaiseModal = this.uiState.showRaiseModal;
  readonly soundEnabled = this.uiState.soundEnabled;
  readonly animationsEnabled = this.uiState.animationsEnabled;

  
  
  

  
  private readonly _gameResultMessage = signal('');
  readonly gameResultMessage = this._gameResultMessage.asReadonly();

  
  private readonly _showModal = signal(false);
  readonly showModal = this._showModal.asReadonly();

  
  
  

  
  readonly sortedPlayers = computed(() => {
    const game = this.game();
    if (!game?.players) return [];

    // Sort by seat position for stable positioning
    return [...game.players].sort((a, b) =>
      (a.seatPosition ?? 0) - (b.seatPosition ?? 0)
    );
  });

  
  readonly isFolded = computed(() => this.humanPlayer()?.folded ?? false);

  
  readonly isAllIn = computed(() => this.humanPlayer()?.isAllIn ?? false);

  
  readonly humanChips = computed(() => this.humanPlayer()?.chips ?? 0);

  
  readonly humanBet = computed(() => this.humanPlayer()?.betAmount ?? 0);

  
  readonly humanHand = computed(() => this.humanPlayer()?.hand ?? []);


  readonly placeholderCards = computed(() => {
    const count = 5 - this.communityCards().length;
    return Array.from({ length: count }, (_, i) => i);
  });

  
  
  

  ngOnInit(): void {
    this.initializeGame();
    this.setupSideEffects();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  
  
  

  private initializeGame(): void {
    const registeredPlayers = this.playerService.getPlayers();
    
    if (registeredPlayers && registeredPlayers.length > 0) {
      const playerInfos: PlayerInfo[] = registeredPlayers.map(p => ({
        name: p.name,
        startingChips: p.startingChips,
        isBot: p.isBot
      }));
      this.store.startGame(playerInfos);
    } else {
      this.store.startGame();
    }
  }

  private setupSideEffects(): void {

    this.store.lastAction$.pipe(
      takeUntil(this.destroy$),
      filter(action => action !== null),
      tap(action => {
        if (this.soundEnabled() && action) {
          this.soundService.playActionSound(action.type);
        }
      })
    ).subscribe();

    // Watch for bot turns - log ALL emissions from currentBot$
    this.store.currentBot$.pipe(
      takeUntil(this.destroy$),
      tap(bot => console.log('[GameTable] currentBot$ emitted:', bot ? { name: bot.name, isBot: bot.isBot, id: bot.id } : null))
    ).subscribe();

    // Bot processing subscription - immediately disable buttons when bot detected
    this.store.currentBot$.pipe(
      takeUntil(this.destroy$),
      filter((bot): bot is Player => bot !== null),
      tap(bot => {
        console.log('[GameTable] Bot detected, disabling buttons and starting processing:', bot.name);
        // Immediately call processBots - it handles the delay internally
        this.store.processBots();
      })
    ).subscribe();

    
    this.store.isGameFinished$.pipe(
      takeUntil(this.destroy$),
      filter(finished => finished),
      tap(() => this.handleGameEnd())
    ).subscribe();
  }

  
  
  

  fold(): void {
    const player = this.humanPlayer();
    if (!player || !this.canPlayerAct()) return;

    this.store.playerAction({
      playerId: player.id,
      action: 'FOLD'
    });
  }

  check(): void {
    const player = this.humanPlayer();
    if (!player || !this.canPlayerAct() || !this.canCheck()) return;

    this.store.playerAction({
      playerId: player.id,
      action: 'CHECK'
    });
  }

  call(): void {
    const player = this.humanPlayer();
    if (!player || !this.canPlayerAct() || !this.canCall()) return;

    this.store.playerAction({
      playerId: player.id,
      action: 'CALL'
    });
  }

  bet(amount: number): void {
    const player = this.humanPlayer();
    if (!player || !this.canPlayerAct()) return;

    this.store.playerAction({
      playerId: player.id,
      action: 'BET',
      amount
    });
  }

  raise(amount: number): void {
    const player = this.humanPlayer();
    if (!player || !this.canPlayerAct()) return;

    const isAllIn = amount >= (player.chips + (player.betAmount ?? 0));
    this.store.playerAction({
      playerId: player.id,
      action: isAllIn ? 'ALL_IN' : 'RAISE',
      amount
    });
  }

  allIn(): void {
    const player = this.humanPlayer();
    if (!player || !this.canPlayerAct()) return;

    const totalAmount = player.chips + (player.betAmount ?? 0);
    this.store.playerAction({
      playerId: player.id,
      action: 'ALL_IN',
      amount: totalAmount
    });
  }

  
  
  

  openRaiseModal(): void {
    if (this.canPlayerAct()) {
      this.uiState.openRaiseModal();
    }
  }

  closeRaiseModal(): void {
    this.uiState.closeRaiseModal();
  }

  onRaiseConfirm(amount: number): void {
    this.raise(amount);
    this.closeRaiseModal();
  }

  
  
  

  startNewHand(): void {
    this._showModal.set(false);
    this.store.startNewHand();
  }

  returnToLobby(): void {
    this._showModal.set(false);
    this.store.resetGame();
    this.router.navigate(['/']);
  }

  // Methods required by template
  handleRaiseAction(amount: number): void {
    this.raise(amount);
  }

  openModal(): void {
    this._showModal.set(true);
  }

  closeModal(): void {
    this._showModal.set(false);
  }

  startNewGame(): void {
    this.startNewHand();
  }

  goToLobby(): void {
    this.returnToLobby();
  }

  private handleGameEnd(): void {
    const winner = this.winnerName();
    const winningHand = this.winningHandDescription();
    
    if (winner) {
      this._gameResultMessage.set(
        winningHand 
          ? `${winner} wins with ${winningHand}!`
          : `${winner} wins!`
      );
    } else {
      this._gameResultMessage.set('Hand complete!');
    }
    
    this._showModal.set(true);

    if (this.soundEnabled()) {
      const human = this.humanPlayer();
      if (human && winner === human.name) {
        this.soundService.playWinSound();
      } else {
        this.soundService.playLoseSound();
      }
    }
  }

  
  
  

  clearError(): void {
    this.store.clearError();
  }

  retryAction(): void {
    this.store.clearError();
    this.store.refreshGame();
  }

  
  
  

  isCurrentPlayer(player: Player): boolean {
    return this.store.isPlayerTurn(player.id);
  }

  
  isPlayerTurn(player: Player): boolean {
    return this.isCurrentPlayer(player);
  }

  isDealer(seatPosition: number): boolean {
    return this.store.isDealer(seatPosition);
  }

  getPlayerStatus(player: Player): string {
    return this.store.getPlayerStatus(player);
  }

  getCardImagePath(card: { suit: string; value: string } | null | undefined): string {
    if (!card || !card.suit || !card.value) {
      return 'assets/cards/back.png';
    }
    const suitName = card.suit.toLowerCase();
    const valueName = this.getCardValueName(card.value);
    return `assets/cards/${valueName}_of_${suitName}.png`;
  }

  
  getCardImage(card: { suit: string; value: string }): string {
    return this.getCardImagePath(card);
  }

  getCardBackImage(): string {
    return 'assets/cards/back.png';
  }

  private getCardValueName(value: string): string {
    const valueNames: Record<string, string> = {
      'TWO': '2', 'THREE': '3', 'FOUR': '4', 'FIVE': '5',
      'SIX': '6', 'SEVEN': '7', 'EIGHT': '8', 'NINE': '9', 'TEN': '10',
      'JACK': 'jack', 'QUEEN': 'queen', 'KING': 'king', 'ACE': 'ace'
    };
    return valueNames[value] || value.toLowerCase();
  }

  trackByPlayerId(index: number, player: Player): string {
    return player.id;
  }

  trackByCardIndex(index: number): number {
    return index;
  }

  trackByCommunityCard(index: number, card: { suit: string; value: string }): string {
    return `${card.suit}-${card.value}`;
  }

  
  
  

  private getDefaultViewModel(): GameViewModel {
    return {
      game: null,
      currentPlayer: null,
      humanPlayer: undefined,
      isLoading: true,
      error: null,
      isHumanTurn: false,
      canCheck: false,
      canCall: false,
      callAmount: 0,
      minRaiseAmount: 20,
      maxRaiseAmount: 0,
      potSize: 0,
      phase: 'PRE_FLOP',
      phaseDisplayName: 'Pre-Flop',
      communityCards: [],
      isGameFinished: false,
      activePlayers: [],
      lastAction: null,
      processingBots: false,
      canPlayerAct: false,
      dealerPosition: 0,
      winnerName: undefined,
      winningHandDescription: undefined
    };
  }
}
