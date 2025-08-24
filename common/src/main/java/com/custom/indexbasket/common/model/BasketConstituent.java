package com.custom.indexbasket.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Basket constituent representing an individual security or asset in a basket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasketConstituent {

    private String id;

    @NotBlank(message = "Basket code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Basket code must be 3-50 characters, uppercase letters, numbers, and underscores only")
    private String basketCode;

    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z0-9.]{1,20}$", message = "Symbol must be 1-20 characters, uppercase letters, numbers, and dots only")
    private String symbol;

    @Size(max = 255, message = "Symbol name must not exceed 255 characters")
    private String symbolName;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.0001", message = "Weight must be greater than 0")
    @DecimalMax(value = "100.0000", message = "Weight cannot exceed 100")
    @Digits(integer = 3, fraction = 4, message = "Weight must have at most 3 digits before decimal and 4 after")
    private BigDecimal weight;

    @Min(value = 1, message = "Shares must be at least 1")
    private Long shares;

    @DecimalMin(value = "0.0001", message = "Target allocation must be greater than 0")
    @DecimalMax(value = "100.0000", message = "Target allocation cannot exceed 100")
    @Digits(integer = 3, fraction = 4, message = "Target allocation must have at most 3 digits before decimal and 4 after")
    private BigDecimal targetAllocation;

    @Size(max = 100, message = "Sector must not exceed 100 characters")
    private String sector;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter currency code")
    private String currency;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime addedAt;

    // Computed fields
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal weightPercentage;
    private BigDecimal dailyReturn;
    private BigDecimal cumulativeReturn;

    /**
     * Validate the constituent
     */
    public void validate() {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }

        if (weight.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Weight cannot exceed 100");
        }

        if (shares != null && shares <= 0) {
            throw new IllegalArgumentException("Shares must be greater than 0");
        }

        if (targetAllocation != null) {
            if (targetAllocation.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Target allocation must be greater than 0");
            }
            if (targetAllocation.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Target allocation cannot exceed 100");
            }
        }
    }

    /**
     * Check if the constituent is valid
     */
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate market value based on current price and shares
     */
    public BigDecimal calculateMarketValue() {
        if (currentPrice != null && shares != null) {
            this.marketValue = currentPrice.multiply(BigDecimal.valueOf(shares));
            return this.marketValue;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate weight percentage of total basket
     */
    public BigDecimal calculateWeightPercentage(BigDecimal totalBasketValue) {
        if (totalBasketValue != null && totalBasketValue.compareTo(BigDecimal.ZERO) > 0 && marketValue != null) {
            this.weightPercentage = marketValue
                .multiply(BigDecimal.valueOf(100))
                .divide(totalBasketValue, 4, BigDecimal.ROUND_HALF_UP);
            return this.weightPercentage;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Update price and recalculate derived values
     */
    public void updatePrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        calculateMarketValue();
    }

    /**
     * Update shares and recalculate derived values
     */
    public void updateShares(Long newShares) {
        this.shares = newShares;
        calculateMarketValue();
    }

    /**
     * Check if constituent has price data
     */
    public boolean hasPriceData() {
        return currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if constituent has market value
     */
    public boolean hasMarketValue() {
        return marketValue != null && marketValue.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get display name (symbol name if available, otherwise symbol)
     */
    public String getDisplayName() {
        return symbolName != null && !symbolName.trim().isEmpty() ? symbolName : symbol;
    }

    /**
     * Check if constituent is overweight relative to target
     */
    public boolean isOverweight() {
        if (targetAllocation != null && weight != null) {
            return weight.compareTo(targetAllocation) > 0;
        }
        return false;
    }

    /**
     * Check if constituent is underweight relative to target
     */
    public boolean isUnderweight() {
        if (targetAllocation != null && weight != null) {
            return weight.compareTo(targetAllocation) < 0;
        }
        return false;
    }

    /**
     * Get weight deviation from target
     */
    public BigDecimal getWeightDeviation() {
        if (targetAllocation != null && weight != null) {
            return weight.subtract(targetAllocation);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get absolute weight deviation from target
     */
    public BigDecimal getAbsoluteWeightDeviation() {
        return getWeightDeviation().abs();
    }

    /**
     * Check if constituent needs rebalancing (deviation > 1%)
     */
    public boolean needsRebalancing() {
        return getAbsoluteWeightDeviation().compareTo(BigDecimal.valueOf(1.0)) > 0;
    }

    /**
     * Get sector classification
     */
    public String getSectorClassification() {
        if (sector != null && !sector.trim().isEmpty()) {
            return sector;
        }
        return "UNCLASSIFIED";
    }

    /**
     * Get country classification
     */
    public String getCountryClassification() {
        if (country != null && !country.trim().isEmpty()) {
            return country;
        }
        return "UNKNOWN";
    }

    /**
     * Get currency classification
     */
    public String getCurrencyClassification() {
        if (currency != null && !currency.trim().isEmpty()) {
            return currency;
        }
        return "USD"; // Default to USD
    }

    @Override
    public String toString() {
        return "BasketConstituent{" +
                "symbol='" + symbol + '\'' +
                ", symbolName='" + symbolName + '\'' +
                ", weight=" + weight +
                ", shares=" + shares +
                ", sector='" + sector + '\'' +
                ", country='" + country + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        BasketConstituent that = (BasketConstituent) o;
        
        if (basketCode != null ? !basketCode.equals(that.basketCode) : that.basketCode != null) return false;
        return symbol != null ? symbol.equals(that.symbol) : that.symbol == null;
    }

    @Override
    public int hashCode() {
        int result = basketCode != null ? basketCode.hashCode() : 0;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        return result;
    }
}
