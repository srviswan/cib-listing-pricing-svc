package com.custom.indexbasket.fix.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Price Publishing Request Model
 * 
 * Represents a request to publish price data via FIX protocol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricePublishingRequest {
    
    private String basketId;
    private String symbol;
    private BigDecimal price;
    private String currency;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private BigDecimal volume;
    private String exchange;
    private String marketDataType;
    
    /**
     * Validate the request
     */
    public boolean isValid() {
        return basketId != null && !basketId.trim().isEmpty() &&
               symbol != null && !symbol.trim().isEmpty() &&
               price != null && price.compareTo(BigDecimal.ZERO) > 0 &&
               currency != null && !currency.trim().isEmpty();
    }
    
    /**
     * Get a formatted symbol for FIX messages
     */
    public String getFormattedSymbol() {
        if (symbol == null) {
            return "";
        }
        // Remove spaces and special characters for FIX compatibility
        return symbol.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
