package com.custom.indexbasket.sma.service;

import com.custom.indexbasket.sma.config.SmaConfiguration;
import com.custom.indexbasket.sma.model.SmaHealthStatus;
import com.custom.indexbasket.sma.model.SmaPriceData;
import com.custom.indexbasket.sma.exception.SmaApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * SMA Price Service
 * 
 * Service for retrieving price data from SMA Refinitiv API with caching.
 */
@Service
@Slf4j
public class SmaPriceService {
    
    private final SmaConnectionManager connectionManager;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final SmaConfiguration config;
    
    @Autowired
    public SmaPriceService(SmaConnectionManager connectionManager, 
                          ReactiveRedisTemplate<String, Object> redisTemplate,
                          SmaConfiguration config) {
        this.connectionManager = connectionManager;
        this.redisTemplate = redisTemplate;
        this.config = config;
    }
    
    /**
     * Get price data for a single symbol
     */
    public Mono<SmaPriceData> getPrice(String symbol) {
        if (!config.getCache().isEnabled()) {
            return connectionManager.fetchPrice(symbol);
        }
        
        String cacheKey = "sma:price:" + symbol;
        
        return redisTemplate.opsForValue().get(cacheKey)
            .cast(SmaPriceData.class)
            .flatMap(cached -> {
                if (cached != null && !isExpired(cached)) {
                    log.debug("Cache hit for symbol: {}", symbol);
                    return Mono.just(cached);
                }
                return Mono.empty();
            })
            .switchIfEmpty(
                connectionManager.fetchPrice(symbol)
                    .flatMap(priceData -> 
                        redisTemplate.opsForValue()
                            .set(cacheKey, priceData, Duration.ofSeconds(config.getCache().getTtl()))
                            .thenReturn(priceData)
                    )
                    .doOnNext(data -> log.debug("Cache updated for symbol: {}", symbol))
            )
            .onErrorMap(throwable -> new SmaApiException("Failed to fetch price for " + symbol, throwable));
    }
    
    /**
     * Get price data for multiple symbols in batch
     */
    public Flux<SmaPriceData> getBatchPrices(List<String> symbols) {
        return Flux.fromIterable(symbols)
            .flatMap(this::getPrice, config.getSubscription().getBatchSize())
            .onErrorContinue((error, symbol) -> 
                log.warn("Failed to fetch price for symbol: {}", symbol, error));
    }
    
    /**
     * Check if cached data is expired
     */
    private boolean isExpired(SmaPriceData priceData) {
        if (priceData.getTimestamp() == null) {
            return true;
        }
        return priceData.getTimestamp().isBefore(
            java.time.LocalDateTime.now().minusSeconds(config.getCache().getTtl())
        );
    }
    
    /**
     * Check SMA API health
     */
    public Mono<SmaHealthStatus> checkHealth() {
        return connectionManager.checkHealth();
    }
    
    /**
     * Start real-time price subscription
     */
    public Flux<SmaPriceData> startRealTimeSubscription(List<String> symbols) {
        return connectionManager.startRealTimeSubscription(symbols)
            .doOnNext(priceData -> {
                if (config.getCache().isEnabled()) {
                    String cacheKey = "sma:price:" + priceData.getSymbol();
                    redisTemplate.opsForValue()
                        .set(cacheKey, priceData, Duration.ofSeconds(config.getCache().getTtl()))
                        .subscribe();
                }
            })
            .onErrorContinue((error, tick) -> 
                log.warn("Real-time subscription error: {}", error.getMessage()));
    }
    
    /**
     * Clear cache for a specific symbol
     */
    public Mono<Boolean> clearCache(String symbol) {
        String cacheKey = "sma:price:" + symbol;
        return redisTemplate.delete(cacheKey)
            .map(count -> count > 0);
    }
    
    /**
     * Clear all SMA cache
     */
    public Mono<Long> clearAllCache() {
        return redisTemplate.delete(redisTemplate.keys("sma:price:*"));
    }
    
    /**
     * Get cache statistics
     */
    public Mono<Long> getCacheSize() {
        return redisTemplate.keys("sma:price:*")
            .count();
    }
}
