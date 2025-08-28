package com.custom.indexbasket.publishing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Publishing status tracking for basket listing and price publishing operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("publishing_status")
public class PublishingStatus {

    @Id
    private Long id;
    
    private String basketId;
    private String vendorName;
    private String status; // PENDING, IN_PROGRESS, SUCCESS, FAILED
    private LocalDateTime publishedAt;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for tracking
    private String operationType; // LISTING, PRICE_PUBLISHING
    private String vendorReference; // Vendor's internal reference
    private Long responseTimeMs; // Response time in milliseconds
    private String requestData; // JSON string of request data
    private String responseData; // JSON string of response data
}
