# Service Contracts - API Specifications

## Overview

This document defines the **complete service contracts** for all microservices in the Custom Index Basket Management Platform. These contracts specify input/output formats, validation rules, and error handling patterns that remain consistent regardless of underlying implementation (Solace PubSub+/Kafka, SolCache/Redis).

## Contract Design Principles

```yaml
API Design Standards:
  ✅ RESTful resource-based URLs
  ✅ Reactive return types (Mono/Flux)
  ✅ Consistent error response format
  ✅ OpenAPI 3.0 specification compliance
  ✅ Version-aware API contracts

Data Standards:
  ✅ JSON for all request/response bodies
  ✅ ISO 8601 for timestamps
  ✅ ISO 4217 for currency codes
  ✅ Standardized field naming (camelCase)
  ✅ Comprehensive validation annotations

Implementation Agnostic:
  ✅ No technology-specific details in contracts
  ✅ Abstracted messaging/caching patterns
  ✅ Consistent behavior across implementations
  ✅ Technology migration without contract changes
```

## Common Data Models

### Base Models

```java
/**
 * Standard API response wrapper
 */
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String timestamp;
    private String requestId;
    private List<ValidationError> errors;
    
    // getters, setters, builders
}

/**
 * Pagination wrapper for list responses
 */
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // getters, setters, builders
}

/**
 * Standard error response
 */
public class ErrorResponse {
    private String error;
    private String message;
    private String timestamp;
    private String path;
    private int status;
    private String requestId;
    private List<ValidationError> details;
    
    // getters, setters, builders
}

/**
 * Validation error details
 */
public class ValidationError {
    private String field;
    private Object rejectedValue;
    private String message;
    private String code;
    
    // getters, setters, builders
}
```

### Business Domain Models

```java
/**
 * Basket entity with validation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Basket {
    
    @NotBlank(message = "Basket code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,20}$", message = "Basket code must be 3-20 characters, uppercase alphanumeric and underscore only")
    private String basketCode;
    
    @NotBlank(message = "Basket name is required")
    @Size(min = 3, max = 255, message = "Basket name must be between 3 and 255 characters")
    private String basketName;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    @NotBlank(message = "Basket type is required")
    @Pattern(regexp = "^(THEMATIC|SECTOR|GEOGRAPHIC|CUSTOM)$", message = "Invalid basket type")
    private String basketType;
    
    @NotBlank(message = "Base currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    private String baseCurrency;
    
    @DecimalMin(value = "99.95", message = "Total weight must be at least 99.95%")
    @DecimalMax(value = "100.05", message = "Total weight must not exceed 100.05%")
    private BigDecimal totalWeight;
    
    @Pattern(regexp = "^(DRAFT|BACKTESTING|BACKTESTED|PENDING_APPROVAL|APPROVED|REJECTED|LISTING|LISTED|ACTIVE|SUSPENDED|DELISTED)$")
    private String status;
    
    @Pattern(regexp = "^v\\d+\\.\\d+$", message = "Version must follow format vX.Y")
    private String version;
    
    private String previousVersion;
    
    @NotBlank(message = "Created by is required")
    private String createdBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
    
    private String approvedBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime approvedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime listedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime activatedAt;
    
    @Valid
    @NotEmpty(message = "Basket must contain at least one constituent")
    @Size(max = 100, message = "Basket cannot contain more than 100 constituents")
    private List<BasketConstituent> constituents;
    
    // getters, setters, builders
}

/**
 * Basket constituent with validation
 */
public class BasketConstituent {
    
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z0-9.]{1,20}$", message = "Invalid symbol format")
    private String symbol;
    
    @Size(max = 255, message = "Symbol name cannot exceed 255 characters")
    private String symbolName;
    
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be at least 0.01%")
    @DecimalMax(value = "50.00", message = "Weight cannot exceed 50.00%")
    @Digits(integer = 2, fraction = 4, message = "Weight precision: max 2 digits before decimal, 4 after")
    private BigDecimal weight;
    
    @Min(value = 1, message = "Shares must be at least 1")
    private Long shares;
    
    @DecimalMin(value = "0.01", message = "Target allocation must be at least 0.01%")
    @DecimalMax(value = "50.00", message = "Target allocation cannot exceed 50.00%")
    private BigDecimal targetAllocation;
    
    @Size(max = 100, message = "Sector name cannot exceed 100 characters")
    private String sector;
    
    @Size(max = 100, message = "Country name cannot exceed 100 characters")
    private String country;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    private String currency;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime addedAt;
    
    // getters, setters, builders
}

/**
 * Market price data
 */
public class MarketPrice {
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0001", message = "Price must be positive")
    @Digits(integer = 10, fraction = 4, message = "Price precision: max 10 digits before decimal, 4 after")
    private BigDecimal price;
    
    @DecimalMin(value = "0.0001", message = "Bid must be positive")
    private BigDecimal bid;
    
    @DecimalMin(value = "0.0001", message = "Ask must be positive")
    private BigDecimal ask;
    
    @Min(value = 0, message = "Volume cannot be negative")
    private Long volume;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    private String currency;
    
    @Size(max = 20, message = "Exchange code cannot exceed 20 characters")
    private String exchange;
    
    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @Pattern(regexp = "^(BLOOMBERG|REFINITIV|ALPHA_VANTAGE|YAHOO|MANUAL)$")
    private String source;
    
    // getters, setters, builders
}

/**
 * Basket valuation result
 */
public class BasketValuation {
    
    @NotBlank(message = "Basket code is required")
    private String basketCode;
    
    @NotNull(message = "Total value is required")
    @DecimalMin(value = "0.01", message = "Total value must be positive")
    private BigDecimal totalValue;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    private String baseCurrency;
    
    @Min(value = 1, message = "Constituent count must be at least 1")
    private Integer constituentCount;
    
    @NotNull(message = "Timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastUpdated;
    
    @Pattern(regexp = "^(REAL_TIME|CALCULATED|ESTIMATED)$")
    private String calculationSource;
    
    private List<ConstituentValuation> constituentValues;
    
    // getters, setters, builders
}

/**
 * Individual constituent valuation
 */
public class ConstituentValuation {
    
    private String symbol;
    private BigDecimal price;
    private BigDecimal weight;
    private Long shares;
    private BigDecimal value;
    private BigDecimal contribution;
    private String currency;
    private LocalDateTime lastPriceUpdate;
    
    // getters, setters, builders
}
```

## Service Contracts

## 1. Basket Core Service

### 1.1 Basket Management APIs

#### Create Basket
```java
@PostMapping("/api/baskets")
public Mono<ResponseEntity<ApiResponse<Basket>>> createBasket(
    @Valid @RequestBody CreateBasketRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class CreateBasketRequest {
    
    @NotBlank(message = "Basket code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,20}$")
    private String basketCode;
    
    @NotBlank(message = "Basket name is required")
    @Size(min = 3, max = 255)
    private String basketName;
    
    @Size(max = 1000)
    private String description;
    
    @NotBlank(message = "Basket type is required")
    @Pattern(regexp = "^(THEMATIC|SECTOR|GEOGRAPHIC|CUSTOM)$")
    private String basketType;
    
    @NotBlank(message = "Base currency is required")
    @Pattern(regexp = "^[A-Z]{3}$")
    private String baseCurrency;
    
    @Valid
    @NotEmpty(message = "Constituents list cannot be empty")
    @Size(max = 100, message = "Maximum 100 constituents allowed")
    private List<CreateConstituentRequest> constituents;
    
    // getters, setters
}

public class CreateConstituentRequest {
    
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z0-9.]{1,20}$")
    private String symbol;
    
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "50.00")
    @Digits(integer = 2, fraction = 4)
    private BigDecimal weight;
    
    @Min(value = 1)
    private Long shares;
    
    private String sector;
    private String country;
    private String currency;
    
    // getters, setters
}

// Output Contract
// Returns: ApiResponse<Basket> with HTTP 201 Created
// Errors: 400 Bad Request, 409 Conflict (duplicate code), 422 Validation Error
```

#### Get Basket
```java
@GetMapping("/api/baskets/{basketCode}")
public Mono<ResponseEntity<ApiResponse<Basket>>> getBasket(
    @PathVariable("basketCode") 
    @Pattern(regexp = "^[A-Z0-9_]{3,20}$", message = "Invalid basket code format") 
    String basketCode,
    @RequestParam(value = "includeConstituents", defaultValue = "true") Boolean includeConstituents,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
// Returns: ApiResponse<Basket> with HTTP 200 OK
// Errors: 404 Not Found, 400 Bad Request
```

#### Update Basket
```java
@PutMapping("/api/baskets/{basketCode}")
public Mono<ResponseEntity<ApiResponse<Basket>>> updateBasket(
    @PathVariable("basketCode") String basketCode,
    @Valid @RequestBody UpdateBasketRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class UpdateBasketRequest {
    
    @Size(min = 3, max = 255)
    private String basketName;
    
    @Size(max = 1000)
    private String description;
    
    @Pattern(regexp = "^(THEMATIC|SECTOR|GEOGRAPHIC|CUSTOM)$")
    private String basketType;
    
    @Valid
    @Size(max = 100)
    private List<UpdateConstituentRequest> constituents;
    
    // getters, setters
}

// Output Contract
// Returns: ApiResponse<Basket> with HTTP 200 OK
// Errors: 404 Not Found, 400 Bad Request, 409 Conflict (status), 422 Validation Error
```

#### List Baskets
```java
@GetMapping("/api/baskets")
public Mono<ResponseEntity<PagedResponse<BasketSummary>>> listBaskets(
    @RequestParam(value = "page", defaultValue = "0") @Min(0) Integer page,
    @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) Integer size,
    @RequestParam(value = "status", required = false) String status,
    @RequestParam(value = "basketType", required = false) String basketType,
    @RequestParam(value = "createdBy", required = false) String createdBy,
    @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
    @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class BasketSummary {
    private String basketCode;
    private String basketName;
    private String basketType;
    private String baseCurrency;
    private String status;
    private String version;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer constituentCount;
    private BigDecimal totalWeight;
    
    // getters, setters
}

// Returns: PagedResponse<BasketSummary> with HTTP 200 OK
// Errors: 400 Bad Request (invalid parameters)
```

### 1.2 Approval Workflow APIs

#### Submit for Approval
```java
@PostMapping("/api/baskets/{basketCode}/submit")
public Mono<ResponseEntity<ApiResponse<ApprovalResult>>> submitForApproval(
    @PathVariable("basketCode") String basketCode,
    @Valid @RequestBody SubmitApprovalRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class SubmitApprovalRequest {
    
    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String comments;
    
    @Pattern(regexp = "^(SINGLE|DUAL)$", message = "Invalid approval type")
    private String approvalType;
    
    @Email(message = "Invalid approver email format")
    private String preferredApprover;
    
    @Pattern(regexp = "^(NORMAL|URGENT|EXPEDITED)$")
    private String priority;
    
    // getters, setters
}

// Output Contract
public class ApprovalResult {
    private String approvalId;
    private String basketCode;
    private String status;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String approvalType;
    private String estimatedApprovalTime;
    
    // getters, setters
}

// Returns: ApiResponse<ApprovalResult> with HTTP 202 Accepted
// Errors: 404 Not Found, 409 Conflict (invalid status), 422 Validation Error
```

#### Approve Basket
```java
@PostMapping("/api/baskets/{basketCode}/approve")
public Mono<ResponseEntity<ApiResponse<ApprovalResult>>> approveBasket(
    @PathVariable("basketCode") String basketCode,
    @Valid @RequestBody ApprovalDecisionRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class ApprovalDecisionRequest {
    
    @NotBlank(message = "Approval ID is required")
    private String approvalId;
    
    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;
    
    @Pattern(regexp = "^(APPROVE|REJECT|REQUEST_CHANGES)$")
    private String decision;
    
    private List<String> conditions;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime effectiveDate;
    
    // getters, setters
}

// Returns: ApiResponse<ApprovalResult> with HTTP 200 OK
// Errors: 404 Not Found, 403 Forbidden, 409 Conflict, 422 Validation Error
```

#### Get Pending Approvals
```java
@GetMapping("/api/approvals/pending")
public Mono<ResponseEntity<PagedResponse<PendingApproval>>> getPendingApprovals(
    @RequestParam(value = "page", defaultValue = "0") @Min(0) Integer page,
    @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) Integer size,
    @RequestParam(value = "approvalType", required = false) String approvalType,
    @RequestParam(value = "priority", required = false) String priority,
    @RequestParam(value = "assignedTo", required = false) String assignedTo,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class PendingApproval {
    private String approvalId;
    private String basketCode;
    private String basketName;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String approvalType;
    private String priority;
    private String assignedTo;
    private Integer daysPending;
    private String slaStatus;
    
    // getters, setters
}

// Returns: PagedResponse<PendingApproval> with HTTP 200 OK
```

### 1.3 State Management APIs

#### Get Basket State
```java
@GetMapping("/api/baskets/{basketCode}/state")
public Mono<ResponseEntity<ApiResponse<BasketState>>> getBasketState(
    @PathVariable("basketCode") String basketCode,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class BasketState {
    private String basketCode;
    private String currentState;
    private String previousState;
    private LocalDateTime lastTransitionAt;
    private Integer transitionCount;
    private Integer retryCount;
    private List<String> allowedTransitions;
    private Map<String, Object> stateMetadata;
    
    // getters, setters
}

// Returns: ApiResponse<BasketState> with HTTP 200 OK
// Errors: 404 Not Found
```

#### Trigger State Transition
```java
@PostMapping("/api/baskets/{basketCode}/transition")
public Mono<ResponseEntity<ApiResponse<BasketState>>> triggerTransition(
    @PathVariable("basketCode") String basketCode,
    @Valid @RequestBody StateTransitionRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class StateTransitionRequest {
    
    @NotBlank(message = "Event is required")
    @Pattern(regexp = "^(SUBMIT_FOR_BACKTEST|BACKTEST_COMPLETED|SUBMIT_FOR_APPROVAL|APPROVE|REJECT|LIST|ACTIVATE|SUSPEND|DELIST)$")
    private String event;
    
    @Size(max = 500)
    private String reason;
    
    private Map<String, Object> eventData;
    
    @AssertTrue(message = "Force transition requires admin privileges")
    private Boolean forceTransition = false;
    
    // getters, setters
}

// Returns: ApiResponse<BasketState> with HTTP 200 OK
// Errors: 404 Not Found, 409 Conflict (invalid transition), 422 Validation Error
```

## 2. Market Data Service

### 2.1 Historical Data APIs

#### Get Historical Prices
```java
@GetMapping("/api/market-data/historical/{symbol}")
public Flux<MarketPrice> getHistoricalPrices(
    @PathVariable("symbol") 
    @Pattern(regexp = "^[A-Z0-9.]{1,20}$") String symbol,
    @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    @RequestParam(value = "frequency", defaultValue = "DAILY") 
    @Pattern(regexp = "^(INTRADAY|DAILY|WEEKLY|MONTHLY)$") String frequency,
    @RequestParam(value = "adjustments", defaultValue = "true") Boolean adjustments,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Validation Rules
// - startDate must be before endDate
// - date range cannot exceed 5 years for intraday data
// - date range cannot exceed 20 years for daily data
// - symbol must exist in our universe

// Returns: Flux<MarketPrice> with HTTP 200 OK
// Errors: 400 Bad Request, 404 Not Found (symbol), 429 Too Many Requests
```

#### Get Basket Historical Data
```java
@GetMapping("/api/market-data/basket/{basketCode}/historical")
public Flux<BasketHistoricalData> getBasketHistoricalData(
    @PathVariable("basketCode") String basketCode,
    @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    @RequestParam(value = "frequency", defaultValue = "DAILY") String frequency,
    @RequestParam(value = "currency", required = false) String currency,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class BasketHistoricalData {
    private LocalDate date;
    private String basketCode;
    private BigDecimal basketValue;
    private String currency;
    private List<ConstituentHistoricalData> constituents;
    private BigDecimal dailyReturn;
    private BigDecimal cumulativeReturn;
    
    // getters, setters
}

public class ConstituentHistoricalData {
    private String symbol;
    private BigDecimal price;
    private BigDecimal adjustedPrice;
    private Long volume;
    private BigDecimal weight;
    private BigDecimal value;
    private BigDecimal contribution;
    
    // getters, setters
}

// Returns: Flux<BasketHistoricalData> with HTTP 200 OK
// Errors: 400 Bad Request, 404 Not Found (basket)
```

### 2.2 Real-time Data APIs

#### Get Current Price
```java
@GetMapping("/api/market-data/live/{symbol}")
public Mono<ResponseEntity<ApiResponse<MarketPrice>>> getCurrentPrice(
    @PathVariable("symbol") String symbol,
    @RequestParam(value = "source", required = false) String source,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Returns: ApiResponse<MarketPrice> with HTTP 200 OK
// Errors: 404 Not Found, 503 Service Unavailable (data feed down)
```

#### Get Current Basket Valuation
```java
@GetMapping("/api/market-data/basket/{basketCode}/valuation")
public Mono<ResponseEntity<ApiResponse<BasketValuation>>> getCurrentBasketValuation(
    @PathVariable("basketCode") String basketCode,
    @RequestParam(value = "currency", required = false) String currency,
    @RequestParam(value = "forceRefresh", defaultValue = "false") Boolean forceRefresh,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Returns: ApiResponse<BasketValuation> with HTTP 200 OK
// Errors: 404 Not Found, 503 Service Unavailable
```

#### Subscribe to Price Stream
```java
@GetMapping(value = "/api/market-data/stream/{symbol}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<MarketPrice>> subscribeToPriceStream(
    @PathVariable("symbol") String symbol,
    @RequestParam(value = "frequency", defaultValue = "REAL_TIME") String frequency,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Returns: Server-Sent Events stream of MarketPrice
// Errors: 404 Not Found, 503 Service Unavailable
```

### 2.3 Data Management APIs

#### Refresh Market Data
```java
@PostMapping("/api/market-data/refresh")
public Mono<ResponseEntity<ApiResponse<RefreshResult>>> refreshMarketData(
    @Valid @RequestBody RefreshDataRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class RefreshDataRequest {
    
    @Size(max = 100, message = "Maximum 100 symbols per request")
    private List<@Pattern(regexp = "^[A-Z0-9.]{1,20}$") String> symbols;
    
    @Pattern(regexp = "^(CURRENT|HISTORICAL|BOTH)$")
    private String dataType;
    
    @Pattern(regexp = "^(BLOOMBERG|REFINITIV|ALL)$")
    private String source;
    
    @AssertTrue(message = "Force refresh requires admin privileges")
    private Boolean forceRefresh = false;
    
    // getters, setters
}

// Output Contract
public class RefreshResult {
    private String refreshId;
    private Integer totalSymbols;
    private Integer successCount;
    private Integer errorCount;
    private List<String> errors;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String status;
    
    // getters, setters
}

// Returns: ApiResponse<RefreshResult> with HTTP 202 Accepted
// Errors: 400 Bad Request, 429 Too Many Requests
```

## 3. Publishing Service

### 3.1 Listing Management APIs

#### Publish Basket Listing
```java
@PostMapping("/api/publishing/list/{basketCode}")
public Mono<ResponseEntity<ApiResponse<PublishingResult>>> publishBasketListing(
    @PathVariable("basketCode") String basketCode,
    @Valid @RequestBody PublishListingRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class PublishListingRequest {
    
    @NotEmpty(message = "At least one vendor must be specified")
    private List<@Pattern(regexp = "^(BLOOMBERG|REFINITIV|ALL)$") String> vendors;
    
    @Pattern(regexp = "^(IMMEDIATE|SCHEDULED)$")
    private String publishingMode;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime scheduledTime;
    
    @Size(max = 500)
    private String notes;
    
    private Map<String, Object> vendorSpecificSettings;
    
    // getters, setters
}

// Output Contract
public class PublishingResult {
    private String publishingId;
    private String basketCode;
    private List<VendorPublishingStatus> vendorStatuses;
    private String overallStatus;
    private LocalDateTime initiatedAt;
    private LocalDateTime estimatedCompletion;
    private String slaStatus;
    
    // getters, setters
}

public class VendorPublishingStatus {
    private String vendor;
    private String status;
    private String vendorReference;
    private String message;
    private LocalDateTime timestamp;
    private Integer retryCount;
    
    // getters, setters
}

// Returns: ApiResponse<PublishingResult> with HTTP 202 Accepted
// Errors: 404 Not Found, 409 Conflict (status), 422 Validation Error
```

#### Get Publishing Status
```java
@GetMapping("/api/publishing/status/{basketCode}")
public Mono<ResponseEntity<ApiResponse<PublishingStatus>>> getPublishingStatus(
    @PathVariable("basketCode") String basketCode,
    @RequestParam(value = "includeHistory", defaultValue = "false") Boolean includeHistory,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class PublishingStatus {
    private String basketCode;
    private String listingStatus;
    private String pricingStatus;
    private List<VendorStatus> vendors;
    private LocalDateTime lastListingUpdate;
    private LocalDateTime lastPriceUpdate;
    private List<PublishingEvent> history;
    
    // getters, setters
}

public class VendorStatus {
    private String vendor;
    private String listingStatus;
    private String pricingStatus;
    private String listingReference;
    private LocalDateTime listedAt;
    private LocalDateTime lastPriceUpdate;
    private Integer priceUpdateCount;
    private String healthStatus;
    
    // getters, setters
}

// Returns: ApiResponse<PublishingStatus> with HTTP 200 OK
// Errors: 404 Not Found
```

### 3.2 Price Publishing APIs

#### Force Price Update
```java
@PostMapping("/api/publishing/prices/update")
public Mono<ResponseEntity<ApiResponse<PriceUpdateResult>>> forcePriceUpdate(
    @Valid @RequestBody ForcePriceUpdateRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class ForcePriceUpdateRequest {
    
    private List<@Pattern(regexp = "^[A-Z0-9_]{3,20}$") String> basketCodes;
    
    private List<@Pattern(regexp = "^(BLOOMBERG|REFINITIV|ALL)$") String> vendors;
    
    @Pattern(regexp = "^(CURRENT|CALCULATED)$")
    private String priceType;
    
    @Size(max = 200)
    private String reason;
    
    // getters, setters
}

// Output Contract
public class PriceUpdateResult {
    private String updateId;
    private Integer totalBaskets;
    private Integer successfulUpdates;
    private Integer failedUpdates;
    private List<BasketUpdateStatus> basketStatuses;
    private LocalDateTime completedAt;
    
    // getters, setters
}

public class BasketUpdateStatus {
    private String basketCode;
    private String status;
    private Map<String, String> vendorResults;
    private String errorMessage;
    private LocalDateTime updatedAt;
    
    // getters, setters
}

// Returns: ApiResponse<PriceUpdateResult> with HTTP 202 Accepted
// Errors: 400 Bad Request, 403 Forbidden
```

### 3.3 Vendor Management APIs

#### Get Vendor Health
```java
@GetMapping("/api/publishing/vendors/health")
public Mono<ResponseEntity<ApiResponse<VendorHealthStatus>>> getVendorHealth(
    @RequestParam(value = "vendor", required = false) String vendor,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class VendorHealthStatus {
    private Map<String, VendorHealth> vendors;
    private String overallHealth;
    private LocalDateTime lastChecked;
    
    // getters, setters
}

public class VendorHealth {
    private String vendor;
    private String connectionStatus;
    private String apiStatus;
    private Integer responseTime;
    private BigDecimal successRate;
    private LocalDateTime lastSuccessfulOperation;
    private List<String> activeIssues;
    
    // getters, setters
}

// Returns: ApiResponse<VendorHealthStatus> with HTTP 200 OK
```

## 4. Analytics Service

### 4.1 Backtesting APIs

#### Run Comprehensive Backtest
```java
@PostMapping("/api/analytics/backtest")
public Mono<ResponseEntity<ApiResponse<BacktestResult>>> runBacktest(
    @Valid @RequestBody BacktestRequest request,
    @RequestHeader("X-User-ID") String userId,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Input Contract
public class BacktestRequest {
    
    @NotBlank(message = "Basket code is required")
    private String basketCode;
    
    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDate endDate;
    
    @DecimalMin(value = "1000.00", message = "Initial value must be at least 1000")
    @DecimalMax(value = "1000000000.00", message = "Initial value cannot exceed 1 billion")
    private BigDecimal initialValue;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency;
    
    @NotEmpty(message = "At least one benchmark index is required")
    @Size(max = 10, message = "Maximum 10 benchmark indices allowed")
    private List<@Pattern(regexp = "^[A-Z0-9]{2,10}$") String> benchmarkIndices;
    
    @Pattern(regexp = "^(MONTHLY|QUARTERLY|ANNUALLY|NEVER)$")
    private String rebalancingFrequency;
    
    @DecimalMin(value = "0.0", message = "Transaction cost cannot be negative")
    @DecimalMax(value = "1.0", message = "Transaction cost cannot exceed 100%")
    private BigDecimal transactionCost;
    
    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        return endDate == null || startDate == null || endDate.isAfter(startDate);
    }
    
    // getters, setters
}

// Output Contract  
public class BacktestResult {
    private String backtestId;
    private String basketCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;
    private BigDecimal initialValue;
    private BigDecimal finalValue;
    private PerformanceMetrics performance;
    private RiskMetrics risk;
    private List<IndexComparison> benchmarkComparisons;
    private List<RebalancingEvent> rebalancingEvents;
    private LocalDateTime generatedAt;
    private String status;
    
    // getters, setters
}

public class PerformanceMetrics {
    private BigDecimal totalReturn;
    private BigDecimal annualizedReturn;
    private BigDecimal volatility;
    private BigDecimal sharpeRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal maxDrawdown;
    private BigDecimal valueAtRisk5;
    private BigDecimal valueAtRisk95;
    private Integer tradingDays;
    
    // getters, setters
}

public class RiskMetrics {
    private BigDecimal beta;
    private BigDecimal alpha;
    private BigDecimal trackingError;
    private BigDecimal informationRatio;
    private BigDecimal calmarRatio;
    private BigDecimal downsideDeviation;
    private Map<String, BigDecimal> correlations;
    
    // getters, setters
}

public class IndexComparison {
    private String indexCode;
    private String indexName;
    private BigDecimal correlation;
    private BigDecimal beta;
    private BigDecimal alpha;
    private BigDecimal outperformance;
    private BigDecimal trackingError;
    private BigDecimal informationRatio;
    
    // getters, setters
}

// Returns: ApiResponse<BacktestResult> with HTTP 202 Accepted (async processing)
// Errors: 400 Bad Request, 404 Not Found (basket), 422 Validation Error
```

#### Run Quick Backtest
```java
@PostMapping("/api/analytics/backtest/quick/{basketCode}")
public Mono<ResponseEntity<ApiResponse<QuickBacktestResult>>> runQuickBacktest(
    @PathVariable("basketCode") String basketCode,
    @RequestParam(value = "period", defaultValue = "1Y") 
    @Pattern(regexp = "^(1M|3M|6M|1Y|2Y|3Y|5Y)$") String period,
    @RequestParam(value = "currency", defaultValue = "USD") String currency,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class QuickBacktestResult {
    private String basketCode;
    private String period;
    private BigDecimal totalReturn;
    private BigDecimal annualizedReturn;
    private BigDecimal volatility;
    private BigDecimal sharpeRatio;
    private BigDecimal maxDrawdown;
    private Map<String, BigDecimal> benchmarkComparisons;
    private LocalDateTime generatedAt;
    
    // getters, setters
}

// Returns: ApiResponse<QuickBacktestResult> with HTTP 200 OK (synchronous for quick tests)
// Errors: 400 Bad Request, 404 Not Found
```

#### Get Backtest Results
```java
@GetMapping("/api/analytics/backtest/{backtestId}")
public Mono<ResponseEntity<ApiResponse<BacktestResult>>> getBacktestResult(
    @PathVariable("backtestId") String backtestId,
    @RequestParam(value = "includeDetails", defaultValue = "true") Boolean includeDetails,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Returns: ApiResponse<BacktestResult> with HTTP 200 OK
// Errors: 404 Not Found
```

### 4.2 Performance Analysis APIs

#### Get Performance Metrics
```java
@GetMapping("/api/analytics/performance/{basketCode}")
public Mono<ResponseEntity<ApiResponse<PerformanceAnalysis>>> getPerformanceMetrics(
    @PathVariable("basketCode") String basketCode,
    @RequestParam(value = "period", defaultValue = "YTD") String period,
    @RequestParam(value = "frequency", defaultValue = "DAILY") String frequency,
    @RequestParam(value = "currency", required = false) String currency,
    @RequestHeader(value = "X-Request-ID", required = false) String requestId
);

// Output Contract
public class PerformanceAnalysis {
    private String basketCode;
    private String period;
    private String currency;
    private PerformanceMetrics metrics;
    private List<PerformanceDataPoint> timeSeries;
    private Map<String, Object> attribution;
    private LocalDateTime generatedAt;
    
    // getters, setters
}

public class PerformanceDataPoint {
    private LocalDate date;
    private BigDecimal value;
    private BigDecimal dailyReturn;
    private BigDecimal cumulativeReturn;
    private BigDecimal volatility;
    
    // getters, setters
}

// Returns: ApiResponse<PerformanceAnalysis> with HTTP 200 OK
// Errors: 404 Not Found, 400 Bad Request
```

## Common Error Handling

### Standard Error Responses

```java
// Validation Error (HTTP 422)
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
      "field": "basketCode",
      "rejectedValue": "invalid-code",
      "message": "Basket code must be 3-20 characters, uppercase alphanumeric and underscore only",
      "code": "PATTERN_VIOLATION"
    }
  ]
}

// Business Rule Error (HTTP 409)
{
  "success": false,
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "Cannot modify basket in APPROVED status",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "path": "/api/baskets/TECH_BASKET",
  "status": 409,
  "requestId": "req-123456"
}

// Resource Not Found (HTTP 404)
{
  "success": false,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Basket with code 'UNKNOWN_BASKET' not found",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "path": "/api/baskets/UNKNOWN_BASKET",
  "status": 404,
  "requestId": "req-123456"
}

// Service Unavailable (HTTP 503)
{
  "success": false,
  "error": "SERVICE_UNAVAILABLE",
  "message": "Market data service is temporarily unavailable",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "path": "/api/market-data/live/AAPL",
  "status": 503,
  "requestId": "req-123456"
}
```

### Error Code Categories

```yaml
Client Errors (4xx):
  400: BAD_REQUEST - Invalid request syntax
  401: UNAUTHORIZED - Authentication required
  403: FORBIDDEN - Insufficient permissions
  404: RESOURCE_NOT_FOUND - Resource does not exist
  409: BUSINESS_RULE_VIOLATION - Business logic conflict
  422: VALIDATION_ERROR - Request validation failed
  429: RATE_LIMIT_EXCEEDED - Too many requests

Server Errors (5xx):
  500: INTERNAL_SERVER_ERROR - Unexpected server error
  502: BAD_GATEWAY - Upstream service error
  503: SERVICE_UNAVAILABLE - Service temporarily down
  504: GATEWAY_TIMEOUT - Upstream timeout
```

## API Documentation Standards

### OpenAPI Specification Example

```yaml
openapi: 3.0.3
info:
  title: Basket Core Service API
  version: 1.0.0
  description: RESTful API for basket lifecycle management
  
servers:
  - url: https://api.basket-platform.company.com/v1
    description: Production server
  - url: https://api-staging.basket-platform.company.com/v1
    description: Staging server

paths:
  /api/baskets:
    post:
      summary: Create a new basket
      operationId: createBasket
      tags:
        - Basket Management
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateBasketRequest'
      responses:
        '201':
          description: Basket created successfully
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/Basket'
        '400':
          $ref: '#/components/responses/BadRequest'
        '409':
          $ref: '#/components/responses/Conflict'
        '422':
          $ref: '#/components/responses/ValidationError'

components:
  schemas:
    ApiResponse:
      type: object
      properties:
        success:
          type: boolean
        message:
          type: string
        timestamp:
          type: string
          format: date-time
        requestId:
          type: string
      required:
        - success
        - timestamp
        
    Basket:
      type: object
      properties:
        basketCode:
          type: string
          pattern: '^[A-Z0-9_]{3,20}$'
          example: 'TECH_BASKET'
        basketName:
          type: string
          minLength: 3
          maxLength: 255
          example: 'Technology Leaders Basket'
        # ... other properties
      required:
        - basketCode
        - basketName
        - basketType
        - baseCurrency
        
  responses:
    BadRequest:
      description: Bad request
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
            
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

security:
  - BearerAuth: []
```

This comprehensive service contract specification ensures:

✅ **Implementation Independence**: Contracts work with any messaging/caching implementation  
✅ **Consistent APIs**: Uniform request/response patterns across all services  
✅ **Comprehensive Validation**: Input validation rules for data integrity  
✅ **Error Handling**: Standardized error responses and codes  
✅ **Documentation**: OpenAPI specifications for developer experience  
✅ **Type Safety**: Strong typing with validation annotations  
✅ **Business Rules**: Domain-specific validation and constraints
