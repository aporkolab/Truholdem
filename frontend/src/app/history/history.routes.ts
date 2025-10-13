import { Routes } from '@angular/router';
import { authGuard } from '../guards/auth.guard';

export const HISTORY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./history-list.component').then(m => m.HistoryListComponent),
    canActivate: [authGuard]
  }
];
