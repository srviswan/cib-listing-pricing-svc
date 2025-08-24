# Phase 3: Market Data Service Implementation Plan

## 🎯 **Overview**

The **Market Data Service** is the second core service in our Custom Index Basket Management Platform, responsible for providing real-time market data, pricing calculations, and comprehensive analytics for basket constituents and portfolios.

## 🏗️ **Architecture Overview**

### **Service Characteristics**
- **Service Type**: Core Service (Phase 2 of 4)
- **Communication Protocol**: gRPC (Primary), REST API (Secondary)
- **Performance Target**: <10ms P99 latency for gRPC, <100ms P95 for REST
- **Data Sources**: Bloomberg, Reuters, Yahoo Finance, Custom Feeds
- **Database**: PostgreSQL/TimescaleDB with hypertables for time-series data

### **Service Dependencies**
```
Market Data Service
├── Basket Core Service (Event Consumer)
├── External Market Data Providers
├── TimescaleDB (Market Data Storage)
└── Common Module (Shared Components)
```

## 📋 **Implementation Status**

### **✅ Completed Components**
- [x] **Project Structure** - Maven multi-module setup
- [x] **Domain Entities** - MarketDataEntity, BasketMarketDataEntity
- [x] **DTOs** - Request/Response models
- [x] **Service Layer** - Core business logic
- [x] **Repository Layer** - Data access interfaces
- [x] **REST Controller** - HTTP endpoints
- [x] **Event Consumer** - Basket lifecycle event handling
- [x] **Calculation Service** - Analytics and risk metrics
- [x] **Database Schema** - Tables, indexes, and views
- [x] **Configuration** - Application properties and settings

### **🔄 In Progress**
- [ ] **gRPC Implementation** - Protocol buffers and gRPC services
- [ ] **External Data Integration** - Market data provider connectors
- [ ] **Real-time Data Streaming** - WebSocket/SSE implementations
- [ ] **Caching Layer** - Redis integration for performance
- [ ] **Monitoring & Alerting** - Advanced health checks and metrics

### **⏳ Pending Implementation**
- [ ] **Proxy Services** - Data source abstraction layer
- [ ] **Data Quality Management** - Validation and cleansing
- [ ] **Performance Optimization** - Connection pooling and caching
- [ ] **Security Layer** - Authentication and authorization
- [ ] **Testing Suite** - Unit, integration, and performance tests

## 🚀 **Core Features Implementation**

### **1. Real-time Market Data Retrieval**
```java
// Market Data Service Interface
public interface MarketDataService {
    Flux<MarketDataResponse> getMarketData(MarketDataRequest request);
    Mono<MarketDataResponse> getInstrumentMarketData(String instrumentId);
    Mono<BasketMarketDataResponse> getBasketMarketData(UUID basketId);
}
```

**Implementation Details:**
- **Data Sources**: Bloomberg, Reuters, Yahoo Finance APIs
- **Caching Strategy**: Redis with TTL-based invalidation
- **Update Frequency**: Real-time (15s intervals for active instruments)
- **Fallback Strategy**: Multiple data source redundancy

### **2. Basket Analytics & Risk Metrics**
```java
// Calculation Service for Analytics
public class MarketDataCalculationService {
    public BigDecimal calculatePortfolioBeta(List<BigDecimal> betas, List<BigDecimal> weights);
    public BigDecimal calculateSectorDiversificationScore(List<String> sectors, List<BigDecimal> weights);
    public BigDecimal calculateRiskScore(BigDecimal beta, BigDecimal volatility, BigDecimal concentration);
}
```

**Analytics Features:**
- **Risk Metrics**: Beta, Volatility, VaR, Sharpe Ratio
- **Diversification**: Sector, Geographic, Asset Class scoring
- **Performance**: Returns, Drawdowns, Rolling metrics
- **Correlation**: Inter-instrument correlation analysis

### **3. Event-Driven Architecture**
```java
// Event Consumer for Basket Lifecycle
public class BasketEventConsumer {
    public Mono<Void> handleBasketCreated(String eventId, UUID basketId, String basketCode);
    public Mono<Void> handleBasketUpdated(String eventId, UUID basketId, String basketCode);
    public Mono<Void> handleBasketStatusChanged(String eventId, UUID basketId, String basketCode, String previousStatus, String newStatus);
}
```

**Event Handling:**
- **Basket Created**: Initialize market data tracking
- **Basket Updated**: Recalculate metrics and analytics
- **Status Changed**: Update rebalancing schedules
- **Basket Deleted**: Cleanup and archive data

## 🔌 **Proxy Services Implementation**

### **1. Data Source Proxy Service**

#### **Purpose**
Abstract external market data providers and provide a unified interface for data retrieval, ensuring reliability, performance, and data quality.

#### **Architecture**
```
Market Data Service
├── Data Source Proxy Service
│   ├── Bloomberg Proxy
│   ├── Reuters Proxy
│   ├── Yahoo Finance Proxy
│   └── Custom Feed Proxy
└── Data Quality Manager
```

#### **Implementation Plan**

##### **Phase 3.1: Core Proxy Infrastructure (Week 1-2)**
```java
// Data Source Proxy Interface
public interface DataSourceProxy {
    Mono<MarketDataResponse> getInstrumentData(String instrumentId);
    Flux<MarketDataResponse> getBatchData(List<String> instrumentIds);
    Mono<DataSourceHealth> getHealthStatus();
    Mono<DataSourceMetrics> getPerformanceMetrics();
}

// Abstract Base Implementation
public abstract class AbstractDataSourceProxy implements DataSourceProxy {
    protected final DataSourceConfig config;
    protected final CircuitBreaker circuitBreaker;
    protected final RateLimiter rateLimiter;
    
    // Common implementation patterns
    protected abstract Mono<RawMarketData> fetchFromSource(String instrumentId);
    protected abstract MarketDataResponse transformData(RawMarketData rawData);
}
```

##### **Phase 3.2: Bloomberg Proxy Implementation (Week 2-3)**
```java
@Service
public class BloombergProxy extends AbstractDataSourceProxy {
    
    private final BloombergApiClient apiClient;
    private final BloombergDataTransformer transformer;
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        return apiClient.getMarketData(instrumentId)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .doOnError(error -> log.error("Bloomberg API error for {}: {}", instrumentId, error.getMessage()));
    }
    
    @Override
    protected MarketDataResponse transformData(RawMarketData rawData) {
        return transformer.transform(rawData);
    }
}
```

##### **Phase 3.3: Reuters Proxy Implementation (Week 3-4)**
```java
@Service
public class ReutersProxy extends AbstractDataSourceProxy {
    
    private final ReutersApiClient apiClient;
    private final ReutersDataTransformer transformer;
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        return apiClient.getMarketData(instrumentId)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .doOnError(error -> log.error("Reuters API error for {}: {}", instrumentId, error.getMessage()));
    }
}
```

##### **Phase 3.4: Yahoo Finance Proxy Implementation (Week 4-5)**
```java
@Service
public class YahooFinanceProxy extends AbstractDataSourceProxy {
    
    private final YahooFinanceApiClient apiClient;
    private final YahooFinanceDataTransformer transformer;
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        return apiClient.getMarketData(instrumentId)
            .timeout(Duration.ofSeconds(10)) // Yahoo Finance is slower
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)))
            .doOnError(error -> log.error("Yahoo Finance API error for {}: {}", instrumentId, error.getMessage()));
    }
}
```

### **2. Data Quality Proxy Service**

#### **Purpose**
Ensure data quality, consistency, and reliability across all data sources through validation, cleansing, and quality scoring.

#### **Implementation Plan**

##### **Phase 3.5: Data Quality Framework (Week 5-6)**
```java
// Data Quality Manager
public interface DataQualityManager {
    Mono<DataQualityReport> validateData(MarketDataResponse data);
    Mono<DataQualityScore> calculateQualityScore(String instrumentId, String dataSource);
    Mono<Void> reportQualityIssues(DataQualityIssue issue);
}

// Data Quality Validators
public interface DataValidator {
    ValidationResult validate(MarketDataResponse data);
}

@Component
public class PriceValidator implements DataValidator {
    @Override
    public ValidationResult validate(MarketDataResponse data) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Price validation rules
        if (data.getLastPrice() != null && data.getLastPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError("INVALID_PRICE", "Last price must be positive"));
        }
        
        // Bid-ask spread validation
        if (data.getBidPrice() != null && data.getAskPrice() != null) {
            if (data.getAskPrice().compareTo(data.getBidPrice()) <= 0) {
                errors.add(new ValidationError("INVALID_SPREAD", "Ask price must be greater than bid price"));
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

### **3. Caching Proxy Service**

#### **Purpose**
Implement intelligent caching strategies to improve performance and reduce external API calls.

#### **Implementation Plan**

##### **Phase 3.6: Caching Infrastructure (Week 6-7)**
```java
// Cache Manager Interface
public interface CacheManager {
    Mono<MarketDataResponse> getCachedData(String instrumentId);
    Mono<Void> cacheData(String instrumentId, MarketDataResponse data);
    Mono<Void> invalidateCache(String instrumentId);
    Mono<CacheStats> getCacheStats();
}

// Redis-based Cache Implementation
@Service
public class RedisCacheManager implements CacheManager {
    
    private final ReactiveRedisTemplate<String, MarketDataResponse> redisTemplate;
    private final CacheConfig config;
    
    @Override
    public Mono<MarketDataResponse> getCachedData(String instrumentId) {
        String key = buildCacheKey(instrumentId);
        return redisTemplate.opsForValue().get(key)
            .timeout(Duration.ofMillis(100))
            .onErrorReturn(null);
    }
    
    @Override
    public Mono<Void> cacheData(String instrumentId, MarketDataResponse data) {
        String key = buildCacheKey(instrumentId);
        Duration ttl = calculateTTL(data.getDataQuality());
        
        return redisTemplate.opsForValue().set(key, data, ttl)
            .then();
    }
    
    private Duration calculateTTL(String dataQuality) {
        return switch (dataQuality) {
            case "HIGH" -> Duration.ofMinutes(5);
            case "MEDIUM" -> Duration.ofMinutes(2);
            case "LOW" -> Duration.ofMinutes(1);
            default -> Duration.ofMinutes(1);
        };
    }
}
```

## 🗄️ **Database Schema & Optimization**

### **Tables Structure**
```sql
-- Market Data Table (TimescaleDB Hypertable)
CREATE TABLE market_data (
    id UUID PRIMARY KEY,
    instrument_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    last_price DECIMAL(20,6),
    -- ... other fields
    data_timestamp TIMESTAMP WITH TIME ZONE
);

-- Convert to hypertable for time-series optimization
SELECT create_hypertable('market_data', 'data_timestamp');

-- Basket Market Data Table
CREATE TABLE basket_market_data (
    id UUID PRIMARY KEY,
    basket_id UUID NOT NULL,
    total_market_value DECIMAL(20,2),
    risk_score DECIMAL(5,4),
    -- ... other fields
);
```

### **Performance Optimizations**
- **Hypertables**: Time-series data optimization
- **Indexing**: Composite indexes for common queries
- **Partitioning**: Time-based partitioning for historical data
- **Materialized Views**: Pre-calculated analytics

## 📊 **Monitoring & Observability**

### **Health Checks**
```java
@Component
public class MarketDataHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        return Health.up()
            .withDetail("dataSources", getDataSourcesHealth())
            .withDetail("cache", getCacheHealth())
            .withDetail("database", getDatabaseHealth())
            .build();
    }
}
```

### **Metrics & Prometheus**
```java
@Component
public class MarketDataMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer dataRetrievalTimer;
    private final Counter dataQualityCounter;
    
    public MarketDataMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.dataRetrievalTimer = Timer.builder("market_data.retrieval.duration")
            .description("Time taken to retrieve market data")
            .register(meterRegistry);
        this.dataQualityCounter = Counter.builder("market_data.quality.score")
            .description("Data quality scores")
            .register(meterRegistry);
    }
}
```

## 🧪 **Testing Strategy**

### **Unit Tests**
- Service layer business logic
- Calculation service algorithms
- Data transformation logic

### **Integration Tests**
- Repository layer with test database
- External API integrations (mocked)
- Event handling workflows

### **Performance Tests**
- Latency benchmarks (<10ms gRPC, <100ms REST)
- Throughput testing (1000+ instruments/second)
- Cache performance validation

### **End-to-End Tests**
- Complete data flow from external sources to API responses
- Event-driven workflows
- Error handling and recovery scenarios

## 🚀 **Deployment & Operations**

### **Docker Configuration**
```dockerfile
FROM openjdk:23-jdk-slim
WORKDIR /app
COPY target/market-data-service-*.jar app.jar
EXPOSE 8082 9090
CMD ["java", "-jar", "app.jar"]
```

### **Environment Configuration**
```yaml
# Production Configuration
market-data:
  data-sources:
    bloomberg:
      api-key: ${BLOOMBERG_API_KEY}
      base-url: ${BLOOMBERG_BASE_URL}
      rate-limit: 1000
    reuters:
      api-key: ${REUTERS_API_KEY}
      base-url: ${REUTERS_BASE_URL}
      rate-limit: 500
```

### **Health Check Endpoints**
- `/actuator/health` - Service health status
- `/api/v1/market-data/health` - Market data specific health
- `/api/v1/market-data/stats` - Performance statistics

## 📈 **Performance Targets**

### **Latency Requirements**
- **gRPC Endpoints**: <10ms P99
- **REST Endpoints**: <100ms P95
- **Cache Hits**: <1ms P99
- **Database Queries**: <5ms P95

### **Throughput Requirements**
- **Real-time Updates**: 1000+ instruments/second
- **Batch Operations**: 10,000+ instruments/batch
- **Concurrent Users**: 100+ simultaneous connections

### **Availability Requirements**
- **Service Uptime**: 99.9%
- **Data Freshness**: <15 seconds for active instruments
- **Cache Hit Rate**: >90%

## 🔄 **Next Steps & Timeline**

### **Week 1-2: Core Proxy Infrastructure**
- [ ] Implement abstract proxy base classes
- [ ] Create data source configuration framework
- [ ] Implement circuit breaker and rate limiting

### **Week 3-4: Data Source Proxies**
- [ ] Bloomberg proxy implementation
- [ ] Reuters proxy implementation
- [ ] Yahoo Finance proxy implementation

### **Week 5-6: Data Quality & Caching**
- [ ] Data quality validation framework
- [ ] Redis caching implementation
- [ ] Quality scoring algorithms

### **Week 7-8: Testing & Optimization**
- [ ] Comprehensive test suite
- [ ] Performance optimization
- [ ] Security implementation

### **Week 9-10: Integration & Deployment**
- [ ] End-to-end testing
- [ ] Production deployment
- [ ] Monitoring and alerting setup

## 🎯 **Success Criteria**

### **Functional Requirements**
- [ ] Real-time market data for 1000+ instruments
- [ ] Basket analytics with risk metrics
- [ ] Event-driven data updates
- [ ] Multi-source data integration

### **Non-Functional Requirements**
- [ ] <10ms gRPC latency (P99)
- [ ] <100ms REST latency (P95)
- [ ] 99.9% availability
- [ ] >90% cache hit rate

### **Integration Requirements**
- [ ] Seamless integration with Basket Core Service
- [ ] Event publishing to downstream services
- [ ] Real-time data streaming capabilities
- [ ] Comprehensive monitoring and alerting

---

**Document Version**: 1.0.0  
**Last Updated**: 2025-08-24  
**Next Review**: 2025-09-07  
**Phase Lead**: Custom Index Basket Team
