package com.custom.indexbasket.common.communication;

/**
 * Adapter interface for Akka actor model communication.
 * Provides a unified interface for actor operations across different implementations.
 */
public interface ActorAdapter {
    
    /**
     * Send a message to an actor
     */
    void sendMessage(String actorPath, Object message);
    
    /**
     * Send a message and expect a reply
     */
    <T> T askMessage(String actorPath, Object message, Class<T> responseType, long timeoutMs);
    
    /**
     * Check if the actor system is healthy
     */
    boolean isHealthy();
    
    /**
     * Get the current message processing latency in milliseconds
     */
    long getProcessingLatencyMs();
    
    /**
     * Get the actor system status
     */
    String getSystemStatus();
    
    /**
     * Get the number of active actors
     */
    int getActiveActorCount();
}
