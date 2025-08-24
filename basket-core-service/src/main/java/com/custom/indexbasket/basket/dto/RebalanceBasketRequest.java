package com.custom.indexbasket.basket.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RebalanceBasketRequest(
    @NotNull(message = "New constituents are required")
    List<CreateConstituentRequest> newConstituents,
    
    String updatedBy
) {}
