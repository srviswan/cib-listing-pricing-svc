package com.custom.indexbasket.common.communication;

import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.common.caching.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Smart Communication Router that selects optimal communication protocols
 * based on latency requirements, frequency patterns, and business context
 */
@Component
public class SmartCommunicationRouter {
    
    private static final Logger log = LoggerFactory.getLogger(SmartCommunicationRouter.class);
    
    @Autowired
    private RestAdapter restAdapter;
    
    @Autowired
    private EventAdapter eventAdapter;
    
    @Autowired
    private GrpcAdapter grpcAdapter;
    
    @Autowired
    private ActorAdapter actorAdapter;
    
    /**
     * Route communication request using optimal protocol
     */
    public Mono<Void> route(CommunicationRequest request) {
        CommunicationProtocol protocol = selectOptimalProtocol(request);
        
        log.debug("Routing request '{}' via {} protocol", request.getOperation(), protocol);
        
        return switch (protocol) {
            case REST_API -> Mono.fromRunnable(() -> log.debug("REST API processing: {}", request.getOperation()));
            case EVENT_STREAMING -> Mono.fromRunnable(() -> log.debug("Event streaming processing: {}", request.getOperation()));
            case GRPC -> Mono.fromRunnable(() -> log.debug("gRPC processing: {}", request.getOperation()));
            case ACTOR_MODEL -> Mono.fromRunnable(() -> log.debug("Actor model processing: {}", request.getOperation()));
        };
    }
    
    /**
     * Select optimal communication protocol based on request characteristics
     */
    private CommunicationProtocol selectOptimalProtocol(CommunicationRequest request) {
        // Priority 1: Ultra-low latency requirements (<1ms)
        if (request.getLatencyRequirement() == CommunicationRequest.LatencyRequirement.ULTRA_LOW) {
            return CommunicationProtocol.ACTOR_MODEL;
        }
        
        // Priority 2: Low latency requirements (<10ms)
        if (request.getLatencyRequirement() == CommunicationRequest.LatencyRequirement.LOW) {
            return CommunicationProtocol.GRPC;
        }
        
        // Priority 3: Medium latency with eventual consistency
        if (request.getLatencyRequirement() == CommunicationRequest.LatencyRequirement.MEDIUM && 
            request.getConsistencyRequirement() == CommunicationRequest.ConsistencyRequirement.EVENTUAL) {
            return CommunicationProtocol.EVENT_STREAMING;
        }
        
        // Priority 4: High latency, low frequency operations
        if (request.getLatencyRequirement() == CommunicationRequest.LatencyRequirement.HIGH && 
            request.getFrequencyRequirement() == CommunicationRequest.FrequencyRequirement.LOW) {
            return CommunicationProtocol.REST_API;
        }
        
        // Priority 5: High frequency operations
        if (request.getFrequencyRequirement() == CommunicationRequest.FrequencyRequirement.HIGH || 
            request.getFrequencyRequirement() == CommunicationRequest.FrequencyRequirement.VERY_HIGH) {
            return CommunicationProtocol.EVENT_STREAMING;
        }
        
        // Default: REST API for user operations
        return CommunicationProtocol.REST_API;
    }
    
    /**
     * Route with specific protocol override
     */
    public Mono<Void> routeWithProtocol(CommunicationRequest request, CommunicationProtocol protocol) {
        log.debug("Routing request '{}' via explicit {} protocol", request.getOperation(), protocol);
        
        return switch (protocol) {
            case REST_API -> Mono.fromRunnable(() -> log.debug("REST API processing: {}", request.getOperation()));
            case EVENT_STREAMING -> Mono.fromRunnable(() -> log.debug("Event streaming processing: {}", request.getOperation()));
            case GRPC -> Mono.fromRunnable(() -> log.debug("gRPC processing: {}", request.getOperation()));
            case ACTOR_MODEL -> Mono.fromRunnable(() -> log.debug("Actor model processing: {}", request.getOperation()));
        };
    }
    
    /**
     * Get protocol recommendations for an operation
     */
    public ProtocolRecommendation getProtocolRecommendation(String operation) {
        return switch (operation) {
            case "basket.create", "basket.update", "basket.delete" -> 
                new ProtocolRecommendation(CommunicationProtocol.REST_API, 0.9, "User operation, high latency acceptable");
                
            case "basket.approval.submit", "basket.approval.complete" -> 
                new ProtocolRecommendation(CommunicationProtocol.EVENT_STREAMING, 0.8, "Workflow operation, eventual consistency");
                
            case "basket.state.update", "basket.validation" -> 
                new ProtocolRecommendation(CommunicationProtocol.GRPC, 0.95, "Internal service call, low latency required");
                
            case "price.update", "valuation.calculate" -> 
                new ProtocolRecommendation(CommunicationProtocol.ACTOR_MODEL, 0.98, "Real-time processing, ultra-low latency");
                
            case "backtest.execute", "analytics.process" -> 
                new ProtocolRecommendation(CommunicationProtocol.EVENT_STREAMING, 0.85, "Long-running process, async execution");
                
            default -> new ProtocolRecommendation(CommunicationProtocol.REST_API, 0.7, "Default fallback");
        };
    }
    
    /**
     * Check protocol health and availability
     */
    public Mono<Map<CommunicationProtocol, Boolean>> checkProtocolHealth() {
        return Mono.zip(
            Mono.just(restAdapter.isHealthy()),
            Mono.just(eventAdapter.isHealthy()),
            Mono.just(grpcAdapter.isHealthy()),
            Mono.just(actorAdapter.isHealthy())
        ).map(results -> Map.of(
            CommunicationProtocol.REST_API, results.getT1(),
            CommunicationProtocol.EVENT_STREAMING, results.getT2(),
            CommunicationProtocol.GRPC, results.getT3(),
            CommunicationProtocol.ACTOR_MODEL, results.getT4()
        ));
    }
    
    /**
     * Get performance metrics for all protocols
     */
    public Mono<Map<CommunicationProtocol, ProtocolMetrics>> getProtocolMetrics() {
        return Mono.just(Map.of(
            CommunicationProtocol.REST_API, new ProtocolMetrics(CommunicationProtocol.REST_API),
            CommunicationProtocol.EVENT_STREAMING, new ProtocolMetrics(CommunicationProtocol.EVENT_STREAMING),
            CommunicationProtocol.GRPC, new ProtocolMetrics(CommunicationProtocol.GRPC),
            CommunicationProtocol.ACTOR_MODEL, new ProtocolMetrics(CommunicationProtocol.ACTOR_MODEL)
        ));
    }
}
