package com.custom.indexbasket.marketdata.proxy.impl;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.AbstractDataSourceProxy;
import com.custom.indexbasket.marketdata.proxy.config.DataSourceConfig;
import com.custom.indexbasket.marketdata.proxy.model.RawMarketData;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * SMA Refinitiv data source proxy implementation.
 * This integrates with the SMA Adapter Service for real-time price data.
 */
@Service
@Slf4j
public class SmaRefinitivProxy extends AbstractDataSourceProxy {
    
    private final WebClient smaWebClient;
    
    public SmaRefinitivProxy(DataSourceConfig config, MeterRegistry meterRegistry, 
                           @Autowired(required = false) WebClient.Builder webClientBuilder) {
        super(config, meterRegistry);
        config.setDataSourceName("SMA_REFINITIV");
        
        // Initialize SMA WebClient if builder is available
        if (webClientBuilder != null) {
            this.smaWebClient = webClientBuilder
                .baseUrl("http://localhost:8084/sma-adapter")
                .build();
        } else {
            this.smaWebClient = WebClient.builder()
                .baseUrl("http://localhost:8084/sma-adapter")
                .build();
        }
    }
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        log.debug("Fetching SMA data for instrument: {}", instrumentId);
        
        return smaWebClient
            .get()
            .uri("/api/v1/sma/prices/{symbol}", instrumentId)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> transformSmaResponseToRawData(instrumentId, response))
            .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(1)))
            .doOnSuccess(data -> log.debug("SMA data fetched successfully for: {}", instrumentId))
            .doOnError(error -> log.error("SMA API error for {}: {}", instrumentId, error.getMessage()));
    }
    
    @Override
    protected MarketDataResponse transformData(RawMarketData rawData) {
        // Transform SMA raw data to MarketDataResponse
        return MarketDataResponse.builder()
            .instrumentId(rawData.getInstrumentId())
            .symbol(rawData.getInstrumentId())
            .lastPrice(rawData.getPrice())
            .bidPrice(rawData.getBidPrice())
            .askPrice(rawData.getAskPrice())
            .openPrice(rawData.getOpenPrice())
            .highPrice(rawData.getHighPrice())
            .lowPrice(rawData.getLowPrice())
            .volume(rawData.getVolume().longValue())
            .currency(rawData.getCurrency())
            .exchange(rawData.getExchange())
            .dataTimestamp(rawData.getTimestamp())
            .dataSource("SMA_REFINITIV")
            .dataQuality("HIGH")
            .changeAmount(BigDecimal.ZERO)
            .changePercentage(BigDecimal.ZERO)
            .build();
    }
    
    @Override
    public Mono<Boolean> isAvailable() {
        return checkAvailability();
    }
    
    @Override
    protected Mono<Boolean> checkAvailability() {
        return smaWebClient
            .get()
            .uri("/api/v1/sma/health")
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> "UP".equals(response.get("status")))
            .onErrorReturn(false)
            .timeout(java.time.Duration.ofSeconds(5));
    }
    
    @Override
    public reactor.core.publisher.Flux<String> getSupportedInstrumentTypes() {
        return reactor.core.publisher.Flux.fromIterable(
            java.util.List.of("STOCK", "BOND", "ETF", "INDEX", "CURRENCY", "COMMODITY")
        );
    }
    
    /**
     * Transform SMA API response to RawMarketData
     */
    private RawMarketData transformSmaResponseToRawData(String instrumentId, Map<String, Object> response) {
        return RawMarketData.builder()
            .instrumentId(instrumentId)
            .dataSource("SMA_REFINITIV")
            .timestamp(LocalDateTime.now())
            .price(extractBigDecimal(response, "lastPrice"))
            .bidPrice(extractBigDecimal(response, "bidPrice"))
            .askPrice(extractBigDecimal(response, "askPrice"))
            .openPrice(extractBigDecimal(response, "openPrice"))
            .highPrice(extractBigDecimal(response, "highPrice"))
            .lowPrice(extractBigDecimal(response, "lowPrice"))
            .volume(extractBigDecimal(response, "volume"))
            .currency((String) response.getOrDefault("currency", "USD"))
            .exchange((String) response.getOrDefault("exchange", "NYSE"))
            .additionalFields(response)
            .rawData(response.toString())
            .metadata(Map.of("source", "SMA_REFINITIV", "quality", "HIGH"))
            .build();
    }
    
    /**
     * Extract BigDecimal value from response map
     */
    private BigDecimal extractBigDecimal(Map<String, Object> response, String key) {
        Object value = response.get(key);
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }
}
