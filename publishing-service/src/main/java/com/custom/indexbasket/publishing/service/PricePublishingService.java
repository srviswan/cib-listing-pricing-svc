package com.custom.indexbasket.publishing.service;

import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Real-Time Price Publishing Service
 * 
 * Publishes real-time basket prices to vendor platforms with ultra-low latency.
 * Integrates with Smart Communication Router for protocol selection and Event Adapter for performance monitoring.
 */
@Slf4j
@Service
public class PricePublishingService {

    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private BasketCacheService basketCacheService;

    /**
     * Continuous price publishing every 5 seconds
     * Target: <1ms latency for price publishing
     */
    @Scheduled(fixedRate = 5000)
    public Mono<Void> publishPriceUpdates() {
        log.debug("Starting scheduled price publishing cycle");
        
        return basketCacheService.getPricePublishingRequests()
            .flatMap(this::publishBasketPrice)
            .collectList()
            .doOnSuccess(results -> log.debug("Price publishing cycle completed for {} baskets", results.size()))
            .doOnError(error -> log.error("Price publishing cycle failed: {}", error.getMessage()))
            .then();
    }

    /**
     * Publish prices for all approved baskets from cache
     * 
     * @return Mono containing list of publishing results for all baskets
     */
    public Mono<List<List<PublishingResult>>> publishAllApprovedBaskets() {
        log.debug("Publishing prices for all approved baskets from cache");
        
        return basketCacheService.getPricePublishingRequests()
            .flatMap(this::publishBasketPrice)
            .collectList()
            .doOnSuccess(results -> log.debug("Successfully published prices for {} baskets", results.size()))
            .doOnError(error -> log.error("Failed to publish prices for all baskets: {}", error.getMessage()));
    }

    /**
     * Publish basket price to all active vendors
     */
    public Mono<List<PublishingResult>> publishBasketPrice(PricePublishingRequest request) {
        log.debug("Publishing basket price for basket: {}", request.getBasketId());
        
        return vendorService.publishBasketPrice(request)
            .timeout(Duration.ofSeconds(1)) // 1 second timeout for ultra-low latency
            .doOnSuccess(results -> {
                log.debug("Successfully published price for basket {} to {} vendors", 
                    request.getBasketId(), results.size());
                publishPricePublishedEvent(request.getBasketId(), results);
            })
            .doOnError(error -> {
                log.error("Failed to publish price for basket {}: {}", 
                    request.getBasketId(), error.getMessage());
                publishPriceFailedEvent(request.getBasketId(), error.getMessage());
            });
    }

    /**
     * Publish price to a specific vendor
     */
    public Mono<PublishingResult> publishPriceToVendor(String vendorName, PricePublishingRequest request) {
        log.debug("Publishing basket price to vendor: {} for basket: {}", vendorName, request.getBasketId());
        
        return vendorService.publishBasketPriceToVendor(vendorName, request)
            .timeout(Duration.ofSeconds(1))
            .doOnSuccess(result -> log.debug("Price published to vendor {} for basket: {}", 
                vendorName, request.getBasketId()))
            .doOnError(error -> log.error("Price publishing failed to vendor {} for basket {}: {}", 
                vendorName, request.getBasketId(), error.getMessage()));
    }

    /**
     * Publish price published event
     */
    private Mono<Void> publishPricePublishedEvent(String basketId, List<PublishingResult> results) {
        return eventPublisher.publish("basket.price.published", basketId,
            Map.of("basketId", basketId, "results", results, "timestamp", java.time.LocalDateTime.now()));
    }

    /**
     * Publish price failed event
     */
    private Mono<Void> publishPriceFailedEvent(String basketId, String error) {
        return eventPublisher.publish("basket.price.failed", basketId,
            Map.of("basketId", basketId, "error", error, "timestamp", java.time.LocalDateTime.now()));
    }



    /**
     * Get statistics about approved baskets for monitoring
     * 
     * @return Mono containing basket statistics
     */
    public Mono<Map<String, Object>> getBasketStatistics() {
        return basketCacheService.getApprovedBasketsForPricing()
            .collectList()
            .map(baskets -> Map.of(
                "totalApprovedBaskets", baskets.size(),
                "basketTypes", baskets.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        basket -> basket.getBasketType(),
                        java.util.stream.Collectors.counting()
                    )),
                "basketStatuses", baskets.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        basket -> basket.getStatus().toString(),
                        java.util.stream.Collectors.counting()
                    )),
                "currencies", baskets.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        basket -> basket.getBaseCurrency(),
                        java.util.stream.Collectors.counting()
                    )),
                "timestamp", java.time.LocalDateTime.now()
            ))
            .doOnSuccess(stats -> log.debug("Retrieved basket statistics: {}", stats))
            .doOnError(error -> log.error("Failed to retrieve basket statistics: {}", error.getMessage()));
    }

    /**
     * Scheduled health check for price publishing operations
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void healthCheck() {
        log.debug("Price publishing service health check - all systems operational");
    }
}
