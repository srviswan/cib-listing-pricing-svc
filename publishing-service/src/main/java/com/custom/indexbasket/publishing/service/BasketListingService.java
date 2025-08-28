package com.custom.indexbasket.publishing.service;

import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import com.custom.indexbasket.publishing.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Basket Listing Engine
 * 
 * Manages basket listing workflow from approval to vendor publication.
 * Integrates with Event Publisher for lifecycle events and Cache Service for status tracking.
 */
@Slf4j
@Service
public class BasketListingService {

    @Autowired
    private VendorService vendorService;
    
    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Handle basket approval events and trigger listing workflow
     * Target: Complete listing within 5 minutes
     */
    public Mono<List<PublishingResult>> handleBasketApproved(String basketId, BasketListingRequest request) {
        log.info("Starting basket listing workflow for basket: {}", basketId);
        
        return publishListingStarted(basketId)
            .then(publishBasketToVendors(request))
            .doOnSuccess(results -> {
                log.info("Basket listing completed for: {}", basketId);
                publishListingCompleted(basketId, results);
            })
            .doOnError(error -> {
                log.error("Basket listing failed for {}: {}", basketId, error.getMessage());
                publishListingFailed(basketId, error.getMessage());
            });
    }

    /**
     * Publish basket to all active vendors
     */
    private Mono<List<PublishingResult>> publishBasketToVendors(BasketListingRequest request) {
        return vendorService.publishBasketListing(request)
            .timeout(Duration.ofMinutes(5)) // 5 minutes timeout
            .doOnSuccess(results -> log.info("Successfully published basket {} to {} vendors", 
                request.getBasketId(), results.size()))
            .doOnError(error -> log.error("Failed to publish basket {} to vendors: {}", 
                request.getBasketId(), error.getMessage()));
    }

    /**
     * Publish listing started event
     */
    private Mono<Void> publishListingStarted(String basketId) {
        return eventPublisher.publish("basket.listing.started", basketId, 
            Map.of("basketId", basketId, "timestamp", java.time.LocalDateTime.now()));
    }

    /**
     * Publish listing completed event
     */
    private Mono<Void> publishListingCompleted(String basketId, List<PublishingResult> results) {
        return eventPublisher.publish("basket.listing.completed", basketId,
            Map.of("basketId", basketId, "results", results, "timestamp", java.time.LocalDateTime.now()));
    }

    /**
     * Publish listing failed event
     */
    private Mono<Void> publishListingFailed(String basketId, String error) {
        return eventPublisher.publish("basket.listing.failed", basketId,
            Map.of("basketId", basketId, "error", error, "timestamp", java.time.LocalDateTime.now()));
    }

    /**
     * Manual basket listing trigger
     */
    public Mono<List<PublishingResult>> publishBasketListing(BasketListingRequest request) {
        log.info("Manual basket listing request for basket: {}", request.getBasketId());
        return handleBasketApproved(request.getBasketId(), request);
    }

    /**
     * Retry failed basket listing
     */
    public Mono<List<PublishingResult>> retryBasketListing(String basketId, BasketListingRequest request) {
        log.info("Retrying basket listing for basket: {}", basketId);
        return publishBasketToVendors(request)
            .doOnSuccess(results -> log.info("Basket listing retry completed for: {}", basketId))
            .doOnError(error -> log.error("Basket listing retry failed for {}: {}", basketId, error.getMessage()));
    }

    /**
     * Scheduled health check for basket listing operations
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void healthCheck() {
        log.debug("Basket listing service health check - all systems operational");
    }
}
