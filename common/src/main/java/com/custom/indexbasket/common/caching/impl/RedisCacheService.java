package com.custom.indexbasket.common.caching.impl;

import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.caching.CacheStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis implementation of the common CacheService interface.
 * Provides reactive Redis caching with metrics and monitoring.
 */
@Service
public class RedisCacheService implements CacheService {
    
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheSetCounter;
    private final Counter cacheDeleteCounter;
    private final Timer cacheGetTimer;
    private final Timer cacheSetTimer;
    
    private final Instant startTime;
    
    public RedisCacheService(ReactiveRedisTemplate<String, Object> redisTemplate,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.startTime = Instant.now();
        
        // Initialize metrics
        this.cacheHitCounter = Counter.builder("cache.redis.hit")
            .description("Number of cache hits")
            .register(meterRegistry);
        this.cacheMissCounter = Counter.builder("cache.redis.miss")
            .description("Number of cache misses")
            .register(meterRegistry);
        this.cacheSetCounter = Counter.builder("cache.redis.set")
            .description("Number of cache sets")
            .register(meterRegistry);
        this.cacheDeleteCounter = Counter.builder("cache.redis.delete")
            .description("Number of cache deletions")
            .register(meterRegistry);
        this.cacheGetTimer = Timer.builder("cache.redis.get.duration")
            .description("Cache get operation duration")
            .register(meterRegistry);
        this.cacheSetTimer = Timer.builder("cache.redis.set.duration")
            .description("Cache set operation duration")
            .register(meterRegistry);
    }
    
    @Override
    public <T> Mono<Void> put(String key, T value) {
        return put(key, value, null);
    }
    
    @Override
    public <T> Mono<Void> put(String key, T value, Duration ttl) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            Mono<Boolean> operation = (ttl != null) 
                ? redisTemplate.opsForValue().set(key, value, ttl)
                : redisTemplate.opsForValue().set(key, value);
                
            return operation
                .doOnSuccess(result -> {
                    sample.stop(cacheSetTimer);
                    cacheSetCounter.increment();
                    log.debug("Cached value for key: {} with TTL: {}", key, ttl);
                })
                .doOnError(error -> {
                    sample.stop(cacheSetTimer);
                    log.error("Cache set error for key {}: {}", key, error.getMessage());
                })
                .then();
        });
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Mono<T> get(String key, Class<T> type) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            return redisTemplate.opsForValue().get(key)
                .cast(type)
                .doOnSuccess(value -> {
                    sample.stop(cacheGetTimer);
                    if (value != null) {
                        cacheHitCounter.increment();
                        log.debug("Cache hit for key: {}", key);
                    } else {
                        cacheMissCounter.increment();
                        log.debug("Cache miss for key: {}", key);
                    }
                })
                .doOnError(error -> {
                    sample.stop(cacheGetTimer);
                    cacheMissCounter.increment();
                    log.error("Cache get error for key {}: {}", key, error.getMessage());
                })
                .onErrorReturn(null);
        });
    }
    
    @Override
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key)
            .doOnError(error -> log.error("Cache exists check error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(false);
    }
    
    @Override
    public Mono<Boolean> delete(String key) {
        return redisTemplate.delete(key)
            .map(count -> count > 0)
            .doOnSuccess(deleted -> {
                if (deleted) {
                    cacheDeleteCounter.increment();
                    log.debug("Deleted cache entry for key: {}", key);
                }
            })
            .doOnError(error -> log.error("Cache delete error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(false);
    }
    
    @Override
    public <T> Mono<Void> putAll(Map<String, T> entries) {
        return putAll(entries, null);
    }
    
    @Override
    public <T> Mono<Void> putAll(Map<String, T> entries, Duration ttl) {
        return Flux.fromIterable(entries.entrySet())
            .flatMap(entry -> put(entry.getKey(), entry.getValue(), ttl))
            .then();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Mono<Map<String, T>> getAll(Set<String> keys, Class<T> type) {
        return redisTemplate.opsForValue().multiGet(keys)
            .map(values -> {
                Map<String, T> result = keys.stream()
                    .collect(Collectors.toMap(
                        key -> key,
                        key -> {
                            Object value = values.get(keys.stream().toList().indexOf(key));
                            return value != null ? type.cast(value) : null;
                        }
                    ));
                return result.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            })
            .doOnError(error -> log.error("Cache getAll error: {}", error.getMessage()))
            .onErrorReturn(Map.of());
    }
    
    @Override
    public Mono<Long> deleteAll(Set<String> keys) {
        return redisTemplate.delete(Flux.fromIterable(keys))
            .doOnSuccess(count -> {
                cacheDeleteCounter.increment(count);
                log.debug("Deleted {} cache entries", count);
            })
            .doOnError(error -> log.error("Cache deleteAll error: {}", error.getMessage()))
            .onErrorReturn(0L);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Mono<T> getAndPut(String key, T value, Class<T> type) {
        return redisTemplate.opsForValue().getAndSet(key, value)
            .cast(type)
            .doOnSuccess(oldValue -> {
                cacheSetCounter.increment();
                if (oldValue != null) {
                    cacheHitCounter.increment();
                } else {
                    cacheMissCounter.increment();
                }
            })
            .doOnError(error -> log.error("Cache getAndPut error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(null);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Mono<T> putIfAbsent(String key, T value, Class<T> type) {
        return redisTemplate.opsForValue().setIfAbsent(key, value)
            .flatMap(wasSet -> wasSet 
                ? Mono.just(value)
                : get(key, type))
            .doOnSuccess(result -> {
                if (result != null && result.equals(value)) {
                    cacheSetCounter.increment();
                }
            })
            .doOnError(error -> log.error("Cache putIfAbsent error for key {}: {}", key, error.getMessage()));
    }
    
    @Override
    public <T> Mono<Boolean> replace(String key, T oldValue, T newValue) {
        // Redis doesn't have a native compare-and-swap for objects, so we use a Lua script approach
        return redisTemplate.opsForValue().get(key)
            .flatMap(currentValue -> {
                if (oldValue.equals(currentValue)) {
                    return redisTemplate.opsForValue().set(key, newValue)
                        .doOnSuccess(result -> cacheSetCounter.increment());
                } else {
                    return Mono.just(false);
                }
            })
            .doOnError(error -> log.error("Cache replace error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(false);
    }
    
    @Override
    public Mono<Long> increment(String key) {
        return increment(key, 1L);
    }
    
    @Override
    public Mono<Long> increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta)
            .doOnError(error -> log.error("Cache increment error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(0L);
    }
    
    @Override
    public Mono<Double> increment(String key, double delta) {
        return redisTemplate.opsForValue().increment(key, delta)
            .doOnError(error -> log.error("Cache increment error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(0.0);
    }
    
    @Override
    public Mono<Boolean> expire(String key, Duration ttl) {
        return redisTemplate.expire(key, ttl)
            .doOnError(error -> log.error("Cache expire error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(false);
    }
    
    @Override
    public Mono<Duration> getTtl(String key) {
        return redisTemplate.getExpire(key)
            .doOnError(error -> log.error("Cache getTtl error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(Duration.ZERO);
    }
    
    @Override
    public Mono<Boolean> persist(String key) {
        return redisTemplate.persist(key)
            .doOnError(error -> log.error("Cache persist error for key {}: {}", key, error.getMessage()))
            .onErrorReturn(false);
    }
    
    @Override
    public Mono<CacheStats> getStats() {
        return Mono.fromCallable(() -> {
            return new CacheStats("redis-cache");
        });
    }
    
    @Override
    public Mono<Boolean> isHealthy() {
        return redisTemplate.opsForValue().get("health-check")
            .map(value -> true)
            .onErrorReturn(false);
    }
    
    @Override
    public Mono<Void> clearAll() {
        return redisTemplate.getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .flushAll()
            .doOnSuccess(result -> log.info("Cache cleared successfully"))
            .doOnError(error -> log.error("Cache clear error: {}", error.getMessage()))
            .then();
    }
}
