package com.custom.indexbasket.common.communication;

/**
 * Adapter interface for event streaming communication.
 * Provides a unified interface for event operations across different implementations.
 */
public interface EventAdapter {
    
    /**
     * Publish an event to the specified topic
     */
    void publishEvent(String topic, Object event);
    
    /**
     * Subscribe to events from the specified topic
     */
    void subscribeToTopic(String topic, EventHandler handler);
    
    /**
     * Check if the event streaming service is healthy
     */
    boolean isHealthy();
    
    /**
     * Get the current publish latency in milliseconds
     */
    long getPublishLatencyMs();
    
    /**
     * Get the broker connection status
     */
    String getConnectionStatus();
    
    /**
     * Functional interface for event handling
     */
    @FunctionalInterface
    interface EventHandler {
        void handleEvent(Object event);
    }
}
