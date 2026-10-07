import { defineConfig } from 'cypress';
import { readFileSync } from 'node:fs';

export default defineConfig({
  e2e: {
    baseUrl: process.env['CYPRESS_BASE_URL'] || 'http://localhost:4200',
    viewportWidth: 1280,
    viewportHeight: 720,
    video: false,
    screenshotOnRunFailure: true,
    defaultCommandTimeout: 10000,
    requestTimeout: 10000,
    responseTimeout: 30000,
    retries: {
      runMode: 2,
      openMode: 0
    },
    setupNodeEvents(on, config) {
      const axeSource = readFileSync(require.resolve('axe-core/axe.min.js'), 'utf8');
      on('task', { axeSource: () => axeSource });
    },
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.ts'
  },
  component: {
    devServer: {
      framework: 'angular',
      bundler: 'webpack'
    },
    specPattern: '**/*.cy.ts'
  }
});
