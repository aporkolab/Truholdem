import { Component, ElementRef, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { Router } from '@angular/router';
import { NgFor, NgIf, AsyncPipe } from '@angular/common';
import { Subject, takeUntil, filter, delay } from 'rxjs';

import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';
import { PlayerService } from '../services/player.service';
import { PokerService, PlayerInfo } from '../services/poker.service';
import { RaiseInputComponent } from '../raise-input/raise-input.component';

@Component({
    selector: 'app-game-table',
    templateUrl: './game-table.component.html',
    styleUrls: ['./game-table.component.scss'],
    imports: [NgFor, NgIf, AsyncPipe, RaiseInputComponent]
})
export class GameTableComponent implements OnInit, OnDestroy {
  private pokerService = inject(PokerService);
  private playerService = inject(PlayerService);
  private router = inject(Router);
  private destroy$ = new Subject<void>();

  
  game: Game = new Game();
  currentPot = 0;
  
  
  humanPlayer: Player | undefined;
  
  
  showModal = false;
  gameResultMessage = '';
  winningHandDescription = '';
  isLoading = false;
  errorMessage = '';
  
  
  private processingBots = false;

  @ViewChild(RaiseInputComponent) raiseInputComponent!: RaiseInputComponent;
  @ViewChild('raiseModal') raiseModal!: ElementRef;

  ngOnInit(): void {
    this.subscribeToGameState();
    this.initializeGame();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  

  private subscribeToGameState(): void {
    
    this.pokerService.game$.pipe(
      takeUntil(this.destroy$),
      filter(game => game !== null)
    ).subscribe(game => {
      if (game) {
        this.updateGameState(game);
      }
    });

    
    this.pokerService.loading$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(loading => {
      this.isLoading = loading;
    });

    
    this.pokerService.error$.pipe(
      takeUntil(this.destroy$),
      filter(error => error !== null)
    ).subscribe(error => {
      if (error) {
        this.errorMessage = error;
        console.error('Game error:', error);
      }
    });
  }

  private initializeGame(): void {
    
    this.pokerService.getGameStatus().subscribe({
      next: () => {
        
      },
      error: () => {
        
        this.startNewGame();
      }
    });
  }

  

  private updateGameState(game: Game): void {
    this.game = game;
    this.currentPot = game.currentPot || this.calculateCurrentPot();
    
    
    this.humanPlayer = this.game.players.find(p => !p.name?.startsWith('Bot'));
    
    
    this.sortPlayersForDisplay();
    
    
    if (game.isFinished || game.phase === 'SHOWDOWN') {
      this.handleGameEnd();
    } else if (!this.processingBots) {
      this.processBotsIfNeeded();
    }
  }

  private sortPlayersForDisplay(): void {
    if (this.game?.players) {
      const bots = this.game.players.filter(p => p.name?.startsWith('Bot'));
      const humans = this.game.players.filter(p => !p.name?.startsWith('Bot'));
      this.game.players = [...bots, ...humans];
    }
  }

  private calculateCurrentPot(): number {
    if (!this.game?.players) return 0;
    return this.game.players.reduce((total, player) => total + (player.betAmount || 0), 0);
  }

  

  fold(): void {
    if (!this.humanPlayer || !this.canPlayerAct()) return;

    this.pokerService.fold(this.humanPlayer.id).subscribe({
      error: (error) => console.error('Error during fold:', error)
    });
  }

  check(): void {
    if (!this.humanPlayer || !this.canPlayerAct()) return;

    if (!this.canCheck()) {
      this.errorMessage = 'Cannot check when facing a bet. Call, raise, or fold.';
      return;
    }

    this.pokerService.check(this.humanPlayer.id).subscribe({
      error: (error) => console.error('Error during check:', error)
    });
  }

  call(): void {
    if (!this.humanPlayer || !this.canPlayerAct()) return;

    this.pokerService.call(this.humanPlayer.id).subscribe({
      error: (error) => console.error('Error during call:', error)
    });
  }

  bet(amount: number): void {
    if (!this.humanPlayer || !this.canPlayerAct()) return;

    this.pokerService.bet(this.humanPlayer.id, amount).subscribe({
      error: (error) => console.error('Error during bet:', error)
    });
  }

  raise(amount: number): void {
    if (!this.humanPlayer || !this.canPlayerAct()) return;

    this.pokerService.raise(this.humanPlayer.id, amount).subscribe({
      error: (error) => console.error('Error during raise:', error)
    });
  }

  allIn(): void {
    if (!this.humanPlayer || !this.canPlayerAct()) return;

    const allInAmount = this.humanPlayer.chips + (this.humanPlayer.betAmount || 0);
    
    if (this.game.currentBet > 0) {
      this.pokerService.raise(this.humanPlayer.id, allInAmount).subscribe({
        error: (error) => console.error('Error during all-in:', error)
      });
    } else {
      this.pokerService.bet(this.humanPlayer.id, allInAmount).subscribe({
        error: (error) => console.error('Error during all-in:', error)
      });
    }
  }

  
  handleRaiseAction(amount?: number): void {
    if (amount && this.humanPlayer) {
      if (this.game.currentBet > 0) {
        this.raise(amount);
      } else {
        this.bet(amount);
      }
    }
  }

  

  startNewGame(): void {
    const registeredPlayers = this.playerService.getPlayers();
    
    let playersToStart: PlayerInfo[];
    if (registeredPlayers && registeredPlayers.length >= 2) {
      playersToStart = registeredPlayers.map(p => ({
        name: p.name,
        startingChips: p.chips || 1000,
        isBot: p.isBot
      }));
    } else {
      
      playersToStart = [
        { name: 'Player', startingChips: 1000, isBot: false },
        { name: 'Bot1', startingChips: 1000, isBot: true },
        { name: 'Bot2', startingChips: 1000, isBot: true }
      ];
    }

    this.pokerService.startGame(playersToStart).subscribe({
      next: () => {
        this.closeModal();
        this.errorMessage = '';
      },
      error: (error) => console.error('Error starting game:', error)
    });
  }

  startNewHand(): void {
    this.pokerService.startNewHand().subscribe({
      next: () => {
        this.closeModal();
        this.errorMessage = '';
      },
      error: (error) => {
        console.error('Error starting new hand:', error);
        
        this.startNewGame();
      }
    });
  }

  resetGame(): void {
    this.pokerService.resetGame().subscribe({
      next: () => {
        this.closeModal();
        this.startNewGame();
      },
      error: (error) => console.error('Error resetting game:', error)
    });
  }

  

  private processBotsIfNeeded(): void {
    if (this.processingBots || this.game.isFinished || this.game.phase === 'SHOWDOWN') {
      return;
    }

    
    const currentPlayer = this.game.players[this.game.currentPlayerIndex || 0];
    if (currentPlayer && currentPlayer.name?.startsWith('Bot') && !currentPlayer.folded && !currentPlayer.isAllIn) {
      this.processingBots = true;
      this.executeBotAction(currentPlayer.id);
    }
  }

  private executeBotAction(botId: string): void {
    
    setTimeout(() => {
      this.pokerService.executeBotAction(botId).subscribe({
        next: () => {
          this.processingBots = false;
          
        },
        error: (error) => {
          console.error('Error during bot action:', error);
          this.processingBots = false;
        }
      });
    }, 800);
  }

  

  private handleGameEnd(): void {
    if (this.game.winnerName) {
      this.gameResultMessage = `Winner: ${this.game.winnerName}`;
      this.winningHandDescription = this.game.winningHandDescription || '';
    } else {
      
      this.pokerService.endGame().subscribe({
        next: (result) => {
          this.gameResultMessage = result.message || 'Game Over';
        },
        error: () => {
          this.gameResultMessage = 'Game Over';
        }
      });
    }
    this.showModal = true;
  }

  

  canPlayerAct(): boolean {
    if (!this.humanPlayer || this.game.isFinished) return false;
    if (this.humanPlayer.folded || this.humanPlayer.isAllIn) return false;
    
    
    const currentPlayer = this.game.players[this.game.currentPlayerIndex || 0];
    return currentPlayer?.id === this.humanPlayer.id;
  }

  canCheck(): boolean {
    if (!this.humanPlayer) return false;
    return (this.humanPlayer.betAmount || 0) >= this.game.currentBet;
  }

  canCall(): boolean {
    if (!this.humanPlayer) return false;
    const callAmount = this.game.currentBet - (this.humanPlayer.betAmount || 0);
    return callAmount > 0 && callAmount <= this.humanPlayer.chips;
  }

  getCallAmount(): number {
    if (!this.humanPlayer) return 0;
    return Math.max(0, this.game.currentBet - (this.humanPlayer.betAmount || 0));
  }

  getMinRaiseAmount(): number {
    return this.game.currentBet + (this.game.minRaiseAmount || this.game.bigBlind || 20);
  }

  isPlayerTurn(player: Player): boolean {
    const currentPlayer = this.game.players[this.game.currentPlayerIndex || 0];
    return currentPlayer?.id === player.id && !player.folded && !player.isAllIn;
  }

  isDealer(playerIndex: number): boolean {
    return playerIndex === (this.game.dealerPosition || 0);
  }

  getPlayerStatus(player: Player): string {
    if (player.folded) return 'Folded';
    if (player.isAllIn) return 'All-In';
    if (player.chips === 0) return 'Out';
    return '';
  }

  isFolded(): boolean {
    return this.humanPlayer?.folded || false;
  }

  getPhaseDisplayName(): string {
    const phases: Record<string, string> = {
      'PRE_FLOP': 'Pre-Flop',
      'FLOP': 'Flop',
      'TURN': 'Turn',
      'RIVER': 'River',
      'SHOWDOWN': 'Showdown'
    };
    return phases[this.game.phase] || this.game.phase;
  }

  

  getCardImage(card: Card): string {
    if (!card) return 'assets/cards/back.png';

    let rank = card.value?.toLowerCase() || '';
    const rankMap: Record<string, string> = {
      'two': '2', 'three': '3', 'four': '4', 'five': '5',
      'six': '6', 'seven': '7', 'eight': '8', 'nine': '9', 'ten': '10'
    };
    rank = rankMap[rank] || rank;

    const suit = card.suit?.toLowerCase() || '';
    return `assets/cards/${rank}_of_${suit}.png`;
  }

  getCardBackImage(): string {
    return 'assets/cards/back.png';
  }

  

  openModal(): void {
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.gameResultMessage = '';
    this.winningHandDescription = '';
  }

  

  goToLobby(): void {
    this.router.navigate(['/']);
  }
}

