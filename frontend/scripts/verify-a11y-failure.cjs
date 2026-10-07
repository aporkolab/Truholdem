const assert = require('node:assert/strict');
const cypress = require('cypress');

async function main() {
  const result = await cypress.run({
    browser: process.env.CYPRESS_BROWSER || 'electron',
    config: {
      specPattern: 'cypress/regression/axe-violation.cy.ts',
      retries: 0,
      screenshotOnRunFailure: false,
      video: false
    }
  });

  assert.notEqual(result.status, 'failed', 'Cypress must execute the negative regression');
  assert.equal(result.runs.length, 1, 'Exactly one regression spec must run');
  assert.equal(result.totalTests, 1, 'Exactly one deliberate violation must be tested');
  assert.equal(result.totalFailed, 1, 'The unnamed button must fail cy.checkA11y()');
  assert.equal(result.totalPassed, 0, 'A no-op accessibility helper must fail this regression gate');

  const test = result.runs[0].tests[0];
  assert.equal(test.state, 'failed');
  assert.match(test.displayError, /Accessibility violations:/);
  assert.match(test.displayError, /button-name/);
  assert.match(test.displayError, /#unnamed-button/);
  console.log('PASS: cy.checkA11y() rejected the deliberate button-name violation at #unnamed-button.');
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
