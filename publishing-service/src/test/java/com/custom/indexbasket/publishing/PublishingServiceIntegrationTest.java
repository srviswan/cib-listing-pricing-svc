package com.custom.indexbasket.publishing;

import com.custom.indexbasket.publishing.dto.BasketListingRequest;
import com.custom.indexbasket.publishing.dto.PricePublishingRequest;
import com.custom.indexbasket.publishing.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Publishing Service
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PublishingServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
        assertNotNull(restTemplate);
    }

    @Test
    void healthEndpointShouldReturnUp() {
        // Test health endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/publishing/health", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("Publishing Service", response.getBody().get("service"));
    }

    @Test
    void vendorHealthEndpointShouldReturnVendors() {
        // Test vendor health endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/publishing/vendors/health", Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HEALTHY", response.getBody().get("overallStatus"));
        assertNotNull(response.getBody().get("vendors"));
        assertTrue(((java.util.List) response.getBody().get("vendors")).size() > 0);
    }

    @Test
    void basketListingEndpointShouldAcceptValidRequest() {
        // Test basket listing endpoint with valid request
        BasketListingRequest request = TestDataFactory.createSampleBasketListingRequest();
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/publishing/basket/TEST_BASKET_001/list",
            request, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("Basket listing started successfully", response.getBody().get("message"));
    }

    @Test
    void pricePublishingEndpointShouldAcceptValidRequest() {
        // Test price publishing endpoint with valid request
        PricePublishingRequest request = TestDataFactory.createSamplePricePublishingRequest();
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/publishing/basket/TEST_BASKET_001/price",
            request, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("Basket price published successfully", response.getBody().get("message"));
    }

    @Test
    void testDataFactoryShouldCreateValidData() {
        // Test that our test data factory creates valid objects
        BasketListingRequest listingRequest = TestDataFactory.createSampleBasketListingRequest();
        PricePublishingRequest priceRequest = TestDataFactory.createSamplePricePublishingRequest();
        
        assertNotNull(listingRequest);
        assertEquals("TEST_BASKET_001", listingRequest.getBasketId());
        assertEquals("TECH_ETF", listingRequest.getBasketCode());
        assertEquals(5, listingRequest.getConstituents().size());
        
        assertNotNull(priceRequest);
        assertEquals("TEST_BASKET_001", priceRequest.getBasketId());
        assertEquals("TECH_ETF", priceRequest.getBasketCode());
        assertNotNull(priceRequest.getPrice());
        assertEquals(0, priceRequest.getPrice().compareTo(java.math.BigDecimal.valueOf(150.75)));
    }
}
