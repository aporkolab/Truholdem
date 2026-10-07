// Intentionally outside the normal spec pattern. The Node runner requires this to fail.
it('rejects an unnamed button', () => {
  cy.intercept('GET', '/__a11y-regression', { fixture: 'a11y.html' });
  cy.visit('/__a11y-regression');
  cy.injectAxe();
  cy.checkA11y(null, { runOnly: ['button-name'] }, violations => {
    // A callback must receive real results without bypassing the failure assertion.
    expect(violations.map(violation => violation.id)).to.deep.equal(['button-name']);
  });
});
