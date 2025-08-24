package com.custom.indexbasket.marketdata.proxy;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.marketdata.proxy.quality.DataQualityManager;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceHealth;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages and coordinates all proxy services.
 * Provides a unified interface for market data operations.
 */
@Service
public class ProxyServiceManager {
    
    private static final Logger log = LoggerFactory.getLogger(ProxyServiceManager.class);
    private final Map<String, DataSourceProxy> dataSourceProxies;
    private final CacheService cacheService;
    private final DataQualityManager dataQualityManager;
    private final MeterRegistry meterRegistry;
    
    public ProxyServiceManager(List<DataSourceProxy> dataSourceProxies,
                             CacheService cacheService,
                             DataQualityManager dataQualityManager,
                             MeterRegistry meterRegistry) {
        this.dataSourceProxies = new ConcurrentHashMap<>();
        this.cacheService = cacheService;
        this.dataQualityManager = dataQualityManager;
        this.meterRegistry = meterRegistry;
        
        // Register all data source proxies
        dataSourceProxies.forEach(proxy -> {
            String name = proxy.getClass().getSimpleName().replace("Proxy", "").toUpperCase();
            this.dataSourceProxies.put(name, proxy);
            log.info("Registered data source proxy: {}", name);
        });
    }
    
    /**
     * Get market data for an instrument with caching and quality validation.
     */
    public Mono<MarketDataResponse> getMarketData(String instrumentId, String preferredDataSource) {
        return Mono.defer(() -> {
            log.debug("Requesting market data for instrument: {} from source: {}", instrumentId, preferredDataSource);
            
            // First try to get from cache
            return cacheService.get(instrumentId, MarketDataResponse.class)
                .flatMap(cachedData -> {
                    if (cachedData != null) {
                        log.debug("Cache hit for instrument: {}", instrumentId);
                        return Mono.just(cachedData);
                    }
                    
                    // Cache miss, fetch from data source
                    log.debug("Cache miss for instrument: {}, fetching from data source", instrumentId);
                    return fetchFromDataSource(instrumentId, preferredDataSource)
                        .flatMap(data -> {
                            // Validate data quality
                            return dataQualityManager.validateData(data)
                                .flatMap(qualityReport -> {
                                    if (qualityReport.isValid()) {
                                        // Cache the data
                                        return cacheService.put(instrumentId, data)
                                            .thenReturn(data);
                                    } else {
                                        log.warn("Data quality validation failed for instrument {}: {}", 
                                            instrumentId, qualityReport.errors());
                                        return Mono.just(data); // Return data even if validation fails
                                    }
                                });
                        });
                });
        });
    }
    
    /**
     * Get market data for multiple instruments in batch.
     */
    public Flux<MarketDataResponse> getBatchMarketData(List<String> instrumentIds, String preferredDataSource) {
        return Flux.fromIterable(instrumentIds)
            .flatMap(instrumentId -> getMarketData(instrumentId, preferredDataSource), 10)
            .doOnComplete(() -> log.info("Batch market data retrieval completed for {} instruments", instrumentIds.size()));
    }
    
    /**
     * Get health status for all data sources.
     */
    public Flux<DataSourceHealth> getAllDataSourceHealth() {
        return Flux.fromIterable(dataSourceProxies.entrySet())
            .flatMap(entry -> entry.getValue().getHealthStatus())
            .doOnComplete(() -> log.debug("Health check completed for all data sources"));
    }
    
    /**
     * Get performance metrics for all data sources.
     */
    public Flux<DataSourceMetrics> getAllDataSourceMetrics() {
        return Flux.fromIterable(dataSourceProxies.entrySet())
            .flatMap(entry -> entry.getValue().getPerformanceMetrics())
            .doOnComplete(() -> log.debug("Performance metrics collected for all data sources"));
    }
    
    /**
     * Get cache statistics.
     */
    public Mono<Map<String, Object>> getCacheStatistics() {
        return cacheService.getStats()
            .map(stats -> Map.of(
                "hitCount", stats.getTotalHits(),
                "missCount", stats.getTotalMisses(),
                "setCount", stats.getTotalPuts(),
                "hitRate", stats.getHitRate(),
                "totalRequests", stats.getTotalRequests(),
                "uptimeSeconds", stats.getUptimeSeconds()
            ));
    }
    
    /**
     * Clear cache for a specific instrument.
     */
    public Mono<Void> invalidateCache(String instrumentId) {
        return cacheService.delete(instrumentId)
            .then()
            .doOnSuccess(result -> log.info("Cache invalidated for instrument: {}", instrumentId))
            .doOnError(error -> log.error("Failed to invalidate cache for instrument {}: {}", 
                instrumentId, error.getMessage()));
    }
    
    /**
     * Clear all cache entries.
     */
    public Mono<Void> clearAllCache() {
        return cacheService.clearAll()
            .doOnSuccess(result -> log.info("All cache entries cleared"))
            .doOnError(error -> log.error("Failed to clear cache: {}", error.getMessage()));
    }
    
    /**
     * Get list of available data sources.
     */
    public Flux<String> getAvailableDataSources() {
        return Flux.fromIterable(dataSourceProxies.keySet())
            .doOnComplete(() -> log.debug("Available data sources: {}", dataSourceProxies.keySet()));
    }
    
    /**
     * Check if a specific data source is available.
     */
    public Mono<Boolean> isDataSourceAvailable(String dataSourceName) {
        DataSourceProxy proxy = dataSourceProxies.get(dataSourceName.toUpperCase());
        if (proxy == null) {
            return Mono.just(false);
        }
        return proxy.isAvailable();
    }
    
    // Private helper methods
    private Mono<MarketDataResponse> fetchFromDataSource(String instrumentId, String preferredDataSource) {
        // Try preferred data source first
        if (preferredDataSource != null && dataSourceProxies.containsKey(preferredDataSource.toUpperCase())) {
            DataSourceProxy preferredProxy = dataSourceProxies.get(preferredDataSource.toUpperCase());
            return preferredProxy.isAvailable()
                .flatMap(available -> {
                    if (available) {
                        return preferredProxy.getInstrumentData(instrumentId);
                    } else {
                        log.warn("Preferred data source {} is not available, trying alternatives", preferredDataSource);
                        return tryAlternativeDataSources(instrumentId, preferredDataSource);
                    }
                });
        }
        
        // No preferred source, try all available sources
        return tryAlternativeDataSources(instrumentId, null);
    }
    
    private Mono<MarketDataResponse> tryAlternativeDataSources(String instrumentId, String excludeSource) {
        return Flux.fromIterable(dataSourceProxies.entrySet())
            .filter(entry -> !entry.getKey().equalsIgnoreCase(excludeSource))
            .flatMap(entry -> entry.getValue().isAvailable()
                .flatMap(available -> available ? 
                    entry.getValue().getInstrumentData(instrumentId) : Mono.empty()))
            .next()
            .switchIfEmpty(Mono.error(new RuntimeException("No available data sources for instrument: " + instrumentId)));
    }
}
