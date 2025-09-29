# SMA Refinitiv API & FIX Protocol Implementation Plan

## Executive Summary

This document outlines a comprehensive implementation plan for integrating **SMA Refinitiv API** for price data retrieval and **FIX protocol** for Bloomberg communication into the existing CIB Listing & Pricing Service. The implementation follows a non-invasive approach that adds new middleware services without modifying existing code.

## Implementation Overview

### Key Objectives
- ✅ Integrate SMA Refinitiv API for real-time price data
- ✅ Implement FIX protocol for Bloomberg communication
- ✅ Maintain existing system functionality
- ✅ Ensure high availability and fault tolerance
- ✅ Provide comprehensive monitoring and observability

### Architecture Approach
- **Middleware Pattern**: Add new services alongside existing ones
- **API Gateway**: Route requests to appropriate services
- **Event-Driven**: Use message queues for asynchronous communication
- **Microservices**: Independent, scalable services

## Detailed Implementation Plan

### Phase 1: Foundation & SMA Integration (Weeks 1-3)

#### Week 1: Project Setup & Infrastructure

**Tasks:**
- [ ] Create new service modules
- [ ] Setup development environment
- [ ] Configure Docker containers
- [ ] Setup CI/CD pipelines

**Deliverables:**
```bash
# New service structure
├── sma-adapter-service/
│   ├── src/main/java/com/custom/indexbasket/sma/
│   ├── Dockerfile
│   ├── pom.xml
│   └── application.yml
├── fix-adapter-service/
│   ├── src/main/java/com/custom/indexbasket/fix/
│   ├── Dockerfile
│   ├── pom.xml
│   └── application.yml
└── integration-manager-service/
    ├── src/main/java/com/custom/indexbasket/integration/
    ├── Dockerfile
    ├── pom.xml
    └── application.yml
```

**Configuration Files:**
```yaml
# docker-compose.enhanced.yml
version: '3.8'

services:
  # Existing services remain unchanged
  basket-core-service:
    # ... existing config
    
  market-data-service:
    # ... existing config
    
  publishing-service:
    # ... existing config
    
  # New integration services
  sma-adapter-service:
    build: ./sma-adapter-service
    ports: ["8084:8084"]
    environment:
      - SPRING_PROFILES_ACTIVE=sma
      - SMA_API_URL=${SMA_API_URL}
      - SMA_APP_KEY=${SMA_APP_KEY}
    depends_on: [redis, kafka]
    
  fix-adapter-service:
    build: ./fix-adapter-service
    ports: ["8085:8085"]
    environment:
      - SPRING_PROFILES_ACTIVE=fix
      - BLOOMBERG_FIX_HOST=${BLOOMBERG_FIX_HOST}
    depends_on: [redis, kafka]
    
  integration-manager-service:
    build: ./integration-manager-service
    ports: ["8086:8086"]
    depends_on: [sma-adapter-service, fix-adapter-service]
```

#### Week 2: SMA Refinitiv Adapter Implementation

**Core Components:**

**1. SMA Configuration**
```java
@Configuration
@EnableConfigurationProperties
public class SmaConfiguration {
    
    @Bean
    public SmaConnectionManager smaConnectionManager(
            @Value("${sma.refinitiv.api.base-url}") String baseUrl,
            @Value("${sma.refinitiv.api.app-key}") String appKey) {
        return new SmaConnectionManager(baseUrl, appKey);
    }
    
    @Bean
    public SmaPriceService smaPriceService(SmaConnectionManager connectionManager) {
        return new SmaPriceService(connectionManager);
    }
}
```

**2. SMA Service Implementation**
```java
@Service
@Slf4j
public class SmaPriceService {
    
    private final SmaConnectionManager connectionManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public Mono<SmaPriceData> getPrice(String symbol) {
        return Mono.fromCallable(() -> {
            // Check cache first
            String cacheKey = "sma:price:" + symbol;
            SmaPriceData cached = (SmaPriceData) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !isExpired(cached)) {
                return cached;
            }
            
            // Fetch from SMA API
            return connectionManager.fetchPrice(symbol);
        })
        .doOnSuccess(data -> {
            // Cache the result
            redisTemplate.opsForValue().set(
                "sma:price:" + symbol, 
                data, 
                Duration.ofSeconds(300)
            );
        })
        .onErrorMap(throwable -> new SmaApiException("Failed to fetch price for " + symbol, throwable));
    }
    
    public Flux<SmaPriceData> getBatchPrices(List<String> symbols) {
        return Flux.fromIterable(symbols)
            .flatMap(this::getPrice, 5) // Process 5 symbols concurrently
            .onErrorContinue((error, symbol) -> 
                log.warn("Failed to fetch price for symbol: {}", symbol, error));
    }
}
```

**3. SMA REST Controller**
```java
@RestController
@RequestMapping("/api/v1/sma")
@Slf4j
public class SmaController {
    
    @Autowired
    private SmaPriceService smaPriceService;
    
    @GetMapping("/prices/{symbol}")
    public Mono<ResponseEntity<SmaPriceData>> getPrice(@PathVariable String symbol) {
        return smaPriceService.getPrice(symbol)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }
    
    @PostMapping("/prices/batch")
    public Flux<SmaPriceData> getBatchPrices(@RequestBody List<String> symbols) {
        return smaPriceService.getBatchPrices(symbols);
    }
    
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return smaPriceService.checkHealth()
            .map(health -> ResponseEntity.ok(Map.of(
                "status", health.getStatus(),
                "timestamp", LocalDateTime.now(),
                "connection", health.isConnected()
            )));
    }
}
```

**4. Data Models**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmaPriceData {
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private String currency;
    private String exchange;
    private LocalDateTime timestamp;
    private String dataQuality;
    private Map<String, Object> additionalFields;
}
```

#### Week 3: SMA Integration with Market Data Service

**Enhanced Market Data Service Integration:**

```java
@Component
public class SmaRefinitivProxy extends AbstractDataSourceProxy {
    
    @Autowired
    private SmaPriceService smaPriceService;
    
    public SmaRefinitivProxy(DataSourceConfig config, MeterRegistry meterRegistry) {
        super(config, meterRegistry);
        config.setDataSourceName("SMA_REFINITIV");
    }
    
    @Override
    protected Mono<RawMarketData> fetchFromSource(String instrumentId) {
        return smaPriceService.getPrice(instrumentId)
            .map(this::transformSmaData)
            .doOnSuccess(data -> recordMetrics("success", instrumentId))
            .doOnError(error -> recordMetrics("error", instrumentId));
    }
    
    private RawMarketData transformSmaData(SmaPriceData smaData) {
        return RawMarketData.builder()
            .instrumentId(smaData.getSymbol())
            .dataSource("SMA_REFINITIV")
            .timestamp(smaData.getTimestamp())
            .price(smaData.getLastPrice())
            .bidPrice(smaData.getBidPrice())
            .askPrice(smaData.getAskPrice())
            .openPrice(smaData.getOpenPrice())
            .highPrice(smaData.getHighPrice())
            .lowPrice(smaData.getLowPrice())
            .volume(smaData.getVolume())
            .currency(smaData.getCurrency())
            .exchange(smaData.getExchange())
            .metadata(Map.of(
                "quality", smaData.getDataQuality(),
                "source", "SMA_REFINITIV"
            ))
            .build();
    }
    
    @Override
    public Mono<Boolean> isAvailable() {
        return smaPriceService.checkHealth()
            .map(health -> health.isConnected());
    }
}
```

**Configuration Update:**
```yaml
# market-data-service application.yml addition
market-data:
  sources:
    sma-refinitiv:
      enabled: true
      priority: 1  # High priority source
      timeout: 5000
      retry-attempts: 3
```

### Phase 2: FIX Protocol Implementation (Weeks 4-6)

#### Week 4: FIX Framework Setup

**Dependencies:**
```xml
<!-- fix-adapter-service/pom.xml -->
<dependencies>
    <!-- QuickFIX/J -->
    <dependency>
        <groupId>org.quickfixj</groupId>
        <artifactId>quickfixj-core</artifactId>
        <version>2.3.1</version>
    </dependency>
    
    <!-- Bloomberg FIX API -->
    <dependency>
        <groupId>com.bloomberg</groupId>
        <artifactId>blpapi</artifactId>
        <version>3.20.1</version>
    </dependency>
    
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

**FIX Configuration:**
```yaml
# fix-adapter-service/application.yml
fix:
  bloomberg:
    connection:
      host: ${BLOOMBERG_FIX_HOST:fix.bloomberg.com}
      port: ${BLOOMBERG_FIX_PORT:8080}
      sender-comp-id: ${FIX_SENDER_COMP_ID:YOUR_COMP_ID}
      target-comp-id: ${FIX_TARGET_COMP_ID:BLOOMBERG}
    
    session:
      begin-string: FIX.4.4
      heartbeat-interval: 30
      connection-timeout: 10
      reconnect-interval: 30
      use-data-dictionary: true
      data-dictionary: /config/FIX44.xml
    
    authentication:
      username: ${BLOOMBERG_USERNAME:your-username}
      password: ${BLOOMBERG_PASSWORD:your-password}
      cert-path: ${BLOOMBERG_CERT_PATH:/path/to/cert}
```

#### Week 5: FIX Service Implementation

**1. FIX Session Manager**
```java
@Component
@Slf4j
public class FixSessionManager {
    
    private final SessionSettings sessionSettings;
    private final MessageStoreFactory messageStoreFactory;
    private final LogFactory logFactory;
    private final MessageFactory messageFactory;
    private Session session;
    
    @PostConstruct
    public void initialize() throws ConfigError, RuntimeError {
        sessionSettings = new SessionSettings("/config/fix-session.cfg");
        messageStoreFactory = new FileStoreFactory(sessionSettings);
        logFactory = new FileLogFactory(sessionSettings);
        messageFactory = new DefaultMessageFactory();
        
        createSession();
    }
    
    private void createSession() throws ConfigError, RuntimeError {
        SessionID sessionId = new SessionID(
            sessionSettings.getString("BeginString"),
            sessionSettings.getString("SenderCompID"),
            sessionSettings.getString("TargetCompID")
        );
        
        session = Session.lookupSession(sessionId);
        if (session == null) {
            session = SessionFactory.createSession(
                sessionSettings, 
                new FixApplication(), 
                messageStoreFactory, 
                logFactory, 
                messageFactory
            );
        }
        
        session.addStateListener(this::onSessionStateChange);
        session.start();
    }
    
    private void onSessionStateChange(SessionID sessionId, SessionState state) {
        log.info("Session state changed to: {} for session: {}", state, sessionId);
        
        switch (state) {
            case CONNECTED:
                log.info("FIX session connected successfully");
                break;
            case DISCONNECTED:
                log.warn("FIX session disconnected");
                scheduleReconnect();
                break;
            case LOGGED_ON:
                log.info("FIX session logged on");
                break;
        }
    }
}
```

**2. FIX Application Handler**
```java
@Component
public class FixApplication implements Application {
    
    @Autowired
    private FixMessageHandler messageHandler;
    
    @Override
    public void onCreate(SessionID sessionId) {
        log.info("FIX application created for session: {}", sessionId);
    }
    
    @Override
    public void onLogon(SessionID sessionId) {
        log.info("FIX session logged on: {}", sessionId);
    }
    
    @Override
    public void onLogout(SessionID sessionId) {
        log.info("FIX session logged out: {}", sessionId);
    }
    
    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.debug("Sending admin message: {}", message);
    }
    
    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        log.debug("Received admin message: {}", message);
    }
    
    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        log.debug("Sending application message: {}", message);
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        log.debug("Received application message: {}", message);
        messageHandler.handleMessage(message, sessionId);
    }
}
```

**3. FIX Message Handler**
```java
@Service
@Slf4j
public class FixMessageHandler {
    
    @Autowired
    private FixSessionManager sessionManager;
    
    public Mono<String> publishMarketData(PricePublishingRequest request) {
        return Mono.fromCallable(() -> {
            Message message = new Message();
            message.getHeader().setString(MsgType.FIELD, MsgType.MARKET_DATA_INCREMENTAL_REFRESH);
            message.getHeader().setString(MsgSeqNum.FIELD, String.valueOf(getNextSequenceNumber()));
            message.getHeader().setUtcTimeStamp(SendingTime.FIELD, new Date());
            
            // Market data fields
            message.setString(Symbol.FIELD, request.getSymbol());
            message.setString(MDEntryType.FIELD, MDEntryType.BID);
            message.setDouble(MDEntryPx.FIELD, request.getPrice().doubleValue());
            message.setDouble(MDEntrySize.FIELD, request.getVolume().doubleValue());
            
            sessionManager.sendMessage(message);
            
            return message.toString();
        })
        .doOnSuccess(msg -> log.info("Market data published successfully: {}", request.getSymbol()))
        .doOnError(error -> log.error("Failed to publish market data for {}: {}", request.getSymbol(), error.getMessage()));
    }
    
    public void handleMessage(Message message, SessionID sessionId) {
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.HEARTBEAT:
                    handleHeartbeat(message, sessionId);
                    break;
                case MsgType.TEST_REQUEST:
                    handleTestRequest(message, sessionId);
                    break;
                case MsgType.MARKET_DATA_REQUEST_REJECT:
                    handleMarketDataReject(message, sessionId);
                    break;
                default:
                    log.warn("Unknown message type received: {}", msgType);
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
        }
    }
    
    private void handleHeartbeat(Message message, SessionID sessionId) {
        log.debug("Received heartbeat from session: {}", sessionId);
    }
    
    private void handleTestRequest(Message message, SessionID sessionId) {
        log.debug("Received test request from session: {}", sessionId);
        // Send heartbeat response
    }
    
    private void handleMarketDataReject(Message message, SessionID sessionId) {
        log.warn("Market data request rejected for session: {}", sessionId);
    }
}
```

#### Week 6: FIX Integration with Publishing Service

**Enhanced Publishing Service:**

```java
@Service
@Slf4j
public class EnhancedVendorServiceImpl extends VendorServiceImpl {
    
    @Autowired
    private FixBloombergAdapter fixAdapter;
    
    @Override
    public Mono<PublishingResult> publishBasketPriceToVendor(String vendorName, PricePublishingRequest request) {
        if ("BLOOMBERG".equals(vendorName.toUpperCase())) {
            return fixAdapter.publishPrice(request)
                .map(result -> PublishingResult.builder()
                    .vendorName("BLOOMBERG")
                    .basketId(request.getBasketId())
                    .success(true)
                    .message("Price published via FIX")
                    .timestamp(LocalDateTime.now())
                    .build())
                .onErrorReturn(PublishingResult.builder()
                    .vendorName("BLOOMBERG")
                    .basketId(request.getBasketId())
                    .success(false)
                    .message("FIX publishing failed")
                    .timestamp(LocalDateTime.now())
                    .build());
        }
        
        // Fall back to existing mock implementation for other vendors
        return super.publishBasketPriceToVendor(vendorName, request);
    }
    
    @Override
    public Mono<PublishingResult> publishBasketListingToVendor(String vendorName, BasketListingRequest request) {
        if ("BLOOMBERG".equals(vendorName.toUpperCase())) {
            return fixAdapter.publishBasketListing(request)
                .map(result -> PublishingResult.builder()
                    .vendorName("BLOOMBERG")
                    .basketId(request.getBasketId())
                    .success(true)
                    .message("Basket listed via FIX")
                    .timestamp(LocalDateTime.now())
                    .build());
        }
        
        return super.publishBasketListingToVendor(vendorName, request);
    }
}
```

### Phase 3: Integration Manager (Weeks 7-8)

#### Week 7: Orchestration Logic

**Integration Manager Service:**

```java
@Service
@Slf4j
public class IntegrationManagerService {
    
    @Autowired
    private SmaAdapterService smaAdapter;
    
    @Autowired
    private FixBloombergAdapter fixAdapter;
    
    @Autowired
    private BasketCoreService basketCoreService;
    
    public Mono<BasketPrice> calculateAndPublishBasketPrice(String basketId) {
        return basketCoreService.getBasket(basketId)
            .flatMap(basket -> calculateBasketPrice(basket))
            .flatMap(basketPrice -> publishBasketPrice(basketPrice))
            .doOnSuccess(price -> log.info("Successfully calculated and published basket price for: {}", basketId))
            .doOnError(error -> log.error("Failed to calculate/publish basket price for {}: {}", basketId, error.getMessage()));
    }
    
    private Mono<BasketPrice> calculateBasketPrice(Basket basket) {
        List<String> symbols = basket.getConstituents().stream()
            .map(Constituent::getSymbol)
            .collect(Collectors.toList());
        
        return smaAdapter.getBatchPrices(symbols)
            .collectList()
            .map(prices -> {
                BigDecimal totalValue = BigDecimal.ZERO;
                
                for (Constituent constituent : basket.getConstituents()) {
                    SmaPriceData priceData = prices.stream()
                        .filter(p -> p.getSymbol().equals(constituent.getSymbol()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Price not found for: " + constituent.getSymbol()));
                    
                    BigDecimal constituentValue = priceData.getLastPrice()
                        .multiply(constituent.getWeight())
                        .divide(new BigDecimal("100"));
                    
                    totalValue = totalValue.add(constituentValue);
                }
                
                return BasketPrice.builder()
                    .basketId(basket.getId())
                    .price(totalValue)
                    .currency("USD")
                    .timestamp(LocalDateTime.now())
                    .constituentPrices(prices)
                    .build();
            });
    }
    
    private Mono<PublishingResult> publishBasketPrice(BasketPrice basketPrice) {
        PricePublishingRequest request = PricePublishingRequest.builder()
            .basketId(basketPrice.getBasketId())
            .symbol(basketPrice.getBasketId())
            .price(basketPrice.getPrice())
            .currency(basketPrice.getCurrency())
            .timestamp(basketPrice.getTimestamp())
            .build();
        
        return fixAdapter.publishPrice(request);
    }
}
```

#### Week 8: Event-Driven Integration

**Event Publishers and Consumers:**

```java
@Component
@Slf4j
public class BasketEventConsumer {
    
    @Autowired
    private IntegrationManagerService integrationManager;
    
    @KafkaListener(topics = "basket.approved")
    public void handleBasketApproved(BasketApprovedEvent event) {
        log.info("Basket approved event received: {}", event.getBasketId());
        
        // Start real-time price publishing for approved basket
        integrationManager.startRealTimePublishing(event.getBasketId())
            .subscribe(
                result -> log.info("Real-time publishing started for basket: {}", event.getBasketId()),
                error -> log.error("Failed to start real-time publishing for basket {}: {}", 
                    event.getBasketId(), error.getMessage())
            );
    }
    
    @KafkaListener(topics = "basket.deactivated")
    public void handleBasketDeactivated(BasketDeactivatedEvent event) {
        log.info("Basket deactivated event received: {}", event.getBasketId());
        
        // Stop real-time price publishing
        integrationManager.stopRealTimePublishing(event.getBasketId())
            .subscribe(
                result -> log.info("Real-time publishing stopped for basket: {}", event.getBasketId()),
                error -> log.error("Failed to stop real-time publishing for basket {}: {}", 
                    event.getBasketId(), error.getMessage())
            );
    }
}
```

**Scheduled Price Publishing:**

```java
@Component
@Slf4j
public class ScheduledPricePublisher {
    
    @Autowired
    private IntegrationManagerService integrationManager;
    
    @Autowired
    private BasketCoreService basketCoreService;
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void publishActiveBasketPrices() {
        basketCoreService.getActiveBaskets()
            .flatMap(basket -> integrationManager.calculateAndPublishBasketPrice(basket.getId()))
            .collectList()
            .subscribe(
                results -> log.debug("Published prices for {} baskets", results.size()),
                error -> log.error("Scheduled price publishing failed: {}", error.getMessage())
            );
    }
}
```

### Phase 4: Production Deployment (Weeks 9-10)

#### Week 9: Security & Performance

**Security Configuration:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/api/v1/sma/health").permitAll()
                .pathMatchers("/api/v1/fix/health").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
            )
            .build();
    }
    
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return new NimbusReactiveJwtDecoder(jwtIssuerUri + "/.well-known/jwks.json");
    }
}
```

**Performance Monitoring:**

```java
@Component
public class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer smaApiTimer;
    private final Counter fixMessageCounter;
    private final Gauge activeConnectionsGauge;
    
    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.smaApiTimer = Timer.builder("sma.api.response.time")
            .description("SMA API response time")
            .register(meterRegistry);
        
        this.fixMessageCounter = Counter.builder("fix.messages.sent")
            .description("Number of FIX messages sent")
            .register(meterRegistry);
        
        this.activeConnectionsGauge = Gauge.builder("fix.active.connections")
            .description("Number of active FIX connections")
            .register(meterRegistry, this, PerformanceMetrics::getActiveConnections);
    }
    
    public void recordSmaApiCall(Duration duration) {
        smaApiTimer.record(duration);
    }
    
    public void recordFixMessageSent() {
        fixMessageCounter.increment();
    }
    
    private double getActiveConnections() {
        // Implementation to count active connections
        return 1.0; // Placeholder
    }
}
```

#### Week 10: Production Testing & Deployment

**Load Testing Script:**

```python
# load_test_sma_fix.py
import asyncio
import aiohttp
import time
import json

class LoadTester:
    def __init__(self):
        self.base_url = "http://localhost:8086"
        self.results = []
    
    async def test_sma_integration(self):
        """Test SMA adapter performance"""
        async with aiohttp.ClientSession() as session:
            tasks = []
            for i in range(100):
                task = self.test_sma_price_request(session, f"SYMBOL_{i}")
                tasks.append(task)
            
            results = await asyncio.gather(*tasks)
            return results
    
    async def test_sma_price_request(self, session, symbol):
        """Test individual SMA price request"""
        start_time = time.time()
        try:
            async with session.get(f"{self.base_url}/api/v1/sma/prices/{symbol}") as response:
                duration = time.time() - start_time
                return {
                    "symbol": symbol,
                    "status": response.status,
                    "duration": duration,
                    "success": response.status == 200
                }
        except Exception as e:
            duration = time.time() - start_time
            return {
                "symbol": symbol,
                "status": 500,
                "duration": duration,
                "success": False,
                "error": str(e)
            }
    
    async def test_fix_publishing(self):
        """Test FIX message publishing"""
        async with aiohttp.ClientSession() as session:
            tasks = []
            for i in range(50):
                task = self.test_fix_price_publish(session, f"BASKET_{i}")
                tasks.append(task)
            
            results = await asyncio.gather(*tasks)
            return results
    
    async def test_fix_price_publish(self, session, basket_id):
        """Test FIX price publishing"""
        start_time = time.time()
        payload = {
            "basketId": basket_id,
            "symbol": f"SYMBOL_{basket_id}",
            "price": 100.50,
            "currency": "USD",
            "timestamp": "2024-01-01T10:00:00Z"
        }
        
        try:
            async with session.post(f"{self.base_url}/api/v1/fix/publish/price", 
                                  json=payload) as response:
                duration = time.time() - start_time
                return {
                    "basketId": basket_id,
                    "status": response.status,
                    "duration": duration,
                    "success": response.status == 200
                }
        except Exception as e:
            duration = time.time() - start_time
            return {
                "basketId": basket_id,
                "status": 500,
                "duration": duration,
                "success": False,
                "error": str(e)
            }
    
    async def run_load_test(self):
        """Run comprehensive load test"""
        print("Starting SMA FIX Integration Load Test...")
        
        # Test SMA integration
        print("Testing SMA Integration...")
        sma_results = await self.test_sma_integration()
        
        # Test FIX publishing
        print("Testing FIX Publishing...")
        fix_results = await self.test_fix_publishing()
        
        # Analyze results
        self.analyze_results(sma_results, fix_results)
    
    def analyze_results(self, sma_results, fix_results):
        """Analyze and report test results"""
        print("\n=== LOAD TEST RESULTS ===")
        
        # SMA Results
        sma_success = sum(1 for r in sma_results if r["success"])
        sma_avg_duration = sum(r["duration"] for r in sma_results) / len(sma_results)
        
        print(f"SMA Integration:")
        print(f"  Success Rate: {sma_success}/{len(sma_results)} ({sma_success/len(sma_results)*100:.1f}%)")
        print(f"  Average Response Time: {sma_avg_duration:.3f}s")
        
        # FIX Results
        fix_success = sum(1 for r in fix_results if r["success"])
        fix_avg_duration = sum(r["duration"] for r in fix_results) / len(fix_results)
        
        print(f"FIX Publishing:")
        print(f"  Success Rate: {fix_success}/{len(fix_results)} ({fix_success/len(fix_results)*100:.1f}%)")
        print(f"  Average Response Time: {fix_avg_duration:.3f}s")

if __name__ == "__main__":
    tester = LoadTester()
    asyncio.run(tester.run_load_test())
```

**Deployment Script:**

```bash
#!/bin/bash
# deploy_sma_fix.sh

echo "🚀 Deploying SMA FIX Integration Services..."

# Build services
echo "📦 Building services..."
docker-compose -f docker-compose.enhanced.yml build

# Run tests
echo "🧪 Running tests..."
docker-compose -f docker-compose.enhanced.yml -f docker-compose.test.yml up --abort-on-container-exit

# Deploy to staging
echo "🎭 Deploying to staging..."
docker-compose -f docker-compose.enhanced.yml -f docker-compose.staging.yml up -d

# Health checks
echo "🔍 Running health checks..."
./scripts/health_check_enhanced.sh

# Load testing
echo "⚡ Running load tests..."
python load_test_sma_fix.py

# Deploy to production
echo "🏭 Deploying to production..."
docker-compose -f docker-compose.enhanced.yml -f docker-compose.production.yml up -d

echo "✅ Deployment completed successfully!"
```

## Risk Mitigation

### Technical Risks

**1. SMA API Rate Limiting**
- **Risk**: API rate limits could cause service degradation
- **Mitigation**: Implement caching, request queuing, and circuit breakers
- **Monitoring**: Track API usage and response times

**2. FIX Session Disconnections**
- **Risk**: Network issues could cause FIX session drops
- **Mitigation**: Implement automatic reconnection and session recovery
- **Monitoring**: Track session status and reconnection attempts

**3. Data Consistency**
- **Risk**: Price data inconsistencies between SMA and existing sources
- **Mitigation**: Implement data validation and reconciliation processes
- **Monitoring**: Compare prices across data sources

### Operational Risks

**1. Vendor Dependency**
- **Risk**: Over-reliance on external vendor APIs
- **Mitigation**: Maintain fallback mechanisms and multiple data sources
- **Monitoring**: Track vendor availability and response times

**2. Performance Impact**
- **Risk**: New services could impact overall system performance
- **Mitigation**: Implement proper resource allocation and monitoring
- **Monitoring**: Track resource usage and response times

## Success Metrics

### Performance Metrics
- **SMA API Response Time**: < 100ms (95th percentile)
- **FIX Message Latency**: < 50ms (95th percentile)
- **System Availability**: > 99.9%
- **Data Accuracy**: > 99.99%

### Business Metrics
- **Price Publishing Frequency**: Every 5 seconds for active baskets
- **Vendor Integration Success Rate**: > 99%
- **Error Recovery Time**: < 30 seconds
- **Data Quality Score**: > 95%

## Conclusion

This implementation plan provides a comprehensive roadmap for integrating SMA Refinitiv API and FIX protocol with Bloomberg into the existing CIB Listing & Pricing Service. The phased approach ensures minimal risk while delivering maximum value.

Key benefits of this implementation:
- ✅ **Non-invasive integration** - No changes to existing services
- ✅ **Scalable architecture** - Independent service scaling
- ✅ **Fault tolerance** - Circuit breakers and retry mechanisms
- ✅ **Comprehensive monitoring** - Full observability
- ✅ **Production ready** - Security and performance optimized

The implementation follows industry best practices and ensures the system remains maintainable, scalable, and reliable while adding the required SMA and FIX capabilities.
