import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { PlayerInfo } from '../register-players/register-players.component';

@Injectable({
  providedIn: 'root'
})
export class PlayerService {
  private playersSubject = new BehaviorSubject<PlayerInfo[]>([]);
  players$ = this.playersSubject.asObservable();

  setPlayers(players: PlayerInfo[]): void {
    this.playersSubject.next(players);
  }

  getPlayers(): PlayerInfo[] {
    return this.playersSubject.value;
  }
}