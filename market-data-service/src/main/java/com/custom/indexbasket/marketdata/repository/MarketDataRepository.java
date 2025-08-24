package com.custom.indexbasket.marketdata.repository;

import com.custom.indexbasket.marketdata.domain.MarketDataEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Market Data Repository - Data access for market data entities
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Repository
public interface MarketDataRepository extends ReactiveCrudRepository<MarketDataEntity, UUID> {

    /**
     * Find market data by instrument ID
     */
    Mono<MarketDataEntity> findByInstrumentId(String instrumentId);

    /**
     * Find market data by symbol
     */
    Mono<MarketDataEntity> findBySymbol(String symbol);

    /**
     * Find market data by exchange
     */
    Flux<MarketDataEntity> findByExchange(String exchange);

    /**
     * Find all active market data
     */
    @Query("SELECT * FROM market_data WHERE is_active = true")
    Flux<MarketDataEntity> findAllActive();

    /**
     * Find market data by instrument type
     */
    Flux<MarketDataEntity> findByInstrumentType(String instrumentType);

    /**
     * Find market data by currency
     */
    Flux<MarketDataEntity> findByCurrency(String currency);

    /**
     * Find market data updated after timestamp
     */
    @Query("SELECT * FROM market_data WHERE updated_at > :timestamp")
    Flux<MarketDataEntity> findByUpdatedAfter(LocalDateTime timestamp);

    /**
     * Count active instruments
     */
    @Query("SELECT COUNT(*) FROM market_data WHERE is_active = true")
    Mono<Long> countActiveInstruments();

    /**
     * Count distinct data sources
     */
    @Query("SELECT COUNT(DISTINCT data_source) FROM market_data WHERE is_active = true")
    Mono<Long> countDataSources();

    /**
     * Get last update time
     */
    @Query("SELECT MAX(updated_at) FROM market_data WHERE is_active = true")
    Mono<LocalDateTime> getLastUpdateTime();

    /**
     * Get average data quality score
     */
    @Query("SELECT AVG(CASE WHEN data_quality = 'HIGH' THEN 1.0 WHEN data_quality = 'MEDIUM' THEN 0.7 ELSE 0.3 END) FROM market_data WHERE is_active = true")
    Mono<BigDecimal> getAverageDataQuality();

    /**
     * Find market data by data quality
     */
    Flux<MarketDataEntity> findByDataQuality(String dataQuality);

    /**
     * Find market data by data source
     */
    Flux<MarketDataEntity> findByDataSource(String dataSource);
}
