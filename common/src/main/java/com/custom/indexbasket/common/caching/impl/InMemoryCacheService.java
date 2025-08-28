package com.custom.indexbasket.common.caching.impl;

import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.caching.CacheStats;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the common CacheService interface.
 * Provides in-memory caching with TTL support and metrics.
 * Used when Redis is not available (e.g., in minimal deployment mode).
 */
@Service
public class InMemoryCacheService implements CacheService {
    
    private static final Logger log = LoggerFactory.getLogger(InMemoryCacheService.class);
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;
    private final ScheduledExecutorService cleanupExecutor;
    private final CacheStats cacheStats;
    
    // Metrics
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheSetCounter;
    private final Counter cacheDeleteCounter;
    private final Timer cacheGetTimer;
    private final Timer cacheSetTimer;
    
    public InMemoryCacheService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.cacheStats = new CacheStats("InMemoryCache");
        
        // Initialize metrics
        this.cacheHitCounter = Counter.builder("cache.inmemory.hit")
            .description("Number of cache hits")
            .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("cache.inmemory.miss")
            .description("Number of cache misses")
            .register(meterRegistry);
        this.cacheSetCounter = Counter.builder("cache.inmemory.set")
            .description("Number of cache sets")
            .register(meterRegistry);
        this.cacheDeleteCounter = Counter.builder("cache.inmemory.delete")
            .description("Number of cache deletions")
            .register(meterRegistry);
        this.cacheGetTimer = Timer.builder("cache.inmemory.get.duration")
            .description("Cache get operation duration")
            .register(meterRegistry);
        this.cacheSetTimer = Timer.builder("cache.inmemory.set.duration")
            .description("Cache set operation duration")
            .register(meterRegistry);
        
        // Start cleanup executor for expired entries
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule cleanup every 30 seconds
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 30, 30, TimeUnit.SECONDS);
        
        log.info("InMemoryCacheService initialized");
    }
    
    @Override
    public <T> Mono<Void> put(String key, T value) {
        return put(key, value, null);
    }
    
    @Override
    public <T> Mono<Void> put(String key, T value, Duration ttl) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            Instant expiryTime = (ttl != null) ? Instant.now().plus(ttl) : null;
            CacheEntry entry = new CacheEntry(value, expiryTime);
            
            cache.put(key, entry);
            cacheStats.recordPut();
            
            sample.stop(cacheSetTimer);
            cacheSetCounter.increment();
            log.debug("Cached value for key: {} with TTL: {}", key, ttl);
            
            return Mono.empty();
        });
    }
    
    @Override
    public <T> Mono<T> get(String key, Class<T> type) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            CacheEntry entry = cache.get(key);
            
            if (entry == null || entry.isExpired()) {
                if (entry != null && entry.isExpired()) {
                    cache.remove(key); // Clean up expired entry
                }
                sample.stop(cacheGetTimer);
                cacheMissCounter.increment();
                cacheStats.recordGet(false);
                log.debug("Cache miss for key: {}", key);
                return Mono.empty();
            }
            
            sample.stop(cacheGetTimer);
            cacheHitCounter.increment();
            cacheStats.recordGet(true);
            log.debug("Cache hit for key: {}", key);
            
            @SuppressWarnings("unchecked")
            T value = (T) entry.getValue();
            return Mono.just(value);
        });
    }
    
    @Override
    public Mono<Boolean> exists(String key) {
        return Mono.defer(() -> {
            CacheEntry entry = cache.get(key);
            boolean exists = entry != null && !entry.isExpired();
            return Mono.just(exists);
        });
    }
    
    @Override
    public Mono<Boolean> delete(String key) {
        return Mono.defer(() -> {
            CacheEntry removed = cache.remove(key);
            boolean deleted = removed != null;
            
            if (deleted) {
                cacheDeleteCounter.increment();
                cacheStats.recordDelete();
                log.debug("Deleted cache entry for key: {}", key);
            }
            
            return Mono.just(deleted);
        });
    }
    
    @Override
    public <T> Mono<Void> putAll(Map<String, T> entries) {
        return putAll(entries, null);
    }
    
    @Override
    public <T> Mono<Void> putAll(Map<String, T> entries, Duration ttl) {
        return Mono.defer(() -> {
            entries.forEach((key, value) -> {
                Instant expiryTime = (ttl != null) ? Instant.now().plus(ttl) : null;
                CacheEntry entry = new CacheEntry(value, expiryTime);
                cache.put(key, entry);
                cacheStats.recordPut();
            });
            log.debug("Bulk cached {} entries", entries.size());
            return Mono.empty();
        });
    }
    
    @Override
    public <T> Mono<Map<String, T>> getAll(Set<String> keys, Class<T> type) {
        return Mono.defer(() -> {
            Map<String, T> result = keys.stream()
                .filter(key -> cache.containsKey(key))
                .collect(Collectors.toMap(
                    key -> key,
                    key -> {
                        CacheEntry entry = cache.get(key);
                        if (entry != null && !entry.isExpired()) {
                            @SuppressWarnings("unchecked")
                            T value = (T) entry.getValue();
                            return value;
                        }
                        return null;
                    }
                ));
            return Mono.just(result);
        });
    }
    
    @Override
    public Mono<Long> deleteAll(Set<String> keys) {
        return Mono.defer(() -> {
            long deletedCount = keys.stream()
                .mapToLong(key -> cache.remove(key) != null ? 1 : 0)
                .sum();
            return Mono.just(deletedCount);
        });
    }
    
    @Override
    public <T> Mono<T> getAndPut(String key, T value, Class<T> type) {
        return Mono.defer(() -> {
            T oldValue = get(key, type).block();
            put(key, value).block();
            return Mono.justOrEmpty(oldValue);
        });
    }
    
    @Override
    public <T> Mono<T> putIfAbsent(String key, T value, Class<T> type) {
        return Mono.defer(() -> {
            if (cache.containsKey(key)) {
                CacheEntry entry = cache.get(key);
                if (entry != null && !entry.isExpired()) {
                    @SuppressWarnings("unchecked")
                    T existingValue = (T) entry.getValue();
                    return Mono.just(existingValue);
                }
            }
            put(key, value).block();
            return Mono.just(value);
        });
    }
    
    @Override
    public <T> Mono<Boolean> replace(String key, T oldValue, T newValue) {
        return Mono.defer(() -> {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                @SuppressWarnings("unchecked")
                T existingValue = (T) entry.getValue();
                if (existingValue.equals(oldValue)) {
                    put(key, newValue).block();
                    return Mono.just(true);
                }
            }
            return Mono.just(false);
        });
    }
    
    @Override
    public Mono<Long> increment(String key) {
        return increment(key, 1L);
    }
    
    @Override
    public Mono<Long> increment(String key, long delta) {
        return Mono.defer(() -> {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired() && entry.getValue() instanceof Number) {
                long currentValue = ((Number) entry.getValue()).longValue();
                long newValue = currentValue + delta;
                put(key, newValue).block();
                return Mono.just(newValue);
            }
            put(key, delta).block();
            return Mono.just(delta);
        });
    }
    
    @Override
    public Mono<Double> increment(String key, double delta) {
        return Mono.defer(() -> {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired() && entry.getValue() instanceof Number) {
                double currentValue = ((Number) entry.getValue()).doubleValue();
                double newValue = currentValue + delta;
                put(key, newValue).block();
                return Mono.just(newValue);
            }
            put(key, delta).block();
            return Mono.just(delta);
        });
    }
    
    @Override
    public Mono<Boolean> expire(String key, Duration ttl) {
        return Mono.defer(() -> {
            CacheEntry entry = cache.get(key);
            if (entry != null) {
                Instant newExpiryTime = Instant.now().plus(ttl);
                CacheEntry updatedEntry = new CacheEntry(entry.getValue(), newExpiryTime);
                cache.put(key, updatedEntry);
                log.debug("Updated TTL for key: {} to {}", key, ttl);
                return Mono.just(true);
            }
            return Mono.just(false);
        });
    }
    
    @Override
    public Mono<Duration> getTtl(String key) {
        return Mono.defer(() -> {
            CacheEntry entry = cache.get(key);
            if (entry != null && entry.expiryTime != null) {
                Duration ttl = Duration.between(Instant.now(), entry.expiryTime);
                return ttl.isNegative() ? Mono.empty() : Mono.just(ttl);
            }
            return Mono.empty();
        });
    }
    
    @Override
    public Mono<Boolean> persist(String key) {
        return Mono.defer(() -> {
            CacheEntry entry = cache.get(key);
            if (entry != null) {
                CacheEntry persistentEntry = new CacheEntry(entry.getValue(), null);
                cache.put(key, persistentEntry);
                return Mono.just(true);
            }
            return Mono.just(false);
        });
    }
    
    @Override
    public Mono<CacheStats> getStats() {
        return Mono.just(cacheStats);
    }
    
    @Override
    public Mono<Boolean> isHealthy() {
        return Mono.just(cacheStats.isHealthy());
    }
    
    @Override
    public Mono<Void> clearAll() {
        return Mono.defer(() -> {
            cache.clear();
            cacheStats.reset();
            log.info("Cleared all cache entries");
            return Mono.empty();
        });
    }
    
    private void cleanupExpiredEntries() {
        try {
            int initialSize = cache.size();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            int finalSize = cache.size();
            
            if (initialSize != finalSize) {
                log.debug("Cleaned up {} expired cache entries", initialSize - finalSize);
            }
        } catch (Exception e) {
            log.warn("Error during cache cleanup: {}", e.getMessage());
        }
    }
    
    /**
     * Cache entry wrapper with expiration support
     */
    private static class CacheEntry {
        private final Object value;
        private final Instant expiryTime;
        
        public CacheEntry(Object value, Instant expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
        
        public Object getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return expiryTime != null && Instant.now().isAfter(expiryTime);
        }
    }
}
