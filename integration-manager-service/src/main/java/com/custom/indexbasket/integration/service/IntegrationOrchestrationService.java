package com.custom.indexbasket.integration.service;

import com.custom.indexbasket.integration.model.BasketPrice;
// Local models - in production these would be shared libraries
import com.custom.indexbasket.integration.model.PricePublishingRequest;
import com.custom.indexbasket.integration.model.PublishingResult;
import com.custom.indexbasket.integration.model.SmaPriceData;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Integration Orchestration Service
 * 
 * Orchestrates data flow between SMA and FIX adapters for basket price calculation and publishing.
 */
@Service
@Slf4j
public class IntegrationOrchestrationService {
    
    @Autowired
    private WebClient smaWebClient;
    
    @Autowired
    private WebClient fixWebClient;
    
    @Autowired
    private WebClient basketCoreWebClient;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Cache for basket data
    private final ConcurrentHashMap<String, BasketPrice> basketPriceCache = new ConcurrentHashMap<>();
    
    /**
     * Calculate and publish basket price
     */
    public Mono<BasketPrice> calculateAndPublishBasketPrice(String basketId) {
        long startTime = System.currentTimeMillis();
        
        return getBasketData(basketId)
            .flatMap(basket -> calculateBasketPrice(basket))
            .flatMap(basketPrice -> publishBasketPrice(basketPrice))
            .doOnSuccess(price -> {
                long processingTime = System.currentTimeMillis() - startTime;
                basketPriceCache.put(basketId, price);
                
                meterRegistry.counter("integration.basket.price.calculated").increment();
                meterRegistry.timer("integration.basket.processing.time").record(Duration.ofMillis(processingTime));
                
                log.info("Successfully calculated and published basket price for: {} in {}ms", basketId, processingTime);
            })
            .doOnError(error -> {
                meterRegistry.counter("integration.basket.price.failed").increment();
                log.error("Failed to calculate/publish basket price for {}: {}", basketId, error.getMessage());
            });
    }
    
    /**
     * Get basket data from Basket Core Service
     */
    private Mono<Map<String, Object>> getBasketData(String basketId) {
        return basketCoreWebClient
            .get()
            .uri("/api/v1/baskets/{id}", basketId)
            .retrieve()
            .bodyToMono(Map.class)
            .map(map -> (Map<String, Object>) map)
            .timeout(Duration.ofSeconds(10))
            .doOnError(error -> log.error("Failed to get basket data for {}: {}", basketId, error.getMessage()));
    }
    
    /**
     * Calculate basket price from constituent prices
     */
    private Mono<BasketPrice> calculateBasketPrice(Map<String, Object> basketData) {
        try {
            String basketId = (String) basketData.get("id");
            String basketCode = (String) basketData.get("code");
            String currency = (String) basketData.getOrDefault("currency", "USD");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> constituents = (List<Map<String, Object>>) basketData.get("constituents");
            
            if (constituents == null || constituents.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Basket has no constituents"));
            }
            
            List<String> symbols = constituents.stream()
                .map(constituent -> (String) constituent.get("symbol"))
                .collect(Collectors.toList());
            
            // Get constituent prices from SMA adapter
            return getConstituentPrices(symbols)
                .collectList()
                .map(priceDataList -> {
                    BigDecimal totalValue = BigDecimal.ZERO;
                    Map<String, BigDecimal> weights = new ConcurrentHashMap<>();
                    
                    for (Map<String, Object> constituent : constituents) {
                        String symbol = (String) constituent.get("symbol");
                        BigDecimal weight = new BigDecimal(constituent.get("weight").toString());
                        
                        SmaPriceData priceData = priceDataList.stream()
                            .filter(p -> p.getSymbol().equals(symbol))
                            .findFirst()
                            .orElse(null);
                        
                        if (priceData != null && priceData.getPrice() != null) {
                            BigDecimal constituentValue = priceData.getPrice().multiply(weight)
                                .divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP);
                            totalValue = totalValue.add(constituentValue);
                        }
                        
                        weights.put(symbol, weight);
                    }
                    
                    // Get previous price from cache
                    BasketPrice previousPrice = basketPriceCache.get(basketId);
                    
                    return BasketPrice.builder()
                        .basketId(basketId)
                        .basketCode(basketCode)
                        .price(totalValue)
                        .currency(currency)
                        .timestamp(LocalDateTime.now())
                        .previousPrice(previousPrice != null ? previousPrice.getPrice() : null)
                        .constituentPrices(priceDataList)
                        .constituentWeights(weights)
                        .metadata(Map.of(
                            "calculationMethod", "WEIGHTED_AVERAGE",
                            "constituentCount", constituents.size(),
                            "calculationTimestamp", LocalDateTime.now().toString()
                        ))
                        .build();
                });
                
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error calculating basket price: " + e.getMessage(), e));
        }
    }
    
    /**
     * Get constituent prices from SMA adapter
     */
    private Flux<SmaPriceData> getConstituentPrices(List<String> symbols) {
        return smaWebClient
            .post()
            .uri("/api/v1/sma/prices/batch")
            .bodyValue(symbols)
            .retrieve()
            .bodyToFlux(SmaPriceData.class)
            .timeout(Duration.ofSeconds(15))
            .doOnError(error -> log.error("Failed to get constituent prices: {}", error.getMessage()));
    }
    
    /**
     * Publish basket price via FIX adapter
     */
    private Mono<BasketPrice> publishBasketPrice(BasketPrice basketPrice) {
        PricePublishingRequest request = PricePublishingRequest.builder()
            .basketId(basketPrice.getBasketId())
            .symbol(basketPrice.getBasketCode())
            .price(basketPrice.getPrice())
            .currency(basketPrice.getCurrency())
            .timestamp(basketPrice.getTimestamp())
            .volume(new BigDecimal("1000")) // Default volume
            .exchange("NYSE") // Default exchange
            .marketDataType("BASKET_PRICE")
            .build();
        
        return fixWebClient
            .post()
            .uri("/api/v1/fix/publish/price")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PublishingResult.class)
            .timeout(Duration.ofSeconds(10))
            .map(result -> {
                if (result.isSuccess()) {
                    log.info("Successfully published basket price for: {} via FIX", basketPrice.getBasketId());
                    meterRegistry.counter("integration.fix.publish.success").increment();
                } else {
                    log.warn("Failed to publish basket price for: {} via FIX: {}", 
                        basketPrice.getBasketId(), result.getErrorMessage());
                    meterRegistry.counter("integration.fix.publish.failed").increment();
                }
                return basketPrice;
            })
            .doOnError(error -> {
                meterRegistry.counter("integration.fix.publish.error").increment();
                log.error("Error publishing basket price for {}: {}", basketPrice.getBasketId(), error.getMessage());
            });
    }
    
    /**
     * Get cached basket price
     */
    public Mono<BasketPrice> getCachedBasketPrice(String basketId) {
        BasketPrice cached = basketPriceCache.get(basketId);
        if (cached != null && !isExpired(cached)) {
            return Mono.just(cached);
        }
        return Mono.empty();
    }
    
    /**
     * Check if cached price is expired (older than 5 minutes)
     */
    private boolean isExpired(BasketPrice price) {
        return price.getTimestamp().isBefore(LocalDateTime.now().minusMinutes(5));
    }
    
    /**
     * Get all active baskets from Basket Core Service
     */
    public Flux<String> getActiveBaskets() {
        return basketCoreWebClient
            .get()
            .uri("/api/v1/baskets?status=ACTIVE")
            .retrieve()
            .bodyToFlux(Map.class)
            .map(basket -> (String) basket.get("id"))
            .timeout(Duration.ofSeconds(10))
            .doOnError(error -> log.error("Failed to get active baskets: {}", error.getMessage()));
    }
    
    /**
     * Start real-time publishing for a basket
     */
    public Mono<Boolean> startRealTimePublishing(String basketId) {
        log.info("Starting real-time publishing for basket: {}", basketId);
        
        return calculateAndPublishBasketPrice(basketId)
            .then(Mono.just(true))
            .doOnSuccess(success -> log.info("Real-time publishing started for basket: {}", basketId))
            .doOnError(error -> log.error("Failed to start real-time publishing for basket {}: {}", basketId, error.getMessage()));
    }
    
    /**
     * Stop real-time publishing for a basket
     */
    public Mono<Boolean> stopRealTimePublishing(String basketId) {
        log.info("Stopping real-time publishing for basket: {}", basketId);
        
        basketPriceCache.remove(basketId);
        
        return Mono.just(true)
            .doOnSuccess(success -> log.info("Real-time publishing stopped for basket: {}", basketId));
    }
    
    /**
     * Get cache statistics
     */
    public Mono<Map<String, Object>> getCacheStatistics() {
        return Mono.fromCallable(() -> {
            long totalEntries = basketPriceCache.size();
            long expiredEntries = basketPriceCache.values().stream()
                .mapToLong(price -> isExpired(price) ? 1 : 0)
                .sum();
            
            return Map.of(
                "totalEntries", totalEntries,
                "expiredEntries", expiredEntries,
                "activeEntries", totalEntries - expiredEntries,
                "timestamp", LocalDateTime.now()
            );
        });
    }
}
