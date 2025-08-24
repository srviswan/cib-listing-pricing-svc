package com.custom.indexbasket.marketdata.repository;

import com.custom.indexbasket.marketdata.domain.BasketMarketDataEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Basket Market Data Repository - Data access for basket market data entities
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Repository
public interface BasketMarketDataRepository extends ReactiveCrudRepository<BasketMarketDataEntity, UUID> {

    /**
     * Find basket market data by basket ID
     */
    Mono<BasketMarketDataEntity> findByBasketId(UUID basketId);

    /**
     * Find basket market data by basket code
     */
    Mono<BasketMarketDataEntity> findByBasketCode(String basketCode);

    /**
     * Find all active basket market data
     */
    @Query("SELECT * FROM basket_market_data WHERE is_active = true")
    Flux<BasketMarketDataEntity> findAllActive();

    /**
     * Find basket market data by base currency
     */
    Flux<BasketMarketDataEntity> findByBaseCurrency(String baseCurrency);

    /**
     * Find basket market data by risk score range
     */
    @Query("SELECT * FROM basket_market_data WHERE risk_score BETWEEN :minRisk AND :maxRisk AND is_active = true")
    Flux<BasketMarketDataEntity> findByRiskScoreRange(Double minRisk, Double maxRisk);

    /**
     * Find basket market data by performance score range
     */
    @Query("SELECT * FROM basket_market_data WHERE performance_score BETWEEN :minPerformance AND :maxPerformance AND is_active = true")
    Flux<BasketMarketDataEntity> findByPerformanceScoreRange(Double minPerformance, Double maxPerformance);

    /**
     * Find basket market data by data quality score range
     */
    @Query("SELECT * FROM basket_market_data WHERE data_quality_score BETWEEN :minQuality AND :maxQuality AND is_active = true")
    Flux<BasketMarketDataEntity> findByDataQualityScoreRange(Double minQuality, Double maxQuality);

    /**
     * Find basket market data by constituent count range
     */
    @Query("SELECT * FROM basket_market_data WHERE constituent_count BETWEEN :minCount AND :maxCount AND is_active = true")
    Flux<BasketMarketDataEntity> findByConstituentCountRange(Integer minCount, Integer maxCount);

    /**
     * Find basket market data by market cap range
     */
    @Query("SELECT * FROM basket_market_data WHERE market_cap_total BETWEEN :minMarketCap AND :maxMarketCap AND is_active = true")
    Flux<BasketMarketDataEntity> findByMarketCapRange(Double minMarketCap, Double maxMarketCap);

    /**
     * Find basket market data by sector diversification score range
     */
    @Query("SELECT * FROM basket_market_data WHERE sector_diversification_score BETWEEN :minScore AND :maxScore AND is_active = true")
    Flux<BasketMarketDataEntity> findBySectorDiversificationScoreRange(Double minScore, Double maxScore);

    /**
     * Find basket market data by geographic diversification score range
     */
    @Query("SELECT * FROM basket_market_data WHERE geographic_diversification_score BETWEEN :minScore AND :maxScore AND is_active = true")
    Flux<BasketMarketDataEntity> findByGeographicDiversificationScoreRange(Double minScore, Double maxScore);

    /**
     * Find basket market data that need rebalancing
     */
    @Query("SELECT * FROM basket_market_data WHERE next_rebalance_date <= :currentDate AND is_active = true")
    Flux<BasketMarketDataEntity> findBasketsNeedingRebalancing(java.time.LocalDateTime currentDate);

    /**
     * Count active baskets
     */
    @Query("SELECT COUNT(*) FROM basket_market_data WHERE is_active = true")
    Mono<Long> countActiveBaskets();

    /**
     * Count baskets by base currency
     */
    @Query("SELECT COUNT(*) FROM basket_market_data WHERE base_currency = :currency AND is_active = true")
    Mono<Long> countBasketsByCurrency(String currency);
}
