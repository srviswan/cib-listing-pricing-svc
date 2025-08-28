# Minimal Deployment Guide - CIB Listing & Pricing Service

## Overview

This guide explains how to run the CIB Listing & Pricing Service with minimal dependencies by disabling monitoring, tracing, and other optional components. This is useful for development, testing, or production environments where you want to minimize resource usage and external dependencies.

## 🚀 Quick Start - Minimal Mode

### 1. Database Only (Minimal)
```bash
# Start only the essential database
docker-compose up postgres-timescale

# Or use the minimal compose file
docker-compose -f docker-compose.service-only.yml up
```

### 2. Core Services (Recommended for Development)
```bash
# Start core services without monitoring
docker-compose --profile core up
```

### 3. Full Stack (Production)
```bash
# Start everything including monitoring
docker-compose up
```

## 📋 Service Dependencies Matrix

| Service | Required | Optional | Dependencies | Port |
|---------|----------|----------|--------------|------|
| **PostgreSQL** | ✅ **REQUIRED** | - | None | 5432 |
| **Publishing Service** | ✅ **REQUIRED** | - | PostgreSQL | 8083 |
| **Basket Core Service** | ✅ **REQUIRED** | - | PostgreSQL | 8081 |
| **Market Data Service** | ✅ **REQUIRED** | - | PostgreSQL | 8082 |
| **Redis** | ⚠️ **CONDITIONAL** | - | None | 6379 |
| **Kafka** | ❌ **OPTIONAL** | - | Zookeeper | 9092 |
| **Zookeeper** | ❌ **OPTIONAL** | - | None | 2181 |
| **Prometheus** | ❌ **OPTIONAL** | - | None | 9090 |
| **Grafana** | ❌ **OPTIONAL** | - | Prometheus | 3000 |
| **Jaeger** | ❌ **OPTIONAL** | - | None | 16686 |
| **Solace** | ❌ **OPTIONAL** | - | None | 55555 |

## 🔧 Configuration Options

### 1. Disable Prometheus Metrics

**File**: `publishing-service/src/main/resources/application.yml`

```yaml
# Before (with monitoring)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# After (without monitoring)
management:
  endpoints:
    web:
      exposure:
        include: health,info  # Remove metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: false  # Disable Prometheus
```

### 2. Disable Jaeger Tracing

**File**: `publishing-service/src/main/resources/application.yml`

```yaml
# Add this section to disable tracing
opentelemetry:
  traces:
    exporter: none  # Disable tracing
  metrics:
    exporter: none  # Disable metrics
```

### 3. Disable Detailed Logging

**File**: `publishing-service/src/main/resources/application.yml`

```yaml
# Before (verbose logging)
logging:
  level:
    com.custom.indexbasket.publishing: DEBUG
    com.custom.indexbasket.publishing.proxy: DEBUG
    com.custom.indexbasket.publishing.vendor: DEBUG
    org.springframework.r2dbc: DEBUG
    io.r2dbc.postgresql: DEBUG
    reactor.netty: DEBUG

# After (minimal logging)
logging:
  level:
    com.custom.indexbasket.publishing: INFO
    com.custom.indexbasket.publishing.proxy: WARN
    com.custom.indexbasket.publishing.vendor: WARN
    org.springframework.r2dbc: WARN
    io.r2dbc.postgresql: WARN
    reactor.netty: WARN
  pattern:
    console: "%d{HH:mm:ss} %-5level %logger{36} - %msg%n"
```

## 🐳 Docker Compose Profiles

### Profile: `core` (Recommended for Development)
```yaml
# docker-compose.core.yml
version: '3.8'
services:
  postgres-timescale:
    # ... (same as main file)
  
  redis:
    # ... (same as main file)
    profiles: []  # Always start
  
  publishing-service:
    build: ./publishing-service
    ports:
      - "8083:8083"
    depends_on:
      - postgres-timescale
      - redis
    environment:
      - SPRING_PROFILES_ACTIVE=core
      - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info
      - METRICS_EXPORT_PROMETHEUS_ENABLED=false
```

### Profile: `minimal` (Database Only)
```yaml
# docker-compose.minimal.yml
version: '3.8'
services:
  postgres-timescale:
    # ... (same as main file)
  
  publishing-service:
    build: ./publishing-service
    ports:
      - "8083:8083"
    depends_on:
      - postgres-timescale
    environment:
      - SPRING_PROFILES_ACTIVE=minimal
      - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health
      - METRICS_EXPORT_PROMETHEUS_ENABLED=false
      - LOGGING_LEVEL_ROOT=WARN
```

## 🚀 Running Commands

### 1. Minimal Mode (Database + Core Service)
```bash
# Start only essential services
docker-compose --profile core up -d

# Or use minimal compose file
docker-compose -f docker-compose.minimal.yml up -d
```

### 2. Development Mode (Core + Redis)
```bash
# Start core services with Redis
docker-compose --profile core up -d
docker-compose up redis -d
```

### 3. Production Mode (Everything)
```bash
# Start all services including monitoring
docker-compose up -d
```

## 📊 What Gets Disabled

### ❌ **Prometheus Metrics**
- Application metrics collection
- Custom business metrics
- Performance monitoring data
- **Impact**: No metrics dashboard, but service still works

### ❌ **Grafana Dashboards**
- Performance visualization
- Business metrics charts
- System health monitoring
- **Impact**: No visual monitoring, but logs still available

### ❌ **Jaeger Tracing**
- Distributed request tracing
- Performance bottleneck identification
- Request flow visualization
- **Impact**: Harder to debug distributed issues

### ❌ **Zookeeper**
- Kafka cluster coordination
- Service discovery (if using Kafka)
- **Impact**: Kafka won't work, but Redis can be used instead

### ❌ **Kafka**
- Event streaming
- Message queuing
- **Impact**: Events won't be streamed, but direct database operations still work

## ✅ What Still Works

### **Core Functionality**
- ✅ **Basket Core Service** (Port 8081): Basket lifecycle management, approval workflows, state machine
- ✅ **Market Data Service** (Port 8082): Market data, analytics, data quality management
- ✅ **Publishing Service** (Port 8083): Basket listing, price publishing, vendor integration
- ✅ Vendor health monitoring
- ✅ Database operations
- ✅ REST API endpoints
- ✅ Scheduled tasks
- ✅ Event publishing and messaging

### **Essential Monitoring**
- ✅ Health check endpoints (`/actuator/health`)
- ✅ Basic application info (`/actuator/info`)
- ✅ Application logs (console)
- ✅ Database connectivity

### **Performance Features**
- ✅ Connection pooling
- ✅ Caching (if Redis is enabled)
- ✅ Circuit breakers
- ✅ Retry mechanisms

## 🔍 Health Check Endpoints

### Available in Minimal Mode
```bash
# Basket Core Service Health
curl http://localhost:8081/actuator/health

# Market Data Service Health
curl http://localhost:8082/actuator/health

# Publishing Service Health
curl http://localhost:8083/actuator/health

# Publishing Service API Health
curl http://localhost:8083/api/v1/publishing/health

# Vendor Health
curl http://localhost:8083/api/v1/publishing/vendors/health

# Basket Statistics
curl http://localhost:8083/api/v1/publishing/baskets/statistics
```

### Not Available in Minimal Mode
```bash
# Metrics endpoint (disabled)
curl http://localhost:8083/actuator/metrics  # 404

# Prometheus endpoint (disabled)
curl http://localhost:8083/actuator/prometheus  # 404
```

## 🛠️ Troubleshooting

### 1. Service Won't Start
```bash
# Check if database is running
docker-compose ps postgres-timescale

# Check service logs
docker-compose logs basket-core-service
docker-compose logs market-data-service
docker-compose logs publishing-service

# Verify database connectivity
docker exec -it basket-timescaledb psql -U basket_user -d basket_platform
```

### 2. Missing Dependencies
```bash
# Check what's running
docker-compose ps

# Start missing services
docker-compose up -d postgres-timescale

# Check all service health
curl http://localhost:8081/actuator/health  # Basket Core
curl http://localhost:8082/actuator/health  # Market Data
curl http://localhost:8083/actuator/health  # Publishing
```

### 3. Performance Issues
```bash
# Check database performance
docker exec -it basket-timescaledb psql -U basket_user -d basket_platform -c "SELECT * FROM pg_stat_activity;"

# Check service metrics (if enabled)
curl http://localhost:8083/actuator/metrics

# Check logs for errors
docker-compose logs -f publishing-service
```

## 📈 Resource Usage Comparison

| Mode | Memory | CPU | Disk | Network |
|------|--------|-----|------|---------|
| **Minimal** | ~512MB | ~0.5 cores | ~1GB | Low |
| **Core** | ~1GB | ~1 core | ~2GB | Medium |
| **Full** | ~2GB | ~2 cores | ~5GB | High |

## 🎯 Recommendations

### **Development Environment**
```bash
# Use core profile
docker-compose --profile core up -d
```
- Includes database, Redis, and core services
- Good balance of functionality and resources
- Easy to debug and develop

### **Testing Environment**
```bash
# Use minimal profile
docker-compose --profile minimal up -d
```
- Database only
- Fast startup
- Minimal resource usage

### **Production Environment**
```bash
# Use full stack
docker-compose up -d
```
- Complete monitoring and tracing
- Production-grade observability
- Higher resource usage

## 🔄 Switching Between Modes

### From Minimal to Full
```bash
# Stop minimal services
docker-compose --profile minimal down

# Start full stack
docker-compose up -d
```

### From Full to Minimal
```bash
# Stop all services
docker-compose down

# Start minimal services
docker-compose --profile minimal up -d
```

## 📝 Summary

The CIB Listing & Pricing Service can run in multiple modes:

1. **Minimal Mode**: Database + Core Service (fastest, least resources)
2. **Core Mode**: Database + Redis + Core Services (recommended for development)
3. **Full Mode**: Everything including monitoring (production-ready)

Choose the mode based on your needs:
- **Development**: Use Core mode
- **Testing**: Use Minimal mode  
- **Production**: Use Full mode

All core functionality works in every mode - only monitoring, tracing, and advanced features are disabled in minimal modes.
