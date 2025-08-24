# Market Data Service - Proxy Services Implementation

## 🎯 **Overview**

This document provides the detailed technical implementation for the **Proxy Services Layer** of the Market Data Service. The proxy services abstract external market data providers, implement data quality management, and provide intelligent caching strategies.

## 🏗️ **Architecture Overview**

```
Market Data Service
├── Proxy Services Layer
│   ├── Data Source Proxy Service
│   │   ├── Bloomberg Proxy
│   │   ├── Reuters Proxy
│   │   ├── Yahoo Finance Proxy
│   │   └── Custom Feed Proxy
│   ├── Data Quality Proxy Service
│   │   ├── Validation Engine
│   │   ├── Quality Scorer
│   │   └── Issue Reporter
│   └── Caching Proxy Service
│       ├── Redis Cache Manager
│       ├── TTL Strategy
│       └── Cache Invalidation
└── Core Business Logic
    ├── Market Data Service
    ├── Calculation Service
    └── Event Consumer
```

## 🔌 **1. Data Source Proxy Service**

### **1.1 Core Interfaces**

#### **DataSourceProxy Interface**
```java
public interface DataSourceProxy {
    
    /**
     * Get market data for a single instrument
     */
    Mono<MarketDataResponse> getInstrumentData(String instrumentId);
    
    /**
     * Get market data for multiple instruments in batch
     */
    Flux<MarketDataResponse> getBatchData(List<String> instrumentIds);
    
    /**
     * Get data source health status
     */
    Mono<DataSourceHealth> getHealthStatus();
    
    /**
     * Get performance metrics for the data source
     */
    Mono<DataSourceMetrics> getPerformanceMetrics();
    
    /**
     * Check if data source is available
     */
    Mono<Boolean> isAvailable();
    
    /**
     * Get supported instrument types
     */
    Flux<String> getSupportedInstrumentTypes();
}
```

#### **DataSourceHealth Record**
```java
public record DataSourceHealth(
    String dataSourceName,
    HealthStatus status,
    LocalDateTime lastCheck,
    String errorMessage,
    Map<String, Object> details
) {
    public enum HealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
    }
}
```

#### **DataSourceMetrics Record**
```java
public record DataSourceMetrics(
    String dataSourceName,
    long totalRequests,
    long successfulRequests,
    long failedRequests,
    Duration averageResponseTime,
    Duration lastResponseTime,
    double successRate,
    Map<String, Object> customMetrics
) {}
```

### **1.2 Abstract Base Implementation**

#### **AbstractDataSourceProxy Class**
```java
@Slf4j
public abstract class AbstractDataSourceProxy implements DataSourceProxy {
    
    protected final DataSourceConfig config;
    protected final CircuitBreaker circuitBreaker;
    protected final RateLimiter rateLimiter;
    protected final MeterRegistry meterRegistry;
    
    protected final Timer requestTimer;
    protected final Counter successCounter;
    protected final Counter failureCounter;
    
    protected AbstractDataSourceProxy(DataSourceConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
        
        // Initialize circuit breaker
        this.circuitBreaker = CircuitBreaker.builder()
            .name(config.getDataSourceName() + "-circuit-breaker")
            .slidingWindowSize(10)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .build();
        
        // Initialize rate limiter
        this.rateLimiter = RateLimiter.builder()
            .name(config.getDataSourceName() + "-rate-limiter")
            .rate(config.getRateLimit())
            .timeUnit(TimeUnit.SECONDS)
            .build();
        
        // Initialize metrics
        this.requestTimer = Timer.builder("market_data.proxy.request.duration")
            .tag("data_source", config.getDataSourceName())
            .register(meterRegistry);
        
        this.successCounter = Counter.builder("market_data.proxy.request.success")
            .tag("data_source", config.getDataSourceName())
            .register(meterRegistry);
        
        this.failureCounter = Counter.builder("market_data.proxy.request.failure")
            .tag("data_source", config.getDataSourceName())
            .register(meterRegistry);
    }
    
    @Override
    public Mono<MarketDataResponse> getInstrumentData(String instrumentId) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            return rateLimiter.acquirePermission()
                .then(circuitBreaker.run(
                    () -> fetchFromSource(instrumentId)
                        .map(this::transformData)
                        .doOnSuccess(result -> {
                            sample.stop(requestTimer);
                            successCounter.increment();
                        })
                        .doOnError(error -> {
                            sample.stop(requestTimer);
                            failureCounter.increment();
                            log.error("Error fetching data for {} from {}: {}", 
                                instrumentId, config.getDataSourceName(), error.getMessage());
                        })
                ))
                .onErrorResume(CircuitBreakerOpenException.class, 
                    error -> handleCircuitBreakerOpen(instrumentId))
                .onErrorResume(RateLimiterAcquireException.class, 
                    error -> handleRateLimitExceeded(instrumentId));
        });
    }
    
    @Override
    public Flux<MarketDataResponse> getBatchData(List<String> instrumentIds) {
        return Flux.fromIterable(instrumentIds)
            .flatMap(this::getInstrumentData, config.getConcurrencyLimit())
            .doOnComplete(() -> log.info("Batch data retrieval completed for {} instruments from {}", 
                instrumentIds.size(), config.getDataSourceName()));
    }
    
    @Override
    public Mono<DataSourceHealth> getHealthStatus() {
        return isAvailable()
            .map(available -> {
                HealthStatus status = available ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
                return new DataSourceHealth(
                    config.getDataSourceName(),
                    status,
                    LocalDateTime.now(),
                    null,
                    Map.of("lastCheck", LocalDateTime.now())
                );
            })
            .onErrorReturn(new DataSourceHealth(
                config.getDataSourceName(),
                HealthStatus.UNKNOWN,
                LocalDateTime.now(),
                "Health check failed",
                Map.of("error", "Health check failed")
            ));
    }
    
    @Override
    public Mono<DataSourceMetrics> getPerformanceMetrics() {
        return Mono.just(new DataSourceMetrics(
            config.getDataSourceName(),
            successCounter.count() + failureCounter.count(),
            successCounter.count(),
            failureCounter.count(),
            Duration.ofNanos((long) requestTimer.mean(TimeUnit.NANOSECONDS)),
            Duration.ofNanos((long) requestTimer.max(TimeUnit.NANOSECONDS)),
            calculateSuccessRate(),
            Map.of("circuitBreakerState", circuitBreaker.getState())
        ));
    }
    
    // Abstract methods to be implemented by concrete classes
    protected abstract Mono<RawMarketData> fetchFromSource(String instrumentId);
    protected abstract MarketDataResponse transformData(RawMarketData rawData);
    protected abstract Mono<Boolean> checkAvailability();
    
    // Helper methods
    private double calculateSuccessRate() {
        long total = successCounter.count() + failureCounter.count();
        return total > 0 ? (double) successCounter.count() / total : 0.0;
    }
    
    private Mono<MarketDataResponse> handleCircuitBreakerOpen(String instrumentId) {
        log.warn("Circuit breaker open for {}, using fallback data", config.getDataSourceName());
        return getFallbackData(instrumentId);
    }
    
    private Mono<MarketDataResponse> handleRateLimitExceeded(String instrumentId) {
        log.warn("Rate limit exceeded for {}, queuing request", config.getDataSourceName());
        return Mono.error(new RateLimitExceededException("Rate limit exceeded for " + config.getDataSourceName()));
    }
    
    protected Mono<MarketDataResponse> getFallbackData(String instrumentId) {
        // Default fallback implementation
        return Mono.empty();
    }
}
```

### **1.3 Concrete Implementations**

#### **Bloomberg Proxy Implementation**
```java
@Service
@Slf4j
public class BloombergProxy extends AbstractDataSourceProxy {
    
    private final BloombergApiClient apiClient;
    private final BloombergDataTransformer transformer;
    private final BloombergConfig bloombergConfig;
    
    public BloombergProxy(BloombergConfig config, MeterRegistry meterRegistry) {
        super(config, meterRegistry);
        this.bloombergConfig = config;
        this.apiClient = new BloombergApiClient(config);
        this.transformer = new BloombergDataTransformer();
    }
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        return apiClient.getMarketData(instrumentId)
            .timeout(Duration.ofSeconds(bloombergConfig.getTimeoutSeconds()))
            .retryWhen(Retry.backoff(bloombergConfig.getMaxRetries(), 
                Duration.ofSeconds(bloombergConfig.getRetryDelaySeconds())))
            .doOnSubscribe(sub -> log.debug("Fetching Bloomberg data for instrument: {}", instrumentId))
            .doOnSuccess(data -> log.debug("Bloomberg data fetched successfully for: {}", instrumentId))
            .doOnError(error -> log.error("Bloomberg API error for {}: {}", instrumentId, error.getMessage()));
    }
    
    @Override
    protected MarketDataResponse transformData(RawMarketData rawData) {
        return transformer.transform(rawData);
    }
    
    @Override
    public Mono<Boolean> isAvailable() {
        return checkAvailability();
    }
    
    @Override
    protected Mono<Boolean> checkAvailability() {
        return apiClient.ping()
            .timeout(Duration.ofSeconds(5))
            .map(response -> response.isSuccess())
            .onErrorReturn(false);
    }
    
    @Override
    public Flux<String> getSupportedInstrumentTypes() {
        return Flux.fromIterable(List.of("STOCK", "BOND", "ETF", "INDEX", "CURRENCY", "COMMODITY"));
    }
}
```

#### **Reuters Proxy Implementation**
```java
@Service
@Slf4j
public class ReutersProxy extends AbstractDataSourceProxy {
    
    private final ReutersApiClient apiClient;
    private final ReutersDataTransformer transformer;
    private final ReutersConfig reutersConfig;
    
    public ReutersProxy(ReutersConfig config, MeterRegistry meterRegistry) {
        super(config, meterRegistry);
        this.reutersConfig = config;
        this.apiClient = new ReutersApiClient(config);
        this.transformer = new ReutersDataTransformer();
    }
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        return apiClient.getMarketData(instrumentId)
            .timeout(Duration.ofSeconds(reutersConfig.getTimeoutSeconds()))
            .retryWhen(Retry.backoff(reutersConfig.getMaxRetries(), 
                Duration.ofSeconds(reutersConfig.getRetryDelaySeconds())))
            .doOnSubscribe(sub -> log.debug("Fetching Reuters data for instrument: {}", instrumentId))
            .doOnSuccess(data -> log.debug("Reuters data fetched successfully for: {}", instrumentId))
            .doOnError(error -> log.error("Reuters API error for {}: {}", instrumentId, error.getMessage()));
    }
    
    @Override
    protected MarketDataResponse transformData(RawMarketData rawData) {
        return transformer.transform(rawData);
    }
    
    @Override
    protected Mono<Boolean> checkAvailability() {
        return apiClient.healthCheck()
            .timeout(Duration.ofSeconds(5))
            .map(response -> response.getStatus().equals("OK"))
            .onErrorReturn(false);
    }
    
    @Override
    public Flux<String> getSupportedInstrumentTypes() {
        return Flux.fromIterable(List.of("STOCK", "BOND", "ETF", "INDEX", "CURRENCY"));
    }
}
```

## 🔍 **2. Data Quality Proxy Service**

### **2.1 Core Interfaces**

#### **DataQualityManager Interface**
```java
public interface DataQualityManager {
    
    /**
     * Validate market data and return quality report
     */
    Mono<DataQualityReport> validateData(MarketDataResponse data);
    
    /**
     * Calculate quality score for an instrument from a specific data source
     */
    Mono<DataQualityScore> calculateQualityScore(String instrumentId, String dataSource);
    
    /**
     * Report data quality issues for investigation
     */
    Mono<Void> reportQualityIssues(DataQualityIssue issue);
    
    /**
     * Get overall data quality metrics
     */
    Mono<DataQualityMetrics> getQualityMetrics();
    
    /**
     * Get quality trends over time
     */
    Flux<DataQualityTrend> getQualityTrends(Duration timeWindow);
}
```

#### **DataValidator Interface**
```java
public interface DataValidator {
    
    /**
     * Validate market data and return validation result
     */
    ValidationResult validate(MarketDataResponse data);
    
    /**
     * Get validator name for identification
     */
    String getValidatorName();
    
    /**
     * Get validation rules description
     */
    String getValidationRules();
}
```

### **2.2 Implementation Classes**

#### **PriceValidator Implementation**
```java
@Component
@Slf4j
public class PriceValidator implements DataValidator {
    
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000.00");
    private static final BigDecimal MAX_SPREAD_PERCENTAGE = new BigDecimal("50.0");
    
    @Override
    public ValidationResult validate(MarketDataResponse data) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Price validation rules
        validatePrice(data.getLastPrice(), "lastPrice", errors);
        validatePrice(data.getBidPrice(), "bidPrice", errors);
        validatePrice(data.getAskPrice(), "askPrice", errors);
        validatePrice(data.getOpenPrice(), "openPrice", errors);
        validatePrice(data.getHighPrice(), "highPrice", errors);
        validatePrice(data.getLowPrice(), "lowPrice", errors);
        
        // Bid-ask spread validation
        validateBidAskSpread(data.getBidPrice(), data.getAskPrice(), errors);
        
        // Price consistency validation
        validatePriceConsistency(data, errors);
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private void validatePrice(BigDecimal price, String fieldName, List<ValidationError> errors) {
        if (price != null) {
            if (price.compareTo(MIN_PRICE) < 0) {
                errors.add(new ValidationError(
                    "PRICE_TOO_LOW", 
                    String.format("%s price %s is below minimum threshold %s", fieldName, price, MIN_PRICE)
                ));
            }
            
            if (price.compareTo(MAX_PRICE) > 0) {
                errors.add(new ValidationError(
                    "PRICE_TOO_HIGH", 
                    String.format("%s price %s is above maximum threshold %s", fieldName, price, MAX_PRICE)
                ));
            }
        }
    }
    
    private void validateBidAskSpread(BigDecimal bidPrice, BigDecimal askPrice, List<ValidationError> errors) {
        if (bidPrice != null && askPrice != null) {
            if (askPrice.compareTo(bidPrice) <= 0) {
                errors.add(new ValidationError(
                    "INVALID_BID_ASK_SPREAD", 
                    "Ask price must be greater than bid price"
                ));
            }
            
            // Calculate spread percentage
            BigDecimal spread = askPrice.subtract(bidPrice);
            BigDecimal midPrice = bidPrice.add(askPrice).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            BigDecimal spreadPercentage = spread.divide(midPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            if (spreadPercentage.compareTo(MAX_SPREAD_PERCENTAGE) > 0) {
                errors.add(new ValidationError(
                    "SPREAD_TOO_WIDE", 
                    String.format("Bid-ask spread %.2f%% exceeds maximum threshold %.2f%%", 
                        spreadPercentage, MAX_SPREAD_PERCENTAGE)
                ));
            }
        }
    }
    
    private void validatePriceConsistency(MarketDataResponse data, List<ValidationError> errors) {
        // High price should be >= Low price
        if (data.getHighPrice() != null && data.getLowPrice() != null) {
            if (data.getHighPrice().compareTo(data.getLowPrice()) < 0) {
                errors.add(new ValidationError(
                    "INVALID_HIGH_LOW_PRICES", 
                    "High price must be greater than or equal to low price"
                ));
            }
        }
        
        // Last price should be between high and low
        if (data.getLastPrice() != null && data.getHighPrice() != null && data.getLowPrice() != null) {
            if (data.getLastPrice().compareTo(data.getHighPrice()) > 0 || 
                data.getLastPrice().compareTo(data.getLowPrice()) < 0) {
                errors.add(new ValidationError(
                    "LAST_PRICE_OUT_OF_RANGE", 
                    "Last price must be between high and low prices"
                ));
            }
        }
    }
    
    @Override
    public String getValidatorName() {
        return "PriceValidator";
    }
    
    @Override
    public String getValidationRules() {
        return "Validates price ranges, bid-ask spreads, and price consistency";
    }
}
```

## 🗄️ **3. Caching Proxy Service**

### **3.1 Core Interfaces**

#### **CacheManager Interface**
```java
public interface CacheManager {
    
    /**
     * Get cached market data for an instrument
     */
    Mono<MarketDataResponse> getCachedData(String instrumentId);
    
    /**
     * Cache market data for an instrument
     */
    Mono<Void> cacheData(String instrumentId, MarketDataResponse data);
    
    /**
     * Invalidate cache for an instrument
     */
    Mono<Void> invalidateCache(String instrumentId);
    
    /**
     * Get cache statistics
     */
    Mono<CacheStats> getCacheStats();
    
    /**
     * Clear all cache entries
     */
    Mono<Void> clearCache();
    
    /**
     * Get cache keys for debugging
     */
    Flux<String> getCacheKeys();
}
```

### **3.2 Redis Implementation**

#### **RedisCacheManager Implementation**
```java
@Service
@Slf4j
public class RedisCacheManager implements CacheManager {
    
    private final ReactiveRedisTemplate<String, MarketDataResponse> redisTemplate;
    private final CacheConfig config;
    private final MeterRegistry meterRegistry;
    
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheSetCounter;
    private final Timer cacheGetTimer;
    private final Timer cacheSetTimer;
    
    public RedisCacheManager(ReactiveRedisTemplate<String, MarketDataResponse> redisTemplate,
                           CacheConfig config, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.cacheHitCounter = Counter.builder("market_data.cache.hit")
            .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("market_data.cache.miss")
            .register(meterRegistry);
        this.cacheSetCounter = Counter.builder("market_data.cache.set")
            .register(meterRegistry);
        this.cacheGetTimer = Timer.builder("market_data.cache.get.duration")
            .register(meterRegistry);
        this.cacheSetTimer = Timer.builder("market_data.cache.set.duration")
            .register(meterRegistry);
    }
    
    @Override
    public Mono<MarketDataResponse> getCachedData(String instrumentId) {
        String key = buildCacheKey(instrumentId);
        
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            return redisTemplate.opsForValue().get(key)
                .timeout(Duration.ofMillis(config.getGetTimeoutMs()))
                .doOnSuccess(data -> {
                    sample.stop(cacheGetTimer);
                    if (data != null) {
                        cacheHitCounter.increment();
                        log.debug("Cache hit for instrument: {}", instrumentId);
                    } else {
                        cacheMissCounter.increment();
                        log.debug("Cache miss for instrument: {}", instrumentId);
                    }
                })
                .doOnError(error -> {
                    sample.stop(cacheGetTimer);
                    cacheMissCounter.increment();
                    log.warn("Cache error for instrument {}: {}", instrumentId, error.getMessage());
                })
                .onErrorReturn(null);
        });
    }
    
    @Override
    public Mono<Void> cacheData(String instrumentId, MarketDataResponse data) {
        String key = buildCacheKey(instrumentId);
        Duration ttl = calculateTTL(data.getDataQuality());
        
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            return redisTemplate.opsForValue().set(key, data, ttl)
                .timeout(Duration.ofMillis(config.getSetTimeoutMs()))
                .doOnSuccess(result -> {
                    sample.stop(cacheSetTimer);
                    cacheSetCounter.increment();
                    log.debug("Data cached for instrument: {} with TTL: {}", instrumentId, ttl);
                })
                .doOnError(error -> {
                    sample.stop(cacheSetTimer);
                    log.error("Cache set error for instrument {}: {}", instrumentId, error.getMessage());
                })
                .then();
        });
    }
    
    @Override
    public Mono<Void> invalidateCache(String instrumentId) {
        String key = buildCacheKey(instrumentId);
        
        return redisTemplate.delete(key)
            .doOnSuccess(result -> log.debug("Cache invalidated for instrument: {}", instrumentId))
            .doOnError(error -> log.error("Cache invalidation error for instrument {}: {}", 
                instrumentId, error.getMessage()))
            .then();
    }
    
    @Override
    public Mono<CacheStats> getCacheStats() {
        return Mono.just(new CacheStats(
            cacheHitCounter.count(),
            cacheMissCounter.count(),
            cacheSetCounter.count(),
            calculateHitRate(),
            config.getMaxSize(),
            getCurrentCacheSize()
        ));
    }
    
    @Override
    public Mono<Void> clearCache() {
        return redisTemplate.execute(RedisScript.of("return redis.call('flushdb')"))
            .doOnSuccess(result -> log.info("Cache cleared successfully"))
            .doOnError(error -> log.error("Cache clear error: {}", error.getMessage()))
            .then();
    }
    
    @Override
    public Flux<String> getCacheKeys() {
        return redisTemplate.execute(RedisScript.of("return redis.call('keys', '*')"))
            .flatMapMany(Flux::fromIterable)
            .cast(String.class)
            .filter(key -> key.startsWith(config.getKeyPrefix()));
    }
    
    // Private helper methods
    private String buildCacheKey(String instrumentId) {
        return config.getKeyPrefix() + ":" + instrumentId;
    }
    
    private Duration calculateTTL(String dataQuality) {
        return switch (dataQuality) {
            case "HIGH" -> Duration.ofMinutes(config.getHighQualityTTLMinutes());
            case "MEDIUM" -> Duration.ofMinutes(config.getMediumQualityTTLMinutes());
            case "LOW" -> Duration.ofMinutes(config.getLowQualityTTLMinutes());
            default -> Duration.ofMinutes(config.getDefaultTTLMinutes());
        };
    }
    
    private double calculateHitRate() {
        long total = cacheHitCounter.count() + cacheMissCounter.count();
        return total > 0 ? (double) cacheHitCounter.count() / total : 0.0;
    }
    
    private long getCurrentCacheSize() {
        // This would typically query Redis for current database size
        return 0L; // Placeholder
    }
}
```

## ⚙️ **4. Configuration Classes**

### **4.1 Data Source Configuration**
```java
@ConfigurationProperties(prefix = "market-data.data-sources")
@Data
public class DataSourceConfig {
    
    private String dataSourceName;
    private String baseUrl;
    private String apiKey;
    private int rateLimit;
    private int timeoutSeconds;
    private int maxRetries;
    private int retryDelaySeconds;
    private int concurrencyLimit;
    
    // Bloomberg specific
    private String bloombergAppName;
    private String bloombergServerHost;
    private int bloombergServerPort;
    
    // Reuters specific
    private String reutersUsername;
    private String reutersPassword;
    private String reutersAppId;
    
    // Yahoo Finance specific
    private String yahooFinanceApiKey;
    private int yahooFinanceTimeoutSeconds;
}
```

### **4.2 Cache Configuration**
```java
@ConfigurationProperties(prefix = "market-data.cache")
@Data
public class CacheConfig {
    
    private String keyPrefix = "market_data";
    private int maxSize = 10000;
    private int getTimeoutMs = 100;
    private int setTimeoutMs = 200;
    
    // TTL Configuration
    private int highQualityTTLMinutes = 5;
    private int mediumQualityTTLMinutes = 2;
    private int lowQualityTTLMinutes = 1;
    private int defaultTTLMinutes = 1;
    
    // Redis Configuration
    private String redisHost = "localhost";
    private int redisPort = 6379;
    private String redisPassword;
    private int redisDatabase = 0;
    private int redisConnectionPoolSize = 10;
}
```

## 🧪 **5. Testing Strategy**

### **5.1 Unit Tests**
```java
@ExtendWith(MockitoExtension.class)
class BloombergProxyTest {
    
    @Mock
    private BloombergApiClient apiClient;
    
    @Mock
    private BloombergDataTransformer transformer;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    private BloombergProxy proxy;
    
    @BeforeEach
    void setUp() {
        BloombergConfig config = new BloombergConfig();
        config.setDataSourceName("BLOOMBERG");
        config.setRateLimit(1000);
        config.setTimeoutSeconds(5);
        
        proxy = new BloombergProxy(config, meterRegistry);
        // Set mocks using reflection or constructor injection
    }
    
    @Test
    void getInstrumentData_Success() {
        // Given
        String instrumentId = "AAPL_US";
        RawMarketData rawData = createMockRawData();
        MarketDataResponse expectedResponse = createMockResponse();
        
        when(apiClient.getMarketData(instrumentId)).thenReturn(Mono.just(rawData));
        when(transformer.transform(rawData)).thenReturn(expectedResponse);
        
        // When
        MarketDataResponse result = proxy.getInstrumentData(instrumentId).block();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInstrumentId()).isEqualTo(instrumentId);
        verify(apiClient).getMarketData(instrumentId);
        verify(transformer).transform(rawData);
    }
    
    @Test
    void getInstrumentData_CircuitBreakerOpen() {
        // Given
        String instrumentId = "AAPL_US";
        when(apiClient.getMarketData(instrumentId)).thenReturn(Mono.error(new RuntimeException("API Error")));
        
        // When & Then
        assertThatThrownBy(() -> proxy.getInstrumentData(instrumentId).block())
            .isInstanceOf(CircuitBreakerOpenException.class);
    }
}
```

## 📊 **6. Performance Monitoring**

### **6.1 Metrics Collection**
```java
@Component
public class ProxyServiceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public ProxyServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordDataSourceLatency(String dataSource, Duration latency) {
        Timer.builder("market_data.proxy.latency")
            .tag("data_source", dataSource)
            .register(meterRegistry)
            .record(latency);
    }
    
    public void recordDataQualityScore(String dataSource, double qualityScore) {
        Gauge.builder("market_data.proxy.quality_score")
            .tag("data_source", dataSource)
            .register(meterRegistry, () -> qualityScore);
    }
    
    public void recordCacheHitRate(double hitRate) {
        Gauge.builder("market_data.proxy.cache_hit_rate")
            .register(meterRegistry, () -> hitRate);
    }
}
```

## 🚀 **7. Deployment & Operations**

### **7.1 Docker Configuration**
```dockerfile
FROM openjdk:23-jdk-slim

# Install Redis client for health checks
RUN apt-get update && apt-get install -y redis-tools && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/market-data-service-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1

EXPOSE 8082 9090
CMD ["java", "-jar", "app.jar"]
```

### **7.2 Kubernetes Configuration**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: market-data-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: market-data-service
  template:
    metadata:
      labels:
        app: market-data-service
    spec:
      containers:
      - name: market-data-service
        image: market-data-service:latest
        ports:
        - containerPort: 8082
        - containerPort: 9090
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: BLOOMBERG_API_KEY
          valueFrom:
            secretKeyRef:
              name: market-data-secrets
              key: bloomberg-api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

**Document Version**: 1.0.0  
**Last Updated**: 2025-08-24  
**Next Review**: 2025-09-07  
**Phase Lead**: Custom Index Basket Team
