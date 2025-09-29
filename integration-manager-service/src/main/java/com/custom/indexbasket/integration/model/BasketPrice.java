package com.custom.indexbasket.integration.model;

import com.custom.indexbasket.integration.model.SmaPriceData;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Basket Price Model
 * 
 * Represents calculated basket price with constituent prices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasketPrice {
    
    private String basketId;
    private String basketCode;
    private BigDecimal price;
    private String currency;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private BigDecimal changeAmount;
    private BigDecimal changePercentage;
    private BigDecimal previousPrice;
    
    private List<SmaPriceData> constituentPrices;
    private Map<String, BigDecimal> constituentWeights;
    private Map<String, Object> metadata;
    
    /**
     * Calculate change amount from previous price
     */
    public BigDecimal getChangeAmount() {
        if (previousPrice != null && price != null) {
            return price.subtract(previousPrice);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate change percentage from previous price
     */
    public BigDecimal getChangePercentage() {
        if (previousPrice != null && price != null && previousPrice.compareTo(BigDecimal.ZERO) != 0) {
            return getChangeAmount().divide(previousPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Check if the basket price is valid
     */
    public boolean isValid() {
        return basketId != null && !basketId.trim().isEmpty() &&
               price != null && price.compareTo(BigDecimal.ZERO) > 0 &&
               currency != null && !currency.trim().isEmpty() &&
               constituentPrices != null && !constituentPrices.isEmpty();
    }
    
    /**
     * Get total number of constituents
     */
    public int getConstituentCount() {
        return constituentPrices != null ? constituentPrices.size() : 0;
    }
    
    /**
     * Get price per constituent
     */
    public BigDecimal getPricePerConstituent() {
        if (constituentPrices != null && !constituentPrices.isEmpty()) {
            return price.divide(new BigDecimal(constituentPrices.size()), 4, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
