# Service Contracts Documentation

## Overview

This directory contains comprehensive **service contracts** that define the input/output specifications, validation rules, and error handling patterns for all microservices in the Custom Index Basket Management Platform. These contracts ensure consistency and reliability across different technology implementations (Solace/Kafka, SolCache/Redis).

## Contract Philosophy

```yaml
Design Principles:
  ✅ Implementation-agnostic contracts
  ✅ Consistent behavior across technology stacks
  ✅ Comprehensive validation and error handling
  ✅ Self-documenting API specifications
  ✅ Version-aware evolution support

Business Value:
  ✅ Accelerated development with clear contracts
  ✅ Reduced integration errors
  ✅ Consistent user experience
  ✅ Simplified testing and debugging
  ✅ Technology migration without API changes
```

## Contract Documents

### 📋 [Service Contracts](./service-contracts.md)
**Complete REST API specifications for all microservices**

- **Basket Core Service**: Lifecycle management, approval workflows, state transitions
- **Market Data Service**: Historical data, real-time pricing, data management
- **Publishing Service**: Vendor integration, listing management, price publishing
- **Analytics Service**: Backtesting, performance analysis, reporting

**Key Features:**
- Reactive return types (Mono/Flux) for all endpoints
- Comprehensive input validation with Bean Validation API
- Consistent response wrapper patterns
- Pagination support for list endpoints
- Rich metadata for debugging and tracing

### 🔄 [Event Contracts](./event-contracts.md)
**Asynchronous messaging specifications between services**

- **CloudEvents Compliance**: Standard event format across all implementations
- **Event Sourcing Patterns**: Complete audit trail for business events
- **Saga Coordination**: Distributed transaction management
- **Stream Processing**: Real-time data processing contracts

**Event Categories:**
- **Basket Lifecycle Events**: Created, Updated, Status Changed
- **Approval Workflow Events**: Requested, Completed, Rejected
- **Market Data Events**: Price Updates, Basket Valuations
- **Publishing Events**: Listed, Price Published, Vendor Status
- **Analytics Events**: Backtest Started/Completed

### ✅ [Validation Contracts](./validation-contracts.md)
**Comprehensive validation rules and error handling patterns**

- **Custom Validators**: Business-specific validation logic
- **Cross-field Validation**: Complex business rule enforcement
- **Reactive Error Handling**: Error patterns for WebFlux applications
- **Standardized Error Responses**: Consistent error format across services

**Validation Features:**
- Domain-specific validators (basket codes, symbols, currencies)
- Business date validation with market calendar integration
- Weight allocation validation for portfolio compliance
- Comprehensive error codes and metadata

### ⚡ [Actor & gRPC Contracts](./actor-grpc-contracts.md)
**High-performance concurrency and internal communication patterns**

- **Akka Typed Actors**: Type-safe actor model for concurrent processing
- **gRPC Services**: Binary protocol for efficient internal communication
- **Cluster Sharding**: Distributed actor placement and fault tolerance
- **Event Sourcing**: Actor persistence with Akka Persistence

**Key Features:**
- Partitioned basket entity actors for scalable state management
- Symbol actors for real-time price updates and subscriptions
- Protocol Buffer schemas for type-safe service contracts
- Bidirectional streaming for real-time data flows

### 🔄 [Hybrid Communication Strategy](./hybrid-communication-strategy.md)
**Smart protocol selection based on latency requirements and use cases**

- **REST APIs**: User-facing operations, external integrations (>100ms latency acceptable)
- **Event Streaming**: Asynchronous workflows, audit trails (10-100ms latency)
- **gRPC**: High-frequency internal calls, real-time processing (<10ms latency)
- **Actor Model**: Ultra-low latency operations, concurrent state management (<1ms latency)

**Smart Features:**
- Automatic protocol selection based on latency requirements
- Configuration-driven protocol rules and overrides
- Dynamic protocol switching based on performance metrics
- Performance optimization per communication pattern

## Implementation Examples

### REST API Contract Example
```java
// Service Interface (Implementation-Agnostic)
@PostMapping("/api/baskets")
public Mono<ResponseEntity<ApiResponse<Basket>>> createBasket(
    @Valid @RequestBody CreateBasketRequest request,
    @RequestHeader("X-User-ID") String userId
);

// Works with ANY messaging/caching implementation
@Service
public class BasketService {
    @Autowired
    private EventPublisher eventPublisher;  // Solace OR Kafka
    
    @Autowired  
    private CacheService cacheService;      // SolCache OR Redis
    
    public Mono<Basket> createBasket(CreateBasketRequest request, String userId) {
        return validateAndCreateBasket(request)
            .flatMap(basket -> 
                // Cache the basket
                cacheService.put("basket:" + basket.getBasketCode(), basket)
                    // Publish creation event
                    .then(eventPublisher.publish("basket.created", basket.getBasketCode(), 
                        new BasketCreatedEvent(basket)))
                    .thenReturn(basket)
            );
    }
}
```

### Event Contract Example
```java
// Event Schema (CloudEvents Compliant)
public class BasketCreatedEvent extends BaseEvent {
    public static final String EVENT_TYPE = "com.custom.basket.created.v1";
    
    private BasketCreatedData data;
    
    // Routing Configuration
    // Solace: basket/v1/*/*/created/{basketCode}
    // Kafka: basket.v1.created (partition by basketCode)
}

// Event Handler (Implementation-Agnostic)
@Component
public class BasketCreatedEventHandler extends BaseEventHandler<BasketCreatedEvent> {
    
    @Override
    protected Mono<Void> processEvent(BasketCreatedEvent event) {
        // Works with any messaging implementation
        return enrichBasketWithMarketData(event.getData())
            .then(updateBasketCache(event.getData()))
            .then(triggerBacktestIfRequested(event.getData()));
    }
}
```

### Validation Contract Example
```java
// Custom Business Validator
@ValidWeightAllocation(tolerance = 0.05)
@ValidDateRange(startField = "startDate", endField = "endDate", maxDaysRange = 365)
public class CreateBasketRequest {
    
    @ValidBasketCode(checkAvailability = true)
    private String basketCode;
    
    @ValidCurrency(checkSupported = true)
    private String baseCurrency;
    
    @Valid
    @NotEmpty
    private List<@Valid CreateConstituentRequest> constituents;
}

// Standardized Error Response
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "path": "/api/baskets",
  "status": 422,
  "requestId": "req-123456",
  "details": [
    {
      "field": "constituents[0].weight",
      "rejectedValue": 60.0,
      "message": "Weight cannot exceed 50.00%",
      "code": "WEIGHT_LIMIT_EXCEEDED"
    }
  ]
}
```

## Technology Implementation Matrix

### Messaging Layer Abstractions
| Contract Element | Solace PubSub+ | Apache Kafka | 
|------------------|----------------|--------------|
| **Event Publishing** | `SolaceEventPublisher` | `KafkaEventPublisher` |
| **Topic Structure** | `basket/v1/*/*/created/{id}` | `basket.v1.created` |
| **Guaranteed Delivery** | Persistent Messages | `acks=all` |
| **Event Ordering** | Topic Subscriptions | Partition Keys |
| **Dead Letter Queues** | Built-in DLQ | Kafka Streams DLQ |

### Caching Layer Abstractions
| Contract Element | SolCache | Redis |
|------------------|----------|-------|
| **Cache Operations** | `SolCacheService` | `RedisService` |
| **Data Structures** | Native Objects | JSON Strings |
| **TTL Management** | Region-based | Key-based |
| **Transactions** | Cache Transactions | Redis Transactions |
| **Clustering** | Data Grid | Redis Cluster |

### Validation Implementation
| Validation Type | Implementation | Technology Independence |
|-----------------|----------------|-------------------------|
| **Format Validation** | Bean Validation API | ✅ Framework-agnostic |
| **Business Rules** | Custom Validators | ✅ Pure Java logic |
| **Cross-service Validation** | Reactive calls via abstracted clients | ✅ Uses service abstractions |
| **Market Data Validation** | External API calls via abstracted interfaces | ✅ Vendor-agnostic |

## Contract Evolution Strategy

### Versioning Approach
```yaml
API Versioning:
  - URL-based versioning: /api/v1/baskets, /api/v2/baskets
  - Header-based versioning: Accept: application/vnd.api+json;version=1
  - Backward compatibility: Maintain previous versions for migration period
  
Event Versioning:
  - Schema versioning in event type: com.custom.basket.created.v2
  - CloudEvents schema evolution patterns
  - Consumer compatibility matrix maintenance
  
Validation Versioning:
  - Validation group evolution
  - Conditional validation based on API version
  - Deprecation warnings for old validation rules
```

### Migration Process
```yaml
Phase 1: Contract Definition (✅ Completed)
  - Define comprehensive service contracts
  - Create validation frameworks
  - Establish error handling patterns
  
Phase 2: Implementation Mapping
  - Map contracts to Solace implementations
  - Map contracts to Kafka/Redis implementations
  - Create abstraction layer bindings
  
Phase 3: Contract Testing
  - Consumer contract tests
  - Producer contract tests
  - Cross-implementation compatibility tests
  
Phase 4: Production Deployment
  - Blue-green deployment with contract validation
  - Real-time contract compliance monitoring
  - Rollback procedures for contract violations
```

## Contract Testing Strategy

### Consumer Contract Tests
```java
@SpringBootTest
@ExtendWith(PactConsumerTestExt.class)
public class BasketServiceContractTest {
    
    @Test
    @PactTestFor(pactMethod = "createBasketContract")
    public void testCreateBasketContract() {
        // Test that service honors the contract
        CreateBasketRequest request = createValidRequest();
        
        StepVerifier.create(basketService.createBasket(request, "test-user"))
            .assertNext(basket -> {
                assertThat(basket.getBasketCode()).isEqualTo(request.getBasketCode());
                assertThat(basket.getStatus()).isEqualTo("DRAFT");
            })
            .verifyComplete();
    }
    
    @Pact(consumer = "basket-service")
    public RequestResponsePact createBasketContract(PactDslWithProvider builder) {
        return builder
            .given("valid basket creation request")
            .uponReceiving("create basket request")
            .path("/api/baskets")
            .method("POST")
            .body(createBasketRequestJson())
            .willRespondWith()
            .status(201)
            .body(createBasketResponseJson())
            .toPact();
    }
}
```

### Cross-Implementation Tests
```java
@ParameterizedTest
@ValueSource(classes = {SolaceImplementation.class, KafkaImplementation.class})
public void contractComplianceTest(Class<?> implementationClass) {
    // Test that both implementations satisfy the same contracts
    EventPublisher publisher = createPublisher(implementationClass);
    CacheService cache = createCache(implementationClass);
    
    // Contract compliance test
    BasketService service = new BasketService(publisher, cache);
    CreateBasketRequest request = createValidRequest();
    
    StepVerifier.create(service.createBasket(request, "test-user"))
        .assertNext(basket -> assertContractCompliance(basket, request))
        .verifyComplete();
}
```

## Benefits Delivered

### 🎯 **Development Acceleration**
- **Clear Contracts**: Developers know exactly what to implement
- **Reduced Integration Time**: Consistent interfaces across services
- **Parallel Development**: Teams can work independently with clear contracts
- **Faster Debugging**: Standardized error responses and validation

### 🔧 **Technology Flexibility**
- **Implementation Independence**: Business logic unchanged across tech stacks
- **Migration Support**: Switch between Solace/Kafka without breaking contracts
- **Vendor Neutrality**: No technology-specific details in contracts
- **Future-Proofing**: Contracts evolve independently of implementations

### 🛡️ **Quality Assurance**
- **Comprehensive Validation**: Business rules enforced consistently
- **Error Handling**: Standardized error responses across all services
- **Contract Testing**: Automated verification of contract compliance
- **Backward Compatibility**: Safe evolution of APIs and events

### 📊 **Operational Excellence**
- **Monitoring**: Consistent metrics and observability patterns
- **Debugging**: Rich error metadata and tracing information
- **Documentation**: Self-documenting APIs with OpenAPI specifications
- **Compliance**: Audit trail through event sourcing patterns

These contracts provide the foundation for a robust, scalable, and maintainable microservices architecture that can evolve with changing business requirements while maintaining consistency and reliability across all implementations.
