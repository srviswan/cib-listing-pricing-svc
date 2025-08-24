# Validation and Error Handling Contracts

## Overview

This document defines comprehensive **validation rules** and **error handling contracts** for the Custom Index Basket Management Platform. These contracts ensure consistent data validation, error responses, and exception handling across all services, regardless of underlying technology implementation.

## Validation Framework Design

```yaml
Validation Strategy:
  ✅ Annotation-based validation with Bean Validation API
  ✅ Custom validators for business rules
  ✅ Cross-field validation for complex constraints
  ✅ Conditional validation based on context
  ✅ International validation for global markets

Error Handling Strategy:
  ✅ Consistent error response format across all services
  ✅ Structured error codes for programmatic handling
  ✅ Localized error messages for user experience
  ✅ Comprehensive error metadata for debugging
  ✅ Circuit breaker patterns for resilience
```

## Core Validation Annotations

### Business Domain Validators

```java
/**
 * Validates basket code format and availability
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BasketCodeValidator.class)
public @interface ValidBasketCode {
    String message() default "Invalid basket code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    boolean checkAvailability() default false;
    boolean allowDraft() default true;
}

@Component
public class BasketCodeValidator implements ConstraintValidator<ValidBasketCode, String> {
    
    @Autowired
    private BasketRepository basketRepository;
    
    private boolean checkAvailability;
    private boolean allowDraft;
    
    @Override
    public void initialize(ValidBasketCode constraintAnnotation) {
        this.checkAvailability = constraintAnnotation.checkAvailability();
        this.allowDraft = constraintAnnotation.allowDraft();
    }
    
    @Override
    public boolean isValid(String basketCode, ConstraintValidatorContext context) {
        if (basketCode == null || basketCode.isBlank()) {
            return false;
        }
        
        // Format validation
        if (!basketCode.matches("^[A-Z0-9_]{3,20}$")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Basket code must be 3-20 characters, uppercase alphanumeric and underscore only")
                .addConstraintViolation();
            return false;
        }
        
        // Reserved words check
        if (RESERVED_BASKET_CODES.contains(basketCode)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Basket code '" + basketCode + "' is reserved and cannot be used")
                .addConstraintViolation();
            return false;
        }
        
        // Availability check
        if (checkAvailability) {
            return basketRepository.existsByBasketCode(basketCode)
                .map(exists -> {
                    if (exists) {
                        Basket existing = basketRepository.findByBasketCode(basketCode).block();
                        if (!allowDraft && "DRAFT".equals(existing.getStatus())) {
                            return false;
                        }
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(
                            "Basket code '" + basketCode + "' is already in use")
                            .addConstraintViolation();
                        return false;
                    }
                    return true;
                })
                .blockOptional()
                .orElse(true);
        }
        
        return true;
    }
    
    private static final Set<String> RESERVED_BASKET_CODES = Set.of(
        "ADMIN", "TEST", "SYSTEM", "DEFAULT", "TEMP", "NULL", "UNDEFINED"
    );
}

/**
 * Validates symbol format and market availability
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SymbolValidator.class)
public @interface ValidSymbol {
    String message() default "Invalid symbol";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    boolean checkMarketData() default false;
    String[] allowedExchanges() default {};
}

@Component
public class SymbolValidator implements ConstraintValidator<ValidSymbol, String> {
    
    @Autowired
    private MarketDataService marketDataService;
    
    private boolean checkMarketData;
    private Set<String> allowedExchanges;
    
    @Override
    public void initialize(ValidSymbol constraintAnnotation) {
        this.checkMarketData = constraintAnnotation.checkMarketData();
        this.allowedExchanges = Set.of(constraintAnnotation.allowedExchanges());
    }
    
    @Override
    public boolean isValid(String symbol, ConstraintValidatorContext context) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        
        // Format validation
        if (!symbol.matches("^[A-Z0-9.]{1,20}$")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Symbol must be 1-20 characters, uppercase alphanumeric and dots only")
                .addConstraintViolation();
            return false;
        }
        
        // Market data availability check
        if (checkMarketData) {
            return marketDataService.isSymbolAvailable(symbol)
                .map(available -> {
                    if (!available) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(
                            "Symbol '" + symbol + "' is not available in market data feeds")
                            .addConstraintViolation();
                        return false;
                    }
                    return true;
                })
                .blockOptional()
                .orElse(false);
        }
        
        return true;
    }
}

/**
 * Validates currency code against ISO 4217 and supported currencies
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyValidator.class)
public @interface ValidCurrency {
    String message() default "Invalid currency code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    boolean checkSupported() default true;
}

@Component
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {
    
    private boolean checkSupported;
    
    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        this.checkSupported = constraintAnnotation.checkSupported();
    }
    
    @Override
    public boolean isValid(String currency, ConstraintValidatorContext context) {
        if (currency == null || currency.isBlank()) {
            return false;
        }
        
        // ISO 4217 format check
        if (!currency.matches("^[A-Z]{3}$")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Currency must be a 3-letter ISO 4217 code")
                .addConstraintViolation();
            return false;
        }
        
        // ISO 4217 validity check
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Currency '" + currency + "' is not a valid ISO 4217 currency code")
                .addConstraintViolation();
            return false;
        }
        
        // Supported currencies check
        if (checkSupported && !SUPPORTED_CURRENCIES.contains(currency)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Currency '" + currency + "' is not supported. Supported currencies: " + 
                SUPPORTED_CURRENCIES)
                .addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
        "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "SEK", "NOK", "DKK",
        "HKD", "SGD", "KRW", "CNY", "INR", "BRL", "MXN", "ZAR"
    );
}

/**
 * Validates weight allocation in basket constituents
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WeightAllocationValidator.class)
public @interface ValidWeightAllocation {
    String message() default "Invalid weight allocation";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    double tolerance() default 0.05; // 5 basis points tolerance
    boolean allowOverAllocation() default false;
}

public class WeightAllocationValidator implements ConstraintValidator<ValidWeightAllocation, Object> {
    
    private double tolerance;
    private boolean allowOverAllocation;
    
    @Override
    public void initialize(ValidWeightAllocation constraintAnnotation) {
        this.tolerance = constraintAnnotation.tolerance();
        this.allowOverAllocation = constraintAnnotation.allowOverAllocation();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        List<BasketConstituent> constituents = null;
        
        // Extract constituents from different object types
        if (value instanceof CreateBasketRequest) {
            constituents = ((CreateBasketRequest) value).getConstituents()
                .stream()
                .map(this::mapToBasketConstituent)
                .collect(Collectors.toList());
        } else if (value instanceof Basket) {
            constituents = ((Basket) value).getConstituents();
        } else if (value instanceof List) {
            constituents = (List<BasketConstituent>) value;
        }
        
        if (constituents == null || constituents.isEmpty()) {
            return true; // Let other validators handle empty lists
        }
        
        // Calculate total weight
        BigDecimal totalWeight = constituents.stream()
            .map(BasketConstituent::getWeight)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double totalWeightDouble = totalWeight.doubleValue();
        
        // Check minimum allocation
        if (totalWeightDouble < (100.0 - tolerance)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Total weight allocation (%.4f%%) is below required minimum (%.4f%%)", 
                    totalWeightDouble, 100.0 - tolerance))
                .addConstraintViolation();
            return false;
        }
        
        // Check maximum allocation
        if (!allowOverAllocation && totalWeightDouble > (100.0 + tolerance)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Total weight allocation (%.4f%%) exceeds maximum allowed (%.4f%%)", 
                    totalWeightDouble, 100.0 + tolerance))
                .addConstraintViolation();
            return false;
        }
        
        // Check for duplicate symbols
        Set<String> symbols = new HashSet<>();
        for (BasketConstituent constituent : constituents) {
            if (!symbols.add(constituent.getSymbol())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Duplicate symbol found: " + constituent.getSymbol())
                    .addConstraintViolation();
                return false;
            }
        }
        
        // Check individual weight limits
        for (BasketConstituent constituent : constituents) {
            if (constituent.getWeight() != null && constituent.getWeight().doubleValue() > 50.0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("Symbol %s weight (%.4f%%) exceeds maximum individual allocation (50.00%%)",
                        constituent.getSymbol(), constituent.getWeight().doubleValue()))
                    .addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }
    
    private BasketConstituent mapToBasketConstituent(CreateConstituentRequest request) {
        BasketConstituent constituent = new BasketConstituent();
        constituent.setSymbol(request.getSymbol());
        constituent.setWeight(request.getWeight());
        constituent.setShares(request.getShares());
        constituent.setSector(request.getSector());
        constituent.setCountry(request.getCountry());
        constituent.setCurrency(request.getCurrency());
        return constituent;
    }
}
```

### Date and Time Validators

```java
/**
 * Validates business date constraints
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BusinessDateValidator.class)
public @interface ValidBusinessDate {
    String message() default "Invalid business date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    boolean allowWeekends() default false;
    boolean allowHolidays() default false;
    String[] markets() default {"US"};
}

@Component
public class BusinessDateValidator implements ConstraintValidator<ValidBusinessDate, LocalDate> {
    
    @Autowired
    private MarketCalendarService marketCalendarService;
    
    private boolean allowWeekends;
    private boolean allowHolidays;
    private Set<String> markets;
    
    @Override
    public void initialize(ValidBusinessDate constraintAnnotation) {
        this.allowWeekends = constraintAnnotation.allowWeekends();
        this.allowHolidays = constraintAnnotation.allowHolidays();
        this.markets = Set.of(constraintAnnotation.markets());
    }
    
    @Override
    public boolean isValid(LocalDate date, ConstraintValidatorContext context) {
        if (date == null) {
            return true;
        }
        
        // Check weekends
        if (!allowWeekends && (date.getDayOfWeek() == DayOfWeek.SATURDAY || 
                              date.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Date " + date + " falls on a weekend and weekends are not allowed")
                .addConstraintViolation();
            return false;
        }
        
        // Check market holidays
        if (!allowHolidays) {
            for (String market : markets) {
                if (marketCalendarService.isHoliday(market, date)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                        "Date " + date + " is a holiday in " + market + " market")
                        .addConstraintViolation();
                    return false;
                }
            }
        }
        
        return true;
    }
}

/**
 * Validates date range constraints
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {
    String message() default "Invalid date range";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    String startField();
    String endField();
    long maxDaysRange() default Long.MAX_VALUE;
    boolean allowSameDate() default false;
}

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {
    
    private String startField;
    private String endField;
    private long maxDaysRange;
    private boolean allowSameDate;
    
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startField = constraintAnnotation.startField();
        this.endField = constraintAnnotation.endField();
        this.maxDaysRange = constraintAnnotation.maxDaysRange();
        this.allowSameDate = constraintAnnotation.allowSameDate();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        try {
            Field startDateField = value.getClass().getDeclaredField(startField);
            Field endDateField = value.getClass().getDeclaredField(endField);
            
            startDateField.setAccessible(true);
            endDateField.setAccessible(true);
            
            LocalDate startDate = (LocalDate) startDateField.get(value);
            LocalDate endDate = (LocalDate) endDateField.get(value);
            
            if (startDate == null || endDate == null) {
                return true; // Let other validators handle null values
            }
            
            // Check if end date is after start date
            if (endDate.isBefore(startDate)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "End date (" + endDate + ") must be after start date (" + startDate + ")")
                    .addConstraintViolation();
                return false;
            }
            
            // Check if same date is allowed
            if (!allowSameDate && endDate.equals(startDate)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "End date cannot be the same as start date")
                    .addConstraintViolation();
                return false;
            }
            
            // Check maximum range
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > maxDaysRange) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("Date range (%d days) exceeds maximum allowed (%d days)", 
                        daysBetween, maxDaysRange))
                    .addConstraintViolation();
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            throw new RuntimeException("Error validating date range", e);
        }
    }
}
```

## Error Response Contracts

### Standard Error Response Format

```java
/**
 * Standardized error response across all services
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @NotNull
    private boolean success = false;
    
    @NotBlank
    private String error;
    
    @NotBlank  
    private String message;
    
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    @NotBlank
    private String path;
    
    @Min(400)
    @Max(599)
    private int status;
    
    private String requestId;
    
    private String traceId;
    
    private List<ValidationError> details;
    
    private Map<String, Object> metadata;
    
    // For debugging (only in non-production)
    private String stackTrace;
    
    private String serviceVersion;
    
    private String correlationId;
    
    // getters, setters, builders
    
    public static ErrorResponse of(ErrorCode errorCode, String message, String path, int status) {
        return ErrorResponse.builder()
            .error(errorCode.getCode())
            .message(message)
            .path(path)
            .status(status)
            .timestamp(LocalDateTime.now())
            .requestId(RequestContextHolder.getCurrentRequestId())
            .traceId(TracingContextHolder.getCurrentTraceId())
            .serviceVersion(ApplicationInfoHolder.getVersion())
            .build();
    }
}

/**
 * Validation error details
 */
public class ValidationError {
    
    @NotBlank
    private String field;
    
    private Object rejectedValue;
    
    @NotBlank
    private String message;
    
    @NotBlank
    private String code;
    
    private String objectName;
    
    private Map<String, Object> metadata;
    
    // getters, setters, builders
    
    public static ValidationError of(String field, Object rejectedValue, String message, String code) {
        return ValidationError.builder()
            .field(field)
            .rejectedValue(rejectedValue)
            .message(message)
            .code(code)
            .build();
    }
}
```

### Error Code Enumeration

```java
/**
 * Standardized error codes across all services
 */
public enum ErrorCode {
    
    // Client Errors (4xx)
    BAD_REQUEST("BAD_REQUEST", "Invalid request syntax or parameters"),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication is required"),
    FORBIDDEN("FORBIDDEN", "Insufficient permissions for this operation"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource was not found"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "HTTP method not supported for this endpoint"),
    NOT_ACCEPTABLE("NOT_ACCEPTABLE", "Requested content type not supported"),
    CONFLICT("CONFLICT", "Request conflicts with current resource state"),
    GONE("GONE", "Resource is no longer available"),
    PRECONDITION_FAILED("PRECONDITION_FAILED", "Request precondition was not met"),
    PAYLOAD_TOO_LARGE("PAYLOAD_TOO_LARGE", "Request payload exceeds size limit"),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "Content type not supported"),
    UNPROCESSABLE_ENTITY("UNPROCESSABLE_ENTITY", "Request validation failed"),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", "Too many requests"),
    
    // Business Rule Violations (4xx)
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", "Business rule constraint violated"),
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", "Invalid state transition attempted"),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "Resource already exists"),
    DEPENDENT_RESOURCE_EXISTS("DEPENDENT_RESOURCE_EXISTS", "Cannot delete resource with dependencies"),
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "Insufficient account balance"),
    EXPIRED_RESOURCE("EXPIRED_RESOURCE", "Resource has expired"),
    SUSPENDED_RESOURCE("SUSPENDED_RESOURCE", "Resource is suspended"),
    
    // Validation Errors (422)
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed"),
    INVALID_FORMAT("INVALID_FORMAT", "Invalid data format"),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "Required field is missing"),
    INVALID_FIELD_VALUE("INVALID_FIELD_VALUE", "Field value is invalid"),
    FIELD_LENGTH_EXCEEDED("FIELD_LENGTH_EXCEEDED", "Field length exceeds maximum"),
    INVALID_DATE_RANGE("INVALID_DATE_RANGE", "Invalid date range specified"),
    INVALID_CURRENCY("INVALID_CURRENCY", "Invalid currency code"),
    INVALID_SYMBOL("INVALID_SYMBOL", "Invalid symbol"),
    INVALID_WEIGHT_ALLOCATION("INVALID_WEIGHT_ALLOCATION", "Invalid weight allocation"),
    
    // Server Errors (5xx)
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An unexpected error occurred"),
    NOT_IMPLEMENTED("NOT_IMPLEMENTED", "Feature not implemented"),
    BAD_GATEWAY("BAD_GATEWAY", "Invalid response from upstream service"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service temporarily unavailable"),
    GATEWAY_TIMEOUT("GATEWAY_TIMEOUT", "Upstream service timeout"),
    
    // External Service Errors (5xx)
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "External service error"),
    MARKET_DATA_UNAVAILABLE("MARKET_DATA_UNAVAILABLE", "Market data service unavailable"),
    VENDOR_API_ERROR("VENDOR_API_ERROR", "Vendor API error"),
    DATABASE_ERROR("DATABASE_ERROR", "Database operation failed"),
    CACHE_ERROR("CACHE_ERROR", "Cache operation failed"),
    MESSAGING_ERROR("MESSAGING_ERROR", "Messaging service error"),
    
    // Timeout Errors (5xx)
    REQUEST_TIMEOUT("REQUEST_TIMEOUT", "Request processing timeout"),
    DATABASE_TIMEOUT("DATABASE_TIMEOUT", "Database operation timeout"),
    EXTERNAL_API_TIMEOUT("EXTERNAL_API_TIMEOUT", "External API timeout"),
    CACHE_TIMEOUT("CACHE_TIMEOUT", "Cache operation timeout");
    
    private final String code;
    private final String defaultMessage;
    
    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
    
    public static ErrorCode fromHttpStatus(int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> CONFLICT;
            case 410 -> GONE;
            case 412 -> PRECONDITION_FAILED;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 422 -> VALIDATION_ERROR;
            case 429 -> RATE_LIMIT_EXCEEDED;
            case 500 -> INTERNAL_SERVER_ERROR;
            case 501 -> NOT_IMPLEMENTED;
            case 502 -> BAD_GATEWAY;
            case 503 -> SERVICE_UNAVAILABLE;
            case 504 -> GATEWAY_TIMEOUT;
            default -> INTERNAL_SERVER_ERROR;
        };
    }
}
```

## Global Exception Handler

```java
/**
 * Global exception handler for all REST controllers
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @Autowired
    private MessageSource messageSource;
    
    @Autowired
    private ApplicationProperties applicationProperties;
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        List<ValidationError> details = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> ValidationError.of(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage(),
                fieldError.getCode()
            ))
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.VALIDATION_ERROR,
            "Request validation failed",
            request.getRequestURI(),
            422
        );
        errorResponse.setDetails(details);
        
        log.warn("Validation error for request {}: {}", request.getRequestURI(), details);
        
        return ResponseEntity.status(422).body(errorResponse);
    }
    
    /**
     * Handle constraint violation errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        
        List<ValidationError> details = ex.getConstraintViolations()
            .stream()
            .map(violation -> ValidationError.of(
                violation.getPropertyPath().toString(),
                violation.getInvalidValue(),
                violation.getMessage(),
                violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
            ))
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.VALIDATION_ERROR,
            "Constraint validation failed",
            request.getRequestURI(),
            422
        );
        errorResponse.setDetails(details);
        
        return ResponseEntity.status(422).body(errorResponse);
    }
    
    /**
     * Handle business rule violations
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolationException(
            BusinessRuleViolationException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            409
        );
        
        if (ex.getMetadata() != null) {
            errorResponse.setMetadata(ex.getMetadata());
        }
        
        log.warn("Business rule violation for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.status(409).body(errorResponse);
    }
    
    /**
     * Handle resource not found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.RESOURCE_NOT_FOUND,
            ex.getMessage(),
            request.getRequestURI(),
            404
        );
        
        return ResponseEntity.status(404).body(errorResponse);
    }
    
    /**
     * Handle external service errors
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex,
            HttpServletRequest request) {
        
        ErrorCode errorCode = mapExternalServiceError(ex.getServiceName(), ex.getErrorType());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            errorCode,
            ex.getMessage(),
            request.getRequestURI(),
            503
        );
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("serviceName", ex.getServiceName());
        metadata.put("errorType", ex.getErrorType());
        metadata.put("retryable", ex.isRetryable());
        errorResponse.setMetadata(metadata);
        
        log.error("External service error for request {}: service={}, error={}", 
            request.getRequestURI(), ex.getServiceName(), ex.getErrorType(), ex);
        
        return ResponseEntity.status(503).body(errorResponse);
    }
    
    /**
     * Handle timeout exceptions
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(
            TimeoutException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.REQUEST_TIMEOUT,
            "Request processing timeout: " + ex.getMessage(),
            request.getRequestURI(),
            504
        );
        
        log.error("Timeout error for request {}: {}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.status(504).body(errorResponse);
    }
    
    /**
     * Handle WebFlux errors
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {
        
        ErrorCode errorCode = ErrorCode.fromHttpStatus(ex.getStatus().value());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            errorCode,
            ex.getReason() != null ? ex.getReason() : errorCode.getDefaultMessage(),
            request.getRequestURI(),
            ex.getStatus().value()
        );
        
        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            request.getRequestURI(),
            500
        );
        
        // Include stack trace in development mode
        if (applicationProperties.isDebugMode()) {
            errorResponse.setStackTrace(getStackTrace(ex));
        }
        
        log.error("Unexpected error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        return ResponseEntity.status(500).body(errorResponse);
    }
    
    private ErrorCode mapExternalServiceError(String serviceName, String errorType) {
        return switch (serviceName.toLowerCase()) {
            case "market-data", "bloomberg", "refinitiv" -> ErrorCode.MARKET_DATA_UNAVAILABLE;
            case "database", "postgres", "timescale" -> ErrorCode.DATABASE_ERROR;
            case "cache", "redis", "solcache" -> ErrorCode.CACHE_ERROR;
            case "messaging", "kafka", "solace" -> ErrorCode.MESSAGING_ERROR;
            default -> ErrorCode.EXTERNAL_SERVICE_ERROR;
        };
    }
    
    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}
```

## Custom Business Exceptions

```java
/**
 * Base business exception class
 */
public abstract class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;
    
    protected BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }
    
    protected BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null);
    }
    
    protected BusinessException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> metadata) {
        super(message, cause);
        this.errorCode = errorCode;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}

/**
 * Business rule violation exception
 */
public class BusinessRuleViolationException extends BusinessException {
    
    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
    
    public BusinessRuleViolationException(String message, Map<String, Object> metadata) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, null, metadata);
    }
    
    public static BusinessRuleViolationException invalidStateTransition(String currentState, String requestedState, String basketCode) {
        Map<String, Object> metadata = Map.of(
            "basketCode", basketCode,
            "currentState", currentState,
            "requestedState", requestedState
        );
        return new BusinessRuleViolationException(
            String.format("Cannot transition basket %s from %s to %s", basketCode, currentState, requestedState),
            metadata
        );
    }
    
    public static BusinessRuleViolationException basketAlreadyExists(String basketCode) {
        Map<String, Object> metadata = Map.of("basketCode", basketCode);
        return new BusinessRuleViolationException(
            "Basket with code '" + basketCode + "' already exists",
            metadata
        );
    }
    
    public static BusinessRuleViolationException insufficientPermissions(String operation, String resource, String userId) {
        Map<String, Object> metadata = Map.of(
            "operation", operation,
            "resource", resource,
            "userId", userId
        );
        return new BusinessRuleViolationException(
            String.format("User %s does not have permission to %s on %s", userId, operation, resource),
            metadata
        );
    }
}

/**
 * Resource not found exception
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, 
            String.format("%s with id '%s' not found", resourceType, resourceId),
            null,
            Map.of("resourceType", resourceType, "resourceId", resourceId));
    }
    
    public static ResourceNotFoundException basket(String basketCode) {
        return new ResourceNotFoundException("Basket", basketCode);
    }
    
    public static ResourceNotFoundException approval(String approvalId) {
        return new ResourceNotFoundException("Approval", approvalId);
    }
    
    public static ResourceNotFoundException backtest(String backtestId) {
        return new ResourceNotFoundException("Backtest", backtestId);
    }
}

/**
 * External service exception
 */
public class ExternalServiceException extends BusinessException {
    
    private final String serviceName;
    private final String errorType;
    private final boolean retryable;
    
    public ExternalServiceException(String serviceName, String errorType, String message, boolean retryable) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message);
        this.serviceName = serviceName;
        this.errorType = errorType;
        this.retryable = retryable;
        
        addMetadata("serviceName", serviceName);
        addMetadata("errorType", errorType);
        addMetadata("retryable", retryable);
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    public static ExternalServiceException marketDataUnavailable(String symbol, String vendor) {
        return new ExternalServiceException(
            vendor,
            "DATA_UNAVAILABLE",
            "Market data for symbol '" + symbol + "' is not available from " + vendor,
            true
        );
    }
    
    public static ExternalServiceException vendorApiError(String vendor, String operation, String error) {
        return new ExternalServiceException(
            vendor,
            "API_ERROR",
            "Error performing " + operation + " on " + vendor + ": " + error,
            true
        );
    }
}
```

## Reactive Error Handling

```java
/**
 * Reactive error handling utilities
 */
@Component
public class ReactiveErrorHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ReactiveErrorHandler.class);
    
    /**
     * Handle errors in reactive streams with fallback
     */
    public static <T> Function<Throwable, Mono<T>> handleWithFallback(T fallbackValue) {
        return error -> {
            log.warn("Error in reactive stream, using fallback: {}", error.getMessage());
            return Mono.just(fallbackValue);
        };
    }
    
    /**
     * Handle errors with retry logic
     */
    public static <T> Function<Flux<T>, Flux<T>> retryWithBackoff(int maxRetries, Duration initialDelay) {
        return flux -> flux
            .retryWhen(Retry.backoff(maxRetries, initialDelay)
                .filter(throwable -> isRetryableError(throwable))
                .doBeforeRetry(retrySignal -> 
                    log.warn("Retrying operation, attempt {}: {}", 
                        retrySignal.totalRetries() + 1, 
                        retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> 
                    new ExternalServiceException(
                        "RetryService",
                        "MAX_RETRIES_EXCEEDED",
                        "Max retries exceeded: " + retrySignal.failure().getMessage(),
                        false
                    )));
    }
    
    /**
     * Handle timeout errors
     */
    public static <T> Function<Throwable, Mono<T>> handleTimeout(String operation) {
        return error -> {
            if (error instanceof TimeoutException) {
                log.error("Timeout in operation: {}", operation);
                return Mono.error(new ExternalServiceException(
                    "TimeoutService",
                    "OPERATION_TIMEOUT",
                    "Operation '" + operation + "' timed out",
                    true
                ));
            }
            return Mono.error(error);
        };
    }
    
    /**
     * Circuit breaker error handling
     */
    public static <T> Function<Throwable, Mono<T>> handleCircuitBreakerOpen(String serviceName) {
        return error -> {
            if (error instanceof CallNotPermittedException) {
                log.warn("Circuit breaker open for service: {}", serviceName);
                return Mono.error(new ExternalServiceException(
                    serviceName,
                    "CIRCUIT_BREAKER_OPEN",
                    "Service " + serviceName + " is temporarily unavailable",
                    false
                ));
            }
            return Mono.error(error);
        };
    }
    
    private static boolean isRetryableError(Throwable throwable) {
        return throwable instanceof ConnectException ||
               throwable instanceof TimeoutException ||
               throwable instanceof HttpServerErrorException ||
               (throwable instanceof ExternalServiceException && 
                ((ExternalServiceException) throwable).isRetryable());
    }
}
```

## Validation Groups

```java
/**
 * Validation groups for different contexts
 */
public interface ValidationGroups {
    
    interface Create {}
    interface Update {}
    interface Approve {}
    interface Submit {}
    interface Publish {}
    
    interface Draft {}
    interface Production {}
    
    interface BasicValidation {}
    interface AdvancedValidation {}
    interface BusinessRuleValidation {}
}

/**
 * Example usage with validation groups
 */
@ValidWeightAllocation(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
public class CreateBasketRequest {
    
    @NotBlank(groups = ValidationGroups.BasicValidation.class)
    @ValidBasketCode(checkAvailability = true, groups = ValidationGroups.Create.class)
    private String basketCode;
    
    @NotBlank(groups = ValidationGroups.BasicValidation.class)
    @Size(min = 3, max = 255, groups = ValidationGroups.BasicValidation.class)
    private String basketName;
    
    @Valid
    @NotEmpty(groups = ValidationGroups.BasicValidation.class)
    @Size(max = 100, groups = ValidationGroups.AdvancedValidation.class)
    private List<CreateConstituentRequest> constituents;
    
    // getters, setters
}

/**
 * Controller method with validation groups
 */
@PostMapping("/api/baskets")
@Validated({ValidationGroups.Create.class, ValidationGroups.BasicValidation.class, ValidationGroups.AdvancedValidation.class})
public Mono<ResponseEntity<ApiResponse<Basket>>> createBasket(
    @Valid @RequestBody CreateBasketRequest request,
    @RequestHeader("X-User-ID") String userId) {
    
    return basketService.createBasket(request, userId)
        .map(basket -> ResponseEntity.status(201)
            .body(ApiResponse.success(basket, "Basket created successfully")));
}
```

This comprehensive validation and error handling contract ensures:

✅ **Consistent Validation**: Uniform validation rules across all services  
✅ **Business Rule Enforcement**: Domain-specific validators for complex constraints  
✅ **Standardized Errors**: Uniform error response format and error codes  
✅ **Reactive Error Handling**: Proper error handling in reactive streams  
✅ **Comprehensive Metadata**: Rich error information for debugging  
✅ **Internationalization**: Support for localized error messages  
✅ **Validation Groups**: Context-specific validation rules  
✅ **Circuit Breaker Integration**: Resilience patterns for external services
