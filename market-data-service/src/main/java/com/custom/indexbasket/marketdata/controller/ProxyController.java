package com.custom.indexbasket.marketdata.controller;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.ProxyServiceManager;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceHealth;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceMetrics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST controller for proxy services.
 * Provides endpoints for accessing market data through proxy services.
 */
@RestController
@RequestMapping("/api/v1/proxy")
public class ProxyController {
    
    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);
    private final ProxyServiceManager proxyServiceManager;
    
    public ProxyController(ProxyServiceManager proxyServiceManager) {
        this.proxyServiceManager = proxyServiceManager;
    }
    
    /**
     * Get market data for a single instrument through proxy services.
     */
    @GetMapping("/market-data/{instrumentId}")
    public Mono<ResponseEntity<MarketDataResponse>> getMarketData(
            @PathVariable String instrumentId,
            @RequestParam(required = false) String dataSource) {
        
        log.info("Proxy market data request for instrument: {} from source: {}", instrumentId, dataSource);
        
        return proxyServiceManager.getMarketData(instrumentId, dataSource)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.debug("Proxy market data response for instrument: {}", instrumentId))
            .doOnError(error -> log.error("Proxy market data error for instrument {}: {}", instrumentId, error.getMessage()));
    }
    
    /**
     * Get market data for multiple instruments in batch.
     */
    @PostMapping("/market-data/batch")
    public Flux<MarketDataResponse> getBatchMarketData(
            @RequestBody List<String> instrumentIds,
            @RequestParam(required = false) String dataSource) {
        
        log.info("Proxy batch market data request for {} instruments from source: {}", instrumentIds.size(), dataSource);
        
        return proxyServiceManager.getBatchMarketData(instrumentIds, dataSource)
            .doOnComplete(() -> log.debug("Proxy batch market data completed for {} instruments", instrumentIds.size()))
            .doOnError(error -> log.error("Proxy batch market data error: {}", error.getMessage()));
    }
    
    /**
     * Get health status for all data sources.
     */
    @GetMapping("/health")
    public Flux<DataSourceHealth> getAllDataSourceHealth() {
        log.debug("Proxy health check request for all data sources");
        
        return proxyServiceManager.getAllDataSourceHealth()
            .doOnComplete(() -> log.debug("Proxy health check completed"))
            .doOnError(error -> log.error("Proxy health check error: {}", error.getMessage()));
    }
    
    /**
     * Get performance metrics for all data sources.
     */
    @GetMapping("/metrics")
    public Flux<DataSourceMetrics> getAllDataSourceMetrics() {
        log.debug("Proxy metrics request for all data sources");
        
        return proxyServiceManager.getAllDataSourceMetrics()
            .doOnComplete(() -> log.debug("Proxy metrics collection completed"))
            .doOnError(error -> log.error("Proxy metrics collection error: {}", error.getMessage()));
    }
    
    /**
     * Get cache statistics.
     */
    @GetMapping("/cache/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getCacheStatistics() {
        log.debug("Proxy cache statistics request");
        
        return proxyServiceManager.getCacheStatistics()
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.debug("Proxy cache statistics retrieved"))
            .doOnError(error -> log.error("Proxy cache statistics error: {}", error.getMessage()));
    }
    
    /**
     * Invalidate cache for a specific instrument.
     */
    @DeleteMapping("/cache/{instrumentId}")
    public Mono<ResponseEntity<Void>> invalidateCache(@PathVariable String instrumentId) {
        log.info("Proxy cache invalidation request for instrument: {}", instrumentId);
        
        return proxyServiceManager.invalidateCache(instrumentId)
            .then(Mono.just(ResponseEntity.ok().<Void>build()))
            .doOnSuccess(response -> log.info("Proxy cache invalidated for instrument: {}", instrumentId))
            .doOnError(error -> log.error("Proxy cache invalidation error for instrument {}: {}", 
                instrumentId, error.getMessage()));
    }
    
    /**
     * Clear all cache entries.
     */
    @DeleteMapping("/cache")
    public Mono<ResponseEntity<Void>> clearAllCache() {
        log.info("Proxy clear all cache request");
        
        return proxyServiceManager.clearAllCache()
            .then(Mono.just(ResponseEntity.ok().<Void>build()))
            .doOnSuccess(response -> log.info("Proxy all cache entries cleared"))
            .doOnError(error -> log.error("Proxy clear all cache error: {}", error.getMessage()));
    }
    
    /**
     * Get list of available data sources.
     */
    @GetMapping("/data-sources")
    public Flux<String> getAvailableDataSources() {
        log.debug("Proxy available data sources request");
        
        return proxyServiceManager.getAvailableDataSources()
            .doOnComplete(() -> log.debug("Proxy available data sources retrieved"))
            .doOnError(error -> log.error("Proxy available data sources error: {}", error.getMessage()));
    }
    
    /**
     * Check if a specific data source is available.
     */
    @GetMapping("/data-sources/{dataSourceName}/available")
    public Mono<ResponseEntity<Boolean>> isDataSourceAvailable(@PathVariable String dataSourceName) {
        log.debug("Proxy data source availability check for: {}", dataSourceName);
        
        return proxyServiceManager.isDataSourceAvailable(dataSourceName)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.debug("Proxy data source {} availability: {}", dataSourceName, response.getBody()))
            .doOnError(error -> log.error("Proxy data source availability check error for {}: {}", 
                dataSourceName, error.getMessage()));
    }
}
