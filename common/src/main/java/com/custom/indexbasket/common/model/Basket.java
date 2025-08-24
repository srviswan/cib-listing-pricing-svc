package com.custom.indexbasket.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core Basket domain model representing a thematic investment basket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Basket {

    @NotBlank(message = "Basket code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Basket code must be 3-50 characters, uppercase letters, numbers, and underscores only")
    private String basketCode;

    @NotBlank(message = "Basket name is required")
    @Size(min = 1, max = 255, message = "Basket name must be between 1 and 255 characters")
    private String basketName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Basket type is required")
    @Pattern(regexp = "^(EQUITY|FIXED_INCOME|COMMODITY|MIXED)$", message = "Basket type must be EQUITY, FIXED_INCOME, COMMODITY, or MIXED")
    private String basketType;

    @NotBlank(message = "Base currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency must be a 3-letter currency code")
    private String baseCurrency;

    @NotNull(message = "Total weight is required")
    @DecimalMin(value = "0.01", message = "Total weight must be greater than 0")
    @DecimalMax(value = "100.00", message = "Total weight cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Total weight must have at most 3 digits before decimal and 2 after")
    private BigDecimal totalWeight;

    @NotNull(message = "Status is required")
    private BasketStatus status;

    @NotBlank(message = "Version is required")
    @Pattern(regexp = "^v\\d+\\.\\d+(\\.\\d+)?$", message = "Version must be in format vX.Y or vX.Y.Z")
    private String version;

    private String previousVersion;

    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;

    private String approvedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime approvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime listedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime activatedAt;

    @Valid
    @Size(min = 2, max = 100, message = "Basket must have between 2 and 100 constituents")
    private List<BasketConstituent> constituents;

    // Computed fields
    @JsonIgnore
    public boolean isEditable() {
        return status == BasketStatus.DRAFT || 
               status == BasketStatus.BACKTEST_FAILED || 
               status == BasketStatus.REJECTED;
    }

    @JsonIgnore
    public boolean isOperational() {
        return status == BasketStatus.ACTIVE || status == BasketStatus.SUSPENDED;
    }

    @JsonIgnore
    public boolean isTerminal() {
        return status == BasketStatus.DELETED || status == BasketStatus.DELISTED;
    }

    @JsonIgnore
    public boolean canTransitionTo(BasketStatus targetStatus) {
        return status.canTransitionTo(targetStatus);
    }

    @JsonIgnore
    public boolean isValidForBacktesting() {
        return constituents != null && 
               constituents.size() >= 2 && 
               totalWeight.equals(BigDecimal.valueOf(100.00)) &&
               constituents.stream().allMatch(BasketConstituent::isValid);
    }

    @JsonIgnore
    public boolean isValidForApproval() {
        return status == BasketStatus.BACKTESTED && isValidForBacktesting();
    }

    // Business logic methods
    public void addConstituent(BasketConstituent constituent) {
        if (constituents == null) {
            constituents = new java.util.ArrayList<>();
        }
        constituents.add(constituent);
        recalculateWeights();
    }

    public void removeConstituent(String symbol) {
        if (constituents != null) {
            constituents.removeIf(c -> c.getSymbol().equals(symbol));
            recalculateWeights();
        }
    }

    public void updateConstituentWeight(String symbol, BigDecimal newWeight) {
        if (constituents != null) {
            constituents.stream()
                .filter(c -> c.getSymbol().equals(symbol))
                .findFirst()
                .ifPresent(c -> {
                    c.setWeight(newWeight);
                    recalculateWeights();
                });
        }
    }

    private void recalculateWeights() {
        if (constituents != null && !constituents.isEmpty()) {
            BigDecimal total = constituents.stream()
                .map(BasketConstituent::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                // Normalize weights to sum to 100
                constituents.forEach(c -> {
                    BigDecimal normalizedWeight = c.getWeight()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(total, 4, BigDecimal.ROUND_HALF_UP);
                    c.setWeight(normalizedWeight);
                });
                this.totalWeight = BigDecimal.valueOf(100.00);
            }
        }
    }

    public void validateBasket() {
        if (constituents == null || constituents.size() < 2) {
            throw new IllegalArgumentException("Basket must have at least 2 constituents");
        }

        if (totalWeight.compareTo(BigDecimal.valueOf(100.00)) != 0) {
            throw new IllegalArgumentException("Total weight must equal 100.00");
        }

        constituents.forEach(BasketConstituent::validate);
    }

    @Override
    public String toString() {
        return "Basket{" +
                "basketCode='" + basketCode + '\'' +
                ", basketName='" + basketName + '\'' +
                ", status=" + status +
                ", baseCurrency='" + baseCurrency + '\'' +
                ", totalWeight=" + totalWeight +
                ", constituentCount=" + (constituents != null ? constituents.size() : 0) +
                '}';
    }
}
