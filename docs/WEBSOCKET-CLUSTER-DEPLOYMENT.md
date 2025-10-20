# WebSocket Cluster Deployment Guide

## Overview

This guide covers deploying TruHoldem with horizontal WebSocket scaling using Redis Pub/Sub for cross-instance event broadcasting. The architecture supports multiple backend instances behind a load balancer, with all instances sharing game state through Redis.

## Architecture

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    │  (nginx/HAProxy)│
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   Backend 1   │   │   Backend 2   │   │   Backend 3   │
│  (WebSocket)  │   │  (WebSocket)  │   │  (WebSocket)  │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                    ┌───────▼───────┐
                    │     Redis     │
                    │   Pub/Sub     │
                    └───────────────┘
```

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- Redis 7.0+
- PostgreSQL 15+
- Kubernetes 1.25+ (for K8s deployment)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `WEBSOCKET_CLUSTER_ENABLED` | Enable WebSocket clustering | `false` |
| `SPRING_DATA_REDIS_HOST` | Redis hostname | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `HOSTNAME` | Instance identifier | Random UUID |

### Application Properties

```properties
# Enable cluster mode
app.websocket.cluster.enabled=${WEBSOCKET_CLUSTER_ENABLED:false}
app.websocket.cluster.instance-id=${HOSTNAME:${random.uuid}}

# Redis configuration
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=5000ms

# Connection pool
spring.data.redis.lettuce.pool.max-active=16
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2
```

## Docker Compose Deployment

### Production docker-compose.yml

```yaml
version: '3.8'

services:
  # Redis for Pub/Sub and session storage
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - truholdem-network

  # PostgreSQL database
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: truholdem
      POSTGRES_USER: ${DB_USER:-truholdem}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-truholdem}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-truholdem}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - truholdem-network

  # Backend instance 1
  backend-1:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/truholdem
      - SPRING_DATASOURCE_USERNAME=${DB_USER:-truholdem}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-truholdem}
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - WEBSOCKET_CLUSTER_ENABLED=true
      - HOSTNAME=backend-1
      - JWT_SECRET=${JWT_SECRET:-your-super-secret-jwt-key-min-32-chars}
    depends_on:
      redis:
        condition: service_healthy
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - truholdem-network

  # Backend instance 2
  backend-2:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/truholdem
      - SPRING_DATASOURCE_USERNAME=${DB_USER:-truholdem}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-truholdem}
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - WEBSOCKET_CLUSTER_ENABLED=true
      - HOSTNAME=backend-2
      - JWT_SECRET=${JWT_SECRET:-your-super-secret-jwt-key-min-32-chars}
    depends_on:
      redis:
        condition: service_healthy
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - truholdem-network

  # Backend instance 3
  backend-3:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/truholdem
      - SPRING_DATASOURCE_USERNAME=${DB_USER:-truholdem}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-truholdem}
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
      - WEBSOCKET_CLUSTER_ENABLED=true
      - HOSTNAME=backend-3
      - JWT_SECRET=${JWT_SECRET:-your-super-secret-jwt-key-min-32-chars}
    depends_on:
      redis:
        condition: service_healthy
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - truholdem-network

  # Nginx load balancer with WebSocket support
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - backend-1
      - backend-2
      - backend-3
    networks:
      - truholdem-network

  # Frontend
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    depends_on:
      - nginx
    networks:
      - truholdem-network

volumes:
  redis-data:
  postgres-data:

networks:
  truholdem-network:
    driver: bridge
```

### Nginx Configuration for WebSocket

```nginx
# nginx/nginx.conf
events {
    worker_connections 4096;
}

http {
    # Backend upstream with sticky sessions
    upstream backend {
        ip_hash;  # Sticky sessions for WebSocket
        server backend-1:8080 weight=1;
        server backend-2:8080 weight=1;
        server backend-3:8080 weight=1;
        
        keepalive 32;
    }

    # WebSocket upgrade mapping
    map $http_upgrade $connection_upgrade {
        default upgrade;
        '' close;
    }

    server {
        listen 80;
        server_name _;

        # API and WebSocket proxy
        location /api {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # WebSocket support
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
            
            # Timeouts for long-lived WebSocket connections
            proxy_read_timeout 3600s;
            proxy_send_timeout 3600s;
            proxy_connect_timeout 60s;
            
            # Buffering
            proxy_buffering off;
            proxy_cache off;
        }

        # Dedicated WebSocket endpoint
        location /api/ws {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            
            # Long timeouts for WebSocket
            proxy_read_timeout 86400s;
            proxy_send_timeout 86400s;
            
            # Disable buffering
            proxy_buffering off;
        }

        # Frontend static files
        location / {
            root /usr/share/nginx/html;
            try_files $uri $uri/ /index.html;
        }

        # Health check endpoint
        location /health {
            return 200 'OK';
            add_header Content-Type text/plain;
        }
    }
}
```

## Kubernetes Deployment

### Namespace and ConfigMap

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: truholdem

---
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: truholdem-config
  namespace: truholdem
data:
  SPRING_PROFILES_ACTIVE: "docker"
  WEBSOCKET_CLUSTER_ENABLED: "true"
  SPRING_DATA_REDIS_HOST: "redis-service"
  SPRING_DATA_REDIS_PORT: "6379"
```

### Redis Deployment

```yaml
# k8s/redis.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: truholdem
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          command: ["redis-server", "--appendonly", "yes"]
          ports:
            - containerPort: 6379
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "256Mi"
              cpu: "500m"
          livenessProbe:
            exec:
              command: ["redis-cli", "ping"]
            initialDelaySeconds: 10
            periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: truholdem
spec:
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
```

### Backend Deployment with HPA

```yaml
# k8s/backend.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: truholdem
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      containers:
        - name: backend
          image: truholdem/backend:latest
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: truholdem-config
            - secretRef:
                name: truholdem-secrets
          env:
            - name: HOSTNAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: truholdem
spec:
  selector:
    app: backend
  ports:
    - port: 8080
      targetPort: 8080

---
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: truholdem
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Pods
      pods:
        metric:
          name: websocket_sessions_active
        target:
          type: AverageValue
          averageValue: "100"
```

### Ingress with WebSocket Support

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: truholdem-ingress
  namespace: truholdem
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
    nginx.ingress.kubernetes.io/upstream-hash-by: "$remote_addr"
    nginx.ingress.kubernetes.io/affinity: "cookie"
    nginx.ingress.kubernetes.io/session-cookie-name: "SERVERID"
    nginx.ingress.kubernetes.io/session-cookie-max-age: "3600"
spec:
  rules:
    - host: truholdem.example.com
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: backend-service
                port:
                  number: 8080
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-service
                port:
                  number: 80
```

## Monitoring

### Key Metrics to Monitor

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `websocket_sessions_active` | Active WebSocket connections per instance | > 1000 |
| `websocket_cluster_events_published` | Events published to Redis | Rate < 1/min |
| `websocket_cluster_events_forwarded` | Events forwarded to local clients | - |
| `websocket_cluster_events_dropped` | Dropped events (echo/duplicate) | > 100/min |
| `redis_connected_clients` | Redis client connections | > 100 |
| `redis_used_memory_bytes` | Redis memory usage | > 80% |

### Prometheus Alerting Rules

```yaml
groups:
  - name: truholdem-websocket
    rules:
      - alert: HighWebSocketConnections
        expr: sum(websocket_sessions_active) > 5000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: High WebSocket connection count
          
      - alert: WebSocketClusterEventDropRate
        expr: rate(websocket_cluster_events_dropped[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: High WebSocket event drop rate
          
      - alert: RedisConnectionFailure
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: Redis connection failure
```

## Troubleshooting

### Common Issues

1. **WebSocket connections failing**
   - Check nginx WebSocket upgrade headers
   - Verify `proxy_read_timeout` is sufficient
   - Ensure sticky sessions are configured

2. **Events not propagating across instances**
   - Verify `WEBSOCKET_CLUSTER_ENABLED=true`
   - Check Redis connectivity from all instances
   - Review Redis Pub/Sub channel subscriptions

3. **High latency on game updates**
   - Monitor Redis latency metrics
   - Check network between instances and Redis
   - Review event deduplication performance

4. **Session lost on reconnection**
   - Verify session registry is syncing to Redis
   - Check session TTL configuration
   - Review reconnection handler logs

### Debug Commands

```bash
# Check Redis Pub/Sub channels
redis-cli PUBSUB CHANNELS "truholdem:*"

# Monitor Redis events in real-time
redis-cli SUBSCRIBE truholdem:game:events

# Check active sessions in Redis
redis-cli KEYS "ws:sessions:*"

# View session details
redis-cli HGETALL "ws:sessions:<session-id>"

# Check instance connections
redis-cli SMEMBERS "ws:instances:<instance-id>"
```

## Performance Tuning

### Redis Configuration

```
# redis.conf
maxclients 10000
maxmemory 1gb
maxmemory-policy allkeys-lru
tcp-keepalive 300
timeout 0

# Pub/Sub specific
client-output-buffer-limit pubsub 32mb 8mb 60
```

### JVM Options

```bash
JAVA_OPTS="-Xms512m -Xmx1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Dspring.data.redis.lettuce.pool.max-active=32"
```

## Security Considerations

1. **Redis Authentication**: Always use `requirepass` in production
2. **TLS**: Enable TLS for Redis connections in production
3. **Network Policies**: Restrict Redis access to backend pods only
4. **JWT Validation**: Ensure consistent JWT secret across all instances

## Rollback Procedure

1. Disable cluster mode: Set `WEBSOCKET_CLUSTER_ENABLED=false`
2. Restart backend instances
3. Clear Redis WebSocket data: `redis-cli KEYS "ws:*" | xargs redis-cli DEL`
4. Monitor for connection stability
