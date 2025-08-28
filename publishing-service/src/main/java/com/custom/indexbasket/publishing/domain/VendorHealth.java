package com.custom.indexbasket.publishing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vendor health status tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("vendor_health")
public class VendorHealth {

    @Id
    private Long id;
    
    private String vendorName;
    private String status; // HEALTHY, DEGRADED, UNHEALTHY
    private LocalDateTime lastHeartbeat;
    private Integer responseTimeMs;
    private BigDecimal errorRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional health metrics
    private Integer consecutiveFailures;
    private String circuitBreakerState; // CLOSED, OPEN, HALF_OPEN
    private String lastErrorMessage;
    private Integer totalRequests;
    private Integer successfulRequests;
    private Integer failedRequests;
}
