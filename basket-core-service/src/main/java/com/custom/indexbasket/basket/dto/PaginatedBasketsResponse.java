package com.custom.indexbasket.basket.dto;

import com.custom.indexbasket.basket.domain.BasketEntity;
import java.util.List;

public record PaginatedBasketsResponse(
    List<BasketEntity> baskets,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
