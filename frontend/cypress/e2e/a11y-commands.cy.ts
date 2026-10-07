describe('axe-core commands', () => {
  beforeEach(() => {
    cy.intercept('GET', '/__a11y-regression', { fixture: 'a11y.html' });
    cy.visit('/__a11y-regression');
    cy.injectAxe();
  });

  it('scans the requested context and calls the callback with real results', () => {
    const onViolations = cy.stub().as('onViolations');
    cy.checkA11y('#valid', { runOnly: ['button-name'] }, onViolations);
    cy.get('@onViolations').should('have.been.calledOnceWithExactly', []);
  });

  it('passes a full-document scan after the accessible name is fixed', () => {
    cy.get('#unnamed-button').invoke('attr', 'aria-label', 'Fold');
    cy.checkA11y(null, { runOnly: ['button-name'] });
  });

  it('reinjects into the new application window after a reload', () => {
    cy.reload();
    cy.injectAxe();
    cy.checkA11y('#valid', { runOnly: ['button-name'] });
  });
});
