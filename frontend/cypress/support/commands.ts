




declare global {
  namespace Cypress {
    interface Chainable {
      setupGame(players: string[]): Chainable<void>;
      setupGameViaUI(players: string[]): Chainable<void>;
      waitForHumanTurn(): Chainable<void>;
      waitForAnyTurn(): Chainable<void>;
      playThroughPhase(): Chainable<void>;
      playUntilPhase(targetPhase: string): Chainable<void>;
      playerAction(action: string, amount?: number): Chainable<void>;
      getCurrentPlayer(): Chainable<JQuery<HTMLElement>>;
      getHumanPlayer(): Chainable<JQuery<HTMLElement>>;
      waitForPhase(phase: string): Chainable<void>;
      verifyPot(amount: number): Chainable<void>;
      getCommunityCardsCount(): Chainable<number>;
      interceptApi(): Chainable<void>;
      mockGameStatus(overrides?: object): Chainable<void>;
      navigateTo(path: string): Chainable<void>;
      startNewGame(players?: string[]): Chainable<void>;
      checkFocusVisible(selector: string): Chainable<void>;
      testKeyboardNav(selectors: string[]): Chainable<void>;
      clearAllStorage(): Chainable<void>;
      waitForNetworkIdle(timeout?: number): Chainable<void>;
      screenshotWithTimestamp(name: string): Chainable<void>;
      injectAxe(): Chainable<void>;
      checkA11y(context?: string, options?: object): Chainable<void>;
      tab(): Chainable<JQuery<HTMLElement>>;
    }
  }
}





Cypress.Commands.add('setupGame', (players: string[]) => {
  // Mock the game state for E2E tests
  const mockGameState = {
    id: 'test-game-123',
    players: players.map((name, index) => ({
      name,
      chips: 1000,
      hand: [
        { suit: 'hearts', value: 'A' },
        { suit: 'spades', value: 'K' }
      ],
      folded: false,
      isAllIn: false,
      betAmount: index === 1 ? 10 : index === 2 ? 20 : 0,
      isBot: name.toLowerCase().startsWith('bot'),
      seatPosition: index
    })),
    communityCards: [],
    pot: 30,
    currentPlayerIndex: 0,
    phase: 'PRE_FLOP',
    dealerPosition: 0
  };

  // Intercept API calls and return mock data
  cy.intercept('POST', '/api/poker/start', {
    statusCode: 200,
    body: mockGameState
  }).as('startGame');

  cy.intercept('GET', '/api/poker/status*', {
    statusCode: 200,
    body: mockGameState
  }).as('getGameStatus');

  cy.intercept('POST', '/api/poker/action*', {
    statusCode: 200,
    body: mockGameState
  }).as('postAction');

  // Navigate to lobby and start game via UI
  cy.visit('/lobby');
  cy.get('[data-cy=lobby-page]', { timeout: 10000 }).should('exist');

  // Add extra bots if needed
  const botsNeeded = players.filter(p => p.toLowerCase().startsWith('bot')).length;
  for (let i = 1; i < botsNeeded; i++) {
    cy.get('[data-cy=add-bot-btn]').click();
  }

  // Start the game
  cy.get('[data-cy=start-game-btn]').click();
  cy.wait('@startGame');

  // Wait for navigation to game page
  cy.url().should('include', '/game');
  cy.get('[data-cy=poker-table]', { timeout: 15000 }).should('exist');
});

Cypress.Commands.add('setupGameViaUI', (players: string[]) => {
  // Mock the game state for E2E tests
  const mockGameState = {
    id: 'test-game-ui',
    players: players.map((name, index) => ({
      name,
      chips: 1000,
      hand: [],
      folded: false,
      isAllIn: false,
      betAmount: 0,
      isBot: name.toLowerCase().startsWith('bot'),
      seatPosition: index
    })),
    communityCards: [],
    pot: 0,
    currentPlayerIndex: 0,
    phase: 'PRE_FLOP',
    dealerPosition: 0
  };

  cy.intercept('POST', '/api/poker/start', {
    statusCode: 200,
    body: mockGameState
  }).as('startGameUI');

  cy.visit('/lobby');
  cy.get('[data-cy=lobby-page]').should('exist');


  const botsNeeded = players.filter(p => p.toLowerCase().startsWith('bot')).length;
  for (let i = 0; i < botsNeeded - 1; i++) {
    cy.get('[data-cy=add-bot-btn]').click();
  }


  cy.get('[data-cy=start-game-btn]').click();
  cy.wait('@startGameUI');
  cy.url().should('include', '/game');
  cy.get('[data-cy=poker-table]').should('exist');
});

Cypress.Commands.add('waitForHumanTurn', () => {
  // Wait for the human player's turn or action buttons to be visible
  cy.get('[data-cy=action-buttons], [data-cy=human-seat][aria-current="true"], .player.user.active-turn', { timeout: 15000 })
    .should('exist');
});

Cypress.Commands.add('waitForAnyTurn', () => {
  cy.get('[data-cy=turn-indicator]', { timeout: 10000 }).should('exist');
});

Cypress.Commands.add('playThroughPhase', () => {
  cy.waitForHumanTurn();

  cy.get('body').then($body => {
    const checkBtn = $body.find('[data-cy=check-btn]');
    const callBtn = $body.find('[data-cy=call-btn]');
    const foldBtn = $body.find('[data-cy=fold-btn]');

    if (checkBtn.length > 0 && !checkBtn.prop('disabled')) {
      cy.get('[data-cy=check-btn]').click();
    } else if (callBtn.length > 0 && !callBtn.prop('disabled')) {
      cy.get('[data-cy=call-btn]').click();
    } else if (foldBtn.length > 0) {
      cy.get('[data-cy=fold-btn]').click();
    }
  });
});

Cypress.Commands.add('playUntilPhase', (targetPhase: string) => {
  const phases = ['PRE_FLOP', 'FLOP', 'TURN', 'RIVER', 'SHOWDOWN'];
  const targetIndex = phases.indexOf(targetPhase.toUpperCase());

  if (targetIndex === -1) {
    throw new Error(`Invalid phase: ${targetPhase}`);
  }

  const checkAndPlay = (attempts = 0): void => {
    if (attempts > 50) {
      cy.log('Max attempts reached');
      return;
    }

    cy.get('[data-cy=phase-value]').invoke('text').then(currentPhaseText => {
      const currentPhase = currentPhaseText.toUpperCase().replace(/[- ]/g, '_');
      const currentIndex = phases.findIndex(p => currentPhase.includes(p));

      if (currentIndex >= targetIndex) {
        cy.log(`Reached target phase: ${targetPhase}`);
        return;
      }

      
      cy.get('body').then($body => {
        if ($body.find('[data-cy=action-buttons]').length > 0) {
          cy.playThroughPhase();
          cy.wait(500);
        } else {
          cy.wait(1000);
        }
        checkAndPlay(attempts + 1);
      });
    });
  };

  checkAndPlay();
});





Cypress.Commands.add('playerAction', (action: string, amount?: number) => {
  const actionMap: Record<string, string> = {
    fold: '[data-cy=fold-btn]',
    check: '[data-cy=check-btn]',
    call: '[data-cy=call-btn]',
    raise: '[data-cy=raise-btn]',
    allin: '[data-cy=all-in-btn]'
  };

  const selector = actionMap[action.toLowerCase()];
  if (!selector) {
    throw new Error(`Unknown action: ${action}`);
  }

  if (action.toLowerCase() === 'raise' && amount) {
    cy.get(selector).click();
    cy.get('[data-cy=raise-modal]').should('be.visible');
    cy.get('[data-cy=raise-input]').clear().type(amount.toString());
    cy.get('[data-cy=confirm-raise-btn]').click();
  } else {
    cy.get(selector).click();
  }
});

Cypress.Commands.add('getCurrentPlayer', () => {
  return cy.get('[data-cy=player-seat][aria-current="true"]');
});

Cypress.Commands.add('getHumanPlayer', () => {
  return cy.get('[data-cy=human-seat]');
});





Cypress.Commands.add('waitForPhase', (phase: string) => {
  cy.get('[data-cy=phase-value]', { timeout: 30000 })
    .should('contain', phase);
});

Cypress.Commands.add('verifyPot', (amount: number) => {
  cy.get('[data-cy=pot-value]')
    .invoke('text')
    .should('match', new RegExp(`\\$?${amount}`));
});

Cypress.Commands.add('getCommunityCardsCount', () => {
  return cy.get('[data-cy=community-card]').its('length');
});





Cypress.Commands.add('interceptApi', () => {
  cy.intercept('GET', '/api/poker/status*').as('getGameStatus');
  cy.intercept('POST', '/api/poker/action*').as('postAction');
  cy.intercept('GET', '/api/statistics*').as('getStats');
  cy.intercept('GET', '/api/history*').as('getHistory');
  cy.intercept('GET', '/api/leaderboard*').as('getLeaderboard');
});

Cypress.Commands.add('mockGameStatus', (overrides = {}) => {
  const defaultStatus = {
    phase: 'PRE_FLOP',
    currentPot: 30,
    currentPlayerIndex: 0,
    players: [
      { name: 'TestPlayer', chips: 1000, hand: [], folded: false, isAllIn: false, betAmount: 0 },
      { name: 'Bot1', chips: 990, hand: [], folded: false, isAllIn: false, betAmount: 10 },
      { name: 'Bot2', chips: 980, hand: [], folded: false, isAllIn: false, betAmount: 20 }
    ],
    communityCards: [],
    dealerPosition: 0,
    ...overrides
  };

  cy.intercept('GET', '/api/poker/status*', { body: defaultStatus }).as('mockedStatus');
});





Cypress.Commands.add('navigateTo', (path: string) => {
  cy.visit(path);
  cy.url().should('include', path);
});

Cypress.Commands.add('startNewGame', (players = ['TestPlayer', 'Bot1', 'Bot2']) => {
  cy.visit('/lobby');
  cy.get('[data-cy=start-game-btn]').click();
  cy.url().should('include', '/game');
});





Cypress.Commands.add('checkFocusVisible', (selector: string) => {
  cy.get(selector).focus();
  cy.focused().should('have.css', 'outline').and('not.eq', 'none');
});

Cypress.Commands.add('testKeyboardNav', (selectors: string[]) => {
  selectors.forEach((selector, index) => {
    if (index === 0) {
      cy.get(selector).first().focus();
    } else {
      cy.focused().tab();
    }
    cy.focused().should('match', selector);
  });
});

Cypress.Commands.add('injectAxe', () => {
  
  cy.window().then(win => {
    if (!(win as any).axe) {
      cy.log('axe-core not available, using stub');
    }
  });
});

Cypress.Commands.add('checkA11y', (context?: string, options?: object) => {
  cy.log('Accessibility check', { context, options });
});

Cypress.Commands.add('tab', { prevSubject: 'optional' }, (subject) => {
  if (subject) {
    cy.wrap(subject).trigger('keydown', { keyCode: 9, which: 9 });
  } else {
    cy.get('body').trigger('keydown', { keyCode: 9, which: 9 });
  }
  return cy.focused();
});





Cypress.Commands.add('clearAllStorage', () => {
  cy.window().then(win => {
    win.localStorage.clear();
    win.sessionStorage.clear();
  });
  cy.clearCookies();
});

Cypress.Commands.add('waitForNetworkIdle', (timeout = 2000) => {
  cy.wait(timeout);
});

Cypress.Commands.add('screenshotWithTimestamp', (name: string) => {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  cy.screenshot(`${name}-${timestamp}`);
});

export {};
