package com.custom.indexbasket.basket.event;

import reactor.core.publisher.Mono;

/**
 * Event Publisher interface for publishing basket events to downstream services
 * Supports multiple communication protocols based on latency requirements
 */
public interface EventPublisher {
    
    /**
     * Publish an event to downstream services
     * Uses the Smart Communication Router to select optimal protocol
     */
    Mono<Void> publishEvent(BasketEvent event);
    
    /**
     * Publish event with specific protocol preference
     */
    Mono<Void> publishEvent(BasketEvent event, CommunicationProtocol protocol);
    
    /**
     * Publish event with retry mechanism
     */
    Mono<Void> publishEventWithRetry(BasketEvent event, int maxRetries);
    
    /**
     * Publish event to specific downstream service
     */
    Mono<Void> publishEventToService(BasketEvent event, String serviceName);
    
    /**
     * Publish event to multiple downstream services
     */
    Mono<Void> publishEventToMultipleServices(BasketEvent event, String... serviceNames);
    
    /**
     * Check if event publishing is healthy
     */
    Mono<Boolean> isHealthy();
    
    /**
     * Get publishing statistics
     */
    Mono<PublishingStats> getPublishingStats();
    
    /**
     * Communication protocols supported by the platform
     */
    enum CommunicationProtocol {
        REST_API,           // >100ms latency - User operations
        EVENT_STREAMING,    // 10-100ms latency - Workflows
        GRPC,              // <10ms latency - Internal calls
        ACTOR_MODEL        // <1ms latency - Real-time processing
    }
    
    /**
     * Publishing statistics
     */
    class PublishingStats {
        private long totalEventsPublished;
        private long successfulPublications;
        private long failedPublications;
        private long eventsInQueue;
        private double averagePublishingLatencyMs;
        
        // Getters and setters
        public long getTotalEventsPublished() { return totalEventsPublished; }
        public void setTotalEventsPublished(long totalEventsPublished) { this.totalEventsPublished = totalEventsPublished; }
        
        public long getSuccessfulPublications() { return successfulPublications; }
        public void setSuccessfulPublications(long successfulPublications) { this.successfulPublications = successfulPublications; }
        
        public long getFailedPublications() { return failedPublications; }
        public void setFailedPublications(long failedPublications) { this.failedPublications = failedPublications; }
        
        public long getEventsInQueue() { return eventsInQueue; }
        public void setEventsInQueue(long eventsInQueue) { this.eventsInQueue = eventsInQueue; }
        
        public double getAveragePublishingLatencyMs() { return averagePublishingLatencyMs; }
        public void setAveragePublishingLatencyMs(double averagePublishingLatencyMs) { this.averagePublishingLatencyMs = averagePublishingLatencyMs; }
        
        public double getSuccessRate() {
            return totalEventsPublished > 0 ? (double) successfulPublications / totalEventsPublished * 100 : 0.0;
        }
    }
}
