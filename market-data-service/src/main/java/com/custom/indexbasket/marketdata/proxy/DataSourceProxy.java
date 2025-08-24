package com.custom.indexbasket.marketdata.proxy;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceHealth;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceMetrics;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Core interface for data source proxy services.
 * Provides abstraction layer for external market data providers.
 */
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
