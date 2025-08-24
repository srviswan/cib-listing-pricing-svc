package com.custom.indexbasket.common.communication;

/**
 * Defines the available communication protocols for the Smart Communication Router.
 * Each protocol has specific performance characteristics and use cases.
 */
public enum CommunicationProtocol {
    
    /**
     * REST API - Best for user operations and external integrations
     * Latency: >100ms, Consistency: Strong, Frequency: Low
     */
    REST_API("REST API", "HTTP/REST endpoints for user operations", 100, 500),
    
    /**
     * Event Streaming - Best for workflow orchestration and data pipelines
     * Latency: 10-100ms, Consistency: Eventual, Frequency: Medium
     */
    EVENT_STREAMING("Event Streaming", "Pub/Sub messaging for workflows", 50, 100),
    
    /**
     * gRPC - Best for internal service-to-service communication
     * Latency: <10ms, Consistency: Strong, Frequency: High
     */
    GRPC("gRPC", "High-performance RPC for internal calls", 5, 10),
    
    /**
     * Actor Model - Best for real-time processing and state management
     * Latency: <1ms, Consistency: Eventual, Frequency: Very High
     */
    ACTOR_MODEL("Actor Model", "Real-time processing with Akka", 1, 5);
    
    private final String displayName;
    private final String description;
    private final int targetLatencyMs;
    private final int maxLatencyMs;
    
    CommunicationProtocol(String displayName, String description, int targetLatencyMs, int maxLatencyMs) {
        this.displayName = displayName;
        this.description = description;
        this.targetLatencyMs = targetLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getTargetLatencyMs() {
        return targetLatencyMs;
    }
    
    public int getMaxLatencyMs() {
        return maxLatencyMs;
    }
    
    /**
     * Check if this protocol meets the latency requirement
     */
    public boolean meetsLatencyRequirement(int requiredLatencyMs) {
        return maxLatencyMs <= requiredLatencyMs;
    }
    
    /**
     * Get the performance tier of this protocol
     */
    public PerformanceTier getPerformanceTier() {
        if (maxLatencyMs <= 1) return PerformanceTier.ULTRA_LOW_LATENCY;
        if (maxLatencyMs <= 10) return PerformanceTier.LOW_LATENCY;
        if (maxLatencyMs <= 100) return PerformanceTier.MEDIUM_LATENCY;
        return PerformanceTier.HIGH_LATENCY;
    }
    
    public enum PerformanceTier {
        ULTRA_LOW_LATENCY("Ultra Low Latency", "<1ms"),
        LOW_LATENCY("Low Latency", "<10ms"),
        MEDIUM_LATENCY("Medium Latency", "<100ms"),
        HIGH_LATENCY("High Latency", ">100ms");
        
        private final String displayName;
        private final String latencyRange;
        
        PerformanceTier(String displayName, String latencyRange) {
            this.displayName = displayName;
            this.latencyRange = latencyRange;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getLatencyRange() {
            return latencyRange;
        }
    }
}
