package com.custom.indexbasket.basket.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * Smart Communication Router that automatically selects optimal communication protocols
 * based on latency requirements, frequency patterns, and business context
 */
@Slf4j
@Component
public class SmartCommunicationRouter {
    
    // Protocol selection rules based on event characteristics
    private static final Map<String, EventPublisher.CommunicationProtocol> EVENT_PROTOCOL_MAPPING;
    
    static {
        EVENT_PROTOCOL_MAPPING = new HashMap<>();
        
        // Lifecycle events - High priority, real-time updates
        EVENT_PROTOCOL_MAPPING.put("BASKET_CREATED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("BASKET_DELETED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("STATUS_CHANGED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        
        // State machine events - Workflow coordination
        EVENT_PROTOCOL_MAPPING.put("BACKTEST_STARTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("BACKTEST_COMPLETED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("BACKTEST_FAILED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("APPROVAL_REQUESTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("APPROVAL_GRANTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("APPROVAL_REJECTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("LISTING_STARTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("LISTING_COMPLETED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("LISTING_FAILED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("ACTIVATION_STARTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("ACTIVATION_COMPLETED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("SUSPENSION_STARTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("SUSPENSION_COMPLETED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("DELISTING_STARTED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        EVENT_PROTOCOL_MAPPING.put("DELISTING_COMPLETED", EventPublisher.CommunicationProtocol.EVENT_STREAMING);
        
        // Operational events - Standard updates
        EVENT_PROTOCOL_MAPPING.put("BASKET_UPDATED", EventPublisher.CommunicationProtocol.REST_API);
        EVENT_PROTOCOL_MAPPING.put("CONSTITUENT_ADDED", EventPublisher.CommunicationProtocol.REST_API);
        EVENT_PROTOCOL_MAPPING.put("CONSTITUENT_REMOVED", EventPublisher.CommunicationProtocol.REST_API);
        EVENT_PROTOCOL_MAPPING.put("CONSTITUENT_UPDATED", EventPublisher.CommunicationProtocol.REST_API);
        EVENT_PROTOCOL_MAPPING.put("REBALANCE_STARTED", EventPublisher.CommunicationProtocol.REST_API);
        EVENT_PROTOCOL_MAPPING.put("REBALANCE_COMPLETED", EventPublisher.CommunicationProtocol.REST_API);
        
        // Performance events - Monitoring and analytics
        EVENT_PROTOCOL_MAPPING.put("PERFORMANCE_METRICS_UPDATED", EventPublisher.CommunicationProtocol.GRPC);
        EVENT_PROTOCOL_MAPPING.put("RISK_METRICS_UPDATED", EventPublisher.CommunicationProtocol.GRPC);
        EVENT_PROTOCOL_MAPPING.put("MARKET_DATA_UPDATED", EventPublisher.CommunicationProtocol.ACTOR_MODEL);
    }
    
    // Service-specific protocol preferences
    private static final Map<String, EventPublisher.CommunicationProtocol> SERVICE_PROTOCOL_PREFERENCES;
    
    static {
        SERVICE_PROTOCOL_PREFERENCES = new HashMap<>();
        SERVICE_PROTOCOL_PREFERENCES.put("market-data-service", EventPublisher.CommunicationProtocol.GRPC);      // Low latency for market data
        SERVICE_PROTOCOL_PREFERENCES.put("publishing-service", EventPublisher.CommunicationProtocol.EVENT_STREAMING); // Workflow coordination
        SERVICE_PROTOCOL_PREFERENCES.put("analytics-service", EventPublisher.CommunicationProtocol.REST_API);   // Batch processing
        SERVICE_PROTOCOL_PREFERENCES.put("notification-service", EventPublisher.CommunicationProtocol.EVENT_STREAMING); // Real-time notifications
        SERVICE_PROTOCOL_PREFERENCES.put("audit-service", EventPublisher.CommunicationProtocol.REST_API);       // Audit trail
    }
    
    // Performance targets for each protocol
    private static final Map<EventPublisher.CommunicationProtocol, PerformanceTarget> PROTOCOL_PERFORMANCE_TARGETS;
    
    static {
        PROTOCOL_PERFORMANCE_TARGETS = new HashMap<>();
        PROTOCOL_PERFORMANCE_TARGETS.put(EventPublisher.CommunicationProtocol.REST_API, new PerformanceTarget(500, 1000));      // P95 < 500ms, P99 < 1000ms
        PROTOCOL_PERFORMANCE_TARGETS.put(EventPublisher.CommunicationProtocol.EVENT_STREAMING, new PerformanceTarget(100, 200)); // P95 < 100ms, P99 < 200ms
        PROTOCOL_PERFORMANCE_TARGETS.put(EventPublisher.CommunicationProtocol.GRPC, new PerformanceTarget(10, 50));            // P95 < 10ms, P99 < 50ms
        PROTOCOL_PERFORMANCE_TARGETS.put(EventPublisher.CommunicationProtocol.ACTOR_MODEL, new PerformanceTarget(1, 5));        // P95 < 1ms, P99 < 5ms
    }
    
    /**
     * Select optimal communication protocol for an event
     */
    public EventPublisher.CommunicationProtocol selectOptimalProtocol(BasketEvent event) {
        // Check if event has specific protocol mapping
        EventPublisher.CommunicationProtocol eventProtocol = EVENT_PROTOCOL_MAPPING.get(event.getEventType());
        if (eventProtocol != null) {
            log.debug("Selected protocol {} for event type: {}", eventProtocol, event.getEventType());
            return eventProtocol;
        }
        
        // Default based on event category
        EventPublisher.CommunicationProtocol categoryProtocol = selectProtocolByCategory(event.getEventCategory());
        log.debug("Selected protocol {} for event category: {}", categoryProtocol, event.getEventCategory());
        return categoryProtocol;
    }
    
    /**
     * Select protocol based on event category
     */
    private EventPublisher.CommunicationProtocol selectProtocolByCategory(BasketEvent.EventCategory category) {
        return switch (category) {
            case LIFECYCLE -> EventPublisher.CommunicationProtocol.EVENT_STREAMING;  // Real-time workflow updates
            case OPERATIONAL -> EventPublisher.CommunicationProtocol.REST_API;       // Standard CRUD operations
            case PERFORMANCE -> EventPublisher.CommunicationProtocol.GRPC;           // Low-latency metrics
            case BUSINESS -> EventPublisher.CommunicationProtocol.EVENT_STREAMING;   // Business process coordination
            case SYSTEM -> EventPublisher.CommunicationProtocol.GRPC;               // System-level communication
        };
    }
    
    /**
     * Select protocol for specific service
     */
    public EventPublisher.CommunicationProtocol selectProtocolForService(String serviceName) {
        return SERVICE_PROTOCOL_PREFERENCES.getOrDefault(serviceName, EventPublisher.CommunicationProtocol.REST_API);
    }
    
    /**
     * Get performance targets for a protocol
     */
    public PerformanceTarget getPerformanceTarget(EventPublisher.CommunicationProtocol protocol) {
        return PROTOCOL_PERFORMANCE_TARGETS.getOrDefault(protocol, new PerformanceTarget(1000, 2000));
    }
    
    /**
     * Validate if protocol meets performance requirements
     */
    public boolean meetsPerformanceRequirements(EventPublisher.CommunicationProtocol protocol, long actualLatencyMs) {
        PerformanceTarget target = getPerformanceTarget(protocol);
        return actualLatencyMs <= target.p95TargetMs;
    }
    
    /**
     * Performance targets for each protocol
     */
    public static class PerformanceTarget {
        private final long p95TargetMs;
        private final long p99TargetMs;
        
        public PerformanceTarget(long p95TargetMs, long p99TargetMs) {
            this.p95TargetMs = p95TargetMs;
            this.p99TargetMs = p99TargetMs;
        }
        
        public long getP95TargetMs() { return p95TargetMs; }
        public long getP99TargetMs() { return p99TargetMs; }
    }
    
    /**
     * Get routing statistics
     */
    public Mono<RoutingStats> getRoutingStats() {
        return Mono.just(new RoutingStats(
            EVENT_PROTOCOL_MAPPING.size(),
            SERVICE_PROTOCOL_PREFERENCES.size(),
            PROTOCOL_PERFORMANCE_TARGETS.size()
        ));
    }
    
    /**
     * Routing statistics
     */
    public static class RoutingStats {
        private final int eventProtocolMappings;
        private final int serviceProtocolPreferences;
        private final int performanceTargets;
        
        public RoutingStats(int eventProtocolMappings, int serviceProtocolPreferences, int performanceTargets) {
            this.eventProtocolMappings = eventProtocolMappings;
            this.serviceProtocolPreferences = serviceProtocolPreferences;
            this.performanceTargets = performanceTargets;
        }
        
        public int getEventProtocolMappings() { return eventProtocolMappings; }
        public int getServiceProtocolPreferences() { return serviceProtocolPreferences; }
        public int getPerformanceTargets() { return performanceTargets; }
    }
}
