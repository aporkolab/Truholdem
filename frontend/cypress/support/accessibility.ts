import axe, { type Result, type RunOptions, type ElementContext } from 'axe-core';

declare global {
  interface Window {
    axe?: typeof axe;
  }

  namespace Cypress {
    interface Chainable {
      injectAxe(): Chainable<void>;
      checkA11y(
        context?: ElementContext | null,
        options?: RunOptions,
        violationCallback?: (violations: Result[]) => void
      ): Chainable<void>;
    }
  }
}

Cypress.Commands.add('injectAxe', () => {
  cy.window({ log: false }).then(win => {
    // Evaluate the installed engine in the application window, not Cypress's runner.
    win.eval(axe.source);
    expect(win.axe?.run, 'axe-core is available in the application window').to.be.a('function');
  });
});

Cypress.Commands.add('checkA11y', (context, options = {}, violationCallback) => {
  cy.window({ log: false })
    .then({ timeout: 30000 }, win => {
      if (!win.axe) {
        throw new Error('axe-core is not injected. Call cy.injectAxe() after cy.visit() or cy.reload().');
      }
      return win.axe.run(context ?? win.document, options);
    })
    .then(({ violations }) => {
      for (const violation of violations) {
        Cypress.log({
          name: 'a11y',
          message: `${violation.id}: ${violation.help}`,
          consoleProps: () => ({ ...violation })
        });
      }
      violationCallback?.(violations);

      const details = violations.map(violation =>
        `${violation.id} (${violation.impact}): ${violation.nodes.map(node => node.target.join(' ')).join(', ')}`
      ).join('\n');
      expect(violations, `Accessibility violations:\n${details}`).to.have.length(0);
    });
});
