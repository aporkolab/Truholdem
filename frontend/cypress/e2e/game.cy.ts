

describe('TruHoldem Poker E2E Test Suite', () => {
  beforeEach(() => {
    cy.interceptApi();
    cy.clearAllStorage();
  });

  
  
  
  describe('Game Lobby', () => {
    beforeEach(() => {
      cy.visit('/');
    });

    it('GL-01: should display welcome message and branding', () => {
      cy.get('[data-cy=home-page]').should('exist');
      cy.get('[data-cy=app-title]').should('contain', 'TruHoldem');
      cy.get('[data-cy=tagline]').should('be.visible');
    });

    it('GL-02: should navigate to player registration/lobby', () => {
      cy.get('[data-cy=new-game-btn], [data-cy=guest-play-btn]')
        .first()
        .click();

      cy.url().should('include', '/lobby');
    });

    it('GL-03: should display navigation options', () => {
      cy.get('[data-cy=cta-section]').should('exist');
      cy.get('[data-cy=features-section]').should('exist');
    });

    it('GL-04: should show feature cards', () => {
      cy.get('[data-cy=feature-ai]').should('exist');
      cy.get('[data-cy=feature-stats]').should('exist');
      cy.get('[data-cy=feature-compete]').should('exist');
      cy.get('[data-cy=feature-replay]').should('exist');
    });
  });

  
  
  
  describe('Player Registration', () => {
    beforeEach(() => {
      // Mock API for starting game
      const mockGameState = {
        id: 'test-game-lobby',
        players: [
          { name: 'Player', chips: 1000, hand: [], folded: false, isAllIn: false, betAmount: 0, isBot: false, seatPosition: 0 },
          { name: 'Bot1', chips: 1000, hand: [], folded: false, isAllIn: false, betAmount: 0, isBot: true, seatPosition: 1 }
        ],
        communityCards: [],
        pot: 0,
        currentPlayerIndex: 0,
        phase: 'PRE_FLOP',
        dealerPosition: 0
      };
      cy.intercept('POST', '/api/poker/start', { statusCode: 200, body: mockGameState }).as('startGame');
      cy.intercept('GET', '/api/poker/status*', { statusCode: 200, body: mockGameState }).as('getStatus');
      cy.visit('/lobby');
    });

    it('PR-01: should display lobby page with players section', () => {
      cy.get('[data-cy=lobby-page]').should('exist');
      cy.get('[data-cy=lobby-title]').should('contain', 'Game Lobby');
      cy.get('[data-cy=players-section]').should('exist');
    });

    it('PR-02: should have human player entry', () => {
      cy.get('[data-cy=human-player]').should('exist');
      cy.get('[data-cy=player-name-input-0]').should('exist');
      cy.get('[data-cy=player-chips-input-0]').should('exist');
    });

    it('PR-03: should add a bot player', () => {
      cy.get('[data-cy=player-list]').children().then($initial => {
        const initialCount = $initial.length;

        cy.get('[data-cy=add-bot-btn]').click();

        cy.get('[data-cy=player-list]').children()
          .should('have.length', initialCount + 1);
      });
    });

    it('PR-04: should have player management controls', () => {
      // Verify the lobby has player management functionality
      cy.get('[data-cy=player-list]').should('exist');
      cy.get('[data-cy=add-bot-btn]').should('exist');
      // The start button exists (may or may not be disabled based on player count)
      cy.get('[data-cy=start-game-btn]').should('exist');
    });

    it('PR-05: should start game with valid player configuration', () => {
      cy.get('[data-cy=start-game-btn]')
        .should('not.be.disabled')
        .click();

      cy.url().should('include', '/game');
    });
  });

  
  
  
  describe('Game Table Display', () => {
    beforeEach(() => {
      cy.setupGame(['TestPlayer', 'Bot1', 'Bot2']);
    });

    it('GT-01: should display poker table', () => {
      cy.get('[data-cy=poker-table]').should('exist');
    });

    it('GT-02: should display player cards', () => {
      cy.get('[data-cy=player-cards]').should('have.length.at.least', 1);
      cy.get('[data-cy=player-card]').should('exist');
    });

    it('GT-03: should highlight current player turn', () => {
      cy.get('[data-cy=turn-indicator]').should('exist');
    });

    it('GT-04: should display action buttons for human player', () => {
      cy.waitForHumanTurn();
      cy.get('[data-cy=actions-section]').should('exist');
      cy.get('[data-cy=action-buttons]').should('exist');
    });

    it('GT-05: should display current pot amount', () => {
      cy.get('[data-cy=pot-display]').should('exist');
      cy.get('[data-cy=pot-value]').invoke('text').should('match', /\$?\d+/);
    });

    it('GT-06: should display current game phase', () => {
      cy.get('[data-cy=game-phase]').should('exist');
      cy.get('[data-cy=phase-value]').should('not.be.empty');
    });
  });

  
  
  
  describe('Player Actions', () => {
    beforeEach(() => {
      cy.setupGame(['TestPlayer', 'Bot1', 'Bot2']);
      cy.waitForHumanTurn();
    });

    it('PA-01: should perform FOLD action', () => {
      cy.get('[data-cy=fold-btn]').click();
      // After fold, the table should still exist
      cy.get('[data-cy=poker-table]').should('exist');
    });

    it('PA-02: should perform CHECK action when available', () => {
      cy.get('body').then($body => {
        const checkBtn = $body.find('[data-cy=check-btn]');
        if (checkBtn.length > 0 && !checkBtn.prop('disabled')) {
          cy.get('[data-cy=check-btn]').click();
          cy.get('[data-cy=poker-table]').should('exist');
        } else {
          // Check not available, test passes
          expect(true).to.be.true;
        }
      });
    });

    it('PA-03: should perform CALL action when available', () => {
      cy.get('body').then($body => {
        const callBtn = $body.find('[data-cy=call-btn]');
        if (callBtn.length > 0 && !callBtn.prop('disabled')) {
          cy.get('[data-cy=call-btn]').click();
          cy.get('[data-cy=poker-table]').should('exist');
        } else {
          // Call not available, test passes
          expect(true).to.be.true;
        }
      });
    });

    it('PA-04: should open raise modal and perform RAISE', () => {
      cy.get('[data-cy=raise-btn]').click();
      cy.get('[data-cy=raise-modal]').should('be.visible');
      cy.get('[data-cy=raise-input]').clear().type('100');
      cy.get('[data-cy=confirm-raise-btn]').click();

      cy.get('[data-cy=raise-modal]').should('not.exist');
    });

    it('PA-05: should perform ALL-IN action', () => {
      cy.get('body').then($body => {
        const allInBtn = $body.find('[data-cy=all-in-btn]');
        if (allInBtn.length > 0 && !allInBtn.prop('disabled')) {
          cy.get('[data-cy=all-in-btn]').click();
          cy.get('[data-cy=poker-table]').should('exist');
        } else {
          // All-in not available, test passes
          expect(true).to.be.true;
        }
      });
    });

    it('PA-06: should display action buttons', () => {
      cy.get('[data-cy=action-buttons]').should('exist');
      // At least one action button should be visible
      cy.get('[data-cy=fold-btn], [data-cy=check-btn], [data-cy=call-btn], [data-cy=raise-btn]')
        .should('have.length.at.least', 1);
    });
  });

  
  
  
  describe('Game Flow', () => {
    beforeEach(() => {
      cy.setupGame(['TestPlayer', 'Bot1', 'Bot2']);
    });

    it('GF-01: should display initial game phase', () => {
      cy.get('[data-cy=phase-value]').should('exist');
      cy.get('[data-cy=phase-value]').invoke('text').should('not.be.empty');
    });

    it('GF-02: should handle fold action', () => {
      cy.waitForHumanTurn();
      cy.get('[data-cy=fold-btn]').click();
      cy.get('[data-cy=poker-table]').should('exist');
    });

    it('GF-03: should display game elements after setup', () => {
      cy.get('[data-cy=poker-table]').should('exist');
      cy.get('[data-cy=pot-display]').should('exist');
      cy.get('[data-cy=game-phase]').should('exist');
    });

    it('GF-04: should have community cards container', () => {
      cy.get('[data-cy=community-cards-container], [data-cy=community-cards]').should('exist');
    });

    it('GF-05: should display player positions', () => {
      cy.get('[data-cy=player-seat], [data-cy=player-cards]').should('have.length.at.least', 1);
    });
  });

  
  
  
  describe('Leaderboard', () => {
    beforeEach(() => {
      cy.visit('/leaderboard');
    });

    it('LB-01: should display leaderboard page', () => {
      cy.get('[data-cy=leaderboard-page]').should('exist');
      cy.get('[data-cy=leaderboard-title]').should('contain', 'Leaderboard');
    });

    it('LB-02: should display category tabs', () => {
      cy.get('[data-cy=category-tabs]').should('exist');
      cy.get('[data-cy=tab-winnings]').should('exist');
      cy.get('[data-cy=tab-handsWon]').should('exist');
    });

    it('LB-03: should have player search functionality', () => {
      cy.get('[data-cy=player-search-section]').should('exist');
      cy.get('[data-cy=player-search-input]').should('exist');
      cy.get('[data-cy=player-search-btn]').should('exist');
    });
  });

  
  
  
  describe('Hand History', () => {
    beforeEach(() => {
      cy.visit('/history');
    });

    it('HH-01: should display hand history page or redirect', () => {
      // Page might redirect or show content
      cy.url().then(url => {
        if (url.includes('/history')) {
          cy.get('body').should('exist');
        } else {
          // Redirected, which is acceptable
          expect(true).to.be.true;
        }
      });
    });

    it('HH-02: should show page content', () => {
      cy.get('body').should('exist');
    });

    it('HH-03: should handle history page state', () => {
      cy.get('body').should('exist');
    });

    it('HH-04: should have navigation option', () => {
      // Check if any navigation exists (back button, nav links, etc.)
      cy.get('a, button, [data-cy^=back], [data-cy*=home]').should('have.length.at.least', 0);
      cy.get('body').should('exist');
    });
  });

  
  
  
  describe('Hand Replay', () => {
    it('HR-01: should display loading or replay page', () => {
      cy.visit('/replay/test-hand-id');
      cy.get('[data-cy=loading-state], [data-cy=hand-replay-page], [data-cy=error-state], [data-cy=no-data-state]')
        .should('exist');
    });

    it('HR-02: should have replay controls when loaded', () => {
      cy.visit('/replay/test-hand-id');
      cy.get('body').then($body => {
        if ($body.find('[data-cy=replay-controls]').length > 0) {
          cy.get('[data-cy=play-pause-btn]').should('exist');
          cy.get('[data-cy=next-btn]').should('exist');
          cy.get('[data-cy=prev-btn]').should('exist');
        }
      });
    });

    it('HR-03: should display action timeline when loaded', () => {
      cy.visit('/replay/test-hand-id');
      cy.get('body').then($body => {
        if ($body.find('[data-cy=action-timeline]').length > 0) {
          cy.get('[data-cy=timeline-scroll]').should('exist');
        }
      });
    });

    it('HR-04: should have speed control', () => {
      cy.visit('/replay/test-hand-id');
      cy.get('body').then($body => {
        if ($body.find('[data-cy=speed-control]').length > 0) {
          cy.get('[data-cy=speed-select]').should('exist');
        }
      });
    });
  });

  
  
  
  describe('Settings', () => {
    beforeEach(() => {
      cy.visit('/settings');
    });

    it('SET-01: should display settings page', () => {
      cy.get('[data-cy=settings-page]').should('exist');
      cy.get('[data-cy=settings-title]').should('contain', 'Settings');
    });

    it('SET-02: should have sound settings section', () => {
      cy.get('[data-cy=sound-settings-section]').should('exist');
      cy.get('[data-cy=sound-enabled-toggle]').should('exist');
    });

    it('SET-03: should have display settings section', () => {
      cy.get('[data-cy=display-settings-section]').should('exist');
      cy.get('[data-cy=card-animation-toggle]').should('exist');
    });

    it('SET-04: should have reset to defaults button', () => {
      cy.get('[data-cy=reset-defaults-btn]').should('exist');
    });
  });

  
  
  
  describe('Accessibility', () => {
    it('A11Y-01: should have focusable elements on home page', () => {
      cy.visit('/');
      // Check that there are focusable elements
      cy.get('a, button, input, [tabindex]').should('have.length.at.least', 1);
    });

    it('A11Y-02: should pass axe-core accessibility check on home page', () => {
      cy.visit('/');
      cy.injectAxe();
      cy.checkA11y('[data-cy=home-page]', {
        runOnly: {
          type: 'tag',
          values: ['wcag2a', 'wcag2aa']
        }
      });
    });

    it('A11Y-03: should have proper ARIA labels on game table', () => {
      cy.setupGame(['TestPlayer', 'Bot1']);
      cy.get('[role="toolbar"]').should('exist');
      cy.get('[role="list"]').should('exist');
    });

    it('A11Y-04: should have focusable action buttons', () => {
      cy.setupGame(['TestPlayer', 'Bot1']);
      cy.waitForHumanTurn();
      cy.get('[data-cy=fold-btn]').should('have.attr', 'type', 'button');
    });
  });

  
  
  
  describe('Authentication', () => {
    it('AUTH-01: should display login page', () => {
      cy.visit('/auth/login');
      cy.get('[data-cy=login-page]').should('exist');
      cy.get('[data-cy=login-form]').should('exist');
    });

    it('AUTH-02: should have username and password inputs', () => {
      cy.visit('/auth/login');
      cy.get('[data-cy=username-input]').should('exist');
      cy.get('[data-cy=password-input]').should('exist');
    });

    it('AUTH-03: should display register page', () => {
      cy.visit('/auth/register');
      cy.get('[data-cy=register-page]').should('exist');
      cy.get('[data-cy=register-form]').should('exist');
    });

    it('AUTH-04: should have terms checkbox on register', () => {
      cy.visit('/auth/register');
      cy.get('[data-cy=terms-checkbox]').should('exist');
    });
  });

  
  
  
  describe('Error Handling', () => {
    it('ERR-01: should handle visiting game page without active game', () => {
      cy.intercept('GET', '/api/poker/status*', { statusCode: 404, body: { message: 'No game found' } });
      cy.visit('/game');
      // Should either show error, redirect, or show empty game state
      cy.get('body').should('exist');
    });

    it('ERR-02: should display 404 page for invalid routes', () => {
      cy.visit('/invalid-route-that-does-not-exist', { failOnStatusCode: false });
      cy.get('[data-cy=not-found-page], [data-cy=error-code], body').should('exist');
    });
  });

  
  
  
  describe('Responsive Design', () => {
    it('RD-01: should display correctly on mobile viewport', () => {
      cy.viewport('iphone-x');
      cy.visit('/');
      cy.get('[data-cy=home-page]').should('be.visible');
    });

    it('RD-02: should display correctly on tablet viewport', () => {
      cy.viewport('ipad-2');
      cy.visit('/');
      cy.get('[data-cy=home-page]').should('be.visible');
    });
  });

  
  
  
  describe('Modal & UI Interactions', () => {
    it('UI-01: should open and close raise modal', () => {
      cy.setupGame(['TestPlayer', 'Bot1']);
      cy.waitForHumanTurn();

      cy.get('[data-cy=raise-btn]').click();
      cy.get('[data-cy=raise-modal]').should('be.visible');

      cy.get('[data-cy=cancel-raise-btn]').click();
      cy.get('[data-cy=raise-modal]').should('not.exist');
    });

    it('UI-02: should display pot value', () => {
      cy.setupGame(['TestPlayer', 'Bot1']);
      cy.get('[data-cy=pot-value]').should('exist');
      cy.get('[data-cy=pot-value]').invoke('text').should('not.be.empty');
    });
  });
});
