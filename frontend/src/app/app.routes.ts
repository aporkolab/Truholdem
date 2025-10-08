import { Routes } from '@angular/router';
import { gameGuard, replayGuard } from './guards/game.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./home/home.component').then((m) => m.HomeComponent),
    title: "TruHoldem - Texas Hold'em Poker",
  },

  {
    path: 'game',
    loadComponent: () =>
      import('./game-table/game-table.component').then(
        (m) => m.GameTableComponent
      ),
    canActivate: [gameGuard],
    title: 'Game Table - TruHoldem',
  },

  {
    path: 'lobby',
    loadComponent: () =>
      import('./lobby/lobby.component').then((m) => m.LobbyComponent),
    title: 'Game Lobby - TruHoldem',
  },

  {
    path: 'leaderboard',
    loadComponent: () =>
      import('./leaderboard/leaderboard.component').then(
        (m) => m.LeaderboardComponent
      ),
    title: 'Leaderboard - TruHoldem',
  },

  {
    path: 'tournaments',
    loadChildren: () =>
      import('./tournament/tournament.routes').then((m) => m.TOURNAMENT_ROUTES),
    title: 'Tournaments - TruHoldem',
  },
  {
    path: 'tournament',
    redirectTo: 'tournaments',
    pathMatch: 'full',
  },

  {
    path: 'settings',
    loadComponent: () =>
      import('./settings/settings.component').then((m) => m.SettingsComponent),
    title: 'Settings - TruHoldem',
  },

  {
    path: 'history',
    loadChildren: () =>
      import('./history/history.routes').then((m) => m.HISTORY_ROUTES),
    title: 'Hand History - TruHoldem',
  },

  {
    path: 'replay/:handId',
    loadComponent: () =>
      import('./hand-replay/hand-replay.component').then(
        (m) => m.HandReplayComponent
      ),
    canActivate: [replayGuard],
    title: 'Hand Replay - TruHoldem',
  },

  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },

  {
    path: 'start',
    redirectTo: 'game',
    pathMatch: 'full',
  },
  {
    path: 'login',
    redirectTo: 'auth/login',
    pathMatch: 'full',
  },
  {
    path: 'register',
    redirectTo: 'auth/register',
    pathMatch: 'full',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./not-found/not-found.component').then(
        (m) => m.NotFoundComponent
      ),
    title: '404 - Page Not Found',
  },
  {
    path: 'analysis',
    loadChildren: () =>
      import('./analysis/analysis.routes').then((m) => m.ANALYSIS_ROUTES),
  },
];
