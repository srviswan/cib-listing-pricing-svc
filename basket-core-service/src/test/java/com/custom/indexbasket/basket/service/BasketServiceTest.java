package com.custom.indexbasket.basket.service;

import com.custom.indexbasket.basket.domain.BasketEntity;
import com.custom.indexbasket.basket.domain.BasketConstituentEntity;
import com.custom.indexbasket.basket.dto.*;
import com.custom.indexbasket.basket.repository.BasketRepository;
import com.custom.indexbasket.basket.repository.BasketConstituentRepository;
import com.custom.indexbasket.common.model.BasketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.custom.indexbasket.basket.config.MonitoringConfig.BasketMetrics;
import com.custom.indexbasket.basket.config.StateMachineConfig;
import io.micrometer.core.instrument.Timer;
import com.custom.indexbasket.basket.event.EventPublisher;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    @Mock
    private BasketRepository basketRepository;
    
    @Mock
    private BasketConstituentRepository basketConstituentRepository;
    
    @Mock
    private BasketMetrics basketMetrics;
    
    @Mock
    private StateMachineConfig stateMachineConfig;
    
    @Mock
    private EventPublisher eventPublisher;
    
    private BasketService basketService;
    private BasketEntity testBasket;
    private BasketConstituentEntity testConstituent;
    private UUID testBasketId;

    @BeforeEach
    void setUp() {
        // Mock the timer methods
        when(basketMetrics.startCreateBasketTimer()).thenReturn(Timer.start());
        when(basketMetrics.startReadBasketTimer()).thenReturn(Timer.start());
        when(basketMetrics.startUpdateBasketTimer()).thenReturn(Timer.start());
        when(basketMetrics.startDeleteBasketTimer()).thenReturn(Timer.start());
        when(basketMetrics.startListBasketsTimer()).thenReturn(Timer.start());
        
        // Mock the state machine config
        when(stateMachineConfig.determineNewStatus(any(BasketStatus.class), anyString())).thenReturn(BasketStatus.ACTIVE);
        
        basketService = new BasketService(
            basketRepository, 
            basketConstituentRepository, 
            basketMetrics, 
            stateMachineConfig,
            eventPublisher
        );
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

        testConstituent = new BasketConstituentEntity();
        testConstituent.setEntityId(UUID.randomUUID());
        testConstituent.setBasketId(testBasketId);
        testConstituent.setSymbol("AAPL");
        testConstituent.setSymbolName("Apple Inc.");
        testConstituent.setWeight(BigDecimal.valueOf(10.0));
        testConstituent.setShares(100L);
        testConstituent.setSector("Technology");
        testConstituent.setCountry("US");
        testConstituent.setCurrency("USD");
        testConstituent.setIsActive(true);
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

        when(basketRepository.save(any(BasketEntity.class))).thenReturn(Mono.just(testBasket));

        // When & Then
        StepVerifier.create(basketService.createBasket(request))
                .expectNext(testBasketId)
                .verifyComplete();

        verify(basketRepository).save(any(BasketEntity.class));
    }

    @Test
    void getBasket_Success() {
        // Given
        when(basketService.getBasketById(testBasketId)).thenReturn(Mono.just(testBasket));

        // When & Then
        StepVerifier.create(basketService.getBasketById(testBasketId))
                .expectNext(testBasket)
                .verifyComplete();

        verify(basketRepository).findById(testBasketId);
    }

    @Test
    void getBasket_NotFound() {
        // Given
        when(basketRepository.findById(testBasketId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(basketService.getBasketById(testBasketId))
                .expectError(RuntimeException.class)
                .verify();

        verify(basketRepository).findById(testBasketId);
    }

    @Test
    void getAllBaskets_WithoutFilters() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(basketRepository.findAllActive()).thenReturn(Flux.just(testBasket));
        when(basketRepository.countActive()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(basketService.getAllBaskets(pageRequest, null, null))
                .expectNextMatches(response -> 
                    response.baskets().size() == 1 &&
                    response.baskets().get(0).equals(testBasket) &&
                    response.totalElements() == 1L &&
                    response.page() == 0 &&
                    response.size() == 20 &&
                    response.totalPages() == 1
                )
                .verifyComplete();

        verify(basketRepository).findAllActive();
        verify(basketRepository).countActive();
    }

    @Test
    void getAllBaskets_WithStatusFilter() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 20);
        BasketStatus status = BasketStatus.DRAFT;
        when(basketRepository.findByStatus(status)).thenReturn(Flux.just(testBasket));
        when(basketRepository.countByStatus(status)).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(basketService.getAllBaskets(pageRequest, status, null))
                .expectNextMatches(response -> 
                    response.baskets().size() == 1 &&
                    response.totalElements() == 1L
                )
                .verifyComplete();

        verify(basketRepository).findByStatus(status);
        verify(basketRepository).countByStatus(status);
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
        updatedBasket.setDescription("Updated Description");

        when(basketRepository.findById(testBasketId)).thenReturn(Mono.just(testBasket));
        when(basketRepository.save(any(BasketEntity.class))).thenReturn(Mono.just(updatedBasket));

        // When & Then
        StepVerifier.create(basketService.updateBasket(testBasketId, request))
                .expectNextMatches(basket -> 
                    "Updated Basket Name".equals(basket.getBasketName()) &&
                    "Updated Description".equals(basket.getDescription())
                )
                .verifyComplete();

        verify(basketRepository).findById(testBasketId);
        verify(basketRepository).save(any(BasketEntity.class));
    }

    @Test
    void updateBasket_NotFound() {
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

        when(basketRepository.findById(testBasketId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(basketService.updateBasket(testBasketId, request))
                .expectError(RuntimeException.class)
                .verify();

        verify(basketRepository).findById(testBasketId);
        verify(basketRepository, never()).save(any(BasketEntity.class));
    }

    @Test
    void updateBasketStatus_Success() {
        // Given
        String event = "ACTIVATE"; // Use event string instead of BasketStatus
        String updatedBy = "admin";

        when(basketRepository.findById(testBasketId)).thenReturn(Mono.just(testBasket));
        when(basketRepository.updateBasketFields(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(basketService.updateBasketStatus(testBasketId, event, updatedBy))
                .verifyComplete();

        verify(basketRepository).findById(testBasketId);
        verify(basketRepository).updateBasketFields(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteBasket_Success() {
        // Given
        when(basketRepository.findById(testBasketId)).thenReturn(Mono.just(testBasket));
        when(basketRepository.softDeleteBasket(any(), any(), any(), any())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(basketService.deleteBasket(testBasketId, "testuser"))
                .verifyComplete();

        verify(basketRepository).findById(testBasketId);
        verify(basketRepository).softDeleteBasket(any(), any(), any(), any());
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

        when(basketConstituentRepository.save(any(BasketConstituentEntity.class)))
                .thenReturn(Mono.just(testConstituent));

        // When & Then
        StepVerifier.create(basketService.addConstituent(testBasketId, request))
                .expectNext(testConstituent)
                .verifyComplete();

        verify(basketConstituentRepository).save(any(BasketConstituentEntity.class));
    }

    @Test
    void updateConstituentWeight_Success() {
        // Given
        String symbol = "AAPL";
        BigDecimal newWeight = BigDecimal.valueOf(15.0);
        String updatedBy = "admin";

        when(basketConstituentRepository.findByBasketIdAndSymbol(testBasketId, symbol))
                .thenReturn(Mono.just(testConstituent));
        when(basketConstituentRepository.save(any(BasketConstituentEntity.class)))
                .thenReturn(Mono.just(testConstituent));

        // When & Then
        StepVerifier.create(basketService.updateConstituentWeight(testBasketId, symbol, newWeight, updatedBy))
                .verifyComplete();

        verify(basketConstituentRepository).findByBasketIdAndSymbol(testBasketId, symbol);
        verify(basketConstituentRepository).save(any(BasketConstituentEntity.class));
    }

    @Test
    void removeConstituent_Success() {
        // Given
        String symbol = "AAPL";

        when(basketConstituentRepository.findByBasketIdAndSymbol(testBasketId, symbol))
                .thenReturn(Mono.just(testConstituent));
        when(basketConstituentRepository.save(any(BasketConstituentEntity.class)))
                .thenReturn(Mono.just(testConstituent));

        // When & Then
        StepVerifier.create(basketService.removeConstituent(testBasketId, symbol))
                .verifyComplete();

        verify(basketConstituentRepository).findByBasketIdAndSymbol(testBasketId, symbol);
        verify(basketConstituentRepository).save(any(BasketConstituentEntity.class));
    }

    @Test
    void getBasketConstituents_Success() {
        // Given
        when(basketConstituentRepository.findByBasketId(testBasketId))
                .thenReturn(Flux.just(testConstituent));

        // When & Then
        StepVerifier.create(basketService.getBasketConstituents(testBasketId))
                .expectNext(List.of(testConstituent))
                .verifyComplete();

        verify(basketConstituentRepository).findByBasketId(testBasketId);
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
        List<CreateConstituentRequest> newConstituents = List.of(newConstituent);
        String updatedBy = "admin";

        when(basketRepository.findById(testBasketId)).thenReturn(Mono.just(testBasket));
        when(basketConstituentRepository.findByBasketId(testBasketId)).thenReturn(Flux.just(testConstituent));
        when(basketConstituentRepository.save(any(BasketConstituentEntity.class))).thenReturn(Mono.just(testConstituent));
        when(basketRepository.save(any(BasketEntity.class))).thenReturn(Mono.just(testBasket));

        // When & Then
        StepVerifier.create(basketService.rebalanceBasket(testBasketId, newConstituents, updatedBy))
                .expectNext(testBasket)
                .verifyComplete();

        verify(basketRepository).findById(testBasketId);
        verify(basketConstituentRepository).findByBasketId(testBasketId);
        verify(basketConstituentRepository, times(2)).save(any(BasketConstituentEntity.class)); // Deactivate old + add new
        verify(basketRepository).save(any(BasketEntity.class));
    }

    @Test
    void getBasketAnalytics_Success() {
        // When & Then
        StepVerifier.create(basketService.getBasketAnalytics(testBasketId))
                .expectNextMatches(analytics -> 
                    analytics.basketCode().equals(testBasketId.toString()) &&
                    analytics.totalMarketValue().equals(BigDecimal.ZERO)
                )
                .verifyComplete();
    }

    @Test
    void getSectorExposure_Success() {
        // When & Then
        StepVerifier.create(basketService.getSectorExposure(testBasketId))
                .expectNext(List.of())
                .verifyComplete();
    }

    @Test
    void getHealth_Success() {
        // When & Then
        StepVerifier.create(basketService.getHealth())
                .expectNextMatches(health -> 
                    "UP".equals(health.status()) &&
                    health.details().containsKey("service")
                )
                .verifyComplete();
    }

    @Test
    void getMetrics_Success() {
        // When & Then
        StepVerifier.create(basketService.getMetrics())
                .expectNextMatches(metrics -> 
                    metrics.metrics().containsKey("active_baskets") &&
                    metrics.metrics().containsKey("total_constituents")
                )
                .verifyComplete();
    }
}
