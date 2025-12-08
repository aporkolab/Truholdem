



Cypress.Commands.add('startGame', (players) => {
  const defaultPlayers = [
    { name: 'Player', chips: 1000, isBot: false },
    { name: 'Bot1', chips: 1000, isBot: true },
    { name: 'Bot2', chips: 1000, isBot: true }
  ];

  const gamePlayers = players || defaultPlayers;

  cy.visit('/register-players');
  
  
  cy.get('[data-cy=clear-players]').click();
  
  gamePlayers.forEach((player, index) => {
    cy.get('[data-cy=player-name-input]').type(player.name);
    cy.get('[data-cy=player-chips-input]').clear().type(player.chips.toString());
    if (player.isBot) {
      cy.get('[data-cy=is-bot-checkbox]').check();
    }
    cy.get('[data-cy=add-player-btn]').click();
  });

  cy.get('[data-cy=start-game-btn]').click();
  cy.url().should('include', '/game');
});

Cypress.Commands.add('playerAction', (action, amount) => {
  switch (action) {
    case 'fold':
      cy.get('[data-cy=fold-btn]').click();
      break;
    case 'check':
      cy.get('[data-cy=check-btn]').click();
      break;
    case 'call':
      cy.get('[data-cy=call-btn]').click();
      break;
    case 'raise':
      if (amount) {
        cy.get('[data-cy=raise-input]').clear().type(amount.toString());
      }
      cy.get('[data-cy=raise-btn]').click();
      break;
  }
});

Cypress.Commands.add('waitForPhase', (phase) => {
  cy.get('[data-cy=game-phase]', { timeout: 30000 })
    .should('contain', phase);
});

Cypress.Commands.add('getCurrentPlayer', () => {
  return cy.get('.player-seat.active');
});

Cypress.Commands.add('interceptApi', () => {
  cy.intercept('POST', '/api/poker/start').as('startGame');
  cy.intercept('POST', '/api/poker/fold*').as('fold');
  cy.intercept('POST', '/api/poker/check*').as('check');
  cy.intercept('POST', '/api/poker/call*').as('call');
  cy.intercept('POST', '/api/poker/raise').as('raise');
  cy.intercept('POST', '/api/poker/bet').as('bet');
  cy.intercept('GET', '/api/poker/status').as('gameStatus');
  cy.intercept('POST', '/api/poker/bot-action/*').as('botAction');
  cy.intercept('GET', '/api/stats/*').as('stats');
  cy.intercept('GET', '/api/history/*').as('history');
});
