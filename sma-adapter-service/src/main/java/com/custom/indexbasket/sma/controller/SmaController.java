package com.custom.indexbasket.sma.controller;

import com.custom.indexbasket.sma.model.SmaHealthStatus;
import com.custom.indexbasket.sma.model.SmaPriceData;
import com.custom.indexbasket.sma.service.SmaPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * SMA Controller
 * 
 * REST controller for SMA Refinitiv API operations.
 */
@RestController
@RequestMapping("/api/v1/sma")
@Slf4j
public class SmaController {
    
    @Autowired
    private SmaPriceService smaPriceService;
    
    /**
     * Get current price for a single symbol
     */
    @GetMapping("/prices/{symbol}")
    public Mono<ResponseEntity<SmaPriceData>> getPrice(@PathVariable String symbol) {
        log.debug("Getting price for symbol: {}", symbol);
        
        return smaPriceService.getPrice(symbol)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build())
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Successfully retrieved price for symbol: {}", symbol);
                } else {
                    log.warn("Failed to retrieve price for symbol: {}", symbol);
                }
            });
    }
    
    /**
     * Get prices for multiple symbols in batch
     */
    @PostMapping("/prices/batch")
    public Flux<SmaPriceData> getBatchPrices(@RequestBody List<String> symbols) {
        log.debug("Getting batch prices for {} symbols", symbols.size());
        
        return smaPriceService.getBatchPrices(symbols)
            .doOnNext(priceData -> log.debug("Retrieved price for symbol: {}", priceData.getSymbol()))
            .doOnError(error -> log.error("Batch price retrieval failed: {}", error.getMessage()));
    }
    
    /**
     * Start real-time price subscription
     */
    @PostMapping("/subscribe")
    public Flux<SmaPriceData> subscribeToRealTimePrices(@RequestBody List<String> symbols) {
        log.info("Starting real-time subscription for {} symbols", symbols.size());
        
        return smaPriceService.startRealTimeSubscription(symbols)
            .doOnNext(priceData -> log.debug("Real-time price update for symbol: {}", priceData.getSymbol()))
            .doOnError(error -> log.error("Real-time subscription failed: {}", error.getMessage()));
    }
    
    /**
     * Get historical price data (mock implementation)
     */
    @GetMapping("/history/{symbol}")
    public Mono<ResponseEntity<List<SmaPriceData>>> getHistoricalPrices(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1") int days) {
        
        log.debug("Getting historical prices for symbol: {} ({} days)", symbol, days);
        
        // Mock implementation - in production this would call SMA historical API
        return Mono.just(ResponseEntity.ok(List.<SmaPriceData>of()))
            .doOnNext(response -> log.debug("Historical prices retrieved for symbol: {}", symbol));
    }
    
    /**
     * Check SMA service health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return smaPriceService.checkHealth()
            .map(health -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("status", health.getStatus());
                response.put("connected", health.isConnected());
                response.put("timestamp", java.time.LocalDateTime.now());
                response.put("successRate", health.getSuccessRate());
                response.put("averageResponseTime", health.getAverageResponseTime());
                response.put("totalRequests", health.getSuccessfulRequests() + health.getFailedRequests());
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "error", "Health check failed")))
            .doOnNext(response -> log.debug("Health check completed with status: {}", response.getStatusCode()));
    }
    
    /**
     * Get service status
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<SmaHealthStatus>> status() {
        return smaPriceService.checkHealth()
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build())
            .doOnNext(response -> log.debug("Status check completed"));
    }
    
    /**
     * Get performance metrics
     */
    @GetMapping("/metrics")
    public Mono<ResponseEntity<Map<String, Object>>> metrics() {
        return smaPriceService.checkHealth()
            .flatMap(health -> smaPriceService.getCacheSize()
                .map(cacheSize -> ResponseEntity.ok(Map.of(
                    "health", health,
                    "cacheSize", cacheSize,
                    "timestamp", java.time.LocalDateTime.now()
                )))
            )
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Metrics retrieval failed")));
    }
    
    /**
     * Clear cache for a specific symbol
     */
    @DeleteMapping("/cache/{symbol}")
    public Mono<ResponseEntity<Map<String, Object>>> clearCache(@PathVariable String symbol) {
        return smaPriceService.clearCache(symbol)
            .map(success -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("symbol", symbol);
                response.put("cleared", success);
                response.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Cache clear failed")));
    }
    
    /**
     * Clear all cache
     */
    @DeleteMapping("/cache")
    public Mono<ResponseEntity<Map<String, Object>>> clearAllCache() {
        return smaPriceService.clearAllCache()
            .map(count -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("clearedEntries", count);
                response.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Cache clear failed")));
    }
}
