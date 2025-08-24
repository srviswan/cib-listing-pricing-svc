package com.custom.indexbasket.basket.domain;

import com.custom.indexbasket.common.model.BasketStatus;
import io.r2dbc.spi.Row;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC entity for baskets - standalone entity to avoid inheritance issues
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("baskets")
public class BasketEntity {

    @Id
    private UUID id;

    // Core basket fields
    @Column("basket_code")
    private String basketCode;
    @Column("basket_name")
    private String basketName;
    private String description;
    @Column("basket_type")
    private String basketType;
    @Column("base_currency")
    private String baseCurrency;
    @Column("total_weight")
    private BigDecimal totalWeight;
    
    @Column("status")
    private BasketStatus status;
    
    private String version;
    @Column("previous_version")
    private String previousVersion;
    @Column("created_by")
    private String createdBy;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("approved_by")
    private String approvedBy;
    @Column("approved_at")
    private LocalDateTime approvedAt;
    @Column("listed_at")
    private LocalDateTime listedAt;
    @Column("activated_at")
    private LocalDateTime activatedAt;

    // Service-specific fields
    @Column("updated_by")
    private String updatedBy;
    @Column("entity_version")
    private Long entityVersion;
    @Column("is_active")
    private Boolean isActive;
    @Column("last_rebalance_date")
    private LocalDateTime lastRebalanceDate;
    @Column("next_rebalance_date")
    private LocalDateTime nextRebalanceDate;
    @Column("total_market_value")
    private BigDecimal totalMarketValue;
    @Column("risk_metrics")
    private String riskMetrics;
    @Column("performance_metrics")
    private String performanceMetrics;
    @Column("backtest_score")
    private BigDecimal backtestScore;
    @Column("constituent_count")
    private Integer constituentCount;

    /**
     * Create a BasketEntity from a database row
     */
    public static BasketEntity fromRow(Row row) {
        BasketEntity entity = new BasketEntity();
        
        // Set core fields
        entity.setId(row.get("id", UUID.class));
        entity.setBasketCode(row.get("basket_code", String.class));
        entity.setBasketName(row.get("basket_name", String.class));
        entity.setDescription(row.get("description", String.class));
        entity.setBasketType(row.get("basket_type", String.class));
        entity.setBaseCurrency(row.get("base_currency", String.class));
        entity.setTotalWeight(row.get("total_weight", BigDecimal.class));
        entity.setStatus(BasketStatus.valueOf(row.get("status", String.class)));
        entity.setVersion(row.get("version", String.class));
        entity.setPreviousVersion(row.get("previous_version", String.class));
        entity.setCreatedBy(row.get("created_by", String.class));
        entity.setCreatedAt(row.get("created_at", LocalDateTime.class));
        entity.setUpdatedAt(row.get("updated_at", LocalDateTime.class));
        entity.setApprovedBy(row.get("approved_by", String.class));
        entity.setApprovedAt(row.get("approved_at", LocalDateTime.class));
        entity.setListedAt(row.get("listed_at", LocalDateTime.class));
        entity.setActivatedAt(row.get("activated_at", LocalDateTime.class));
        
        // Set service-specific fields
        entity.setUpdatedBy(row.get("updated_by", String.class));
        entity.setEntityVersion(row.get("entity_version", Long.class));
        entity.setIsActive(row.get("is_active", Boolean.class));
        entity.setLastRebalanceDate(row.get("last_rebalance_date", LocalDateTime.class));
        entity.setNextRebalanceDate(row.get("next_rebalance_date", LocalDateTime.class));
        entity.setTotalMarketValue(row.get("total_market_value", BigDecimal.class));
        entity.setRiskMetrics(row.get("risk_metrics", String.class));
        entity.setPerformanceMetrics(row.get("performance_metrics", String.class));
        entity.setBacktestScore(row.get("backtest_score", BigDecimal.class));
        entity.setConstituentCount(row.get("constituent_count", Integer.class));
        
        return entity;
    }

    /**
     * Convert to common Basket model
     */
    public com.custom.indexbasket.common.model.Basket toBasket() {
        com.custom.indexbasket.common.model.Basket basket = new com.custom.indexbasket.common.model.Basket();
        basket.setBasketCode(this.getBasketCode());
        basket.setBasketName(this.getBasketName());
        basket.setDescription(this.getDescription());
        basket.setBasketType(this.getBasketType());
        basket.setBaseCurrency(this.getBaseCurrency());
        basket.setTotalWeight(this.getTotalWeight());
        basket.setStatus(this.getStatus());
        basket.setVersion(this.getVersion());
        basket.setPreviousVersion(this.getPreviousVersion());
        basket.setCreatedBy(this.getCreatedBy());
        basket.setCreatedAt(this.getCreatedAt());
        basket.setUpdatedAt(this.getUpdatedAt());
        basket.setApprovedBy(this.getApprovedBy());
        basket.setApprovedAt(this.getApprovedAt());
        basket.setListedAt(this.getListedAt());
        basket.setActivatedAt(this.getActivatedAt());
        // Note: constituents are loaded separately from basket_constituents table
        return basket;
    }
}
