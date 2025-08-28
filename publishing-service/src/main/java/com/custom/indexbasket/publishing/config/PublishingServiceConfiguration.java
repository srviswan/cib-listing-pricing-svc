package com.custom.indexbasket.publishing.config;

import com.custom.indexbasket.common.caching.CacheService;
import com.custom.indexbasket.common.caching.CacheStats;
import com.custom.indexbasket.common.communication.ActorAdapter;
import com.custom.indexbasket.common.communication.EventAdapter;
import com.custom.indexbasket.common.communication.GrpcAdapter;
import com.custom.indexbasket.common.communication.RestAdapter;
import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.common.messaging.PublishResult;
import com.custom.indexbasket.common.messaging.PublishingMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for Publishing Service.
 * Sets up in-memory implementations of common module beans for development/testing.
 */
@Configuration
@EnableWebFluxSecurity
public class PublishingServiceConfiguration {
    
    /**
     * Simple in-memory cache service for development/testing
     */
    @Bean
    public CacheService cacheService() {
        return new CacheService() {
            private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
            
            @Override
            public <T> Mono<T> get(String key, Class<T> type) {
                Object value = cache.get(key);
                if (value != null && type.isInstance(value)) {
                    return Mono.just(type.cast(value));
                }
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> put(String key, T value) {
                cache.put(key, value);
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> put(String key, T value, Duration ttl) {
                cache.put(key, value);
                return Mono.empty();
            }
            
            @Override
            public Mono<Boolean> exists(String key) {
                return Mono.just(cache.containsKey(key));
            }
            
            @Override
            public Mono<Boolean> delete(String key) {
                Object removed = cache.remove(key);
                return Mono.just(removed != null);
            }
            
            @Override
            public <T> Mono<Void> putAll(Map<String, T> entries) {
                cache.putAll(entries);
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> putAll(Map<String, T> entries, Duration ttl) {
                cache.putAll(entries);
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Map<String, T>> getAll(Set<String> keys, Class<T> type) {
                Map<String, T> result = new HashMap<>();
                for (String key : keys) {
                    Object value = cache.get(key);
                    if (value != null && type.isInstance(value)) {
                        result.put(key, type.cast(value));
                    }
                }
                return Mono.just(result);
            }
            
            @Override
            public Mono<Long> deleteAll(Set<String> keys) {
                long deleted = 0;
                for (String key : keys) {
                    if (cache.remove(key) != null) {
                        deleted++;
                    }
                }
                return Mono.just(deleted);
            }
            
            @Override
            public <T> Mono<T> getAndPut(String key, T value, Class<T> type) {
                Object oldValue = cache.get(key);
                cache.put(key, value);
                if (oldValue != null && type.isInstance(oldValue)) {
                    return Mono.just(type.cast(oldValue));
                }
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<T> putIfAbsent(String key, T value, Class<T> type) {
                Object existing = cache.putIfAbsent(key, value);
                if (existing != null && type.isInstance(existing)) {
                    return Mono.just(type.cast(existing));
                }
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Boolean> replace(String key, T oldValue, T newValue) {
                Object existing = cache.get(key);
                if (existing != null && existing.equals(oldValue)) {
                    cache.put(key, newValue);
                    return Mono.just(true);
                }
                return Mono.just(false);
            }
            
            @Override
            public Mono<Long> increment(String key) {
                return increment(key, 1L);
            }
            
            @Override
            public Mono<Long> increment(String key, long delta) {
                Object existing = cache.get(key);
                long newValue = (existing instanceof Number) ? ((Number) existing).longValue() + delta : delta;
                cache.put(key, newValue);
                return Mono.just(newValue);
            }
            
            @Override
            public Mono<Double> increment(String key, double delta) {
                Object existing = cache.get(key);
                double newValue = (existing instanceof Number) ? ((Number) existing).doubleValue() + delta : delta;
                cache.put(key, newValue);
                return Mono.just(newValue);
            }
            
            @Override
            public Mono<Boolean> expire(String key, Duration ttl) {
                // Simple implementation doesn't track expiration
                return Mono.just(true);
            }
            
            @Override
            public Mono<Duration> getTtl(String key) {
                // Simple implementation doesn't track expiration
                return Mono.empty();
            }
            
            @Override
            public Mono<Boolean> persist(String key) {
                // Simple implementation doesn't track expiration
                return Mono.just(true);
            }
            
            @Override
            public Mono<CacheStats> getStats() {
                return Mono.just(new CacheStats("publishing-service-cache"));
            }
            
            @Override
            public Mono<Boolean> isHealthy() {
                return Mono.just(true);
            }
            
            @Override
            public Mono<Void> clearAll() {
                cache.clear();
                return Mono.empty();
            }
        };
    }
    
    /**
     * Simple in-memory event publisher for development/testing
     */
    @Bean
    public EventPublisher eventPublisher() {
        return new EventPublisher() {
            @Override
            public <T> Mono<Void> publish(String topic, String key, T event) {
                // Simple in-memory implementation - just log the event
                System.out.println("Event published - Topic: " + topic + ", Key: " + key + ", Event: " + event);
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> publish(String topic, String key, T event, Map<String, String> headers) {
                // Simple in-memory implementation - just log the event
                System.out.println("Event published with headers - Topic: " + topic + ", Key: " + key + ", Event: " + event + ", Headers: " + headers);
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> publishBatch(String topic, Map<String, T> events) {
                // Simple in-memory implementation - just log the events
                System.out.println("Batch events published - Topic: " + topic + ", Events: " + events.size());
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<Void> publishGuaranteed(String topic, String key, T event) {
                // Simple in-memory implementation - just log the event
                System.out.println("Guaranteed event published - Topic: " + topic + ", Key: " + key + ", Event: " + event);
                return Mono.empty();
            }
            
            @Override
            public <T> Mono<PublishResult> publishWithConfirmation(String topic, String key, T event) {
                // Simple in-memory implementation - just log the event
                System.out.println("Event published with confirmation - Topic: " + topic + ", Key: " + key + ", Event: " + event);
                return Mono.just(new PublishResult(topic, key, true, 0L));
            }
            
            @Override
            public <T, R> Mono<R> request(String topic, String key, T request, Class<R> responseType, Duration timeout) {
                // Simple in-memory implementation - just log the request
                System.out.println("Request published - Topic: " + topic + ", Key: " + key + ", Request: " + request);
                return Mono.empty();
            }
            
            @Override
            public Mono<Boolean> isTopicHealthy(String topic) {
                // Simple in-memory implementation - always return healthy
                return Mono.just(true);
            }
            
            @Override
            public Mono<PublishingMetrics> getMetrics() {
                // Simple in-memory implementation - return empty metrics
                return Mono.just(new PublishingMetrics("publishing-service"));
            }
        };
    }
    
    /**
     * Security configuration to permit all requests for development
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .httpBasic(httpBasic -> httpBasic.disable());
        
        return http.build();
    }
    
    /**
     * Simple in-memory REST adapter for development/testing
     */
    @Bean
    public RestAdapter restAdapter() {
        return new RestAdapter() {
            @Override
            public <T> T executeRequest(String url, String method, Object requestBody, Class<T> responseType) {
                // Simple in-memory implementation - just log the request
                System.out.println("REST " + method + " request to: " + url + " with body: " + requestBody);
                return null;
            }
            
            @Override
            public boolean isHealthy() {
                return true;
            }
            
            @Override
            public long getResponseTimeMs() {
                return 0L;
            }
            
            @Override
            public String getEndpointUrl() {
                return "http://localhost:8083";
            }
        };
    }
    
    /**
     * Simple in-memory event adapter for development/testing
     */
    @Bean
    public EventAdapter eventAdapter() {
        return new EventAdapter() {
            @Override
            public void publishEvent(String topic, Object event) {
                // Simple in-memory implementation - just log the event
                System.out.println("Event published to topic: " + topic + " with event: " + event);
            }
            
            @Override
            public void subscribeToTopic(String topic, EventAdapter.EventHandler handler) {
                // Simple in-memory implementation - just log the subscription
                System.out.println("Subscribed to topic: " + topic);
            }
            
            @Override
            public boolean isHealthy() {
                return true;
            }
            
            @Override
            public long getPublishLatencyMs() {
                return 0L;
            }
            
            @Override
            public String getConnectionStatus() {
                return "CONNECTED";
            }
        };
    }
    
    /**
     * Simple in-memory gRPC adapter for development/testing
     */
    @Bean
    public GrpcAdapter grpcAdapter() {
        return new GrpcAdapter() {
            @Override
            public <T> T executeCall(String service, String method, Object request, Class<T> responseType) {
                // Simple in-memory implementation - just log the call
                System.out.println("gRPC call to service: " + service + " method: " + method + " with request: " + request);
                return null;
            }
            
            @Override
            public boolean isHealthy() {
                return true;
            }
            
            @Override
            public long getCallLatencyMs() {
                return 0L;
            }
            
            @Override
            public String getEndpoint() {
                return "localhost:9090";
            }
            
            @Override
            public String getConnectionStatus() {
                return "CONNECTED";
            }
        };
    }
    
    /**
     * Simple in-memory actor adapter for development/testing
     */
    @Bean
    public ActorAdapter actorAdapter() {
        return new ActorAdapter() {
            @Override
            public void sendMessage(String actorPath, Object message) {
                // Simple in-memory implementation - just log the message
                System.out.println("Actor message sent to: " + actorPath + " with message: " + message);
            }
            
            @Override
            public <T> T askMessage(String actorPath, Object message, Class<T> responseType, long timeoutMs) {
                // Simple in-memory implementation - just log the message
                System.out.println("Actor ask to: " + actorPath + " with message: " + message);
                return null;
            }
            
            @Override
            public boolean isHealthy() {
                return true;
            }
            
            @Override
            public long getProcessingLatencyMs() {
                return 0L;
            }
            
            @Override
            public String getSystemStatus() {
                return "RUNNING";
            }
            
            @Override
            public int getActiveActorCount() {
                return 0;
            }
        };
    }
}
