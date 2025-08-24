package com.custom.indexbasket.basket.controller;

import com.custom.indexbasket.basket.domain.BasketEntity;
import com.custom.indexbasket.basket.domain.BasketConstituentEntity;
import com.custom.indexbasket.basket.dto.*;
import com.custom.indexbasket.basket.service.BasketService;
import com.custom.indexbasket.common.model.BasketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import com.custom.indexbasket.basket.config.StateMachineConfig;
import com.custom.indexbasket.basket.event.EventPublisher;

@WebFluxTest(BasketController.class)
class BasketControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BasketService basketService;

    @MockBean
    private StateMachineConfig stateMachineConfig;
    
    @MockBean
    private EventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testBasketId;
    private BasketEntity testBasket;

    @BeforeEach
    void setUp() {
        testBasketId = UUID.randomUUID();
        
        testBasket = new BasketEntity();
        testBasket.setId(testBasketId);
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
    }

    @Test
    void createBasket_Success() {
        // Given
        CreateBasketRequest request = new CreateBasketRequest(
                "TEST_BASKET_001",
                "Test Basket",
                "Test Description",
                "EQUITY",
                "USD",
                BigDecimal.valueOf(100.0),
                "v1.0",
                null,
                "testuser",
                List.of()
        );

        when(basketService.createBasket(any(CreateBasketRequest.class)))
                .thenReturn(Mono.just(testBasketId));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/baskets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isEqualTo(testBasketId.toString())
                .jsonPath("$.message").isEqualTo("Basket created successfully");
    }

    @Test
    void createBasket_InvalidRequest() {
        // Given
        CreateBasketRequest request = new CreateBasketRequest(
                "", // Invalid empty basket code
                "Test Basket",
                "Test Description",
                "EQUITY",
                "USD",
                BigDecimal.valueOf(100.0),
                "v1.0",
                null,
                "testuser",
                List.of()
        );

        // When & Then
        webTestClient.post()
                .uri("/api/v1/baskets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getBasket_Success() {
        // Given
        when(basketService.getBasketById(testBasketId)).thenReturn(Mono.just(testBasket));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets/{basketId}", testBasketId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(testBasketId.toString())
                .jsonPath("$.data.basketCode").isEqualTo("TEST_BASKET_001")
                .jsonPath("$.data.basketName").isEqualTo("Test Basket");
    }

    @Test
    void getBasket_NotFound() {
        // Given
        when(basketService.getBasketById(testBasketId)).thenReturn(Mono.error(new RuntimeException("Basket not found")));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets/{basketId}", testBasketId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void getAllBaskets_Success() {
        // Given
        PaginatedBasketsResponse response = new PaginatedBasketsResponse(
                List.of(testBasket),
                0,
                20,
                1L,
                1
        );

        when(basketService.getAllBaskets(any(PageRequest.class), any(), any()))
                .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.baskets").isArray()
                .jsonPath("$.data.baskets[0].id").isEqualTo(testBasketId.toString())
                .jsonPath("$.data.totalElements").isEqualTo(1)
                .jsonPath("$.data.totalPages").isEqualTo(1);
    }

    @Test
    void getAllBaskets_WithFilters() {
        // Given
        PaginatedBasketsResponse response = new PaginatedBasketsResponse(
                List.of(testBasket),
                0,
                20,
                1L,
                1
        );

        when(basketService.getAllBaskets(any(PageRequest.class), eq(BasketStatus.DRAFT), eq("EQUITY")))
                .thenReturn(Mono.just(response));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets?page=0&size=20&status=DRAFT&basketType=EQUITY")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.baskets").isArray()
                .jsonPath("$.data.totalElements").isEqualTo(1);
    }

    @Test
    void updateBasket_Success() {
        // Given
        UpdateBasketRequest request = new UpdateBasketRequest(
                "Updated Basket Name",           // basketName
                "Updated Description",           // description
                null,                            // basketType
                null,                            // baseCurrency
                BigDecimal.valueOf(100.0),       // totalWeight
                null,                            // status
                null,                            // version
                "testuser"                       // updatedBy
        );

        BasketEntity updatedBasket = new BasketEntity();
        updatedBasket.setId(testBasketId);
        updatedBasket.setBasketName("Updated Basket Name");

        when(basketService.updateBasket(eq(testBasketId), any(UpdateBasketRequest.class)))
                .thenReturn(Mono.just(updatedBasket));

        // When & Then
        webTestClient.put()
                .uri("/api/v1/baskets/{basketId}", testBasketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Basket updated successfully")
                .jsonPath("$.data.basketName").isEqualTo("Updated Basket Name");
    }

    @Test
    void updateBasketStatus_Success() {
        // Given
        UpdateBasketStatusRequest request = new UpdateBasketStatusRequest(
                "ACTIVATE",
                "admin"
        );

        when(basketService.updateBasketStatus(testBasketId, "ACTIVATE", "admin"))
            .thenReturn(Mono.empty());
        
        when(basketService.getEventPublishingStatus())
            .thenReturn(Mono.just(new EventPublisher.PublishingStats()));

        // When & Then
        webTestClient.patch()
                .uri("/api/v1/baskets/{basketId}/status", testBasketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Basket status updated successfully");
    }

    @Test
    void deleteBasket_Success() {
        // Given
        when(basketService.deleteBasket(testBasketId, "testuser")).thenReturn(Mono.empty());

        // When & Then
        webTestClient.delete()
                .uri("/api/v1/baskets/{basketId}?deletedBy=testuser", testBasketId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Basket deleted successfully");
    }

    @Test
    void addConstituent_Success() {
        // Given
        AddConstituentRequest request = new AddConstituentRequest(
                "AAPL",
                "Apple Inc.",
                BigDecimal.valueOf(10.0),
                100L,
                BigDecimal.valueOf(10.0),
                "Technology",
                "US",
                "USD"
        );

        BasketConstituentEntity constituent = new BasketConstituentEntity();
        constituent.setSymbol("AAPL");

        when(basketService.addConstituent(eq(testBasketId), any(AddConstituentRequest.class)))
                .thenReturn(Mono.just(constituent));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/baskets/{basketId}/constituents", testBasketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Constituent added successfully")
                .jsonPath("$.data.symbol").isEqualTo("AAPL");
    }

    @Test
    void updateConstituentWeight_Success() {
        // Given
        UpdateConstituentWeightRequest request = new UpdateConstituentWeightRequest(
                BigDecimal.valueOf(15.0),
                "admin"
        );

        when(basketService.updateConstituentWeight(testBasketId, "AAPL", BigDecimal.valueOf(15.0), "admin"))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.put()
                .uri("/api/v1/baskets/{basketId}/constituents/{symbol}/weight", testBasketId, "AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Constituent weight updated successfully");
    }

    @Test
    void removeConstituent_Success() {
        // Given
        when(basketService.removeConstituent(testBasketId, "AAPL")).thenReturn(Mono.empty());

        // When & Then
        webTestClient.delete()
                .uri("/api/v1/baskets/{basketId}/constituents/{symbol}", testBasketId, "AAPL")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Constituent removed successfully");
    }

    @Test
    void rebalanceBasket_Success() {
        // Given
        CreateConstituentRequest newConstituent = new CreateConstituentRequest(
                "MSFT",
                "Microsoft Corp.",
                BigDecimal.valueOf(20.0),
                50L,
                BigDecimal.valueOf(20.0),
                "Technology",
                "US",
                "USD"
        );

        RebalanceBasketRequest request = new RebalanceBasketRequest(
                List.of(newConstituent),
                "admin"
        );

        when(basketService.rebalanceBasket(eq(testBasketId), anyList(), eq("admin")))
                .thenReturn(Mono.just(testBasket));

        // When & Then
        webTestClient.post()
                .uri("/api/v1/baskets/{basketId}/rebalance", testBasketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Basket rebalanced successfully");
    }

    @Test
    void getBasketConstituents_Success() {
        // Given
        BasketConstituentEntity constituent = new BasketConstituentEntity();
        constituent.setSymbol("AAPL");

        when(basketService.getBasketConstituents(testBasketId))
                .thenReturn(Mono.just(List.of(constituent)));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets/{basketId}/constituents", testBasketId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].symbol").isEqualTo("AAPL");
    }

    @Test
    void getBasketAnalytics_Success() {
        // Given
        BasketAnalyticsResponse analytics = new BasketAnalyticsResponse(
                testBasketId.toString(),
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.12),
                BigDecimal.valueOf(0.15),
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(0.08),
                List.of(),
                LocalDateTime.now()
        );

        when(basketService.getBasketAnalytics(testBasketId)).thenReturn(Mono.just(analytics));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets/{basketId}/analytics", testBasketId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.basketCode").isEqualTo(testBasketId.toString())
                .jsonPath("$.data.totalMarketValue").isEqualTo(1000000);
    }

    @Test
    void getSectorExposure_Success() {
        // Given
        SectorExposureResponse exposure = new SectorExposureResponse(
                "Technology",
                BigDecimal.valueOf(50.0),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(0.08)
        );

        when(basketService.getSectorExposure(testBasketId))
                .thenReturn(Mono.just(List.of(exposure)));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets/{basketId}/sector-exposure", testBasketId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].sector").isEqualTo("Technology");
    }

    @Test
    void getHealth_Success() {
        // Given
        HealthResponse health = new HealthResponse(
                "UP",
                LocalDateTime.now(),
                Map.of("service", "Basket Core Service", "status", "healthy")
        );

        when(basketService.getHealth()).thenReturn(Mono.just(health));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.status").isEqualTo("UP");
    }

    @Test
    void getMetrics_Success() {
        // Given
        MetricsResponse metrics = new MetricsResponse(
                LocalDateTime.now(),
                Map.of("active_baskets", 10, "total_constituents", 250)
        );

        when(basketService.getMetrics()).thenReturn(Mono.just(metrics));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/baskets/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.metrics.active_baskets").isEqualTo(10)
                .jsonPath("$.data.metrics.total_constituents").isEqualTo(250);
    }
}
