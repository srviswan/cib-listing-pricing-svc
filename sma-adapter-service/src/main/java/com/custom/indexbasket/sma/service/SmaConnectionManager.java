package com.custom.indexbasket.sma.service;

import com.custom.indexbasket.sma.config.SmaConfiguration;
import com.custom.indexbasket.sma.model.SmaHealthStatus;
import com.custom.indexbasket.sma.model.SmaPriceData;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SMA Connection Manager
 * 
 * Manages connections to Refinitiv SMA API and handles data retrieval.
 */
@Service
@Slf4j
public class SmaConnectionManager {
    
    private final SmaConfiguration config;
    private final MeterRegistry meterRegistry;
    private final Scheduler scheduler;
    
    // Health tracking
    private final AtomicInteger connectionAttempts = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private volatile LocalDateTime lastConnected;
    private volatile LocalDateTime lastError;
    private volatile String lastErrorMessage;
    private volatile boolean connected = false;
    
    // Mock data for development - replace with real SMA API calls
    private final ConcurrentHashMap<String, SmaPriceData> mockData = new ConcurrentHashMap<>();
    
    @Autowired
    public SmaConnectionManager(SmaConfiguration config, MeterRegistry meterRegistry, Scheduler scheduler) {
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.scheduler = scheduler;
        initializeMockData();
    }
    
    /**
     * Initialize mock data for development
     */
    private void initializeMockData() {
        String[] symbols = config.getSubscription().getSymbols().split(",");
        for (String symbol : symbols) {
            mockData.put(symbol.trim(), createMockPriceData(symbol.trim()));
        }
    }
    
    /**
     * Fetch price data for a single symbol
     */
    public Mono<SmaPriceData> fetchPrice(String symbol) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            connectionAttempts.incrementAndGet();
            
            try {
                // Simulate API call delay
                Thread.sleep(50 + (long)(Math.random() * 100));
                
                SmaPriceData priceData = mockData.getOrDefault(symbol, createMockPriceData(symbol));
                priceData.setTimestamp(LocalDateTime.now());
                
                successfulRequests.incrementAndGet();
                connected = true;
                lastConnected = LocalDateTime.now();
                
                long responseTime = System.currentTimeMillis() - startTime;
                totalResponseTime.addAndGet(responseTime);
                
                // Update metrics
                meterRegistry.counter("sma.requests.success").increment();
                meterRegistry.timer("sma.response.time").record(Duration.ofMillis(responseTime));
                
                return priceData;
                
            } catch (Exception e) {
                failedRequests.incrementAndGet();
                connected = false;
                lastError = LocalDateTime.now();
                lastErrorMessage = e.getMessage();
                
                meterRegistry.counter("sma.requests.failed").increment();
                log.error("Failed to fetch price for symbol {}: {}", symbol, e.getMessage());
                
                throw new RuntimeException("SMA API error for " + symbol, e);
            }
        })
        .subscribeOn(scheduler)
        .timeout(Duration.ofMillis(config.getConnection().getTimeout()));
    }
    
    /**
     * Fetch price data for multiple symbols in batch
     */
    public Flux<SmaPriceData> fetchBatchPrices(List<String> symbols) {
        return Flux.fromIterable(symbols)
            .flatMap(this::fetchPrice, config.getSubscription().getBatchSize())
            .onErrorContinue((error, symbol) -> 
                log.warn("Failed to fetch price for symbol: {}", symbol, error));
    }
    
    /**
     * Check SMA API health
     */
    public Mono<SmaHealthStatus> checkHealth() {
        return Mono.fromCallable(() -> {
            double avgResponseTime = successfulRequests.get() > 0 ? 
                (double) totalResponseTime.get() / successfulRequests.get() : 0.0;
            
            return SmaHealthStatus.builder()
                .status(connected ? "UP" : "DOWN")
                .connected(connected)
                .lastConnected(lastConnected)
                .lastError(lastError)
                .lastErrorMessage(lastErrorMessage)
                .connectionAttempts(connectionAttempts.get())
                .successfulRequests(successfulRequests.get())
                .failedRequests(failedRequests.get())
                .averageResponseTime(avgResponseTime)
                .build();
        })
        .subscribeOn(scheduler);
    }
    
    /**
     * Create mock price data for development
     */
    private SmaPriceData createMockPriceData(String symbol) {
        double basePrice = 100.0 + (symbol.hashCode() % 1000);
        double randomVariation = (Math.random() - 0.5) * 10;
        double price = basePrice + randomVariation;
        
        return SmaPriceData.builder()
            .symbol(symbol)
            .lastPrice(java.math.BigDecimal.valueOf(price))
            .bidPrice(java.math.BigDecimal.valueOf(price - 0.05))
            .askPrice(java.math.BigDecimal.valueOf(price + 0.05))
            .openPrice(java.math.BigDecimal.valueOf(basePrice))
            .highPrice(java.math.BigDecimal.valueOf(price + 2.0))
            .lowPrice(java.math.BigDecimal.valueOf(price - 2.0))
            .volume(1000000L + (long)(Math.random() * 5000000))
            .currency("USD")
            .exchange("NYSE")
            .timestamp(LocalDateTime.now())
            .dataQuality("HIGH")
            .build();
    }
    
    /**
     * Start real-time subscription (mock implementation)
     */
    public Flux<SmaPriceData> startRealTimeSubscription(List<String> symbols) {
        return Flux.interval(Duration.ofMillis(config.getSubscription().getUpdateFrequency()))
            .flatMap(tick -> Flux.fromIterable(symbols)
                .flatMap(symbol -> fetchPrice(symbol)
                    .map(priceData -> {
                        // Add some random variation to simulate real-time updates
                        double variation = (Math.random() - 0.5) * 0.02; // ±1%
                        double newPrice = priceData.getLastPrice().doubleValue() * (1 + variation);
                        priceData.setLastPrice(java.math.BigDecimal.valueOf(newPrice));
                        priceData.setBidPrice(java.math.BigDecimal.valueOf(newPrice - 0.05));
                        priceData.setAskPrice(java.math.BigDecimal.valueOf(newPrice + 0.05));
                        priceData.setTimestamp(LocalDateTime.now());
                        return priceData;
                    })
                )
            )
            .onErrorContinue((error, tick) -> 
                log.warn("Real-time subscription error at tick {}: {}", tick, error.getMessage()));
    }
}
