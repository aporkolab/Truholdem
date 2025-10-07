const fs = require('fs');
const path = require('path');

function getBackendPort() {
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

  const envPort = process.env.BACKEND_PORT || '8080';
  console.log(`[Proxy] Using fallback port: ${envPort}`);
  return envPort;
}

const BACKEND_PORT = getBackendPort();

module.exports = {
  "/api": {
    "target": `http://localhost:${BACKEND_PORT}`,
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  },
  "/ws": {
    "target": `http://localhost:${BACKEND_PORT}`,
    "secure": false,
    "ws": true,
    "changeOrigin": true,
    "logLevel": "debug"
  }
};
