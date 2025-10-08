import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PlayerInfo } from './register-players/register-players.component';
import { PlayerService } from './services/player.service';
import { Router } from '@angular/router';
import { SoundService } from './services/sound.service';
import { NotificationComponent } from './shared/notification/notification.component';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: true,
    imports: [CommonModule, RouterModule, NotificationComponent]
})
export class AppComponent implements OnInit, OnDestroy {
  private playerService = inject(PlayerService);
  private router = inject(Router);
  private soundService = inject(SoundService);
  private destroy$ = new Subject<void>();

  title = 'TruHoldem';
  registeredPlayers: PlayerInfo[] = [];
  soundEnabled = true;

  ngOnInit(): void {
    
    this.playerService.players$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(players => {
      this.registeredPlayers = players;
    });

    
    this.soundService.settings$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(settings => {
      this.soundEnabled = settings.enabled;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleSound(): void {
    this.soundService.toggleSound();
    this.soundService.playClick();
  }

  onPlayersRegistered(playersInfo: PlayerInfo[]): void {
    if (this.isValidPlayersArray(playersInfo)) {
      console.log('Registered players:', playersInfo);
      this.playerService.setPlayers(playersInfo);
      this.router.navigate(['/start']);
    } else {
      console.error('Unexpected players data:', playersInfo);
    }
  }

  private isValidPlayersArray(playersInfo: PlayerInfo[]): boolean {
    return Array.isArray(playersInfo) && playersInfo.length > 0;
  }
}
