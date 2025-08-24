package com.custom.indexbasket.basket.domain;

import com.custom.indexbasket.common.model.BasketConstituent;
import io.r2dbc.spi.Row;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC entity for basket constituents, extending the common BasketConstituent model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("basket_constituents")
public class BasketConstituentEntity extends BasketConstituent {

    @Id
    private UUID entityId; // Renamed to avoid conflict with parent id

    private UUID basketId;
    private String name;
    private String isin;
    private String cusip;
    private String sedol;
    private String exchange;
    private String industry;
    private BigDecimal riskScore;
    private BigDecimal performanceScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long entityVersion; // Renamed to avoid conflict
    private Boolean isActive;
    private LocalDateTime lastPriceUpdate;

    /**
     * Create a BasketConstituentEntity from a database row
     */
    public static BasketConstituentEntity fromRow(Row row) {
        BasketConstituentEntity entity = new BasketConstituentEntity();
        
        // Set common BasketConstituent fields
        entity.setBasketCode(row.get("basket_code", String.class));
        entity.setSymbol(row.get("symbol", String.class));
        entity.setSymbolName(row.get("symbol_name", String.class));
        entity.setWeight(row.get("weight", BigDecimal.class));
        entity.setShares(row.get("shares", Long.class));
        entity.setTargetAllocation(row.get("target_allocation", BigDecimal.class));
        entity.setSector(row.get("sector", String.class));
        entity.setCountry(row.get("country", String.class));
        entity.setCurrency(row.get("currency", String.class));
        entity.setAddedAt(row.get("added_at", LocalDateTime.class));
        entity.setCurrentPrice(row.get("current_price", BigDecimal.class));
        entity.setMarketValue(row.get("market_value", BigDecimal.class));
        entity.setWeightPercentage(row.get("weight_percentage", BigDecimal.class));
        entity.setDailyReturn(row.get("daily_return", BigDecimal.class));
        entity.setCumulativeReturn(row.get("cumulative_return", BigDecimal.class));
        
        // Set service-specific fields
        entity.setEntityId(row.get("id", UUID.class));
        entity.setBasketId(row.get("basket_id", UUID.class));
        entity.setName(row.get("name", String.class));
        entity.setIsin(row.get("isin", String.class));
        entity.setCusip(row.get("cusip", String.class));
        entity.setSedol(row.get("sedol", String.class));
        entity.setExchange(row.get("exchange", String.class));
        entity.setIndustry(row.get("industry", String.class));
        entity.setRiskScore(row.get("risk_score", BigDecimal.class));
        entity.setPerformanceScore(row.get("performance_score", BigDecimal.class));
        entity.setCreatedAt(row.get("created_at", LocalDateTime.class));
        entity.setUpdatedAt(row.get("updated_at", LocalDateTime.class));
        entity.setCreatedBy(row.get("created_by", String.class));
        entity.setUpdatedBy(row.get("updated_by", String.class));
        entity.setEntityVersion(row.get("version", Long.class));
        entity.setIsActive(row.get("is_active", Boolean.class));
        entity.setLastPriceUpdate(row.get("last_price_update", LocalDateTime.class));
        
        return entity;
    }

    /**
     * Convert to common BasketConstituent model
     */
    public BasketConstituent toBasketConstituent() {
        BasketConstituent constituent = new BasketConstituent();
        constituent.setBasketCode(this.getBasketCode());
        constituent.setSymbol(this.getSymbol());
        constituent.setSymbolName(this.getSymbolName());
        constituent.setWeight(this.getWeight());
        constituent.setShares(this.getShares());
        constituent.setTargetAllocation(this.getTargetAllocation());
        constituent.setSector(this.getSector());
        constituent.setCountry(this.getCountry());
        constituent.setCurrency(this.getCurrency());
        constituent.setAddedAt(this.getAddedAt());
        constituent.setCurrentPrice(this.getCurrentPrice());
        constituent.setMarketValue(this.getMarketValue());
        constituent.setWeightPercentage(this.getWeightPercentage());
        constituent.setDailyReturn(this.getDailyReturn());
        constituent.setCumulativeReturn(this.getCumulativeReturn());
        return constituent;
    }

    /**
     * Update price and recalculate derived fields
     */
    public void updatePrice(BigDecimal newPrice) {
        this.setCurrentPrice(newPrice);
        this.setLastPriceUpdate(LocalDateTime.now());
        
        if (this.getShares() != null && newPrice != null) {
            this.setMarketValue(newPrice.multiply(BigDecimal.valueOf(this.getShares())));
        }
    }

    /**
     * Check if constituent is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.getIsActive());
    }
}
