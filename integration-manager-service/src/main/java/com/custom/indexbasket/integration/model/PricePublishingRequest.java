package com.custom.indexbasket.integration.model;

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
    private String basketCode;
    private BigDecimal price;
    private String currency;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private BigDecimal volume;
    private String exchange;
    private String marketDataType;
    
    /**
     * Get formatted symbol for FIX message
     */
    public String getFormattedSymbol() {
        return basketCode != null ? basketCode : symbol;
    }
}
