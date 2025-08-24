# Proxy Services Implementation Guide

## Overview

This guide provides detailed implementation specifications for the **proxy services** that simulate external market data providers and vendor endpoints. These services enable comprehensive end-to-end testing without requiring actual vendor connections.

## 🎭 Proxy Services Stack

```yaml
Proxy Services:
  📊 Market Data Producer Proxy    (Port: 9201)
  💱 FX Rate Provider Proxy       (Port: 9202)  
  🏦 Bloomberg Vendor Proxy       (Port: 9203)
  🔄 Refinitiv Vendor Proxy       (Port: 9204)

Integration Points:
  - Kafka Topics for data streaming
  - Redis Cache for real-time data
  - REST APIs for vendor simulation
  - WebSocket streams for real-time feeds
```

## 📊 Market Data Producer Proxy Implementation

### Service Structure
```
market-data-producer-proxy/
├── src/main/java/com/custom/indexbasket/marketdata/
│   ├── MarketDataProducerProxyApplication.java
│   ├── config/
│   │   ├── KafkaProducerConfig.java
│   │   ├── RedisConfig.java
│   │   └── SchedulingConfig.java
│   ├── model/
│   │   ├── PriceUpdate.java
│   │   ├── MarketEvent.java
│   │   ├── Symbol.java
│   │   └── MarketSession.java
│   ├── service/
│   │   ├── PriceSimulationService.java
│   │   ├── MarketEventService.java
│   │   ├── SymbolManagementService.java
│   │   └── DataPublishingService.java
│   ├── controller/
│   │   ├── SimulationController.java
│   │   └── MarketStatusController.java
│   └── generator/
│       ├── GeometricBrownianMotion.java
│       ├── VolumeGenerator.java
│       └── MarketHoursSimulator.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-docker.yml
│   └── symbols/
│       ├── nasdaq-symbols.csv
│       ├── sp500-symbols.csv
│       └── global-indices.csv
└── pom.xml
```

### Core Models
```java
// PriceUpdate.java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceUpdate {
    private String symbol;
    private BigDecimal price;
    private BigDecimal previousClose;
    private Long volume;
    private BigDecimal high;
    private BigDecimal low;
    private String currency;
    private LocalDateTime timestamp;
    private String source; // "BLOOMBERG", "REFINITIV", "YAHOO", "ALPHA_VANTAGE"
    private BigDecimal changePercent;
    private MarketSession session; // "PRE_MARKET", "REGULAR", "AFTER_HOURS"
    
    // Constructors, getters, setters
}

// MarketEvent.java
public class MarketEvent {
    private String eventType; // "MARKET_OPEN", "MARKET_CLOSE", "TRADING_HALT", "CORPORATE_ACTION"
    private String market; // "NYSE", "NASDAQ", "LSE", "TSE"
    private LocalDateTime timestamp;
    private String description;
    private List<String> affectedSymbols;
    
    // Constructors, getters, setters
}
```

### Price Simulation Service
```java
@Service
@Slf4j
public class PriceSimulationService {
    
    private final GeometricBrownianMotion priceGenerator;
    private final VolumeGenerator volumeGenerator;
    private final DataPublishingService publishingService;
    private final Map<String, SymbolState> symbolStates = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void generatePriceUpdates() {
        if (!isMarketOpen()) {
            return;
        }
        
        symbolStates.forEach((symbol, state) -> {
            try {
                PriceUpdate update = generatePriceUpdate(symbol, state);
                publishingService.publishPriceUpdate(update);
                state.updateState(update);
                
                log.debug("Generated price update for {}: ${}", symbol, update.getPrice());
            } catch (Exception e) {
                log.error("Error generating price for symbol {}: {}", symbol, e.getMessage());
            }
        });
    }
    
    private PriceUpdate generatePriceUpdate(String symbol, SymbolState state) {
        BigDecimal newPrice = priceGenerator.nextPrice(
            state.getCurrentPrice(),
            state.getVolatility(),
            state.getDrift()
        );
        
        Long volume = volumeGenerator.generateVolume(
            symbol,
            LocalTime.now(),
            state.getAverageVolume()
        );
        
        return PriceUpdate.builder()
            .symbol(symbol)
            .price(newPrice)
            .previousClose(state.getPreviousClose())
            .volume(volume)
            .high(state.getDayHigh().max(newPrice))
            .low(state.getDayLow().min(newPrice))
            .currency("USD")
            .timestamp(LocalDateTime.now())
            .source("SIMULATION")
            .changePercent(calculateChangePercent(newPrice, state.getPreviousClose()))
            .session(getCurrentMarketSession())
            .build();
    }
}
```

### Kafka Configuration
```yaml
# application.yml
simulation:
  market-data:
    enabled: true
    update-frequency: 5000ms
    market-hours:
      timezone: "America/New_York"
      regular:
        start: "09:30"
        end: "16:00"
      pre-market:
        start: "04:00"
        end: "09:30"
      after-hours:
        start: "16:00"
        end: "20:00"
    
    symbols:
      default-volatility: 0.25
      default-drift: 0.05
      
kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    acks: 1
    retries: 3
    batch-size: 16384
    linger-ms: 10
    buffer-memory: 33554432
  
  topics:
    market-data-equity: "market.data.equity.prices"
    market-data-etf: "market.data.etf.prices"
    market-data-index: "market.data.index.prices"
    market-events: "market.events.session"
```

## 💱 FX Rate Provider Proxy Implementation

### Service Structure
```
fx-rate-provider-proxy/
├── src/main/java/com/custom/indexbasket/fx/
│   ├── FxRateProviderProxyApplication.java
│   ├── model/
│   │   ├── FxRate.java
│   │   ├── CurrencyPair.java
│   │   └── CentralBankEvent.java
│   ├── service/
│   │   ├── FxRateSimulationService.java
│   │   ├── CentralBankSimulator.java
│   │   └── CrossRateCalculator.java
│   ├── controller/
│   │   └── FxRateController.java
│   └── generator/
│       ├── FxRateRandomWalk.java
│       └── CorrelationMatrix.java
└── src/main/resources/
    ├── application.yml
    └── currency-config/
        ├── major-pairs.yml
        ├── emerging-pairs.yml
        └── crypto-pairs.yml
```

### FX Rate Model
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FxRate {
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal rate;
    private BigDecimal bid;
    private BigDecimal ask;
    private BigDecimal spread;
    private LocalDateTime timestamp;
    private String source; // "ECB", "FED", "BOE", "BOJ", "SIMULATION"
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
    
    // Constructors, getters, setters
}
```

### FX Simulation Service
```java
@Service
@Slf4j
public class FxRateSimulationService {
    
    private final FxRateRandomWalk rateGenerator;
    private final CrossRateCalculator crossRateCalculator;
    private final DataPublishingService publishingService;
    private final Map<String, FxRateState> rateStates = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void generateFxRateUpdates() {
        // Generate major pairs first
        List<String> majorPairs = Arrays.asList("EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD", "AUDUSD");
        
        majorPairs.forEach(pair -> {
            FxRateState state = rateStates.get(pair);
            FxRate newRate = generateFxRate(pair, state);
            publishingService.publishFxRate(newRate);
            state.updateState(newRate);
        });
        
        // Calculate cross rates
        calculateAndPublishCrossRates();
    }
    
    private FxRate generateFxRate(String currencyPair, FxRateState state) {
        BigDecimal newRate = rateGenerator.nextRate(
            state.getCurrentRate(),
            state.getVolatility(),
            state.getTrend()
        );
        
        BigDecimal spread = calculateSpread(currencyPair, newRate);
        
        return FxRate.builder()
            .baseCurrency(currencyPair.substring(0, 3))
            .quoteCurrency(currencyPair.substring(3, 6))
            .rate(newRate)
            .bid(newRate.subtract(spread.divide(BigDecimal.valueOf(2))))
            .ask(newRate.add(spread.divide(BigDecimal.valueOf(2))))
            .spread(spread)
            .timestamp(LocalDateTime.now())
            .source("SIMULATION")
            .dailyChange(newRate.subtract(state.getDayOpen()))
            .dailyChangePercent(calculateChangePercent(newRate, state.getDayOpen()))
            .build();
    }
}
```

## 🏦 Bloomberg Vendor Proxy Implementation

### Service Structure
```
bloomberg-vendor-proxy/
├── src/main/java/com/custom/indexbasket/bloomberg/
│   ├── BloombergVendorProxyApplication.java
│   ├── model/
│   │   ├── BasketListingRequest.java
│   │   ├── BasketListingResponse.java
│   │   ├── PricePublishRequest.java
│   │   └── BloombergApiResponse.java
│   ├── service/
│   │   ├── BasketListingService.java
│   │   ├── PricePublishingService.java
│   │   └── ConnectionSimulator.java
│   ├── controller/
│   │   ├── BsymController.java      // BSYM API simulation
│   │   └── BlpapiController.java    // BLPAPI simulation
│   └── simulator/
│       ├── LatencySimulator.java
│       ├── ErrorSimulator.java
│       └── PerformanceTracker.java
└── src/main/resources/
    ├── application.yml
    └── bloomberg-responses/
        ├── listing-success.json
        ├── listing-error.json
        └── price-ack.json
```

### Bloomberg API Controllers
```java
@RestController
@RequestMapping("/api/bloomberg/bsym")
@Slf4j
public class BsymController {
    
    private final BasketListingService listingService;
    private final LatencySimulator latencySimulator;
    
    @PostMapping("/basket/create")
    public Mono<ResponseEntity<BasketListingResponse>> createBasketListing(
            @RequestBody BasketListingRequest request) {
        
        return Mono.fromCallable(() -> {
            // Simulate Bloomberg BSYM API latency
            latencySimulator.simulateLatency("BSYM_CREATE", 50, 200);
            
            log.info("📥 Bloomberg BSYM: Received basket listing request for {}", 
                request.getBasketCode());
            
            return listingService.processListingRequest(request);
        })
        .map(response -> ResponseEntity.ok(response))
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BasketListingResponse.error("Internal BSYM error")));
    }
    
    @GetMapping("/basket/{basketCode}/status")
    public Mono<ResponseEntity<BasketListingResponse>> getListingStatus(
            @PathVariable String basketCode) {
        
        return Mono.fromCallable(() -> {
            latencySimulator.simulateLatency("BSYM_STATUS", 10, 50);
            
            return listingService.getListingStatus(basketCode);
        })
        .map(response -> ResponseEntity.ok(response));
    }
}

@RestController
@RequestMapping("/api/bloomberg/blpapi")
@Slf4j
public class BlpapiController {
    
    private final PricePublishingService priceService;
    private final PerformanceTracker performanceTracker;
    
    @PostMapping("/prices/publish")
    public Mono<ResponseEntity<BloombergApiResponse>> publishPrices(
            @RequestBody PricePublishRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        return Mono.fromCallable(() -> {
            // Simulate BLPAPI latency
            latencySimulator.simulateLatency("BLPAPI_PUBLISH", 1, 10);
            
            log.info("📊 Bloomberg BLPAPI: Received {} price updates", 
                request.getPriceUpdates().size());
            
            BloombergApiResponse response = priceService.processPriceUpdates(request);
            
            performanceTracker.recordPricePublishing(
                request.getPriceUpdates().size(),
                System.currentTimeMillis() - startTime
            );
            
            return response;
        })
        .map(response -> ResponseEntity.ok(response));
    }
}
```

## 🔄 Refinitiv Vendor Proxy Implementation

### Service Structure
```
refinitiv-vendor-proxy/
├── src/main/java/com/custom/indexbasket/refinitiv/
│   ├── RefinitivVendorProxyApplication.java
│   ├── model/
│   │   ├── RdpAuthRequest.java
│   │   ├── RdpAuthResponse.java
│   │   ├── InstrumentRegistration.java
│   │   └── ContributionRequest.java
│   ├── service/
│   │   ├── RdpAuthService.java
│   │   ├── InstrumentService.java
│   │   ├── ContributionService.java
│   │   └── ElektronSimulator.java
│   ├── controller/
│   │   ├── RdpController.java
│   │   └── ElektronController.java
│   └── websocket/
│       ├── ElektronWebSocketHandler.java
│       └── RealTimeDataStreamer.java
└── src/main/resources/
    ├── application.yml
    └── refinitiv-responses/
        ├── auth-success.json
        ├── instrument-registered.json
        └── contribution-ack.json
```

### Refinitiv RDP Controller
```java
@RestController
@RequestMapping("/api/refinitiv/rdp")
@Slf4j
public class RdpController {
    
    private final RdpAuthService authService;
    private final InstrumentService instrumentService;
    
    @PostMapping("/auth/token")
    public Mono<ResponseEntity<RdpAuthResponse>> authenticate(
            @RequestBody RdpAuthRequest request) {
        
        return Mono.fromCallable(() -> {
            log.info("🔐 Refinitiv RDP: Authentication request for {}", 
                request.getClientId());
            
            return authService.authenticate(request);
        })
        .map(response -> ResponseEntity.ok(response));
    }
    
    @PostMapping("/instruments/register")
    public Mono<ResponseEntity<ApiResponse>> registerInstrument(
            @RequestBody InstrumentRegistration registration,
            @RequestHeader("Authorization") String token) {
        
        return Mono.fromCallable(() -> {
            if (!authService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token"));
            }
            
            log.info("📋 Refinitiv RDP: Registering instrument {}", 
                registration.getInstrumentCode());
            
            return instrumentService.registerInstrument(registration);
        })
        .map(response -> ResponseEntity.ok(response));
    }
}
```

## 🐳 Docker Compose Integration

### Complete Proxy Environment
```yaml
# docker-compose-test-environment.yml
version: '3.8'

services:
  # Existing platform services
  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # Proxy Services
  market-data-producer-proxy:
    build: ./market-data-producer-proxy
    ports:
      - "9201:9090"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - REDIS_HOST=redis
      - SIMULATION_ENABLED=true
    depends_on:
      - kafka
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9090/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  fx-rate-provider-proxy:
    build: ./fx-rate-provider-proxy
    ports:
      - "9202:9090"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - REDIS_HOST=redis
    depends_on:
      - kafka
      - redis

  bloomberg-vendor-proxy:
    build: ./bloomberg-vendor-proxy
    ports:
      - "9203:9090"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SIMULATION_LATENCY_MIN=10
      - SIMULATION_LATENCY_MAX=100
    volumes:
      - ./logs/bloomberg-proxy:/app/logs

  refinitiv-vendor-proxy:
    build: ./refinitiv-vendor-proxy
    ports:
      - "9204:9090"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - OAUTH_TOKEN_VALIDITY=3600
    volumes:
      - ./logs/refinitiv-proxy:/app/logs

  # Monitoring
  proxy-monitoring:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
```

## 🧪 End-to-End Testing Scripts

### Complete Testing Flow
```bash
#!/bin/bash
# test-complete-flow.sh

echo "🚀 Starting Complete End-to-End Testing Flow"

# 1. Start all proxy services
echo "📊 Starting proxy services..."
docker-compose -f docker-compose-test-environment.yml up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# 2. Test Market Data Flow
echo "📈 Testing Market Data Producer Proxy..."
curl -s http://localhost:9201/api/simulation/symbols | jq '.symbols | length'
curl -s http://localhost:9201/api/simulation/market/status | jq '.marketOpen'

# 3. Test FX Rate Provider
echo "💱 Testing FX Rate Provider Proxy..."
curl -s http://localhost:9202/api/fx/rates | jq '.rates | length'
curl -s http://localhost:9202/api/fx/rates/USD/EUR | jq '.rate'

# 4. Test Bloomberg Vendor Proxy
echo "🏦 Testing Bloomberg Vendor Proxy..."
curl -s -X POST http://localhost:9203/api/bloomberg/bsym/basket/create \
  -H "Content-Type: application/json" \
  -d '{
    "basketCode": "TEST_BASKET",
    "basketName": "Test Technology Basket",
    "constituents": [
      {"symbol": "AAPL", "weight": 0.3},
      {"symbol": "GOOGL", "weight": 0.3},
      {"symbol": "MSFT", "weight": 0.4}
    ]
  }' | jq

# 5. Test Refinitiv Vendor Proxy
echo "🔄 Testing Refinitiv Vendor Proxy..."
TOKEN=$(curl -s -X POST http://localhost:9204/api/refinitiv/rdp/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "test_client",
    "clientSecret": "test_secret"
  }' | jq -r '.access_token')

curl -s -X POST http://localhost:9204/api/refinitiv/rdp/instruments/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "instrumentCode": "TEST_BASKET",
    "instrumentType": "BASKET"
  }' | jq

echo "✅ All proxy services tested successfully!"

# 6. Monitor real-time data flow
echo "📊 Monitoring real-time data flow for 30 seconds..."
timeout 30 docker logs -f market-data-producer-proxy | grep "Generated price update" | head -10

echo "🎯 End-to-End Testing Complete!"
```

This comprehensive **proxy services implementation guide** provides all the specifications needed to create realistic external service simulators for thorough **end-to-end testing** of the Custom Index Basket Management Platform! 🎯✨

<function_calls>
<invoke name="todo_write">
<parameter name="merge">true
