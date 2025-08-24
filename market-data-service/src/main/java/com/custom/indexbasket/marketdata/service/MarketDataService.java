package com.custom.indexbasket.marketdata.service;

import com.custom.indexbasket.marketdata.dto.BasketMarketDataResponse;
import com.custom.indexbasket.marketdata.dto.MarketDataRequest;
import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Market Data Service Interface - Core market data operations
 * 
 * This service provides:
 * - Real-time market data retrieval
 * - Basket market data calculations
 * - Market analytics and risk metrics
 * - Performance monitoring
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
public interface MarketDataService {

    /**
     * Get real-time market data for instruments
     */
    Flux<MarketDataResponse> getMarketData(MarketDataRequest request);

    /**
     * Get market data for a specific instrument
     */
    Mono<MarketDataResponse> getInstrumentMarketData(String instrumentId);

    /**
     * Get basket market data and analytics
     */
    Mono<BasketMarketDataResponse> getBasketMarketData(UUID basketId);

    /**
     * Initialize market data for a new basket
     */
    Mono<Void> initializeBasketMarketData(UUID basketId, String basketCode);

    /**
     * Update market data for an existing basket
     */
    Mono<Void> updateBasketMarketData(UUID basketId, String basketCode);

    /**
     * Handle basket status changes
     */
    Mono<Void> handleBasketStatusChange(UUID basketId, String basketCode, String previousStatus, String newStatus);

    /**
     * Deactivate market data for a deleted basket
     */
    Mono<Void> deactivateBasketMarketData(UUID basketId, String basketCode);

    /**
     * Refresh market data for all active instruments
     */
    Mono<Void> refreshAllMarketData();

    /**
     * Get market data health status
     */
    Mono<MarketDataHealthStatus> getHealthStatus();

    /**
     * Market Data Health Status
     */
    record MarketDataHealthStatus(
        boolean isHealthy,
        String status,
        int activeInstruments,
        int dataSourcesConnected,
        String lastUpdateTime,
        String dataQualityScore
    ) {}
}
