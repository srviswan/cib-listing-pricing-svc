package com.custom.indexbasket.integration.controller;

import com.custom.indexbasket.integration.model.BasketPrice;
import com.custom.indexbasket.integration.model.IntegrationStatus;
import com.custom.indexbasket.integration.service.IntegrationHealthService;
import com.custom.indexbasket.integration.service.IntegrationOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Integration Controller
 * 
 * REST controller for integration orchestration operations.
 */
@RestController
@RequestMapping("/api/v1/integration")
@Slf4j
public class IntegrationController {
    
    @Autowired
    private IntegrationOrchestrationService orchestrationService;
    
    @Autowired
    private IntegrationHealthService healthService;
    
    /**
     * Calculate and publish basket price
     */
    @PostMapping("/baskets/{basketId}/calculate-publish")
    public Mono<ResponseEntity<BasketPrice>> calculateAndPublishBasketPrice(@PathVariable String basketId) {
        log.debug("Calculating and publishing basket price for: {}", basketId);
        
        return orchestrationService.calculateAndPublishBasketPrice(basketId)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            .doOnSuccess(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully calculated and published basket price for: {}", basketId);
                } else {
                    log.warn("Failed to calculate and publish basket price for: {}", basketId);
                }
            });
    }
    
    /**
     * Get cached basket price
     */
    @GetMapping("/baskets/{basketId}/price")
    public Mono<ResponseEntity<BasketPrice>> getCachedBasketPrice(@PathVariable String basketId) {
        log.debug("Getting cached basket price for: {}", basketId);
        
        return orchestrationService.getCachedBasketPrice(basketId)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
            .doOnSuccess(response -> log.debug("Retrieved cached basket price for: {}", basketId));
    }
    
    /**
     * Start real-time publishing for a basket
     */
    @PostMapping("/baskets/{basketId}/start-publishing")
    public Mono<ResponseEntity<Map<String, Object>>> startRealTimePublishing(@PathVariable String basketId) {
        log.info("Starting real-time publishing for basket: {}", basketId);
        
        return orchestrationService.startRealTimePublishing(basketId)
            .map(success -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("basketId", basketId);
                response.put("status", success ? "STARTED" : "FAILED");
                response.put("message", success ? "Real-time publishing started" : "Failed to start real-time publishing");
                response.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("basketId", basketId, "status", "FAILED", "error", "Failed to start real-time publishing")))
            .doOnSuccess(response -> log.info("Real-time publishing status for basket {}: {}", 
                basketId, response.getStatusCode()));
    }
    
    /**
     * Stop real-time publishing for a basket
     */
    @PostMapping("/baskets/{basketId}/stop-publishing")
    public Mono<ResponseEntity<Map<String, Object>>> stopRealTimePublishing(@PathVariable String basketId) {
        log.info("Stopping real-time publishing for basket: {}", basketId);
        
        return orchestrationService.stopRealTimePublishing(basketId)
            .map(success -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("basketId", basketId);
                response.put("status", "STOPPED");
                response.put("message", "Real-time publishing stopped");
                response.put("timestamp", java.time.LocalDateTime.now());
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("basketId", basketId, "status", "FAILED", "error", "Failed to stop real-time publishing")))
            .doOnSuccess(response -> log.info("Real-time publishing stopped for basket: {}", basketId));
    }
    
    /**
     * Get all active baskets
     */
    @GetMapping("/baskets/active")
    public Flux<String> getActiveBaskets() {
        log.debug("Getting active baskets");
        
        return orchestrationService.getActiveBaskets()
            .doOnNext(basketId -> log.debug("Active basket: {}", basketId))
            .doOnError(error -> log.error("Failed to get active baskets: {}", error.getMessage()));
    }
    
    /**
     * Publish all active basket prices
     */
    @PostMapping("/baskets/publish-all")
    public Flux<Map<String, Object>> publishAllActiveBasketPrices() {
        log.info("Publishing all active basket prices");
        
        return orchestrationService.getActiveBaskets()
            .flatMap(basketId -> orchestrationService.calculateAndPublishBasketPrice(basketId)
                .map(price -> {
                    Map<String, Object> response = new java.util.HashMap<>();
                    response.put("basketId", basketId);
                    response.put("status", "SUCCESS");
                    response.put("price", price.getPrice());
                    response.put("currency", price.getCurrency());
                    response.put("timestamp", java.time.LocalDateTime.now());
                    return response;
                })
                .onErrorReturn(Map.of("basketId", basketId, "status", "FAILED", "error", "Failed to calculate and publish price", "timestamp", java.time.LocalDateTime.now()))
            )
            .doOnNext(result -> log.debug("Published basket price: {}", result))
            .doOnError(error -> log.error("Error publishing basket prices: {}", error.getMessage()));
    }
    
    /**
     * Get integration health status
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<IntegrationStatus>> getIntegrationHealth() {
        log.debug("Getting integration health status");
        
        return healthService.checkIntegrationHealth()
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build())
            .doOnSuccess(response -> log.debug("Integration health check completed with status: {}", 
                response.getStatusCode()));
    }
    
    /**
     * Get service status by name
     */
    @GetMapping("/services/{serviceName}/status")
    public Mono<ResponseEntity<IntegrationStatus.ServiceStatus>> getServiceStatus(@PathVariable String serviceName) {
        log.debug("Getting status for service: {}", serviceName);
        
        return healthService.getServiceStatus(serviceName)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build())
            .doOnSuccess(response -> log.debug("Service status retrieved for: {}", serviceName));
    }
    
    /**
     * Get cache statistics
     */
    @GetMapping("/cache/statistics")
    public Mono<ResponseEntity<Map<String, Object>>> getCacheStatistics() {
        log.debug("Getting cache statistics");
        
        return orchestrationService.getCacheStatistics()
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get cache statistics")))
            .doOnSuccess(response -> log.debug("Cache statistics retrieved"));
    }
    
    /**
     * Scheduled task to publish all active basket prices
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void scheduledPublishActiveBasketPrices() {
        log.debug("Running scheduled basket price publishing");
        
        orchestrationService.getActiveBaskets()
            .flatMap(basketId -> orchestrationService.calculateAndPublishBasketPrice(basketId)
                .doOnSuccess(price -> log.debug("Scheduled publishing completed for basket: {}", basketId))
                .doOnError(error -> log.warn("Scheduled publishing failed for basket {}: {}", 
                    basketId, error.getMessage()))
            )
            .subscribe(
                price -> log.debug("Scheduled price published for basket: {}", price.getBasketId()),
                error -> log.error("Scheduled basket price publishing failed: {}", error.getMessage())
            );
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/actuator/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return healthService.checkIntegrationHealth()
            .map(status -> {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("status", status.getStatus());
                response.put("healthy", status.isHealthy());
                response.put("timestamp", java.time.LocalDateTime.now());
                
                Map<String, Object> services = new java.util.HashMap<>();
                services.put("smaAdapter", status.getSmaAdapter() != null ? status.getSmaAdapter().getStatus() : "UNKNOWN");
                services.put("fixAdapter", status.getFixAdapter() != null ? status.getFixAdapter().getStatus() : "UNKNOWN");
                services.put("basketCore", status.getBasketCore() != null ? status.getBasketCore().getStatus() : "UNKNOWN");
                services.put("marketData", status.getMarketData() != null ? status.getMarketData().getStatus() : "UNKNOWN");
                response.put("services", services);
                
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "error", "Health check failed")))
            .doOnSuccess(response -> log.debug("Health check completed with status: {}", response.getStatusCode()));
    }
}
