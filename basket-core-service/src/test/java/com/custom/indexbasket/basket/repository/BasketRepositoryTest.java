package com.custom.indexbasket.basket.repository;

import com.custom.indexbasket.basket.domain.BasketEntity;
import com.custom.indexbasket.common.model.BasketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers
class BasketRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("basket_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> 
            "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private BasketRepository basketRepository;

    private BasketEntity testBasket;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        basketRepository.deleteAll().block();

        testBasket = new BasketEntity();
        testBasket.setId(UUID.randomUUID());
        testBasket.setBasketCode("TEST_BASKET_001");
        testBasket.setBasketName("Test Basket");
        testBasket.setDescription("Test Description");
        testBasket.setBasketType("EQUITY");
        testBasket.setBaseCurrency("USD");
        testBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        testBasket.setStatus(BasketStatus.DRAFT);
        testBasket.setVersion("v1.0");
        testBasket.setCreatedBy("testuser");
        testBasket.setCreatedAt(LocalDateTime.now());
        testBasket.setUpdatedAt(LocalDateTime.now());
        testBasket.setIsActive(true);
        testBasket.setTotalMarketValue(BigDecimal.valueOf(1000000));
        testBasket.setBacktestScore(BigDecimal.valueOf(0.85));
    }

    @Test
    void save_Success() {
        // When & Then
        StepVerifier.create(basketRepository.save(testBasket))
                .expectNextMatches(saved -> 
                    saved.getBasketCode().equals("TEST_BASKET_001") &&
                    saved.getBasketName().equals("Test Basket") &&
                    saved.getStatus() == BasketStatus.DRAFT
                )
                .verifyComplete();
    }

    @Test
    void findById_Success() {
        // Given
        BasketEntity saved = basketRepository.save(testBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.findById(saved.getId()))
                .expectNextMatches(found -> 
                    found.getBasketCode().equals("TEST_BASKET_001") &&
                    found.getBasketName().equals("Test Basket")
                )
                .verifyComplete();
    }

    @Test
    void findById_NotFound() {
        // When & Then
        StepVerifier.create(basketRepository.findById(UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void findByStatus_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity activeBasket = new BasketEntity();
        activeBasket.setId(UUID.randomUUID());
        activeBasket.setBasketCode("ACTIVE_BASKET");
        activeBasket.setBasketName("Active Basket");
        activeBasket.setBasketType("EQUITY");
        activeBasket.setBaseCurrency("USD");
        activeBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        activeBasket.setStatus(BasketStatus.ACTIVE);
        activeBasket.setVersion("v1.0");
        activeBasket.setCreatedBy("testuser");
        activeBasket.setCreatedAt(LocalDateTime.now());
        activeBasket.setUpdatedAt(LocalDateTime.now());
        activeBasket.setIsActive(true);

        basketRepository.save(activeBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.findByStatus(BasketStatus.DRAFT))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(basketRepository.findByStatus(BasketStatus.ACTIVE))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void findByCreatedBy_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity anotherBasket = new BasketEntity();
        anotherBasket.setId(UUID.randomUUID());
        anotherBasket.setBasketCode("ANOTHER_BASKET");
        anotherBasket.setBasketName("Another Basket");
        anotherBasket.setBasketType("EQUITY");
        anotherBasket.setBaseCurrency("USD");
        anotherBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        anotherBasket.setStatus(BasketStatus.DRAFT);
        anotherBasket.setVersion("v1.0");
        anotherBasket.setCreatedBy("anotheruser");
        anotherBasket.setCreatedAt(LocalDateTime.now());
        anotherBasket.setUpdatedAt(LocalDateTime.now());
        anotherBasket.setIsActive(true);

        basketRepository.save(anotherBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.findByCreatedBy("testuser"))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(basketRepository.findByCreatedBy("anotheruser"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void findByBasketType_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity bondBasket = new BasketEntity();
        bondBasket.setId(UUID.randomUUID());
        bondBasket.setBasketCode("BOND_BASKET");
        bondBasket.setBasketName("Bond Basket");
        bondBasket.setBasketType("FIXED_INCOME");
        bondBasket.setBaseCurrency("USD");
        bondBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        bondBasket.setStatus(BasketStatus.DRAFT);
        bondBasket.setVersion("v1.0");
        bondBasket.setCreatedBy("testuser");
        bondBasket.setCreatedAt(LocalDateTime.now());
        bondBasket.setUpdatedAt(LocalDateTime.now());
        bondBasket.setIsActive(true);

        basketRepository.save(bondBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.findByBasketType("EQUITY"))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(basketRepository.findByBasketType("FIXED_INCOME"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void findByBacktestScoreGreaterThan_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity lowScoreBasket = new BasketEntity();
        lowScoreBasket.setId(UUID.randomUUID());
        lowScoreBasket.setBasketCode("LOW_SCORE_BASKET");
        lowScoreBasket.setBasketName("Low Score Basket");
        lowScoreBasket.setBasketType("EQUITY");
        lowScoreBasket.setBaseCurrency("USD");
        lowScoreBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        lowScoreBasket.setStatus(BasketStatus.DRAFT);
        lowScoreBasket.setVersion("v1.0");
        lowScoreBasket.setCreatedBy("testuser");
        lowScoreBasket.setCreatedAt(LocalDateTime.now());
        lowScoreBasket.setUpdatedAt(LocalDateTime.now());
        lowScoreBasket.setIsActive(true);
        lowScoreBasket.setBacktestScore(BigDecimal.valueOf(0.65));

        basketRepository.save(lowScoreBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.findByBacktestScoreGreaterThan(BigDecimal.valueOf(0.8)))
                .expectNextCount(1) // Only testBasket with score 0.85
                .verifyComplete();

        StepVerifier.create(basketRepository.findByBacktestScoreGreaterThan(BigDecimal.valueOf(0.6)))
                .expectNextCount(2) // Both baskets
                .verifyComplete();
    }

    @Test
    void countByStatus_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity activeBasket = new BasketEntity();
        activeBasket.setId(UUID.randomUUID());
        activeBasket.setBasketCode("ACTIVE_BASKET");
        activeBasket.setBasketName("Active Basket");
        activeBasket.setBasketType("EQUITY");
        activeBasket.setBaseCurrency("USD");
        activeBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        activeBasket.setStatus(BasketStatus.ACTIVE);
        activeBasket.setVersion("v1.0");
        activeBasket.setCreatedBy("testuser");
        activeBasket.setCreatedAt(LocalDateTime.now());
        activeBasket.setUpdatedAt(LocalDateTime.now());
        activeBasket.setIsActive(true);

        basketRepository.save(activeBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.countByStatus(BasketStatus.DRAFT))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(basketRepository.countByStatus(BasketStatus.ACTIVE))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(basketRepository.countByStatus(BasketStatus.DELETED))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void findAllActive_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity inactiveBasket = new BasketEntity();
        inactiveBasket.setId(UUID.randomUUID());
        inactiveBasket.setBasketCode("INACTIVE_BASKET");
        inactiveBasket.setBasketName("Inactive Basket");
        inactiveBasket.setBasketType("EQUITY");
        inactiveBasket.setBaseCurrency("USD");
        inactiveBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        inactiveBasket.setStatus(BasketStatus.DRAFT);
        inactiveBasket.setVersion("v1.0");
        inactiveBasket.setCreatedBy("testuser");
        inactiveBasket.setCreatedAt(LocalDateTime.now());
        inactiveBasket.setUpdatedAt(LocalDateTime.now());
        inactiveBasket.setIsActive(false); // Inactive

        basketRepository.save(inactiveBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.findAllActive())
                .expectNextCount(1) // Only active basket
                .verifyComplete();
    }

    @Test
    void countActive_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity inactiveBasket = new BasketEntity();
        inactiveBasket.setId(UUID.randomUUID());
        inactiveBasket.setBasketCode("INACTIVE_BASKET");
        inactiveBasket.setBasketName("Inactive Basket");
        inactiveBasket.setBasketType("EQUITY");
        inactiveBasket.setBaseCurrency("USD");
        inactiveBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        inactiveBasket.setStatus(BasketStatus.DRAFT);
        inactiveBasket.setVersion("v1.0");
        inactiveBasket.setCreatedBy("testuser");
        inactiveBasket.setCreatedAt(LocalDateTime.now());
        inactiveBasket.setUpdatedAt(LocalDateTime.now());
        inactiveBasket.setIsActive(false); // Inactive

        basketRepository.save(inactiveBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.countActive())
                .expectNext(1L) // Only active basket
                .verifyComplete();
    }

    @Test
    void findByMarketValueRange_Success() {
        // Given
        basketRepository.save(testBasket).block();

        BasketEntity highValueBasket = new BasketEntity();
        highValueBasket.setId(UUID.randomUUID());
        highValueBasket.setBasketCode("HIGH_VALUE_BASKET");
        highValueBasket.setBasketName("High Value Basket");
        highValueBasket.setBasketType("EQUITY");
        highValueBasket.setBaseCurrency("USD");
        highValueBasket.setTotalWeight(BigDecimal.valueOf(100.0));
        highValueBasket.setStatus(BasketStatus.DRAFT);
        highValueBasket.setVersion("v1.0");
        highValueBasket.setCreatedBy("testuser");
        highValueBasket.setCreatedAt(LocalDateTime.now());
        highValueBasket.setUpdatedAt(LocalDateTime.now());
        highValueBasket.setIsActive(true);
        highValueBasket.setTotalMarketValue(BigDecimal.valueOf(5000000));

        basketRepository.save(highValueBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.findByMarketValueRange(
                BigDecimal.valueOf(500000), 
                BigDecimal.valueOf(2000000)
        ))
                .expectNextCount(1) // Only testBasket with 1M value
                .verifyComplete();

        StepVerifier.create(basketRepository.findByMarketValueRange(
                BigDecimal.valueOf(500000), 
                BigDecimal.valueOf(10000000)
        ))
                .expectNextCount(2) // Both baskets
                .verifyComplete();
    }

    @Test
    void update_Success() {
        // Given
        BasketEntity saved = basketRepository.save(testBasket).block();

        // When
        saved.setBasketName("Updated Test Basket");
        saved.setStatus(BasketStatus.ACTIVE);
        saved.setUpdatedAt(LocalDateTime.now());

        // Then
        StepVerifier.create(basketRepository.save(saved))
                .expectNextMatches(updated -> 
                    updated.getBasketName().equals("Updated Test Basket") &&
                    updated.getStatus() == BasketStatus.ACTIVE &&
                    updated.getId().equals(saved.getId())
                )
                .verifyComplete();
    }

    @Test
    void deleteById_Success() {
        // Given
        BasketEntity saved = basketRepository.save(testBasket).block();

        // When & Then
        StepVerifier.create(basketRepository.deleteById(saved.getId()))
                .verifyComplete();

        StepVerifier.create(basketRepository.findById(saved.getId()))
                .verifyComplete();
    }
}
