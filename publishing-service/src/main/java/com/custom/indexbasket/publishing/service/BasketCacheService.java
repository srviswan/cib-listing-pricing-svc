package com.custom.indexbasket.publishing.service;

import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.model.Basket;
import com.custom.indexbasket.common.model.BasketStatus;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for retrieving approved baskets from cache for price publishing
 * 
 * This service integrates with the cache to get baskets that are:
 * - APPROVED: Ready for listing
 * - LISTED: Successfully listed on vendor platforms  
 * - ACTIVE: Live baskets with real-time pricing
 * - SUSPENDED: Temporarily suspended but can be reactivated
 */
@Slf4j
@Service
public class BasketCacheService {

    @Autowired
    private CacheService cacheService;

    /**
     * Get all approved baskets from cache that are ready for price publishing
     * 
     * @return Flux of baskets ready for price publishing
     */
    public Flux<Basket> getApprovedBasketsForPricing() {
        log.debug("Retrieving approved baskets from cache for price publishing");
        
        return Flux.fromIterable(getCachedBaskets())
            .filter(this::isReadyForPricePublishing)
            .doOnNext(basket -> log.debug("Found approved basket: {} with status: {}", 
                basket.getBasketCode(), basket.getStatus()))
            .doOnComplete(() -> log.debug("Finished retrieving approved baskets from cache"));
    }

    /**
     * Get all approved baskets and convert them to price publishing requests
     * 
     * @return Flux of price publishing requests
     */
    public Flux<PricePublishingRequest> getPricePublishingRequests() {
        return getApprovedBasketsForPricing()
            .map(this::convertBasketToPriceRequest)
            .doOnNext(request -> log.debug("Converted basket {} to price request", 
                request.getBasketId()));
    }

    /**
     * Check if a basket is ready for price publishing
     * 
     * @param basket The basket to check
     * @return true if the basket is ready for price publishing
     */
    private boolean isReadyForPricePublishing(Basket basket) {
        if (basket == null || basket.getStatus() == null) {
            return false;
        }

        // Baskets that are approved, listed, active, or suspended can have prices published
        return basket.getStatus() == BasketStatus.APPROVED ||
               basket.getStatus() == BasketStatus.LISTED ||
               basket.getStatus() == BasketStatus.ACTIVE ||
               basket.getStatus() == BasketStatus.SUSPENDED;
    }

    /**
     * Convert a basket to a price publishing request
     * 
     * @param basket The basket to convert
     * @return PricePublishingRequest for the basket
     */
    private PricePublishingRequest convertBasketToPriceRequest(Basket basket) {
        // Generate realistic price data based on basket characteristics
        BigDecimal basePrice = generateBasePrice(basket);
        BigDecimal changeAmount = generateChangeAmount(basePrice);
        BigDecimal changePercentage = changeAmount.divide(basePrice, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        return PricePublishingRequest.builder()
            .basketId(basket.getBasketCode())
            .basketCode(basket.getBasketCode())
            .price(basePrice)
            .currency(basket.getBaseCurrency())
            .timestamp(LocalDateTime.now())
            .changeAmount(changeAmount)
            .changePercentage(changePercentage)
            .openPrice(basePrice.subtract(changeAmount))
            .highPrice(basePrice.add(changeAmount.multiply(BigDecimal.valueOf(0.5))))
            .lowPrice(basePrice.subtract(changeAmount.multiply(BigDecimal.valueOf(0.5))))
            .volume(generateVolume(basket))
            .exchange(determineExchange(basket))
            .build();
    }

    /**
     * Generate a realistic base price for a basket based on its characteristics
     * 
     * @param basket The basket to generate a price for
     * @return Realistic base price
     */
    private BigDecimal generateBasePrice(Basket basket) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Base price varies by basket type
        double baseMultiplier = switch (basket.getBasketType()) {
            case "EQUITY" -> random.nextDouble(50.0, 200.0);
            case "FIXED_INCOME" -> random.nextDouble(95.0, 105.0);
            case "COMMODITY" -> random.nextDouble(20.0, 150.0);
            case "MIXED" -> random.nextDouble(75.0, 175.0);
            default -> random.nextDouble(50.0, 150.0);
        };
        
        // Adjust based on number of constituents (more constituents = higher price)
        if (basket.getConstituents() != null) {
            int constituentCount = basket.getConstituents().size();
            if (constituentCount > 20) {
                baseMultiplier *= 1.2; // 20% higher for large baskets
            } else if (constituentCount < 5) {
                baseMultiplier *= 0.8; // 20% lower for small baskets
            }
        }
        
        return BigDecimal.valueOf(baseMultiplier).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Generate a realistic change amount for the price
     * 
     * @param basePrice The base price to calculate change from
     * @return Change amount (can be positive or negative)
     */
    private BigDecimal generateChangeAmount(BigDecimal basePrice) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Change is typically 0.1% to 2% of base price
        double changePercent = random.nextDouble(-0.02, 0.02);
        BigDecimal change = basePrice.multiply(BigDecimal.valueOf(changePercent));
        
        return change.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Generate realistic volume based on basket characteristics
     * 
     * @param basket The basket to generate volume for
     * @return Realistic volume
     */
    private Long generateVolume(Basket basket) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Base volume varies by basket type
        long baseVolume = switch (basket.getBasketType()) {
            case "EQUITY" -> random.nextLong(50000, 500000);
            case "FIXED_INCOME" -> random.nextLong(100000, 1000000);
            case "COMMODITY" -> random.nextLong(25000, 250000);
            case "MIXED" -> random.nextLong(75000, 750000);
            default -> random.nextLong(50000, 500000);
        };
        
        // Adjust based on basket size
        if (basket.getConstituents() != null) {
            int constituentCount = basket.getConstituents().size();
            if (constituentCount > 20) {
                baseVolume *= 2; // Higher volume for large baskets
            } else if (constituentCount < 5) {
                baseVolume /= 2; // Lower volume for small baskets
            }
        }
        
        return baseVolume;
    }

    /**
     * Determine the appropriate exchange for the basket
     * 
     * @param basket The basket to determine exchange for
     * @return Exchange name
     */
    private String determineExchange(Basket basket) {
        // Default to major exchanges based on basket type
        return switch (basket.getBasketType()) {
            case "EQUITY" -> "NASDAQ";
            case "FIXED_INCOME" -> "NYSE";
            case "COMMODITY" -> "CME";
            case "MIXED" -> "NASDAQ";
            default -> "NASDAQ";
        };
    }

    /**
     * Get cached baskets from the cache service
     * For now, return mock data until real cache integration is implemented
     * 
     * @return List of cached baskets
     */
    private List<Basket> getCachedBaskets() {
        // TODO: Replace with actual cache retrieval
        // For now, return mock baskets for testing
        return List.of(
            createMockBasket("TECH_ETF", "Technology ETF", "EQUITY", BasketStatus.ACTIVE),
            createMockBasket("BOND_FUND", "Fixed Income Fund", "FIXED_INCOME", BasketStatus.ACTIVE),
            createMockBasket("COMMODITY_MIX", "Commodity Mix", "COMMODITY", BasketStatus.ACTIVE),
            createMockBasket("BALANCED_PORT", "Balanced Portfolio", "MIXED", BasketStatus.ACTIVE),
            createMockBasket("GROWTH_ETF", "Growth ETF", "EQUITY", BasketStatus.LISTED),
            createMockBasket("DIVIDEND_FUND", "Dividend Fund", "EQUITY", BasketStatus.APPROVED)
        );
    }

    /**
     * Create a mock basket for testing purposes
     * 
     * @param code Basket code
     * @param name Basket name
     * @param type Basket type
     * @param status Basket status
     * @return Mock basket
     */
    private Basket createMockBasket(String code, String name, String type, BasketStatus status) {
        return Basket.builder()
            .basketCode(code)
            .basketName(name)
            .basketType(type)
            .baseCurrency("USD")
            .totalWeight(BigDecimal.valueOf(100.00))
            .status(status)
            .version("v1.0")
            .createdBy("SYSTEM")
            .createdAt(LocalDateTime.now())
            .approvedBy("APPROVER")
            .approvedAt(LocalDateTime.now())
            .constituents(List.of()) // Empty for now, can be populated if needed
            .build();
    }

    /**
     * Get basket by code from cache
     * 
     * @param basketCode The basket code to retrieve
     * @return Mono containing the basket or empty if not found
     */
    public Mono<Basket> getBasketByCode(String basketCode) {
        log.debug("Retrieving basket from cache: {}", basketCode);
        
        return cacheService.get("basket:" + basketCode, Basket.class)
            .doOnSuccess(basket -> {
                if (basket != null) {
                    log.debug("Found basket in cache: {} with status: {}", 
                        basket.getBasketCode(), basket.getStatus());
                } else {
                    log.debug("Basket not found in cache: {}", basketCode);
                }
            })
            .doOnError(error -> log.error("Error retrieving basket {} from cache: {}", 
                basketCode, error.getMessage()));
    }

    /**
     * Cache a basket
     * 
     * @param basket The basket to cache
     * @return Mono indicating success
     */
    public Mono<Void> cacheBasket(Basket basket) {
        log.debug("Caching basket: {} with status: {}", 
            basket.getBasketCode(), basket.getStatus());
        
        return cacheService.put("basket:" + basket.getBasketCode(), basket)
            .doOnSuccess(v -> log.debug("Successfully cached basket: {}", basket.getBasketCode()))
            .doOnError(error -> log.error("Failed to cache basket {}: {}", 
                basket.getBasketCode(), error.getMessage()));
    }

    /**
     * Remove basket from cache
     * 
     * @param basketCode The basket code to remove
     * @return Mono indicating success
     */
    public Mono<Void> removeBasketFromCache(String basketCode) {
        log.debug("Removing basket from cache: {}", basketCode);
        
        return cacheService.delete("basket:" + basketCode)
            .then()
            .doOnSuccess(v -> log.debug("Successfully removed basket from cache: {}", basketCode))
            .doOnError(error -> log.error("Failed to remove basket {} from cache: {}", 
                basketCode, error.getMessage()));
    }
}
