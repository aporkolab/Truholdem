import { Routes } from '@angular/router';
import { authGuard } from '../guards/auth.guard';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./tournament-list/tournament-list.component')
      .then(m => m.TournamentListComponent),
    title: 'Tournaments - TruHoldem'
  },
  {
    path: ':id',
    loadComponent: () => import('./tournament-lobby/tournament-lobby.component')
      .then(m => m.TournamentLobbyComponent),
    title: 'Tournament Lobby - TruHoldem'
  },
  {
    path: ':id/play',
    loadComponent: () => import('./tournament-table/tournament-table.component')
      .then(m => m.TournamentTableComponent),
    canActivate: [authGuard],
    title: 'Tournament Table - TruHoldem'
  }
];
