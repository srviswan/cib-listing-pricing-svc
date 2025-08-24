package com.custom.indexbasket.marketdata.proxy;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceHealth;
import com.custom.indexbasket.marketdata.proxy.model.DataSourceMetrics;
import com.custom.indexbasket.marketdata.proxy.model.RawMarketData;
import com.custom.indexbasket.marketdata.proxy.exception.RateLimitExceededException;
import com.custom.indexbasket.marketdata.proxy.config.DataSourceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for data source proxy implementations.
 * Provides common functionality including circuit breaker, rate limiting, and metrics.
 */
public abstract class AbstractDataSourceProxy implements DataSourceProxy {
    
    private static final Logger log = LoggerFactory.getLogger(AbstractDataSourceProxy.class);
    protected final DataSourceConfig config;
    protected final CircuitBreaker circuitBreaker;
    protected final RateLimiter rateLimiter;
    protected final MeterRegistry meterRegistry;
    
    protected final Timer requestTimer;
    protected final Counter successCounter;
    protected final Counter failureCounter;
    
    protected AbstractDataSourceProxy(DataSourceConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
        
        // Initialize circuit breaker
        this.circuitBreaker = CircuitBreaker.ofDefaults(config.getDataSourceName() + "-circuit-breaker");
        
        // Initialize rate limiter
        this.rateLimiter = RateLimiter.ofDefaults(config.getDataSourceName() + "-rate-limiter");
        
        // Initialize metrics
        this.requestTimer = Timer.builder("market_data.proxy.request.duration")
            .tag("data_source", config.getDataSourceName())
            .register(meterRegistry);
        
        this.successCounter = Counter.builder("market_data.proxy.request.success")
            .tag("data_source", config.getDataSourceName())
            .register(meterRegistry);
        
        this.failureCounter = Counter.builder("market_data.proxy.request.failure")
            .tag("data_source", config.getDataSourceName())
            .register(meterRegistry);
    }
    
    @Override
    public Mono<MarketDataResponse> getInstrumentData(String instrumentId) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            return Mono.fromCallable(() -> rateLimiter.acquirePermission())
                .then(fetchFromSource(instrumentId)
                    .map(this::transformData))
                .doOnSuccess(result -> {
                    sample.stop(requestTimer);
                    successCounter.increment();
                })
                .doOnError(error -> {
                    sample.stop(requestTimer);
                    failureCounter.increment();
                    log.error("Error fetching data for {} from {}: {}", 
                        instrumentId, config.getDataSourceName(), error.getMessage());
                })
                .onErrorResume(error -> {
                    String errorMessage = error.getMessage();
                    if (errorMessage != null && errorMessage.contains("CircuitBreaker")) {
                        return handleCircuitBreakerOpen(instrumentId);
                    } else if (errorMessage != null && errorMessage.contains("RateLimiter")) {
                        return handleRateLimitExceeded(instrumentId);
                    }
                    return Mono.error(error);
                });
        });
    }
    
    @Override
    public Flux<MarketDataResponse> getBatchData(List<String> instrumentIds) {
        return Flux.fromIterable(instrumentIds)
            .flatMap(this::getInstrumentData, config.getConcurrencyLimit())
            .doOnComplete(() -> log.info("Batch data retrieval completed for {} instruments from {}", 
                instrumentIds.size(), config.getDataSourceName()));
    }
    
    @Override
    public Mono<DataSourceHealth> getHealthStatus() {
        return checkAvailability()
            .map(available -> {
                DataSourceHealth.HealthStatus status = Boolean.TRUE.equals(available) ? 
                    DataSourceHealth.HealthStatus.HEALTHY : DataSourceHealth.HealthStatus.UNHEALTHY;
                return new DataSourceHealth(
                    config.getDataSourceName(),
                    status,
                    LocalDateTime.now(),
                    null,
                    Map.of("lastCheck", LocalDateTime.now())
                );
            })
            .onErrorReturn(new DataSourceHealth(
                config.getDataSourceName(),
                DataSourceHealth.HealthStatus.UNKNOWN,
                LocalDateTime.now(),
                "Health check failed",
                Map.of("error", "Health check failed")
            ));
    }
    
    @Override
    public Mono<DataSourceMetrics> getPerformanceMetrics() {
        return Mono.just(new DataSourceMetrics(
            config.getDataSourceName(),
            (long) (successCounter.count() + failureCounter.count()),
            (long) successCounter.count(),
            (long) failureCounter.count(),
            Duration.ofMillis((long) (requestTimer.mean(TimeUnit.MILLISECONDS))),
            Duration.ofMillis((long) (requestTimer.max(TimeUnit.MILLISECONDS))),
            calculateSuccessRate(),
            Map.of("circuitBreakerState", circuitBreaker.getState())
        ));
    }
    
    // Abstract methods to be implemented by concrete classes
    protected abstract Mono<RawMarketData> fetchFromSource(String instrumentId);
    protected abstract MarketDataResponse transformData(RawMarketData rawData);
    protected abstract Mono<Boolean> checkAvailability();
    
    // Helper methods
    private double calculateSuccessRate() {
        long total = (long) (successCounter.count() + failureCounter.count());
        return total > 0 ? (double) successCounter.count() / total : 0.0;
    }
    
    private Mono<MarketDataResponse> handleCircuitBreakerOpen(String instrumentId) {
        log.warn("Circuit breaker open for {}, using fallback data", config.getDataSourceName());
        return getFallbackData(instrumentId);
    }
    
    private Mono<MarketDataResponse> handleRateLimitExceeded(String instrumentId) {
        log.warn("Rate limit exceeded for {}, queuing request", config.getDataSourceName());
        return Mono.error(new RateLimitExceededException("Rate limit exceeded for " + config.getDataSourceName()));
    }
    
    protected Mono<MarketDataResponse> getFallbackData(String instrumentId) {
        // Default fallback implementation
        return Mono.empty();
    }
}
