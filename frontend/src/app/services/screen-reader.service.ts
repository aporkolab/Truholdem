import { Injectable, signal, computed } from '@angular/core';


@Injectable({
  providedIn: 'root'
})
export class ScreenReaderService {
  
  
  private readonly _politeAnnouncement = signal<string>('');
  private readonly _assertiveAnnouncement = signal<string>('');
  
  
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly DEBOUNCE_MS = 100;
  private readonly CLEAR_DELAY_MS = 5000;
  
  
  readonly politeAnnouncement = this._politeAnnouncement.asReadonly();
  readonly assertiveAnnouncement = this._assertiveAnnouncement.asReadonly();
  
  
  readonly currentAnnouncement = computed(() => 
    this._assertiveAnnouncement() || this._politeAnnouncement()
  );

  
  announcePolite(message: string, clearDelay = this.CLEAR_DELAY_MS): void {
    this.queueAnnouncement(message, 'polite', clearDelay);
  }

  
  announceAssertive(message: string, clearDelay = this.CLEAR_DELAY_MS): void {
    this.queueAnnouncement(message, 'assertive', clearDelay);
  }

  private queueAnnouncement(
    message: string, 
    type: 'polite' | 'assertive',
    clearDelay: number
  ): void {
    
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }

    this.debounceTimer = setTimeout(() => {
      
      if (type === 'polite') {
        this._politeAnnouncement.set('');
        setTimeout(() => this._politeAnnouncement.set(message), 50);
      } else {
        this._assertiveAnnouncement.set('');
        setTimeout(() => this._assertiveAnnouncement.set(message), 50);
      }

      
      setTimeout(() => {
        if (type === 'polite') {
          this._politeAnnouncement.set('');
        } else {
          this._assertiveAnnouncement.set('');
        }
      }, clearDelay);
    }, this.DEBOUNCE_MS);
  }

  
  
  

  
  announcePhase(phase: string): void {
    const phaseDescriptions: Record<string, string> = {
      'PRE_FLOP': 'Pre-flop betting round. No community cards revealed.',
      'FLOP': 'The flop has been dealt. 3 community cards revealed.',
      'TURN': 'The turn has been dealt. 4 community cards revealed.',
      'RIVER': 'The river has been dealt. All 5 community cards revealed.',
      'SHOWDOWN': 'Showdown! Players reveal their hands.'
    };
    
    const description = phaseDescriptions[phase] || `Phase: ${phase}`;
    this.announcePolite(description);
  }

  
  announceTurn(playerName: string, isHuman: boolean): void {
    if (isHuman) {
      this.announceAssertive(`Your turn, ${playerName}. Choose an action.`);
    } else {
      this.announcePolite(`Waiting for ${playerName} to act.`);
    }
  }

  
  announceAction(playerName: string, action: string, amount?: number): void {
    const actionMessages: Record<string, string> = {
      'FOLD': `${playerName} folds.`,
      'CHECK': `${playerName} checks.`,
      'CALL': amount ? `${playerName} calls ${amount} chips.` : `${playerName} calls.`,
      'BET': amount ? `${playerName} bets ${amount} chips.` : `${playerName} bets.`,
      'RAISE': amount ? `${playerName} raises to ${amount} chips.` : `${playerName} raises.`,
      'ALL_IN': amount ? `${playerName} goes all-in with ${amount} chips!` : `${playerName} goes all-in!`
    };

    const message = actionMessages[action] || `${playerName} ${action.toLowerCase()}.`;
    this.announcePolite(message);
  }

  
  announceCommunityCards(cards: { value: string; suit: string }[]): void {
    if (cards.length === 0) return;

    const cardDescriptions = cards.map(c => this.getCardDescription(c)).join(', ');
    
    let phaseLabel = '';
    if (cards.length === 3) phaseLabel = 'Flop';
    else if (cards.length === 4) phaseLabel = 'Turn';
    else if (cards.length === 5) phaseLabel = 'River';

    const message = phaseLabel 
      ? `${phaseLabel}: ${cardDescriptions}`
      : `Community cards: ${cardDescriptions}`;
    
    this.announcePolite(message);
  }

  
  announcePot(amount: number): void {
    this.announcePolite(`Pot is now ${amount} chips.`);
  }

  
  announceWinner(playerName: string, handDescription?: string, potAmount?: number): void {
    let message = `${playerName} wins`;
    if (potAmount) message += ` ${potAmount} chips`;
    if (handDescription) message += ` with ${handDescription}`;
    message += '!';
    
    this.announceAssertive(message);
  }

  
  announceAvailableActions(
    canCheck: boolean,
    canCall: boolean,
    callAmount: number,
    minRaise: number
  ): void {
    const actions: string[] = [];
    
    actions.push('Press F to fold');
    
    if (canCheck) {
      actions.push('Press C to check');
    } else if (canCall) {
      actions.push(`Press C to call ${callAmount} chips`);
    }
    
    actions.push(`Press R to raise (minimum ${minRaise})`);
    actions.push('Press A to go all-in');
    
    this.announcePolite(actions.join('. '));
  }

  
  announceHoleCards(cards: { value: string; suit: string }[]): void {
    if (cards.length !== 2) return;
    
    const card1 = this.getCardDescription(cards[0]);
    const card2 = this.getCardDescription(cards[1]);
    
    this.announcePolite(`Your cards: ${card1} and ${card2}`);
  }

  
  announceError(message: string): void {
    this.announceAssertive(`Error: ${message}`);
  }

  
  announceNewHand(handNumber: number): void {
    this.announcePolite(`Hand ${handNumber} starting.`);
  }

  
  announceNewGame(): void {
    this.announceAssertive('New game started. Good luck!');
  }

  
  
  

  
  getCardDescription(card: { value: string; suit: string }): string {
    const valueNames: Record<string, string> = {
      'TWO': '2',
      'THREE': '3',
      'FOUR': '4',
      'FIVE': '5',
      'SIX': '6',
      'SEVEN': '7',
      'EIGHT': '8',
      'NINE': '9',
      'TEN': '10',
      'JACK': 'Jack',
      'QUEEN': 'Queen',
      'KING': 'King',
      'ACE': 'Ace'
    };

    const suitNames: Record<string, string> = {
      'HEARTS': 'Hearts',
      'DIAMONDS': 'Diamonds',
      'CLUBS': 'Clubs',
      'SPADES': 'Spades'
    };

    const valueName = valueNames[card.value] || card.value;
    const suitName = suitNames[card.suit] || card.suit;

    return `${valueName} of ${suitName}`;
  }

  
  clearAnnouncements(): void {
    this._politeAnnouncement.set('');
    this._assertiveAnnouncement.set('');
  }
}
