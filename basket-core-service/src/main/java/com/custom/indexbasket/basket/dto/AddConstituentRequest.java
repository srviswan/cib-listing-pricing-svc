package com.custom.indexbasket.basket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AddConstituentRequest(
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z0-9.]{1,20}$", message = "Symbol must be 1-20 characters, uppercase letters, numbers, and dots only")
    String symbol,
    
    @Size(max = 255, message = "Symbol name must not exceed 255 characters")
    String symbolName,
    
    @NotNull(message = "Weight is required")
    BigDecimal weight,
    
    Long shares,
    
    BigDecimal targetAllocation,
    
    @Size(max = 100, message = "Sector must not exceed 100 characters")
    String sector,
    
    @Size(max = 100, message = "Country must not exceed 100 characters")
    String country,
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter currency code")
    String currency
) {}
