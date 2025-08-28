package com.custom.indexbasket.publishing.service;

import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import com.custom.indexbasket.publishing.proxy.VendorProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Vendor Service Implementation using Proxy Services
 * 
 * Implements vendor operations using proxy services for development and testing.
 * Will be replaced with real vendor integration when vendor onboarding is complete.
 */
@Slf4j
@Service
public class VendorServiceImpl implements VendorService {

    private final VendorProxyService proxyService;

    public VendorServiceImpl(VendorProxyService proxyService) {
        this.proxyService = proxyService;
    }

    private static final List<String> ACTIVE_VENDORS = Arrays.asList("BLOOMBERG", "REFINITIV", "GENERIC");

    @Override
    public Mono<List<PublishingResult>> publishBasketListing(BasketListingRequest request) {
        log.info("Publishing basket listing to all vendors for basket: {}", request.getBasketId());
        
        return Flux.fromIterable(ACTIVE_VENDORS)
            .flatMap(vendor -> publishBasketListingToVendor(vendor, request), 3) // Process 3 vendors concurrently
            .collectList()
            .doOnSuccess(results -> log.info("Basket listing completed for basket: {}. Results: {}", 
                request.getBasketId(), results.size()))
            .doOnError(error -> log.error("Basket listing failed for basket {}: {}", 
                request.getBasketId(), error.getMessage()));
    }

    @Override
    public Mono<List<PublishingResult>> publishBasketPrice(PricePublishingRequest request) {
        log.info("Publishing basket price to all vendors for basket: {}", request.getBasketId());
        
        return Flux.fromIterable(ACTIVE_VENDORS)
            .flatMap(vendor -> publishBasketPriceToVendor(vendor, request), 5) // Process 5 vendors concurrently
            .collectList()
            .doOnSuccess(results -> log.info("Basket price publishing completed for basket: {}. Results: {}", 
                request.getBasketId(), results.size()))
            .doOnError(error -> log.error("Basket price publishing failed for basket {}: {}", 
                request.getBasketId(), error.getMessage()));
    }

    @Override
    public Mono<PublishingResult> publishBasketListingToVendor(String vendorName, BasketListingRequest request) {
        log.debug("Publishing basket listing to vendor: {} for basket: {}", vendorName, request.getBasketId());
        
        return switch (vendorName.toUpperCase()) {
            case "BLOOMBERG" -> proxyService.mockBloombergListing(request);
            case "REFINITIV" -> proxyService.mockRefinitivListing(request);
            default -> proxyService.mockGenericVendor(vendorName, request);
        };
    }

    @Override
    public Mono<PublishingResult> publishBasketPriceToVendor(String vendorName, PricePublishingRequest request) {
        log.debug("Publishing basket price to vendor: {} for basket: {}", vendorName, request.getBasketId());
        
        return switch (vendorName.toUpperCase()) {
            case "BLOOMBERG" -> proxyService.mockBloombergPricePublishing(request);
            case "REFINITIV" -> proxyService.mockRefinitivPricePublishing(request);
            default -> proxyService.mockGenericVendorPrice(vendorName, request);
        };
    }

    @Override
    public List<String> getActiveVendors() {
        return ACTIVE_VENDORS;
    }

    @Override
    public Mono<Boolean> isVendorHealthy(String vendorName) {
        // For now, all proxy vendors are considered healthy
        // This will be enhanced with real health checks when real vendors are integrated
        return Mono.just(true);
    }
}
