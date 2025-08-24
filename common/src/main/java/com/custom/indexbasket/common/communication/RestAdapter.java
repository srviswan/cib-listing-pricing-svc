package com.custom.indexbasket.common.communication;

/**
 * Adapter interface for REST API communication.
 * Provides a unified interface for REST operations across different implementations.
 */
public interface RestAdapter {
    
    /**
     * Execute a REST request with the given parameters
     */
    <T> T executeRequest(String url, String method, Object requestBody, Class<T> responseType);
    
    /**
     * Check if the REST service is healthy
     */
    boolean isHealthy();
    
    /**
     * Get the current response time in milliseconds
     */
    long getResponseTimeMs();
    
    /**
     * Get the service endpoint URL
     */
    String getEndpointUrl();
}
