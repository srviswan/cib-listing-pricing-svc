package com.custom.indexbasket.publishing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Result of publishing operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishingResult {

    private String vendor;
    private String basketId;
    private String status; // SUCCESS, FAILED, TIMEOUT, CIRCUIT_BREAKER_OPEN
    private String vendorReference; // Vendor's internal reference
    private Duration responseTime;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    // Additional result data
    private String operationType; // LISTING, PRICE_PUBLISHING
    private Integer retryCount;
    private String requestId;
    private String responseData; // JSON string of vendor response
    
    // Static factory methods
    public static PublishingResult success(String vendor, String basketId, String vendorReference, Duration responseTime) {
        return PublishingResult.builder()
            .vendor(vendor)
            .basketId(basketId)
            .status("SUCCESS")
            .vendorReference(vendorReference)
            .responseTime(responseTime)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static PublishingResult failed(String vendor, String basketId, String errorMessage, Duration responseTime) {
        return PublishingResult.builder()
            .vendor(vendor)
            .basketId(basketId)
            .status("FAILED")
            .errorMessage(errorMessage)
            .responseTime(responseTime)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static PublishingResult timeout(String vendor, String basketId, Duration responseTime) {
        return PublishingResult.builder()
            .vendor(vendor)
            .basketId(basketId)
            .status("TIMEOUT")
            .errorMessage("Operation timed out")
            .responseTime(responseTime)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static PublishingResult circuitBreakerOpen(String vendor, String basketId) {
        return PublishingResult.builder()
            .vendor(vendor)
            .basketId(basketId)
            .status("CIRCUIT_BREAKER_OPEN")
            .errorMessage("Circuit breaker is open")
            .timestamp(LocalDateTime.now())
            .build();
    }
}
