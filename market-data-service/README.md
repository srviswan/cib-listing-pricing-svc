# Market Data Service - Development Environment

## 🎯 **Overview**

The Market Data Service is a core component of the Custom Index Basket Management Platform that provides real-time market data, comprehensive analytics, and risk metrics. This service implements a sophisticated proxy architecture for handling multiple external data sources with intelligent caching, quality management, and performance optimization.

## 🏗️ **Architecture**

### **Core Components**
- **Proxy Services Layer**: Abstracts external market data providers
- **Data Quality Service**: Validates and scores data quality
- **Caching Service**: Intelligent Redis-based caching with quality-aware TTL
- **Circuit Breaker**: Resilient external API calls with automatic recovery
- **Rate Limiting**: Prevents API quota exhaustion
- **Metrics Collection**: Comprehensive performance monitoring

### **Data Sources**
- **Bloomberg**: Primary data source for institutional-grade data
- **Reuters**: Secondary source for news and market data
- **Yahoo Finance**: Backup source for public market data
- **Custom Feeds**: Extensible framework for additional sources

## 🚀 **Getting Started**

### **Prerequisites**
- Java 23
- Maven 3.8+
- PostgreSQL 14+ with TimescaleDB extension
- Redis 6+
- Docker & Docker Compose (optional)

### **Environment Setup**

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd cib-listing-pricing-svc
   ```

2. **Set up database**
   ```bash
   # Start PostgreSQL with TimescaleDB
   docker-compose up -d postgres
   
   # Apply database schema
   psql -h localhost -U basket_app_user -d custom_index_basket -f database/init/03-market-data-schema.sql
   ```

3. **Set up Redis**
   ```bash
   # Start Redis
   docker-compose up -d redis
   ```

4. **Configure environment variables**
   ```bash
   export BLOOMBERG_API_KEY="your-bloomberg-api-key"
   export REUTERS_API_KEY="your-reuters-api-key"
   export YAHOO_FINANCE_API_KEY="your-yahoo-api-key"
   ```

### **Running the Service**

1. **Build the project**
   ```bash
   mvn clean install -pl market-data-service
   ```

2. **Run the service**
   ```bash
   mvn spring-boot:run -pl market-data-service
   ```

3. **Verify the service**
   ```bash
   # Health check
   curl http://localhost:8082/actuator/health
   
   # Available endpoints
   curl http://localhost:8082/actuator/info
   ```

## 📡 **API Endpoints**

### **Core Market Data**
- `GET /api/v1/market-data/{instrumentId}` - Get market data for an instrument
- `POST /api/v1/market-data/batch` - Get market data for multiple instruments
- `GET /api/v1/market-data/health` - Service health status
- `GET /api/v1/market-data/stats` - Service statistics

### **Proxy Services**
- `GET /api/v1/proxy/market-data/{instrumentId}` - Get data through proxy with caching
- `POST /api/v1/proxy/market-data/batch` - Batch data retrieval through proxy
- `GET /api/v1/proxy/health` - Data source health status
- `GET /api/v1/proxy/metrics` - Data source performance metrics
- `GET /api/v1/proxy/cache/stats` - Cache statistics
- `GET /api/v1/proxy/data-sources` - Available data sources

### **Management & Monitoring**
- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics export

## 🔧 **Configuration**

### **Application Properties**
The service configuration is defined in `src/main/resources/application.yml`:

```yaml
market-data:
  proxy:
    enabled: true
    circuit-breaker:
      enabled: true
      failure-rate-threshold: 50.0
    rate-limiting:
      enabled: true
      default-rate: 1000
    data-quality:
      enabled: true
      validation-rules:
        price:
          min-price: 0.01
          max-price: 1000000.00
    caching:
      enabled: true
      strategy: "quality-based-ttl"
```

### **Data Source Configuration**
```yaml
market-data:
  data-sources:
    bloomberg:
      data-source-name: BLOOMBERG
      rate-limit: 1000
      timeout-seconds: 30
      max-retries: 3
```

### **Cache Configuration**
```yaml
market-data:
  cache:
    key-prefix: market_data
    high-quality-ttl-minutes: 5
    medium-quality-ttl-minutes: 2
    low-quality-ttl-minutes: 1
```

## 🧪 **Testing**

### **Unit Tests**
```bash
# Run all tests
mvn test -pl market-data-service

# Run specific test class
mvn test -pl market-data-service -Dtest=ProxyServiceManagerTest

# Run tests with coverage
mvn test jacoco:report -pl market-data-service
```

### **Integration Tests**
```bash
# Run integration tests
mvn verify -pl market-data-service -P integration-test
```

### **API Testing**
```bash
# Test market data endpoint
curl "http://localhost:8082/api/v1/proxy/market-data/AAPL_US?dataSource=BLOOMBERG"

# Test batch endpoint
curl -X POST "http://localhost:8082/api/v1/proxy/market-data/batch?dataSource=BLOOMBERG" \
  -H "Content-Type: application/json" \
  -d '["AAPL_US", "MSFT_US", "GOOGL_US"]'

# Test health endpoint
curl "http://localhost:8082/api/v1/proxy/health"
```

## 📊 **Monitoring & Observability**

### **Health Checks**
- **Application Health**: `/actuator/health`
- **Data Source Health**: `/api/v1/proxy/health`
- **Cache Health**: Built into Redis health check

### **Metrics**
- **Application Metrics**: `/actuator/metrics`
- **Custom Metrics**: Proxy service performance, cache hit rates, data quality scores
- **Prometheus Export**: `/actuator/prometheus`

### **Logging**
```yaml
logging:
  level:
    com.custom.indexbasket.marketdata.proxy: DEBUG
    com.custom.indexbasket.marketdata.proxy.quality: DEBUG
    com.custom.indexbasket.marketdata.proxy.cache: DEBUG
```

## 🔍 **Troubleshooting**

### **Common Issues**

1. **Database Connection Issues**
   ```bash
   # Check database connectivity
   psql -h localhost -U basket_app_user -d custom_index_basket -c "SELECT 1;"
   
   # Verify TimescaleDB extension
   psql -h localhost -U basket_app_user -d custom_index_basket -c "SELECT * FROM pg_extension WHERE extname = 'timescaledb';"
   ```

2. **Redis Connection Issues**
   ```bash
   # Check Redis connectivity
   redis-cli ping
   
   # Check Redis info
   redis-cli info
   ```

3. **Circuit Breaker Issues**
   ```bash
   # Check circuit breaker state
   curl "http://localhost:8082/api/v1/proxy/metrics" | jq '.[] | select(.dataSourceName == "BLOOMBERG")'
   ```

### **Performance Tuning**

1. **Connection Pooling**
   ```yaml
   spring:
     r2dbc:
       pool:
         max-size: 20
         max-idle-time: 30m
   ```

2. **Cache TTL Optimization**
   ```yaml
   market-data:
     cache:
       high-quality-ttl-minutes: 10
       medium-quality-ttl-minutes: 5
       low-quality-ttl-minutes: 2
   ```

3. **Rate Limiting**
   ```yaml
   market-data:
     data-sources:
       bloomberg:
         rate-limit: 2000  # Increase for higher throughput
   ```

## 🚀 **Development Workflow**

### **Adding New Data Sources**
1. Create a new proxy class extending `AbstractDataSourceProxy`
2. Implement the required abstract methods
3. Add configuration properties
4. Register the proxy in `ProxyServiceManager`
5. Add tests and documentation

### **Adding New Validators**
1. Create a new validator implementing `DataValidator`
2. Add validation logic and rules
3. Register the validator in `SimpleDataQualityManager`
4. Add tests and configuration

### **Extending Cache Strategy**
1. Modify `RedisCacheManager` for new strategies
2. Update TTL calculation logic
3. Add new cache metrics
4. Update configuration and tests

## 📚 **Documentation**

- **Implementation Plan**: `docs/implementation/PHASE3_MARKET_DATA_SERVICE_IMPLEMENTATION.md`
- **Proxy Services**: `docs/implementation/market-data-proxy-services-implementation.md`
- **Implementation Summary**: `docs/implementation/PHASE3_IMPLEMENTATION_SUMMARY.md`

## 🤝 **Contributing**

1. Follow the established code style and patterns
2. Add comprehensive tests for new functionality
3. Update documentation for API changes
4. Ensure all tests pass before submitting PR
5. Follow the reactive programming patterns established in the codebase

## 📄 **License**

This project is proprietary software developed for the Custom Index Basket Management Platform.

---

**Last Updated**: 2025-08-24  
**Version**: 1.0.0  
**Status**: Development Environment Ready
