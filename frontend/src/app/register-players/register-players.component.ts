import { NgFor } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PlayerService } from '../services/player.service';
import { environment } from '../../environments/environment';

export interface PlayerInfo {
  name: string;
  startingChips: number;
  isBot: boolean;
}

@Component({
  selector: 'app-register-players',
  standalone: true,
  templateUrl: './register-players.component.html',
  styleUrls: ['./register-players.component.scss'],
  imports: [FormsModule, NgFor],
})
export class RegisterPlayersComponent {
  private http = inject(HttpClient);
  private playerService = inject(PlayerService);
  private router = inject(Router);

  maxBotPlayers = 3;
  maxHumanPlayers = 1;
  players: PlayerInfo[] = [
    { name: this.generateRandomName(), startingChips: 1000, isBot: false }, 
    { name: this.generateRandomName(), startingChips: 1000, isBot: true }, 
    { name: this.generateRandomName(), startingChips: 1000, isBot: true },
    { name: this.generateRandomName(), startingChips: 1000, isBot: true },
  ];


  
  addPlayer(): void {
    if (this.players.length >= 4) {
      alert("You can't have more than 4 players.");
      return;
    }
    
    this.players.push({
      name: this.generateRandomName(),
      startingChips: 1000,
      isBot: true,
    });
  }

  
  removePlayer(index: number): void {
    this.players.splice(index, 1);
  }

  
  private finalizeNames(): void {
    this.players.forEach((player) => {
      if (!player.name.trim()) {
        player.name = this.generateRandomName();
      }
      if (player.isBot && !player.name.startsWith('Bot')) {
        player.name = 'Bot ' + player.name;
      }
    });
  }

  onSubmit(): void {
    
    this.finalizeNames();

    
    this.http.post(`${environment.apiUrl}/poker/reset`, {}).subscribe({
      next: () => {
        
        this.registerPlayers();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error resetting game:', error.message);
        alert(
          'An error occurred while resetting the game. Please try again later.'
        );
      },
    });
  }

  registerPlayers(): void {
    this.http
      .post<{ players: { id: string; name: string }[] }>(
        `${environment.apiUrl}/poker/start`,
        this.players
      )
      .subscribe({
        next: (response: { players: { id: string; name: string }[] }) => {
          if (response && response.players && Array.isArray(response.players)) {
            this.changePlayerNames(response.players);
          } else {
            console.error(
              'Unexpected response format. Expected players array but got:',
              response
            );
            alert('Unexpected response from server.');
          }
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error registering players:', error.message);
          alert(
            'An error occurred during registration. Please try again later.'
          );
        },
      });
  }

  changePlayerNames(serverPlayers: { id: string; name: string }[]): void {
    const changePlayerNameRecursively = (index: number) => {
      if (index >= serverPlayers.length) {
        
        this.playerService.setPlayers(this.players);
        this.router.navigate(['/start']);
        return;
      }

      const serverPlayer = serverPlayers[index];
      const clientPlayer = this.players[index];

      
      if (serverPlayer.name !== clientPlayer.name) {
        this.http
          .post(`${environment.apiUrl}/poker/change-name`, {
            playerId: serverPlayer.id,
            newName: clientPlayer.name,
          })
          .subscribe({
            next: () => {
              
              changePlayerNameRecursively(index + 1);
            },
            error: (error: HttpErrorResponse) => {
              console.error(
                'Error changing player name for',
                clientPlayer.name,
                ':',
                error.message
              );
              alert(
                'An error occurred while changing player names. Please try again later.'
              );
            },
          });
      } else {
        
        changePlayerNameRecursively(index + 1);
      }
    };

    
    changePlayerNameRecursively(0);
  }

  
  private generateRandomName(): string {
    const commonNames = [
      'James',
      'Mary',
      'John',
      'Patricia',
      'Robert',
      'Jennifer',
      'Michael',
      'Linda',
      'William',
      'Elizabeth',
      'David',
      'Barbara',
      'Richard',
      'Susan',
      'Joseph',
      'Jessica',
      'Thomas',
      'Sarah',
      'Charles',
      'Karen',
      'Christopher',
      'Nancy',
      'Daniel',
      'Margaret',
      'Matthew',
      'Lisa',
      'Anthony',
      'Betty',
      'Donald',
      'Dorothy',
      'Mark',
      'Sandra',
      'Paul',
      'Ashley',
      'Steven',
      'Kimberly',
      'Andrew',
      'Donna',
      'Kenneth',
      'Emily',
      'Joshua',
      'Michelle',
      'George',
      'Carol',
      'Kevin',
      'Amanda',
      'Brian',
      'Melissa',
      'Edward',
      'Deborah',
      'Ronald',
      'Stephanie',
      'Timothy',
      'Rebecca',
      'Jason',
      'Laura',
      'Jeffrey',
      'Sharon',
      'Ryan',
      'Cynthia',
      'Jacob',
      'Kathleen',
      'Gary',
      'Amy',
      'Nicholas',
      'Shirley',
      'Eric',
      'Angela',
      'Stephen',
      'Helen',
      'Jonathan',
      'Anna',
      'Larry',
      'Brenda',
      'Justin',
      'Pamela',
      'Scott',
      'Nicole',
      'Brandon',
      'Emma',
      'Frank',
      'Samantha',
      'Benjamin',
      'Katherine',
      'Gregory',
      'Christine',
      'Raymond',
      'Debra',
      'Samuel',
      'Rachel',
      'Patrick',
      'Catherine',
      'Alexander',
      'Carolyn',
      'Jack',
      'Janet',
      'Dennis',
      'Ruth',
      'Jerry',
      'Maria',
    ];
    return commonNames[Math.floor(Math.random() * commonNames.length)];
  }
}
