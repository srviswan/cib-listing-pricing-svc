package com.custom.indexbasket.basket.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple Event Publisher implementation
 * Provides event publishing to downstream services with protocol selection
 */
@Slf4j
@Component
public class AkkaEventPublisher implements EventPublisher {
    
    private final SmartCommunicationRouter router;
    
    // Publishing statistics
    private final AtomicLong totalEventsPublished = new AtomicLong(0);
    private final AtomicLong successfulPublications = new AtomicLong(0);
    private final AtomicLong failedPublications = new AtomicLong(0);
    private final AtomicLong eventsInQueue = new AtomicLong(0);
    private final AtomicReference<Double> averagePublishingLatencyMs = new AtomicReference<>(0.0);
    
    // Service registry for downstream services
    private final Map<String, ServiceEndpoint> serviceRegistry = new ConcurrentHashMap<>();
    
    @Autowired
    public AkkaEventPublisher(SmartCommunicationRouter router) {
        this.router = router;
        
        // Initialize service registry
        initializeServiceRegistry();
        
        log.info("🚀 Simple Event Publisher initialized");
    }
    
    /**
     * Initialize service registry with downstream services
     */
    private void initializeServiceRegistry() {
        // Market Data Service - gRPC for low latency
        serviceRegistry.put("market-data-service", new ServiceEndpoint(
            "market-data-service",
            "localhost:9090",
            EventPublisher.CommunicationProtocol.GRPC,
            true
        ));
        
        // Publishing Service - Event Streaming for workflow coordination
        serviceRegistry.put("publishing-service", new ServiceEndpoint(
            "publishing-service",
            "localhost:9091",
            EventPublisher.CommunicationProtocol.EVENT_STREAMING,
            true
        ));
        
        // Analytics Service - REST API for batch processing
        serviceRegistry.put("analytics-service", new ServiceEndpoint(
            "analytics-service",
            "localhost:9092",
            EventPublisher.CommunicationProtocol.REST_API,
            true
        ));
        
        // Notification Service - Event Streaming for real-time notifications
        serviceRegistry.put("notification-service", new ServiceEndpoint(
            "notification-service",
            "localhost:9093",
            EventPublisher.CommunicationProtocol.EVENT_STREAMING,
            true
        ));
        
        // Audit Service - REST API for audit trail
        serviceRegistry.put("audit-service", new ServiceEndpoint(
            "audit-service",
            "localhost:9094",
            EventPublisher.CommunicationProtocol.REST_API,
            true
        ));
        
        log.info("📋 Service registry initialized with {} services", serviceRegistry.size());
    }
    
    @Override
    public Mono<Void> publishEvent(BasketEvent event) {
        EventPublisher.CommunicationProtocol protocol = router.selectOptimalProtocol(event);
        return publishEvent(event, protocol);
    }
    
    @Override
    public Mono<Void> publishEvent(BasketEvent event, CommunicationProtocol protocol) {
        long startTime = System.currentTimeMillis();
        eventsInQueue.incrementAndGet();
        
        return Mono.fromRunnable(() -> {
            // Simulate event publishing based on protocol
            simulateEventPublishing(event, protocol);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doFinally(signalType -> {
            long latency = System.currentTimeMillis() - startTime;
            eventsInQueue.decrementAndGet();
            totalEventsPublished.incrementAndGet();
            successfulPublications.incrementAndGet();
            
            // Update average latency
            double currentAvg = averagePublishingLatencyMs.get();
            double newAvg = (currentAvg * (totalEventsPublished.get() - 1) + latency) / totalEventsPublished.get();
            averagePublishingLatencyMs.set(newAvg);
            
            log.debug("📤 Event published: {} via {} in {}ms", event.getEventType(), protocol, latency);
        })
        .then();
    }
    
    /**
     * Simulate event publishing based on protocol
     */
    private void simulateEventPublishing(BasketEvent event, CommunicationProtocol protocol) {
        switch (protocol) {
            case GRPC -> {
                log.info("📤 Publishing event {} via gRPC to downstream services", event.getEventType());
                // Simulate gRPC publishing with low latency
                try {
                    Thread.sleep(5); // Simulate 5ms latency
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            case EVENT_STREAMING -> {
                log.info("📤 Publishing event {} via Event Streaming to downstream services", event.getEventType());
                // Simulate event streaming publishing
                try {
                    Thread.sleep(50); // Simulate 50ms latency
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            case REST_API -> {
                log.info("📤 Publishing event {} via REST API to downstream services", event.getEventType());
                // Simulate REST API publishing
                try {
                    Thread.sleep(200); // Simulate 200ms latency
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            case ACTOR_MODEL -> {
                log.info("📤 Publishing event {} via Actor Model to downstream services", event.getEventType());
                // Simulate actor model publishing with ultra-low latency
                try {
                    Thread.sleep(1); // Simulate 1ms latency
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    @Override
    public Mono<Void> publishEventWithRetry(BasketEvent event, int maxRetries) {
        return publishEvent(event)
            .retryWhen(reactor.util.retry.Retry.fixedDelay(maxRetries, Duration.ofSeconds(1))
                .filter(throwable -> {
                    log.warn("🔄 Retrying event publication for event: {}", event.getEventId());
                    return true;
                }));
    }
    
    @Override
    public Mono<Void> publishEventToService(BasketEvent event, String serviceName) {
        ServiceEndpoint service = serviceRegistry.get(serviceName);
        if (service == null) {
            return Mono.error(new IllegalArgumentException("Service not found: " + serviceName));
        }
        
        return publishEvent(event, service.getPreferredProtocol());
    }
    
    @Override
    public Mono<Void> publishEventToMultipleServices(BasketEvent event, String... serviceNames) {
        return Mono.when(
            java.util.Arrays.stream(serviceNames)
                .map(serviceName -> publishEventToService(event, serviceName))
                .toList()
        );
    }
    
    @Override
    public Mono<Boolean> isHealthy() {
        return Mono.just(true);
    }
    
    @Override
    public Mono<PublishingStats> getPublishingStats() {
        PublishingStats stats = new PublishingStats();
        stats.setTotalEventsPublished(totalEventsPublished.get());
        stats.setSuccessfulPublications(successfulPublications.get());
        stats.setFailedPublications(failedPublications.get());
        stats.setEventsInQueue(eventsInQueue.get());
        stats.setAveragePublishingLatencyMs(averagePublishingLatencyMs.get());
        
        return Mono.just(stats);
    }
    
    /**
     * Service endpoint information
     */
    private static class ServiceEndpoint {
        private final String serviceName;
        private final String endpoint;
        private final CommunicationProtocol preferredProtocol;
        private final boolean active;
        
        public ServiceEndpoint(String serviceName, String endpoint, CommunicationProtocol preferredProtocol, boolean active) {
            this.serviceName = serviceName;
            this.endpoint = endpoint;
            this.preferredProtocol = preferredProtocol;
            this.active = active;
        }
        
        public String getServiceName() { return serviceName; }
        public String getEndpoint() { return endpoint; }
        public CommunicationProtocol getPreferredProtocol() { return preferredProtocol; }
        public boolean isActive() { return active; }
    }
}
