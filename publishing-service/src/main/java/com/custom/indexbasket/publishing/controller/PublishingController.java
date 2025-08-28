package com.custom.indexbasket.publishing.controller;

import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import com.custom.indexbasket.publishing.service.BasketListingService;
import com.custom.indexbasket.publishing.service.PricePublishingService;
import com.custom.indexbasket.publishing.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Publishing Service REST Controller
 * 
 * Provides REST API endpoints for basket listing and price publishing operations.
 * Uses same security patterns as Market Data Service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/publishing")
public class PublishingController {

    @Autowired
    private BasketListingService basketListingService;
    
    @Autowired
    private PricePublishingService pricePublishingService;
    
    @Autowired
    private VendorService vendorService;

    /**
     * Start basket listing workflow
     */
    @PostMapping("/basket/{basketId}/list")
    public Mono<ResponseEntity<Map<String, Object>>> startBasketListing(
            @PathVariable String basketId,
            @Valid @RequestBody BasketListingRequest request) {
        
        log.info("Basket listing request for basket: {}", basketId);
        
        return basketListingService.publishBasketListing(request)
            .map(results -> {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Basket listing started successfully",
                    "basketId", basketId,
                    "vendors", results.size(),
                    "results", results
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Basket listing failed for basket {}: {}", basketId, error.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Basket listing failed: " + error.getMessage(),
                    "basketId", basketId
                );
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }

    /**
     * Get publishing status for a basket
     */
    @GetMapping("/basket/{basketId}/status")
    public Mono<ResponseEntity<Map<String, Object>>> getPublishingStatus(@PathVariable String basketId) {
        log.debug("Getting publishing status for basket: {}", basketId);
        
        // TODO: Implement status retrieval from database
        Map<String, Object> status = Map.of(
            "basketId", basketId,
            "status", "IN_PROGRESS",
            "vendors", vendorService.getActiveVendors(),
            "timestamp", java.time.LocalDateTime.now()
        );
        
        return Mono.just(ResponseEntity.ok(status));
    }

    /**
     * Publish basket price
     */
    @PostMapping("/basket/{basketId}/price")
    public Mono<ResponseEntity<Map<String, Object>>> publishBasketPrice(
            @PathVariable String basketId,
            @Valid @RequestBody PricePublishingRequest request) {
        
        log.info("Price publishing request for basket: {}", basketId);
        
        return pricePublishingService.publishBasketPrice(request)
            .map(results -> {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Basket price published successfully",
                    "basketId", basketId,
                    "vendors", results.size(),
                    "results", results
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Price publishing failed for basket {}: {}", basketId, error.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Price publishing failed: " + error.getMessage(),
                    "basketId", basketId
                );
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }

    /**
     * Publish basket price to specific vendor
     */
    @PostMapping("/basket/{basketId}/price/{vendor}")
    public Mono<ResponseEntity<Map<String, Object>>> publishBasketPriceToVendor(
            @PathVariable String basketId,
            @PathVariable String vendor,
            @Valid @RequestBody PricePublishingRequest request) {
        
        log.info("Price publishing request for basket: {} to vendor: {}", basketId, vendor);
        
        return pricePublishingService.publishPriceToVendor(vendor, request)
            .map(result -> {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Basket price published to vendor successfully",
                    "basketId", basketId,
                    "vendor", vendor,
                    "result", result
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Price publishing failed for basket {} to vendor {}: {}", 
                    basketId, vendor, error.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Price publishing failed: " + error.getMessage(),
                    "basketId", basketId,
                    "vendor", vendor
                );
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }

    /**
     * Get vendor health status
     */
    @GetMapping("/vendors/health")
    public Mono<ResponseEntity<Map<String, Object>>> getVendorHealth() {
        log.debug("Getting vendor health status");
        
        List<String> activeVendors = vendorService.getActiveVendors();
        Map<String, Object> healthStatus = Map.of(
            "timestamp", java.time.LocalDateTime.now(),
            "activeVendors", activeVendors.size(),
            "vendors", activeVendors,
            "overallStatus", "HEALTHY"
        );
        
        return Mono.just(ResponseEntity.ok(healthStatus));
    }

    /**
     * Retry failed basket listing
     */
    @PostMapping("/basket/{basketId}/retry")
    public Mono<ResponseEntity<Map<String, Object>>> retryBasketListing(
            @PathVariable String basketId,
            @Valid @RequestBody BasketListingRequest request) {
        
        log.info("Retry basket listing request for basket: {}", basketId);
        
        return basketListingService.retryBasketListing(basketId, request)
            .map(results -> {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Basket listing retry completed successfully",
                    "basketId", basketId,
                    "vendors", results.size(),
                    "results", results
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Basket listing retry failed for basket {}: {}", basketId, error.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Basket listing retry failed: " + error.getMessage(),
                    "basketId", basketId
                );
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }

    /**
     * Get basket statistics for monitoring
     */
    @GetMapping("/baskets/statistics")
    public Mono<ResponseEntity<Map<String, Object>>> getBasketStatistics() {
        log.debug("Getting basket statistics");
        
        return pricePublishingService.getBasketStatistics()
            .map(stats -> ResponseEntity.ok(stats))
            .onErrorResume(error -> {
                log.error("Failed to get basket statistics: {}", error.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Failed to get basket statistics: " + error.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
                );
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Manually trigger price publishing for all approved baskets
     */
    @PostMapping("/baskets/publish-prices")
    public Mono<ResponseEntity<Map<String, Object>>> publishAllApprovedBaskets() {
        log.info("Manual trigger for price publishing of all approved baskets");
        
        return pricePublishingService.publishAllApprovedBaskets()
            .map(results -> {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Price publishing completed for all approved baskets",
                    "totalBaskets", results.size(),
                    "totalVendors", results.stream().mapToInt(List::size).sum(),
                    "timestamp", java.time.LocalDateTime.now()
                );
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to publish prices for all approved baskets: {}", error.getMessage());
                Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Failed to publish prices for all approved baskets: " + error.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
                );
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "Publishing Service",
            "timestamp", java.time.LocalDateTime.now(),
            "activeVendors", vendorService.getActiveVendors().size()
        );
        
        return Mono.just(ResponseEntity.ok(health));
    }
}
