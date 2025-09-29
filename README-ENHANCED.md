# CIB Listing & Pricing Service - Enhanced with SMA & FIX Integration

## Overview

This enhanced version of the CIB Listing & Pricing Service now includes **SMA Refinitiv API** integration for price data retrieval and **FIX protocol** integration for Bloomberg communication, implemented as non-invasive middleware services.

## Architecture

### Service Architecture
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

🆕 NEW INTEGRATION LAYER:
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   SMA Adapter       │    │   FIX Adapter       │    │   Integration       │
│   Service           │    │   Service           │    │   Manager           │
│   Port: 8084       │    │   Port: 8085        │    │   Port: 8086        │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
         │                          │                          │
         │                          │                          │
         ▼                          ▼                          │
┌─────────────────────┐    ┌─────────────────────┐            │
│   SMA Refinitiv     │    │   Bloomberg         │            │
│   API               │    │   FixNet            │            │
└─────────────────────┘    └─────────────────────┘            │
                                                              │
                    ┌─────────────────────────────────────────┘
                    │
                    ▼
            ┌─────────────────────┐
            │   Redis Cache       │
            │   Port: 6379        │
            └─────────────────────┘
```

## New Services

### 1. SMA Adapter Service (Port 8084)
- **Purpose**: Bridge between Refinitiv SMA API and existing Market Data Service
- **Features**:
  - Real-time price data subscription via Refinitiv Real-Time SDK
  - Historical data retrieval
  - Caching and performance optimization
  - Rate limiting and circuit breaker patterns
- **API Endpoints**:
  - `GET /api/v1/sma/prices/{symbol}` - Get current price
  - `POST /api/v1/sma/prices/batch` - Batch price retrieval
  - `GET /api/v1/sma/health` - Health check

### 2. FIX Adapter Service (Port 8085)
- **Purpose**: Handle FIX protocol communication with Bloomberg FixNet
- **Features**:
  - FIX 4.4/5.0 protocol support
  - Session management and automatic reconnection
  - Market data publishing and order management
  - Message validation and error handling
- **API Endpoints**:
  - `POST /api/v1/fix/publish/price` - Publish price via FIX
  - `POST /api/v1/fix/publish/basket` - Publish basket listing
  - `GET /api/v1/fix/health` - Health check

### 3. Integration Manager Service (Port 8086)
- **Purpose**: Orchestrate data flow between SMA and FIX adapters
- **Features**:
  - Basket price calculation from constituent prices
  - Real-time publishing coordination
  - Data validation and reconciliation
  - End-to-end monitoring
- **API Endpoints**:
  - `POST /api/v1/integration/baskets/{id}/calculate-publish` - Calculate and publish basket price
  - `GET /api/v1/integration/health` - Overall integration health
  - `POST /api/v1/integration/baskets/publish-all` - Publish all active baskets

## Data Flow

### Real-time Price Flow
```
SMA Refinitiv API → SMA Adapter → Redis Cache → Market Data Service
Market Data Service → Integration Manager → FIX Adapter → Bloomberg FixNet
```

### Batch Publishing Flow
```
Integration Manager → SMA Adapter (Batch Prices) → Calculate Basket Price → FIX Adapter → Bloomberg
```

## Quick Start

### 1. Start Enhanced Services
```bash
# Make script executable (already done)
chmod +x start-enhanced.sh

# Start all services
./start-enhanced.sh
```

### 2. Verify Services
```bash
# Check all services are running
docker-compose -f docker-compose.enhanced.yml ps

# Test SMA integration
curl http://localhost:8084/sma-adapter/api/v1/sma/prices/AAPL

# Test FIX integration
curl http://localhost:8085/fix-adapter/api/v1/fix/health

# Test integration manager
curl http://localhost:8086/integration-manager/api/v1/integration/health
```

### 3. Test End-to-End Flow
```bash
# Calculate and publish basket price
curl -X POST http://localhost:8086/integration-manager/api/v1/integration/baskets/TEST_BASKET/calculate-publish

# Publish all active basket prices
curl -X POST http://localhost:8086/integration-manager/api/v1/integration/baskets/publish-all
```

## Configuration

### Environment Variables
```bash
# SMA Refinitiv Configuration
export SMA_API_URL="https://api.refinitiv.com"
export SMA_APP_KEY="your-app-key"
export SMA_USERNAME="your-username"
export SMA_PASSWORD="your-password"

# Bloomberg FIX Configuration
export BLOOMBERG_FIX_HOST="fix.bloomberg.com"
export BLOOMBERG_FIX_PORT="8080"
export FIX_SENDER_COMP_ID="YOUR_COMP_ID"
export FIX_TARGET_COMP_ID="BLOOMBERG"
export BLOOMBERG_USERNAME="your-username"
export BLOOMBERG_PASSWORD="your-password"
```

### Application Configuration
Each service has its own configuration file:
- `sma-adapter-service/src/main/resources/application.yml`
- `fix-adapter-service/src/main/resources/application.yml`
- `integration-manager-service/src/main/resources/application.yml`

## Key Features

### ✅ Non-Invasive Integration
- No changes to existing services
- Preserves current functionality
- Maintains backward compatibility

### ✅ Scalable Architecture
- Independent service scaling
- Load balancing capabilities
- Horizontal scaling support

### ✅ Fault Tolerance
- Circuit breaker patterns
- Retry mechanisms
- Graceful degradation

### ✅ Comprehensive Monitoring
- Health checks for all services
- Performance metrics
- Real-time alerting

### ✅ Production Ready
- Security with OAuth2/JWT
- Comprehensive logging
- Docker containerization

## API Documentation

### SMA Adapter Service
```bash
# Get current price
curl http://localhost:8084/sma-adapter/api/v1/sma/prices/AAPL

# Get batch prices
curl -X POST http://localhost:8084/sma-adapter/api/v1/sma/prices/batch \
  -H "Content-Type: application/json" \
  -d '["AAPL", "MSFT", "GOOGL"]'

# Health check
curl http://localhost:8084/sma-adapter/api/v1/sma/health
```

### FIX Adapter Service
```bash
# Publish price via FIX
curl -X POST http://localhost:8085/fix-adapter/api/v1/fix/publish/price \
  -H "Content-Type: application/json" \
  -d '{
    "basketId": "TEST_BASKET",
    "symbol": "AAPL",
    "price": 150.50,
    "currency": "USD",
    "timestamp": "2024-01-01T10:00:00Z"
  }'

# Health check
curl http://localhost:8085/fix-adapter/api/v1/fix/health
```

### Integration Manager Service
```bash
# Calculate and publish basket price
curl -X POST http://localhost:8086/integration-manager/api/v1/integration/baskets/TEST_BASKET/calculate-publish

# Get integration health
curl http://localhost:8086/integration-manager/api/v1/integration/health

# Publish all active baskets
curl -X POST http://localhost:8086/integration-manager/api/v1/integration/baskets/publish-all
```

## Monitoring & Health Checks

### Service Health
```bash
# All services
curl http://localhost:8081/actuator/health  # Basket Core
curl http://localhost:8082/actuator/health  # Market Data
curl http://localhost:8083/actuator/health  # Publishing
curl http://localhost:8084/sma-adapter/actuator/health  # SMA Adapter
curl http://localhost:8085/fix-adapter/actuator/health  # FIX Adapter
curl http://localhost:8086/integration-manager/actuator/health  # Integration Manager
```

### Integration Health
```bash
# Overall integration status
curl http://localhost:8086/integration-manager/api/v1/integration/health
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8084/sma-adapter/actuator/prometheus
curl http://localhost:8085/fix-adapter/actuator/prometheus
curl http://localhost:8086/integration-manager/actuator/prometheus
```

## Troubleshooting

### Service Won't Start
```bash
# Check service logs
docker-compose -f docker-compose.enhanced.yml logs sma-adapter-service
docker-compose -f docker-compose.enhanced.yml logs fix-adapter-service
docker-compose -f docker-compose.enhanced.yml logs integration-manager-service
```

### Integration Issues
```bash
# Check service connectivity
curl http://localhost:8084/sma-adapter/api/v1/sma/health
curl http://localhost:8085/fix-adapter/api/v1/fix/health
curl http://localhost:8086/integration-manager/api/v1/integration/health
```

### Database Issues
```bash
# Check database connectivity
docker exec -it basket-timescaledb pg_isready -U basket_user -d basket_platform

# Check Redis connectivity
docker exec -it basket-redis redis-cli ping
```

## Development

### Building Services
```bash
# Build all services
mvn clean package -DskipTests

# Build individual services
cd sma-adapter-service && mvn clean package
cd fix-adapter-service && mvn clean package
cd integration-manager-service && mvn clean package
```

### Running Tests
```bash
# Run all tests
mvn test

# Run integration tests
mvn test -Dtest=*IntegrationTest
```

## Production Deployment

### Security Considerations
- Configure proper authentication tokens
- Set up SSL/TLS certificates
- Configure firewall rules
- Enable audit logging

### Performance Tuning
- Adjust connection pool sizes
- Configure caching strategies
- Set up load balancing
- Monitor resource usage

### Monitoring Setup
- Configure Prometheus scraping
- Set up Grafana dashboards
- Configure alerting rules
- Set up log aggregation

## Support

For issues or questions:
1. Check the health endpoints
2. Review service logs
3. Check the integration status
4. Refer to the architecture documentation

## License

This project is licensed under the MIT License - see the LICENSE file for details.
