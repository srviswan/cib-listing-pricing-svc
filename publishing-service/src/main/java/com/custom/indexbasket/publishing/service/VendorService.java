package com.custom.indexbasket.publishing.service;

import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Vendor Service Interface
 * 
 * Abstracts vendor operations for basket listing and price publishing.
 * Can be implemented by proxy services (development) or real vendor services (production).
 */
public interface VendorService {

    /**
     * Publish basket listing to all active vendors
     */
    Mono<List<PublishingResult>> publishBasketListing(BasketListingRequest request);

    /**
     * Publish basket price to all active vendors
     */
    Mono<List<PublishingResult>> publishBasketPrice(PricePublishingRequest request);

    /**
     * Publish basket listing to a specific vendor
     */
    Mono<PublishingResult> publishBasketListingToVendor(String vendorName, BasketListingRequest request);

    /**
     * Publish basket price to a specific vendor
     */
    Mono<PublishingResult> publishBasketPriceToVendor(String vendorName, PricePublishingRequest request);

    /**
     * Get list of active vendors
     */
    List<String> getActiveVendors();

    /**
     * Check vendor health status
     */
    Mono<Boolean> isVendorHealthy(String vendorName);
}
