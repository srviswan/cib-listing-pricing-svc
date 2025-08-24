package com.custom.indexbasket.marketdata.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Basket Market Data Entity - Links baskets with their market data and calculated metrics
 * 
 * This entity captures:
 * - Basket-specific market calculations
 * - Real-time market values
 * - Performance metrics
 * - Risk indicators
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Table("basket_market_data")
public class BasketMarketDataEntity {

    @Id
    private UUID id;

    @Column("basket_id")
    private UUID basketId;

    @Column("basket_code")
    private String basketCode;

    @Column("basket_name")
    private String basketName;

    @Column("total_market_value")
    private BigDecimal totalMarketValue;

    @Column("total_weight")
    private BigDecimal totalWeight;

    @Column("constituent_count")
    private Integer constituentCount;

    @Column("last_rebalance_date")
    private LocalDateTime lastRebalanceDate;

    @Column("next_rebalance_date")
    private LocalDateTime nextRebalanceDate;

    @Column("base_currency")
    private String baseCurrency;

    @Column("market_cap_total")
    private BigDecimal marketCapTotal;

    @Column("pe_ratio_weighted")
    private BigDecimal peRatioWeighted;

    @Column("dividend_yield_weighted")
    private BigDecimal dividendYieldWeighted;

    @Column("beta_weighted")
    private BigDecimal betaWeighted;

    @Column("volatility_weighted")
    private BigDecimal volatilityWeighted;

    @Column("sector_diversification_score")
    private BigDecimal sectorDiversificationScore;

    @Column("geographic_diversification_score")
    private BigDecimal geographicDiversificationScore;

    @Column("risk_score")
    private BigDecimal riskScore;

    @Column("performance_score")
    private BigDecimal performanceScore;

    @Column("data_quality_score")
    private BigDecimal dataQualityScore;

    @Column("last_updated")
    private LocalDateTime lastUpdated;

    @Column("is_active")
    private Boolean isActive;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("entity_version")
    private Integer entityVersion;

    // Default constructor
    public BasketMarketDataEntity() {}

    // All-args constructor
    public BasketMarketDataEntity(UUID id, UUID basketId, String basketCode, String basketName,
                                 BigDecimal totalMarketValue, BigDecimal totalWeight, Integer constituentCount,
                                 LocalDateTime lastRebalanceDate, LocalDateTime nextRebalanceDate,
                                 String baseCurrency, BigDecimal marketCapTotal, BigDecimal peRatioWeighted,
                                 BigDecimal dividendYieldWeighted, BigDecimal betaWeighted, BigDecimal volatilityWeighted,
                                 BigDecimal sectorDiversificationScore, BigDecimal geographicDiversificationScore,
                                 BigDecimal riskScore, BigDecimal performanceScore, BigDecimal dataQualityScore,
                                 LocalDateTime lastUpdated, Boolean isActive, LocalDateTime createdAt,
                                 LocalDateTime updatedAt, Integer entityVersion) {
        this.id = id;
        this.basketId = basketId;
        this.basketCode = basketCode;
        this.basketName = basketName;
        this.totalMarketValue = totalMarketValue;
        this.totalWeight = totalWeight;
        this.constituentCount = constituentCount;
        this.lastRebalanceDate = lastRebalanceDate;
        this.nextRebalanceDate = nextRebalanceDate;
        this.baseCurrency = baseCurrency;
        this.marketCapTotal = marketCapTotal;
        this.peRatioWeighted = peRatioWeighted;
        this.dividendYieldWeighted = dividendYieldWeighted;
        this.betaWeighted = betaWeighted;
        this.volatilityWeighted = volatilityWeighted;
        this.sectorDiversificationScore = sectorDiversificationScore;
        this.geographicDiversificationScore = geographicDiversificationScore;
        this.riskScore = riskScore;
        this.performanceScore = performanceScore;
        this.dataQualityScore = dataQualityScore;
        this.lastUpdated = lastUpdated;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.entityVersion = entityVersion;
    }

    // Builder pattern
    public static BasketMarketDataEntityBuilder builder() {
        return new BasketMarketDataEntityBuilder();
    }

    public static class BasketMarketDataEntityBuilder {
        private UUID id;
        private UUID basketId;
        private String basketCode;
        private String basketName;
        private BigDecimal totalMarketValue;
        private BigDecimal totalWeight;
        private Integer constituentCount;
        private LocalDateTime lastRebalanceDate;
        private LocalDateTime nextRebalanceDate;
        private String baseCurrency;
        private BigDecimal marketCapTotal;
        private BigDecimal peRatioWeighted;
        private BigDecimal dividendYieldWeighted;
        private BigDecimal betaWeighted;
        private BigDecimal volatilityWeighted;
        private BigDecimal sectorDiversificationScore;
        private BigDecimal geographicDiversificationScore;
        private BigDecimal riskScore;
        private BigDecimal performanceScore;
        private BigDecimal dataQualityScore;
        private LocalDateTime lastUpdated;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Integer entityVersion;

        public BasketMarketDataEntityBuilder id(UUID id) { this.id = id; return this; }
        public BasketMarketDataEntityBuilder basketId(UUID basketId) { this.basketId = basketId; return this; }
        public BasketMarketDataEntityBuilder basketCode(String basketCode) { this.basketCode = basketCode; return this; }
        public BasketMarketDataEntityBuilder basketName(String basketName) { this.basketName = basketName; return this; }
        public BasketMarketDataEntityBuilder totalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; return this; }
        public BasketMarketDataEntityBuilder totalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; return this; }
        public BasketMarketDataEntityBuilder constituentCount(Integer constituentCount) { this.constituentCount = constituentCount; return this; }
        public BasketMarketDataEntityBuilder lastRebalanceDate(LocalDateTime lastRebalanceDate) { this.lastRebalanceDate = lastRebalanceDate; return this; }
        public BasketMarketDataEntityBuilder nextRebalanceDate(LocalDateTime nextRebalanceDate) { this.nextRebalanceDate = nextRebalanceDate; return this; }
        public BasketMarketDataEntityBuilder baseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; return this; }
        public BasketMarketDataEntityBuilder marketCapTotal(BigDecimal marketCapTotal) { this.marketCapTotal = marketCapTotal; return this; }
        public BasketMarketDataEntityBuilder peRatioWeighted(BigDecimal peRatioWeighted) { this.peRatioWeighted = peRatioWeighted; return this; }
        public BasketMarketDataEntityBuilder dividendYieldWeighted(BigDecimal dividendYieldWeighted) { this.dividendYieldWeighted = dividendYieldWeighted; return this; }
        public BasketMarketDataEntityBuilder betaWeighted(BigDecimal betaWeighted) { this.betaWeighted = betaWeighted; return this; }
        public BasketMarketDataEntityBuilder volatilityWeighted(BigDecimal volatilityWeighted) { this.volatilityWeighted = volatilityWeighted; return this; }
        public BasketMarketDataEntityBuilder sectorDiversificationScore(BigDecimal sectorDiversificationScore) { this.sectorDiversificationScore = sectorDiversificationScore; return this; }
        public BasketMarketDataEntityBuilder geographicDiversificationScore(BigDecimal geographicDiversificationScore) { this.geographicDiversificationScore = geographicDiversificationScore; return this; }
        public BasketMarketDataEntityBuilder riskScore(BigDecimal riskScore) { this.riskScore = riskScore; return this; }
        public BasketMarketDataEntityBuilder performanceScore(BigDecimal performanceScore) { this.performanceScore = performanceScore; return this; }
        public BasketMarketDataEntityBuilder dataQualityScore(BigDecimal dataQualityScore) { this.dataQualityScore = dataQualityScore; return this; }
        public BasketMarketDataEntityBuilder lastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; return this; }
        public BasketMarketDataEntityBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public BasketMarketDataEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public BasketMarketDataEntityBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public BasketMarketDataEntityBuilder entityVersion(Integer entityVersion) { this.entityVersion = entityVersion; return this; }

        public BasketMarketDataEntity build() {
            return new BasketMarketDataEntity(id, basketId, basketCode, basketName, totalMarketValue, totalWeight,
                constituentCount, lastRebalanceDate, nextRebalanceDate, baseCurrency, marketCapTotal,
                peRatioWeighted, dividendYieldWeighted, betaWeighted, volatilityWeighted,
                sectorDiversificationScore, geographicDiversificationScore, riskScore, performanceScore,
                dataQualityScore, lastUpdated, isActive, createdAt, updatedAt, entityVersion);
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBasketId() { return basketId; }
    public void setBasketId(UUID basketId) { this.basketId = basketId; }

    public String getBasketCode() { return basketCode; }
    public void setBasketCode(String basketCode) { this.basketCode = basketCode; }

    public String getBasketName() { return basketName; }
    public void setBasketName(String basketName) { this.basketName = basketName; }

    public BigDecimal getTotalMarketValue() { return totalMarketValue; }
    public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }

    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }

    public Integer getConstituentCount() { return constituentCount; }
    public void setConstituentCount(Integer constituentCount) { this.constituentCount = constituentCount; }

    public LocalDateTime getLastRebalanceDate() { return lastRebalanceDate; }
    public void setLastRebalanceDate(LocalDateTime lastRebalanceDate) { this.lastRebalanceDate = lastRebalanceDate; }

    public LocalDateTime getNextRebalanceDate() { return nextRebalanceDate; }
    public void setNextRebalanceDate(LocalDateTime nextRebalanceDate) { this.nextRebalanceDate = nextRebalanceDate; }

    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }

    public BigDecimal getMarketCapTotal() { return marketCapTotal; }
    public void setMarketCapTotal(BigDecimal marketCapTotal) { this.marketCapTotal = marketCapTotal; }

    public BigDecimal getPeRatioWeighted() { return peRatioWeighted; }
    public void setPeRatioWeighted(BigDecimal peRatioWeighted) { this.peRatioWeighted = peRatioWeighted; }

    public BigDecimal getDividendYieldWeighted() { return dividendYieldWeighted; }
    public void setDividendYieldWeighted(BigDecimal dividendYieldWeighted) { this.dividendYieldWeighted = dividendYieldWeighted; }

    public BigDecimal getBetaWeighted() { return betaWeighted; }
    public void setBetaWeighted(BigDecimal betaWeighted) { this.betaWeighted = betaWeighted; }

    public BigDecimal getVolatilityWeighted() { return volatilityWeighted; }
    public void setVolatilityWeighted(BigDecimal volatilityWeighted) { this.volatilityWeighted = volatilityWeighted; }

    public BigDecimal getSectorDiversificationScore() { return sectorDiversificationScore; }
    public void setSectorDiversificationScore(BigDecimal sectorDiversificationScore) { this.sectorDiversificationScore = sectorDiversificationScore; }

    public BigDecimal getGeographicDiversificationScore() { return geographicDiversificationScore; }
    public void setGeographicDiversificationScore(BigDecimal geographicDiversificationScore) { this.geographicDiversificationScore = geographicDiversificationScore; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public BigDecimal getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(BigDecimal performanceScore) { this.performanceScore = performanceScore; }

    public BigDecimal getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(BigDecimal dataQualityScore) { this.dataQualityScore = dataQualityScore; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getEntityVersion() { return entityVersion; }
    public void setEntityVersion(Integer entityVersion) { this.entityVersion = entityVersion; }
}
