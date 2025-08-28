package com.custom.indexbasket.publishing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request for basket listing operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasketListingRequest {

    @NotBlank(message = "Basket ID is required")
    private String basketId;
    
    @NotBlank(message = "Basket code is required")
    private String basketCode;
    
    @NotBlank(message = "Basket name is required")
    private String basketName;
    
    @NotBlank(message = "Basket type is required")
    private String basketType;
    
    @NotBlank(message = "Base currency is required")
    private String baseCurrency;
    
    @NotNull(message = "Total weight is required")
    private Double totalWeight;
    
    @NotNull(message = "Constituents are required")
    private List<BasketConstituentRequest> constituents;
    
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasketConstituentRequest {
        @NotBlank(message = "Symbol is required")
        private String symbol;
        
        @NotBlank(message = "Symbol name is required")
        private String symbolName;
        
        @NotNull(message = "Weight is required")
        private Double weight;
        
        private Long shares;
        private String sector;
        private String country;
        private String currency;
    }
}
