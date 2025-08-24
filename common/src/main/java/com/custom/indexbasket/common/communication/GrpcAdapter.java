package com.custom.indexbasket.common.communication;

/**
 * Adapter interface for gRPC communication.
 * Provides a unified interface for gRPC operations across different implementations.
 */
public interface GrpcAdapter {
    
    /**
     * Execute a gRPC call with the given request
     */
    <T> T executeCall(String service, String method, Object request, Class<T> responseType);
    
    /**
     * Check if the gRPC service is healthy
     */
    boolean isHealthy();
    
    /**
     * Get the current call latency in milliseconds
     */
    long getCallLatencyMs();
    
    /**
     * Get the service endpoint
     */
    String getEndpoint();
    
    /**
     * Get the connection status
     */
    String getConnectionStatus();
}
