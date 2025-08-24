package com.custom.indexbasket.common.caching.config;

import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.caching.impl.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for cache services.
 * Provides Redis-based caching implementation when Redis is available.
 */
@Configuration
@ConditionalOnClass(ReactiveRedisConnectionFactory.class)
public class CacheConfiguration {
    
    /**
     * Configure ReactiveRedisTemplate for object serialization
     */
    @Bean
    @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true", matchIfMissing = true)
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        RedisSerializationContext<String, Object> context = RedisSerializationContext
            .<String, Object>newSerializationContext()
            .key(stringSerializer)
            .value(jsonSerializer)
            .hashKey(stringSerializer)
            .hashValue(jsonSerializer)
            .build();
            
        ReactiveRedisTemplate<String, Object> template = new ReactiveRedisTemplate<>(connectionFactory, context);
        return template;
    }
    
    /**
     * Configure the common CacheService implementation
     */
    @Bean
    @ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true", matchIfMissing = true)
    public CacheService cacheService(ReactiveRedisTemplate<String, Object> redisTemplate,
                                   ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry) {
        return new RedisCacheService(redisTemplate, objectMapper, meterRegistry);
    }
}
