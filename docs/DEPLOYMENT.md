# TruHoldem Deployment Guide

Production deployment and operations guide for the TruHoldem poker platform.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Development Setup](#development-setup)
- [Docker Deployment](#docker-deployment)
- [Production Configuration](#production-configuration)
- [CI/CD Pipeline](#cicd-pipeline)
- [Monitoring & Observability](#monitoring--observability)
- [Scaling](#scaling)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Component | Version | Purpose |
|-----------|---------|---------|
| Docker | 24+ | Containerization |
| Docker Compose | 2.20+ | Multi-container orchestration |
| Java | 21 | Backend runtime |
| Node.js | 20+ | Frontend build |
| PostgreSQL | 16 | Primary database |
| Redis | 7 | Caching & WebSocket pub/sub |

---

## Quick Start

One-command deployment with Docker Compose:

```bash
# Clone and start
git clone https://github.com/yourusername/truholdem.git
cd truholdem
docker-compose up -d
```

Access points after startup:

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:4200 | - |
| API | http://localhost:8080/api | - |
| Swagger UI | http://localhost:8080/api/swagger-ui.html | - |
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Jaeger UI | http://localhost:16686 | - |

---

## Development Setup

### Backend

```bash
cd backend

# Install dependencies and run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or with specific database
DB_USERNAME=user DB_PASSWORD=password ./mvnw spring-boot:run
```

Required services for local development:

```bash
# Start only infrastructure
docker-compose up -d postgres redis
```

### Frontend

```bash
cd frontend

# Install dependencies
npm ci

# Development server with hot reload
npm run dev

# Build for production
npm run build
```

### Configuration Files

Backend configuration hierarchy:

```
application.properties          # Base config
application-dev.properties      # Development overrides
application-docker.properties   # Docker environment
application-prod.properties     # Production settings
application-test.properties     # Test configuration
```

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | dev | Active Spring profile |
| `DB_USERNAME` | user | Database username |
| `DB_PASSWORD` | password | Database password |
| `REDIS_HOST` | localhost | Redis server host |
| `JWT_SECRET` | - | JWT signing key (min 32 chars) |
| `CORS_ORIGINS` | localhost:4200 | Allowed CORS origins |
| `OTEL_ENABLED` | true | Enable OpenTelemetry |

---

## Docker Deployment

### Building Images

```bash
# Build backend image
docker build -t truholdem-backend:latest ./backend

# Build frontend image
docker build -t truholdem-frontend:latest ./frontend \
  --build-arg API_URL=https://api.yourdomain.com
```

### Docker Compose Services

The `docker-compose.yml` orchestrates:

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| postgres | postgres:16-alpine | 5432 | Primary database |
| redis | redis:7-alpine | 6379 | Cache & pub/sub |
| backend | custom | 8080 | Spring Boot API |
| frontend | custom | 4200 | Angular SPA |
| nginx | nginx:alpine | 80, 443 | Reverse proxy |
| prometheus | prom/prometheus | 9090 | Metrics collection |
| grafana | grafana/grafana | 3000 | Dashboards |
| jaeger | jaegertracing/all-in-one | 16686 | Distributed tracing |
| otel-collector | otel/opentelemetry-collector | 4317 | Telemetry pipeline |

### Health Checks

All services include health checks:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/api/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

---

## Production Configuration

### Security Checklist

- [ ] Generate strong JWT secret: `openssl rand -base64 64`
- [ ] Change default database credentials
- [ ] Configure CORS for production domains only
- [ ] Enable HTTPS with valid certificates
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Disable Swagger in production
- [ ] Configure firewall rules

### Environment Variables (Production)

```bash
# Security
JWT_SECRET=$(openssl rand -base64 64)
DB_PASSWORD=$(openssl rand -base64 32)

# Database
DB_USERNAME=truholdem_prod
SPRING_DATASOURCE_URL=jdbc:postgresql://db.internal:5432/truholdem

# Application
SPRING_PROFILES_ACTIVE=prod
CORS_ORIGINS=https://yourdomain.com
WEBSOCKET_ORIGINS=https://yourdomain.com

# Observability
OTEL_SERVICE_NAME=truholdem-prod
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
```

### SSL/TLS Configuration

Nginx SSL configuration example:

```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;

    location / {
        proxy_pass http://frontend:80;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
    }

    location /api/ws {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### Database Setup

```sql
-- Create production database
CREATE USER truholdem_prod WITH PASSWORD 'secure_password';
CREATE DATABASE truholdem_prod OWNER truholdem_prod;
GRANT ALL PRIVILEGES ON DATABASE truholdem_prod TO truholdem_prod;
```

Liquibase handles schema migrations automatically on startup.

---

## CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/ci-cd.yml`):

### Pipeline Stages

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Backend Tests  │     │ Frontend Tests  │     │    Lint         │
│  (PostgreSQL)   │     │   (Jest)        │     │   (ESLint)      │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                         ┌───────▼───────┐
                         │   E2E Tests   │
                         │   (Cypress)   │
                         └───────┬───────┘
                                 │
                         ┌───────▼───────┐
                         │ Docker Build  │
                         │  & Push GHCR  │
                         └───────────────┘
```

### Triggers

- **Push to main/develop**: Full pipeline with Docker build
- **Pull requests**: Tests only (no Docker build)

### Test Environment

The CI pipeline provisions:

- PostgreSQL 16 service container
- Node.js 20 for frontend builds
- Java 21 (Temurin) for backend
- Cypress for E2E tests

---

## Monitoring & Observability

### Prometheus Metrics

Backend exposes metrics at `/api/actuator/prometheus`:

```
# Custom game metrics
truholdem_games_active_total
truholdem_games_created_total
truholdem_hands_played_total
truholdem_player_actions_total{action="fold|call|raise"}

# JVM metrics
jvm_memory_used_bytes
jvm_gc_pause_seconds

# HTTP metrics
http_server_requests_seconds_count
http_server_requests_seconds_sum
```

### Grafana Dashboards

Pre-configured dashboards in `docker/grafana/dashboards/`:

- **Application Overview**: Request rates, latencies, error rates
- **JVM Metrics**: Memory, GC, threads
- **Game Statistics**: Active games, hands per minute
- **Database**: Connection pool, query times

### Distributed Tracing (Jaeger)

OpenTelemetry automatically traces:

- HTTP requests
- Database queries
- Redis operations
- WebSocket events

Access traces at http://localhost:16686

### Alert Rules

Example Prometheus alerts (`docker/prometheus/alerts.yml`):

```yaml
groups:
  - name: truholdem
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: High error rate detected

      - alert: SlowResponses
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 10m
        labels:
          severity: warning

      - alert: DatabaseConnectionsExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
```

---

## Scaling

### Horizontal Scaling

Enable WebSocket clustering for multiple backend instances:

```yaml
backend:
  deploy:
    replicas: 3
  environment:
    WEBSOCKET_CLUSTER_ENABLED: "true"
    REDIS_HOST: redis
```

Architecture with load balancer:

```
                    ┌─────────────┐
                    │   Nginx     │
                    │   (LB)      │
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           │               │               │
    ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │  Backend 1  │ │  Backend 2  │ │  Backend 3  │
    └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
           │               │               │
           └───────────────┼───────────────┘
                    ┌──────▼──────┐
                    │    Redis    │
                    │  (Pub/Sub)  │
                    └─────────────┘
```

### Load Balancer Configuration

```nginx
upstream backend {
    least_conn;
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
}

# Sticky sessions for WebSocket
upstream backend_ws {
    ip_hash;
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
}
```

See `docs/WEBSOCKET-CLUSTER-DEPLOYMENT.md` for detailed cluster setup.

---

## Troubleshooting

### Common Issues

**Backend won't start**
```bash
# Check logs
docker-compose logs backend

# Common fixes
# 1. Wait for PostgreSQL to be ready
# 2. Verify database credentials
# 3. Check JWT_SECRET is set (min 32 chars)
```

**WebSocket connection fails**
```bash
# Verify CORS settings
curl -v http://localhost:8080/api/ws/info

# Check WebSocket origins config
echo $WEBSOCKET_ORIGINS
```

**Database connection pool exhausted**
```bash
# Check active connections
docker-compose exec postgres psql -U user -d truholdem \
  -c "SELECT count(*) FROM pg_stat_activity WHERE datname='truholdem';"

# Increase pool size in application.properties
spring.datasource.hikari.maximum-pool-size=30
```

**Out of memory errors**
```bash
# Increase JVM heap
docker-compose up -d backend -e JAVA_OPTS="-Xmx1024m -Xms512m"
```

### Health Check Endpoints

| Endpoint | Description |
|----------|-------------|
| `/api/actuator/health` | Overall health status |
| `/api/actuator/health/db` | Database connectivity |
| `/api/actuator/health/redis` | Redis connectivity |
| `/api/actuator/health/diskSpace` | Disk space |
| `/api/actuator/metrics` | All metrics |
| `/api/actuator/prometheus` | Prometheus format |

### Useful Commands

```bash
# View all logs
docker-compose logs -f

# Restart specific service
docker-compose restart backend

# Execute command in container
docker-compose exec backend sh

# Database backup
docker-compose exec postgres pg_dump -U user truholdem > backup.sql

# Database restore
cat backup.sql | docker-compose exec -T postgres psql -U user truholdem

# Clean up everything
docker-compose down -v --rmi all
```

---

## Additional Resources

- [Architecture Documentation](./ARCHITECTURE.md)
- [WebSocket Cluster Setup](./WEBSOCKET-CLUSTER-DEPLOYMENT.md)
- [API Reference](http://localhost:8080/api/swagger-ui.html)
