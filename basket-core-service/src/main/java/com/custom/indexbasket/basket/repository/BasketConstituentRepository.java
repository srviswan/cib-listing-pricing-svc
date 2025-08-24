package com.custom.indexbasket.basket.repository;

import com.custom.indexbasket.basket.domain.BasketConstituentEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Repository for Basket Constituent Operations
 * 
 * Provides reactive database operations for basket constituent management with
 * custom queries for common business operations.
 */
@Repository
public interface BasketConstituentRepository extends ReactiveCrudRepository<BasketConstituentEntity, UUID> {

    /**
     * Find all constituents for a specific basket
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND is_active = true ORDER BY weight DESC")
    Flux<BasketConstituentEntity> findByBasketId(UUID basketId);

    /**
     * Find constituent by basket ID and symbol
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND symbol = :symbol AND is_active = true")
    Mono<BasketConstituentEntity> findByBasketIdAndSymbol(UUID basketId, String symbol);

    /**
     * Find constituents by sector within a basket
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND sector = :sector AND is_active = true ORDER BY weight DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndSector(UUID basketId, String sector);

    /**
     * Find constituents by industry within a basket
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND industry = :industry AND is_active = true ORDER BY weight DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndIndustry(UUID basketId, String industry);

    /**
     * Find constituents by country within a basket
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND country = :country AND is_active = true ORDER BY weight DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndCountry(UUID basketId, String country);

    /**
     * Find constituents by exchange within a basket
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND exchange = :exchange AND is_active = true ORDER BY weight DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndExchange(UUID basketId, String exchange);

    /**
     * Find constituents by currency within a basket
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND currency = :currency AND is_active = true ORDER BY weight DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndCurrency(UUID basketId, String currency);

    /**
     * Find constituents with weights above threshold
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND weight >= :minWeight AND is_active = true ORDER BY weight DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndWeightGreaterThan(UUID basketId, BigDecimal minWeight);

    /**
     * Find constituents with weights below threshold
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND weight <= :maxWeight AND is_active = true ORDER BY weight ASC")
    Flux<BasketConstituentEntity> findByBasketIdAndWeightLessThan(UUID basketId, BigDecimal maxWeight);

    /**
     * Find constituents by risk score range
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND risk_score BETWEEN :minRisk AND :maxRisk AND is_active = true ORDER BY risk_score DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndRiskScoreRange(UUID basketId, BigDecimal minRisk, BigDecimal maxRisk);

    /**
     * Find constituents by performance score range
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND performance_score BETWEEN :minPerformance AND :maxPerformance AND is_active = true ORDER BY performance_score DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndPerformanceScoreRange(UUID basketId, BigDecimal minPerformance, BigDecimal maxPerformance);

    /**
     * Find constituents that need price updates
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND (last_price_update IS NULL OR last_price_update < :threshold) AND is_active = true ORDER BY last_price_update ASC NULLS FIRST")
    Flux<BasketConstituentEntity> findConstituentsNeedingPriceUpdates(UUID basketId, LocalDateTime threshold);

    /**
     * Find constituents by market value range
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND market_value BETWEEN :minValue AND :maxValue AND is_active = true ORDER BY market_value DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndMarketValueRange(UUID basketId, BigDecimal minValue, BigDecimal maxValue);

    /**
     * Find constituents by shares range
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND shares BETWEEN :minShares AND :maxShares AND is_active = true ORDER BY shares DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndSharesRange(UUID basketId, BigDecimal minShares, BigDecimal maxShares);

    /**
     * Count constituents by basket ID
     */
    @Query("SELECT COUNT(*) FROM basket_constituents WHERE basket_id = :basketId AND is_active = true")
    Mono<Long> countByBasketId(UUID basketId);

    /**
     * Calculate total weight for a basket
     */
    @Query("SELECT COALESCE(SUM(weight), 0) FROM basket_constituents WHERE basket_id = :basketId AND is_active = true")
    Mono<BigDecimal> calculateTotalWeightByBasketId(UUID basketId);

    /**
     * Calculate total market value for a basket
     */
    @Query("SELECT COALESCE(SUM(market_value), 0) FROM basket_constituents WHERE basket_id = :basketId AND is_active = true")
    Mono<BigDecimal> calculateTotalMarketValueByBasketId(UUID basketId);

    /**
     * Find constituents by ISIN
     */
    @Query("SELECT * FROM basket_constituents WHERE isin = :isin AND is_active = true")
    Flux<BasketConstituentEntity> findByIsin(String isin);

    /**
     * Find constituents by CUSIP
     */
    @Query("SELECT * FROM basket_constituents WHERE cusip = :cusip AND is_active = true")
    Flux<BasketConstituentEntity> findByCusip(String cusip);

    /**
     * Find constituents by SEDOL
     */
    @Query("SELECT * FROM basket_constituents WHERE sedol = :sedol AND is_active = true")
    Flux<BasketConstituentEntity> findBySedol(String sedol);

    /**
     * Find constituents with high weights (top holdings)
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND is_active = true ORDER BY weight DESC LIMIT :limit")
    Flux<BasketConstituentEntity> findTopHoldingsByBasketId(UUID basketId, Integer limit);

    /**
     * Find constituents by sector weight distribution
     */
    @Query("""
        SELECT sector, SUM(weight) as total_weight, COUNT(*) as constituent_count
        FROM basket_constituents 
        WHERE basket_id = :basketId AND is_active = true 
        GROUP BY sector 
        ORDER BY total_weight DESC
        """)
    Flux<SectorWeightSummary> getSectorWeightDistribution(UUID basketId);

    /**
     * Find constituents by country weight distribution
     */
    @Query("""
        SELECT country, SUM(weight) as total_weight, COUNT(*) as constituent_count
        FROM basket_constituents 
        WHERE basket_id = :basketId AND is_active = true 
        GROUP BY country 
        ORDER BY total_weight DESC
        """)
    Flux<CountryWeightSummary> getCountryWeightDistribution(UUID basketId);

    /**
     * Find constituents that need rebalancing (weight deviation > threshold)
     */
    @Query("""
        SELECT * FROM basket_constituents 
        WHERE basket_id = :basketId 
        AND ABS(weight - target_weight) > :threshold 
        AND is_active = true 
        ORDER BY ABS(weight - target_weight) DESC
        """)
    Flux<BasketConstituentEntity> findConstituentsNeedingRebalancing(UUID basketId, BigDecimal threshold);

    /**
     * Find constituents by last update time
     */
    @Query("SELECT * FROM basket_constituents WHERE basket_id = :basketId AND updated_at >= :since AND is_active = true ORDER BY updated_at DESC")
    Flux<BasketConstituentEntity> findByBasketIdAndUpdatedSince(UUID basketId, LocalDateTime since);

    /**
     * Find constituents with missing data
     */
    @Query("""
        SELECT * FROM basket_constituents 
        WHERE basket_id = :basketId 
        AND (current_price IS NULL OR market_value IS NULL OR risk_score IS NULL OR performance_score IS NULL)
        AND is_active = true
        ORDER BY weight DESC
        """)
    Flux<BasketConstituentEntity> findConstituentsWithMissingData(UUID basketId);
}

/**
 * Summary classes for aggregation queries
 */
class SectorWeightSummary {
    private String sector;
    private BigDecimal totalWeight;
    private Long constituentCount;
    
    // Getters and setters
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }
    public Long getConstituentCount() { return constituentCount; }
    public void setConstituentCount(Long constituentCount) { this.constituentCount = constituentCount; }
}

class CountryWeightSummary {
    private String country;
    private BigDecimal totalWeight;
    private Long constituentCount;
    
    // Getters and setters
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }
    public Long getConstituentCount() { return constituentCount; }
    public void setConstituentCount(Long constituentCount) { this.constituentCount = constituentCount; }
}
