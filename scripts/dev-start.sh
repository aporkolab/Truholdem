#!/bin/bash
# Development startup script that finds an available port

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Cleanup stale connections and processes
cleanup_stale_connections() {
    echo -e "${YELLOW}Cleaning up stale connections...${NC}"

    # Kill any existing Spring Boot processes
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "TruholdemApplication" 2>/dev/null || true

    # Wait a moment for processes to terminate
    sleep 1

    # Check if PostgreSQL is running in Docker
    local pg_container=$(docker ps --filter "name=postgres" --format "{{.Names}}" 2>/dev/null | head -1)

    if [ -n "$pg_container" ]; then
        echo -e "${YELLOW}Terminating idle PostgreSQL connections...${NC}"

        # Terminate all idle connections (except the one we're using)
        # Try with 'app' user first (Docker setup), then 'postgres' user
        docker exec "$pg_container" psql -U app -d truholdem -c "
            SELECT pg_terminate_backend(pid)
            FROM pg_stat_activity
            WHERE datname = 'truholdem'
            AND pid <> pg_backend_pid()
            AND state = 'idle';
        " 2>/dev/null || docker exec "$pg_container" psql -U postgres -d truholdem -c "
            SELECT pg_terminate_backend(pid)
            FROM pg_stat_activity
            WHERE datname = 'truholdem'
            AND pid <> pg_backend_pid()
            AND state = 'idle';
        " 2>/dev/null || true

        # If connection slots are exhausted, restart the container
        if ! docker exec "$pg_container" psql -U app -c "SELECT 1;" > /dev/null 2>&1 && \
           ! docker exec "$pg_container" psql -U postgres -c "SELECT 1;" > /dev/null 2>&1; then
            echo -e "${RED}PostgreSQL connections exhausted. Restarting container...${NC}"
            docker restart "$pg_container"
            sleep 3
        fi

        echo -e "${GREEN}Connection cleanup complete.${NC}"
    fi
}

# Find available port starting from 8080
find_available_port() {
    local port=${1:-8080}
    local max_port=$((port + 100))

    while [ $port -lt $max_port ]; do
        if ! lsof -i :$port > /dev/null 2>&1; then
            echo $port
            return 0
        fi
        port=$((port + 1))
    done

    echo "Error: No available port found in range 8080-$max_port" >&2
    return 1
}

echo -e "${BLUE}=== TruHoldem Development Server ===${NC}"

# Run cleanup before starting
cleanup_stale_connections

# Find available port
BACKEND_PORT=$(find_available_port 8080)
echo -e "${GREEN}Found available port: ${BACKEND_PORT}${NC}"

export SERVER_PORT=$BACKEND_PORT
export BACKEND_PORT=$BACKEND_PORT

# Check if we should start backend
if [ "$1" != "--frontend-only" ]; then
    echo -e "${YELLOW}Starting backend on port ${BACKEND_PORT}...${NC}"
    cd "$PROJECT_ROOT/backend"
    ./mvnw spring-boot:run \
        -Dspring-boot.run.jvmArguments="-Dotel.sdk.disabled=true" \
        -Dspring-boot.run.arguments="--server.port=${BACKEND_PORT}" &
    BACKEND_PID=$!

    # Wait for backend to start
    echo "Waiting for backend to start..."
    for i in {1..30}; do
        if curl -s "http://localhost:${BACKEND_PORT}/api/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}Backend started successfully!${NC}"
            break
        fi
        sleep 2
    done
fi

# Start frontend with proxy pointing to backend
echo -e "${YELLOW}Starting frontend with proxy to port ${BACKEND_PORT}...${NC}"
cd "$PROJECT_ROOT/frontend"
BACKEND_PORT=$BACKEND_PORT npm run dev

# Cleanup on exit
cleanup() {
    if [ ! -z "$BACKEND_PID" ]; then
        echo "Stopping backend..."
        kill $BACKEND_PID 2>/dev/null || true
    fi
}
trap cleanup EXIT
