package com.custom.indexbasket.basket.util;

import com.custom.indexbasket.basket.domain.BasketEntity;
import com.custom.indexbasket.basket.domain.BasketConstituentEntity;
import com.custom.indexbasket.basket.dto.*;
import com.custom.indexbasket.common.model.BasketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Factory class for creating test data objects
 */
public class TestDataFactory {

    /**
     * Create a default test basket entity
     */
    public static BasketEntity createTestBasket() {
        return createTestBasket(UUID.randomUUID(), "TEST_BASKET_001", "Test Basket");
    }

    /**
     * Create a test basket entity with custom values
     */
    public static BasketEntity createTestBasket(UUID id, String basketCode, String basketName) {
        BasketEntity basket = new BasketEntity();
        basket.setId(id);
        basket.setBasketCode(basketCode);
        basket.setBasketName(basketName);
        basket.setDescription("Test Description for " + basketName);
        basket.setBasketType("EQUITY");
        basket.setBaseCurrency("USD");
        basket.setTotalWeight(BigDecimal.valueOf(100.0));
        basket.setStatus(BasketStatus.DRAFT);
        basket.setVersion("v1.0");
        basket.setCreatedBy("testuser");
        basket.setCreatedAt(LocalDateTime.now());
        basket.setUpdatedAt(LocalDateTime.now());
        basket.setIsActive(true);
        basket.setTotalMarketValue(BigDecimal.valueOf(1000000));
        basket.setBacktestScore(BigDecimal.valueOf(0.85));
        return basket;
    }

    /**
     * Create a test basket entity with specific status
     */
    public static BasketEntity createTestBasketWithStatus(BasketStatus status) {
        BasketEntity basket = createTestBasket();
        basket.setStatus(status);
        return basket;
    }

    /**
     * Create a test basket entity with specific type
     */
    public static BasketEntity createTestBasketWithType(String basketType) {
        BasketEntity basket = createTestBasket();
        basket.setBasketType(basketType);
        return basket;
    }

    /**
     * Create a default test basket constituent entity
     */
    public static BasketConstituentEntity createTestConstituent(UUID basketId) {
        return createTestConstituent(basketId, "AAPL", "Apple Inc.", BigDecimal.valueOf(10.0));
    }

    /**
     * Create a test basket constituent entity with custom values
     */
    public static BasketConstituentEntity createTestConstituent(UUID basketId, String symbol, String symbolName, BigDecimal weight) {
        BasketConstituentEntity constituent = new BasketConstituentEntity();
        constituent.setEntityId(UUID.randomUUID());
        constituent.setBasketId(basketId);
        constituent.setSymbol(symbol);
        constituent.setSymbolName(symbolName);
        constituent.setWeight(weight);
        constituent.setShares(100L);
        constituent.setCurrentPrice(BigDecimal.valueOf(150.0));
        constituent.setMarketValue(BigDecimal.valueOf(15000.0));
        constituent.setSector("Technology");
        constituent.setIndustry("Technology Hardware");
        constituent.setCountry("US");
        constituent.setExchange("NASDAQ");
        constituent.setCurrency("USD");
        constituent.setIsin("US0378331005");
        constituent.setCusip("037833100");
        constituent.setSedol("2046251");
        constituent.setIsActive(true);
        constituent.setCreatedAt(LocalDateTime.now());
        constituent.setUpdatedAt(LocalDateTime.now());
        constituent.setRiskScore(BigDecimal.valueOf(0.60));
        constituent.setPerformanceScore(BigDecimal.valueOf(0.80));
        return constituent;
    }

    /**
     * Create a test basket constituent in a specific sector
     */
    public static BasketConstituentEntity createTestConstituentInSector(UUID basketId, String sector) {
        BasketConstituentEntity constituent = createTestConstituent(basketId);
        constituent.setSector(sector);
        return constituent;
    }

    /**
     * Create a CreateBasketRequest for testing
     */
    public static CreateBasketRequest createBasketRequest() {
        return createBasketRequest("TEST_BASKET_001", "Test Basket");
    }

    /**
     * Create a CreateBasketRequest with custom values
     */
    public static CreateBasketRequest createBasketRequest(String basketCode, String basketName) {
        return new CreateBasketRequest(
                basketCode,
                basketName,
                "Test Description",
                "EQUITY",
                "USD",
                BigDecimal.valueOf(100.0),
                "v1.0",
                null,
                "testuser",
                List.of()
        );
    }

    /**
     * Create an UpdateBasketRequest for testing
     */
    public static UpdateBasketRequest createUpdateBasketRequest() {
        return new UpdateBasketRequest(
                "Updated Test Basket",           // basketName
                "Updated Description",           // description
                null,                            // basketType
                null,                            // baseCurrency
                BigDecimal.valueOf(100.0),       // totalWeight
                null,                            // status
                null,                            // version
                "testuser"                       // updatedBy
        );
    }

    /**
     * Create an UpdateBasketStatusRequest for testing
     */
    public static UpdateBasketStatusRequest createUpdateBasketStatusRequest(String event) {
        return new UpdateBasketStatusRequest(event, "admin");
    }

    /**
     * Create an AddConstituentRequest for testing
     */
    public static AddConstituentRequest createAddConstituentRequest() {
        return createAddConstituentRequest("AAPL", "Apple Inc.", BigDecimal.valueOf(10.0));
    }

    /**
     * Create an AddConstituentRequest with custom values
     */
    public static AddConstituentRequest createAddConstituentRequest(String symbol, String symbolName, BigDecimal weight) {
        return new AddConstituentRequest(
                symbol,
                symbolName,
                weight,
                100L,
                weight,
                "Technology",
                "US",
                "USD"
        );
    }

    /**
     * Create an UpdateConstituentWeightRequest for testing
     */
    public static UpdateConstituentWeightRequest createUpdateConstituentWeightRequest(BigDecimal newWeight) {
        return new UpdateConstituentWeightRequest(newWeight, "admin");
    }

    /**
     * Create a RebalanceBasketRequest for testing
     */
    public static RebalanceBasketRequest createRebalanceBasketRequest() {
        CreateConstituentRequest constituent = new CreateConstituentRequest(
                "MSFT",
                "Microsoft Corp.",
                BigDecimal.valueOf(20.0),
                50L,
                BigDecimal.valueOf(20.0),
                "Technology",
                "US",
                "USD"
        );
        return new RebalanceBasketRequest(List.of(constituent), "admin");
    }

    /**
     * Create a PaginatedBasketsResponse for testing
     */
    public static PaginatedBasketsResponse createPaginatedBasketsResponse(List<BasketEntity> baskets) {
        return new PaginatedBasketsResponse(
                baskets,
                0,
                20,
                baskets.size(),
                1
        );
    }

    /**
     * Create a BasketAnalyticsResponse for testing
     */
    public static BasketAnalyticsResponse createBasketAnalyticsResponse(String basketCode) {
        return new BasketAnalyticsResponse(
                basketCode,
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.12),
                BigDecimal.valueOf(0.15),
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(0.08),
                List.of(),
                LocalDateTime.now()
        );
    }

    /**
     * Create a SectorExposureResponse for testing
     */
    public static SectorExposureResponse createSectorExposureResponse(String sector) {
        return new SectorExposureResponse(
                sector,
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(0.08)
        );
    }

    /**
     * Create a HealthResponse for testing
     */
    public static HealthResponse createHealthResponse() {
        return new HealthResponse(
                "UP",
                LocalDateTime.now(),
                java.util.Map.of(
                        "service", "Basket Core Service",
                        "status", "healthy",
                        "database", "connected"
                )
        );
    }

    /**
     * Create a MetricsResponse for testing
     */
    public static MetricsResponse createMetricsResponse() {
        return new MetricsResponse(
                LocalDateTime.now(),
                java.util.Map.of(
                        "active_baskets", 10,
                        "total_constituents", 250,
                        "uptime_minutes", 120,
                        "memory_usage_mb", 512
                )
        );
    }

    /**
     * Create multiple test baskets for bulk operations
     */
    public static List<BasketEntity> createMultipleTestBaskets(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> createTestBasket(
                        UUID.randomUUID(),
                        "TEST_BASKET_" + String.format("%03d", i + 1),
                        "Test Basket " + (i + 1)
                ))
                .toList();
    }

    /**
     * Create multiple test constituents for a basket
     */
    public static List<BasketConstituentEntity> createMultipleTestConstituents(UUID basketId, int count) {
        String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "AMD", "CRM"};
        String[] names = {"Apple Inc.", "Microsoft Corp.", "Alphabet Inc.", "Amazon.com Inc.", "Tesla Inc.",
                         "Meta Platforms", "NVIDIA Corp.", "Netflix Inc.", "Advanced Micro Devices", "Salesforce"};

        return java.util.stream.IntStream.range(0, Math.min(count, symbols.length))
                .mapToObj(i -> createTestConstituent(
                        basketId,
                        symbols[i],
                        names[i],
                        BigDecimal.valueOf(10.0 + i)
                ))
                .toList();
    }

    /**
     * Create test baskets with different statuses
     */
    public static List<BasketEntity> createBasketsWithDifferentStatuses() {
        return List.of(
                createTestBasketWithStatus(BasketStatus.DRAFT),
                createTestBasketWithStatus(BasketStatus.ACTIVE),
                createTestBasketWithStatus(BasketStatus.SUSPENDED),
                createTestBasketWithStatus(BasketStatus.PENDING_APPROVAL),
                createTestBasketWithStatus(BasketStatus.APPROVED)
        );
    }

    /**
     * Create test baskets with different types
     */
    public static List<BasketEntity> createBasketsWithDifferentTypes() {
        return List.of(
                createTestBasketWithType("EQUITY"),
                createTestBasketWithType("FIXED_INCOME"),
                createTestBasketWithType("COMMODITY"),
                createTestBasketWithType("CURRENCY"),
                createTestBasketWithType("MIXED")
        );
    }
}
