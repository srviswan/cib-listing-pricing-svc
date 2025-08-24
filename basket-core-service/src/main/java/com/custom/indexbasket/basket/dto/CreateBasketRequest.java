package com.custom.indexbasket.basket.dto;

import com.custom.indexbasket.common.model.BasketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateBasketRequest(
    @NotBlank(message = "Basket code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Basket code must be 3-50 characters, uppercase letters, numbers, and underscores only")
    String basketCode,
    
    @NotBlank(message = "Basket name is required")
    @Size(min = 1, max = 255, message = "Basket name must be between 1 and 255 characters")
    String basketName,
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,
    
    @NotBlank(message = "Basket type is required")
    @Pattern(regexp = "^(EQUITY|FIXED_INCOME|COMMODITY|MIXED)$", message = "Basket type must be EQUITY, FIXED_INCOME, COMMODITY, or MIXED")
    String basketType,
    
    @NotBlank(message = "Base currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency must be a 3-letter currency code")
    String baseCurrency,
    
    @NotNull(message = "Total weight is required")
    BigDecimal totalWeight,
    
    @NotBlank(message = "Version is required")
    @Pattern(regexp = "^v\\d+\\.\\d+(\\.\\d+)?$", message = "Version must be in format vX.Y or vX.Y.Z")
    String version,
    
    String previousVersion,
    
    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    String createdBy,
    
    List<CreateConstituentRequest> constituents
) {}
