package com.custom.indexbasket.marketdata.proxy.impl;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.AbstractDataSourceProxy;
import com.custom.indexbasket.marketdata.proxy.config.DataSourceConfig;
import com.custom.indexbasket.marketdata.proxy.model.RawMarketData;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Bloomberg data source proxy implementation.
 * This is a mock implementation for development purposes.
 */
@Service
public class BloombergProxy extends AbstractDataSourceProxy {
    
    private static final Logger log = LoggerFactory.getLogger(BloombergProxy.class);
    
    private final BloombergConfig bloombergConfig;
    
    public BloombergProxy(DataSourceConfig config, MeterRegistry meterRegistry) {
        // Set the data source name for Bloomberg
        config.setDataSourceName("BLOOMBERG");
        super(config, meterRegistry);
        this.bloombergConfig = new BloombergConfig(config);
    }
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        log.debug("Fetching Bloomberg data for instrument: {}", instrumentId);
        
        // Mock implementation - in production this would call Bloomberg API
        return Mono.just(createMockBloombergData(instrumentId))
            .delayElement(java.time.Duration.ofMillis(100)) // Simulate API latency
            .doOnSuccess(data -> log.debug("Bloomberg data fetched successfully for: {}", instrumentId))
            .doOnError(error -> log.error("Bloomberg API error for {}: {}", instrumentId, error.getMessage()));
    }
    
    @Override
    protected MarketDataResponse transformData(RawMarketData rawData) {
        // Transform Bloomberg raw data to MarketDataResponse
        return MarketDataResponse.builder()
            .instrumentId(rawData.getInstrumentId())
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
            .dataSource("BLOOMBERG")
            .dataQuality("HIGH")
            .build();
    }
    
    @Override
    public Mono<Boolean> isAvailable() {
        return checkAvailability();
    }
    
    @Override
    protected Mono<Boolean> checkAvailability() {
        // Mock health check - in production this would ping Bloomberg servers
        return Mono.just(true)
            .delayElement(java.time.Duration.ofMillis(50))
            .onErrorReturn(false);
    }
    
    @Override
    public Flux<String> getSupportedInstrumentTypes() {
        return Flux.fromIterable(List.of("STOCK", "BOND", "ETF", "INDEX", "CURRENCY", "COMMODITY"));
    }
    
    private RawMarketData createMockBloombergData(String instrumentId) {
        // Generate mock data for development/testing
        BigDecimal basePrice = new BigDecimal("100.00");
        BigDecimal randomVariation = new BigDecimal(Math.random() * 10 - 5);
        BigDecimal price = basePrice.add(randomVariation);
        
        return RawMarketData.builder()
            .instrumentId(instrumentId)
            .dataSource("BLOOMBERG")
            .timestamp(LocalDateTime.now())
            .price(price)
            .bidPrice(price.subtract(new BigDecimal("0.05")))
            .askPrice(price.add(new BigDecimal("0.05")))
            .openPrice(basePrice)
            .highPrice(price.add(new BigDecimal("2.00")))
            .lowPrice(price.subtract(new BigDecimal("2.00")))
            .volume(new BigDecimal("1000000"))
            .currency("USD")
            .exchange("NYSE")
            .additionalFields(Map.of("bloomberg_ticker", instrumentId + " US Equity"))
            .rawData("Mock Bloomberg data for " + instrumentId)
            .metadata(Map.of("source", "BLOOMBERG", "quality", "HIGH"))
            .build();
    }
    
    /**
     * Bloomberg-specific configuration wrapper.
     */
    private static class BloombergConfig {
        private final String appName;
        private final String serverHost;
        private final int serverPort;
        
        public BloombergConfig(DataSourceConfig config) {
            this.appName = config.getBloombergAppName() != null ? config.getBloombergAppName() : "MarketDataService";
            this.serverHost = config.getBloombergServerHost() != null ? config.getBloombergServerHost() : "localhost";
            this.serverPort = config.getBloombergServerPort() > 0 ? config.getBloombergServerPort() : 8194;
        }
        
        public String getAppName() { return appName; }
        public String getServerHost() { return serverHost; }
        public int getServerPort() { return serverPort; }
    }
}
