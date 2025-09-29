package com.custom.indexbasket.sma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * SMA Configuration
 * 
 * Configuration for SMA Refinitiv API integration.
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "sma.refinitiv")
public class SmaConfiguration {
    
    private Api api = new Api();
    private Connection connection = new Connection();
    private Subscription subscription = new Subscription();
    private Cache cache = new Cache();
    
    @Data
    public static class Api {
        private String baseUrl;
        private String appKey;
        private String username;
        private String password;
    }
    
    @Data
    public static class Connection {
        private int timeout = 30000;
        private int retryAttempts = 3;
        private int retryDelay = 1000;
        private int maxConnections = 10;
        private boolean keepAlive = true;
    }
    
    @Data
    public static class Subscription {
        private String symbols;
        private String fields;
        private int updateFrequency = 1000;
        private int batchSize = 100;
    }
    
    @Data
    public static class Cache {
        private boolean enabled = true;
        private int ttl = 300;
        private int maxSize = 10000;
        private int cleanupInterval = 600;
    }
    
    @Bean
    public WebClient smaWebClient() {
        return WebClient.builder()
            .baseUrl(api.getBaseUrl())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }
    
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        
        RedisSerializationContext<String, Object> serializationContext = 
            RedisSerializationContext.<String, Object>newSerializationContext()
                .key(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();
        
        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
    
    @Bean
    public Scheduler smaScheduler() {
        return Schedulers.boundedElastic();
    }
}
