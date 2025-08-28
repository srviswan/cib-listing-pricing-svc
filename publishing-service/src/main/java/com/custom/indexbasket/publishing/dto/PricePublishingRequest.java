package com.custom.indexbasket.publishing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request for price publishing operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricePublishingRequest {

    @NotBlank(message = "Basket ID is required")
    private String basketId;
    
    @NotBlank(message = "Basket code is required")
    private String basketCode;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    @NotNull(message = "Currency is required")
    private String currency;
    
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    private BigDecimal changeAmount;
    private BigDecimal changePercentage;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private String exchange;
}
