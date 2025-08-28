# CIB Listing & Pricing Service

## 🚀 Quick Start

### **Choose Your Deployment Mode:**

| Mode | Use Case | Resources | Command |
|------|----------|-----------|---------|
| **Minimal** | Testing, CI/CD | ~512MB RAM | `./start-minimal.sh` |
| **Core** | Development | ~1GB RAM | `./start-core.sh` |
| **Full** | Production | ~2GB RAM | `./start-full.sh` |

### **One-Command Deployment:**

```bash
# Minimal mode (fastest, least resources)
./start-minimal.sh

# Core mode (recommended for development)
./start-core.sh

# Full mode (production with monitoring)
./start-full.sh
```

## 📋 What Each Mode Includes

### **Minimal Mode** (`./start-minimal.sh`)
- ✅ PostgreSQL Database
- ✅ **Basket Core Service** (8081) - Basket lifecycle management
- ✅ **Market Data Service** (8082) - Market data & analytics
- ✅ **Publishing Service** (8083) - Basket listing & price publishing
- ❌ No Redis
- ❌ No Monitoring
- ❌ No Tracing

### **Core Mode** (`./start-core.sh`)
- ✅ PostgreSQL Database
- ✅ Redis Cache
- ✅ **Basket Core Service** (8081) - Basket lifecycle management
- ✅ **Market Data Service** (8082) - Market data & analytics
- ✅ **Publishing Service** (8083) - Basket listing & price publishing
- ❌ No Monitoring
- ❌ No Tracing

### **Full Mode** (`./start-full.sh`)
- ✅ PostgreSQL Database
- ✅ Redis Cache
- ✅ **Basket Core Service** (8081) - Basket lifecycle management
- ✅ **Market Data Service** (8082) - Market data & analytics
- ✅ **Publishing Service** (8083) - Basket listing & price publishing
- ✅ Prometheus Metrics (9090)
- ✅ Grafana Dashboards (3000)
- ✅ Jaeger Tracing (16686)

## 🔍 Health Checks

```bash
# Basket Core Service (Basket lifecycle management)
curl http://localhost:8081/actuator/health

# Market Data Service (Market data & analytics)
curl http://localhost:8082/actuator/health

# Publishing Service (Basket listing & price publishing)
curl http://localhost:8083/actuator/health

# Publishing Service API Health
curl http://localhost:8083/api/v1/publishing/health

# Vendor Health
curl http://localhost:8083/api/v1/publishing/vendors/health

# Basket Statistics
curl http://localhost:8083/api/v1/publishing/baskets/statistics
```

## 📚 Documentation

- **[Minimal Deployment Guide](minimal-deployment-guide.md)** - Complete guide for running with minimal dependencies
- **[Deployment Quick Reference](deployment-quick-reference.md)** - Quick commands and troubleshooting
- **[Implementation Plan](implementation-plan.md)** - Overall project roadmap

## 🛑 Stopping Services

```bash
# Stop minimal services
docker-compose -f docker-compose.minimal.yml down

# Stop core services
docker-compose -f docker-compose.core.yml down

# Stop all services
docker-compose down
```

## 🎯 Use Cases

- **Development**: Use Core mode for full functionality with caching
- **Testing**: Use Minimal mode for fast startup and minimal resources
- **Production**: Use Full mode for complete monitoring and observability

## 📊 Performance

All modes provide the same core functionality:
- ✅ Basket listing and management
- ✅ Real-time price publishing
- ✅ Vendor integration
- ✅ Database operations
- ✅ REST API endpoints

Only monitoring, tracing, and advanced features are disabled in minimal modes.
