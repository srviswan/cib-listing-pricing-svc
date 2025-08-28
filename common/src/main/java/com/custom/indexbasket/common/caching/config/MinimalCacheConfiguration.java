package com.custom.indexbasket.common.caching.config;

import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.caching.impl.InMemoryCacheService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Minimal cache configuration for deployment modes without Redis.
 * Provides in-memory caching implementation only.
 */
@Configuration
@ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "false")
public class MinimalCacheConfiguration {
    
    /**
     * Configure the in-memory CacheService implementation
     * Used when Redis is explicitly disabled
     */
    @Bean
    @Primary
    public CacheService cacheService(MeterRegistry meterRegistry) {
        return new InMemoryCacheService(meterRegistry);
    }
}
