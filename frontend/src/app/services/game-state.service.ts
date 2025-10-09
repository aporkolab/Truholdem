import { Injectable, computed, signal } from '@angular/core';
import { Game, GamePhase } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';


export interface PlayerAction {
  type: 'FOLD' | 'CHECK' | 'CALL' | 'BET' | 'RAISE' | 'ALL_IN';
  playerId: string;
  playerName: string;
  amount?: number;
  timestamp: number;
}


interface GameStateInternal {
  game: Game | null;
  isLoading: boolean;
  error: string | null;
  lastAction: PlayerAction | null;
  selectedPlayer: Player | null;
  connectionStatus: 'connected' | 'disconnected' | 'reconnecting';
}


@Injectable({ providedIn: 'root' })
export class GameStateService {
  
  
  

  private readonly _gameState = signal<GameStateInternal>({
    game: null,
    isLoading: false,
    error: null,
    lastAction: null,
    selectedPlayer: null,
    connectionStatus: 'disconnected'
  });

  
  
  

  
  readonly game = computed(() => this._gameState().game);
  
  
  readonly isLoading = computed(() => this._gameState().isLoading);
  
  
  readonly error = computed(() => this._gameState().error);
  
  
  readonly lastAction = computed(() => this._gameState().lastAction);
  
  
  readonly selectedPlayer = computed(() => this._gameState().selectedPlayer);
  
  
  readonly connectionStatus = computed(() => this._gameState().connectionStatus);

  
  
  

  
  readonly players = computed(() => this.game()?.players ?? []);

  
  readonly currentPlayer = computed<Player | null>(() => {
    const game = this.game();
    if (!game || game.currentPlayerIndex === undefined) return null;
    return game.players[game.currentPlayerIndex] ?? null;
  });

  
  readonly humanPlayer = computed<Player | undefined>(() => {
    return this.game()?.players.find(p => !p.name?.startsWith('Bot') && !p.isBot);
  });

  
  readonly isHumanTurn = computed<boolean>(() => {
    const current = this.currentPlayer();
    const human = this.humanPlayer();
    return !!current && !!human && current.id === human.id;
  });

  
  readonly potSize = computed<number>(() => this.game()?.currentPot ?? 0);

  
  readonly communityCards = computed<Card[]>(() => this.game()?.communityCards ?? []);

  
  readonly phase = computed<GamePhase | string>(() => this.game()?.phase ?? 'PRE_FLOP');

  
  readonly phaseDisplayName = computed<string>(() => {
    const phases: Record<string, string> = {
      'PRE_FLOP': 'Pre-Flop',
      'FLOP': 'Flop',
      'TURN': 'Turn',
      'RIVER': 'River',
      'SHOWDOWN': 'Showdown'
    };
    return phases[this.phase()] || this.phase();
  });

  
  readonly currentBet = computed<number>(() => this.game()?.currentBet ?? 0);

  
  readonly minRaiseAmount = computed<number>(() => {
    const game = this.game();
    if (!game) return 20;
    return game.currentBet + (game.minRaiseAmount || game.bigBlind || 20);
  });

  
  readonly canCheck = computed<boolean>(() => {
    const human = this.humanPlayer();
    const bet = this.currentBet();
    return (human?.betAmount ?? 0) >= bet;
  });

  
  readonly callAmount = computed<number>(() => {
    const human = this.humanPlayer();
    const bet = this.currentBet();
    return Math.max(0, bet - (human?.betAmount ?? 0));
  });

  
  readonly canCall = computed<boolean>(() => {
    const human = this.humanPlayer();
    const amount = this.callAmount();
    return amount > 0 && (human?.chips ?? 0) >= amount;
  });

  
  readonly canPlayerAct = computed<boolean>(() => {
    const human = this.humanPlayer();
    const game = this.game();
    if (!human || !game || game.isFinished) return false;
    if (human.folded || human.isAllIn) return false;
    return this.isHumanTurn();
  });

  
  readonly isGameFinished = computed<boolean>(() => {
    const game = this.game();
    return game?.isFinished || game?.phase === 'SHOWDOWN' || false;
  });

  
  readonly activePlayers = computed<Player[]>(() => {
    return this.players().filter(p => !p.folded);
  });

  
  readonly activePlayerCount = computed<number>(() => this.activePlayers().length);

  
  readonly activeBots = computed<Player[]>(() => {
    return this.players().filter(p => 
      p.name?.startsWith('Bot') && 
      !p.folded && 
      !p.isAllIn && 
      p.chips > 0
    );
  });

  
  readonly dealerPosition = computed<number>(() => this.game()?.dealerPosition ?? 0);

  
  readonly winnerName = computed<string | undefined>(() => this.game()?.winnerName);

  
  readonly winningHandDescription = computed<string | undefined>(() => 
    this.game()?.winningHandDescription
  );

  
  readonly handNumber = computed<number>(() => this.game()?.handNumber ?? 1);

  
  readonly humanChips = computed<number>(() => this.humanPlayer()?.chips ?? 0);

  
  readonly humanBet = computed<number>(() => this.humanPlayer()?.betAmount ?? 0);

  
  readonly humanHand = computed<Card[]>(() => this.humanPlayer()?.hand ?? []);

  
  readonly isHumanFolded = computed<boolean>(() => this.humanPlayer()?.folded ?? false);

  
  readonly isHumanAllIn = computed<boolean>(() => this.humanPlayer()?.isAllIn ?? false);

  
  
  

  
  setGame(game: Game | null): void {
    this._gameState.update(state => ({
      ...state,
      game,
      error: null
    }));
  }

  
  setLoading(isLoading: boolean): void {
    this._gameState.update(state => ({
      ...state,
      isLoading
    }));
  }

  
  setError(error: string | null): void {
    this._gameState.update(state => ({
      ...state,
      error,
      isLoading: false
    }));
  }

  
  clearError(): void {
    this.setError(null);
  }

  
  setLastAction(action: PlayerAction | null): void {
    this._gameState.update(state => ({
      ...state,
      lastAction: action
    }));
  }

  
  recordAction(
    type: PlayerAction['type'],
    playerId: string,
    playerName: string,
    amount?: number
  ): void {
    this.setLastAction({
      type,
      playerId,
      playerName,
      amount,
      timestamp: Date.now()
    });
  }

  
  setSelectedPlayer(player: Player | null): void {
    this._gameState.update(state => ({
      ...state,
      selectedPlayer: player
    }));
  }

  
  setConnectionStatus(status: 'connected' | 'disconnected' | 'reconnecting'): void {
    this._gameState.update(state => ({
      ...state,
      connectionStatus: status
    }));
  }

  
  reset(): void {
    this._gameState.set({
      game: null,
      isLoading: false,
      error: null,
      lastAction: null,
      selectedPlayer: null,
      connectionStatus: 'disconnected'
    });
  }

  
  updateGame(updates: Partial<Game>): void {
    const currentGame = this.game();
    if (!currentGame) return;

    this.setGame({
      ...currentGame,
      ...updates
    } as Game);
  }

  
  
  

  
  isPlayerTurn(playerId: string): boolean {
    return this.currentPlayer()?.id === playerId;
  }

  
  getPlayerById(playerId: string): Player | undefined {
    return this.players().find(p => p.id === playerId);
  }

  
  isDealer(playerIndex: number): boolean {
    return playerIndex === this.dealerPosition();
  }

  
  getPlayerStatus(player: Player): string {
    if (player.folded) return 'Folded';
    if (player.isAllIn) return 'All-In';
    if (player.chips === 0) return 'Out';
    return '';
  }
}
