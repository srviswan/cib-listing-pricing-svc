package com.custom.indexbasket.marketdata.controller;

import com.custom.indexbasket.marketdata.dto.BasketMarketDataResponse;
import com.custom.indexbasket.marketdata.dto.MarketDataRequest;
import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * Market Data Controller - REST endpoints for market data operations
 * 
 * This controller provides:
 * - Real-time market data retrieval
 * - Basket market data and analytics
 * - Health monitoring and status
 * - Performance metrics
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/market-data")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataController.class);
    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /**
     * Get real-time market data for multiple instruments
     */
    @PostMapping("/instruments")
    public Mono<ResponseEntity<Flux<MarketDataResponse>>> getMarketData(@Valid @RequestBody MarketDataRequest request) {
        log.info("📊 POST /api/v1/market-data/instruments - Retrieving market data for {} instruments", 
            request.getInstrumentIds().size());
        
        Flux<MarketDataResponse> marketData = marketDataService.getMarketData(request);
        
        return Mono.just(ResponseEntity.ok(marketData))
            .doOnSuccess(response -> log.info("✅ Market data request processed successfully"))
            .doOnError(error -> log.error("❌ Failed to process market data request: {}", error.getMessage()));
    }

    /**
     * Get market data for a specific instrument
     */
    @GetMapping("/instruments/{instrumentId}")
    public Mono<ResponseEntity<MarketDataResponse>> getInstrumentMarketData(@PathVariable String instrumentId) {
        log.info("🔍 GET /api/v1/market-data/instruments/{} - Retrieving instrument market data", instrumentId);
        
        return marketDataService.getInstrumentMarketData(instrumentId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Instrument market data retrieved for: {}", instrumentId);
                } else {
                    log.warn("⚠️ Instrument market data not found for: {}", instrumentId);
                }
            })
            .doOnError(error -> log.error("❌ Failed to retrieve instrument market data for {}: {}", instrumentId, error.getMessage()));
    }

    /**
     * Get basket market data and analytics
     */
    @GetMapping("/baskets/{basketId}")
    public Mono<ResponseEntity<BasketMarketDataResponse>> getBasketMarketData(@PathVariable UUID basketId) {
        log.info("📊 GET /api/v1/market-data/baskets/{} - Retrieving basket market data", basketId);
        
        return marketDataService.getBasketMarketData(basketId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Basket market data retrieved for: {}", basketId);
                } else {
                    log.warn("⚠️ Basket market data not found for: {}", basketId);
                }
            })
            .doOnError(error -> log.error("❌ Failed to retrieve basket market data for {}: {}", basketId, error.getMessage()));
    }

    /**
     * Refresh all market data
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<String>> refreshAllMarketData() {
        log.info("🔄 POST /api/v1/market-data/refresh - Refreshing all market data");
        
        return marketDataService.refreshAllMarketData()
            .then(Mono.just(ResponseEntity.ok("Market data refresh initiated successfully")))
            .doOnSuccess(response -> log.info("✅ Market data refresh completed"))
            .doOnError(error -> log.error("❌ Failed to refresh market data: {}", error.getMessage()));
    }

    /**
     * Get market data service health status
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<MarketDataService.MarketDataHealthStatus>> getHealthStatus() {
        log.debug("🏥 GET /api/v1/market-data/health - Checking service health");
        
        return marketDataService.getHealthStatus()
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.debug("✅ Health check completed"))
            .doOnError(error -> log.error("❌ Health check failed: {}", error.getMessage()));
    }

    /**
     * Get market data statistics
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<MarketDataStats>> getMarketDataStats() {
        log.info("📈 GET /api/v1/market-data/stats - Retrieving market data statistics");
        
        // This would typically aggregate data from multiple sources
        MarketDataStats stats = new MarketDataStats(
            "Market Data Service Statistics",
            System.currentTimeMillis(),
            "ACTIVE",
            1000, // Active instruments
            5,    // Data sources
            99.5, // Data quality score
            "USD" // Base currency
        );
        
        return Mono.just(ResponseEntity.ok(stats))
            .doOnSuccess(response -> log.info("✅ Market data statistics retrieved"))
            .doOnError(error -> log.error("❌ Failed to retrieve market data statistics: {}", error.getMessage()));
    }

    /**
     * Market Data Statistics DTO
     */
    public record MarketDataStats(
        String serviceName,
        long timestamp,
        String status,
        int activeInstruments,
        int dataSources,
        double dataQualityScore,
        String baseCurrency
    ) {}
}
