package com.custom.indexbasket.common.messaging;

import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;

/**
 * Universal event publishing interface
 * Abstracts messaging implementation (Solace vs Kafka)
 */
public interface EventPublisher {
    
    /**
     * Publish single event asynchronously
     */
    <T> Mono<Void> publish(String topic, String key, T event);
    
    /**
     * Publish single event with headers
     */
    <T> Mono<Void> publish(String topic, String key, T event, Map<String, String> headers);
    
    /**
     * Publish batch of events efficiently
     */
    <T> Mono<Void> publishBatch(String topic, Map<String, T> events);
    
    /**
     * Publish with guaranteed delivery (persistence)
     */
    <T> Mono<Void> publishGuaranteed(String topic, String key, T event);
    
    /**
     * Publish with delivery confirmation
     */
    <T> Mono<PublishResult> publishWithConfirmation(String topic, String key, T event);
    
    /**
     * Request-Reply pattern for synchronous operations
     */
    <T, R> Mono<R> request(String topic, String key, T request, Class<R> responseType, Duration timeout);
    
    /**
     * Check if topic exists and is healthy
     */
    Mono<Boolean> isTopicHealthy(String topic);
    
    /**
     * Get publishing metrics
     */
    Mono<PublishingMetrics> getMetrics();
}
