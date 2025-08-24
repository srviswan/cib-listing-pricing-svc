package com.custom.indexbasket.marketdata.service;

import com.custom.indexbasket.marketdata.dto.BasketMarketDataResponse;
import com.custom.indexbasket.marketdata.dto.MarketDataRequest;
import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.domain.BasketMarketDataEntity;
import com.custom.indexbasket.marketdata.domain.MarketDataEntity;
import com.custom.indexbasket.marketdata.repository.BasketMarketDataRepository;
import com.custom.indexbasket.marketdata.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Market Data Service Implementation - Core market data operations
 * 
 * This service provides:
 * - Real-time market data retrieval and caching
 * - Basket market data calculations and analytics
 * - Risk metrics and performance calculations
 * - Data quality monitoring and health checks
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Service
public class MarketDataServiceImpl implements MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImpl.class);
    private final MarketDataRepository marketDataRepository;
    private final BasketMarketDataRepository basketMarketDataRepository;
    private final MarketDataCalculationService calculationService;

    public MarketDataServiceImpl(MarketDataRepository marketDataRepository, 
                               BasketMarketDataRepository basketMarketDataRepository, 
                               MarketDataCalculationService calculationService) {
        this.marketDataRepository = marketDataRepository;
        this.basketMarketDataRepository = basketMarketDataRepository;
        this.calculationService = calculationService;
    }

    @Override
    public Flux<MarketDataResponse> getMarketData(MarketDataRequest request) {
        log.info("📊 Retrieving market data for {} instruments from {}", 
            request.getInstrumentIds().size(), request.getDataSource());
        
        return Flux.fromIterable(request.getInstrumentIds())
            .flatMap(this::getInstrumentMarketData)
            .doOnComplete(() -> log.info("✅ Market data retrieval completed for {} instruments", request.getInstrumentIds().size()));
    }

    @Override
    public Mono<MarketDataResponse> getInstrumentMarketData(String instrumentId) {
        log.debug("🔍 Retrieving market data for instrument: {}", instrumentId);
        
        return marketDataRepository.findByInstrumentId(instrumentId)
            .map(this::mapToResponse)
            .doOnSuccess(response -> log.debug("✅ Market data retrieved for instrument: {}", instrumentId))
            .doOnError(error -> log.error("❌ Failed to retrieve market data for instrument {}: {}", instrumentId, error.getMessage()));
    }

    @Override
    public Mono<BasketMarketDataResponse> getBasketMarketData(UUID basketId) {
        log.info("📊 Retrieving basket market data for basket: {}", basketId);
        
        return basketMarketDataRepository.findByBasketId(basketId)
            .flatMap(basketData -> {
                // Get constituent market data
                return getConstituentMarketData(basketId)
                    .collectList()
                    .map(constituents -> buildBasketResponse(basketData, constituents));
            })
            .doOnSuccess(response -> log.info("✅ Basket market data retrieved for basket: {}", basketId))
            .doOnError(error -> log.error("❌ Failed to retrieve basket market data for {}: {}", basketId, error.getMessage()));
    }

    @Override
    public Mono<Void> initializeBasketMarketData(UUID basketId, String basketCode) {
        log.info("🚀 Initializing market data for new basket: {}", basketCode);
        
        BasketMarketDataEntity basketData = BasketMarketDataEntity.builder()
            .id(UUID.randomUUID())
            .basketId(basketId)
            .basketCode(basketCode)
            .basketName("New Basket") // Will be updated when basket details are available
            .totalMarketValue(BigDecimal.ZERO)
            .totalWeight(BigDecimal.ZERO)
            .constituentCount(0)
            .baseCurrency("USD")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .entityVersion(1)
            .build();
        
        return basketMarketDataRepository.save(basketData)
            .doOnSuccess(saved -> log.info("✅ Basket market data initialized for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to initialize basket market data for {}: {}", basketCode, error.getMessage()))
            .then();
    }

    @Override
    public Mono<Void> updateBasketMarketData(UUID basketId, String basketCode) {
        log.info("🔄 Updating market data for basket: {}", basketCode);
        
        return basketMarketDataRepository.findByBasketId(basketId)
            .flatMap(existingData -> {
                // Update timestamp
                existingData.setUpdatedAt(LocalDateTime.now());
                existingData.setEntityVersion(existingData.getEntityVersion() + 1);
                
                return basketMarketDataRepository.save(existingData);
            })
            .doOnSuccess(saved -> log.info("✅ Basket market data updated for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to update basket market data for {}: {}", basketCode, error.getMessage()))
            .then();
    }

    @Override
    public Mono<Void> handleBasketStatusChange(UUID basketId, String basketCode, String previousStatus, String newStatus) {
        log.info("🔄 Handling basket status change: {} -> {} for basket: {}", previousStatus, newStatus, basketCode);
        
        return basketMarketDataRepository.findByBasketId(basketId)
            .flatMap(basketData -> {
                // Update based on status change
                if ("ACTIVE".equals(newStatus)) {
                    basketData.setLastRebalanceDate(LocalDateTime.now());
                    basketData.setNextRebalanceDate(LocalDateTime.now().plusMonths(1));
                }
                
                basketData.setUpdatedAt(LocalDateTime.now());
                basketData.setEntityVersion(basketData.getEntityVersion() + 1);
                
                return basketMarketDataRepository.save(basketData);
            })
            .doOnSuccess(saved -> log.info("✅ Basket status change handled for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to handle basket status change for {}: {}", basketCode, error.getMessage()))
            .then();
    }

    @Override
    public Mono<Void> deactivateBasketMarketData(UUID basketId, String basketCode) {
        log.info("🔄 Deactivating market data for basket: {}", basketCode);
        
        return basketMarketDataRepository.findByBasketId(basketId)
            .flatMap(basketData -> {
                basketData.setIsActive(false);
                basketData.setUpdatedAt(LocalDateTime.now());
                basketData.setEntityVersion(basketData.getEntityVersion() + 1);
                
                return basketMarketDataRepository.save(basketData);
            })
            .doOnSuccess(saved -> log.info("✅ Basket market data deactivated for: {}", basketCode))
            .doOnError(error -> log.error("❌ Failed to deactivate basket market data for {}: {}", basketCode, error.getMessage()))
            .then();
    }

    @Override
    public Mono<Void> refreshAllMarketData() {
        log.info("🔄 Refreshing all market data");
        
        return marketDataRepository.findAllActive()
            .flatMap(this::refreshInstrumentMarketData)
            .doOnComplete(() -> log.info("✅ All market data refreshed"))
            .then();
    }

    @Override
    public Mono<MarketDataHealthStatus> getHealthStatus() {
        log.debug("🏥 Checking market data service health");
        
        return Mono.zip(
            marketDataRepository.countActiveInstruments(),
            marketDataRepository.countDataSources(),
            marketDataRepository.getLastUpdateTime(),
            marketDataRepository.getAverageDataQuality()
        ).map(tuple -> new MarketDataHealthStatus(
            true,
            "HEALTHY",
            tuple.getT1().intValue(),
            tuple.getT2().intValue(),
            tuple.getT3().toString(),
            tuple.getT4().toString()
        ));
    }

    // Private helper methods

    private MarketDataResponse mapToResponse(MarketDataEntity entity) {
        return MarketDataResponse.builder()
            .instrumentId(entity.getInstrumentId())
            .symbol(entity.getSymbol())
            .exchange(entity.getExchange())
            .currency(entity.getCurrency())
            .lastPrice(entity.getLastPrice())
            .bidPrice(entity.getBidPrice())
            .askPrice(entity.getAskPrice())
            .volume(entity.getVolume())
            .changeAmount(entity.getChangeAmount())
            .changePercentage(entity.getChangePercentage())
            .dataTimestamp(entity.getDataTimestamp())
            .dataSource(entity.getDataSource())
            .dataQuality(entity.getDataQuality())
            .marketCap(entity.getMarketCap())
            .peRatio(entity.getPeRatio())
            .dividendYield(entity.getDividendYield())
            .beta(entity.getBeta())
            .volatility(entity.getVolatility())
            .build();
    }

    private Flux<BasketMarketDataResponse.ConstituentMarketData> getConstituentMarketData(UUID basketId) {
        // This would typically fetch from a basket constituent service
        // For now, return empty flux
        return Flux.empty();
    }

    private BasketMarketDataResponse buildBasketResponse(BasketMarketDataEntity basketData, 
                                                       List<BasketMarketDataResponse.ConstituentMarketData> constituents) {
        return BasketMarketDataResponse.builder()
            .basketId(basketData.getBasketId().toString())
            .basketCode(basketData.getBasketCode())
            .basketName(basketData.getBasketName())
            .totalMarketValue(basketData.getTotalMarketValue())
            .totalWeight(basketData.getTotalWeight())
            .constituentCount(basketData.getConstituentCount())
            .baseCurrency(basketData.getBaseCurrency())
            .lastRebalanceDate(basketData.getLastRebalanceDate())
            .nextRebalanceDate(basketData.getNextRebalanceDate())
            .marketCapTotal(basketData.getMarketCapTotal())
            .peRatioWeighted(basketData.getPeRatioWeighted())
            .dividendYieldWeighted(basketData.getDividendYieldWeighted())
            .betaWeighted(basketData.getBetaWeighted())
            .volatilityWeighted(basketData.getVolatilityWeighted())
            .sectorDiversificationScore(basketData.getSectorDiversificationScore())
            .geographicDiversificationScore(basketData.getGeographicDiversificationScore())
            .riskScore(basketData.getRiskScore())
            .performanceScore(basketData.getPerformanceScore())
            .dataQualityScore(basketData.getDataQualityScore())
            .constituents(constituents)
            .build();
    }

    private Mono<Void> refreshInstrumentMarketData(MarketDataEntity entity) {
        // This would typically call external market data providers
        // For now, just update the timestamp
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setEntityVersion(entity.getEntityVersion() + 1);
        
        return marketDataRepository.save(entity).then();
    }
}
