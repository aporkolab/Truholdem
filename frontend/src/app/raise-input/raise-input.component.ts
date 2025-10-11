import { Component, EventEmitter, Input, OnInit, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';

@Component({
    selector: 'app-raise-input',
    standalone: true,
    templateUrl: './raise-input.component.html',
    styleUrls: ['./raise-input.component.scss'],
    imports: [NgIf, FormsModule]
})
export class RaiseInputComponent implements OnInit, OnChanges {
  @Input() game?: Game | null;
  // Alternative inputs for tournament-table compatibility
  @Input() minAmount?: number;
  @Input() maxAmount?: number;
  @Input() currentBet?: number;
  
  @Output() actionTaken = new EventEmitter<number>();
  // Alternative outputs for tournament-table compatibility
  @Output() raiseConfirmed = new EventEmitter<number>();
  @Output() raiseCancel = new EventEmitter<void>();

  isRaiseInputVisible = false;
  raiseAmount = 0;
  maxRaiseAmount = 0;
  minRaiseAmount = 0;
  currentPlayer: Player | undefined;
  errorMessage = '';

  ngOnInit(): void {
    this.updateRaiseLimits();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['game']) {
      this.updateRaiseLimits();
    }
  }

  private updateRaiseLimits(): void {
    // If alternative inputs are provided (tournament mode), use them
    if (this.minAmount !== undefined && this.maxAmount !== undefined) {
      this.minRaiseAmount = this.minAmount;
      this.maxRaiseAmount = this.maxAmount;
      this.raiseAmount = this.minRaiseAmount;
      return;
    }

    // Otherwise use game-based calculation
    if (!this.game?.players) {
      console.warn('RaiseInput: No game or players available');
      return;
    }

    // Find current player - try non-bot first, then any active player
    this.currentPlayer = this.game.players.find(p => !p.name?.startsWith('Bot') && !p.folded && !p.isAllIn);

    // If no non-bot player found, try to find the current player by index
    if (!this.currentPlayer && this.game.currentPlayerIndex !== undefined) {
      const playerAtIndex = this.game.players[this.game.currentPlayerIndex];
      if (playerAtIndex && !playerAtIndex.folded && !playerAtIndex.isAllIn) {
        this.currentPlayer = playerAtIndex;
      }
    }

    if (!this.currentPlayer) {
      console.warn('RaiseInput: No active current player found');
      return;
    }

    this.maxRaiseAmount = (this.currentPlayer.chips ?? 0) + (this.currentPlayer.betAmount ?? 0);

    const currentBetValue = this.currentBet ?? this.game.currentBet ?? 0;
    const minRaiseIncrement = this.game.minRaiseAmount || this.game.bigBlind || environment.defaultBigBlind;

    if (currentBetValue > 0) {
      this.minRaiseAmount = currentBetValue + minRaiseIncrement;
    } else {
      this.minRaiseAmount = this.game.bigBlind || environment.defaultBigBlind;
    }

    // Ensure min doesn't exceed max
    if (this.minRaiseAmount > this.maxRaiseAmount) {
      this.minRaiseAmount = this.maxRaiseAmount;
    }

    this.raiseAmount = this.minRaiseAmount;
    console.debug('RaiseInput limits updated:', {
      min: this.minRaiseAmount,
      max: this.maxRaiseAmount,
      currentBet: currentBetValue,
      pot: this.game.currentPot
    });
  }

  showRaiseInput(): void {
    this.updateRaiseLimits();
    this.isRaiseInputVisible = true;
    this.errorMessage = '';
  }

  cancelRaise(): void {
    this.isRaiseInputVisible = false;
    this.errorMessage = '';
    this.raiseAmount = this.minRaiseAmount;
    this.raiseCancel.emit();
  }

  confirmRaise(): void {
    // Skip player check in tournament mode (when using alternative inputs)
    const isTournamentMode = this.minAmount !== undefined;
    
    if (!isTournamentMode && !this.currentPlayer) {
      this.errorMessage = 'No player found';
      return;
    }

    if (this.raiseAmount < this.minRaiseAmount) {
      this.errorMessage = `Minimum raise is $${this.minRaiseAmount}`;
      return;
    }

    if (this.raiseAmount > this.maxRaiseAmount) {
      this.errorMessage = `Maximum is $${this.maxRaiseAmount} (all-in)`;
      return;
    }

    // Emit both events for compatibility
    this.actionTaken.emit(this.raiseAmount);
    this.raiseConfirmed.emit(this.raiseAmount);
    this.isRaiseInputVisible = false;
    this.errorMessage = '';
  }

  setBetPercentage(percentage: number): void {
    const pot = this.game?.currentPot || 0;
    if (pot <= 0) {
      // If no pot, just use minRaiseAmount
      this.raiseAmount = this.minRaiseAmount;
      console.debug('setBetPercentage: No pot, using min raise', this.minRaiseAmount);
      return;
    }
    const calculatedAmount = Math.ceil(pot * percentage);
    this.raiseAmount = Math.max(this.minRaiseAmount, Math.min(calculatedAmount, this.maxRaiseAmount));
    console.debug('setBetPercentage:', { percentage, pot, calculated: calculatedAmount, final: this.raiseAmount });
  }

  setMinRaise(): void {
    console.debug('setMinRaise: setting to', this.minRaiseAmount);
    this.raiseAmount = this.minRaiseAmount;
  }

  setHalfPot(): void {
    this.setBetPercentage(0.5);
  }

  setFullPot(): void {
    this.setBetPercentage(1);
  }

  setAllIn(): void {
    this.raiseAmount = this.maxRaiseAmount;
  }

  
  getButtonLabel(): string {
    if (!this.game) return 'Bet';
    return this.game.currentBet > 0 ? 'Raise' : 'Bet';
  }

  canRaise(): boolean {
    if (!this.currentPlayer) return false;
    return this.currentPlayer.chips > 0 && !this.currentPlayer.folded && !this.currentPlayer.isAllIn;
  }

  isFolded(): boolean {
    return this.currentPlayer?.folded || false;
  }
}