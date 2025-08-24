package com.custom.indexbasket.marketdata.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Market Data Event - Represents market data related events
 * 
 * This event class integrates with the common event system and follows
 * the same pattern as BasketEvent for consistency across services.
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketDataEvent {
    
    // Event identification
    private String eventId;
    private String eventType;
    private String eventCategory;
    
    // Market data context
    private String instrumentId;
    private String dataSource;
    private String exchange;
    private String currency;
    
    // Market data values
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    
    // Event metadata
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    private String correlationId;
    private String userId;
    private String sessionId;
    
    // Additional context
    private Map<String, Object> additionalData;
    private Map<String, String> headers;
    
    // Event categories for routing
    public enum EventCategory {
        MARKET_DATA_UPDATE,    // Real-time price/volume updates
        PRICE_ALERT,          // Price threshold breaches
        VOLATILITY_SPIKE,     // Unusual price movements
        DATA_QUALITY,         // Data quality issues
        PERFORMANCE_METRIC,   // Calculated metrics
        SYSTEM_HEALTH,        // Service health events
        USER_ACTION           // User-initiated actions
    }
    
    // Event types for specific handling
    public enum EventType {
        // Market Data Updates
        MARKET_DATA_UPDATED("Real-time market data update"),
        PRICE_CHANGE("Significant price change detected"),
        VOLUME_SPIKE("Unusual volume activity"),
        SPREAD_CHANGE("Bid-ask spread change"),
        
        // Alerts and Notifications
        PRICE_ALERT("Price threshold breached"),
        VOLATILITY_ALERT("High volatility detected"),
        LIQUIDITY_ALERT("Low liquidity warning"),
        
        // Data Quality
        DATA_QUALITY_DEGRADED("Data quality below threshold"),
        DATA_SOURCE_OFFLINE("Data source unavailable"),
        DATA_LATENCY_HIGH("Data latency above acceptable level"),
        
        // Performance Metrics
        BASKET_PERFORMANCE_UPDATED("Basket performance metrics updated"),
        RISK_METRICS_UPDATED("Risk metrics recalculated"),
        ANALYTICS_COMPLETED("Analytics calculation completed"),
        
        // System Events
        SERVICE_HEALTH_CHECK("Service health status"),
        CACHE_PERFORMANCE("Cache performance metrics"),
        EXTERNAL_API_STATUS("External API connectivity status");
        
        private final String description;
        
        EventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Create a new market data event with default values
     */
    public static MarketDataEvent create(String eventType, String instrumentId, String dataSource) {
        return MarketDataEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .instrumentId(instrumentId)
            .dataSource(dataSource)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a price update event
     */
    public static MarketDataEvent priceUpdate(String instrumentId, String dataSource, 
                                           BigDecimal lastPrice, BigDecimal bidPrice, BigDecimal askPrice) {
        return MarketDataEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.MARKET_DATA_UPDATED.name())
            .eventCategory(EventCategory.MARKET_DATA_UPDATE.name())
            .instrumentId(instrumentId)
            .dataSource(dataSource)
            .lastPrice(lastPrice)
            .bidPrice(bidPrice)
            .askPrice(askPrice)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a price alert event
     */
    public static MarketDataEvent priceAlert(String instrumentId, String dataSource, 
                                          BigDecimal currentPrice, BigDecimal threshold, String alertType) {
        return MarketDataEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.PRICE_ALERT.name())
            .eventCategory(EventCategory.PRICE_ALERT.name())
            .instrumentId(instrumentId)
            .dataSource(dataSource)
            .lastPrice(currentPrice)
            .additionalData(Map.of(
                "threshold", threshold,
                "alertType", alertType,
                "breachAmount", currentPrice.subtract(threshold).abs()
            ))
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a volatility alert event
     */
    public static MarketDataEvent volatilityAlert(String instrumentId, String dataSource, 
                                               BigDecimal volatility, BigDecimal threshold) {
        return MarketDataEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.VOLATILITY_ALERT.name())
            .eventCategory(EventCategory.VOLATILITY_SPIKE.name())
            .instrumentId(instrumentId)
            .dataSource(dataSource)
            .additionalData(Map.of(
                "volatility", volatility,
                "threshold", threshold,
                "severity", volatility.compareTo(threshold) > 0 ? "HIGH" : "MEDIUM"
            ))
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a data quality event
     */
    public static MarketDataEvent dataQualityEvent(String instrumentId, String dataSource, 
                                                 String qualityLevel, String issue, Map<String, Object> metrics) {
        return MarketDataEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.DATA_QUALITY_DEGRADED.name())
            .eventCategory(EventCategory.DATA_QUALITY.name())
            .instrumentId(instrumentId)
            .dataSource(dataSource)
            .additionalData(Map.of(
                "qualityLevel", qualityLevel,
                "issue", issue,
                "metrics", metrics
            ))
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a performance metrics event
     */
    public static MarketDataEvent performanceMetricsEvent(String instrumentId, String dataSource, 
                                                        Map<String, Object> metrics) {
        return MarketDataEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventType.BASKET_PERFORMANCE_UPDATED.name())
            .eventCategory(EventCategory.PERFORMANCE_METRIC.name())
            .instrumentId(instrumentId)
            .dataSource(dataSource)
            .additionalData(metrics)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Add correlation ID for tracing
     */
    public MarketDataEvent withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
    
    /**
     * Add user context
     */
    public MarketDataEvent withUserContext(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
        return this;
    }
    
    /**
     * Add custom headers
     */
    public MarketDataEvent withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }
    
    /**
     * Add additional data
     */
    public MarketDataEvent withAdditionalData(String key, Object value) {
        if (this.additionalData == null) {
            this.additionalData = Map.of();
        }
        this.additionalData.put(key, value);
        return this;
    }
}
