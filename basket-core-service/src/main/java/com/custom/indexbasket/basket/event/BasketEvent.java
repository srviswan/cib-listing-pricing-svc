package com.custom.indexbasket.basket.event;

import com.custom.indexbasket.common.model.BasketStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base event model for basket lifecycle events
 * All events should be published to downstream services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasketEvent {
    
    // Event metadata
    private String eventId;
    private String eventType;
    private String eventVersion;
    private LocalDateTime eventTimestamp;
    private String sourceService;
    private String correlationId;
    
    // Basket information
    private UUID basketId;
    private String basketCode;
    private String basketName;
    private BasketStatus previousStatus;
    private BasketStatus newStatus;
    private String eventTrigger;
    private String triggeredBy;
    
    // Additional context
    private String description;
    private String basketType;
    private String baseCurrency;
    private BigDecimal totalWeight;
    private Integer constituentCount;
    private BigDecimal totalMarketValue;
    
    // Performance metrics
    private Long processingTimeMs;
    private String errorMessage;
    
    // Event categories for routing
    public enum EventCategory {
        LIFECYCLE,      // Status changes, creation, deletion
        OPERATIONAL,    // CRUD operations, updates
        PERFORMANCE,    // Metrics, monitoring
        BUSINESS,       // Business logic events
        SYSTEM         // System-level events
    }
    
    private EventCategory eventCategory;
    
    // Event types for specific operations
    public enum EventType {
        // Lifecycle Events
        BASKET_CREATED,
        BASKET_UPDATED,
        BASKET_DELETED,
        STATUS_CHANGED,
        
        // State Machine Events
        BACKTEST_STARTED,
        BACKTEST_COMPLETED,
        BACKTEST_FAILED,
        APPROVAL_REQUESTED,
        APPROVAL_GRANTED,
        APPROVAL_REJECTED,
        LISTING_STARTED,
        LISTING_COMPLETED,
        LISTING_FAILED,
        ACTIVATION_STARTED,
        ACTIVATION_COMPLETED,
        SUSPENSION_STARTED,
        SUSPENSION_COMPLETED,
        DELISTING_STARTED,
        DELISTING_COMPLETED,
        
        // Operational Events
        CONSTITUENT_ADDED,
        CONSTITUENT_REMOVED,
        CONSTITUENT_UPDATED,
        REBALANCE_STARTED,
        REBALANCE_COMPLETED,
        
        // Performance Events
        PERFORMANCE_METRICS_UPDATED,
        RISK_METRICS_UPDATED,
        MARKET_DATA_UPDATED
    }
    
    /**
     * Create a standard event with common metadata
     */
    public static BasketEvent createEvent(EventType eventType, UUID basketId, String basketCode) {
        return BasketEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType.name())
                .eventVersion("1.0")
                .eventTimestamp(LocalDateTime.now())
                .sourceService("basket-core-service")
                .correlationId(UUID.randomUUID().toString())
                .basketId(basketId)
                .basketCode(basketCode)
                .eventCategory(determineEventCategory(eventType))
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withBasketName(String basketName) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(basketName)
                .description(this.description)
                .basketType(this.basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withDescription(String description) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(description)
                .basketType(this.basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withBasketType(String basketType) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(this.description)
                .basketType(basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withBaseCurrency(String baseCurrency) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(this.description)
                .basketType(this.basketType)
                .baseCurrency(baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withTotalWeight(BigDecimal totalWeight) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(this.description)
                .basketType(this.basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withTriggeredBy(String triggeredBy) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(this.description)
                .basketType(this.basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withPreviousStatus(BasketStatus previousStatus) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(this.description)
                .basketType(this.basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withNewStatus(BasketStatus newStatus) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(this.description)
                .basketType(this.basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(newStatus)
                .eventTrigger(this.eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Create a copy of this event with updated fields
     */
    public BasketEvent withEventTrigger(String eventTrigger) {
        return BasketEvent.builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .eventVersion(this.eventVersion)
                .eventTimestamp(this.eventTimestamp)
                .sourceService(this.sourceService)
                .correlationId(this.correlationId)
                .basketId(this.basketId)
                .basketCode(this.basketCode)
                .basketName(this.basketName)
                .description(this.description)
                .basketType(this.basketType)
                .baseCurrency(this.baseCurrency)
                .totalWeight(this.totalWeight)
                .previousStatus(this.previousStatus)
                .newStatus(this.newStatus)
                .eventTrigger(eventTrigger)
                .triggeredBy(this.triggeredBy)
                .constituentCount(this.constituentCount)
                .totalMarketValue(this.totalMarketValue)
                .processingTimeMs(this.processingTimeMs)
                .errorMessage(this.errorMessage)
                .eventCategory(this.eventCategory)
                .build();
    }
    
    /**
     * Determine event category based on event type
     */
    private static EventCategory determineEventCategory(EventType eventType) {
        if (eventType.name().contains("STATUS") || eventType.name().contains("BACKTEST") || 
            eventType.name().contains("APPROVAL") || eventType.name().contains("LISTING") ||
            eventType.name().contains("ACTIVATION") || eventType.name().contains("SUSPENSION") ||
            eventType.name().contains("DELISTING")) {
            return EventCategory.LIFECYCLE;
        } else if (eventType.name().contains("CREATED") || eventType.name().contains("UPDATED") ||
                   eventType.name().contains("DELETED") || eventType.name().contains("CONSTITUENT")) {
            return EventCategory.OPERATIONAL;
        } else if (eventType.name().contains("METRICS") || eventType.name().contains("PERFORMANCE")) {
            return EventCategory.PERFORMANCE;
        } else if (eventType.name().contains("REBALANCE") || eventType.name().contains("MARKET")) {
            return EventCategory.BUSINESS;
        } else {
            return EventCategory.SYSTEM;
        }
    }
}
