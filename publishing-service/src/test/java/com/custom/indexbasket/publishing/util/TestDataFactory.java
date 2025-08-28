package com.custom.indexbasket.publishing.util;

import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.dto.PublishingResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Test Data Factory for Publishing Service Tests
 */
public class TestDataFactory {

    /**
     * Create a sample basket listing request
     */
    public static BasketListingRequest createSampleBasketListingRequest() {
        return BasketListingRequest.builder()
            .basketId("TEST_BASKET_001")
            .basketCode("TECH_ETF")
            .basketName("Technology ETF Basket")
            .basketType("EQUITY")
            .baseCurrency("USD")
            .totalWeight(100.0)
            .constituents(Arrays.asList(
                BasketListingRequest.BasketConstituentRequest.builder()
                    .symbol("AAPL")
                    .symbolName("Apple Inc.")
                    .weight(25.0)
                    .shares(1000L)
                    .sector("Technology")
                    .country("USA")
                    .currency("USD")
                    .build(),
                BasketListingRequest.BasketConstituentRequest.builder()
                    .symbol("MSFT")
                    .symbolName("Microsoft Corporation")
                    .weight(25.0)
                    .shares(800L)
                    .sector("Technology")
                    .country("USA")
                    .currency("USD")
                    .build(),
                BasketListingRequest.BasketConstituentRequest.builder()
                    .symbol("GOOGL")
                    .symbolName("Alphabet Inc.")
                    .weight(20.0)
                    .shares(500L)
                    .sector("Technology")
                    .country("USA")
                    .currency("USD")
                    .build(),
                BasketListingRequest.BasketConstituentRequest.builder()
                    .symbol("AMZN")
                    .symbolName("Amazon.com Inc.")
                    .weight(20.0)
                    .shares(600L)
                    .sector("Technology")
                    .country("USA")
                    .currency("USD")
                    .build(),
                BasketListingRequest.BasketConstituentRequest.builder()
                    .symbol("TSLA")
                    .symbolName("Tesla Inc.")
                    .weight(10.0)
                    .shares(400L)
                    .sector("Technology")
                    .country("USA")
                    .currency("USD")
                    .build()
            ))
            .build();
    }

    /**
     * Create a sample price publishing request
     */
    public static PricePublishingRequest createSamplePricePublishingRequest() {
        return PricePublishingRequest.builder()
            .basketId("TEST_BASKET_001")
            .basketCode("TECH_ETF")
            .price(BigDecimal.valueOf(150.75))
            .currency("USD")
            .timestamp(LocalDateTime.now())
            .changeAmount(BigDecimal.valueOf(2.25))
            .changePercentage(BigDecimal.valueOf(1.52))
            .openPrice(BigDecimal.valueOf(148.50))
            .highPrice(BigDecimal.valueOf(151.00))
            .lowPrice(BigDecimal.valueOf(148.00))
            .volume(1000000L)
            .exchange("NASDAQ")
            .build();
    }

    /**
     * Create a sample successful publishing result
     */
    public static PublishingResult createSampleSuccessfulResult(String vendor, String basketId) {
        return PublishingResult.success(vendor, basketId, 
            vendor + "_" + System.currentTimeMillis(), 
            Duration.ofMillis(150 + (int)(Math.random() * 100)));
    }

    /**
     * Create a sample failed publishing result
     */
    public static PublishingResult createSampleFailedResult(String vendor, String basketId, String errorMessage) {
        return PublishingResult.failed(vendor, basketId, errorMessage, 
            Duration.ofMillis(200 + (int)(Math.random() * 100)));
    }

    /**
     * Create a list of sample publishing results
     */
    public static List<PublishingResult> createSamplePublishingResults(String basketId) {
        return Arrays.asList(
            createSampleSuccessfulResult("BLOOMBERG", basketId),
            createSampleSuccessfulResult("REFINITIV", basketId),
            createSampleSuccessfulResult("GENERIC", basketId)
        );
    }

    /**
     * Create a sample publishing result with specific operation type
     */
    public static PublishingResult createSampleResultWithOperationType(String vendor, String basketId, String operationType) {
        PublishingResult result = createSampleSuccessfulResult(vendor, basketId);
        result.setOperationType(operationType);
        return result;
    }
}
