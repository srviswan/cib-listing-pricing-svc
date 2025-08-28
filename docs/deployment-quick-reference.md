# 🚀 Deployment Quick Reference

## 📋 Quick Commands

### **Minimal Mode** (Database + Core Services)
```bash
# Start minimal services
docker-compose -f docker-compose.minimal.yml up -d

# Check status
docker-compose -f docker-compose.minimal.yml ps

# View logs
docker-compose -f docker-compose.minimal.yml logs -f
```

### **Core Mode** (Database + Redis + Core Services)
```bash
# Start core services
docker-compose -f docker-compose.core.yml up -d

# Check status
docker-compose -f docker-compose.core.yml ps

# View logs
docker-compose -f docker-compose.core.yml logs -f
```

### **Full Mode** (Everything including monitoring)
```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

## 🔍 Health Check Commands

```bash
# Database health
docker exec -it basket-timescaledb pg_isready -U basket_user -d basket_platform

# Basket Core Service health (Basket lifecycle management)
curl http://localhost:8081/actuator/health

# Market Data Service health (Market data & analytics)
curl http://localhost:8082/actuator/health

# Publishing Service health (Basket listing & price publishing)
curl http://localhost:8083/actuator/health

# Redis health (Core mode only)
docker exec -it basket-redis redis-cli ping
```

## 🛑 Stop Commands

```bash
# Stop minimal services
docker-compose -f docker-compose.minimal.yml down

# Stop core services
docker-compose -f docker-compose.core.yml down

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## 📊 Port Mapping

| Service | Port | Purpose |
|---------|------|---------|
| **PostgreSQL** | 5432 | Database |
| **Publishing Service** | 8083 | Basket listing & price publishing |
| **Basket Core Service** | 8081 | Basket management |
| **Market Data Service** | 8082 | Market data & analytics |
| **Redis** | 6379 | Caching |
| **Prometheus** | 9090 | Metrics (full mode only) |
| **Grafana** | 3000 | Dashboards (full mode only) |
| **Jaeger** | 16686 | Tracing (full mode only) |

## 🎯 Use Cases

| Mode | Use Case | Resources | Features |
|------|----------|-----------|----------|
| **Minimal** | Testing, CI/CD | ~512MB RAM | Core functionality only |
| **Core** | Development | ~1GB RAM | + Redis caching |
| **Full** | Production | ~2GB RAM | + Monitoring + Tracing |

## ⚡ Quick Start Scripts

### **start-minimal.sh**
```bash
#!/bin/bash
echo "🚀 Starting minimal services..."
docker-compose -f docker-compose.minimal.yml up -d
echo "✅ Minimal services started!"
echo "📊 Health check: curl http://localhost:8083/actuator/health"
```

### **start-core.sh**
```bash
#!/bin/bash
echo "🚀 Starting core services..."
docker-compose -f docker-compose.core.yml up -d
echo "✅ Core services started!"
echo "📊 Health check: curl http://localhost:8083/actuator/health"
```

### **start-full.sh**
```bash
#!/bin/bash
echo "🚀 Starting full stack..."
docker-compose up -d
echo "✅ Full stack started!"
echo "📊 Health check: curl http://localhost:8083/actuator/health"
echo "📈 Grafana: http://localhost:3000 (admin/admin)"
echo "🔍 Jaeger: http://localhost:16686"
```

## 🚨 Troubleshooting

### **Service won't start?**
```bash
# Check logs
docker-compose logs [service-name]

# Check if database is ready
docker exec -it basket-timescaledb pg_isready -U basket_user -d basket_platform

# Restart service
docker-compose restart [service-name]
```

### **Port already in use?**
```bash
# Find what's using the port
lsof -i :8083

# Kill the process
kill -9 [PID]

# Or change port in docker-compose file
```

### **Database connection issues?**
```bash
# Check database status
docker-compose ps postgres-timescale

# Check database logs
docker-compose logs postgres-timescale

# Reset database (WARNING: loses data)
docker-compose down -v
docker-compose up postgres-timescale -d
```

## 📝 Environment Variables

### **Minimal Mode**
```bash
SPRING_PROFILES_ACTIVE=minimal
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health
METRICS_EXPORT_PROMETHEUS_ENABLED=false
LOGGING_LEVEL_ROOT=WARN
```

### **Core Mode**
```bash
SPRING_PROFILES_ACTIVE=core
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
METRICS_EXPORT_PROMETHEUS_ENABLED=false
LOGGING_LEVEL_ROOT=INFO
```

### **Full Mode**
```bash
SPRING_PROFILES_ACTIVE=default
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
METRICS_EXPORT_PROMETHEUS_ENABLED=true
LOGGING_LEVEL_ROOT=INFO
```
