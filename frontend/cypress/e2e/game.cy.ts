describe('TruHoldem Poker Game E2E Tests', () => {
  beforeEach(() => {
    cy.interceptApi();
  });

  describe('Game Setup', () => {
    it('should load the home page', () => {
      cy.visit('/');
      cy.contains('TruHoldem').should('be.visible');
    });

    it('should navigate to player registration', () => {
      cy.visit('/');
      cy.get('[data-cy=new-game-btn]').click();
      cy.url().should('include', '/register-players');
    });

    it('should add players to the game', () => {
      cy.visit('/register-players');
      
      
      cy.get('[data-cy=player-name-input]').type('TestPlayer');
      cy.get('[data-cy=player-chips-input]').clear().type('1000');
      cy.get('[data-cy=add-player-btn]').click();
      
      
      cy.get('[data-cy=player-list]').should('contain', 'TestPlayer');
    });

    it('should require minimum 2 players to start', () => {
      cy.visit('/register-players');
      
      cy.get('[data-cy=player-name-input]').type('OnlyPlayer');
      cy.get('[data-cy=add-player-btn]').click();
      
      cy.get('[data-cy=start-game-btn]').should('be.disabled');
    });

    it('should start game with valid players', () => {
      cy.visit('/register-players');
      
      
      cy.get('[data-cy=player-name-input]').type('Player1');
      cy.get('[data-cy=add-player-btn]').click();
      
      cy.get('[data-cy=player-name-input]').type('Bot1');
      cy.get('[data-cy=is-bot-checkbox]').check();
      cy.get('[data-cy=add-player-btn]').click();
      
      cy.get('[data-cy=start-game-btn]').click();
      cy.wait('@startGame');
      cy.url().should('include', '/game');
    });
  });

  describe('Game Table Display', () => {
    beforeEach(() => {
      
      cy.intercept('GET', '/api/poker/status', {
        statusCode: 200,
        body: {
          id: 'game-123',
          currentPot: 30,
          players: [
            { id: 'p1', name: 'Player1', chips: 990, betAmount: 10, folded: false, isBot: false },
            { id: 'p2', name: 'Bot1', chips: 980, betAmount: 20, folded: false, isBot: true }
          ],
          communityCards: [],
          phase: 'PRE_FLOP',
          currentBet: 20,
          currentPlayerIndex: 0
        }
      }).as('gameStatus');
      
      cy.visit('/game');
    });

    it('should display the poker table', () => {
      cy.get('[data-cy=poker-table]').should('be.visible');
    });

    it('should display current pot', () => {
      cy.get('[data-cy=pot-display]').should('contain', '30');
    });

    it('should display player seats', () => {
      cy.get('[data-cy=player-seat]').should('have.length', 2);
    });

    it('should highlight current player', () => {
      cy.get('[data-cy=player-seat].active').should('exist');
    });

    it('should display game phase', () => {
      cy.get('[data-cy=game-phase]').should('contain', 'Pre-Flop');
    });

    it('should show action buttons for current player', () => {
      cy.get('[data-cy=action-buttons]').should('be.visible');
      cy.get('[data-cy=fold-btn]').should('be.visible');
      cy.get('[data-cy=call-btn]').should('be.visible');
    });
  });

  describe('Player Actions', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/poker/status', {
        statusCode: 200,
        body: {
          id: 'game-123',
          currentPot: 30,
          players: [
            { id: 'p1', name: 'Player1', chips: 990, betAmount: 10, folded: false, isBot: false },
            { id: 'p2', name: 'Bot1', chips: 980, betAmount: 20, folded: false, isBot: true }
          ],
          communityCards: [],
          phase: 'PRE_FLOP',
          currentBet: 20,
          currentPlayerIndex: 0,
          minRaiseAmount: 20
        }
      });
      
      cy.visit('/game');
    });

    it('should perform fold action', () => {
      cy.intercept('POST', '/api/poker/fold*', {
        statusCode: 200,
        body: 'Fold successful'
      }).as('fold');

      cy.get('[data-cy=fold-btn]').click();
      cy.wait('@fold');
    });

    it('should perform call action', () => {
      cy.intercept('POST', '/api/poker/call*', {
        statusCode: 200,
        body: 'Call successful'
      }).as('call');

      cy.get('[data-cy=call-btn]').click();
      cy.wait('@call');
    });

    it('should show raise input when raise button clicked', () => {
      cy.get('[data-cy=raise-btn]').click();
      cy.get('[data-cy=raise-slider]').should('be.visible');
    });

    it('should perform raise action with specified amount', () => {
      cy.intercept('POST', '/api/poker/raise', {
        statusCode: 200,
        body: 'Raise successful'
      }).as('raise');

      cy.get('[data-cy=raise-btn]').click();
      cy.get('[data-cy=raise-input]').clear().type('100');
      cy.get('[data-cy=confirm-raise-btn]').click();
      cy.wait('@raise');
    });
  });

  describe('Game Flow', () => {
    it('should progress through betting rounds', () => {
      cy.intercept('GET', '/api/poker/status').as('status');
      
      
      cy.intercept('GET', '/api/poker/status', {
        body: { phase: 'PRE_FLOP', currentPot: 30 }
      });
      cy.visit('/game');
      cy.get('[data-cy=game-phase]').should('contain', 'Pre-Flop');

      
      cy.intercept('GET', '/api/poker/status', {
        body: { 
          phase: 'FLOP', 
          currentPot: 60,
          communityCards: [
            { suit: 'HEARTS', value: 'ACE' },
            { suit: 'CLUBS', value: 'KING' },
            { suit: 'DIAMONDS', value: 'TEN' }
          ]
        }
      });
      
      cy.reload();
      cy.get('[data-cy=game-phase]').should('contain', 'Flop');
      cy.get('[data-cy=community-card]').should('have.length', 3);
    });

    it('should display community cards on flop', () => {
      cy.intercept('GET', '/api/poker/status', {
        body: {
          phase: 'FLOP',
          communityCards: [
            { suit: 'HEARTS', value: 'ACE' },
            { suit: 'CLUBS', value: 'KING' },
            { suit: 'DIAMONDS', value: 'TEN' }
          ]
        }
      });
      
      cy.visit('/game');
      cy.get('[data-cy=community-card]').should('have.length', 3);
    });

    it('should show winner at showdown', () => {
      cy.intercept('GET', '/api/poker/status', {
        body: {
          phase: 'SHOWDOWN',
          winner: { name: 'Player1', handDescription: 'Pair of Aces' },
          currentPot: 200
        }
      });
      
      cy.visit('/game');
      cy.get('[data-cy=winner-display]').should('contain', 'Player1');
      cy.get('[data-cy=winning-hand]').should('contain', 'Pair of Aces');
    });
  });

  describe('Settings', () => {
    it('should navigate to settings', () => {
      cy.visit('/');
      cy.get('[data-cy=settings-btn]').click();
      cy.url().should('include', '/settings');
    });

    it('should toggle sound setting', () => {
      cy.visit('/settings');
      cy.get('[data-cy=sound-toggle]').click();
      cy.get('[data-cy=sound-toggle]').should('have.class', 'off');
    });

    it('should persist settings', () => {
      cy.visit('/settings');
      cy.get('[data-cy=sound-toggle]').click();
      cy.reload();
      cy.get('[data-cy=sound-toggle]').should('have.class', 'off');
    });
  });

  describe('Statistics', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/stats/player/*', {
        body: {
          playerName: 'TestPlayer',
          handsPlayed: 100,
          handsWon: 25,
          totalWinnings: 5000
        }
      }).as('playerStats');
    });

    it('should navigate to leaderboard', () => {
      cy.visit('/');
      cy.get('[data-cy=leaderboard-btn]').click();
      cy.url().should('include', '/leaderboard');
    });

    it('should display leaderboard data', () => {
      cy.intercept('GET', '/api/stats/leaderboard/*', {
        body: [
          { playerName: 'TopPlayer', totalWinnings: 10000 },
          { playerName: 'Player2', totalWinnings: 8000 }
        ]
      });
      
      cy.visit('/leaderboard');
      cy.get('[data-cy=leaderboard-entry]').should('have.length.at.least', 1);
    });

    it('should display player statistics', () => {
      cy.visit('/stats/TestPlayer');
      cy.wait('@playerStats');
      cy.get('[data-cy=hands-played]').should('contain', '100');
      cy.get('[data-cy=hands-won]').should('contain', '25');
    });
  });

  describe('Hand History', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/history/recent', {
        body: [
          {
            id: 'h1',
            handNumber: 1,
            winnerName: 'Player1',
            finalPot: 150
          }
        ]
      }).as('recentHands');
    });

    it('should display recent hands', () => {
      cy.visit('/history');
      cy.wait('@recentHands');
      cy.get('[data-cy=hand-entry]').should('have.length.at.least', 1);
    });

    it('should navigate to hand replay', () => {
      cy.intercept('GET', '/api/history/*/replay', {
        body: {
          id: 'h1',
          handNumber: 1,
          actions: [],
          winnerName: 'Player1'
        }
      });
      
      cy.visit('/history');
      cy.wait('@recentHands');
      cy.get('[data-cy=replay-btn]').first().click();
      cy.url().should('include', '/replay');
    });
  });

  describe('Hand Replay', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/history/*/replay', {
        body: {
          id: 'h1',
          handNumber: 1,
          smallBlind: 10,
          bigBlind: 20,
          players: [
            { name: 'Player1', startingChips: 1000 },
            { name: 'Bot1', startingChips: 1000 }
          ],
          actions: [
            { playerName: 'Player1', action: 'RAISE', amount: 60, phase: 'PRE_FLOP' },
            { playerName: 'Bot1', action: 'CALL', amount: 60, phase: 'PRE_FLOP' }
          ],
          board: ['ACE of HEARTS', 'KING of CLUBS', 'TEN of DIAMONDS'],
          winnerName: 'Player1',
          winningHand: 'Pair of Aces',
          finalPot: 150
        }
      }).as('replayData');
    });

    it('should load replay data', () => {
      cy.visit('/replay/h1');
      cy.wait('@replayData');
      cy.get('[data-cy=replay-controls]').should('be.visible');
    });

    it('should step through actions', () => {
      cy.visit('/replay/h1');
      cy.wait('@replayData');
      
      cy.get('[data-cy=next-action-btn]').click();
      cy.get('[data-cy=current-action]').should('contain', 'RAISE');
    });

    it('should show final result', () => {
      cy.visit('/replay/h1');
      cy.wait('@replayData');
      
      cy.get('[data-cy=skip-to-end-btn]').click();
      cy.get('[data-cy=winner-display]').should('contain', 'Player1');
    });
  });

  describe('Responsive Design', () => {
    it('should display correctly on mobile', () => {
      cy.viewport('iphone-x');
      cy.visit('/game');
      cy.get('[data-cy=poker-table]').should('be.visible');
      cy.get('[data-cy=action-buttons]').should('be.visible');
    });

    it('should display correctly on tablet', () => {
      cy.viewport('ipad-2');
      cy.visit('/game');
      cy.get('[data-cy=poker-table]').should('be.visible');
    });
  });

  describe('Error Handling', () => {
    it('should handle API errors gracefully', () => {
      cy.intercept('POST', '/api/poker/fold*', {
        statusCode: 500,
        body: { error: 'Server error' }
      });
      
      cy.visit('/game');
      cy.get('[data-cy=fold-btn]').click();
      cy.get('[data-cy=error-notification]').should('be.visible');
    });

    it('should handle network timeout', () => {
      cy.intercept('GET', '/api/poker/status', {
        forceNetworkError: true
      });
      
      cy.visit('/game');
      cy.get('[data-cy=connection-error]').should('be.visible');
    });
  });

  describe('Keyboard Shortcuts', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/poker/status', {
        body: {
          phase: 'PRE_FLOP',
          currentPlayerIndex: 0,
          players: [{ id: 'p1', name: 'Player1', isBot: false }]
        }
      });
      cy.visit('/game');
    });

    it('should fold on F key', () => {
      cy.intercept('POST', '/api/poker/fold*', { body: 'OK' }).as('fold');
      cy.get('body').type('f');
      cy.wait('@fold');
    });

    it('should call on C key when check not available', () => {
      cy.intercept('POST', '/api/poker/call*', { body: 'OK' }).as('call');
      cy.get('body').type('c');
      cy.wait('@call');
    });
  });
});
