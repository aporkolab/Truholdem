// Exercise the real Angular development server after build-system changes.
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { createHash } = require('node:crypto');
const { once } = require('node:events');
const http = require('node:http');
const path = require('node:path');
const { setTimeout: delay } = require('node:timers/promises');

async function listen(server) {
  server.listen(0);
  await once(server, 'listening');
  return server.address().port;
}

async function verifyUpgrade(port, route) {
  const key = 'dGhlIHNhbXBsZSBub25jZQ==';
  await new Promise((resolve, reject) => {
    const request = http.request({
      hostname: '127.0.0.1', port, path: route,
      headers: {
        Connection: 'Upgrade', Upgrade: 'websocket',
        'Sec-WebSocket-Version': '13', 'Sec-WebSocket-Key': key
      }
    });
    const timeout = setTimeout(() => request.destroy(new Error('WebSocket upgrade timed out')), 10000);
    request.on('error', error => { clearTimeout(timeout); reject(error); });
    request.on('response', response => {
      clearTimeout(timeout);
      response.resume();
      reject(new Error(`Expected WebSocket upgrade, got HTTP ${response.statusCode}`));
    });
    request.on('upgrade', (response, socket) => {
      clearTimeout(timeout);
      socket.destroy();
      try {
        assert.equal(response.statusCode, 101);
        assert.equal(response.headers['x-proxy-test-path'], route);
        assert.equal(response.headers['sec-websocket-accept'],
          createHash('sha1').update(key + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11').digest('base64'));
        resolve();
      } catch (error) { reject(error); }
    });
    request.end();
  });
}

async function main() {
  const sockets = new Set();
  const backend = http.createServer((request, response) => {
    response.setHeader('Content-Type', 'application/json');
    response.end(JSON.stringify({ url: request.url, host: request.headers.host }));
  });
  backend.on('connection', socket => {
    sockets.add(socket);
    socket.on('close', () => sockets.delete(socket));
  });
  backend.on('upgrade', (request, socket) => {
    const accept = createHash('sha1')
      .update(request.headers['sec-websocket-key'] + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11')
      .digest('base64');
    socket.write('HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\n' +
      `Connection: Upgrade\r\nSec-WebSocket-Accept: ${accept}\r\n` +
      `X-Proxy-Test-Path: ${request.url}\r\n\r\n`);
  });

  let child;
  let output = '';
  try {
    const backendPort = await listen(backend);
    const reservation = http.createServer();
    const frontendPort = await listen(reservation);
    await new Promise(resolve => reservation.close(resolve));
    child = spawn(process.execPath, [require.resolve('@angular/cli/bin/ng.js'),
      'serve', '--host=127.0.0.1', `--port=${frontendPort}`], {
      cwd: path.resolve(__dirname, '..'),
      env: { ...process.env, BACKEND_PORT: String(backendPort), NG_CLI_ANALYTICS: 'false' },
      stdio: ['ignore', 'pipe', 'pipe']
    });
    child.stdout.on('data', chunk => { output += chunk; });
    child.stderr.on('data', chunk => { output += chunk; });
    let spawnError;
    child.on('error', error => { spawnError = error; });
    const baseUrl = `http://127.0.0.1:${frontendPort}`;
    const deadline = Date.now() + 120000;
    while (true) {
      if (spawnError) throw spawnError;
      if (child.exitCode !== null) throw new Error(`Angular exited with ${child.exitCode}`);
      try {
        const response = await fetch(baseUrl, { signal: AbortSignal.timeout(2000) });
        if (response.ok && (await response.text()).includes('<app-root')) break;
      } catch { /* Wait for the first development build. */ }
      if (Date.now() >= deadline) throw new Error('Angular development server did not become ready');
      await delay(250);
    }
    for (const route of ['/api', '/api/game/test?check=1', '/ws/info', '/ws/123/session/xhr']) {
      const response = await fetch(baseUrl + route, { signal: AbortSignal.timeout(10000) });
      assert.equal(response.status, 200, route);
      assert.deepEqual(await response.json(), { url: route, host: `localhost:${backendPort}` });
    }
    for (const route of ['/api-docs', '/ws-other']) {
      const response = await fetch(baseUrl + route, { signal: AbortSignal.timeout(10000) });
      assert.match(response.headers.get('content-type'), /text\/html/, route);
    }
    await verifyUpgrade(frontendPort, '/ws/123/session/websocket');
    console.log('Development server, API routes, SockJS routes and WebSocket upgrade passed.');
  } catch (error) {
    process.stderr.write(output);
    throw error;
  } finally {
    if (child && child.exitCode === null && child.pid) {
      const exited = once(child, 'exit');
      child.kill('SIGTERM');
      const timeout = setTimeout(() => child.kill('SIGKILL'), 5000);
      await exited;
      clearTimeout(timeout);
    }
    for (const socket of sockets) socket.destroy();
    await new Promise(resolve => backend.close(resolve));
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
