# 🏗️ CIB Listing & Pricing Service - Complete Service Overview

## 📋 Service Architecture

The CIB Listing & Pricing Service consists of **3 core application services** that work together to provide a complete basket management platform:

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   Basket Core       │    │   Market Data       │    │   Publishing        │
│   Service           │    │   Service           │    │   Service           │
│   Port: 8081       │    │   Port: 8082        │    │   Port: 8083        │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
         │                          │                          │
         │                          │                          │
         └──────────────────────────┼──────────────────────────┘
                                    │
                    ┌─────────────────────────────────┐
                    │        PostgreSQL               │
                    │      TimescaleDB                │
                    │        Port: 5432               │
                    └─────────────────────────────────┘
```

## 🎯 Service Responsibilities

### **1. Basket Core Service (Port 8081)**

**Purpose**: Basket lifecycle management and approval workflows

**Key Features**:
- ✅ **Basket Creation**: Create new investment baskets
- ✅ **Lifecycle Management**: State machine for basket approval process
- ✅ **Approval Workflows**: Single/dual approver workflows
- ✅ **Backtesting**: Historical performance analysis
- ✅ **State Transitions**: DRAFT → BACKTESTING → APPROVED → ACTIVE
- ✅ **Validation**: Business rule validation and compliance checks

**Main Endpoints**:
```bash
# Basket Management
POST   /api/v1/baskets                    # Create basket
GET    /api/v1/baskets                    # List baskets
GET    /api/v1/baskets/{id}              # Get basket details
PUT    /api/v1/baskets/{id}              # Update basket
DELETE /api/v1/baskets/{id}              # Delete basket

# Approval Workflows
POST   /api/v1/baskets/{id}/approve      # Approve basket
POST   /api/v1/baskets/{id}/reject       # Reject basket
POST   /api/v1/baskets/{id}/backtest     # Start backtesting

# Health & Monitoring
GET    /actuator/health                   # Service health
GET    /actuator/info                     # Service information
```

**Technologies**:
- Spring Boot 3.3.5
- Spring State Machine
- Akka Actors
- R2DBC (PostgreSQL)
- Reactive Programming

---

### **2. Market Data Service (Port 8082)**

**Purpose**: Market data processing, analytics, and data quality management

**Key Features**:
- ✅ **Real-time Market Data**: Live price feeds from multiple sources
- ✅ **Historical Data**: TimescaleDB for time-series market data
- ✅ **Data Quality Management**: Validation, scoring, and monitoring
- ✅ **Proxy Services**: Bloomberg, Refinitiv, Yahoo Finance integration
- ✅ **Analytics**: Performance analysis and reporting
- ✅ **Caching**: Redis-based data caching for performance

**Main Endpoints**:
```bash
# Market Data
GET    /api/v1/market-data/{symbol}      # Get current price
GET    /api/v1/market-data/{symbol}/history # Get price history
POST   /api/v1/market-data/batch         # Batch price updates

# Data Quality
GET    /api/v1/data-quality/report       # Data quality report
GET    /api/v1/data-quality/score        # Quality score
GET    /api/v1/data-quality/issues       # Quality issues

# Proxy Services
GET    /api/v1/proxy/health              # Proxy service health
GET    /api/v1/proxy/status              # Proxy service status

# Health & Monitoring
GET    /actuator/health                   # Service health
GET    /actuator/info                     # Service information
```

**Technologies**:
- Spring Boot 3.3.5
- TimescaleDB (PostgreSQL extension)
- Redis caching
- Circuit breaker patterns
- Data validation frameworks

---

### **3. Publishing Service (Port 8083)**

**Purpose**: Basket listing and real-time price publishing to vendor platforms

**Key Features**:
- ✅ **Basket Listing**: Publish baskets to vendor platforms
- ✅ **Price Publishing**: Real-time price updates every 5 seconds
- ✅ **Vendor Integration**: Bloomberg, Refinitiv, generic vendors
- ✅ **Event Publishing**: Asynchronous event messaging
- ✅ **Scheduled Tasks**: Automated price publishing cycles
- ✅ **Vendor Health Monitoring**: Real-time vendor status

**Main Endpoints**:
```bash
# Basket Listing
POST   /api/v1/publishing/basket/{id}/list    # List basket on vendors
POST   /api/v1/publishing/basket/{id}/retry   # Retry failed listing

# Price Publishing
POST   /api/v1/publishing/basket/{id}/price   # Publish price to vendors
POST   /api/v1/publishing/baskets/publish-prices # Publish all approved baskets

# Vendor Management
GET    /api/v1/publishing/vendors/health       # Vendor health status
POST   /api/v1/publishing/basket/{id}/price/{vendor} # Publish to specific vendor

# Basket Statistics
GET    /api/v1/publishing/baskets/statistics   # Basket statistics

# Health & Monitoring
GET    /actuator/health                         # Service health
GET    /api/v1/publishing/health                # API health
```

**Technologies**:
- Spring Boot 3.3.5
- Scheduled tasks
- Event publishing
- Vendor proxy services
- Reactive programming

---

## 🔄 Service Interactions

### **Data Flow**:
```
1. Basket Core Service creates/updates basket
   ↓
2. Market Data Service provides pricing data
   ↓
3. Publishing Service publishes to vendor platforms
   ↓
4. Events published back to other services
```

### **Dependencies**:
- **All Services** → **PostgreSQL Database**
- **Market Data Service** → **Redis Cache** (Core/Full mode)
- **Publishing Service** → **Basket Core Service** (for basket data)
- **Publishing Service** → **Market Data Service** (for pricing data)

## 📊 Service Ports & URLs

| Service | Port | Internal URL | External URL | Purpose |
|---------|------|--------------|--------------|---------|
| **Basket Core** | 8081 | `http://basket-core-service:8081` | `http://localhost:8081` | Basket lifecycle management |
| **Market Data** | 8082 | `http://market-data-service:8082` | `http://localhost:8082` | Market data & analytics |
| **Publishing** | 8083 | `http://publishing-service:8083` | `http://localhost:8083` | Basket listing & pricing |
| **Database** | 5432 | `postgresql://postgres-timescale:5432` | `postgresql://localhost:5432` | PostgreSQL with TimescaleDB |
| **Redis** | 6379 | `redis://basket-redis:6379` | `redis://localhost:6379` | Caching (Core/Full mode) |

## 🚀 Deployment Modes

### **Minimal Mode** (Testing, CI/CD)
- ✅ All 3 application services
- ✅ PostgreSQL database
- ❌ No Redis
- ❌ No monitoring/tracing

### **Core Mode** (Development)
- ✅ All 3 application services
- ✅ PostgreSQL database
- ✅ Redis cache
- ❌ No monitoring/tracing

### **Full Mode** (Production)
- ✅ All 3 application services
- ✅ PostgreSQL database
- ✅ Redis cache
- ✅ Prometheus metrics
- ✅ Grafana dashboards
- ✅ Jaeger tracing

## 🔍 Health Check Commands

```bash
# Check all services health
echo "🔍 Checking all services health..."

echo "📊 Basket Core Service (8081):"
curl -s http://localhost:8081/actuator/health | jq .

echo "📊 Market Data Service (8082):"
curl -s http://localhost:8082/actuator/health | jq .

echo "📊 Publishing Service (8083):"
curl -s http://localhost:8083/actuator/health | jq .

echo "📊 Database:"
docker exec -it basket-timescaledb pg_isready -U basket_user -d basket_platform

echo "📊 Redis (Core/Full mode only):"
docker exec -it basket-redis redis-cli ping
```

## 🛠️ Troubleshooting

### **Service Won't Start**:
```bash
# Check service logs
docker-compose logs basket-core-service
docker-compose logs market-data-service
docker-compose logs publishing-service

# Check database connectivity
docker exec -it basket-timescaledb psql -U basket_user -d basket_platform -c "SELECT version();"
```

### **Service Communication Issues**:
```bash
# Check if services can reach each other
docker exec -it basket-core-service curl -s http://market-data-service:8082/actuator/health
docker exec -it publishing-service curl -s http://basket-core-service:8081/actuator/health
```

### **Database Issues**:
```bash
# Check database status
docker-compose ps postgres-timescale

# Check database logs
docker-compose logs postgres-timescale

# Verify database connectivity from services
docker exec -it basket-core-service curl -s http://localhost:8081/actuator/health
```

## 📚 Related Documentation

- **[Minimal Deployment Guide](minimal-deployment-guide.md)** - Complete deployment guide
- **[Deployment Quick Reference](deployment-quick-reference.md)** - Quick commands
- **[Implementation Plan](implementation-plan.md)** - Overall project roadmap

---

## 🎯 Summary

The CIB Listing & Pricing Service provides a **complete basket management platform** with:

- **3 fully functional application services**
- **Comprehensive basket lifecycle management**
- **Real-time market data processing**
- **Automated vendor publishing**
- **Multiple deployment modes** for different environments
- **100% core functionality** in all modes

All services are designed to work together seamlessly, providing a robust foundation for investment basket management operations.
