# TruHoldem Frontend

The frontend currently uses **Angular and Angular CLI 22.2.1**, **NgRx ComponentStore 22.0.1**, **Jest 30**, and **Cypress 15.15.1**. The application dependencies are defined in [package.json](package.json).

The project was originally generated with Angular CLI 16.2.6; that is its scaffolding history, not its current framework version.

## Setup

Use Node.js `^22.22.3 || ^24.15.0 || >=26.0.0` and npm 10+ (the root manifest pins npm 10.9.0). Install from the repository root because the frontend is an npm workspace with a shared root lockfile:

```bash
npm ci
```

The commands below also run from the repository root.

## Development server

```bash
npm run dev:frontend
```

Open `http://localhost:4200/`. The server reloads on source changes and uses `frontend/proxy.conf.js` to forward API requests to the backend. Start the backend separately with `npm run dev:backend`, or start both with `npm run dev`.

Set `BACKEND_PORT` to explicitly select a backend port. Otherwise, the proxy reads the repository's `.backend-port` file and falls back to port 8080.

Run `npm run test:dev-proxy --workspace=frontend` to smoke-test the development server and its HTTP and WebSocket proxy routes against an isolated backend. This check also runs in CI.

## Code scaffolding

```bash
npm run ng --workspace=frontend -- generate component component-name
```

## Production build

```bash
npm run build:frontend
```

The `@angular/build:application` builder uses esbuild, with Vite serving local development builds. Build artifacts are written directly to `frontend/dist/texas-holdem-frontend/`, the path used by Docker and the E2E server.

## Unit tests and coverage

```bash
npm run test:frontend
```

This runs Jest in CI mode with coverage. Use `npm test --workspace=frontend` for a regular Jest run. Coverage gates and reports are configured in [jest.config.ts](jest.config.ts).

## End-to-end tests

Start the backend and frontend before running Cypress:

```bash
# Interactive Cypress runner
npm run e2e --workspace=frontend

# Headless Chrome run
npm run e2e:ci --workspace=frontend

# Frontend unit tests followed by E2E tests
npm run test:all --workspace=frontend
```

Cypress runs E2E tests; component and service unit tests use Jest.

The default base URL is `http://localhost:4200`; set `CYPRESS_BASE_URL` to test another running frontend. Specs and support commands live under `cypress/`, with runner settings in [cypress.config.ts](cypress.config.ts).

See the [main README](../README.md) for backend setup, the complete stack and CI test results.
