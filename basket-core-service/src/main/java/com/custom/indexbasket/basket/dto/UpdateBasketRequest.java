package com.custom.indexbasket.basket.dto;

import com.custom.indexbasket.common.model.BasketStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO for updating an existing basket
 * All fields are optional - only provided fields will be updated
 */
public record UpdateBasketRequest(
    @JsonProperty("basketName")
    String basketName,
    
    @JsonProperty("description")
    String description,
    
    @JsonProperty("basketType")
    String basketType,
    
    @JsonProperty("baseCurrency")
    String baseCurrency,
    
    @JsonProperty("totalWeight")
    BigDecimal totalWeight,
    
    @JsonProperty("status")
    BasketStatus status,
    
    @JsonProperty("version")
    String version,
    
    @JsonProperty("updatedBy")
    String updatedBy
) {
    // Validation can be added here if needed
}
