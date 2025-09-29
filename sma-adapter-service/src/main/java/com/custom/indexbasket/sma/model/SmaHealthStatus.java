package com.custom.indexbasket.sma.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SMA Health Status Model
 * 
 * Represents the health status of the SMA Refinitiv connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmaHealthStatus {
    
    private String status;
    private boolean connected;
    private LocalDateTime lastConnected;
    private LocalDateTime lastError;
    private String lastErrorMessage;
    private int connectionAttempts;
    private int successfulRequests;
    private int failedRequests;
    private double averageResponseTime;
    private Map<String, Object> metrics;
    
    /**
     * Check if the service is healthy
     */
    public boolean isHealthy() {
        return "UP".equals(status) && connected && 
               (lastError == null || lastError.isBefore(LocalDateTime.now().minusMinutes(5)));
    }
    
    /**
     * Get the success rate percentage
     */
    public double getSuccessRate() {
        int totalRequests = successfulRequests + failedRequests;
        if (totalRequests == 0) {
            return 100.0;
        }
        return (double) successfulRequests / totalRequests * 100.0;
    }
}
