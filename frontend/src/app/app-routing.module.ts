import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegisterPlayersComponent } from './register-players/register-players.component';
import { GameTableComponent } from './game-table/game-table.component';
import { LeaderboardComponent } from './leaderboard/leaderboard.component';
import { SettingsComponent } from './settings/settings.component';
import { HandReplayComponent } from './hand-replay/hand-replay.component';

const routes: Routes = [
  {
    path: '',
    component: RegisterPlayersComponent,
  },
  {
    path: 'start',
    component: GameTableComponent,
  },
  {
    path: 'leaderboard',
    component: LeaderboardComponent,
  },
  {
    path: 'settings',
    component: SettingsComponent,
  },
  {
    path: 'replay/:historyId',
    component: HandReplayComponent,
  },
  {
    path: '**',
    redirectTo: '',
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
