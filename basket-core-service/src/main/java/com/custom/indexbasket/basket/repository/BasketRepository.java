package com.custom.indexbasket.basket.repository;

import com.custom.indexbasket.basket.domain.BasketEntity;
import com.custom.indexbasket.common.model.BasketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive Repository for Basket Operations
 * 
 * Provides reactive database operations for basket management with
 * custom queries for common business operations.
 */
@Repository
public interface BasketRepository extends ReactiveCrudRepository<BasketEntity, UUID> {

    /**
     * Custom insert method to handle PostgreSQL enum types properly
     */
    @Query("INSERT INTO baskets (basket_code, basket_name, description, basket_type, base_currency, total_weight, status, version, created_by, created_at, updated_at, entity_version, is_active) " +
           "VALUES (:basketCode, :basketName, :description, :basketType, :baseCurrency, :totalWeight, :status::basket_status, :version, :createdBy, :createdAt, :updatedAt, :entityVersion, :isActive) " +
           "RETURNING *")
    Mono<BasketEntity> insertBasketWithEnumCast(
            String basketCode,
            String basketName,
            String description,
            String basketType,
            String baseCurrency,
            BigDecimal totalWeight,
            String status,
            String version,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long entityVersion,
            Boolean isActive
    );

    /**
     * Find basket by name (case-insensitive)
     */
    @Query("SELECT * FROM baskets WHERE LOWER(name) = LOWER(:name) AND is_active = true")
    Mono<BasketEntity> findByNameIgnoreCase(String name);

    /**
     * Find baskets by status
     */
    @Query("SELECT * FROM baskets WHERE status = :status AND is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findByStatus(BasketStatus status);

    /**
     * Find baskets by creator
     */
    @Query("SELECT * FROM baskets WHERE created_by = :createdBy AND is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findByCreatedBy(String createdBy);

    /**
     * Find baskets that need rebalancing
     */
    @Query("SELECT * FROM baskets WHERE next_rebalance_date <= :date AND status IN ('OPERATIONAL', 'SUSPENDED') AND is_active = true")
    Flux<BasketEntity> findBasketsNeedingRebalancing(LocalDateTime date);

    /**
     * Find baskets ready for publishing
     */
    @Query("SELECT * FROM baskets WHERE status IN ('APPROVED', 'BACKTEST_COMPLETE') AND is_active = true ORDER BY created_at ASC")
    Flux<BasketEntity> findBasketsReadyForPublishing();

    /**
     * Find baskets by strategy
     */
    @Query("SELECT * FROM baskets WHERE strategy = :strategy AND is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findByStrategy(String strategy);

    /**
     * Find baskets created within date range
     */
    @Query("SELECT * FROM baskets WHERE created_at BETWEEN :startDate AND :endDate AND is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findByCreatedDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find baskets with high backtest scores
     */
    @Query("SELECT * FROM baskets WHERE backtest_score >= :minScore AND is_active = true ORDER BY backtest_score DESC")
    Flux<BasketEntity> findByBacktestScoreGreaterThan(BigDecimal minScore);

    /**
     * Count baskets by status
     */
    @Query("SELECT COUNT(*) FROM baskets WHERE status = :status AND is_active = true")
    Mono<Long> countByStatus(BasketStatus status);

    /**
     * Find baskets with pending approvals
     */
    @Query("SELECT * FROM baskets WHERE status = 'APPROVAL_REQUESTED' AND is_active = true ORDER BY created_at ASC")
    Flux<BasketEntity> findPendingApprovals();

    /**
     * Find baskets that are operational
     */
    @Query("SELECT * FROM baskets WHERE status = 'OPERATIONAL' AND is_active = true ORDER BY last_rebalance_date ASC")
    Flux<BasketEntity> findOperationalBaskets();

    /**
     * Find baskets by sector exposure
     */
    @Query("""
        SELECT DISTINCT b.* FROM baskets b
        JOIN basket_constituents bc ON b.id = bc.basket_id
        WHERE bc.sector = :sector AND b.is_active = true
        ORDER BY b.created_at DESC
        """)
    Flux<BasketEntity> findBySectorExposure(String sector);

    /**
     * Find baskets by risk score range
     */
    @Query("SELECT * FROM baskets WHERE risk_metrics::json->>'overall_risk_score' >= :minRisk AND risk_metrics::json->>'overall_risk_score' <= :maxRisk AND is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findByRiskScoreRange(BigDecimal minRisk, BigDecimal maxRisk);

    /**
     * Find baskets by performance score range
     */
    @Query("SELECT * FROM baskets WHERE performance_metrics::json->>'overall_performance_score' >= :minPerformance AND performance_metrics::json->>'overall_performance_score' <= :maxPerformance AND is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findByPerformanceScoreRange(BigDecimal minPerformance, BigDecimal maxPerformance);

    /**
     * Find baskets with upcoming rebalancing
     */
    @Query("SELECT * FROM baskets WHERE next_rebalance_date BETWEEN :startDate AND :endDate AND status IN ('OPERATIONAL', 'SUSPENDED') AND is_active = true ORDER BY next_rebalance_date ASC")
    Flux<BasketEntity> findBasketsWithUpcomingRebalancing(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find baskets by constituent count range
     */
    @Query("SELECT * FROM baskets WHERE constituent_count BETWEEN :minCount AND :maxCount AND is_active = true ORDER BY constituent_count ASC")
    Flux<BasketEntity> findByConstituentCountRange(Integer minCount, Integer maxCount);

    /**
     * Find baskets by market value range
     */
    @Query("SELECT * FROM baskets WHERE total_market_value BETWEEN :minValue AND :maxValue AND is_active = true ORDER BY total_market_value DESC")
    Flux<BasketEntity> findByMarketValueRange(BigDecimal minValue, BigDecimal maxValue);

    /**
     * Find baskets that need attention (multiple criteria)
     */
    @Query("""
        SELECT * FROM baskets 
        WHERE (status = 'SUSPENDED' OR next_rebalance_date <= :date OR backtest_score < 0.7)
        AND is_active = true 
        ORDER BY 
            CASE 
                WHEN status = 'SUSPENDED' THEN 1
                WHEN next_rebalance_date <= :date THEN 2
                ELSE 3
            END,
            created_at ASC
        """)
    Flux<BasketEntity> findBasketsNeedingAttention(LocalDateTime date);

    /**
     * Find baskets by basket type
     */
    @Query("SELECT * FROM baskets WHERE basket_type = :basketType AND is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findByBasketType(String basketType);

    /**
     * Find all active baskets with pagination support
     */
    @Query("SELECT * FROM baskets WHERE is_active = true ORDER BY created_at DESC LIMIT :size OFFSET :offset")
    Flux<BasketEntity> findAllWithPagination(int size, int offset);

    /**
     * Find all active baskets
     */
    @Query("SELECT * FROM baskets WHERE is_active = true ORDER BY created_at DESC")
    Flux<BasketEntity> findAllActive();

    /**
     * Count all active baskets
     */
    @Query("SELECT COUNT(*) FROM baskets WHERE is_active = true")
    Mono<Long> countActive();

    /**
     * Custom update method that only updates specific fields
     */
    @Query("UPDATE baskets SET " +
           "basket_name = COALESCE(:basketName, basket_name), " +
           "description = COALESCE(:description, description), " +
           "basket_type = COALESCE(:basketType, basket_type), " +
           "base_currency = COALESCE(:baseCurrency, base_currency), " +
           "total_weight = COALESCE(:totalWeight, total_weight), " +
           "status = COALESCE(:status::basket_status, status), " +
           "version = COALESCE(:version, version), " +
           "updated_by = COALESCE(:updatedBy, updated_by), " +
           "updated_at = :updatedAt, " +
           "entity_version = :entityVersion " +
           "WHERE id = :basketId")
    Mono<Void> updateBasketFields(
            UUID basketId,
            String basketName,
            String description,
            String basketType,
            String baseCurrency,
            BigDecimal totalWeight,
            BasketStatus status,
            String version,
            String updatedBy,
            LocalDateTime updatedAt,
            Long entityVersion
    );

    /**
     * Custom soft delete method that only updates necessary fields
     */
    @Query("UPDATE baskets SET " +
           "is_active = false, " +
           "updated_at = :updatedAt, " +
           "updated_by = :deletedBy, " +
           "entity_version = :entityVersion " +
           "WHERE id = :basketId")
    Mono<Void> softDeleteBasket(
            UUID basketId,
            String deletedBy,
            LocalDateTime updatedAt,
            Long entityVersion
    );
}
