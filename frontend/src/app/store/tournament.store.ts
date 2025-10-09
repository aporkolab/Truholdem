import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { EMPTY, timer, of } from 'rxjs';
import {
  switchMap,
  tap,
  withLatestFrom,
  catchError,
  takeUntil,
  map,
  distinctUntilChanged,
  shareReplay
} from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  Tournament,
  TournamentListItem,
  TournamentTable,
  TournamentPlayer,
  TournamentUpdate,
  BlindLevel,
  calculateTimeRemaining,
  formatTimeRemaining,
  getNextBlindLevel
} from '../model/tournament';
import { WebSocketService } from '../services/websocket.service';





export interface TournamentStoreState {
  tournaments: TournamentListItem[];
  activeTournament: Tournament | null;
  myTable: TournamentTable | null;
  myPlayer: TournamentPlayer | null;

  isLoading: boolean;
  isRegistering: boolean;
  error: string | null;

  connectionStatus: 'connected' | 'disconnected' | 'reconnecting';
  lastUpdate: TournamentUpdate | null;
}





export interface TournamentListViewModel {
  tournaments: TournamentListItem[];
  openTournaments: TournamentListItem[];
  runningTournaments: TournamentListItem[];
  isLoading: boolean;
  error: string | null;
}

export interface TournamentLobbyViewModel {
  tournament: Tournament | null;
  registeredPlayers: TournamentPlayer[];
  canRegister: boolean;
  isRegistered: boolean;
  isLoading: boolean;
  error: string | null;
  spotsRemaining: number;
  prizePool: number;
}

export interface TournamentTableViewModel {
  tournament: Tournament | null;
  table: TournamentTable | null;
  myPlayer: TournamentPlayer | null;


  currentBlinds: BlindLevel | null;
  nextBlinds: BlindLevel | null;
  timeToNextLevel: number;
  formattedTimeRemaining: string;


  remainingPlayers: number;
  averageStack: number;
  myRank: number | null;
  totalPlayers: number;


  isOnBreak: boolean;
  isFinalTable: boolean;
  isEliminated: boolean;

  isLoading: boolean;
  error: string | null;
}





const initialState: TournamentStoreState = {
  tournaments: [],
  activeTournament: null,
  myTable: null,
  myPlayer: null,

  isLoading: false,
  isRegistering: false,
  error: null,

  connectionStatus: 'disconnected',
  lastUpdate: null
};





@Injectable()
export class TournamentStore extends ComponentStore<TournamentStoreState> {
  private readonly http = inject(HttpClient);
  private readonly wsService = inject(WebSocketService);

  private readonly apiUrl = `${environment.apiUrl}/tournament`;

  constructor() {
    super(initialState);
  }





  readonly tournaments$ = this.select(state => state.tournaments);
  readonly activeTournament$ = this.select(state => state.activeTournament);
  readonly myTable$ = this.select(state => state.myTable);
  readonly myPlayer$ = this.select(state => state.myPlayer);
  readonly isLoading$ = this.select(state => state.isLoading);
  readonly isRegistering$ = this.select(state => state.isRegistering);
  readonly error$ = this.select(state => state.error);
  readonly connectionStatus$ = this.select(state => state.connectionStatus);
  readonly lastUpdate$ = this.select(state => state.lastUpdate);





  readonly openTournaments$ = this.select(
    this.tournaments$,
    tournaments => tournaments.filter(t => t.status === 'REGISTERING')
  );

  readonly runningTournaments$ = this.select(
    this.tournaments$,
    tournaments => tournaments.filter(t =>
      t.status === 'RUNNING' || t.status === 'FINAL_TABLE'
    )
  );

  readonly currentBlinds$ = this.select(
    this.activeTournament$,
    tournament => tournament?.currentBlinds ?? null
  );

  readonly nextBlinds$ = this.select(
    this.activeTournament$,
    tournament => {
      if (!tournament) return null;
      return getNextBlindLevel(tournament.currentLevel, tournament.config.blindLevels);
    }
  );

  readonly remainingPlayers$ = this.select(
    this.activeTournament$,
    tournament => tournament?.remainingPlayers ?? 0
  );

  readonly averageStack$ = this.select(
    this.activeTournament$,
    tournament => tournament?.averageStack ?? 0
  );

  readonly prizePool$ = this.select(
    this.activeTournament$,
    tournament => tournament?.prizePool ?? 0
  );

  readonly isOnBreak$ = this.select(
    this.activeTournament$,
    tournament => tournament?.status === 'PAUSED'
  );

  readonly isFinalTable$ = this.select(
    this.activeTournament$,
    tournament => tournament?.status === 'FINAL_TABLE'
  );

  readonly isEliminated$ = this.select(
    this.myPlayer$,
    player => player?.isEliminated ?? false
  );

  readonly myRank$ = this.select(
    this.activeTournament$,
    this.myPlayer$,
    (tournament, player) => {
      if (!tournament || !player) return null;

      const sortedPlayers = [...tournament.registeredPlayers]
        .filter(p => !p.isEliminated)
        .sort((a, b) => (b.chips ?? 0) - (a.chips ?? 0));

      const index = sortedPlayers.findIndex(p => p.id === player.id);
      return index >= 0 ? index + 1 : null;
    }
  );





  readonly tournamentListVm$ = this.select(
    this.tournaments$,
    this.openTournaments$,
    this.runningTournaments$,
    this.isLoading$,
    this.error$,
    (tournaments, openTournaments, runningTournaments, isLoading, error): TournamentListViewModel => ({
      tournaments,
      openTournaments,
      runningTournaments,
      isLoading,
      error
    })
  );

  readonly tournamentLobbyVm$ = this.select(
    this.activeTournament$,
    this.myPlayer$,
    this.isLoading$,
    this.isRegistering$,
    this.error$,
    (tournament, myPlayer, isLoading, isRegistering, error): TournamentLobbyViewModel => ({
      tournament,
      registeredPlayers: tournament?.registeredPlayers ?? [],
      canRegister: tournament?.status === 'REGISTERING' &&
                   (tournament.registeredPlayers.length < tournament.config.maxPlayers),
      isRegistered: myPlayer !== null && !myPlayer.isEliminated,
      isLoading: isLoading || isRegistering,
      error,
      spotsRemaining: tournament
        ? tournament.config.maxPlayers - tournament.registeredPlayers.length
        : 0,
      prizePool: tournament?.prizePool ?? 0
    })
  );

  readonly tournamentTableVm$ = this.select(
    this.activeTournament$,
    this.myTable$,
    this.myPlayer$,
    this.currentBlinds$,
    this.nextBlinds$,
    this.remainingPlayers$,
    this.averageStack$,
    this.myRank$,
    this.isOnBreak$,
    this.isFinalTable$,
    this.isEliminated$,
    this.isLoading$,
    this.error$,
    (
      tournament, table, myPlayer, currentBlinds, nextBlinds,
      remainingPlayers, averageStack, myRank, isOnBreak, isFinalTable,
      isEliminated, isLoading, error
    ): TournamentTableViewModel => {
      const timeToNextLevel = tournament
        ? calculateTimeRemaining(tournament.levelEndTime)
        : 0;

      return {
        tournament,
        table,
        myPlayer,
        currentBlinds,
        nextBlinds,
        timeToNextLevel,
        formattedTimeRemaining: formatTimeRemaining(timeToNextLevel),
        remainingPlayers,
        averageStack,
        myRank,
        totalPlayers: tournament?.totalPlayers ?? 0,
        isOnBreak,
        isFinalTable,
        isEliminated,
        isLoading,
        error
      };
    }
  );






  readonly timeRemaining$ = this.activeTournament$.pipe(
    switchMap(tournament => {
      if (!tournament) return of(0);

      return timer(0, 1000).pipe(
        map(() => calculateTimeRemaining(tournament.levelEndTime)),
        takeUntil(this.destroy$)
      );
    }),
    distinctUntilChanged(),
    shareReplay(1)
  );

  readonly formattedTimeRemaining$ = this.timeRemaining$.pipe(
    map(time => formatTimeRemaining(time))
  );





  readonly setLoading = this.updater((state, isLoading: boolean) => ({
    ...state,
    isLoading
  }));

  readonly setRegistering = this.updater((state, isRegistering: boolean) => ({
    ...state,
    isRegistering
  }));

  readonly setError = this.updater((state, error: string | null) => ({
    ...state,
    error,
    isLoading: false,
    isRegistering: false
  }));

  readonly clearError = this.updater(state => ({
    ...state,
    error: null
  }));

  readonly setTournaments = this.updater((state, tournaments: TournamentListItem[]) => ({
    ...state,
    tournaments,
    isLoading: false
  }));

  readonly setActiveTournament = this.updater((state, tournament: Tournament | null) => ({
    ...state,
    activeTournament: tournament,
    isLoading: false
  }));

  readonly setMyTable = this.updater((state, table: TournamentTable | null) => ({
    ...state,
    myTable: table
  }));

  readonly setMyPlayer = this.updater((state, player: TournamentPlayer | null) => ({
    ...state,
    myPlayer: player
  }));

  readonly setConnectionStatus = this.updater((state, status: 'connected' | 'disconnected' | 'reconnecting') => ({
    ...state,
    connectionStatus: status
  }));

  readonly setLastUpdate = this.updater((state, update: TournamentUpdate) => ({
    ...state,
    lastUpdate: update
  }));

  readonly updateTournamentState = this.updater((state, partialTournament: Partial<Tournament>) => ({
    ...state,
    activeTournament: state.activeTournament
      ? { ...state.activeTournament, ...partialTournament }
      : null
  }));

  readonly reset = this.updater(() => initialState);






  readonly loadTournaments = this.effect<void>(trigger$ =>
    trigger$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(() =>
        this.http.get<TournamentListItem[]>(`${this.apiUrl}/list`).pipe(
          tapResponse(
            tournaments => this.setTournaments(tournaments),
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );


  readonly loadTournament = this.effect<string>(tournamentId$ =>
    tournamentId$.pipe(
      tap(() => this.setLoading(true)),
      switchMap(tournamentId =>
        this.http.get<Tournament>(`${this.apiUrl}/${tournamentId}`).pipe(
          tapResponse(
            tournament => {
              this.setActiveTournament(tournament);

              const myPlayer = tournament.registeredPlayers.find(p => !p.isBot);
              if (myPlayer) {
                this.setMyPlayer(myPlayer);
                const myTable = tournament.tables.find(t =>
                  t.players.some(p => p.id === myPlayer.id)
                );
                this.setMyTable(myTable ?? null);
              }
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        )
      )
    )
  );


  readonly registerForTournament = this.effect<{ tournamentId: string; playerName: string }>(
    params$ => params$.pipe(
      tap(() => this.setRegistering(true)),
      switchMap(({ tournamentId, playerName }) =>
        this.http.post<TournamentPlayer>(
          `${this.apiUrl}/${tournamentId}/register`,
          { playerName }
        ).pipe(
          tapResponse(
            player => {
              this.setMyPlayer(player);
              this.setRegistering(false);

              this.loadTournament(tournamentId);
            },
            (error: HttpErrorResponse) => {
              this.handleError(error);
              this.setRegistering(false);
            }
          )
        )
      )
    )
  );


  readonly unregisterFromTournament = this.effect<string>(tournamentId$ =>
    tournamentId$.pipe(
      tap(() => this.setLoading(true)),
      withLatestFrom(this.myPlayer$),
      switchMap(([tournamentId, myPlayer]) => {
        if (!myPlayer) {
          this.setLoading(false);
          return EMPTY;
        }

        return this.http.post(
          `${this.apiUrl}/${tournamentId}/unregister`,
          { playerId: myPlayer.id },
          { responseType: 'text' }
        ).pipe(
          tapResponse(
            () => {
              this.setMyPlayer(null);
              this.setMyTable(null);
              this.loadTournament(tournamentId);
            },
            (error: HttpErrorResponse) => this.handleError(error)
          )
        );
      })
    )
  );


  readonly subscribeTournamentUpdates = this.effect<string>(tournamentId$ =>
    tournamentId$.pipe(
      tap(tournamentId => {
        this.setConnectionStatus('reconnecting');




        this.startPolling(tournamentId);
      })
    )
  );


  private startPolling(tournamentId: string): void {
    timer(0, 5000).pipe(
      takeUntil(this.destroy$),
      switchMap(() =>
        this.http.get<Tournament>(`${this.apiUrl}/${tournamentId}`).pipe(
          catchError(() => EMPTY)
        )
      )
    ).subscribe(tournament => {
      this.setActiveTournament(tournament);
      this.setConnectionStatus('connected');


      const myPlayer = this.get().myPlayer;
      if (myPlayer) {
        const updatedPlayer = tournament.registeredPlayers.find(p => p.id === myPlayer.id);
        if (updatedPlayer) {
          this.setMyPlayer(updatedPlayer);
        }

        const myTable = tournament.tables.find(t =>
          t.players.some(p => p.id === myPlayer.id)
        );
        this.setMyTable(myTable ?? null);
      }
    });
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
      console.log(`[TournamentStore] ${message}`, data);
    }
  }


  getMyPlayer(): TournamentPlayer | null {
    return this.get().myPlayer;
  }


  getCurrentTournamentId(): string | null {
    return this.get().activeTournament?.id ?? null;
  }


  isPlayerRegistered(): boolean {
    return this.get().myPlayer !== null;
  }
}
