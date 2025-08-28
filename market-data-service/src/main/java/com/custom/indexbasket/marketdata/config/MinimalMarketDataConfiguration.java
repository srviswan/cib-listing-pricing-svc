package com.custom.indexbasket.marketdata.config;

import com.custom.indexbasket.marketdata.proxy.ProxyServiceManager;
import com.custom.indexbasket.marketdata.proxy.quality.DataQualityManager;
import com.custom.indexbasket.marketdata.proxy.quality.impl.SimpleDataQualityManager;
import com.custom.indexbasket.marketdata.proxy.quality.DataValidator;
import com.custom.indexbasket.marketdata.proxy.quality.impl.PriceValidator;
import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.caching.impl.InMemoryCacheService;
import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.common.messaging.PublishResult;
import com.custom.indexbasket.common.messaging.PublishingMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal market data configuration for deployment modes without complex proxy services.
 * Provides basic market data functionality with simplified implementations.
 */
@Configuration
@Profile("minimal")
@ConditionalOnProperty(name = "market.data.minimal", havingValue = "true", matchIfMissing = false)
public class MinimalMarketDataConfiguration {
    
    private final Environment environment;
    
    public MinimalMarketDataConfiguration(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Configure a minimal cache service for market data
     */
    @Bean("marketDataCacheService")
    @Primary
    public CacheService marketDataCacheService(MeterRegistry meterRegistry) {
        return new InMemoryCacheService(meterRegistry);
    }
    
    /**
     * Configure a simplified data quality manager for minimal mode
     */
    @Bean
    @Primary
    public DataQualityManager dataQualityManager() {
        List<DataValidator> validators = new ArrayList<>();
        validators.add(new PriceValidator());
        return new SimpleDataQualityManager(validators);
    }
    
    /**
     * Configure a minimal event publisher for minimal mode
     */
    @Bean
    @Primary
    public EventPublisher minimalEventPublisher() {
        return new EventPublisher() {
            @Override
            public <T> Mono<Void> publish(String topic, String key, T event) {
                // In minimal mode, just log the event but don't actually publish
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> publish(String topic, String key, T event, Map<String, String> headers) {
                // In minimal mode, just log the event but don't actually publish
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> publishBatch(String topic, Map<String, T> events) {
                // In minimal mode, just log the events but don't actually publish
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> publishGuaranteed(String topic, String key, T event) {
                // In minimal mode, just log the event but don't actually publish
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<PublishResult> publishWithConfirmation(String topic, String key, T event) {
                // In minimal mode, return a success result but don't actually publish
                return Mono.just(new PublishResult(topic, key, true, 0L));
            }
            
            @Override
            public <T, R> Mono<R> request(String topic, String key, T request, Class<R> responseType, Duration timeout) {
                // In minimal mode, return empty for requests
                return Mono.empty();
            }
            
            @Override
            public Mono<Boolean> isTopicHealthy(String topic) {
                // In minimal mode, always return true
                return Mono.just(true);
            }
            
            @Override
            public Mono<PublishingMetrics> getMetrics() {
                // In minimal mode, return empty metrics
                return Mono.just(new PublishingMetrics("minimal-mode"));
            }
        };
    }
    
    /**
     * Configure a minimal proxy service manager that doesn't require complex proxy services
     */
    @Bean
    @Primary
    public ProxyServiceManager proxyServiceManager(MeterRegistry meterRegistry) {
        // Create a minimal proxy service manager with no data source proxies
        return new ProxyServiceManager(
            new ArrayList<>(), // No proxy services
            marketDataCacheService(meterRegistry), // Use minimal cache service
            new SimpleDataQualityManager(List.of(new PriceValidator())),
            meterRegistry
        );
    }
}
