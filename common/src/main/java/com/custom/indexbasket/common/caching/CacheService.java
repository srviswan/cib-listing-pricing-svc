package com.custom.indexbasket.common.caching;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Universal caching interface
 * Abstracts caching implementation (SolCache vs Redis)
 */
public interface CacheService {
    
    /**
     * Basic cache operations
     */
    <T> Mono<Void> put(String key, T value);
    <T> Mono<Void> put(String key, T value, Duration ttl);
    <T> Mono<T> get(String key, Class<T> type);
    Mono<Boolean> exists(String key);
    Mono<Boolean> delete(String key);
    
    /**
     * Batch operations for efficiency
     */
    <T> Mono<Void> putAll(Map<String, T> entries);
    <T> Mono<Void> putAll(Map<String, T> entries, Duration ttl);
    <T> Mono<Map<String, T>> getAll(Set<String> keys, Class<T> type);
    Mono<Long> deleteAll(Set<String> keys);
    
    /**
     * Advanced cache operations
     */
    <T> Mono<T> getAndPut(String key, T value, Class<T> type);
    <T> Mono<T> putIfAbsent(String key, T value, Class<T> type);
    <T> Mono<Boolean> replace(String key, T oldValue, T newValue);
    
    /**
     * Atomic operations
     */
    Mono<Long> increment(String key);
    Mono<Long> increment(String key, long delta);
    Mono<Double> increment(String key, double delta);
    
    /**
     * Time-based operations
     */
    Mono<Boolean> expire(String key, Duration ttl);
    Mono<Duration> getTtl(String key);
    Mono<Boolean> persist(String key);
    
    /**
     * Cache statistics and health
     */
    Mono<CacheStats> getStats();
    Mono<Boolean> isHealthy();
    Mono<Void> clearAll();
}
