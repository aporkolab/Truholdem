# TruHoldem Monitoring Stack

Enterprise-grade observability stack with Prometheus, Grafana, and Jaeger.

## 📊 Dashboards

### 1. Game Metrics (`game-metrics.json`)
- **Active Games** - Real-time count of ongoing games
- **Games Started (24h)** - Daily game creation rate
- **Hands Per Hour** - Game throughput timeseries
- **Hand Duration Distribution** - P50/P95/P99 percentiles
- **Player Actions by Type** - Pie chart (fold/call/raise/check)
- **Average Pot Size** - Timeseries trend
- **Bot Decision Time** - AI performance monitoring
- **Bot vs Human Actions** - Distribution breakdown

### 2. Tournament Metrics (`tournament-metrics.json`)
- **Active Tournaments** - Running tournament count
- **Tournament Players** - Registration vs remaining
- **Eliminations Per Hour** - Elimination rate
- **Tournament Completion Rate** - Success percentage
- **Tournament Types Distribution** - Type breakdown

### 3. System Health (`system-health.json`)
- **JVM Heap Usage** - Memory utilization with thresholds
- **HTTP Request Rate** - Traffic patterns
- **HTTP Response Time P99** - Latency monitoring
- **Database Connection Pool** - HikariCP metrics
- **Redis Cache Hit Rate** - Cache efficiency
- **WebSocket Connections** - Real-time connection count
- **Error Rate (4xx/5xx)** - Application health

## 🚨 Alerting Rules

### Critical Alerts (Immediate Response)
| Alert | Threshold | Description |
|-------|-----------|-------------|
| `VeryHighErrorRate` | >10% 5xx | Server failures requiring immediate action |
| `CriticalMemoryUsage` | >90% heap | OOMKill imminent |
| `DatabaseConnectionPoolExhausted` | >5 pending | Query failures occurring |
| `ServiceDown` | Health check fails | Backend is DOWN |

### Warning Alerts (Investigation Needed)
| Alert | Threshold | Description |
|-------|-----------|-------------|
| `HighErrorRate` | >5% 5xx | Elevated server errors |
| `SlowResponseTime` | P99 > 2s | Performance degradation |
| `HighMemoryUsage` | >80% heap | Memory pressure building |
| `LowCacheHitRate` | <70% hits | Inefficient caching |

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐
│  TruHoldem API  │────▶│   Prometheus    │
│  /actuator/     │     │   (scraping)    │
│  prometheus     │     └────────┬────────┘
└─────────────────┘              │
                                 ▼
┌─────────────────┐     ┌─────────────────┐
│     Jaeger      │◀────│    Grafana      │
│   (tracing)     │     │  (dashboards)   │
└─────────────────┘     └─────────────────┘
```

## 🚀 Quick Start

```bash
# Start the full stack
docker-compose up -d

# Access dashboards
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
# Jaeger: http://localhost:16686
```

## 📁 File Structure

```
docker/
├── grafana/
│   ├── dashboards/
│   │   ├── game-metrics.json
│   │   ├── tournament-metrics.json
│   │   └── system-health.json
│   └── provisioning/
│       ├── dashboards/
│       │   └── dashboards.yml
│       └── datasources/
│           └── datasources.yml
└── prometheus/
    └── alerts.yml

monitoring/
└── prometheus.yml
```

## 🔧 Customization

### Adding New Alerts
Edit `docker/prometheus/alerts.yml` and add new rules under the appropriate group.

### Adding New Dashboards
1. Create dashboard JSON in `docker/grafana/dashboards/`
2. Grafana will auto-load within 30 seconds

### Modifying Scrape Intervals
Edit `monitoring/prometheus.yml` to adjust scrape frequencies.

## 📈 Key Metrics Reference

### Game Metrics (from `GameMetrics.java`)
- `truholdem_games_active` - Current active games
- `truholdem_games_started_total` - Total games started
- `truholdem_hands_played_total` - Total hands played
- `truholdem_player_actions_total{action_type, is_bot}` - Player actions
- `truholdem_bot_decision_time_bucket` - Bot AI decision latency
- `truholdem_pot_size_bucket` - Pot size distribution
- `truholdem_hand_duration_bucket` - Hand duration histogram

### Spring Boot Actuator Metrics
- `http_server_requests_seconds_*` - HTTP request metrics
- `jvm_memory_*` - JVM memory utilization
- `hikaricp_connections_*` - Database connection pool
- `cache_gets_total{result}` - Redis cache operations
- `process_cpu_usage` - CPU utilization

## 🔐 Security Notes

- Change default Grafana password in production
- Consider enabling Prometheus authentication
- Use network policies to restrict metrics access
- Rotate credentials regularly

## 📞 Support

For issues with the monitoring stack:
1. Check container logs: `docker-compose logs grafana prometheus`
2. Verify Prometheus targets: http://localhost:9090/targets
3. Check Grafana datasource connections
