package com.custom.indexbasket.sma.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SMA Price Data Model
 * 
 * Represents market data received from Refinitiv SMA API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmaPriceData {
    
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private String currency;
    private String exchange;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private String dataQuality;
    private Map<String, Object> additionalFields;
    
    /**
     * Check if the price data is recent (within specified minutes)
     */
    public boolean isRecent(int maxAgeMinutes) {
        if (timestamp == null) {
            return false;
        }
        return timestamp.isAfter(LocalDateTime.now().minusMinutes(maxAgeMinutes));
    }
    
    /**
     * Get the mid price (average of bid and ask)
     */
    public BigDecimal getMidPrice() {
        if (bidPrice != null && askPrice != null) {
            return bidPrice.add(askPrice).divide(new BigDecimal("2"));
        }
        return lastPrice;
    }
    
    /**
     * Get the spread (difference between ask and bid)
     */
    public BigDecimal getSpread() {
        if (bidPrice != null && askPrice != null) {
            return askPrice.subtract(bidPrice);
        }
        return BigDecimal.ZERO;
    }
}
