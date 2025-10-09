import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable, EMPTY } from 'rxjs';
import {
  switchMap,
  tap,
  withLatestFrom,
  catchError,
  exhaustMap,
  concatMap,
  delay,
  filter
} from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';






export interface PlayerInfo {
  name: string;
  startingChips: number;
  isBot: boolean;
}


export interface PlayerActionRecord {
  type: PlayerActionType;
  playerId: string;
  playerName: string;
  amount?: number;
  timestamp: number;
}

export type PlayerActionType = 'FOLD' | 'CHECK' | 'CALL' | 'BET' | 'RAISE' | 'ALL_IN';


export interface GameResult {
  message: string;
  winnerName?: string;
  winningHand?: string;
}


export type ConnectionStatus = 'connected' | 'disconnected' | 'reconnecting';


export interface GameStoreState {
  game: Game | null;
  gameHistory: Game[];
  isLoading: boolean;
  error: string | null;
  lastAction: PlayerActionRecord | null;
  processingBots: boolean;
  connectionStatus: ConnectionStatus;
  actionInProgress: boolean;
}


export interface GameViewModel {
  game: Game | null;
  currentPlayer: Player | null;
  humanPlayer: Player | undefined;
  isLoading: boolean;
  error: string | null;
  isHumanTurn: boolean;
  canCheck: boolean;
  canCall: boolean;
  callAmount: number;
  minRaiseAmount: number;
  maxRaiseAmount: number;
  potSize: number;
  phase: string;
  phaseDisplayName: string;
  communityCards: Card[];
  isGameFinished: boolean;
  activePlayers: Player[];
  lastAction: PlayerActionRecord | null;
  processingBots: boolean;
  canPlayerAct: boolean;
  dealerPosition: number;
  winnerName: string | undefined;
  winningHandDescription: string | undefined;
}





const initialState: GameStoreState = {
  game: null,
  gameHistory: [],
  isLoading: false,
  error: null,
  lastAction: null,
  processingBots: false,
  connectionStatus: 'disconnected',
  actionInProgress: false
};






@Injectable()
export class GameStore extends ComponentStore<GameStoreState> {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/poker`;

  constructor() {
    super(initialState);
  }

  
  
  

  
  readonly game$ = this.select(state => state.game);

  
  readonly isLoading$ = this.select(state => state.isLoading);

  
  readonly error$ = this.select(state => state.error);

  
  readonly lastAction$ = this.select(state => state.lastAction);

  
  readonly processingBots$ = this.select(state => state.processingBots);

  
  readonly connectionStatus$ = this.select(state => state.connectionStatus);

  
  readonly gameHistory$ = this.select(state => state.gameHistory);

  
  readonly actionInProgress$ = this.select(state => state.actionInProgress);

  
  
  

  
  readonly players$ = this.select(
    this.game$,
    game => game?.players ?? []
  );

  
  readonly currentPlayer$ = this.select(
    this.game$,
    game => {
      if (!game || game.currentPlayerIndex === undefined || game.currentPlayerIndex === null) {
        return null;
      }
      if (game.currentPlayerIndex < 0 || game.currentPlayerIndex >= game.players.length) {
        return null;
      }
      return game.players[game.currentPlayerIndex] ?? null;
    }
  );

  
  readonly humanPlayer$ = this.select(
    this.game$,
    game => game?.players.find(p => !p.isBot && !p.name?.startsWith('Bot'))
  );

  
  readonly isHumanTurn$ = this.select(
    this.currentPlayer$,
    this.humanPlayer$,
    (currentPlayer, humanPlayer) => {
      const result = currentPlayer && humanPlayer && currentPlayer.id === humanPlayer.id;
      console.log('[GameStore] isHumanTurn check:', {
        currentPlayerExists: !!currentPlayer,
        currentPlayerId: currentPlayer?.id,
        currentPlayerName: currentPlayer?.name,
        humanPlayerExists: !!humanPlayer,
        humanPlayerId: humanPlayer?.id,
        humanPlayerName: humanPlayer?.name,
        idsMatch: currentPlayer?.id === humanPlayer?.id,
        result: !!result
      });
      if (!currentPlayer || !humanPlayer) return false;
      return currentPlayer.id === humanPlayer.id;
    }
  );

  
  readonly potSize$ = this.select(
    this.game$,
    game => game?.currentPot ?? 0
  );

  
  readonly phase$ = this.select(
    this.game$,
    game => game?.phase ?? 'PRE_FLOP'
  );

  
  readonly phaseDisplayName$ = this.select(
    this.phase$,
    phase => {
      const phases: Record<string, string> = {
        'PRE_FLOP': 'Pre-Flop',
        'FLOP': 'Flop',
        'TURN': 'Turn',
        'RIVER': 'River',
        'SHOWDOWN': 'Showdown'
      };
      return phases[phase] || phase;
    }
  );

  
  readonly communityCards$ = this.select(
    this.game$,
    game => game?.communityCards ?? []
  );

  
  readonly currentBet$ = this.select(
    this.game$,
    game => game?.currentBet ?? 0
  );

  
  readonly canCheck$ = this.select(
    this.humanPlayer$,
    this.currentBet$,
    this.game$,
    (humanPlayer, currentBet, game) => {
      // Safety: require game to exist
      if (!humanPlayer || !game) return false;
      const playerBet = humanPlayer.betAmount ?? 0;
      const canCheck = playerBet >= currentBet;
      console.log('[GameStore] canCheck:', { playerBet, currentBet, canCheck });
      return canCheck;
    }
  );

  
  readonly callAmount$ = this.select(
    this.humanPlayer$,
    this.currentBet$,
    (humanPlayer, currentBet) => {
      if (!humanPlayer) return 0;
      return Math.max(0, currentBet - (humanPlayer.betAmount ?? 0));
    }
  );

  
  readonly canCall$ = this.select(
    this.callAmount$,
    this.humanPlayer$,
    (callAmount, humanPlayer) => {
      if (!humanPlayer || callAmount <= 0) return false;
      return (humanPlayer.chips ?? 0) >= callAmount;
    }
  );

  
  readonly minRaiseAmount$ = this.select(
    this.game$,
    this.currentBet$,
    (game, currentBet) => {
      if (!game) return environment.defaultBigBlind * 2;
      const minRaise = game.minRaiseAmount ?? game.bigBlind ?? environment.defaultBigBlind;
      return currentBet + minRaise;
    }
  );

  
  readonly maxRaiseAmount$ = this.select(
    this.humanPlayer$,
    humanPlayer => (humanPlayer?.chips ?? 0) + (humanPlayer?.betAmount ?? 0)
  );

  
  readonly isGameFinished$ = this.select(
    this.game$,
    this.phase$,
    (game, phase) => {
      return game?.isFinished === true || phase === 'SHOWDOWN';
    }
  );

  
  readonly activePlayers$ = this.select(
    this.players$,
    players => players.filter(p => !p.folded)
  );

  
  readonly activePlayerCount$ = this.select(
    this.activePlayers$,
    players => players.length
  );

  
  readonly canPlayerAct$ = this.select(
    this.isHumanTurn$,
    this.humanPlayer$,
    this.isGameFinished$,
    this.actionInProgress$,
    this.processingBots$,
    (isHumanTurn, humanPlayer, isGameFinished, actionInProgress, processingBots) => {
      console.log('[GameStore] canPlayerAct check:', {
        isHumanTurn,
        humanPlayerExists: !!humanPlayer,
        humanPlayerId: humanPlayer?.id,
        isGameFinished,
        actionInProgress,
        processingBots,
        folded: humanPlayer?.folded,
        isAllIn: humanPlayer?.isAllIn
      });
      // Block actions when: not human's turn, no human player, game finished, action in progress, or bots processing
      if (!isHumanTurn || !humanPlayer || isGameFinished || actionInProgress || processingBots) {
        return false;
      }
      return !humanPlayer.folded && !humanPlayer.isAllIn;
    }
  );

  
  readonly dealerPosition$ = this.select(
    this.game$,
    game => game?.dealerPosition ?? 0
  );

  
  readonly winnerName$ = this.select(
    this.game$,
    game => game?.winnerName
  );

  
  readonly winningHandDescription$ = this.select(
    this.game$,
    game => game?.winningHandDescription
  );

  
  readonly activeBots$ = this.select(
    this.players$,
    players => players.filter(p =>
      p.isBot &&
      !p.folded &&
      !p.isAllIn &&
      (p.chips ?? 0) > 0
    )
  );

  
  readonly currentBot$ = this.select(
    this.currentPlayer$,
    currentPlayer => {
      console.log('[GameStore] currentPlayer check:', {
        name: currentPlayer?.name,
        isBot: currentPlayer?.isBot,
        folded: currentPlayer?.folded
      });
      if (currentPlayer?.isBot && !currentPlayer.folded) {
        console.log('[GameStore] Bot detected, should act:', currentPlayer.name);
        return currentPlayer;
      }
      return null;
    }
  );

  
  
  

  
  readonly vm$: Observable<GameViewModel> = this.select(
    this.game$,
    this.currentPlayer$,
    this.humanPlayer$,
    this.isLoading$,
    this.error$,
    this.isHumanTurn$,
    this.canCheck$,
    this.canCall$,
    this.callAmount$,
    this.minRaiseAmount$,
    this.maxRaiseAmount$,
    this.potSize$,
    this.phase$,
    this.phaseDisplayName$,
    this.communityCards$,
    this.isGameFinished$,
    this.activePlayers$,
    this.lastAction$,
    this.processingBots$,
    this.canPlayerAct$,
    this.dealerPosition$,
    this.winnerName$,
    this.winningHandDescription$,
    (
      game, currentPlayer, humanPlayer, isLoading, error,
      isHumanTurn, canCheck, canCall, callAmount, minRaiseAmount, maxRaiseAmount,
      potSize, phase, phaseDisplayName, communityCards, isGameFinished,
      activePlayers, lastAction, processingBots, canPlayerAct,
      dealerPosition, winnerName, winningHandDescription
    ): GameViewModel => ({
      game,
      currentPlayer,
      humanPlayer,
      isLoading,
      error,
      isHumanTurn,
      canCheck,
      canCall,
      callAmount,
      minRaiseAmount,
      maxRaiseAmount,
      potSize,
      phase,
      phaseDisplayName,
      communityCards,
      isGameFinished,
      activePlayers,
      lastAction,
      processingBots,
      canPlayerAct,
      dealerPosition,
      winnerName,
      winningHandDescription
    }),
    { debounce: true }
  );

  
  
  

  
  readonly setGame = this.updater((state, game: Game | null) => ({
    ...state,
    game,
    error: null,
    isLoading: false
  }));

  
  readonly setLoading = this.updater((state, isLoading: boolean) => ({
    ...state,
    isLoading
  }));

  
  readonly setError = this.updater((state, error: string | null) => ({
    ...state,
    error,
    isLoading: false
  }));

  
  readonly clearError = this.updater(state => ({
    ...state,
    error: null
  }));

  
  readonly recordAction = this.updater((state, action: PlayerActionRecord) => ({
    ...state,
    lastAction: action
  }));

  
  readonly clearLastAction = this.updater(state => ({
    ...state,
    lastAction: null
  }));

  
  readonly setProcessingBots = this.updater((state, processingBots: boolean) => ({
    ...state,
    processingBots
  }));

  
  readonly setConnectionStatus = this.updater((state, connectionStatus: ConnectionStatus) => ({
    ...state,
    connectionStatus
  }));

  
  readonly setActionInProgress = this.updater((state, actionInProgress: boolean) => ({
    ...state,
    actionInProgress
  }));

  
  readonly addToHistory = this.updater((state, game: Game) => ({
    ...state,
    gameHistory: [...state.gameHistory, game]
  }));

  
  readonly clearHistory = this.updater(state => ({
    ...state,
    gameHistory: []
  }));

  
  readonly reset = this.updater(() => initialState);

  
  
  

  
  readonly startGame = this.effect<PlayerInfo[] | void>(trigger$ =>
    trigger$.pipe(
      tap(() => {
        this.setLoading(true);
        this.clearError();
        this.clearHistory();
      }),
      switchMap(players => {
        const playersToSend = players || this.getDefaultPlayers();
        return this.http.post<Game>(`${this.apiUrl}/start`, playersToSend).pipe(
          tapResponse(
            game => {
              console.log('[GameStore] Raw game from server:', JSON.stringify(game, null, 2));
              const idx = game.currentPlayerIndex ?? -1;
              console.log('[GameStore] currentPlayerIndex:', idx);
              console.log('[GameStore] Players:', game.players?.map(p => ({
                name: p.name,
                isBot: p.isBot,
                seatPosition: p.seatPosition
              })));

              this.setGame(game);
              this.logDebug('Game started', game);

              // If the first player to act is a bot, trigger bot processing
              if (idx >= 0 && idx < (game.players?.length ?? 0)) {
                const currentPlayer = game.players[idx];
                console.log('[GameStore] Current player should be:', currentPlayer.name, 'isBot:', currentPlayer.isBot);
                if (currentPlayer.isBot && !currentPlayer.folded) {
                  console.log('[GameStore] First player is a bot, triggering processBots');
                  setTimeout(() => this.processBots(), 100);
                }
              }
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        );
      })
    )
  );

  
  readonly refreshGame = this.effect<void>(trigger$ =>
    trigger$.pipe(
      switchMap(() =>
        this.http.get<Game>(`${this.apiUrl}/status`).pipe(
          tapResponse(
            game => {
              this.setGame(game);
              this.addToHistory(game);
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );

  
  readonly startNewHand = this.effect<void>(trigger$ =>
    trigger$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(() =>
        this.http.post<Game>(`${this.apiUrl}/new-match`, {}).pipe(
          tapResponse(
            game => {
              this.setGame(game);
              this.clearLastAction();
              this.logDebug('New hand started', game);

              // If the first player to act is a bot, trigger bot processing
              const idx = game.currentPlayerIndex ?? -1;
              if (idx >= 0 && idx < (game.players?.length ?? 0)) {
                const currentPlayer = game.players[idx];
                if (currentPlayer.isBot && !currentPlayer.folded) {
                  console.log('[GameStore] New hand: first player is a bot, triggering processBots');
                  setTimeout(() => this.processBots(), 100);
                }
              }
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );

  
  readonly resetGame = this.effect<void>(trigger$ =>
    trigger$.pipe(
      switchMap(() =>
        this.http.post(`${this.apiUrl}/reset`, {}, { responseType: 'text' }).pipe(
          tapResponse(
            () => {
              this.reset();
              this.logDebug('Game reset');
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );

  
  readonly playerAction = this.effect<{
    playerId: string;
    action: PlayerActionType;
    amount?: number;
  }>(action$ =>
    action$.pipe(
      tap(() => this.setActionInProgress(true)),
      withLatestFrom(this.players$),
      exhaustMap(([{ playerId, action, amount }, players]) => {
        const player = players.find(p => p.id === playerId);
        const playerName = player?.name ?? 'Unknown';

        let request$: Observable<string>;

        switch (action) {
          case 'FOLD':
            request$ = this.performAction('fold', { playerId });
            break;
          case 'CHECK':
            request$ = this.performAction('check', { playerId });
            break;
          case 'CALL':
            request$ = this.performAction('call', { playerId });
            break;
          case 'BET':
            request$ = this.http.post(
              `${this.apiUrl}/bet`,
              { playerId, amount },
              { responseType: 'text' }
            );
            break;
          case 'RAISE':
          case 'ALL_IN':
            request$ = this.http.post(
              `${this.apiUrl}/raise`,
              { playerId, amount },
              { responseType: 'text' }
            );
            break;
          default:
            return EMPTY;
        }

        return request$.pipe(
          tap(() => {
            this.recordAction({
              type: action,
              playerId,
              playerName,
              amount,
              timestamp: Date.now()
            });
          }),
          switchMap(() => this.http.get<Game>(`${this.apiUrl}/status`)),
          tapResponse(
            game => {
              // Debug: log who the next player is
              const idx = game.currentPlayerIndex ?? -1;
              console.log('[GameStore] After action - currentPlayerIndex:', idx);
              if (idx >= 0 && idx < (game.players?.length ?? 0)) {
                const nextPlayer = game.players[idx];
                console.log('[GameStore] Next player:', nextPlayer.name, 'isBot:', nextPlayer.isBot);
              }
              console.log('[GameStore] All players isBot status:', game.players?.map(p => ({ name: p.name, isBot: p.isBot })));

              this.setGame(game);
              this.addToHistory(game);
              this.setActionInProgress(false);
              this.logDebug('Action completed', { action, playerId });
            },
            (error: HttpErrorResponse) => {
              this.handleError(error);
              this.setActionInProgress(false);
            }
          )
        );
      })
    )
  );

  
  readonly processBots = this.effect<void>(trigger$ =>
    trigger$.pipe(
      withLatestFrom(this.currentBot$, this.isGameFinished$, this.processingBots$),
      filter(([, currentBot, isGameFinished, alreadyProcessing]) => {
        // Don't start if already processing, no bot, or game finished
        if (alreadyProcessing || !currentBot || isGameFinished) {
          console.log('[GameStore] processBots skipped:', { alreadyProcessing, hasBot: !!currentBot, isGameFinished });
          return false;
        }
        return true;
      }),
      tap(() => {
        console.log('[GameStore] processBots starting');
        this.setProcessingBots(true);
      }),
      delay(environment.botActionDelay || 800),
      concatMap(([, currentBot]) => {
        if (!currentBot) {
          this.setProcessingBots(false);
          return EMPTY;
        }

        console.log('[GameStore] Executing bot action for:', currentBot.name);
        return this.http.post<Game>(
          `${this.apiUrl}/bot-action/${currentBot.id}`,
          {}
        ).pipe(
          tapResponse(
            game => {
              // Log the game state after bot action
              const nextIdx = game.currentPlayerIndex ?? -1;
              const nextPlayer = nextIdx >= 0 && nextIdx < (game.players?.length ?? 0)
                ? game.players[nextIdx]
                : null;
              console.log('[GameStore] After bot action - phase:', game.phase,
                'currentPlayerIndex:', nextIdx,
                'nextPlayer:', nextPlayer?.name,
                'nextPlayerIsBot:', nextPlayer?.isBot);

              const botPlayer = game.players.find(p => p.id === currentBot.id);
              if (botPlayer) {
                this.recordAction({
                  type: this.inferActionType(botPlayer),
                  playerId: currentBot.id,
                  playerName: currentBot.name ?? 'Bot',
                  amount: botPlayer.betAmount,
                  timestamp: Date.now()
                });
              }

              this.logDebug('Bot action executed', { botId: currentBot.id });

              // CRITICAL: Set processingBots to false BEFORE updating game state
              // Otherwise currentBot$ emits while processingBots is still true,
              // causing the next bot to be skipped
              this.setProcessingBots(false);

              // Now update game state - this triggers currentBot$ which may call processBots again
              this.setGame(game);
              this.addToHistory(game);
            },
            (error: HttpErrorResponse) => {
              this.handleError(error);
              this.setProcessingBots(false);
            }
          ),
          catchError(() => {
            this.setProcessingBots(false);
            return EMPTY;
          })
        );
      })
    )
  );

  

  
  
  

  
  getPlayerById(playerId: string): Player | undefined {
    return this.get(state => state.game?.players.find(p => p.id === playerId));
  }

  
  isPlayerTurn(playerId: string): boolean {
    const state = this.get();
    if (!state.game || state.game.currentPlayerIndex === undefined) {
      return false;
    }
    const currentPlayer = state.game.players[state.game.currentPlayerIndex];
    return currentPlayer?.id === playerId;
  }

  
  isDealer(seatPosition: number): boolean {
    return this.get(state => state.game?.dealerPosition === seatPosition);
  }

  
  getPlayerStatus(player: Player): string {
    if (player.folded) return 'Folded';
    if (player.isAllIn) return 'All-In';
    if ((player.chips ?? 0) <= 0) return 'Out';
    return '';
  }

  
  
  

  
  private performAction(action: string, params: Record<string, string>): Observable<string> {
    const queryParams = new URLSearchParams(params).toString();
    return this.http.post(
      `${this.apiUrl}/${action}?${queryParams}`,
      null,
      { responseType: 'text' }
    );
  }

  
  private getDefaultPlayers(): PlayerInfo[] {
    return [
      { name: 'Player', startingChips: environment.defaultStartingChips || 1000, isBot: false },
      { name: 'Bot1', startingChips: environment.defaultStartingChips || 1000, isBot: true },
      { name: 'Bot2', startingChips: environment.defaultStartingChips || 1000, isBot: true }
    ];
  }

  
  private inferActionType(player: Player): PlayerActionType {
    if (player.folded) return 'FOLD';
    if (player.isAllIn) return 'ALL_IN';
    if ((player.betAmount ?? 0) > 0) return 'BET';
    return 'CHECK';
  }

  
  private handleError(error: HttpErrorResponse): void {
    const errorMessage = error.error instanceof ErrorEvent
      ? `Error: ${error.error.message}`
      : error.error?.message || `Error Code: ${error.status}, Message: ${error.message}`;

    this.setError(errorMessage);
    this.logDebug('API Error', { error, errorMessage });
  }

  
  private logDebug(message: string, data?: unknown): void {
    if (environment.logApiCalls) {
      console.log(`[GameStore] ${message}`, data);
    }
  }
}
