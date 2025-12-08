import { Component, EventEmitter, Input, OnInit, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';

@Component({
    selector: 'app-raise-input',
    templateUrl: './raise-input.component.html',
    styleUrls: ['./raise-input.component.scss'],
    imports: [NgIf, FormsModule]
})
export class RaiseInputComponent implements OnInit, OnChanges {
  @Input() game!: Game;
  @Output() actionTaken = new EventEmitter<number>();

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
    if (!this.game?.players) return;

    
    this.currentPlayer = this.game.players.find(p => !p.name?.startsWith('Bot'));
    
    if (!this.currentPlayer) return;

    
    this.maxRaiseAmount = this.currentPlayer.chips + (this.currentPlayer.betAmount || 0);

    
    const currentBet = this.game.currentBet || 0;
    const minRaiseIncrement = this.game.minRaiseAmount || this.game.bigBlind || environment.defaultBigBlind;
    
    if (currentBet > 0) {
      
      this.minRaiseAmount = currentBet + minRaiseIncrement;
    } else {
      
      this.minRaiseAmount = this.game.bigBlind || environment.defaultBigBlind;
    }

    
    this.raiseAmount = this.minRaiseAmount;
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
  }

  confirmRaise(): void {
    if (!this.currentPlayer) {
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

    
    this.actionTaken.emit(this.raiseAmount);
    this.isRaiseInputVisible = false;
    this.errorMessage = '';
  }

  
  setBetPercentage(percentage: number): void {
    const pot = this.game.currentPot || 0;
    const calculatedAmount = Math.ceil(pot * percentage);
    this.raiseAmount = Math.max(this.minRaiseAmount, Math.min(calculatedAmount, this.maxRaiseAmount));
  }

  setMinRaise(): void {
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