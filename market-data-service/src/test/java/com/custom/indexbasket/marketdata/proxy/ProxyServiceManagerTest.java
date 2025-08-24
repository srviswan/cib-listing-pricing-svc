package com.custom.indexbasket.marketdata.proxy;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.marketdata.proxy.quality.DataQualityManager;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceHealth;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxyServiceManagerTest {
    
    @Mock
    private DataSourceProxy bloombergProxy;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private DataQualityManager dataQualityManager;
    
    @Mock
    private MeterRegistry meterRegistry;
    
    private ProxyServiceManager proxyServiceManager;
    
    @BeforeEach
    void setUp() {
        List<DataSourceProxy> dataSourceProxies = List.of(bloombergProxy);
        proxyServiceManager = new ProxyServiceManager(dataSourceProxies, cacheService, dataQualityManager, meterRegistry);
    }
    
    @Test
    void getMarketData_WithCacheHit_ReturnsCachedData() {
        // Given
        String instrumentId = "AAPL_US";
        MarketDataResponse cachedData = createMockMarketDataResponse(instrumentId);
        
        when(cacheService.get(instrumentId, MarketDataResponse.class)).thenReturn(Mono.just(cachedData));
        
        // When & Then
        StepVerifier.create(proxyServiceManager.getMarketData(instrumentId, "BLOOMBERG"))
            .expectNext(cachedData)
            .verifyComplete();
    }
    
    @Test
    void getMarketData_WithCacheMiss_FetchesFromDataSource() {
        // Given
        String instrumentId = "AAPL_US";
        MarketDataResponse freshData = createMockMarketDataResponse(instrumentId);
        
        when(cacheService.get(instrumentId, MarketDataResponse.class)).thenReturn(Mono.empty());
        when(bloombergProxy.isAvailable()).thenReturn(Mono.just(true));
        when(bloombergProxy.getInstrumentData(instrumentId)).thenReturn(Mono.just(freshData));
        when(dataQualityManager.validateData(any())).thenReturn(Mono.just(createMockQualityReport(true)));
        when(cacheService.put(anyString(), any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(proxyServiceManager.getMarketData(instrumentId, "BLOOMBERG"))
            .expectNext(freshData)
            .verifyComplete();
    }
    
    @Test
    void getAllDataSourceHealth_ReturnsHealthStatus() {
        // Given
        DataSourceHealth health = new DataSourceHealth(
            "BLOOMBERG",
            DataSourceHealth.HealthStatus.HEALTHY,
            LocalDateTime.now(),
            null,
            Map.of("status", "healthy")
        );
        
        when(bloombergProxy.getHealthStatus()).thenReturn(Mono.just(health));
        
        // When & Then
        StepVerifier.create(proxyServiceManager.getAllDataSourceHealth())
            .expectNext(health)
            .verifyComplete();
    }
    
    @Test
    void getAllDataSourceMetrics_ReturnsMetrics() {
        // Given
        DataSourceMetrics metrics = new DataSourceMetrics(
            "BLOOMBERG",
            100L,
            95L,
            5L,
            java.time.Duration.ofMillis(50),
            java.time.Duration.ofMillis(100),
            0.95,
            Map.of("circuitBreakerState", "CLOSED")
        );
        
        when(bloombergProxy.getPerformanceMetrics()).thenReturn(Mono.just(metrics));
        
        // When & Then
        StepVerifier.create(proxyServiceManager.getAllDataSourceMetrics())
            .expectNext(metrics)
            .verifyComplete();
    }
    
    @Test
    void getCacheStatistics_ReturnsCacheStats() {
        // Given
        when(cacheService.getStats()).thenReturn(Mono.just(createMockCacheStats()));
        
        // When & Then
        StepVerifier.create(proxyServiceManager.getCacheStatistics())
            .expectNextMatches(stats -> 
                stats.containsKey("hitCount") && 
                stats.containsKey("missCount") && 
                stats.containsKey("hitRate") &&
                stats.containsKey("totalRequests"))
            .verifyComplete();
    }
    
    @Test
    void getAvailableDataSources_ReturnsDataSourceList() {
        // When & Then
        StepVerifier.create(proxyServiceManager.getAvailableDataSources())
            .expectNext("BLOOMBERG")
            .verifyComplete();
    }
    
    @Test
    void isDataSourceAvailable_ReturnsAvailabilityStatus() {
        // Given
        when(bloombergProxy.isAvailable()).thenReturn(Mono.just(true));
        
        // When & Then
        StepVerifier.create(proxyServiceManager.isDataSourceAvailable("BLOOMBERG"))
            .expectNext(true)
            .verifyComplete();
    }
    
    // Helper methods
    private MarketDataResponse createMockMarketDataResponse(String instrumentId) {
        return MarketDataResponse.builder()
            .instrumentId(instrumentId)
            .lastPrice(new BigDecimal("150.00"))
            .bidPrice(new BigDecimal("149.95"))
            .askPrice(new BigDecimal("150.05"))
            .openPrice(new BigDecimal("149.50"))
            .highPrice(new BigDecimal("151.00"))
            .lowPrice(new BigDecimal("149.00"))
            .volume(1000000L)
            .currency("USD")
            .exchange("NASDAQ")
            .dataTimestamp(LocalDateTime.now())
            .dataSource("BLOOMBERG")
            .dataQuality("HIGH")
            .build();
    }
    
    private com.custom.indexbasket.marketdata.proxy.quality.model.DataQualityReport createMockQualityReport(boolean isValid) {
        return new com.custom.indexbasket.marketdata.proxy.quality.model.DataQualityReport(
            "AAPL_US",
            "BLOOMBERG",
            LocalDateTime.now(),
            isValid,
            95.0,
            List.of(),
            Map.of("validatorCount", 1)
        );
    }
    
    private com.custom.indexbasket.common.caching.CacheStats createMockCacheStats() {
        com.custom.indexbasket.common.caching.CacheStats stats = new com.custom.indexbasket.common.caching.CacheStats("test-cache");
        // Set up the stats with some test data
        stats.recordGet(true);  // Record a hit
        stats.recordGet(true);  // Record another hit
        stats.recordGet(false); // Record a miss
        stats.recordPut();      // Record a put
        return stats;
    }
}
