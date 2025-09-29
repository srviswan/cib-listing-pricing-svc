package com.custom.indexbasket.integration.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Integration Status Model
 * 
 * Represents the overall status of the integration system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationStatus {
    
    private String status;
    private boolean healthy;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private ServiceStatus smaAdapter;
    private ServiceStatus fixAdapter;
    private ServiceStatus basketCore;
    private ServiceStatus marketData;
    
    private int activeBaskets;
    private int totalBaskets;
    private double averageProcessingTime;
    private int successfulOperations;
    private int failedOperations;
    
    private Map<String, Object> metrics;
    
    /**
     * Check if all services are healthy
     */
    public boolean isAllServicesHealthy() {
        return smaAdapter != null && smaAdapter.isHealthy() &&
               fixAdapter != null && fixAdapter.isHealthy() &&
               basketCore != null && basketCore.isHealthy() &&
               marketData != null && marketData.isHealthy();
    }
    
    /**
     * Get success rate percentage
     */
    public double getSuccessRate() {
        int totalOperations = successfulOperations + failedOperations;
        if (totalOperations == 0) {
            return 100.0;
        }
        return (double) successfulOperations / totalOperations * 100.0;
    }
    
    /**
     * Get service status for a specific service
     */
    public ServiceStatus getServiceStatus(String serviceName) {
        return switch (serviceName.toUpperCase()) {
            case "SMA_ADAPTER" -> smaAdapter;
            case "FIX_ADAPTER" -> fixAdapter;
            case "BASKET_CORE" -> basketCore;
            case "MARKET_DATA" -> marketData;
            default -> null;
        };
    }
    
    /**
     * Service Status Model
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatus {
        private String name;
        private String status;
        private boolean healthy;
        private String url;
        private long responseTime;
        private int successCount;
        private int errorCount;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime lastChecked;
        
        private String lastError;
        
        /**
         * Check if service is healthy
         */
        public boolean isHealthy() {
            return healthy && "UP".equals(status);
        }
        
        /**
         * Get success rate
         */
        public double getSuccessRate() {
            int total = successCount + errorCount;
            if (total == 0) {
                return 100.0;
            }
            return (double) successCount / total * 100.0;
        }
    }
}
