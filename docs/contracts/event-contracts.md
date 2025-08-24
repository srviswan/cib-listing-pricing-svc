# Event Contracts - Messaging Specifications

## Overview

This document defines the **complete event contracts** for asynchronous messaging between services in the Custom Index Basket Management Platform. These contracts specify event schemas, routing patterns, and processing semantics that work consistently across both Solace PubSub+ and Kafka implementations.

## Event Design Principles

```yaml
Event Schema Standards:
  ✅ CloudEvents specification compliance
  ✅ JSON serialization with schema versioning
  ✅ Immutable event data structures
  ✅ Rich metadata for traceability
  ✅ Implementation-agnostic routing

Messaging Patterns:
  ✅ Event sourcing for audit trail
  ✅ CQRS separation for performance
  ✅ Saga pattern for distributed transactions
  ✅ Dead letter queues for error handling
  ✅ Idempotent event processing

Routing Strategy:
  ✅ Topic-based routing (hierarchical)
  ✅ Content-based filtering
  ✅ Geographic routing for compliance
  ✅ Priority-based delivery
  ✅ Replay capability for recovery
```

## Base Event Structures

### CloudEvents Base Schema

```java
/**
 * Base event following CloudEvents v1.0 specification
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseEvent {
    
    @NotBlank(message = "Event ID is required")
    private String id;
    
    @NotBlank(message = "Source is required") 
    private String source;
    
    @NotBlank(message = "Spec version is required")
    private String specversion = "1.0";
    
    @NotBlank(message = "Event type is required")
    private String type;
    
    @NotNull(message = "Event time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime time;
    
    private String datacontenttype = "application/json";
    
    private String dataschema;
    
    private String subject;
    
    // Extension attributes
    private String correlationid;
    private String causationid;
    private String userid;
    private String tenantid;
    private String version = "1.0";
    private String priority = "NORMAL";
    
    // Tracing
    private String traceid;
    private String spanid;
    
    // Business context
    private Map<String, Object> extensions;
    
    // getters, setters, builders
}

/**
 * Event metadata for routing and processing
 */
public class EventMetadata {
    private String routingKey;
    private String partitionKey;
    private Integer ttlSeconds;
    private Boolean persistent;
    private String replyTo;
    private Map<String, String> headers;
    
    // getters, setters
}
```

## Topic/Queue Hierarchy

### Solace Topic Structure
```
basket/v1/{region}/{environment}/{event-type}/{entity-id}
market/v1/{region}/{environment}/{data-type}/{symbol}
publishing/v1/{region}/{environment}/{vendor}/{operation}
analytics/v1/{region}/{environment}/{analysis-type}/{basket-code}
```

### Kafka Topic Structure
```
basket.v1.{event-type}
market.v1.{data-type}
publishing.v1.{operation}
analytics.v1.{analysis-type}
```

## Basket Service Events

### 1. Basket Lifecycle Events

#### Basket Created Event
```java
public class BasketCreatedEvent extends BaseEvent {
    
    // Event type identifier
    public static final String EVENT_TYPE = "com.custom.basket.created.v1";
    
    private BasketCreatedData data;
    
    public BasketCreatedEvent() {
        this.type = EVENT_TYPE;
        this.source = "basket-service";
    }
    
    // getters, setters
}

public class BasketCreatedData {
    
    @NotBlank(message = "Basket code is required")
    private String basketCode;
    
    @NotBlank(message = "Basket name is required")
    private String basketName;
    
    @NotBlank(message = "Basket type is required") 
    private String basketType;
    
    @NotBlank(message = "Base currency is required")
    private String baseCurrency;
    
    @NotBlank(message = "Created by is required")
    private String createdBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @NotBlank(message = "Version is required")
    private String version;
    
    @Valid
    @NotEmpty(message = "Constituents required")
    private List<ConstituentData> constituents;
    
    // Business context
    private String reason;
    private String previousVersion;
    private Map<String, Object> metadata;
    
    // getters, setters
}

public class ConstituentData {
    private String symbol;
    private BigDecimal weight;
    private Long shares;
    private String sector;
    private String country;
    private String currency;
    
    // getters, setters
}

// Routing Configuration
// Solace: basket/v1/*/*/created/{basketCode}
// Kafka: basket.v1.created (partition by basketCode)
```

#### Basket Updated Event
```java
public class BasketUpdatedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.basket.updated.v1";
    
    private BasketUpdatedData data;
    
    public BasketUpdatedEvent() {
        this.type = EVENT_TYPE;
        this.source = "basket-service";
    }
}

public class BasketUpdatedData {
    private String basketCode;
    private String basketName;
    private String basketType;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String version;
    private String previousVersion;
    
    // Change tracking
    private List<FieldChange> changes;
    private List<ConstituentChange> constituentChanges;
    
    // getters, setters
}

public class FieldChange {
    private String fieldName;
    private Object oldValue;
    private Object newValue;
    private String changeType; // ADDED, MODIFIED, REMOVED
    
    // getters, setters
}

public class ConstituentChange {
    private String symbol;
    private String changeType; // ADDED, MODIFIED, REMOVED, WEIGHT_CHANGED
    private ConstituentData oldData;
    private ConstituentData newData;
    
    // getters, setters
}
```

#### Basket Status Changed Event
```java
public class BasketStatusChangedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.basket.status.changed.v1";
    
    private BasketStatusChangedData data;
}

public class BasketStatusChangedData {
    private String basketCode;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private LocalDateTime changedAt;
    private String reason;
    private String transitionEvent;
    private Map<String, Object> stateData;
    
    // Approval context (if applicable)
    private String approvalId;
    private String approver;
    private String approvalComments;
    
    // getters, setters
}

// Routing: High priority event for immediate processing
// Priority: HIGH
// Persistent: true
```

### 2. Approval Workflow Events

#### Approval Requested Event
```java
public class ApprovalRequestedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.approval.requested.v1";
    
    private ApprovalRequestedData data;
}

public class ApprovalRequestedData {
    private String approvalId;
    private String basketCode;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String approvalType; // SINGLE, DUAL
    private String priority; // NORMAL, URGENT, EXPEDITED
    private String preferredApprover;
    private String comments;
    private Integer estimatedApprovalTimeMinutes;
    
    // SLA tracking
    private LocalDateTime slaDeadline;
    private String slaPriority;
    
    // getters, setters
}

// Routing: Route to approval service and notification service
// Solace: approval/v1/*/*/requested/{approvalId}
// Kafka: approval.v1.requested
```

#### Approval Completed Event
```java
public class ApprovalCompletedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.approval.completed.v1";
    
    private ApprovalCompletedData data;
}

public class ApprovalCompletedData {
    private String approvalId;
    private String basketCode;
    private String decision; // APPROVED, REJECTED, REQUEST_CHANGES
    private String approver;
    private LocalDateTime approvedAt;
    private String comments;
    private List<String> conditions;
    
    // SLA tracking
    private Integer approvalTimeMinutes;
    private String slaStatus; // MET, MISSED, WARNING
    
    // Next steps
    private List<String> nextActions;
    private LocalDateTime effectiveDate;
    
    // getters, setters
}

// High priority - triggers immediate downstream actions
// Priority: HIGH
// Persistent: true
```

## Market Data Service Events

### 1. Market Data Events

#### Market Data Updated Event
```java
public class MarketDataUpdatedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.market.data.updated.v1";
    
    private MarketDataUpdatedData data;
}

public class MarketDataUpdatedData {
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotNull(message = "Price is required")
    private BigDecimal price;
    
    private BigDecimal bid;
    private BigDecimal ask;
    private Long volume;
    private String currency;
    private String exchange;
    
    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    private String source; // BLOOMBERG, REFINITIV, etc.
    private String dataType; // REAL_TIME, DELAYED, CALCULATED
    
    // Price change information
    private BigDecimal previousPrice;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    
    // Quality indicators
    private String qualityFlag; // GOOD, STALE, ESTIMATED
    private Integer ageSeconds;
    
    // getters, setters
}

// High frequency event - optimized for performance
// Routing: Route by symbol for load balancing
// Solace: market/v1/*/*/data/{symbol}
// Kafka: market.v1.data (partition by symbol hash)
// Persistent: false (for real-time data)
// TTL: 60 seconds
```

#### Basket Valuation Calculated Event  
```java
public class BasketValuationCalculatedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.basket.valuation.calculated.v1";
    
    private BasketValuationCalculatedData data;
}

public class BasketValuationCalculatedData {
    private String basketCode;
    private BigDecimal totalValue;
    private String baseCurrency;
    private Integer constituentCount;
    private LocalDateTime calculatedAt;
    private String calculationMethod; // REAL_TIME, BATCH, ESTIMATED
    
    // Detailed breakdown
    private List<ConstituentValuationData> constituents;
    
    // Performance metrics
    private BigDecimal dailyReturn;
    private BigDecimal intraday ChangePercent;
    private BigDecimal volatility;
    
    // Data quality
    private String qualityScore; // A, B, C (A=all real-time, C=some stale data)
    private Integer staleDataCount;
    private LocalDateTime oldestDataPoint;
    
    // getters, setters
}

public class ConstituentValuationData {
    private String symbol;
    private BigDecimal price;
    private BigDecimal weight;
    private Long shares;
    private BigDecimal value;
    private BigDecimal contribution;
    private String currency;
    private LocalDateTime lastPriceUpdate;
    private String priceSource;
    
    // getters, setters
}

// Critical for real-time pricing - high priority
// Priority: HIGH
// Persistent: true
// Routing: Route to publishing service and analytics service
```

## Publishing Service Events

### 1. Publishing Events

#### Basket Listed Event
```java
public class BasketListedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.publishing.basket.listed.v1";
    
    private BasketListedData data;
}

public class BasketListedData {
    private String basketCode;
    private String vendor; // BLOOMBERG, REFINITIV
    private String vendorReference;
    private LocalDateTime listedAt;
    private String listingStatus; // ACTIVE, PENDING, FAILED
    
    // Listing details
    private String instrumentId;
    private String isin;
    private String sedol;
    private String cusip;
    private String ticker;
    
    // Publishing configuration
    private String pricingFrequency; // REAL_TIME, 5_SECOND, MINUTE
    private Boolean autoPublishing;
    private String region;
    
    // SLA tracking
    private Integer listingTimeSeconds;
    private String slaStatus;
    
    // getters, setters
}

// Triggers real-time pricing workflow
// Priority: HIGH
// Persistent: true
// Routing: Notify market data service to start pricing
```

#### Price Published Event
```java
public class PricePublishedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.publishing.price.published.v1";
    
    private PricePublishedData data;
}

public class PricePublishedData {
    private String basketCode;
    private BigDecimal price;
    private String currency;
    private LocalDateTime publishedAt;
    
    // Vendor-specific details
    private Map<String, VendorPublishResult> vendorResults;
    
    // Performance tracking
    private Integer calculationToPublishMs;
    private String slaStatus;
    
    // Data lineage
    private String priceCalculationId;
    private String marketDataAge;
    private Integer constituentCount;
    
    // getters, setters
}

public class VendorPublishResult {
    private String vendor;
    private String status; // SUCCESS, FAILED, PARTIAL
    private String vendorReference;
    private LocalDateTime publishedAt;
    private Integer responseTimeMs;
    private String errorMessage;
    
    // getters, setters
}

// High frequency event for monitoring
// Persistent: false (monitoring data)
// TTL: 300 seconds
```

### 2. Vendor Integration Events

#### Vendor Status Changed Event
```java
public class VendorStatusChangedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.publishing.vendor.status.changed.v1";
    
    private VendorStatusChangedData data;
}

public class VendorStatusChangedData {
    private String vendor;
    private String oldStatus;
    private String newStatus; // ONLINE, OFFLINE, DEGRADED, MAINTENANCE
    private LocalDateTime changedAt;
    private String reason;
    
    // Impact assessment
    private List<String> affectedBaskets;
    private String impactLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private Integer estimatedDowntimeMinutes;
    
    // Automated responses
    private Boolean failoverTriggered;
    private String failoverVendor;
    private List<String> automatedActions;
    
    // getters, setters
}

// Critical operational event
// Priority: CRITICAL
// Persistent: true
// Routing: Notify all services, operations team
```

## Analytics Service Events

### 1. Backtesting Events

#### Backtest Started Event
```java
public class BacktestStartedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.analytics.backtest.started.v1";
    
    private BacktestStartedData data;
}

public class BacktestStartedData {
    private String backtestId;
    private String basketCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal initialValue;
    private String currency;
    private List<String> benchmarkIndices;
    private String rebalancingFrequency;
    private String requestedBy;
    private LocalDateTime startedAt;
    private Integer estimatedDurationMinutes;
    
    // getters, setters
}

// Informational event for tracking
// Priority: NORMAL
// Persistent: true
```

#### Backtest Completed Event
```java
public class BacktestCompletedEvent extends BaseEvent {
    
    public static final String EVENT_TYPE = "com.custom.analytics.backtest.completed.v1";
    
    private BacktestCompletedData data;
}

public class BacktestCompletedData {
    private String backtestId;
    private String basketCode;
    private String status; // COMPLETED, FAILED, CANCELLED
    private LocalDateTime completedAt;
    private Integer durationMinutes;
    
    // Results summary
    private BigDecimal totalReturn;
    private BigDecimal annualizedReturn;
    private BigDecimal volatility;
    private BigDecimal sharpeRatio;
    private BigDecimal maxDrawdown;
    
    // Benchmark comparisons
    private Map<String, BigDecimal> benchmarkOutperformance;
    
    // Error information (if failed)
    private String errorMessage;
    private String errorCode;
    
    // File references
    private String resultsFileUrl;
    private String reportFileUrl;
    
    // getters, setters
}

// Triggers notification to user and basket workflow
// Priority: HIGH (for workflow progression)
// Persistent: true
```

## Event Processing Patterns

### 1. Event Handlers

```java
/**
 * Abstract base for all event handlers
 */
public abstract class BaseEventHandler<T extends BaseEvent> {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @Autowired
    protected EventPublisher eventPublisher;
    
    @Autowired
    protected CacheService cacheService;
    
    /**
     * Process event with error handling and retries
     */
    public final Mono<Void> handle(T event) {
        return validateEvent(event)
            .then(processEvent(event))
            .doOnSuccess(v -> logSuccess(event))
            .doOnError(error -> logError(event, error))
            .onErrorResume(this::handleError);
    }
    
    protected abstract Mono<Void> processEvent(T event);
    
    protected Mono<Void> validateEvent(T event) {
        // Implement idempotency check
        String eventId = event.getId();
        return cacheService.exists("processed_event:" + eventId)
            .flatMap(exists -> {
                if (exists) {
                    log.warn("Event {} already processed, skipping", eventId);
                    return Mono.empty();
                }
                return cacheService.put("processed_event:" + eventId, true, Duration.ofHours(24))
                    .then();
            });
    }
    
    protected Mono<Void> handleError(Throwable error) {
        // Implement retry logic, dead letter queue, etc.
        return Mono.error(error);
    }
}

/**
 * Example event handler implementation
 */
@Component
public class BasketCreatedEventHandler extends BaseEventHandler<BasketCreatedEvent> {
    
    @Override
    protected Mono<Void> processEvent(BasketCreatedEvent event) {
        BasketCreatedData data = event.getData();
        
        return Mono.fromRunnable(() -> {
            log.info("Processing basket created: {}", data.getBasketCode());
            
            // Cache basket for quick access
            cacheService.put("basket:" + data.getBasketCode(), data, Duration.ofHours(1))
                .subscribe();
            
            // Trigger market data enrichment
            MarketDataEnrichmentRequestedEvent enrichmentEvent = 
                MarketDataEnrichmentRequestedEvent.builder()
                    .basketCode(data.getBasketCode())
                    .symbols(data.getConstituents().stream()
                        .map(ConstituentData::getSymbol)
                        .collect(Collectors.toList()))
                    .build();
            
            eventPublisher.publish("market.data.enrichment.requested", 
                data.getBasketCode(), enrichmentEvent)
                .subscribe();
        });
    }
}
```

### 2. Saga Pattern Implementation

```java
/**
 * Basket approval saga coordinator
 */
@Component
public class BasketApprovalSaga {
    
    @SagaStart
    @EventHandler
    public void handle(ApprovalRequestedEvent event, SagaLifecycle sagaLifecycle) {
        ApprovalRequestedData data = event.getData();
        
        // Set saga timeout
        sagaLifecycle.setTimeout(Duration.ofMinutes(30));
        
        // Start approval process
        StartApprovalProcessCommand command = StartApprovalProcessCommand.builder()
            .approvalId(data.getApprovalId())
            .basketCode(data.getBasketCode())
            .approvalType(data.getApprovalType())
            .build();
            
        commandGateway.send(command);
    }
    
    @EventHandler
    public void handle(ApprovalCompletedEvent event) {
        ApprovalCompletedData data = event.getData();
        
        if ("APPROVED".equals(data.getDecision())) {
            // Trigger listing process
            TriggerListingCommand command = TriggerListingCommand.builder()
                .basketCode(data.getBasketCode())
                .approvedBy(data.getApprover())
                .build();
                
            commandGateway.send(command);
        } else {
            // Handle rejection
            NotifyApprovalRejectedCommand command = NotifyApprovalRejectedCommand.builder()
                .basketCode(data.getBasketCode())
                .reason(data.getComments())
                .build();
                
            commandGateway.send(command);
        }
    }
    
    @EventHandler
    public void handle(BasketListedEvent event) {
        // Saga completion - basket is now live
        SagaLifecycle.end();
        
        // Publish saga completion event
        BasketApprovalSagaCompletedEvent completionEvent = 
            BasketApprovalSagaCompletedEvent.builder()
                .basketCode(event.getData().getBasketCode())
                .completedAt(LocalDateTime.now())
                .totalDurationMinutes(calculateSagaDuration())
                .build();
                
        eventPublisher.publish("saga.approval.completed", 
            event.getData().getBasketCode(), completionEvent)
            .subscribe();
    }
}
```

### 3. Event Sourcing Repository

```java
/**
 * Event sourcing repository for basket aggregate
 */
@Repository
public class BasketEventStore {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private TimescaleDBRepository repository;
    
    /**
     * Store event and publish to event stream
     */
    public Mono<Void> append(String aggregateId, List<BaseEvent> events) {
        return Flux.fromIterable(events)
            .flatMap(event -> {
                // Store in event store
                return repository.storeEvent(aggregateId, event)
                    // Publish to event stream
                    .then(eventPublisher.publish(
                        getTopicForEvent(event), 
                        aggregateId, 
                        event));
            })
            .then();
    }
    
    /**
     * Replay events to rebuild aggregate state
     */
    public Flux<BaseEvent> getEvents(String aggregateId, Long fromVersion) {
        return repository.getEvents(aggregateId, fromVersion);
    }
    
    /**
     * Get aggregate snapshot with latest events
     */
    public Mono<BasketAggregate> getAggregate(String basketCode) {
        return repository.getSnapshot(basketCode)
            .switchIfEmpty(Mono.just(new BasketAggregate(basketCode)))
            .flatMap(aggregate -> {
                return getEvents(basketCode, aggregate.getVersion())
                    .reduce(aggregate, BasketAggregate::apply);
            });
    }
    
    private String getTopicForEvent(BaseEvent event) {
        // Map event type to topic/queue name
        return EventTopicMapper.getTopicForEventType(event.getType());
    }
}
```

## Event Configuration

### Solace Configuration
```yaml
# Event routing configuration for Solace
solace:
  events:
    default-ttl: 3600  # 1 hour
    default-priority: NORMAL
    
    routing:
      basket-events:
        topic-prefix: "basket/v1"
        delivery-mode: PERSISTENT
        acknowledgment-timeout: 30000
        
      market-data-events:
        topic-prefix: "market/v1"
        delivery-mode: NON_PERSISTENT  # High frequency
        acknowledgment-timeout: 5000
        
      publishing-events:
        topic-prefix: "publishing/v1"
        delivery-mode: PERSISTENT
        acknowledgment-timeout: 60000
        
    dead-letter:
      enabled: true
      max-retries: 3
      retry-delay: 30000
      dlq-topic-suffix: "/dlq"
```

### Kafka Configuration
```yaml
# Event configuration for Kafka
kafka:
  events:
    default-partitions: 12
    default-replication-factor: 3
    
    topics:
      basket-events:
        name: "basket.v1.events"
        partitions: 12
        config:
          retention.ms: 604800000  # 7 days
          cleanup.policy: "compact,delete"
          
      market-data-events:
        name: "market.v1.data"
        partitions: 24  # High throughput
        config:
          retention.ms: 86400000   # 1 day
          cleanup.policy: "delete"
          
      publishing-events:
        name: "publishing.v1.events"
        partitions: 6
        config:
          retention.ms: 2592000000  # 30 days
          
    producers:
      basket-service:
        acks: "all"
        retries: 3
        batch-size: 16384
        
      market-data-service:
        acks: "1"  # Lower durability for speed
        retries: 1
        batch-size: 65536
        
    consumers:
      approval-service:
        group-id: "approval-service"
        auto-offset-reset: "latest"
        enable-auto-commit: false
```

## Event Testing

### Contract Testing
```java
/**
 * Event contract tests ensure compatibility across implementations
 */
@ExtendWith(MockitoExtension.class)
public class EventContractTest {
    
    @Test
    public void basketCreatedEvent_shouldSerializeDeserializeCorrectly() {
        // Given
        BasketCreatedEvent event = BasketCreatedEvent.builder()
            .id(UUID.randomUUID().toString())
            .basketCode("TEST_BASKET")
            .basketName("Test Basket")
            .basketType("THEMATIC")
            .baseCurrency("USD")
            .createdBy("test-user")
            .build();
        
        // When
        String json = JsonUtils.toJson(event);
        BasketCreatedEvent deserialized = JsonUtils.fromJson(json, BasketCreatedEvent.class);
        
        // Then
        assertThat(deserialized).isEqualTo(event);
        assertThat(deserialized.getType()).isEqualTo(BasketCreatedEvent.EVENT_TYPE);
    }
    
    @ParameterizedTest
    @ValueSource(classes = {SolaceEventPublisher.class, KafkaEventPublisher.class})
    public void eventPublisher_shouldHandleBasketCreatedEvent(Class<EventPublisher> publisherClass) {
        // Test that both implementations handle events consistently
        EventPublisher publisher = createPublisher(publisherClass);
        BasketCreatedEvent event = createTestEvent();
        
        StepVerifier.create(publisher.publish("basket.created", "TEST_BASKET", event))
            .verifyComplete();
    }
}
```

This comprehensive event contract specification ensures:

✅ **Implementation Independence**: Events work consistently across Solace and Kafka  
✅ **Schema Evolution**: Versioned events with backward compatibility  
✅ **Distributed Transactions**: Saga pattern for complex workflows  
✅ **Event Sourcing**: Complete audit trail and state reconstruction  
✅ **Error Handling**: Dead letter queues and retry mechanisms  
✅ **Performance Optimization**: Different configurations per event type  
✅ **Monitoring**: Rich metadata for observability and debugging
