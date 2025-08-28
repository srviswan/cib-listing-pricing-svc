package com.custom.indexbasket.publishing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Publishing performance metrics tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("publishing_metrics")
public class PublishingMetrics {

    @Id
    private Long id;
    
    private String basketId;
    private String vendorName;
    private String operationType; // LISTING, PRICE_PUBLISHING
    private Integer durationMs;
    private Boolean success;
    private LocalDateTime timestamp;
    
    // Additional metrics
    private String errorType; // TIMEOUT, CIRCUIT_BREAKER, VENDOR_ERROR, NETWORK_ERROR
    private Integer retryCount;
    private String vendorReference;
    private Long requestSizeBytes;
    private Long responseSizeBytes;
}
