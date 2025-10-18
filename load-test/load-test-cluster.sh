#!/bin/bash























set -e


INSTANCES=${INSTANCES:-3}
CONNECTIONS=${CONNECTIONS:-10}
DURATION=${DURATION:-60s}
RAMP_UP=${RAMP_UP:-10s}
CLEANUP=${CLEANUP:-false}
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"


RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' 

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}


while [[ $
    case $1 in
        --instances)
            INSTANCES="$2"
            shift 2
            ;;
        --connections)
            CONNECTIONS="$2"
            shift 2
            ;;
        --duration)
            DURATION="$2"
            shift 2
            ;;
        --ramp-up)
            RAMP_UP="$2"
            shift 2
            ;;
        --cleanup)
            CLEANUP=true
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

log_info "============================================"
log_info "TruHoldem WebSocket Cluster Load Test"
log_info "============================================"
log_info "Instances: $INSTANCES"
log_info "Connections per VU: $CONNECTIONS"
log_info "Duration: $DURATION"
log_info "Ramp-up: $RAMP_UP"
log_info "============================================"


create_cluster_compose() {
    cat > "$PROJECT_DIR/docker-compose.loadtest.yml" << EOF
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: truholdem
      POSTGRES_USER: truholdem
      POSTGRES_PASSWORD: truholdem
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U truholdem"]
      interval: 5s
      timeout: 3s
      retries: 5

  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./load-test/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
$(for i in $(seq 1 $INSTANCES); do echo "      - backend-$i"; done)

$(for i in $(seq 1 $INSTANCES); do
    PORT=$((8080 + i))
    cat << BACKEND
  backend-$i:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/truholdem
      - SPRING_DATASOURCE_USERNAME=truholdem
      - SPRING_DATASOURCE_PASSWORD=truholdem
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - WEBSOCKET_CLUSTER_ENABLED=true
      - HOSTNAME=backend-$i
      - SERVER_PORT=8080
    depends_on:
      redis:
        condition: service_healthy
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

BACKEND
done)

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./load-test/prometheus.yml:/etc/prometheus/prometheus.yml:ro

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_AUTH_ANONYMOUS_ENABLED=true
    volumes:
      - ./docker/grafana/provisioning:/etc/grafana/provisioning:ro
      - ./docker/grafana/dashboards:/var/lib/grafana/dashboards:ro
EOF
}


create_nginx_config() {
    mkdir -p "$PROJECT_DIR/load-test"
    cat > "$PROJECT_DIR/load-test/nginx.conf" << EOF
events {
    worker_connections 4096;
}

http {
    upstream backend {
        
        ip_hash;
$(for i in $(seq 1 $INSTANCES); do
    echo "        server backend-$i:8080;"
done)
    }

    
    map \$http_upgrade \$connection_upgrade {
        default upgrade;
        '' close;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade \$http_upgrade;
            proxy_set_header Connection \$connection_upgrade;
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_read_timeout 3600s;
            proxy_send_timeout 3600s;
        }

        location /ws {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade \$http_upgrade;
            proxy_set_header Connection \$connection_upgrade;
            proxy_set_header Host \$host;
            proxy_read_timeout 3600s;
            proxy_send_timeout 3600s;
        }
    }
}
EOF
}


create_prometheus_config() {
    cat > "$PROJECT_DIR/load-test/prometheus.yml" << EOF
global:
  scrape_interval: 5s

scrape_configs:
$(for i in $(seq 1 $INSTANCES); do
    cat << SCRAPE
  - job_name: 'backend-$i'
    static_configs:
      - targets: ['backend-$i:8080']
    metrics_path: '/actuator/prometheus'
SCRAPE
done)

  - job_name: 'redis'
    static_configs:
      - targets: ['redis:6379']
EOF
}


create_k6_script() {
    cat > "$PROJECT_DIR/load-test/websocket-load-test.js" << 'EOF'
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import http from 'k6/http';

// Custom metrics
const wsConnections = new Counter('ws_connections');
const wsMessages = new Counter('ws_messages_received');
const wsErrors = new Counter('ws_errors');
const wsLatency = new Trend('ws_message_latency');
const wsConnectTime = new Trend('ws_connect_time');
const wsSuccessRate = new Rate('ws_success_rate');

export const options = {
    stages: [
        { duration: __ENV.RAMP_UP || '10s', target: parseInt(__ENV.VUS) || 50 },
        { duration: __ENV.DURATION || '60s', target: parseInt(__ENV.VUS) || 50 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'ws_success_rate': ['rate>0.95'],
        'ws_connect_time': ['p(95)<5000'],
        'ws_message_latency': ['p(95)<1000'],
        'ws_errors': ['count<100'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WS_URL = BASE_URL.replace('http', 'ws') + '/ws';

export function setup() {
    // Register a test user and get auth token
    const registerRes = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
        username: `loadtest_${Date.now()}`,
        email: `loadtest_${Date.now()}@test.com`,
        password: 'LoadTest123!'
    }), {
        headers: { 'Content-Type': 'application/json' }
    });

    if (registerRes.status !== 200 && registerRes.status !== 201) {
        console.log('Registration failed, trying login...');
    }

    const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
        username: 'loadtest_user',
        password: 'LoadTest123!'
    }), {
        headers: { 'Content-Type': 'application/json' }
    });

    if (loginRes.status === 200) {
        const body = JSON.parse(loginRes.body);
        return { token: body.accessToken };
    }

    // If login failed, continue without auth for basic WebSocket test
    return { token: null };
}

export default function(data) {
    const token = data.token || 'test-token';
    const gameId = `game-${__VU}-${__ITER}`;
    
    const startTime = Date.now();
    
    const res = ws.connect(WS_URL, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    }, function(socket) {
        const connectTime = Date.now() - startTime;
        wsConnectTime.add(connectTime);
        wsConnections.add(1);
        wsSuccessRate.add(1);

        // Subscribe to STOMP
        socket.on('open', function() {
            // STOMP CONNECT frame
            socket.send('CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\x00');
        });

        socket.on('message', function(message) {
            wsMessages.add(1);
            
            if (message.includes('CONNECTED')) {
                // Subscribe to game topic
                socket.send(`SUBSCRIBE\nid:sub-${gameId}\ndestination:/topic/game/${gameId}\n\n\x00`);
            }
            
            if (message.includes('MESSAGE')) {
                const receiveTime = Date.now();
                // Extract timestamp from message if present
                const match = message.match(/"timestamp":(\d+)/);
                if (match) {
                    const sentTime = parseInt(match[1]);
                    wsLatency.add(receiveTime - sentTime);
                }
            }
        });

        socket.on('error', function(e) {
            wsErrors.add(1);
            wsSuccessRate.add(0);
            console.log('WebSocket error:', e);
        });

        socket.on('close', function() {
            // Connection closed
        });

        // Keep connection alive for test duration
        socket.setTimeout(function() {
            // Send a ping every 10 seconds
            socket.send('\n');
        }, 10000);

        // Keep connection open
        sleep(30);
    });

    if (res === null || res.status !== 101) {
        wsErrors.add(1);
        wsSuccessRate.add(0);
    }
}

export function teardown(data) {
    console.log('Load test completed');
}
EOF
}


create_analysis_script() {
    cat > "$PROJECT_DIR/load-test/analyze-results.sh" << 'EOF'
#!/bin/bash



RESULTS_FILE=${1:-"results.json"}

if [ ! -f "$RESULTS_FILE" ]; then
    echo "Results file not found: $RESULTS_FILE"
    exit 1
fi

echo "============================================"
echo "Load Test Results Analysis"
echo "============================================"


echo ""
echo "WebSocket Metrics:"
echo "------------------"
jq -r '.metrics.ws_connections.values.count // "N/A"' "$RESULTS_FILE" | xargs -I {} echo "Total Connections: {}"
jq -r '.metrics.ws_messages_received.values.count // "N/A"' "$RESULTS_FILE" | xargs -I {} echo "Total Messages: {}"
jq -r '.metrics.ws_errors.values.count // "N/A"' "$RESULTS_FILE" | xargs -I {} echo "Total Errors: {}"

echo ""
echo "Latency (ms):"
echo "-------------"
jq -r '.metrics.ws_message_latency.values | "p50: \(.["p(50)"] // "N/A"), p95: \(.["p(95)"] // "N/A"), p99: \(.["p(99)"] // "N/A")"' "$RESULTS_FILE"

echo ""
echo "Connect Time (ms):"
echo "------------------"
jq -r '.metrics.ws_connect_time.values | "p50: \(.["p(50)"] // "N/A"), p95: \(.["p(95)"] // "N/A"), max: \(.max // "N/A")"' "$RESULTS_FILE"

echo ""
echo "Success Rate:"
echo "-------------"
jq -r '.metrics.ws_success_rate.values.rate // "N/A"' "$RESULTS_FILE" | xargs -I {} echo "{}%"

echo ""
echo "Threshold Results:"
echo "------------------"
jq -r '.root_group.checks // {} | to_entries[] | "\(.key): \(.value.passes)/\(.value.fails)"' "$RESULTS_FILE" 2>/dev/null || echo "No threshold data"

echo "============================================"
EOF
    chmod +x "$PROJECT_DIR/load-test/analyze-results.sh"
}


main() {
    log_info "Creating configuration files..."
    create_cluster_compose
    create_nginx_config
    create_prometheus_config
    create_k6_script
    create_analysis_script

    log_success "Configuration files created"

    
    log_info "Starting cluster with $INSTANCES backend instances..."
    cd "$PROJECT_DIR"
    docker-compose -f docker-compose.loadtest.yml up -d --build

    
    log_info "Waiting for services to be healthy..."
    sleep 30

    
    for i in $(seq 1 $INSTANCES); do
        log_info "Checking backend-$i health..."
        docker-compose -f docker-compose.loadtest.yml exec -T backend-$i curl -f http://localhost:8080/actuator/health || log_warn "backend-$i may not be ready"
    done

    
    log_info "Running k6 load test..."
    if command -v k6 &> /dev/null; then
        k6 run \
            --env BASE_URL=http://localhost:8080 \
            --env VUS=$CONNECTIONS \
            --env DURATION=$DURATION \
            --env RAMP_UP=$RAMP_UP \
            --out json=load-test/results.json \
            load-test/websocket-load-test.js

        log_success "Load test completed"
        
        
        ./load-test/analyze-results.sh load-test/results.json
    else
        log_warn "k6 not installed. To run the load test manually:"
        echo "  1. Install k6: https://k6.io/docs/getting-started/installation/"
        echo "  2. Run: k6 run load-test/websocket-load-test.js"
    fi

    
    log_info "View metrics at: http://localhost:3000 (admin/admin)"
    log_info "Prometheus at: http://localhost:9090"

    if [ "$CLEANUP" = true ]; then
        log_info "Cleaning up..."
        docker-compose -f docker-compose.loadtest.yml down -v
        rm -rf load-test/
        log_success "Cleanup complete"
    else
        log_info "To clean up, run: docker-compose -f docker-compose.loadtest.yml down -v"
    fi
}

main
