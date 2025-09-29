package com.custom.indexbasket.integration.config;

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

import java.time.Duration;

/**
 * Integration Configuration
 * 
 * Configuration for integration manager service.
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "integration")
public class IntegrationConfiguration {
    
    private Services services = new Services();
    private Orchestration orchestration = new Orchestration();
    
    @Data
    public static class Services {
        private SmaAdapter smaAdapter = new SmaAdapter();
        private FixAdapter fixAdapter = new FixAdapter();
        private BasketCore basketCore = new BasketCore();
        private MarketData marketData = new MarketData();
    }
    
    @Data
    public static class SmaAdapter {
        private String url = "http://localhost:8084/sma-adapter";
        private int timeout = 5000;
        private int retryAttempts = 3;
    }
    
    @Data
    public static class FixAdapter {
        private String url = "http://localhost:8085/fix-adapter";
        private int timeout = 10000;
        private int retryAttempts = 3;
    }
    
    @Data
    public static class BasketCore {
        private String url = "http://localhost:8081";
        private int timeout = 5000;
        private int retryAttempts = 3;
    }
    
    @Data
    public static class MarketData {
        private String url = "http://localhost:8082";
        private int timeout = 5000;
        private int retryAttempts = 3;
    }
    
    @Data
    public static class Orchestration {
        private BasketPriceCalculation basketPriceCalculation = new BasketPriceCalculation();
        private RealTimePublishing realTimePublishing = new RealTimePublishing();
        private DataValidation dataValidation = new DataValidation();
    }
    
    @Data
    public static class BasketPriceCalculation {
        private boolean enabled = true;
        private int batchSize = 50;
        private int processingInterval = 5000;
    }
    
    @Data
    public static class RealTimePublishing {
        private boolean enabled = true;
        private int publishInterval = 5000;
        private int maxConcurrentBaskets = 100;
    }
    
    @Data
    public static class DataValidation {
        private boolean enabled = true;
        private double priceTolerance = 0.01;
        private int maxAgeMinutes = 5;
    }
    
    @Bean
    public WebClient smaWebClient() {
        return WebClient.builder()
            .baseUrl(services.getSmaAdapter().getUrl())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }
    
    @Bean
    public WebClient fixWebClient() {
        return WebClient.builder()
            .baseUrl(services.getFixAdapter().getUrl())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }
    
    @Bean
    public WebClient basketCoreWebClient() {
        return WebClient.builder()
            .baseUrl(services.getBasketCore().getUrl())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
    }
    
    @Bean
    public WebClient marketDataWebClient() {
        return WebClient.builder()
            .baseUrl(services.getMarketData().getUrl())
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
        
        return new ReactiveRedisTemplate<String, Object>(factory, serializationContext);
    }
}
