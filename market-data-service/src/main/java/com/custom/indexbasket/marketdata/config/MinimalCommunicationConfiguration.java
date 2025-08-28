package com.custom.indexbasket.marketdata.config;

import com.custom.indexbasket.common.communication.*;
import com.custom.indexbasket.common.messaging.EventPublisher;
import com.custom.indexbasket.common.messaging.PublishResult;
import com.custom.indexbasket.common.messaging.PublishingMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Minimal communication configuration for deployment modes without complex communication services.
 * Provides simplified implementations of all required communication adapters.
 */
@Configuration
@Profile("minimal")
@ConditionalOnProperty(name = "market.data.minimal", havingValue = "true", matchIfMissing = false)
public class MinimalCommunicationConfiguration {
    
    /**
     * Configure a minimal REST adapter for minimal mode
     */
    @Bean
    @Primary
    public RestAdapter minimalRestAdapter() {
        return new RestAdapter() {
            @Override
            public <T> T executeRequest(String url, String method, Object requestBody, Class<T> responseType) {
                // In minimal mode, return null for requests
                return null;
            }
            
            @Override
            public boolean isHealthy() {
                // In minimal mode, always return true
                return true;
            }
            
            @Override
            public long getResponseTimeMs() {
                // In minimal mode, return 0 for response time
                return 0L;
            }
            
            @Override
            public String getEndpointUrl() {
                // In minimal mode, return a placeholder URL
                return "minimal-mode://localhost";
            }
        };
    }
    
    /**
     * Configure a minimal Event adapter for minimal mode
     */
    @Bean
    @Primary
    public EventAdapter minimalEventAdapter() {
        return new EventAdapter() {
            @Override
            public void publishEvent(String topic, Object event) {
                // In minimal mode, just log the event but don't actually publish
            }
            
            @Override
            public void subscribeToTopic(String topic, EventAdapter.EventHandler handler) {
                // In minimal mode, do nothing
            }
            
            @Override
            public boolean isHealthy() {
                // In minimal mode, always return true
                return true;
            }
            
            @Override
            public long getPublishLatencyMs() {
                // In minimal mode, return 0 for latency
                return 0L;
            }
            
            @Override
            public String getConnectionStatus() {
                // In minimal mode, return connected status
                return "CONNECTED";
            }
        };
    }
    
    /**
     * Configure a minimal gRPC adapter for minimal mode
     */
    @Bean
    @Primary
    public GrpcAdapter minimalGrpcAdapter() {
        return new GrpcAdapter() {
            @Override
            public <T> T executeCall(String service, String method, Object request, Class<T> responseType) {
                // In minimal mode, return null for calls
                return null;
            }
            
            @Override
            public boolean isHealthy() {
                // In minimal mode, always return true
                return true;
            }
            
            @Override
            public long getCallLatencyMs() {
                // In minimal mode, return 0 for latency
                return 0L;
            }
            
            @Override
            public String getEndpoint() {
                // In minimal mode, return a placeholder endpoint
                return "minimal-mode://localhost:9090";
            }
            
            @Override
            public String getConnectionStatus() {
                // In minimal mode, return connected status
                return "CONNECTED";
            }
        };
    }
    
    /**
     * Configure a minimal Actor adapter for minimal mode
     */
    @Bean
    @Primary
    public ActorAdapter minimalActorAdapter() {
        return new ActorAdapter() {
            @Override
            public void sendMessage(String actorPath, Object message) {
                // In minimal mode, just log the message but don't actually send
            }
            
            @Override
            public <T> T askMessage(String actorPath, Object message, Class<T> responseType, long timeoutMs) {
                // In minimal mode, return null for messages
                return null;
            }
            
            @Override
            public boolean isHealthy() {
                // In minimal mode, always return true
                return true;
            }
            
            @Override
            public long getProcessingLatencyMs() {
                // In minimal mode, return 0 for latency
                return 0L;
            }
            
            @Override
            public String getSystemStatus() {
                // In minimal mode, return running status
                return "RUNNING";
            }
            
            @Override
            public int getActiveActorCount() {
                // In minimal mode, return 0 for active actors
                return 0;
            }
        };
    }
    
    /**
     * Configure a minimal SmartCommunicationRouter for minimal mode
     */
    @Bean
    @Primary
    public SmartCommunicationRouter minimalSmartCommunicationRouter() {
        return new SmartCommunicationRouter() {
            @Override
            public Mono<Void> route(CommunicationRequest request) {
                // In minimal mode, just log the routing but don't actually route
                return Mono.empty();
            }
            
            @Override
            public Mono<Void> routeWithProtocol(CommunicationRequest request, CommunicationProtocol protocol) {
                // In minimal mode, just log the routing but don't actually route
                return Mono.empty();
            }
        };
    }
}
