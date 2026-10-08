const fs = require('fs');
const path = require('path');

function getBackendPort() {
  if (process.env.BACKEND_PORT) {
    return process.env.BACKEND_PORT;
  }

  const portFilePath = path.join(__dirname, '..', '.backend-port');

  try {
    if (fs.existsSync(portFilePath)) {
      const port = fs.readFileSync(portFilePath, 'utf8').trim();
      console.log(`[Proxy] Backend port detected from file: ${port}`);
      return port;
    }
  } catch (e) {
    console.warn(`[Proxy] Could not read port file: ${e.message}`);
  }

  console.log('[Proxy] Using fallback port: 8080');
  return '8080';
}

const BACKEND_PORT = getBackendPort();

module.exports = {
  "/api/**": {
    "target": `http://localhost:${BACKEND_PORT}`,
    "secure": false,
    "changeOrigin": true
  },
  "/ws/**": {
    "target": `http://localhost:${BACKEND_PORT}`,
    "secure": false,
    "ws": true,
    "changeOrigin": true
  }
};
