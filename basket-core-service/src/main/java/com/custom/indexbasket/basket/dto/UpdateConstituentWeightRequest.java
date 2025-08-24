package com.custom.indexbasket.basket.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateConstituentWeightRequest(
    @NotNull(message = "New weight is required")
    BigDecimal newWeight,
    
    String updatedBy
) {}
