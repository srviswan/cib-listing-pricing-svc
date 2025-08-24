package com.custom.indexbasket.common.communication;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a communication request with specific requirements for routing.
 * Used by the Smart Communication Router to select the optimal protocol.
 */
public class CommunicationRequest {
    
    private final String requestId;
    private final String operation;
    private final LatencyRequirement latencyRequirement;
    private final ConsistencyRequirement consistencyRequirement;
    private final FrequencyRequirement frequencyRequirement;
    private final Map<String, Object> context;
    private final Instant timestamp;
    
    public CommunicationRequest(String operation, LatencyRequirement latencyRequirement) {
        this(operation, latencyRequirement, ConsistencyRequirement.STRONG, FrequencyRequirement.LOW);
    }
    
    public CommunicationRequest(String operation, LatencyRequirement latencyRequirement, 
                              ConsistencyRequirement consistencyRequirement, FrequencyRequirement frequencyRequirement) {
        this.requestId = java.util.UUID.randomUUID().toString();
        this.operation = operation;
        this.latencyRequirement = latencyRequirement;
        this.consistencyRequirement = consistencyRequirement;
        this.frequencyRequirement = frequencyRequirement;
        this.context = new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public String getOperation() { return operation; }
    public LatencyRequirement getLatencyRequirement() { return latencyRequirement; }
    public ConsistencyRequirement getConsistencyRequirement() { return consistencyRequirement; }
    public FrequencyRequirement getFrequencyRequirement() { return frequencyRequirement; }
    public Map<String, Object> getContext() { return context; }
    public Instant getTimestamp() { return timestamp; }
    
    // Context management
    public CommunicationRequest withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    public Object getContextValue(String key) {
        return context.get(key);
    }
    
    public boolean hasContext(String key) {
        return context.containsKey(key);
    }
    
    // Enums for requirements
    public enum LatencyRequirement {
        ULTRA_LOW("<1ms", 1),
        LOW("<10ms", 10),
        MEDIUM("<100ms", 100),
        HIGH("<500ms", 500),
        FLEXIBLE(">500ms", Integer.MAX_VALUE);
        
        private final String description;
        private final int maxLatencyMs;
        
        LatencyRequirement(String description, int maxLatencyMs) {
            this.description = description;
            this.maxLatencyMs = maxLatencyMs;
        }
        
        public String getDescription() { return description; }
        public int getMaxLatencyMs() { return maxLatencyMs; }
    }
    
    public enum ConsistencyRequirement {
        STRONG("Strong consistency required"),
        EVENTUAL("Eventual consistency acceptable"),
        WEAK("Weak consistency acceptable");
        
        private final String description;
        
        ConsistencyRequirement(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum FrequencyRequirement {
        VERY_LOW("Very low frequency (<1 req/sec)"),
        LOW("Low frequency (1-10 req/sec)"),
        MEDIUM("Medium frequency (10-100 req/sec)"),
        HIGH("High frequency (100-1000 req/sec)"),
        VERY_HIGH("Very high frequency (>1000 req/sec)");
        
        private final String description;
        
        FrequencyRequirement(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    @Override
    public String toString() {
        return String.format("CommunicationRequest{id=%s, operation=%s, latency=%s, consistency=%s, frequency=%s}",
                requestId, operation, latencyRequirement, consistencyRequirement, frequencyRequirement);
    }
}
