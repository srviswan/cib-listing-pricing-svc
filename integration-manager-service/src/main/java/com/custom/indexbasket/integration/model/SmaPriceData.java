package com.custom.indexbasket.integration.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SMA Price Data Model
 * 
 * Represents price data from Refinitiv SMA API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmaPriceData {
    
    private String symbol;
    private BigDecimal price;
    private BigDecimal bid;
    private BigDecimal ask;
    private BigDecimal volume;
    private String currency;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private String exchange;
    private String instrumentType;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    
    /**
     * Get mid price (average of bid and ask)
     */
    public BigDecimal getMidPrice() {
        if (bid != null && ask != null) {
            return bid.add(ask).divide(BigDecimal.valueOf(2));
        }
        return price;
    }
    
    /**
     * Check if data is recent (within last 5 minutes)
     */
    public boolean isRecent() {
        if (timestamp == null) return false;
        return timestamp.isAfter(LocalDateTime.now().minusMinutes(5));
    }
}
