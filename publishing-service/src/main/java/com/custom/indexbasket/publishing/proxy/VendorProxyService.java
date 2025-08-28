package com.custom.indexbasket.publishing.proxy;

import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Random;

/**
 * Vendor Proxy Service for development and testing
 * 
 * Provides mock vendor responses while working on vendor onboarding.
 * Simulates realistic latency, error rates, and response formats.
 */
@Slf4j
@Service
public class VendorProxyService {

    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    private final Random random = new Random();
    
    // Configuration properties
    @Value("${vendor.proxy.bloomberg.mock-error-rate:0.05}")
    private double bloombergErrorRate;
    
    @Value("${vendor.proxy.refinitiv.mock-error-rate:0.03}")
    private double refinitivErrorRate;
    
    @Value("${vendor.proxy.generic.mock-error-rate:0.10}")
    private double genericErrorRate;

    /**
     * Mock Bloomberg basket listing
     */
    public Mono<PublishingResult> mockBloombergListing(BasketListingRequest request) {
        return Mono.defer(() -> {
            log.debug("Mock Bloomberg listing for basket: {}", request.getBasketId());
            
            // Simulate realistic Bloomberg API response
            Duration responseTime = Duration.ofMillis(150 + random.nextInt(100)); // 150-250ms
            
            // Check if we should simulate an error
            if (random.nextDouble() < bloombergErrorRate) {
                log.debug("Simulating Bloomberg listing error for basket: {}", request.getBasketId());
                return Mono.just(PublishingResult.failed("BLOOMBERG", request.getBasketId(), 
                    "Mock Bloomberg API error", responseTime));
            }
            
            PublishingResult result = PublishingResult.success("BLOOMBERG", request.getBasketId(), 
                "BB_" + System.currentTimeMillis(), responseTime);
            result.setOperationType("LISTING");
            
            // Cache the result
            String key = String.format("bloomberg:listing:%s", request.getBasketId());
            return cacheService.put(key, result, Duration.ofMinutes(30))
                .then(eventPublisher.publish("vendor.listing.completed", request.getBasketId(), result))
                .thenReturn(result);
        });
    }

    /**
     * Mock Bloomberg price publishing
     */
    public Mono<PublishingResult> mockBloombergPricePublishing(PricePublishingRequest request) {
        return Mono.defer(() -> {
            log.debug("Mock Bloomberg price publishing for basket: {}", request.getBasketId());
            
            // Simulate ultra-low latency Bloomberg price publishing
            Duration responseTime = Duration.ofNanos((500 + random.nextInt(500)) * 1000); // 0.5-1ms
            
            // Check if we should simulate an error
            if (random.nextDouble() < bloombergErrorRate) {
                log.debug("Simulating Bloomberg price publishing error for basket: {}", request.getBasketId());
                return Mono.just(PublishingResult.failed("BLOOMBERG", request.getBasketId(), 
                    "Mock Bloomberg price API error", responseTime));
            }
            
            PublishingResult result = PublishingResult.success("BLOOMBERG", request.getBasketId(), 
                "BB_PRICE_" + System.currentTimeMillis(), responseTime);
            result.setOperationType("PRICE_PUBLISHING");
            
            // Cache the result
            String key = String.format("bloomberg:price:%s:%s", request.getBasketId(), request.getTimestamp());
            return cacheService.put(key, result, Duration.ofMinutes(5))
                .then(eventPublisher.publish("vendor.price.published", request.getBasketId(), result))
                .thenReturn(result);
        });
    }

    /**
     * Mock Refinitiv basket listing
     */
    public Mono<PublishingResult> mockRefinitivListing(BasketListingRequest request) {
        return Mono.defer(() -> {
            log.debug("Mock Refinitiv listing for basket: {}", request.getBasketId());
            
            Duration responseTime = Duration.ofMillis(200 + random.nextInt(100)); // 200-300ms
            
            if (random.nextDouble() < refinitivErrorRate) {
                log.debug("Simulating Refinitiv listing error for basket: {}", request.getBasketId());
                return Mono.just(PublishingResult.failed("REFINITIV", request.getBasketId(), 
                    "Mock Refinitiv API error", responseTime));
            }
            
            PublishingResult result = PublishingResult.success("REFINITIV", request.getBasketId(), 
                "REF_" + System.currentTimeMillis(), responseTime);
            result.setOperationType("LISTING");
            
            String key = String.format("refinitiv:listing:%s", request.getBasketId());
            return cacheService.put(key, result, Duration.ofMinutes(30))
                .then(eventPublisher.publish("vendor.listing.completed", request.getBasketId(), result))
                .thenReturn(result);
        });
    }

    /**
     * Mock Refinitiv price publishing
     */
    public Mono<PublishingResult> mockRefinitivPricePublishing(PricePublishingRequest request) {
        return Mono.defer(() -> {
            log.debug("Mock Refinitiv price publishing for basket: {}", request.getBasketId());
            
            Duration responseTime = Duration.ofNanos((800 + random.nextInt(700)) * 1000); // 0.8-1.5ms
            
            if (random.nextDouble() < refinitivErrorRate) {
                log.debug("Simulating Refinitiv price publishing error for basket: {}", request.getBasketId());
                return Mono.just(PublishingResult.failed("REFINITIV", request.getBasketId(), 
                    "Mock Refinitiv price API error", responseTime));
            }
            
            PublishingResult result = PublishingResult.success("REFINITIV", request.getBasketId(), 
                "REF_PRICE_" + System.currentTimeMillis(), responseTime);
            result.setOperationType("PRICE_PUBLISHING");
            
            String key = String.format("refinitiv:price:%s:%s", request.getBasketId(), request.getTimestamp());
            return cacheService.put(key, result, Duration.ofMinutes(5))
                .then(eventPublisher.publish("vendor.price.published", request.getBasketId(), result))
                .thenReturn(result);
        });
    }

    /**
     * Generic vendor proxy for testing different scenarios
     */
    public Mono<PublishingResult> mockGenericVendor(String vendorName, BasketListingRequest request) {
        return Mono.defer(() -> {
            log.debug("Mock {} listing for basket: {}", vendorName, request.getBasketId());
            
            Duration responseTime = Duration.ofMillis(100 + random.nextInt(400)); // 100-500ms
            
            if (random.nextDouble() < genericErrorRate) {
                log.debug("Simulating {} listing error for basket: {}", vendorName, request.getBasketId());
                return Mono.just(PublishingResult.failed(vendorName, request.getBasketId(), 
                    "Mock " + vendorName + " API error", responseTime));
            }
            
            PublishingResult result = PublishingResult.success(vendorName, request.getBasketId(), 
                vendorName + "_" + System.currentTimeMillis(), responseTime);
            result.setOperationType("LISTING");
            
            String key = String.format("%s:listing:%s", vendorName.toLowerCase(), request.getBasketId());
            return cacheService.put(key, result, Duration.ofMinutes(30))
                .then(eventPublisher.publish("vendor.listing.completed", request.getBasketId(), result))
                .thenReturn(result);
        });
    }

    /**
     * Generic vendor price publishing
     */
    public Mono<PublishingResult> mockGenericVendorPrice(String vendorName, PricePublishingRequest request) {
        return Mono.defer(() -> {
            log.debug("Mock {} price publishing for basket: {}", vendorName, request.getBasketId());
            
            Duration responseTime = Duration.ofNanos((1000 + random.nextInt(4000)) * 1000); // 1-5ms
            
            if (random.nextDouble() < genericErrorRate) {
                log.debug("Simulating {} price publishing error for basket: {}", vendorName, request.getBasketId());
                return Mono.just(PublishingResult.failed(vendorName, request.getBasketId(), 
                    "Mock " + vendorName + " price API error", responseTime));
            }
            
            PublishingResult result = PublishingResult.success(vendorName, request.getBasketId(), 
                vendorName + "_PRICE_" + System.currentTimeMillis(), responseTime);
            result.setOperationType("PRICE_PUBLISHING");
            
            String key = String.format("%s:price:%s:%s", vendorName.toLowerCase(), request.getBasketId(), request.getTimestamp());
            return cacheService.put(key, result, Duration.ofMinutes(5))
                .then(eventPublisher.publish("vendor.price.published", request.getBasketId(), result))
                .thenReturn(result);
        });
    }
}
