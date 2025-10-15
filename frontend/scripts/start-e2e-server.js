#!/usr/bin/env node
const { spawn } = require('child_process');
const net = require('net');
const fs = require('fs');
const path = require('path');

async function findAvailablePort(startPort = 3000, endPort = 9000) {
  for (let port = startPort; port <= endPort; port++) {
    const available = await isPortAvailable(port);
    if (available) {
      return port;
    }
  }
  throw new Error(`No available port found in range ${startPort}-${endPort}`);
}

function isPortAvailable(port) {
  return new Promise((resolve) => {
    const server = net.createServer();
    server.listen(port, '127.0.0.1');
    server.on('listening', () => {
      server.close();
      resolve(true);
    });
    server.on('error', () => {
      resolve(false);
    });
  });
}

async function main() {
  try {
    const port = await findAvailablePort();
    console.log(`Found available port: ${port}`);

    // Write to GITHUB_ENV if running in CI
    if (process.env.GITHUB_ENV) {
      fs.appendFileSync(process.env.GITHUB_ENV, `FRONTEND_PORT=${port}\n`);
      fs.appendFileSync(process.env.GITHUB_ENV, `CYPRESS_BASE_URL=http://localhost:${port}\n`);
      console.log(`Exported FRONTEND_PORT and CYPRESS_BASE_URL to GITHUB_ENV`);
    }

    // Write port to a temp file for other scripts to read
    const portFile = path.join(__dirname, '..', '.e2e-port');
    fs.writeFileSync(portFile, port.toString());
    console.log(`Port written to ${portFile}`);

    // Start serve
    console.log(`Starting server on http://localhost:${port}`);
    const serve = spawn('npx', ['serve', '-s', 'dist/texas-holdem-frontend', '-l', port.toString()], {
      stdio: 'inherit',
      cwd: path.join(__dirname, '..')
    });

    serve.on('error', (err) => {
      console.error('Failed to start server:', err);
      process.exit(1);
    });

    serve.on('close', (code) => {
      process.exit(code || 0);
    });

    // Handle termination signals
    process.on('SIGINT', () => serve.kill('SIGINT'));
    process.on('SIGTERM', () => serve.kill('SIGTERM'));

  } catch (err) {
    console.error('Error:', err.message);
    process.exit(1);
  }
}

main();
