package com.custom.indexbasket.publishing.service;

import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import com.custom.indexbasket.publishing.proxy.VendorProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Enhanced Vendor Service Implementation
 * 
 * Extends the existing VendorService to integrate with FIX Bloomberg Adapter.
 * Falls back to existing mock implementation for non-Bloomberg vendors.
 */
@Service
@Slf4j
public class EnhancedVendorServiceImpl extends VendorServiceImpl {
    
    private final WebClient fixWebClient;
    
    @Autowired
    public EnhancedVendorServiceImpl(VendorProxyService proxyService, 
                                   @Autowired(required = false) WebClient.Builder webClientBuilder) {
        super(proxyService);
        
        // Initialize FIX WebClient if builder is available
        if (webClientBuilder != null) {
            this.fixWebClient = webClientBuilder
                .baseUrl("http://localhost:8085/fix-adapter")
                .build();
        } else {
            this.fixWebClient = WebClient.builder()
                .baseUrl("http://localhost:8085/fix-adapter")
                .build();
        }
    }
    
    @Override
    public Mono<PublishingResult> publishBasketPriceToVendor(String vendorName, PricePublishingRequest request) {
        if ("BLOOMBERG".equals(vendorName.toUpperCase())) {
            return publishToBloombergViaFix(request);
        }
        
        // Fall back to existing mock implementation for other vendors
        return super.publishBasketPriceToVendor(vendorName, request);
    }
    
    @Override
    public Mono<PublishingResult> publishBasketListingToVendor(String vendorName, BasketListingRequest request) {
        if ("BLOOMBERG".equals(vendorName.toUpperCase())) {
            return publishBasketListingToBloombergViaFix(request);
        }
        
        // Fall back to existing mock implementation for other vendors
        return super.publishBasketListingToVendor(vendorName, request);
    }
    
    /**
     * Publish price to Bloomberg via FIX adapter
     */
    private Mono<PublishingResult> publishToBloombergViaFix(PricePublishingRequest request) {
        log.debug("Publishing price to Bloomberg via FIX for basket: {}", request.getBasketId());
        
        // Create FIX price publishing request
        Map<String, Object> fixRequest = Map.of(
            "basketId", request.getBasketId(),
            "symbol", request.getBasketCode(),
            "price", request.getPrice(),
            "currency", request.getCurrency(),
            "timestamp", request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now(),
            "volume", request.getVolume() != null ? request.getVolume() : 1000,
            "exchange", "NYSE",
            "marketDataType", "BASKET_PRICE"
        );
        
        return fixWebClient
            .post()
            .uri("/api/v1/fix/publish/price")
            .bodyValue(fixRequest)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                boolean success = response.get("success") != null && (Boolean) response.get("success");
                return PublishingResult.builder()
                    .vendor("BLOOMBERG")
                    .basketId(request.getBasketId())
                    .status(success ? "SUCCESS" : "FAILED")
                    .errorMessage(success ? null : "FIX publishing failed")
                    .timestamp(LocalDateTime.now())
                    .build();
            })
            .onErrorReturn(PublishingResult.builder()
                .vendor("BLOOMBERG")
                .basketId(request.getBasketId())
                .status("FAILED")
                .errorMessage("FIX publishing failed")
                .timestamp(LocalDateTime.now())
                .build())
            .doOnSuccess(result -> {
                if ("SUCCESS".equals(result.getStatus())) {
                    log.info("Successfully published price to Bloomberg via FIX for basket: {}", request.getBasketId());
                } else {
                    log.warn("Failed to publish price to Bloomberg via FIX for basket: {}", request.getBasketId());
                }
            })
            .doOnError(error -> log.error("Error publishing price to Bloomberg via FIX for basket {}: {}", 
                request.getBasketId(), error.getMessage()));
    }
    
    /**
     * Publish basket listing to Bloomberg via FIX adapter
     */
    private Mono<PublishingResult> publishBasketListingToBloombergViaFix(BasketListingRequest request) {
        log.debug("Publishing basket listing to Bloomberg via FIX for basket: {}", request.getBasketId());
        
        // Create FIX basket listing request
        Map<String, Object> fixRequest = Map.of(
            "basketId", request.getBasketId(),
            "symbol", request.getBasketCode(),
            "price", 100.0, // Default initial price
            "currency", request.getBaseCurrency() != null ? request.getBaseCurrency() : "USD",
            "timestamp", LocalDateTime.now(),
            "volume", 1000,
            "exchange", "NYSE",
            "marketDataType", "BASKET_LISTING"
        );
        
        return fixWebClient
            .post()
            .uri("/api/v1/fix/publish/basket")
            .bodyValue(fixRequest)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                boolean success = response.get("success") != null && (Boolean) response.get("success");
                return PublishingResult.builder()
                    .vendor("BLOOMBERG")
                    .basketId(request.getBasketId())
                    .status(success ? "SUCCESS" : "FAILED")
                    .errorMessage(success ? null : "FIX listing failed")
                    .timestamp(LocalDateTime.now())
                    .build();
            })
            .onErrorReturn(PublishingResult.builder()
                .vendor("BLOOMBERG")
                .basketId(request.getBasketId())
                .status("FAILED")
                .errorMessage("FIX listing failed")
                .timestamp(LocalDateTime.now())
                .build())
            .doOnSuccess(result -> {
                if ("SUCCESS".equals(result.getStatus())) {
                    log.info("Successfully published basket listing to Bloomberg via FIX for basket: {}", request.getBasketId());
                } else {
                    log.warn("Failed to publish basket listing to Bloomberg via FIX for basket: {}", request.getBasketId());
                }
            })
            .doOnError(error -> log.error("Error publishing basket listing to Bloomberg via FIX for basket {}: {}", 
                request.getBasketId(), error.getMessage()));
    }
}
